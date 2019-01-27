package net.highwayfrogs.editor.file.map.poly.polygon;

import lombok.Getter;
import lombok.Setter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.file.map.view.TextureMap;
import net.highwayfrogs.editor.file.map.view.TextureMap.TextureEntry;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.standard.psx.ByteUV;
import net.highwayfrogs.editor.file.standard.psx.PSXColorVector;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.gui.GUIEditorGrid;
import net.highwayfrogs.editor.gui.editor.MapUIController;

/**
 * Represents PSX polgons with a texture.
 * Created by Kneesnap on 8/25/2018.
 */
@Getter
@Setter
public class MAPPolyTexture extends MAPPolygon {
    private short flags;
    private ByteUV[] uvs;
    private short clutId;
    private short textureId;
    private PSXColorVector[] vectors;

    public static final int FLAG_SEMI_TRANSPARENT = Constants.BIT_FLAG_0; // setSemiTrans(true)
    public static final int FLAG_ENVIRONMENT_IMAGE = Constants.BIT_FLAG_1; // Show the solid environment bitmap. (For instance, how water appears as a solid body, or sludge in the sewer levels.)
    public static final int FLAG_MAX_ORDER_TABLE = Constants.BIT_FLAG_2; // Puts at the back of the order table. Either the very lowest rendering priority, or the very highest.

    // These are run-time-only it seems. They get applied from the anim section.
    public static final int FLAG_ANIMATED_UV = Constants.BIT_FLAG_3; // Poly has an associated map animation using UV animation.
    public static final int FLAG_ANIMATED_TEXTURE = Constants.BIT_FLAG_4; // Poly has an associated map animation using cel list animation.

    public MAPPolyTexture(MAPPolygonType type, int verticeCount, int colorCount) {
        super(type, verticeCount);
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

        if (this.uvs.length == MAPPolygon.TRI_SIZE)
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
        boolean shouldSwap = (this.uvs.length == MAPPolygon.QUAD_SIZE);

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

    @Override
    public void setupEditor(MapUIController controller, GUIEditorGrid editor) {
        super.setupEditor(controller, editor);
        editor.addShortField("Flags", getFlags(), this::setFlags, null);
        editor.addShortField("Clut ID", getClutId(), this::setClutId, null);
        editor.addShortField("Texture ID", getTextureId(), this::setTextureId, null);

        int id = 0;
        for (PSXColorVector colorVec : getVectors())
            editor.addColorPicker("Color #" + (++id), colorVec.toRGB(), colorVec::fromRGB);

        id = 0;
        for (ByteUV byteUV : getUvs()) {
            editor.addBoldLabel("UV #" + (++id) + ":");
            byteUV.setupEditor(editor);
        }
    }
}
