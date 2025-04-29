package net.highwayfrogs.editor.file.map.poly;

import lombok.Getter;
import lombok.Setter;
import net.highwayfrogs.editor.file.standard.psx.PSXGPUPrimitive;
import net.highwayfrogs.editor.games.sony.frogger.map.FroggerMapFile;
import net.highwayfrogs.editor.games.sony.frogger.map.mesh.FroggerMapPolygon;
import net.highwayfrogs.editor.games.sony.frogger.map.mesh.FroggerMapPolygonType;
import net.highwayfrogs.editor.utils.data.reader.DataReader;
import net.highwayfrogs.editor.utils.data.writer.DataWriter;

/**
 * Represents a MAP primitive.
 * Created by Kneesnap on 1/26/2019.
 */
@Getter
public abstract class MAPPrimitive extends PSXGPUPrimitive {
    @Setter private int[] vertices;
    private MAPPrimitiveType type;
    @Setter private boolean allowDisplay; // Whether or not this prim can be included in a MAP_GROUP.

    public MAPPrimitive(MAPPrimitiveType type, int verticeCount) {
        this.type = type;
        this.vertices = new int[verticeCount];
    }

    @Override
    public void load(DataReader reader) {
        if (this.vertices.length == 3) {
            for (int i = 0; i < vertices.length; i++)
                this.vertices[i] = reader.readUnsignedShortAsInt();
        } else {
            this.vertices[0] = reader.readUnsignedShortAsInt();
            this.vertices[1] = reader.readUnsignedShortAsInt();
            this.vertices[3] = reader.readUnsignedShortAsInt();
            this.vertices[2] = reader.readUnsignedShortAsInt();
        }
    }

    @Override
    public void save(DataWriter writer) {
        if (this.vertices.length == 3) {
            for (int i = 0; i < this.vertices.length; i++)
                writer.writeUnsignedShort(this.vertices[i]);
        } else {
            writer.writeUnsignedShort(this.vertices[0]);
            writer.writeUnsignedShort(this.vertices[1]);
            writer.writeUnsignedShort(this.vertices[3]);
            writer.writeUnsignedShort(this.vertices[2]);
        }
    }

    /**
     * Converts the primitive to the new map format.
     */
    public FroggerMapPolygon convertToNewFormat(FroggerMapFile mapFile) {
        FroggerMapPolygon polygon = new FroggerMapPolygon(mapFile, FroggerMapPolygonType.valueOf(this.type.name()));
        if (this.vertices.length != polygon.getVertexCount())
            throw new RuntimeException("Vertex mismatch!");

        if (this.vertices.length == 3) {
            System.arraycopy(this.vertices, 0, polygon.getVertices(), 0, this.vertices.length);
        } else {
            polygon.getVertices()[0] = this.vertices[0];
            polygon.getVertices()[1] = this.vertices[1];
            polygon.getVertices()[2] = this.vertices[3];
            polygon.getVertices()[3] = this.vertices[2];
        }

        polygon.setVisible(this.allowDisplay);
        return polygon;
    }
}
