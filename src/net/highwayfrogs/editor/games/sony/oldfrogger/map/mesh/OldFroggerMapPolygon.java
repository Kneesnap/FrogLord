package net.highwayfrogs.editor.games.sony.oldfrogger.map.mesh;

import lombok.Getter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.psx.CVector;
import net.highwayfrogs.editor.games.psx.polygon.PSXPolygonType;
import net.highwayfrogs.editor.games.sony.SCGameData;
import net.highwayfrogs.editor.games.sony.oldfrogger.OldFroggerGameInstance;
import net.highwayfrogs.editor.games.sony.shared.SCByteTextureUV;

import java.util.Arrays;

/**
 * Represents a map polygon seen in old Frogger.
 * Created by Kneesnap on 12/9/2023.
 */
@Getter
public class OldFroggerMapPolygon extends SCGameData<OldFroggerGameInstance> {
    private final PSXPolygonType polygonType;
    private final int[] vertices;
    private final CVector[] colors;
    private long textureId = -1;
    private final SCByteTextureUV[] textureUvs;

    private static final SCByteTextureUV[] EMPTY_UV_ARRAY = new SCByteTextureUV[0];

    public OldFroggerMapPolygon(OldFroggerGameInstance instance, PSXPolygonType polygonType) {
        super(instance);
        this.polygonType = polygonType;
        this.vertices = new int[polygonType.getVerticeCount()];
        this.colors = new CVector[polygonType.getColorCount()];
        this.textureUvs = polygonType.isTextured() ? new SCByteTextureUV[polygonType.getVerticeCount()] : EMPTY_UV_ARRAY;
        Arrays.fill(this.vertices, -1);
        for (int i = 0; i < this.colors.length; i++)
            this.colors[i] = new CVector();
        for (int i = 0; i < this.textureUvs.length; i++)
            this.textureUvs[i] = new SCByteTextureUV();
    }

    @Override
    public void load(DataReader reader) {
        for (int i = 0; i < this.vertices.length; i++)
            this.vertices[i] = reader.readUnsignedShortAsInt();
        for (int i = 0; i < this.colors.length; i++)
            this.colors[i].load(reader);
        if (this.polygonType.isTextured())
            this.textureId = reader.readUnsignedIntAsLong();
        for (int i = 0; i < this.textureUvs.length; i++)
            this.textureUvs[i].load(reader);
    }

    @Override
    public void save(DataWriter writer) {
        for (int i = 0; i < this.vertices.length; i++)
            writer.writeUnsignedShort(this.vertices[i]);
        for (int i = 0; i < this.colors.length; i++)
            this.colors[i].save(writer);
        if (this.polygonType.isTextured())
            writer.writeUnsignedInt(this.textureId);
        for (int i = 0; i < this.textureUvs.length; i++)
            this.textureUvs[i].save(writer);
    }

    /**
     * Get the size in bytes of a polygon of the given psx polygon type.
     * @param polygonType The polygon type to calculate the byte size of
     * @return byteSize
     */
    public static int getByteSize(PSXPolygonType polygonType) {
        return (polygonType.getVerticeCount() * Constants.SHORT_SIZE)
                + (polygonType.getColorCount() * CVector.BYTE_LENGTH)
                + (polygonType.isTextured() ? Constants.INTEGER_SIZE : 0)
                + (polygonType.isTextured() ? polygonType.getVerticeCount() * SCByteTextureUV.BYTE_SIZE : 0);
    }
}