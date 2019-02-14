package net.highwayfrogs.editor.file.mof.prims;

import lombok.Getter;
import lombok.Setter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.file.map.view.TextureMap;
import net.highwayfrogs.editor.file.map.view.TextureMap.TextureEntry;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.standard.psx.ByteUV;
import net.highwayfrogs.editor.file.standard.psx.PSXColorVector;
import net.highwayfrogs.editor.file.writer.DataWriter;


/**
 * Represents a MOF polygon with a texture.
 * Created by Kneesnap on 1/1/2019.
 */
@Getter
@Setter
public class MOFPolyTexture extends MOFPolygon {
    private ByteUV[] uvs;
    private short clutId;
    private short textureId;
    private short imageId;
    private PSXColorVector color;
    private boolean flippedUVs;

    public static final int FLAG_SEMI_TRANSPARENT = Constants.BIT_FLAG_0; // setSemiTrans(true)
    public static final int FLAG_ENVIRONMENT_IMAGE = Constants.BIT_FLAG_1; // Show the solid environment bitmap. (For instance, how water appears as a solid body, or sludge in the sewer levels.)
    public static final int FLAG_MAX_ORDER_TABLE = Constants.BIT_FLAG_2; // Puts at the back of the order table. Either the very lowest rendering priority, or the very highest.

    // These are run-time-only it seems. They get applied from the anim section.
    public static final int FLAG_ANIMATED_UV = Constants.BIT_FLAG_3; // Poly has an associated map animation using UV animation.
    public static final int FLAG_ANIMATED_TEXTURE = Constants.BIT_FLAG_4; // Poly has an associated map animation using cel list animation.

    public MOFPolyTexture(MOFPrimType type, int verticeCount, int normalCount) {
        super(type, verticeCount, normalCount, 0);
        this.uvs = new ByteUV[verticeCount];
    }

    @Override
    public void onLoad(DataReader reader) {
        loadUV(0, reader);
        this.clutId = reader.readShort();
        loadUV(1, reader);
        this.textureId = reader.readShort();
        for (int i = 2; i < this.uvs.length; i++)
            loadUV(i, reader);

        this.imageId = reader.readShort();
    }

    @Override
    public void onSave(DataWriter writer) {
        super.onSave(writer);

        this.uvs[0].save(writer);
        writer.writeShort(this.clutId);
        this.uvs[1].save(writer);
        writer.writeShort(this.textureId);

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
        return getTextureId();
    }

    @Override
    public TextureEntry getEntry(TextureMap map) {
        return map.getEntry(getImageId());
    }
}
