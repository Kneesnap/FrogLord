package net.highwayfrogs.editor.file.mof.prims;

import lombok.Getter;
import lombok.Setter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.file.map.MAPFile;
import net.highwayfrogs.editor.file.map.poly.polygon.MAPPolyGT;
import net.highwayfrogs.editor.file.map.poly.polygon.MAPPolyTexture;
import net.highwayfrogs.editor.file.map.poly.polygon.MAPPolygon;
import net.highwayfrogs.editor.file.map.view.FrogMesh;
import net.highwayfrogs.editor.file.map.view.TextureMap;
import net.highwayfrogs.editor.file.map.view.TextureMap.ShaderMode;
import net.highwayfrogs.editor.file.mof.MOFPart;
import net.highwayfrogs.editor.file.mof.view.MOFMesh;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.standard.psx.ByteUV;
import net.highwayfrogs.editor.file.vlo.GameImage;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.system.TexturedPoly;
import net.highwayfrogs.editor.utils.Utils;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.math.BigInteger;


/**
 * Represents a MOF polygon with a texture.
 * Created by Kneesnap on 1/1/2019.
 */
@Getter
@Setter
public class MOFPolyTexture extends MOFPolygon implements TexturedPoly {
    private ByteUV[] uvs;
    private short imageId;

    private transient short viewImageId = -1; // The image id while this MOF is being viewed, so things like animations happen.

    public static final int FLAG_SEMI_TRANSPARENT = Constants.BIT_FLAG_0; // setSemiTrans(true)
    public static final int FLAG_ENVIRONMENT_IMAGE = Constants.BIT_FLAG_1; // Show the solid environment bitmap. (For instance, how water appears as a solid body, or sludge in the sewer levels.)
    public static final int FLAG_MAX_ORDER_TABLE = Constants.BIT_FLAG_2; // Puts at the back of the order table. Either the very lowest rendering priority, or the very highest.

    // These are run-time-only it seems. They get applied from the anim section.
    public static final int FLAG_ANIMATED_UV = Constants.BIT_FLAG_3; // Poly has an associated map animation using UV animation.
    public static final int FLAG_ANIMATED_TEXTURE = Constants.BIT_FLAG_4; // Poly has an associated map animation using cel list animation.

    public MOFPolyTexture(MOFPart parent, MOFPrimType type, int verticeCount, int normalCount) {
        super(parent, type, verticeCount, normalCount, 0);
        this.uvs = new ByteUV[verticeCount];
    }

    @Override
    public void onLoad(DataReader reader) {
        loadUV(0, reader);
        short clutId = reader.readShort();
        if (clutId != 0)
            throw new RuntimeException("MOFPolyTexture had clut id which was not zero! (" + clutId + ").");

        loadUV(1, reader);
        short textureId = reader.readShort();
        if (textureId != 0)
            throw new RuntimeException("MOFPolyTexture had texture id which was not zero! (" + textureId + ").");

        for (int i = 2; i < this.uvs.length; i++)
            loadUV(i, reader);

        this.imageId = reader.readShort();
    }

    @Override
    public void onSave(DataWriter writer) {
        super.onSave(writer);

        this.uvs[0].save(writer);
        writer.writeShort((short) 0);
        this.uvs[1].save(writer);
        writer.writeShort((short) 0);

        for (int i = 2; i < this.uvs.length; i++)
            this.uvs[i].save(writer);

        writer.writeShort(this.imageId);
    }

    public void loadUV(int id, DataReader reader) {
        this.uvs[id] = new ByteUV();
        this.uvs[id].load(reader);
    }

    /**
     * Get the nth obj UV string.
     * @param index The index to get.
     * @return objUvString
     */
    public String getObjUVString(int index) {
        return this.uvs[index].toObjTextureString();
    }

    @Override
    public int getOrderId() {
        return getImageId();
    }

    @Override
    public void performSwap() {
        if (getUvs().length == MAPPolygon.QUAD_SIZE) {
            ByteUV temp = this.uvs[2];
            this.uvs[2] = this.uvs[3];
            this.uvs[3] = temp;
        }
    }

    @Override
    public BufferedImage makeTexture(TextureMap map) {
        if (map.getMode() == ShaderMode.NO_SHADING) {
            return getGameImage(map).toBufferedImage(map.getDisplaySettings());
        } else if (map.getMode() == ShaderMode.OVERLAY_SHADING) {
            return makeShadeImage(MAPFile.VERTEX_COLOR_IMAGE_SIZE, MAPFile.VERTEX_COLOR_IMAGE_SIZE, false);
        } else {
            BufferedImage texture = getGameImage(map).toBufferedImage(map.getDisplaySettings());
            return MAPPolyTexture.makeShadedTexture(texture, makeShadeImage(texture.getWidth(), texture.getHeight(), true));
        }
    }

    private BufferedImage makeShadeImage(int width, int height, boolean useRaw) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        graphics.setColor(useRaw ? getColor().toShadeColor() : Utils.toAWTColor(MAPPolyGT.loadColor(getColor())));
        graphics.fillRect(0, 0, image.getWidth(), image.getHeight());
        graphics.dispose();
        return image;
    }

    @Override
    public boolean isOverlay(TextureMap map) {
        return map.getMode() == ShaderMode.OVERLAY_SHADING;
    }

    @Override
    public BigInteger makeIdentifier(TextureMap map) {
        if (map.getMode() == ShaderMode.NO_SHADING || (map.isUseModelTextureAnimation() && this.viewImageId != (short) -1)) {
            return makeIdentifier(0x7E8BA5E, getUseTextureId(map));
        } else if (isOverlay(map)) {
            return makeIdentifier(0xF1A754AD, getColor().toRGB());
        } else {
            return makeIdentifier(0xF1A77E8, getImageId(), getColor().toRGB());
        }
    }

    @Override
    public GameImage getGameImage(TextureMap map) {
        return map.getVloArchive().getMWD().getImageByTextureId(getUseTextureId(map));
    }

    @Override
    public void onMeshSetup(FrogMesh mesh) {
        if (mesh instanceof MOFMesh && isOverlay(mesh.getTextureMap()))
            ((MOFMesh) mesh).renderOverPolygon(this, this);
    }

    private int getUseTextureId(TextureMap map) {
        return (map.isUseModelTextureAnimation() && this.viewImageId != (short) -1) ? this.viewImageId : getImageId();
    }
}