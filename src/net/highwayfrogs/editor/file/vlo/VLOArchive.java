package net.highwayfrogs.editor.file.vlo;

import javafx.scene.Node;
import javafx.scene.image.Image;
import javafx.scene.layout.AnchorPane;
import lombok.Getter;
import lombok.SneakyThrows;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.file.GameFile;
import net.highwayfrogs.editor.file.MWIFile.FileEntry;
import net.highwayfrogs.editor.file.WADFile;
import net.highwayfrogs.editor.file.WADFile.WADEntry;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.vlo.ImageFilterSettings.ImageState;
import net.highwayfrogs.editor.file.writer.DataWriter;
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
    private static final int PSX_HEADER_SIZE = HEADER_SIZE + (2 * Constants.INTEGER_SIZE);
    public static final int TYPE_ID = 1;
    public static final int WAD_TYPE = 0;
    public static final Image ICON = loadIcon("image");
    public static final ImageFilterSettings ICON_EXPORT = new ImageFilterSettings(ImageState.EXPORT);
    public static final ImageFilterSettings VRAM_EXPORT = new ImageFilterSettings(ImageState.EXPORT).setAllowScrunch(true);

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
        ImageIO.write(makeVRAMImage(), "png", new File(GUIMain.getWorkingDirectory(), Utils.stripExtension(fileEntry.getDisplayName()) + ".png"));
        System.out.println("Exported VRAM Image.");
    }

    @Override
    public void handleWadEdit(WADFile parent) {
        MainController.MAIN_WINDOW.openEditor(MainController.MAIN_WINDOW.getCurrentFilesList(), this);
        ((VLOController) MainController.getCurrentController()).setParentWad(parent);
    }

    @Override
    public List<Tuple2<String, String>> showWadProperties(WADFile wadFile, WADEntry wadEntry) {
        List<Tuple2<String, String>> list = new ArrayList<>();
        list.add(new Tuple2<>("Images", String.valueOf(getImages().size())));
        list.add(new Tuple2<>("PS1 VLO", String.valueOf(isPsxMode())));
        return list;
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
        return getImageByTextureId(textureId, true);
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
        return (isPsxMode() ? GameImage.PSX_X_PAGES * GameImage.PSX_PAGE_WIDTH : GameImage.PC_PAGE_WIDTH);
    }

    private int getVramHeight() {
        if (isPsxMode())
            return GameImage.PSX_Y_PAGES * GameImage.PSX_PAGE_HEIGHT;

        int maxHeight = 0;
        for (GameImage testImage : getImages())
            maxHeight = Math.max(maxHeight, testImage.getVramY() + testImage.getFullHeight());
        return GameImage.PC_PAGE_HEIGHT * ((maxHeight / GameImage.PC_PAGE_HEIGHT) + (maxHeight % GameImage.PC_PAGE_HEIGHT != 0 ? 1 : 0));

    }

    /**
     * Create a BufferedImage which effectively mirrors how Frogger will layout this VLO in memory.
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

        // Draw screen-buffer as a different color.
        if (isPsxMode()) {
            graphics.setColor(Constants.COLOR_DEEP_GREEN); // Screen buffer.
            graphics.fillRect(0, 0, 320, 240);
            graphics.setColor(Constants.COLOR_DARK_YELLOW); // Next frame.
            graphics.fillRect(0, 240, 320, 240);
        }

        // Draw cluts.
        if (isPsxMode())
            for (ClutEntry clutEntry : getClutEntries())
                graphics.drawImage(clutEntry.makeImage(), null, clutEntry.getClutRect().getX(), clutEntry.getClutRect().getY());

        // Create outlines. TODO: This should probably be done by the editor instead, by splitting up the image into chunks.
        graphics.setColor(Constants.COLOR_TAN);
        if (isPsxMode()) {
            for (int yLine = 0; yLine <= GameImage.PSX_Y_PAGES; yLine++) {
                int drawY = GameImage.PSX_PAGE_HEIGHT * yLine;
                graphics.drawLine(0, drawY, vramImage.getWidth(), drawY);
            }

            for (int xLine = 0; xLine <= GameImage.PSX_X_PAGES; xLine++) {
                int drawX = GameImage.PSX_PAGE_WIDTH * xLine;
                graphics.drawLine(drawX, 0, drawX, vramImage.getHeight());
            }

        } else {
            for (int yLine = 0; yLine < vramImage.getHeight(); yLine += GameImage.PC_PAGE_HEIGHT)
                graphics.drawLine(0, yLine, vramImage.getWidth(), yLine);
        }

        for (GameImage image : getImages())
            graphics.drawImage(image.toBufferedImage(VRAM_EXPORT), null, (image.getVramX() / image.getWidthMultiplier()), image.getVramY()); //TODO: Is this the secret to U calculation?

        graphics.dispose();
        return vramImage;
    }
}
