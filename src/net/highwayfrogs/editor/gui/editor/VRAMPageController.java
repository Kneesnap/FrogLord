package net.highwayfrogs.editor.gui.editor;

import javafx.fxml.Initializable;
import net.highwayfrogs.editor.file.vlo.GameImage;
import net.highwayfrogs.editor.file.vlo.ImageFilterSettings;
import net.highwayfrogs.editor.file.vlo.ImageFilterSettings.ImageState;
import net.highwayfrogs.editor.file.vlo.VLOArchive;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.net.URL;
import java.util.ResourceBundle;

/**
 * Allows editing the arrangement of a VRAM page.
 * Goals:
 * - Visualize vRam Layout.
 * - Allow changing vRam layout.
 * Interface: Image: Where you can click on images inside of it, to select / show data for.
 * Dragging would be nice.
 * Should be scrollable vertically, as it's almost always 256 wide, but super long.
 * Created by Kneesnap on 12/2/2018.
 */
public class VRAMPageController implements Initializable {

    private VLOArchive vloArchive;

    private static final ImageFilterSettings SETTINGS = new ImageFilterSettings(ImageState.EXPORT);

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        updateDisplay();
    }

    private void updateDisplay() {

    }

    /**
     * Create a BufferedImage which effectively mirrors how Frogger will structure VRAM in-game.
     * @param vloArchive The archive to make an image of.
     * @return vramImage
     */
    public static BufferedImage makeVRAMImage(VLOArchive vloArchive) {
        int maxWidth = vloArchive.getImages().stream().mapToInt(img -> img.getVramX() + img.getFullWidth()).max().orElse(0);
        int maxHeight = vloArchive.getImages().stream().mapToInt(img -> img.getVramY() + img.getFullHeight()).max().orElse(0);

        BufferedImage vramImage = new BufferedImage(maxWidth, maxHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = vramImage.createGraphics();

        for (GameImage image : vloArchive.getImages())
            graphics.drawImage(image.toBufferedImage(SETTINGS), null, image.getVramX(), image.getVramY());

        graphics.dispose();
        return vramImage;
    }
}
