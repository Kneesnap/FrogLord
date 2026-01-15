package net.highwayfrogs.editor.games.sony.shared.vlo2;

import javafx.scene.image.Image;
import lombok.Getter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.games.sony.SCGameFile.SCSharedGameFile;
import net.highwayfrogs.editor.games.sony.SCGameInstance;
import net.highwayfrogs.editor.games.sony.shared.ui.file.VLOController;
import net.highwayfrogs.editor.gui.ImageResource;
import net.highwayfrogs.editor.gui.SelectionMenu;
import net.highwayfrogs.editor.gui.components.propertylist.PropertyListNode;
import net.highwayfrogs.editor.system.IntList;
import net.highwayfrogs.editor.utils.FileUtils;
import net.highwayfrogs.editor.utils.StringUtils;
import net.highwayfrogs.editor.utils.Utils;
import net.highwayfrogs.editor.utils.data.reader.DataReader;
import net.highwayfrogs.editor.utils.data.writer.DataWriter;

import javax.imageio.ImageIO;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Represents a .VLO file written using Vorg2. (1997-2001)
 * Created by Kneesnap on 11/25/2025.
 */
public class VloFile extends SCSharedGameFile {
    private final List<VloImage> images = new ArrayList<>();
    private final List<VloImage> immutableImages = Collections.unmodifiableList(this.images);
    @Getter private final VloClutList clutList;
    @Getter private boolean psxMode;

    public static final String PC_SIGNATURE = "2GRP";
    public static final String PSX_SIGNATURE = "2GRV";
    private static final int SIGNATURE_LENGTH = 4;

    public static final int ICON_EXPORT = VloImage.DEFAULT_IMAGE_NO_PADDING_EXPORT_SETTINGS;
    private static final short CUSTOM_DATA_VERSION = 0;

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
        for (int i = 0; i < imageCount; i++) {
            VloImage image = new VloImage(this);
            image.load(reader);
            this.images.add(image);
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
        for (VloImage testImage : getImages())
            if (testImage.getTextureId() == textureId)
                return testImage;

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
}