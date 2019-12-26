package net.highwayfrogs.editor.file.map;

import javafx.scene.Node;
import javafx.scene.image.Image;
import lombok.AllArgsConstructor;
import lombok.Getter;
import net.highwayfrogs.editor.file.DummyFile;
import net.highwayfrogs.editor.file.GameFile;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.vlo.GameImage;
import net.highwayfrogs.editor.file.vlo.ImageFilterSettings;
import net.highwayfrogs.editor.file.vlo.ImageFilterSettings.ImageState;
import net.highwayfrogs.editor.file.vlo.ImageWorkHorse;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.gui.editor.SkyLandController;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

/**
 * Reads the sky land file.
 * Tile 3 - Left to right road, with vertical gray line on the right padding. 2 - Ground with dark blob. 1 - Light ground. Afterwords is the red start circle, with a blue background.
 * Created by Kneesnap on 2/15/2019.
 */
@Getter
public class SkyLand extends GameFile {
    private int xLength;
    private int yLength;
    private List<SkyLandTile> skyData = new ArrayList<>();

    @Override
    public Image getIcon() {
        return DummyFile.ICON;
    }

    @Override
    public Node makeEditor() {
        return loadEditor(new SkyLandController(), "skyland", this);
    }

    @Override
    public void load(DataReader reader) {
        this.xLength = reader.readUnsignedShortAsInt();
        this.yLength = reader.readUnsignedShortAsInt();
        for (int i = 0; i < (xLength * yLength); i++)
            skyData.add(new SkyLandTile(reader.readShort()));
    }

    @Override
    public void save(DataWriter writer) {
        writer.writeUnsignedShort(this.xLength);
        writer.writeUnsignedShort(this.yLength);
        for (SkyLandTile tile : getSkyData())
            writer.writeShort(tile.toShort());
    }

    /**
     * Gets the max sky land index.
     * @return maxIndex
     */
    public int getMaxIndex() {
        int max = -1;
        for (SkyLandTile tile : getSkyData())
            if (tile.getId() > max)
                max = tile.getId();
        return max;
    }

    /**
     * Gets the ids of textures used by sky land.
     */
    public short[] getSkyLandTextures() {
        int address = getConfig().getSkyLandTextureAddress();
        if (address <= 0)
            return null; // None, configuration was not set.

        DataReader reader = getConfig().getReader();
        reader.setIndex(address);

        short[] result = new short[getMaxIndex() + 1];
        for (int i = 0; i < result.length; i++)
            result[i] = reader.readShort();
        return result;
    }

    /**
     * Makes the image that appears under sky maps.
     */
    public BufferedImage makeImage() {
        short[] textures = getSkyLandTextures();
        if (textures == null)
            return null; // There's no image if we can't get the textures.

        int width = -1;
        int height = -1;
        BufferedImage[][] images = new BufferedImage[textures.length][];
        ImageFilterSettings settings = new ImageFilterSettings(ImageState.EXPORT).setTrimEdges(true).setAllowTransparency(false);
        for (int i = 0; i < textures.length; i++) {
            GameImage image = getMWD().getImageByTextureId(textures[i]);
            if (image == null)
                throw new RuntimeException("Failed to get image by texture id " + textures[i] + ".");
            BufferedImage base = image.toBufferedImage(settings);

            BufferedImage[] rotatedImages = new BufferedImage[SkyLandRotation.values().length];
            images[i] = rotatedImages;
            for (int j = 0; j < SkyLandRotation.values().length; j++)
                rotatedImages[j] = ImageWorkHorse.rotateImage(base, SkyLandRotation.values()[j].getAngle());

            if (width == -1 && height == -1) {
                width = image.getIngameWidth();
                height = image.getIngameHeight();
            }

            if (width != image.getIngameWidth() || height != image.getIngameHeight())
                throw new RuntimeException("Not all of the sky images matched width and height! [" + width + ", " + height + "] -> [" + image.getIngameWidth() + ", " + image.getIngameHeight() + "]");
        }

        BufferedImage finalImage = new BufferedImage(width * this.xLength, height * this.yLength, BufferedImage.TYPE_INT_ARGB);
        Graphics graphics = finalImage.createGraphics();
        for (int i = 0; i < this.skyData.size(); i++) {
            SkyLandTile tile = this.skyData.get(i);
            int x = (i % this.xLength);
            int y = (i / this.xLength);
            graphics.drawImage(images[tile.getId()][tile.getRotation().ordinal()], x * width, (this.yLength - y - 1) * height, width, height, null);
        }

        graphics.dispose();
        return finalImage;
    }

    @Getter
    public static final class SkyLandTile {
        private short id;
        private SkyLandRotation rotation;

        public SkyLandTile(short readShort) {
            this.id = (short) (readShort & 0x3FFF);
            this.rotation = SkyLandRotation.values()[((readShort & 0xC000) >> 14)];
            // & 0x3FFF -> Get texture id in txl_sky_land. Bits 0 -> 13.
            // & 0xC000 -> Texture Rotation [0->4]. Bits 14 + 15.
        }

        public short toShort() {
            return (short) ((this.id & 0x3FFF) | (this.rotation.ordinal() << 14));
        }
    }

    @Getter
    @AllArgsConstructor
    public enum SkyLandRotation {
        MODE_1(0D),
        MODE_2(270D),
        MODE_3(180D),
        MODE_4(90D);

        private final double angle;
    }
}
