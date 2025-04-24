package net.highwayfrogs.editor.games.sony.frogger.file;

import javafx.scene.image.Image;
import lombok.AllArgsConstructor;
import lombok.Getter;
import net.highwayfrogs.editor.file.vlo.GameImage;
import net.highwayfrogs.editor.file.vlo.ImageFilterSettings;
import net.highwayfrogs.editor.file.vlo.ImageFilterSettings.ImageState;
import net.highwayfrogs.editor.file.vlo.ImageWorkHorse;
import net.highwayfrogs.editor.games.sony.SCGameFile;
import net.highwayfrogs.editor.games.sony.frogger.FroggerConfig;
import net.highwayfrogs.editor.games.sony.frogger.FroggerGameInstance;
import net.highwayfrogs.editor.games.sony.frogger.ui.SkyLandController;
import net.highwayfrogs.editor.gui.ImageResource;
import net.highwayfrogs.editor.utils.data.reader.DataReader;
import net.highwayfrogs.editor.utils.data.writer.DataWriter;

import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * Reads the sky land tile map.
 * TODO: Implement this to make it show up when viewing sky levels.
 * TODO: Does the bit format differ slightly for older versions?
 * This looks buggy in prototype builds. However, it has been confirmed to load as it appears in-game up to the build PSX Alpha.
 * Created by Kneesnap on 2/15/2019.
 */
@Getter
public class FroggerSkyLand extends SCGameFile<FroggerGameInstance> {
    private int xLength;
    private int yLength;
    private short[][] tileMap;

    public FroggerSkyLand(FroggerGameInstance instance) {
        super(instance);
    }

    @Override
    public FroggerConfig getConfig() {
        return (FroggerConfig) super.getConfig();
    }

    @Override
    public Image getCollectionViewIcon() {
        return ImageResource.PHOTO_ALBUM_32.getFxImage();
    }

    @Override
    public SkyLandController makeEditorUI() {
        return loadEditor(getGameInstance(), "edit-file-sky-land", new SkyLandController(getGameInstance()), this);
    }

    @Override
    public void load(DataReader reader) {
        this.xLength = reader.readUnsignedShortAsInt();
        this.yLength = reader.readUnsignedShortAsInt();
        this.tileMap = new short[this.yLength][this.xLength];
        for (int y = 0; y < this.yLength; y++)
            for (int x = 0; x < this.xLength; x++)
                this.tileMap[y][x] = reader.readShort();
    }

    @Override
    public void save(DataWriter writer) {
        writer.writeUnsignedShort(this.xLength);
        writer.writeUnsignedShort(this.yLength);
        for (int y = 0; y < this.yLength; y++)
            for (int x = 0; x < this.xLength; x++)
                writer.writeShort(this.tileMap[y][x]);
    }

    /**
     * Gets the ids of textures used by sky land.
     */
    public short[] getSkyLandTextures() {
        if (getGameInstance().getSkyLandTextureRemap().getTextureIds().isEmpty())
            return null; // None, configuration was not set.

        // Convert from a list to an array.
        short[] result = new short[getGameInstance().getSkyLandTextureRemap().getTextureIds().size()];
        for (int i = 0; i < result.length; i++)
            result[i] = getGameInstance().getSkyLandTextureRemap().getTextureIds().get(i);
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
            GameImage image = getArchive().getImageByTextureId(textures[i]);
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
        }

        BufferedImage finalImage = new BufferedImage(width * this.xLength, height * this.yLength, BufferedImage.TYPE_INT_ARGB);
        Graphics graphics = finalImage.createGraphics();
        for (int y = 0; y < this.yLength; y++) {
            for (int x = 0; x < this.xLength; x++) {
                short packedTileData = this.tileMap[y][x];
                BufferedImage tileImage = images[getLocalTextureId(packedTileData)][getTextureRotation(packedTileData).ordinal()];
                graphics.drawImage(tileImage, x * width, (this.yLength - y - 1) * height, width, height, null);
            }
        }

        graphics.dispose();
        return finalImage;
    }


    /**
     * Gets the local (non-remapped) texture ID from the packed value.
     * @param packedValue The packed value to get the texture ID from.
     * @return localTextureId
     */
    public static short getLocalTextureId(short packedValue) {
        // & 0x3FFF -> Get texture id in txl_sky_land. Bits 0 -> 13.
        return (short) (packedValue & 0x3FFF);
    }

    /**
     * Gets texture rotation from the packed value.
     * @param packedValue The packed value to get the texture rotation from.
     * @return textureRotation
     */
    public static SkyLandRotation getTextureRotation(short packedValue) {
        // & 0xC000 -> Texture Rotation [0->4]. Bits 14 + 15.
        return SkyLandRotation.values()[((packedValue & 0xC000) >> 14)];
    }

    /**
     * Creates a packed sky land tile short.
     * @param localTextureId the texture id to apply
     * @param rotation the texture rotation to apply
     * @return packedSkyLandTileShort
     */
    public static short packSkyLandTile(short localTextureId, SkyLandRotation rotation) {
        if (rotation == null)
            throw new NullPointerException("rotation");

        return (short) ((localTextureId & 0x3FFF) | (rotation.ordinal() << 14));
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