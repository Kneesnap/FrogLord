package net.highwayfrogs.editor.games.sony.shared.landscaper.heightfield;

import lombok.Getter;
import net.highwayfrogs.editor.games.sony.shared.landscaper.LandscapePolygon;
import net.highwayfrogs.editor.games.sony.shared.landscaper.LandscapeVertex;
import net.highwayfrogs.editor.system.math.Vector3f;
import net.highwayfrogs.editor.utils.data.reader.DataReader;
import net.highwayfrogs.editor.utils.data.writer.DataWriter;

/**
 * Represents a vertex which is part of a height-field.
 * Created by Kneesnap on 7/16/2024.
 */
public class AdvancedHeightFieldVertex extends LandscapeVertex {
    private final Vector3f localPosition = new Vector3f(); // Relative to the column origin.
    @Getter private AdvancedHeightFieldVertexColumn column; // The column which the vertex is attached to.

    public AdvancedHeightFieldVertex(AdvancedHeightFieldVertexColumn column, AdvancedHeightFieldLandscapeNode<? extends AdvancedHeightFieldVertex, ? extends LandscapePolygon> heightFieldNode) {
        super(heightFieldNode);
        this.column = column;
    }

    @Override
    public AdvancedHeightFieldLandscapeNode<? extends AdvancedHeightFieldVertex, ? extends LandscapePolygon> getOwner() {
        return (AdvancedHeightFieldLandscapeNode<? extends AdvancedHeightFieldVertex, ? extends LandscapePolygon>) super.getOwner();
    }

    @Override
    public void load(DataReader reader) {
        this.localPosition.load(reader);
    }

    @Override
    public void save(DataWriter writer) {
        this.localPosition.save(writer);
    }

    /**
     * Test if this vertex is a root vertex.
     */
    public boolean isRootVertex() {
        return this.column != null && this.column.getRootVertex() == this;
    }

    @Override
    public Vector3f getCachedLocalPosition() {
        return this.localPosition;
    }

    @Override
    public Vector3f getLocalPosition(Vector3f output) {
        if (output == null)
            output = new Vector3f();

        return output.setXYZ(this.localPosition);
    }

    @Override
    public Vector3f getOriginPosition(Vector3f output) {
        if (output == null)
            output = new Vector3f();

        if (this.column != null) {
            return this.column.getOriginPosition(output);
        } else {
            return output.setXYZ(0, 0, 0);
        }
    }

    @Override
    protected void onRemoveFromNode() {
        if (this.column != null) {
            this.column.removeVertex(this);
            this.column = null;
        }
    }
}
