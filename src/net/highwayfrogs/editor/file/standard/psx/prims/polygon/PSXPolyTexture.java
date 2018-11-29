package net.highwayfrogs.editor.file.standard.psx.prims.polygon;

import lombok.Getter;
import lombok.Setter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.standard.psx.ByteUV;
import net.highwayfrogs.editor.file.standard.psx.PSXColorVector;
import net.highwayfrogs.editor.file.vlo.TextureMap;
import net.highwayfrogs.editor.file.vlo.TextureMap.TextureEntry;
import net.highwayfrogs.editor.file.writer.DataWriter;

/**
 * Represents PSX polgons with a texture.
 * Created by Kneesnap on 8/25/2018.
 */
@Getter
@Setter
public class PSXPolyTexture extends PSXPolygon {
    private short flags;
    private ByteUV[] uvs;
    private short clutId;
    private short textureId;
    private PSXColorVector[] vectors;
    private boolean flippedUVs;

    public static final int FLAG_SEMI_TRANSPARENT = 1; // setSemiTrans(true)
    public static final int FLAG_ENVIRONMENT_IMAGE = 1 << 1; // Show the solid environment bitmap. (For instance, how water appears as a solid body, or sludge in the sewer levels.)
    public static final int FLAG_MAX_ORDER_TABLE = 1 << 2; // Puts at the back of the order table. Either the very lowest rendering priority, or the very highest.

    // These are run-time-only it seems. They get applied from the anim section.
    public static final int FLAG_ANIMATED_UV = 1 << 3; // Poly has an associated map animation using UV animation.
    public static final int FLAG_ANIMATED_TEXTURE = 1 << 4; // Poly has an associated map animation using cel list animation.

    public PSXPolyTexture(int verticeCount, int colorCount) {
        super(verticeCount);
        this.uvs = new ByteUV[verticeCount];
        this.vectors = new PSXColorVector[colorCount];
    }

    @Override
    public void load(DataReader reader) {
        super.load(reader);

        this.flags = reader.readShort();
        reader.readShort(); // Padding

        loadUV(0, reader);
        this.clutId = reader.readShort();
        loadUV(1, reader);
        this.textureId = reader.readShort();

        for (int i = 2; i < this.uvs.length; i++)
            loadUV(i, reader);

        if (this.uvs.length == PSXPolygon.TRI_SIZE)
            reader.readShort(); // Padding.

        for (int i = 0; i < this.vectors.length; i++) {
            PSXColorVector vector = new PSXColorVector();
            vector.load(reader);
            this.vectors[i] = vector;
        }
    }

    @Override
    public void save(DataWriter writer) {
        super.save(writer);

        writer.writeShort(this.flags);
        writer.writeNull(Constants.SHORT_SIZE);
        this.uvs[0].save(writer);
        writer.writeShort(this.clutId);
        this.uvs[1].save(writer);
        writer.writeShort(this.textureId);

        for (int i = 2; i < this.uvs.length; i++)
            this.uvs[i].save(writer);

        if (this.uvs.length == 3)
            writer.writeNull(Constants.SHORT_SIZE);

        for (PSXColorVector colorVector : this.vectors)
            colorVector.save(writer);
    }

    private void loadUV(int id, DataReader reader) {
        this.uvs[id] = new ByteUV();
        this.uvs[id].load(reader);
    }

    /**
     * Get the nth obj UV string.
     * @param index The index to get.
     * @return objUvString
     */
    public String getObjUVString(int index) {
        boolean shouldSwap = (this.uvs.length == PSXPolygon.QUAD_SIZE);

        if (shouldSwap) { // I believe we have to swap it due to the .obj format.
            ByteUV temp = this.uvs[2];
            this.uvs[2] = this.uvs[3];
            this.uvs[3] = temp;
        }

        String uvString = this.uvs[index].toObjTextureString();

        if (shouldSwap) {
            ByteUV temp = this.uvs[2];
            this.uvs[2] = this.uvs[3];
            this.uvs[3] = temp;
        }

        return uvString;

    }

    @Override
    public int getOrderId() {
        return getTextureId();
    }

    @Override
    public TextureEntry getEntry(TextureMap map) {
        return map.getEntryMap().get(map.getRemapList().get(getTextureId()));
    }
}
