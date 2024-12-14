package net.highwayfrogs.editor.games.renderware.mesh.world;

import net.highwayfrogs.editor.games.renderware.chunks.sector.RwAtomicSectorChunk;
import net.highwayfrogs.editor.games.renderware.struct.types.RpTriangle;
import net.highwayfrogs.editor.games.renderware.struct.types.RwTexCoords;
import net.highwayfrogs.editor.games.renderware.struct.types.RwV3d;
import net.highwayfrogs.editor.gui.mesh.DynamicMeshAdapterNode;
import net.highwayfrogs.editor.system.math.Vector2f;

import java.util.List;

/**
 * Manages map terrain for an individual material in an RwWorld.
 * Created by Kneesnap on 8/23/2024.
 */
public class RwWorldMaterialMeshNode extends DynamicMeshAdapterNode<RwAtomicSectorChunk> {
    private int texCoordSetIndex;

    private static final Vector2f[] DEFAULT_TEXCOORDS = {Vector2f.ZERO, Vector2f.UNIT_X, Vector2f.ONE};

    public RwWorldMaterialMeshNode(RwWorldMaterialMesh mesh) {
        super(mesh);
    }

    @Override
    public RwWorldMaterialMesh getMesh() {
        return (RwWorldMaterialMesh) super.getMesh();
    }

    @Override
    protected void onAddedToMesh() {
        super.onAddedToMesh();

        // Setup buffers by traversing the tree.
        for (RwAtomicSectorChunk chunk : getMesh().getWorld().getWorldSectors()) {
            for (int i = 0; i < chunk.getTriangles().size(); i++) {
                RpTriangle triangle = chunk.getTriangles().get(i);

                // Add if there's at least one triangle using the material.
                if (chunk.getMaterialListBaseIndex() + triangle.getMaterialIndex() == getMesh().getRwMaterialIndex()) {
                    this.add(chunk);
                    break;
                }
            }
        }
    }

    @Override
    protected DynamicMeshTypedDataEntry writeValuesToArrayAndCreateEntry(RwAtomicSectorChunk worldSector) {
        DynamicMeshTypedDataEntry entry = new DynamicMeshTypedDataEntry(getMesh(), worldSector);

        // Write vertices and uvs.
        for (int i = 0; i < worldSector.getVertices().size(); i++) {
            RwV3d vertex = worldSector.getVertices().get(i);
            entry.addVertexValue(vertex.getX(), vertex.getY(), vertex.getZ());
        }

        // Write face data.
        int vtxStartIndex = entry.getVertexStartIndex();
        List<RpTriangle> triangles = worldSector.getTriangles();
        int triangleIndex = 0;
        List<RwTexCoords> texCoordSet = getTexCoordSet(worldSector);
        for (int i = 0; i < triangles.size(); i++) {
            RpTriangle triangle = triangles.get(i);
            if (worldSector.getMaterialListBaseIndex() + triangle.getMaterialIndex() != getMesh().getRwMaterialIndex())
                continue;

            for (int j = 0; j < triangle.getVertexIndices().length; j++) {
                if (texCoordSet != null) {
                    RwTexCoords texCoords = texCoordSet.get(triangle.getVertexIndices()[j]);
                    entry.addTexCoordValue(texCoords.getU(), texCoords.getV());
                } else {
                    Vector2f texCoord = DEFAULT_TEXCOORDS[j];
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
        RwAtomicSectorChunk atomicSector = entry.getDataSource();

        // Calculate index information.
        int localVertexIndex = (localTexCoordIndex % RpTriangle.VERTEX_COUNT);
        int bufferTriangleIndex = localTexCoordIndex / RpTriangle.VERTEX_COUNT;

        RpTriangle triangle = null;
        for (int i = 0; i < atomicSector.getTriangles().size(); i++) {
            triangle = atomicSector.getTriangles().get(i);
            if (atomicSector.getMaterialListBaseIndex() + triangle.getMaterialIndex() != getMesh().getRwMaterialIndex())
                continue;

            if (bufferTriangleIndex-- <= 0)
                break;
        }

        if (triangle == null)
            return;

        // Write updated texCoord.
        List<RwTexCoords> texCoordSet = getTexCoordSet(atomicSector);
        int targetVertexId = triangle.getVertexIndices()[localVertexIndex];
        if (texCoordSet != null && texCoordSet.size() > targetVertexId) {
            RwTexCoords texCoords = texCoordSet.get(targetVertexId);
            entry.writeTexCoordValue(localTexCoordIndex, texCoords.getU(), texCoords.getV());
        } else {
            Vector2f texCoord = DEFAULT_TEXCOORDS[localVertexIndex];
            entry.writeTexCoordValue(localTexCoordIndex, texCoord.getX(), texCoord.getY());
        }
    }

    private List<RwTexCoords> getTexCoordSet(RwAtomicSectorChunk atomicSector) {
        if (atomicSector == null || atomicSector.getTexCoordSets().isEmpty())
            return null;

        return atomicSector.getTexCoordSets().get(Math.min(this.texCoordSetIndex, atomicSector.getTexCoordSets().size() - 1));
    }
}