package net.highwayfrogs.editor.file.vlo;

import javafx.scene.Node;
import javafx.scene.image.Image;
import javafx.scene.layout.AnchorPane;
import lombok.Getter;
import lombok.SneakyThrows;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.file.MWIFile.FileEntry;
import net.highwayfrogs.editor.file.WADFile;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.vlo.ImageFilterSettings.ImageState;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.sony.SCGameFile.SCSharedGameFile;
import net.highwayfrogs.editor.games.sony.SCGameInstance;
import net.highwayfrogs.editor.gui.GUIMain;
import net.highwayfrogs.editor.gui.MainController;
import net.highwayfrogs.editor.gui.SelectionMenu;
import net.highwayfrogs.editor.gui.editor.VLOController;
import net.highwayfrogs.editor.system.Tuple2;
import net.highwayfrogs.editor.utils.Utils;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * VLOArchive - Image archive format created by VorgPC/Vorg2.
 * Images are ordered in descending size. (Largest area -> smallest area).
 * ABGR 8888 = 24 + 8.
 * Created by Kneesnap on 8/17/2018.
 */
@Getter
public class VLOArchive extends SCSharedGameFile {
    private final List<GameImage> images = new ArrayList<>();
    private final List<ClutEntry> clutEntries = new ArrayList<>();
    private boolean psxMode;

    public static final String PC_SIGNATURE = "2GRP";
    public static final String PSX_SIGNATURE = "2GRV";
    private static final int SIGNATURE_LENGTH = 4;

    private static final int IMAGE_INFO_BYTES = 24;
    private static final int HEADER_SIZE = SIGNATURE_LENGTH + (2 * Constants.INTEGER_SIZE);
    private static final int PSX_HEADER_SIZE = HEADER_SIZE + (2 * Constants.INTEGER_SIZE);
    public static final Image ICON = loadIcon("image");
    public static final ImageFilterSettings ICON_EXPORT = new ImageFilterSettings(ImageState.EXPORT);
    public static final ImageFilterSettings VRAM_EXPORT_NO_SCRUNCH = new ImageFilterSettings(ImageState.EXPORT);

    public VLOArchive(SCGameInstance instance) {
        super(instance);
    }

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
        int imageHeaderPointer = writer.writeNullPointer();

        int clutHeaderPointer = -1;
        if (isPsxMode()) {
            writer.writeInt(this.clutEntries.size());
            clutHeaderPointer = writer.writeNullPointer(); // This will be written later.
        }

        writer.writeAddressTo(imageHeaderPointer);
        this.images.forEach(image -> image.save(writer));
        if (isPsxMode()) { // Add room for clut setup data.
            writer.writeAddressTo(clutHeaderPointer);
            this.clutEntries.forEach(entry -> entry.save(writer));
        }

        this.images.forEach(image -> image.saveExtra(writer));
        if (isPsxMode())
            this.clutEntries.forEach(entry -> entry.saveExtra(writer));
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
        return loadEditor(new VLOController(getGameInstance()), "vlo", this);
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
        ImageIO.write(makeVRAMImage(), "png", new File(GUIMain.getWorkingDirectory(), Utils.stripExtension(fileEntry.getDisplayName()) + ".png"));
        System.out.println("Exported VRAM Image.");
    }

    @Override
    public void handleWadEdit(WADFile parent) {
        MainController.MAIN_WINDOW.openEditor(MainController.MAIN_WINDOW.getCurrentFilesList(), this);
        ((VLOController) MainController.getCurrentController()).setParentWad(parent);
    }

    @Override
    public List<Tuple2<String, Object>> createPropertyList() {
        List<Tuple2<String, Object>> list = super.createPropertyList();
        list.add(new Tuple2<>("Images", getImages().size()));
        list.add(new Tuple2<>("PS1 VLO", isPsxMode()));
        return list;
    }

    /**
     * Get an image that holds a specific vram coordinate.
     */
    public GameImage getImage(double x, double y) {
        for (GameImage image : getImages())
            if (image.contains(x * image.getWidthMultiplier(), y))
                return image;
        return null;
    }

    /**
     * Gets an image by the given texture ID.
     * @param textureId The texture ID to get.
     * @return gameImage
     */
    public GameImage getImageByTextureId(int textureId) {
        return getImageByTextureId(textureId, false);
    }

    /**
     * Gets an image by the given texture ID.
     * @param textureId The texture ID to get.
     * @return gameImage
     */
    public GameImage getImageByTextureId(int textureId, boolean errorIfFail) {
        for (GameImage testImage : getImages())
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
    public GameImage getGlobalTexture(int textureId) {
        GameImage foundImage = getImageByTextureId(textureId, false);
        return foundImage != null ? foundImage : getArchive().getImageByTextureId(textureId);
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
                image -> image != null ? "#" + image.getLocalImageID() + " (" + image.getTextureId() + ")" : "No Image",
                image -> image.toFXImage(ICON_EXPORT));
    }

    /**
     * Create a BufferedImage which effectively mirrors how Frogger will layout this VLO in memory.
     * @return vramImage
     */
    public BufferedImage makeVRAMImage() {
        return makeVRAMImage(null);
    }

    private int getVramWidth() {
        return (isPsxMode() ? GameImage.PSX_X_PAGES * GameImage.PSX_FULL_PAGE_WIDTH : GameImage.PC_PAGE_WIDTH);
    }

    private int getVramHeight() {
        return isPsxMode() ? (GameImage.PSX_PAGE_HEIGHT * GameImage.PSX_Y_PAGES) : (GameImage.PC_PAGE_HEIGHT * GameImage.TOTAL_PAGES);
    }

    /**
     * Create a BufferedImage which effectively mirrors how Frogger will layout this VLO in memory.
     * One pixel maps to one byte of VRAM.
     * On the PS1, multiple pixels can be stored in a single byte, so there is a loss of quality.
     * @param vramImage The image to write the data onto. If the image is not the right dimensions, it will make a new image and use that one.
     * @return vramImage
     */
    public BufferedImage makeVRAMImage(BufferedImage vramImage) {
        int calcWidth = getVramWidth();
        int calcHeight = getVramHeight();
        if (vramImage == null || (calcWidth != vramImage.getWidth() || calcHeight != vramImage.getHeight()))
            vramImage = new BufferedImage(calcWidth, calcHeight, BufferedImage.TYPE_INT_ARGB);

        // Draw on image.
        Graphics2D graphics = vramImage.createGraphics();

        // Fill background.
        graphics.setColor(Constants.COLOR_TURQUOISE);
        graphics.fillRect(0, 0, vramImage.getWidth(), vramImage.getHeight());

        final int psxMultiple = (GameImage.PSX_FULL_PAGE_WIDTH / GameImage.PSX_PAGE_WIDTH);
        if (isPsxMode()) {
            // Draw screen-buffer as a different color.
            graphics.setColor(Constants.COLOR_DEEP_GREEN); // Screen buffer.
            graphics.fillRect(0, 0, 320 * psxMultiple, 240);
            graphics.setColor(Constants.COLOR_DARK_YELLOW); // Next frame.
            graphics.fillRect(0, 240, 320 * psxMultiple, 240);

            // Draw cluts.
            for (ClutEntry clutEntry : getClutEntries())
                graphics.drawImage(clutEntry.makeImage(), null, clutEntry.getClutRect().getX() * psxMultiple, clutEntry.getClutRect().getY());
        }

        // Draw images.
        for (GameImage image : getImages())
            graphics.drawImage(image.toBufferedImage(VRAM_EXPORT_NO_SCRUNCH), null, image.getVramX() * (isPsxMode() ? (psxMultiple / image.getWidthMultiplier()) : 1), image.getVramY());

        graphics.dispose(); // Cleanup.
        return vramImage;
    }
}