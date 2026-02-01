package net.highwayfrogs.editor.games.sony.shared.vlo2;

import javafx.scene.image.Image;
import lombok.Getter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.games.psx.image.PsxAbrTransparency;
import net.highwayfrogs.editor.games.psx.image.PsxImageBitDepth;
import net.highwayfrogs.editor.games.sony.SCGameFile;
import net.highwayfrogs.editor.games.sony.SCGameFile.SCSharedGameFile;
import net.highwayfrogs.editor.games.sony.SCGameInstance;
import net.highwayfrogs.editor.games.sony.SCGameType;
import net.highwayfrogs.editor.games.sony.shared.ui.file.VLOController;
import net.highwayfrogs.editor.games.sony.shared.vlo2.vram.VloTextureIdTracker;
import net.highwayfrogs.editor.games.sony.shared.vlo2.vram.VloTree;
import net.highwayfrogs.editor.games.sony.shared.vlo2.vram.VloTreeNode;
import net.highwayfrogs.editor.games.sony.shared.vlo2.vram.VloVramSnapshot;
import net.highwayfrogs.editor.gui.ImageResource;
import net.highwayfrogs.editor.gui.SelectionMenu;
import net.highwayfrogs.editor.gui.components.propertylist.PropertyListNode;
import net.highwayfrogs.editor.system.IntList;
import net.highwayfrogs.editor.utils.FileUtils;
import net.highwayfrogs.editor.utils.StringUtils;
import net.highwayfrogs.editor.utils.Utils;
import net.highwayfrogs.editor.utils.Utils.ProblemResponse;
import net.highwayfrogs.editor.utils.data.reader.DataReader;
import net.highwayfrogs.editor.utils.data.writer.DataWriter;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.function.Consumer;

/**
 * Represents a .VLO file written using Vorg2. (1997-2001)
 * Created by Kneesnap on 11/25/2025.
 */
public class VloFile extends SCSharedGameFile {
    private final List<VloImage> images = new ArrayList<>(); // These images appear ordered in the order which they appear to have been inserted into VRAM.
    private final List<VloImage> immutableImages = Collections.unmodifiableList(this.images);
    @Getter private final VloClutList clutList;
    @Getter private boolean psxMode;
    @Getter private boolean vramDirty;

    public static final String PC_SIGNATURE = "2GRP";
    public static final String PSX_SIGNATURE = "2GRV";
    private static final int SIGNATURE_LENGTH = 4;

    public static final int ICON_EXPORT = VloImage.DEFAULT_IMAGE_NO_PADDING_EXPORT_SETTINGS;
    private static final short CUSTOM_DATA_VERSION = 0;

    // This comparator is an attempt to mimic the sorting order for the images list.
    // This images list is also the order which textures appear to have been added into VRAM with.
    // Tiebreaker logic seems arbitrary. In some places it really looks like it's texture ID based, but then in those same files we'll also see places where it's clearly not.
    // The other obvious idea is texture name, but that's not entirely right either... The texture name ordering just isn't consistent enough.
    // I suspect there was no explicit sorting tiebreaker, and this may have been something arbitrary like the order of images in the texture list Vorg would read or something.
    // NOTE: This sorting order works with:
    //  - Old Frogger (PSX, PC)
    //  - Frogger (PSX, PC)
    //  - Beast Wars (PSX, PC)
    //  - MediEvil (PSX)
    // It has been confirmed NOT to work with:
    //  - MoonWarrior
    //  - MediEvil II
    //  - C-12 Final Resistance
    public static Comparator<VloImage> IMAGE_SORTING_ORDER = Comparator
            .comparingInt((VloImage image) -> image.getUnitWidth() * image.getPaddedHeight()).reversed();

    public static final boolean DEBUG_VALIDATE_IMAGE_EXPORT_IMPORT = false;

    public VloFile(SCGameInstance instance) {
        super(instance);
        this.clutList = new VloClutList(instance);
    }

    @Override
    public void load(DataReader reader) {
        String readSignature = reader.readTerminatedString(SIGNATURE_LENGTH);
        if (readSignature.equals(PSX_SIGNATURE)) {
            this.psxMode = true;
        } else {
            Utils.verify(readSignature.equals(PC_SIGNATURE), "Invalid VLO signature: %s.", readSignature);
        }

        int imageCount = reader.readInt();
        int textureOffset = reader.readInt();

        // Load clut data.
        this.clutList.clear();
        List<VloClut> cluts = this.clutList.getCluts();
        int clutStartAddress = -1, clutEndAddress = -1;
        int clutColorsStartAddress = -1, clutColorsEndAddress = -1;
        if (isPsxMode()) { // GRV file has clut data.
            int clutCount = reader.readInt();
            clutStartAddress = reader.readInt();

            // This stuff is at the end of the file, so we'll track addresses to skip this data when we get there.
            reader.jumpTemp(clutStartAddress);
            for (int i = 0; i < clutCount; i++) {
                VloClut clut = new VloClut(this);
                clut.load(reader);
                this.clutList.addClut(clut);
            }

            clutEndAddress = reader.getIndex();
            reader.jumpReturn();

            // Load clut colors.
            clutColorsStartAddress = cluts.size() > 0 ? cluts.get(0).tempColorsPointer : -1;
            clutColorsEndAddress = clutColorsStartAddress;
            reader.jumpTemp(reader.getIndex());
            if (clutColorsStartAddress >= 0)
                reader.setIndex(clutColorsStartAddress);
            for (int i = 0; i < cluts.size(); i++)
                clutColorsEndAddress = Math.max(clutColorsEndAddress, cluts.get(i).readColors(reader));
            reader.jumpReturn();
        }

        // Load image data.
        this.images.clear();
        requireReaderIndex(reader, textureOffset, "Expected VLO texture data");
        VloImage lastImage = null;
        for (int i = 0; i < imageCount; i++) {
            VloImage image = new VloImage(this);
            image.load(reader);
            this.images.add(image);

            if (lastImage != null && isSortingOrderKnown() && IMAGE_SORTING_ORDER.compare(lastImage, image) > 0)
                getLogger().warning("%s is out of order with %s.", image, lastImage);
            lastImage = image;
        }

        // Skip CLUT data.
        if (clutStartAddress >= 0)
            requireReaderIndex(reader, clutStartAddress, "Expected CLUT data");
        if (clutEndAddress >= 0)
            reader.setIndex(clutEndAddress);

        // Read image data.
        for (int i = 0; i < this.images.size(); i++) {
            reader.alignRequireEmpty(Constants.INTEGER_SIZE);
            this.images.get(i).readImageData(reader, this.clutList);
        }

        // Validate image data.
        if (DEBUG_VALIDATE_IMAGE_EXPORT_IMPORT)
            validateImageData(); // Disabled unless a developer specifically needs to use this.

        // Skip CLUT Color Data
        reader.alignRequireEmpty(Constants.INTEGER_SIZE);
        if (clutColorsStartAddress >= 0)
            requireReaderIndex(reader, clutColorsStartAddress, "Expected CLUT color data");
        if (clutColorsEndAddress >= 0)
            reader.setIndex(clutColorsEndAddress);

        // FrogLord custom data (Texture names!)
        if (reader.hasMore())
            readCustomFrogLordData(reader);
    }

    private void readCustomFrogLordData(DataReader reader) {
        reader.verifyString(FileUtils.FROGLORD_EXECUTABLE_SIGNATURE);
        int version = reader.readUnsignedByte();
        if (version > CUSTOM_DATA_VERSION)
            getLogger().warning("Unsupported FrogLord texture data version %d found! (Supported Version: %d)", version, CUSTOM_DATA_VERSION);

        int textureNameCount = reader.readUnsignedShortAsInt();
        for (int i = 0; i < textureNameCount; i++) {
            int index = reader.readUnsignedShortAsInt();
            int length = reader.readUnsignedByte();
            String name = reader.readTerminatedString(length);
            this.images.get(index).setCustomName(name);
        }
    }

    @Override
    public void save(DataWriter writer) {
        writer.writeStringBytes(isPsxMode() ? PSX_SIGNATURE : PC_SIGNATURE);
        writer.writeInt(this.images.size());
        int imageHeaderPointer = writer.writeNullPointer();

        int clutStartAddress = -1;
        if (isPsxMode()) {
            writer.writeInt(this.clutList.getCluts().size());
            clutStartAddress = writer.writeNullPointer(); // This will be written later.
        }

        // Write images.
        writer.writeAddressTo(imageHeaderPointer);
        for (int i = 0; i < this.images.size(); i++)
            this.images.get(i).save(writer);

        // Write CLUT entries.
        if (isPsxMode()) {
            writer.writeAddressTo(clutStartAddress);
            List<VloClut> cluts = this.clutList.getCluts();
            for (int i = 0; i < cluts.size(); i++)
                cluts.get(i).save(writer);
        }

        // Write image data.
        for (int i = 0; i < this.images.size(); i++) {
            writer.align(Constants.INTEGER_SIZE);

            VloImage image = this.images.get(i);
            try {
                image.writeImageData(writer, this.clutList);
            } catch (Throwable th) {
                throw new RuntimeException("Failed to save image data for " + image + ".", th);
            }
        }

        // Write clut colors.
        writer.align(Constants.INTEGER_SIZE);
        if (isPsxMode()) {
            List<VloClut> cluts = this.clutList.getCluts();
            for (int i = 0; i < cluts.size(); i++)
                cluts.get(i).writeColors(writer);
        }

        writeCustomFrogLordDataIfNecessary(writer);
    }

    private void writeCustomFrogLordDataIfNecessary(DataWriter writer) {
        IntList imagesWithCustomNames = new IntList();
        for (int i = 0; i < this.images.size(); i++) {
            VloImage image = this.images.get(i);
            String name = image.getName();
            String originalName = image.getOriginalName();
            if (!Objects.equals(name, originalName))
                imagesWithCustomNames.add(i);
        }

        if (imagesWithCustomNames.isEmpty())
            return; // No need to write custom data.

        writer.writeStringBytes(FileUtils.FROGLORD_EXECUTABLE_SIGNATURE);
        writer.writeUnsignedByte(CUSTOM_DATA_VERSION);

        // Write texture names.
        writer.writeUnsignedShort(imagesWithCustomNames.size());
        for (int i = 0; i < imagesWithCustomNames.size(); i++) {
            int index = imagesWithCustomNames.get(i);
            VloImage image = this.images.get(index);
            String imageName = image.getName();
            writer.writeUnsignedShort(index);
            writer.writeUnsignedByte((short) imageName.length());
            writer.writeStringBytes(imageName);
        }
    }

    @Override
    public void onImport(SCGameFile<?> oldFile, String oldFileName, String importedFileName) {
        super.onImport(oldFile, oldFileName, importedFileName);

        // Stop tracking old images.
        if (oldFile instanceof VloFile) {
            VloFile oldVlo = (VloFile) oldFile;
            for (int i = 0; i < oldVlo.images.size(); i++) {
                VloImage image = oldVlo.images.get(i);
                getArchive().stopTrackingImageByTextureId(image, image.getTextureId());
            }
        }

        // Track texture ids.
        for (int i = 0; i < this.images.size(); i++) {
            VloImage image = this.images.get(i);
            getArchive().startTrackingImageByTextureId(image, image.getTextureId());
        }

        VloTree tree = getGameInstance().getVloTree();
        VloTreeNode node = tree != null ? tree.getNode(this) : null;
        if (node != null) {
            node.loadFromGameDataRecursive(null);
            tree.calculateFreeTextureIds();
        }
    }

    @SuppressWarnings("unused") // This is used to ensure we haven't broken VLO files in some way.
    private void validateImageData() {
        int nonMatchingImages = 0;
        for (int i = 0; i < this.images.size(); i++) {
            VloImage image = this.images.get(i);
            if (image.getPaddedWidth() > VloImage.MAX_IMAGE_DIMENSION || image.getPaddedHeight() > VloImage.MAX_IMAGE_DIMENSION)
                continue; // It's not possible to re-import these images, so skip them.

            int[] startPixelBuffer = image.getPixelBuffer().clone();
            BufferedImage exportImage = image.toBufferedImage(VloImage.DEFAULT_IMAGE_NO_PADDING_EXPORT_SETTINGS);
            image.replaceImage(exportImage, ProblemResponse.THROW_EXCEPTION);
            if (!Arrays.equals(startPixelBuffer, image.getPixelBuffer())) {
                image.getLogger().warning("Re-import was not a byte-match to its original image data.");
                nonMatchingImages++;
            }
        }

        if (nonMatchingImages > 0)
            getLogger().warning("Image re-import problems found! (%d/%d images re-imported with problems)", nonMatchingImages, this.images.size());
    }

    /**
     * Gets the list of images.
     * @return images
     */
    public List<VloImage> getImages() {
        return this.immutableImages;
    }

    /**
     * Export all images in this VLO archive.
     */
    public void exportAllImages(File directory, int settings) {
        try {
            for (int i = 0; i < this.images.size(); i++) {
                File output = new File(directory, i + ".png");
                ImageIO.write(this.images.get(i).toBufferedImage(settings), "png", output);
            }

            getLogger().info("Exported %d image(s).", this.images.size());
        } catch (IOException ex) {
            getLogger().throwing(getClass().getSimpleName(), "exportAllImages", ex);
        }
    }

    @Override
    public Image getCollectionViewIcon() {
        return ImageResource.PHOTO_ALBUM_32.getFxImage();
    }

    @Override
    public VLOController makeEditorUI() {
        return loadEditor(getGameInstance(), "edit-file-vlo", new VLOController(getGameInstance()), this);
    }

    @Override
    public void addToPropertyList(PropertyListNode propertyList) {
        super.addToPropertyList(propertyList);
        propertyList.add("Images", getImages().size());
        propertyList.add("PS1 VLO", isPsxMode());
    }

    /**
     * Gets an image by the given texture ID.
     * @param textureId The texture ID to get.
     * @return gameImage
     */
    public VloImage getImageByTextureId(int textureId) {
        return getImageByTextureId(textureId, false);
    }

    /**
     * Gets an image by the given texture ID.
     * @param textureId The texture ID to get.
     * @return gameImage
     */
    public VloImage getImageByTextureId(int textureId, boolean errorIfFail) {
        // Using for-each loop/iterator right here was profiled and created an unreasonable amount of memory allocation due to how frequently this is called.
        for (int i = 0; i < this.images.size(); i++) {
            VloImage testImage = this.images.get(i);
            if (testImage.getTextureId() == textureId)
                return testImage;
        }

        if (errorIfFail)
            throw new RuntimeException("Could not find a texture with the id: " + textureId + ".");
        return null;
    }

    /**
     * Gets a texture in any VLO by its id, but tests this one first.
     * @param textureId The id to find
     */
    public VloImage getGlobalTexture(int textureId) {
        VloImage foundImage = getImageByTextureId(textureId, false);
        return foundImage != null ? foundImage : getArchive().getImageByTextureId(textureId);
    }

    /**
     * Select a VLO image
     * @param handler   The handler for when the VLO is determined.
     * @param allowNull Are null VLOs allowed?
     */
    public void promptImageSelection(Consumer<VloImage> handler, boolean allowNull) {
        List<VloImage> allImages = new ArrayList<>(getImages());

        if (allowNull)
            allImages.add(0, null);

        SelectionMenu.promptSelection(getGameInstance(), "Select an image.", handler, allImages,
                image -> image != null ? "#" + image.getLocalImageID() + " (" + image.getTextureId() + ")" : "No Image",
                image -> image.toFXImage(ICON_EXPORT));
    }

    /**
     * Finds an image with the given name
     * @param name the name of the image to find
     * @return image
     */
    public VloImage getImageByName(String name) {
        if (StringUtils.isNullOrEmpty(name))
            return null;

        for (int i = 0; i < this.images.size(); i++) {
            VloImage image = this.images.get(i);
            if (name.equals(image.getName()))
                return image;
        }

        return null;
    }

    /**
     * Mark the vlo file as being up-to-date / not needing any changes.
     * Only the vram position updater should call this.
     */
    public void markClean() {
        this.vramDirty = false;
    }

    /**
     * Mark the vlo file as being dirty / needing a rebuild.
     * Only the vram position updater should call this.
     */
    public void markDirty() {
        this.vramDirty = true;
    }

    /**
     * Adds a new image to the VLO file
     * @param name the name of the image to add
     * @param image the image to import
     * @param padding the padding behavior to apply. If null is provided, VloPadding.NONE will be used.
     * @param bitDepth the image bit-depth to apply
     * @param abr the ABR value to apply, ignored on PC.
     * @return newVloImage
     */
    public VloImage addImage(String name, BufferedImage image, VloPadding padding, PsxImageBitDepth bitDepth, PsxAbrTransparency abr, boolean translucent) {
        if (StringUtils.isNullOrWhiteSpace(name))
            throw new IllegalArgumentException("The provided image name was null/empty!");
        if (!VloImage.isValidTextureName(name))
            throw new IllegalArgumentException("Invalid texture name: '" + name + "', try something else.");
        if (image == null)
            throw new NullPointerException("image");
        if (padding == null)
            padding = VloPadding.NONE;

        // Find image.
        VloImage existingImage = getImageByName(name);
        if (existingImage != null)
            throw new IllegalArgumentException("Cannot add image named '" + name +"' because an image with that name already exists! (Local ID: " + existingImage.getLocalImageID() + ")");

        // Get tree.
        VloTree tree = getGameInstance().getVloTree();
        VloTextureIdTracker tracker = tree != null ? tree.getVloTextureIdTracker(this) : null;
        VloVramSnapshot snapshot = tree != null ? tree.getVramSnapshot(this) : null;
        if (tracker == null || snapshot == null)
            throw new IllegalStateException("Images cannot be added to " + getFileDisplayName() + ", because its VloTreeNode has not been configured by FrogLord developers yet.");

        // Select a texture ID for the image.
        // If the texture ID is known based on the name, this may involve reclaiming the texture ID from another image.
        short textureId;
        Short originalTextureId = getGameInstance().getTextureIdByOriginalName(name);
        if (originalTextureId != null) {
            textureId = originalTextureId;

            // If another texture is already using the image ID associated with this texture's name, reclaim the texture ID and force that texture to use another ID.
            VloImage conflictImage = getImageByTextureId(textureId, false);
            if (conflictImage != null)
                conflictImage.setTextureId(tracker.useFreeTextureId());
        } else {
            textureId = tracker.useFreeTextureId();
        }

        VloImage newImage = new VloImage(this);
        newImage.setTextureId(textureId);
        newImage.setCustomName(name);
        newImage.setFlag(VloImage.FLAG_REFERENCED_BY_NAME, getGameInstance().isTextureReferencedByName(textureId)); // Prevents crashes.
        newImage.setFlag(VloImage.FLAG_2D_SPRITE, true); // There is no downside to this.
        newImage.replaceImage(image, bitDepth, padding.getPaddingAmount(this), translucent, ProblemResponse.THROW_EXCEPTION);
        if (abr != null && this.psxMode)
            newImage.setAbr(abr);

        addImageToList(newImage);
        getArchive().startTrackingImageByTextureId(newImage, textureId);

        // Try to add to the VloTree.
        markDirty();
        if (!snapshot.tryAddImage(newImage, false))
            tree.markForRebuild();

        return newImage;
    }

    /**
     * Adds an image if none exist with the name.
     * If an image is found, replace it with the new image data.
     * @param name the image name
     * @param image the image to import
     * @param bitDepth the image bit-depth, null will use the default
     * @param padding the padding apply. If null is provided, VloPadding.NONE will be used instead.
     * @return image
     */
    @SuppressWarnings("unused") // Used by Noodle scripts.
    public VloImage addOrReplaceImage(String name, BufferedImage image, PsxImageBitDepth bitDepth, VloPadding padding, boolean translucent) {
        if (!VloImage.isValidTextureName(name))
            throw new IllegalArgumentException("Bad name: " + name);
        if (image == null)
            throw new NullPointerException("image");
        if (padding == null)
            padding = VloPadding.NONE;

        VloImage vloImage = getImageByName(name);
        if (vloImage != null) {
            int paddingAmount = padding.getPaddingAmount(this);
            vloImage.replaceImage(image, bitDepth, paddingAmount, translucent, ProblemResponse.THROW_EXCEPTION);
        } else {
            vloImage = addImage(name, image, padding, bitDepth, PsxAbrTransparency.DEFAULT, translucent);
        }

        return vloImage;
    }

    /**
     * Removes an image from this VloFile.
     * @param image the image to remove
     * @return if the image was successfully removed
     */
    public boolean removeImage(VloImage image) {
        if (!removeImageFromList(image))
            return false;

        getArchive().stopTrackingImageByTextureId(image, image.getTextureId());

        // Stop tracking the image in its clut.
        // This image object will not be re-added (instead, a new image object would be created), so this seems safe.
        VloClut clut = image.getClut();
        if (clut != null)
            clut.removeImage(image);

        // Change all usages of the texture.
        // This might be a good idea, but I'm not entirely sure in practice if this is preferable to keeping the usages.
        //getGameInstance().onVloTextureIdChange(image, image.getTextureId(), (short) -1);

        // Free the texture ID for use by the VLO file again.
        // This should happen last, after all other responses to the texture ID have occurred.
        VloTree vloTree = getGameInstance().getVloTree();
        if (vloTree != null) {
            VloTextureIdTracker textureIdTracker = vloTree.getVloTextureIdTracker(this);
            if (textureIdTracker != null)
                textureIdTracker.freeTextureId(image.getTextureId());
        }

        return true;
    }

    /**
     * Returns true if FrogLord knows how to sort the VloFile to mimic the original sorting order.
     */
    public boolean isSortingOrderKnown() {
        return !getGameInstance().getGameType().isAtLeast(SCGameType.MOONWARRIOR);
    }

    /**
     * Returns true if FrogLord supports clut fog in this file.
     */
    public boolean hasClutFogSupport() {
        SCGameType gameType = getGameInstance().getGameType();
        return gameType.isAtLeast(SCGameType.MOONWARRIOR) && gameType != SCGameType.C12;
    }

    int addImageToList(VloImage image) {
        if (!isSortingOrderKnown()) {
            int index = this.images.size();
            this.images.add(image);
            return index;
        }

        int searchIndex = Collections.binarySearch(this.images, image, IMAGE_SORTING_ORDER);
        int insertionIndex;
        if (searchIndex >= 0) {
            insertionIndex = searchIndex;
            while (this.images.size() > insertionIndex && IMAGE_SORTING_ORDER.compare(image, this.images.get(insertionIndex)) == 0)
                insertionIndex++; // Insert at the end of matching images.
        } else {
            // No matching entry was found, but we were given the insertion index.
            insertionIndex = -(searchIndex + 1);
        }

        this.images.add(insertionIndex, image);
        return insertionIndex;
    }

    boolean removeImageFromList(VloImage image) {
        return this.images.remove(image);
    }
}