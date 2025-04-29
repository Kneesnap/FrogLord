package net.highwayfrogs.editor.file.map.poly.polygon;

import javafx.scene.paint.Color;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.file.map.MAPFile;
import net.highwayfrogs.editor.file.map.view.TextureMap;
import net.highwayfrogs.editor.file.map.view.TextureMap.ShadingMode;
import net.highwayfrogs.editor.file.standard.psx.ByteUV;
import net.highwayfrogs.editor.file.standard.psx.PSXColorVector;
import net.highwayfrogs.editor.file.vlo.GameImage;
import net.highwayfrogs.editor.file.vlo.ImageFilterSettings;
import net.highwayfrogs.editor.file.vlo.ImageFilterSettings.ImageState;
import net.highwayfrogs.editor.file.vlo.VLOArchive;
import net.highwayfrogs.editor.games.sony.frogger.map.FroggerMapFile;
import net.highwayfrogs.editor.games.sony.frogger.map.mesh.FroggerMapPolygon;
import net.highwayfrogs.editor.games.sony.shared.SCByteTextureUV;
import net.highwayfrogs.editor.system.TexturedPoly;
import net.highwayfrogs.editor.utils.ColorUtils;
import net.highwayfrogs.editor.utils.Utils;
import net.highwayfrogs.editor.utils.data.reader.DataReader;
import net.highwayfrogs.editor.utils.data.writer.DataWriter;

import java.awt.image.BufferedImage;

/**
 * Represents PSX polygons with a texture.
 * Created by Kneesnap on 8/25/2018.
 */
@Getter
@Setter
public abstract class MAPPolyTexture extends MAPPolygon implements TexturedPoly {
    private static final ImageFilterSettings SHOW_SETTINGS = new ImageFilterSettings(ImageState.EXPORT).setTrimEdges(true);
    private short flags;
    private ByteUV[] uvs;
    private short textureId;
    private PSXColorVector[] colors;

    private static final int SHOW_SIZE = 150;

    public MAPPolyTexture(MAPPolygonType type, int verticeCount, int colorCount) {
        super(type, verticeCount);
        this.uvs = new ByteUV[verticeCount];
        this.colors = new PSXColorVector[colorCount];
    }

    @Override
    public void load(DataReader reader) {
        super.load(reader);

        if (getMapFile().getMapConfig().isOldMapTexturedPolyFormat()) {
            readEarlyVersion(reader);
        } else {
            readFinalVersion(reader);
        }
    }

    private void readEarlyVersion(DataReader reader) {
        for (int i = 0; i < this.colors.length; i++) {
            PSXColorVector vector = new PSXColorVector();
            vector.load(reader);
            this.colors[i] = vector;
        }

        this.textureId = reader.readShort();
        this.flags = reader.readShort();

        loadUV(0, reader);
        loadUV(1, reader);
        if (this.uvs.length == 3) {
            loadUV(2, reader);
            reader.skipShort(); // Padding.
        } else if (this.uvs.length == 4) {
            loadUV(3, reader); // Read out of order.
            loadUV(2, reader);
        } else {
            throw new RuntimeException("Cannot handle " + this.uvs.length + " uvs.");
        }
    }

    private void readFinalVersion(DataReader reader) {
        this.flags = reader.readShort();
        reader.skipShort(); // Padding

        loadUV(0, reader);
        reader.skipShort(); // Runtime clut-id.
        loadUV(1, reader);
        this.textureId = reader.readShort();

        if (this.uvs.length == 3) {
            loadUV(2, reader);
            reader.skipShort(); // Padding.
        } else if (this.uvs.length == 4) {
            loadUV(3, reader); // Read out of order.
            loadUV(2, reader);
        } else {
            throw new RuntimeException("Cannot handle " + this.uvs.length + " uvs.");
        }

        for (int i = 0; i < this.colors.length; i++) {
            PSXColorVector vector = new PSXColorVector();
            vector.load(reader);
            this.colors[i] = vector;
        }
    }

    @Override
    public void save(DataWriter writer) {
        super.save(writer);

        if (getMapFile().getMapConfig().isOldMapTexturedPolyFormat()) {
            writeEarlyVersion(writer);
        } else {
            writeFinalVersion(writer);
        }
    }

    private void writeEarlyVersion(DataWriter writer) {
        for (int i = 0; i < this.colors.length; i++)
            this.colors[i].save(writer);

        writer.writeShort(this.textureId);
        writer.writeShort(this.flags);
        this.uvs[0].save(writer);
        this.uvs[1].save(writer);

        if (this.uvs.length == 3) {
            this.uvs[2].save(writer);
            writer.writeShort((short) 0); // Padding.
        } else if (this.uvs.length == 4) {
            this.uvs[3].save(writer);
            this.uvs[2].save(writer);
        } else {
            throw new RuntimeException("Cannot handle " + this.uvs.length + " uvs.");
        }
    }

    private void writeFinalVersion(DataWriter writer) {
        writer.writeShort(this.flags);
        writer.writeNull(Constants.SHORT_SIZE);
        this.uvs[0].save(writer);
        writer.writeShort((short) 0); // Runtime value.
        this.uvs[1].save(writer);
        writer.writeShort(this.textureId);

        if (this.uvs.length == 3) {
            this.uvs[2].save(writer);
            writer.writeNull(Constants.SHORT_SIZE);
        } else if (this.uvs.length == 4) {
            this.uvs[3].save(writer); // Save out of order.
            this.uvs[2].save(writer);
        } else {
            throw new RuntimeException("Cannot save " + this.uvs.length + " uvs.");
        }

        for (PSXColorVector colorVector : this.colors)
            colorVector.save(writer);
    }


    /**
     * Makes the image used for shading.
     * @return shadeImage
     */
    public abstract BufferedImage makeShadeImage(TextureMap map, int width, int height, boolean useRaw);

    /**
     * Makes the image used for shading.
     * @return shadeImage
     */
    public BufferedImage makeShadeImage(TextureMap map, boolean useRaw) {
        return makeShadeImage(map, MAPFile.VERTEX_COLOR_IMAGE_SIZE, MAPFile.VERTEX_COLOR_IMAGE_SIZE, useRaw);
    }

    /**
     * Creates a texture which has shading applied.
     * @return shadedTexture
     */
    public BufferedImage makeShadedTexture(TextureMap map, BufferedImage applyTo) {
        BufferedImage normalImage = applyTo != null ? applyTo : getTreeNode(map).getImage();
        BufferedImage shadeImage = makeShadeImage(map, normalImage.getWidth(), normalImage.getHeight(), true);
        return makeShadedTexture(normalImage, shadeImage);
    }

    private void loadUV(int id, DataReader reader) {
        this.uvs[id] = new ByteUV();
        this.uvs[id].load(reader);
    }

    @Override
    public int getOrderId() {
        return getTextureId();
    }

    /**
     * Test if a flag is present.
     * @param flag The flag in question.
     * @return flagPresent
     */
    public boolean testFlag(PolyTextureFlag flag) {
        return (this.flags & flag.getFlag()) == flag.getFlag();
    }

    @Override
    public void performSwap() {
        if (this.uvs.length == 4) {
            ByteUV temp = this.uvs[3];
            this.uvs[3] = this.uvs[2];
            this.uvs[2] = temp;
        }
    }

    /**
     * Set a flag state.
     * @param flag     The flag to set.
     * @param newState The new flag state.
     */
    public void setFlag(PolyTextureFlag flag, boolean newState) {
        boolean currentState = testFlag(flag);
        if (currentState == newState)
            return; // Prevents the ^ operation from breaking the value.

        if (newState) {
            this.flags |= flag.getFlag();
        } else {
            this.flags ^= flag.getFlag();
        }
    }

    @Getter
    @AllArgsConstructor
    public enum PolyTextureFlag {
        SEMI_TRANSPARENT(Constants.BIT_FLAG_0), // setSemiTrans(true)
        ENVIRONMENT_IMAGE(Constants.BIT_FLAG_1), // Show the solid environment bitmap. (For instance, how water appears as a solid body, or sludge in the sewer levels.)
        MAX_ORDER_TABLE(Constants.BIT_FLAG_2); // Puts at the back of the order table. Either the very lowest rendering priority, or the very highest.

        private final int flag;
    }

    @Override
    public GameImage getGameImage(TextureMap map) {
        short globalTextureId = map.getRemap(getTextureId());
        GameImage image = map.getVloArchive() != null ? map.getVloArchive().getImageByTextureId(globalTextureId, false) : null;
        if (image == null) { // This is probably not going to give a useful texture generally. It was added for build 20 compatibility.
            image = map.getArchive().getImageByTextureId(globalTextureId);
            for (int i = 0; i < 100 && image == null; i++) {
                image = map.getArchive().getImageByTextureId(globalTextureId + i);
                if (image == null && i > 0)
                    image = map.getArchive().getImageByTextureId(globalTextureId - i);
            }

            if (image == null)
                return map.getArchive().getAllFiles(VLOArchive.class).get(0).getImages().get(0);
        }
        if (image == null) // New froglord = we want to just use a "unknown texture" placeholder.
            throw new RuntimeException("Could not find a texture with the id: " + getTextureId() + "/" + globalTextureId + ".");
        return image;
    }

    @Override
    public boolean isOverlay(TextureMap map) {
        if (map.getMode() == ShadingMode.OVERLAY_SHADING)
            return true;

        if (map.getMode() != ShadingMode.MIXED_SHADING)
            return false;

        GameImage image = getGameImage(map);
        long combinedArea = map.getMapTextureList().get(getTextureId()).size() * (image.getFullWidth() * image.getFullHeight());
        return combinedArea >= ((long) map.getWidth() * map.getHeight() / 8); // Test if it's too frequent.
    }

    @Override
    public FroggerMapPolygon convertToNewFormat(FroggerMapFile mapFile) {
        FroggerMapPolygon polygon = super.convertToNewFormat(mapFile);
        if (polygon.getColors().length != this.colors.length)
            throw new RuntimeException("Invalid number of colors! Expected " + this.colors.length + ", got " + polygon.getColors().length + "! (" + getType() + "/" + polygon.getPolygonType() + ")");
        if (polygon.getTextureUvs().length != this.uvs.length)
            throw new RuntimeException("Invalid number of UVs! Expected " + this.uvs.length + ", got " + polygon.getTextureUvs().length + "! (" + getType() + "/" + polygon.getPolygonType() + ")");

        polygon.setFlagMask(FroggerMapPolygon.FLAG_SEMI_TRANSPARENT, (this.flags & FroggerMapPolygon.FLAG_SEMI_TRANSPARENT) != 0);
        polygon.setFlagMask(FroggerMapPolygon.FLAG_ENVIRONMENT_MAPPED, (this.flags & FroggerMapPolygon.FLAG_ENVIRONMENT_MAPPED) != 0);
        polygon.setFlagMask(FroggerMapPolygon.FLAG_MAX_ORDER_TABLE, (this.flags & FroggerMapPolygon.FLAG_MAX_ORDER_TABLE) != 0);
        Utils.verify(this.flags == polygon.getFlags(), "Flag conversion mismatch! (%d, %d)", this.flags, polygon.getFlags());

        polygon.setTextureId(this.textureId);
        for (int i = 0; i < this.colors.length; i++)
            polygon.getColors()[i] = this.colors[i].toCVector(null);

        performSwap();
        for (int i = 0; i < this.uvs.length; i++)
            polygon.getTextureUvs()[i] = new SCByteTextureUV(this.uvs[i].getU(), this.uvs[i].getV());
        performSwap();

        return polygon;
    }

    /**
     * Creates a texture which has shading applied.
     * @return shadedTexture
     */
    public static BufferedImage makeShadedTexture(BufferedImage applyImage, BufferedImage shadeImage) {
        BufferedImage newImage = new BufferedImage(applyImage.getWidth(), applyImage.getHeight(), BufferedImage.TYPE_INT_ARGB);
        for (int x = 0; x < newImage.getWidth(); x++) {
            for (int y = 0; y < newImage.getHeight(); y++) {
                int rgb = applyImage.getRGB(x, y);
                int overlay = shadeImage.getRGB(x, y);
                int alpha = (rgb & 0xFF000000) >> 24;
                int red = (int) (((double) ColorUtils.getRedInt(overlay) / 127D) * (double) ColorUtils.getRedInt(rgb));
                int green = (int) (((double) ColorUtils.getGreenInt(overlay) / 127D) * (double) ColorUtils.getGreenInt(rgb));
                int blue = (int) (((double) ColorUtils.getBlueInt(overlay) / 127D) * (double) ColorUtils.getBlueInt(rgb));
                newImage.setRGB(x, y, ((alpha << 24) | ((red & 0xFF) << 16) | ((green & 0xFF) << 8) | (blue & 0xFF)));
            }
        }

        return newImage;
    }

    /**
     * Creates a texture which has flat shading applied.
     * @return shadedTexture
     */
    public static BufferedImage makeFlatShadedTexture(BufferedImage applyImage, Color color, boolean fullRange) {
        int overlay = ColorUtils.toRGB(color);
        BufferedImage newImage = new BufferedImage(applyImage.getWidth(), applyImage.getHeight(), BufferedImage.TYPE_INT_ARGB);
        for (int x = 0; x < newImage.getWidth(); x++) {
            for (int y = 0; y < newImage.getHeight(); y++) {
                int rgb = applyImage.getRGB(x, y);
                int alpha = (rgb & 0xFF000000) >> 24;
                int red = (int) (((double) ColorUtils.getRedInt(overlay) / (fullRange ? 255D : 127D)) * (double) ColorUtils.getRedInt(rgb));
                int green = (int) (((double) ColorUtils.getGreenInt(overlay) / (fullRange ? 255D : 127D)) * (double) ColorUtils.getGreenInt(rgb));
                int blue = (int) (((double) ColorUtils.getBlueInt(overlay) / (fullRange ? 255D : 127D)) * (double) ColorUtils.getBlueInt(rgb));
                newImage.setRGB(x, y, ((alpha << 24) | ((red & 0xFF) << 16) | ((green & 0xFF) << 8) | (blue & 0xFF)));
            }
        }

        return newImage;
    }
}