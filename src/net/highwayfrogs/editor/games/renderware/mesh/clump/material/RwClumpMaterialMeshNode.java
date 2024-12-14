package net.highwayfrogs.editor.games.renderware.mesh.clump.material;

import net.highwayfrogs.editor.games.renderware.chunks.RwGeometryChunk;
import net.highwayfrogs.editor.games.renderware.chunks.RwGeometryChunk.RpMorphTarget;
import net.highwayfrogs.editor.games.renderware.struct.types.RpTriangle;
import net.highwayfrogs.editor.games.renderware.struct.types.RwTexCoords;
import net.highwayfrogs.editor.games.renderware.struct.types.RwV3d;
import net.highwayfrogs.editor.gui.mesh.DynamicMeshAdapterNode;
import net.highwayfrogs.editor.system.math.Vector2f;

import java.util.List;

/**
 * Manages model terrain for an individual material in an RwClump.
 * Unfortunately, similar to Frogger: The Great Quest, the UVs are somewhat tough to deal with as these textures repeat themselves.
 * TODO: Allow morphing.
 * TODO: Preview texCoord animations.
 * Created by Kneesnap on 8/27/2024.
 */
public class RwClumpMaterialMeshNode extends DynamicMeshAdapterNode<RwGeometryChunk> {
    private int texCoordSetIndex;
    private int morphTargetIndex;

    private static final Vector2f[] DEFAULT_TEXCOORDS = {Vector2f.ZERO, Vector2f.UNIT_X, Vector2f.ONE,
            Vector2f.ZERO, Vector2f.UNIT_Y, Vector2f.ONE};

    public RwClumpMaterialMeshNode(RwClumpMaterialMesh mesh) {
        super(mesh);
    }

    @Override
    public RwClumpMaterialMesh getMesh() {
        return (RwClumpMaterialMesh) super.getMesh();
    }

    @Override
    protected void onAddedToMesh() {
        super.onAddedToMesh();

        // Setup buffers.
        for (RwGeometryChunk chunk : getMesh().getClump().getGeometryList().getGeometries()) {
            for (int i = 0; i < chunk.getTriangles().size(); i++) {
                RpTriangle triangle = chunk.getTriangles().get(i);

                // Add if there's at least one triangle using the material.
                if (triangle.getMaterialIndex() == getMesh().getRwMaterialIndex()) {
                    this.add(chunk);
                    break;
                }
            }
        }
    }

    @Override
    protected DynamicMeshTypedDataEntry writeValuesToArrayAndCreateEntry(RwGeometryChunk geometry) {
        DynamicMeshTypedDataEntry entry = new DynamicMeshTypedDataEntry(getMesh(), geometry);
        RpMorphTarget morphTarget = getMorphTarget(geometry);
        if (morphTarget == null)
            return entry;

        // Write vertices and uvs.
        for (int i = 0; i < morphTarget.getVertices().size(); i++) {
            RwV3d vertex = morphTarget.getVertices().get(i);
            entry.addVertexValue(vertex.getX(), vertex.getY(), vertex.getZ());
        }

        // Write face data.
        int vtxStartIndex = entry.getVertexStartIndex();
        List<RpTriangle> triangles = geometry.getTriangles();
        List<RwTexCoords> texCoordSet = getTexCoordSet(geometry);
        int triangleIndex = 0;
        int missingTexCoordIndex = 0;
        for (int i = 0; i < triangles.size(); i++) {
            RpTriangle triangle = triangles.get(i);
            if (triangle.getMaterialIndex() != getMesh().getRwMaterialIndex())
                continue;

            for (int j = 0; j < triangle.getVertexIndices().length; j++) {
                if (texCoordSet != null) {
                    RwTexCoords texCoords = texCoordSet.get(triangle.getVertexIndices()[j]);
                    entry.addTexCoordValue(texCoords.getU(), texCoords.getV());
                } else {
                    Vector2f texCoord = DEFAULT_TEXCOORDS[missingTexCoordIndex++ % DEFAULT_TEXCOORDS.length];
                    entry.addTexCoordValue(texCoord.getX(), texCoord.getY());
                }
            }

            int uvStartIndex = entry.getTexCoordStartIndex() + (triangleIndex++ * RpTriangle.VERTEX_COUNT);
            entry.addFace(vtxStartIndex + triangle.getVertexIndices()[0], uvStartIndex,
                    vtxStartIndex + triangle.getVertexIndices()[1], uvStartIndex + 1,
                    vtxStartIndex + triangle.getVertexIndices()[2], uvStartIndex + 2);
        }

        return entry;
    }

    @Override
    public void updateVertex(DynamicMeshTypedDataEntry entry, int localVertexIndex) {
        RwV3d vertex = entry.getDataSource().getVertices().get(localVertexIndex);
        entry.writeVertexXYZ(localVertexIndex, vertex.getX(), vertex.getY(), vertex.getZ());
    }

    @Override
    public void updateTexCoord(DynamicMeshTypedDataEntry entry, int localTexCoordIndex) {
        RwGeometryChunk geometry = entry.getDataSource();
        if (localTexCoordIndex < 0 || localTexCoordIndex >= RpTriangle.VERTEX_COUNT * geometry.getTriangles().size())
            throw new IllegalArgumentException("Unsupported local texCoordIndex " + localTexCoordIndex);

        // Calculate index information.
        int bufferTriangleIndex = localTexCoordIndex / RpTriangle.VERTEX_COUNT;
        int realTriangleIndex = 0;
        RpTriangle triangle = null;
        for (int i = 0; i < geometry.getTriangles().size(); i++) {
            triangle = geometry.getTriangles().get(i);
            if (triangle.getMaterialIndex() != getMesh().getRwMaterialIndex())
                continue;

            if (realTriangleIndex++ >= bufferTriangleIndex)
                break;
        }

        if (triangle == null)
            return;

        // Calculate index information.
        int localVertexIndex = (localTexCoordIndex % RpTriangle.VERTEX_COUNT);
        List<RwTexCoords> texCoordSet = getTexCoordSet(geometry);

        // Write updated texCoord.
        if (texCoordSet != null) {
            RwTexCoords texCoords = texCoordSet.get(triangle.getVertexIndices()[localVertexIndex]);
            entry.writeTexCoordValue(localTexCoordIndex, texCoords.getU(), texCoords.getV());
        } else {
            Vector2f texCoord = DEFAULT_TEXCOORDS[bufferTriangleIndex % DEFAULT_TEXCOORDS.length];
            entry.writeTexCoordValue(localTexCoordIndex, texCoord.getX(), texCoord.getY());
        }
    }

    private RpMorphTarget getMorphTarget(RwGeometryChunk geometry) {
        if (geometry == null || geometry.getMorphTargets().isEmpty())
            return null;

        return geometry.getMorphTargets().get(Math.min(geometry.getMorphTargets().size() - 1, this.morphTargetIndex));
    }

    private List<RwTexCoords> getTexCoordSet(RwGeometryChunk geometry) {
        if (geometry == null || geometry.getTexCoordSets().isEmpty())
            return null;

        return geometry.getTexCoordSets().get(Math.min(geometry.getTexCoordSets().size() - 1, this.texCoordSetIndex));
    }
}