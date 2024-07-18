package net.highwayfrogs.editor.games.sony.shared.landscaper.heightfield;

import lombok.Getter;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.sony.shared.landscaper.Landscape;
import net.highwayfrogs.editor.games.sony.shared.landscaper.LandscapeNode;
import net.highwayfrogs.editor.games.sony.shared.landscaper.LandscapePolygon;

/**
 * Represents a height-field with advanced editing capabilities.
 * Used for Frogger map editing and potentially other games too.
 * Created by Kneesnap on 7/15/2024.
 */
public abstract class AdvancedHeightFieldLandscapeNode<TVertex extends AdvancedHeightFieldVertex, TPolygon extends LandscapePolygon> extends LandscapeNode<TVertex, TPolygon> {
    @Getter private final AdvancedHeightField heightField;

    protected AdvancedHeightFieldLandscapeNode(Landscape landscape, AdvancedHeightField heightField) {
        super(landscape);
        this.heightField = heightField;
    }

    @Override
    protected void readVertices(DataReader reader) {
        this.vertices.clear();

        // Read vertex definitions.
        this.heightField.load(reader);
        for (int z = 0; z < this.heightField.getZSquareCount(); z++) {
            for (int x = 0; x < this.heightField.getXSquareCount(); x++) {
                AdvancedHeightFieldVertexColumn column = this.heightField.getGridEntry(x, z);
                if (column == null)
                    continue;

                short vertexCount = reader.readUnsignedByteAsShort();
                for (int j = 0; j < vertexCount; j++) {
                    TVertex nextVertex = createNextVertex(reader, column);
                    nextVertex.load(reader);
                    if (!addVertex(nextVertex))
                        throw new RuntimeException("Failed to register vertex during readVertices()!");
                }
            }
        }
    }

    @Override
    protected void writeVertices(DataWriter writer) {
        this.heightField.save(writer);
        for (int z = 0; z < this.heightField.getZSquareCount(); z++) {
            for (int x = 0; x < this.heightField.getXSquareCount(); x++) {
                AdvancedHeightFieldVertexColumn column = this.heightField.getGridEntry(x, z);
                if (column == null)
                    continue;

                writer.writeUnsignedByte((short) column.getSortedVertices().size());
                for (int j = 0; j < column.getSortedVertices().size(); j++)
                    column.getSortedVertices().get(j).save(writer);
            }
        }
    }

    @Override
    protected void readNodeData(DataReader reader) {
        this.heightField.readGridDataEntries(reader);
    }

    @Override
    protected void writeNodeData(DataWriter writer) {
        this.heightField.writeGridDataEntries(writer);
    }

    @Override
    protected TVertex createNextVertex(DataReader reader) {
        throw new UnsupportedOperationException("Cannot run createNextVertex(DataReader), use createNextVertex(DataReader, AdvancedHeightFieldVertexColumn) instead.");
    }

    /**
     * Creates a new landscape vertex based on heuristics or data read from the reader as well as .
     * @return nextVertex
     */
    @SuppressWarnings("unchecked")
    protected TVertex createNextVertex(DataReader reader, AdvancedHeightFieldVertexColumn column) {
        return (TVertex) new AdvancedHeightFieldVertex(column, this);
    }
}
