package net.highwayfrogs.editor.file.vlo;

import javafx.scene.Node;
import javafx.scene.image.Image;
import javafx.scene.layout.AnchorPane;
import lombok.Getter;
import lombok.SneakyThrows;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.Utils;
import net.highwayfrogs.editor.file.GameFile;
import net.highwayfrogs.editor.file.MWIFile.FileEntry;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.vlo.ImageFilterSettings.ImageState;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.gui.GUIMain;
import net.highwayfrogs.editor.gui.SelectionMenu;
import net.highwayfrogs.editor.gui.editor.VLOController;
import net.highwayfrogs.editor.gui.editor.VRAMPageController;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/**
 * VLOArchive - Image archive format created by VorgPC/Vorg2.
 * ABGR 8888 = 24 + 8.
 * Created by Kneesnap on 8/17/2018.
 */
@Getter
public class VLOArchive extends GameFile {
    private List<GameImage> images = new ArrayList<>();
    private List<ClutEntry> clutEntries = new ArrayList<>();
    private boolean psxMode;

    private static final String PC_SIGNATURE = "2GRP";
    private static final String PSX_SIGNATURE = "2GRV";
    private static final int SIGNATURE_LENGTH = 4;

    private static final int IMAGE_INFO_BYTES = 24;
    private static final int HEADER_SIZE = SIGNATURE_LENGTH + (2 * Constants.INTEGER_SIZE);
    public static final int TYPE_ID = 1;
    public static final int WAD_TYPE = 0;
    public static final Image ICON = loadIcon("image");
    public static final ImageFilterSettings ICON_EXPORT = new ImageFilterSettings(ImageState.EXPORT).setAllowFlip(true);

    @Override
    public void load(DataReader reader) {
        String readSignature = reader.readString(SIGNATURE_LENGTH);
        if (readSignature.equals(PSX_SIGNATURE)) {
            this.psxMode = true;
        } else {
            Utils.verify(readSignature.equals(PC_SIGNATURE), "Invalid VLO signature: %s.", readSignature);
        }

        int fileCount = reader.readInt();
        int textureOffset = reader.readInt();

        // Load clut data.
        if (isPsxMode()) { // GRV file has clut data.
            int clutCount = reader.readInt();
            int clutOffset = reader.readInt();

            reader.jumpTemp(clutOffset);
            for (int i = 0; i < clutCount; i++) {
                ClutEntry clut = new ClutEntry();
                clut.load(reader);
                clutEntries.add(clut);
            }

            reader.jumpReturn();
        }

        // Load image data.
        reader.jumpTemp(textureOffset);
        for (int i = 0; i < fileCount; i++) {
            GameImage image = new GameImage(this);
            image.load(reader);
            this.images.add(image);
        }
        reader.jumpReturn();
    }

    @Override
    public void save(DataWriter writer) {
        int imageCount = getImages().size();
        writer.writeStringBytes(isPsxMode() ? PSX_SIGNATURE : PC_SIGNATURE);
        writer.writeInt(imageCount);
        writer.writeInt(writer.getIndex() + ((isPsxMode() ? 3 : 1) * Constants.INTEGER_SIZE)); // Offset to the VLO table info.

        int clutFirstSetupIndex = -1;
        if (isPsxMode()) {
            writer.writeInt(this.clutEntries.size());
            clutFirstSetupIndex = writer.getIndex();
            writer.writeNull(Constants.INTEGER_SIZE); // This will be written later.
        }


        AtomicInteger imageBytesOffset = new AtomicInteger((IMAGE_INFO_BYTES * imageCount) + HEADER_SIZE);
        if (isPsxMode()) // Add room for clut setup data.
            imageBytesOffset.addAndGet(getClutEntries().stream().mapToInt(ClutEntry::getByteSize).sum());

        for (GameImage image : getImages())
            image.save(writer, imageBytesOffset);

        // Save CLUT data. CLUT should be saved after images are saved, because changed images will generate a new CLUT.
        if (isPsxMode()) {
            writer.jumpTemp(clutFirstSetupIndex);
            writer.writeInt(imageBytesOffset.get());
            writer.jumpReturn();

            for (ClutEntry entry : clutEntries)
                entry.save(writer, imageBytesOffset);
        }
    }

    /**
     * Export all images in this VLO archive.
     */
    public void exportAllImages(File directory, ImageFilterSettings settings) {
        try {
            for (int i = 0; i < getImages().size(); i++) {
                File output = new File(directory, i + ".png");
                ImageIO.write(getImages().get(i).toBufferedImage(settings), "png", output);
                System.out.println("Exported image #" + i + ".");
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    @Override
    public Image getIcon() {
        return ICON;
    }

    @Override
    public Node makeEditor() {
        return loadEditor(new VLOController(), "vlo", this);
    }

    @Override
    public void setupEditor(AnchorPane editorPane, Node node) {
        super.setupEditor(editorPane, node);

        AnchorPane pane = (AnchorPane) node;
        editorPane.setMaxHeight(pane.getMinHeight()); // Restricts the height of this editor, since there's nothing beyond the editing area.
    }

    @Override
    @SneakyThrows
    public void exportAlternateFormat(FileEntry fileEntry) {
        BufferedImage image = VRAMPageController.makeVRAMImage(this);
        ImageIO.write(image, "png", new File(GUIMain.getWorkingDirectory(), Utils.stripExtension(fileEntry.getDisplayName()) + ".png"));
        System.out.println("Exported VRAM Image.");
    }

    /**
     * Get an image that holds a specific vram coordinate.
     */
    public GameImage getImage(double x, double y) {
        for (GameImage image : getImages())
            if (image.contains(x, y))
                return image;
        return null;
    }

    /**
     * Gets an image by the given texture ID.
     * @param textureId The texture ID to get.
     * @return gameImage
     */
    public GameImage getImageByTextureId(int textureId) {
        for (GameImage testImage : getImages())
            if (testImage.getTextureId() == textureId)
                return testImage;

        throw new RuntimeException("Could not find a texture with the id: " + textureId + ".");
    }

    /**
     * Select a VLO image
     * @param handler   The handler for when the VLO is determined.
     * @param allowNull Are null VLOs allowed?
     */
    public void promptImageSelection(Consumer<GameImage> handler, boolean allowNull) {
        List<GameImage> allImages = new ArrayList<>(getImages());

        if (allowNull)
            allImages.add(0, null);

        SelectionMenu.promptSelection("Select an image.", handler, allImages,
                image -> image != null ? "#" + getImages().indexOf(image) + " (" + image.getTextureId() + ")" : "No Image",
                image -> SelectionMenu.makeIcon(image.toBufferedImage(ICON_EXPORT)));
    }
}
