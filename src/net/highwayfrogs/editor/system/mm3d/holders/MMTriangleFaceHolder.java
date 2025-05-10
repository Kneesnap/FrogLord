package net.highwayfrogs.editor.system.mm3d.holders;

import net.highwayfrogs.editor.games.sony.shared.mof2.mesh.MRMofPolygon;
import net.highwayfrogs.editor.system.mm3d.MMDataBlockHeader;
import net.highwayfrogs.editor.system.mm3d.MisfitModel3DObject;
import net.highwayfrogs.editor.system.mm3d.OffsetType;
import net.highwayfrogs.editor.system.mm3d.blocks.MMTriangleFaceBlock;

/**
 * Holds face data.
 * Created by Kneesnap on 3/2/2019.
 */
public class MMTriangleFaceHolder extends MMDataBlockHeader<MMTriangleFaceBlock> {
    public MMTriangleFaceHolder(MisfitModel3DObject model) {
        super(OffsetType.TRIANGLES, model);
    }

    /**
     * Adds a mof polygon face.
     * @param polygon mofPolygon
     */
    public void addMofPolygon(MRMofPolygon polygon) {
        short flags = polygon.getMofPart().isHiddenByConfiguration() ? MMTriangleFaceBlock.FLAG_HIDDEN : 0;
        if (polygon.getVertexCount() == 4) {
            // TODO: addRectangle(polygon.getVertexStart(), polygon.getVertices(), flags);
        } else if (polygon.getVertexCount() == 3) {
            // TODO: addTriangle(polygon.getVertexStart(), polygon.getVertices(), flags);
        } else {
            throw new RuntimeException("Failed to add MOF Face.");
        }
    }

    /**
     * Add a triangle face to this.
     * @param vertices Vertices to add.
     */
    public void addTriangle(int baseVertex, int[] vertices, short flags) {
        addTriangle(baseVertex, vertices[0], vertices[1], vertices[2], flags);
    }

    /**
     * Add a triangle face to this.
     * @param v1 One of the vertices to add.
     * @param v2 One of the vertices to add.
     * @param v3 One of the vertices to add.
     */
    public void addTriangle(int baseVertex, int v1, int v2, int v3, short flags) {
        MMTriangleFaceBlock vertex = addNewElement();
        vertex.setFlags(flags);
        vertex.getVertices()[0] = v3 + baseVertex;
        vertex.getVertices()[1] = v2 + baseVertex;
        vertex.getVertices()[2] = v1 + baseVertex;
    }

    /**
     * Add a rectangle face to this.
     * @param vertices The vertices to add.
     */
    public void addRectangle(int baseVertex, int[] vertices, short flags) {
        addRectangle(baseVertex, vertices[0], vertices[1], vertices[2], vertices[3], flags);
    }

    /**
     * Add a rectangle face to this.
     * @param v1 One of the vertices to add.
     * @param v2 One of the vertices to add.
     * @param v3 One of the vertices to add.
     */
    public void addRectangle(int baseVertex, int v1, int v2, int v3, int v4, short flags) {
        MMTriangleFaceBlock vertex = addNewElement();
        vertex.setFlags(flags);
        vertex.getVertices()[0] = v1 + baseVertex;
        vertex.getVertices()[1] = v4 + baseVertex;
        vertex.getVertices()[2] = v2 + baseVertex;

        vertex = addNewElement();
        vertex.setFlags(flags);
        vertex.getVertices()[0] = v2 + baseVertex;
        vertex.getVertices()[1] = v4 + baseVertex;
        vertex.getVertices()[2] = v3 + baseVertex;
    }
}