package net.highwayfrogs.editor.games.psx;

import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.image.Image;
import jpsxdec.modules.video.save.MdecDecodeQuality;
import jpsxdec.psxvideo.mdec.ChromaUpsample;
import lombok.Getter;
import net.highwayfrogs.editor.games.psx.utils.PsxMdecUtils;
import net.highwayfrogs.editor.games.sony.SCGameFile.SCSharedGameFile;
import net.highwayfrogs.editor.games.sony.SCGameInstance;
import net.highwayfrogs.editor.games.sony.shared.ui.file.BitstreamImageController;
import net.highwayfrogs.editor.gui.GameUIController;
import net.highwayfrogs.editor.gui.ImageResource;
import net.highwayfrogs.editor.gui.components.PropertyListViewerComponent.PropertyList;
import net.highwayfrogs.editor.utils.FXUtils;
import net.highwayfrogs.editor.utils.FileUtils;
import net.highwayfrogs.editor.utils.Utils;
import net.highwayfrogs.editor.utils.data.reader.DataReader;
import net.highwayfrogs.editor.utils.data.writer.DataWriter;

import java.awt.image.BufferedImage;

/**
 * Represents a Bitstream image file. (.BS)
 * This is a format specific to the PSX, enabled by its custom MDEC chip which handles the image encoding/decoding process.
 * Created by Kneesnap on 9/23/2025.
 */
public class PSXBitstreamImage extends SCSharedGameFile {
    private byte[] encodedImageBytes;
    @Getter private BufferedImage cachedImage;

    public PSXBitstreamImage(SCGameInstance instance) {
        super(instance);
    }

    @Override
    public void load(DataReader reader) {
        this.encodedImageBytes = reader.readBytes(reader.getRemaining());
        this.cachedImage = null;
    }

    @Override
    public void save(DataWriter writer) {
        writer.writeBytes(this.encodedImageBytes);
    }

    @Override
    public Image getCollectionViewIcon() {
        return ImageResource.PHOTO_ALBUM_16.getFxImage();
    }

    @Override
    public GameUIController<?> makeEditorUI() {
        return this.cachedImage != null ? loadEditor(getGameInstance(), "edit-file-bs", new BitstreamImageController(getGameInstance()), this) : null;
    }

    @Override
    public PropertyList addToPropertyList(PropertyList propertyList) {
        propertyList = super.addToPropertyList(propertyList);
        propertyList.add("Image Loaded?", this.cachedImage != null);
        if (this.cachedImage != null) {
            propertyList.add("Image Width", getCachedWidth());
            propertyList.add("Image Height", getCachedHeight());
        }

        return propertyList;
    }

    @Override
    public void setupRightClickMenuItems(ContextMenu contextMenu) {
        super.setupRightClickMenuItems(contextMenu);

        MenuItem exportImage = new MenuItem("Export Image");
        contextMenu.getItems().add(exportImage);
        exportImage.setOnAction(event -> promptUserToExportImage());

        MenuItem importImage = new MenuItem("Import Image");
        contextMenu.getItems().add(importImage);
        importImage.setOnAction(event -> promptUserToImportImage());
    }

    /**
     * Gets the cached image width, if known, or -1 if there is no image width cached.
     */
    public int getCachedWidth() {
        return this.cachedImage != null ? this.cachedImage.getWidth() : -1;
    }

    /**
     * Gets the cached image height, if known, or -1 if there is no image height cached.
     */
    public int getCachedHeight() {
        return this.cachedImage != null ? this.cachedImage.getHeight() : -1;
    }

    /**
     * Obtains the image of the given width/height.
     * @param width the width of the image to obtain
     * @param height the height of the image to obtain
     * @return image
     */
    public BufferedImage getImage(int width, int height) {
        if (width <= 0)
            throw new IllegalArgumentException("Invalid width: " + width);
        if (height <= 0)
            throw new IllegalArgumentException("Invalid height: " + height);

        if (this.cachedImage == null || this.cachedImage.getWidth() != width || this.cachedImage.getHeight() != height)
            this.cachedImage = PsxMdecUtils.decodeBsImage(width, height, this.encodedImageBytes, MdecDecodeQuality.PSX, ChromaUpsample.NearestNeighbor);

        return this.cachedImage;
    }

    /**
     * Replaces the image represented by this object with a new one of the same size/dimensions.
     * Keeping the same dimensions is important because the image size is not stored in a .BS file. It must instead be provided to the game in some other manner, such as a hardcoded image size.
     * As such, this only images of the same dimensions as the original (if known) are accepted.
     * @param newImage the image to apply
     */
    public void replaceImage(BufferedImage newImage) {
        if (newImage == null)
            throw new NullPointerException("newImage");

        int newImageWidth = newImage.getWidth();
        int newImageHeight = newImage.getHeight();
        if (this.cachedImage != null && (this.cachedImage.getWidth() != newImageWidth || this.cachedImage.getHeight() != newImageHeight))
            throw new IllegalArgumentException("The new image's size/dimensions (" + newImageWidth + "x" + newImageHeight
                    + ") did not match the old image's dimensions! (" + getCachedWidth() + "x" + getCachedHeight() + ")");

        byte[] newImageBytes = PsxMdecUtils.encodeBsImage(this.encodedImageBytes, newImage);
        if (newImageBytes == null)
            throw new IllegalStateException("Failed to encode the image to a bitstream!");

        // Decode the image instead of using newImage directly, to ensure the preview is accurate.
        BufferedImage newDecodedImage = PsxMdecUtils.decodeBsImage(newImageWidth, newImageHeight, newImageBytes, MdecDecodeQuality.PSX, ChromaUpsample.NearestNeighbor);

        // This should happen last, so that if any exceptions occur, the image remains unchanged.
        this.cachedImage = newDecodedImage;
        this.encodedImageBytes = newImageBytes;
    }

    /**
     * Prompts the user to export the image.
     */
    public void promptUserToExportImage() {
        if (this.cachedImage == null) {
            FXUtils.showPopup(AlertType.ERROR, "Cannot export image.", "The dimensions of this image are unknown, so it cannot be exported.");
            return;
        }

        FileUtils.askUserToSaveImageFile(getGameInstance().getLogger(), getGameInstance(), this.cachedImage, getFileDisplayName());
    }

    /**
     * Prompts the user to import the image.
     * @return true iff the image was successfully imported
     */
    public boolean promptUserToImportImage() {
        BufferedImage image = FileUtils.askUserToOpenImageFile(getGameInstance().getLogger(), getGameInstance());
        if (image == null)
            return false;

        try {
            replaceImage(image);
            return true;
        } catch (Throwable th) {
            Utils.handleError(getLogger(), th, true, "The image could not be imported.");
            return false;
        }
    }
}
