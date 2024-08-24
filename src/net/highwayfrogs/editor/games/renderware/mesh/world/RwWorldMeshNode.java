package net.highwayfrogs.editor.games.renderware.mesh.world;

import net.highwayfrogs.editor.games.psx.shading.PSXShadeTextureDefinition;
import net.highwayfrogs.editor.games.renderware.chunks.sector.RwAtomicSectorChunk;
import net.highwayfrogs.editor.games.renderware.struct.types.RpTriangle;
import net.highwayfrogs.editor.games.renderware.struct.types.RwTexCoords;
import net.highwayfrogs.editor.games.renderware.struct.types.RwV3d;
import net.highwayfrogs.editor.gui.mesh.DynamicMeshAdapterNode;
import net.highwayfrogs.editor.gui.texture.atlas.AtlasTexture;
import net.highwayfrogs.editor.system.math.Vector2f;

import java.util.List;

/**
 * Implements the main mesh node for rendering an RwWorld.
 * Unfortunately, similar to Frogger: The Great Quest, the UVs are somewhat tough to deal with as these textures repeat themselves.
 * So, for many maps this doesn't look right.
 * TODO: Shading is also broken (we need to consider adding support for the texture condensation effort, for gouraud textures)
 *  -> Alternatively, vertex colors in JavaFX.
 * Created by Kneesnap on 8/18/2024.
 */
public class RwWorldMeshNode extends DynamicMeshAdapterNode<RwAtomicSectorChunk> {
    private final Vector2f tempVector = new Vector2f();

    public RwWorldMeshNode(RwWorldMesh mesh) {
        super(mesh);
    }

    @Override
    public RwWorldMesh getMesh() {
        return (RwWorldMesh) super.getMesh();
    }

    @Override
    protected void onAddedToMesh() {
        super.onAddedToMesh();

        // Setup buffers by traversing the tree.
        getMesh().getWorld().getWorldSectors().forEach(this::add);
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
        int texCoordInterval = worldSector.getTexCoordSets().size();
        List<RpTriangle> triangles = worldSector.getTriangles();
        for (int i = 0; i < triangles.size(); i++) {
            RpTriangle triangle = triangles.get(i);

            PSXShadeTextureDefinition shadeDefinition = getMesh().getShadedTextureManager().getShadedTexture(triangle);
            AtlasTexture materialTexture = getMesh().getTextureAtlas().getTextureFromSourceOrFallback(shadeDefinition);
            for (int j = 0; j < texCoordInterval; j++) {
                List<RwTexCoords> texCoordSet = worldSector.getTexCoordSets().get(j);
                for (int k = 0; k < triangle.getVertexIndices().length; k++) {
                    RwTexCoords texCoords = texCoordSet.get(triangle.getVertexIndices()[k]);
                    this.tempVector.setXY(texCoords.getU() % 1F, texCoords.getV() % 1F);
                    Vector2f localUv = getMesh().getTextureAtlas().getUV(materialTexture, this.tempVector, this.tempVector);
                    entry.addTexCoordValue(localUv);
                }
            }

            int uvStartIndex = entry.getTexCoordStartIndex() + (i * RpTriangle.VERTEX_COUNT * texCoordInterval);
            entry.addFace(vtxStartIndex + triangle.getVertexIndices()[0], uvStartIndex,
                    vtxStartIndex + triangle.getVertexIndices()[1], uvStartIndex + texCoordInterval,
                    vtxStartIndex + triangle.getVertexIndices()[2], uvStartIndex + (2 * texCoordInterval));
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
        int faceCount = atomicSector.getTriangles().size();
        int texCoordInterval = atomicSector.getTexCoordSets().size();
        if (localTexCoordIndex < 0 || localTexCoordIndex >= RpTriangle.VERTEX_COUNT * faceCount * texCoordInterval)
            throw new IllegalArgumentException("Unsupported local texCoordIndex " + localTexCoordIndex);

        // Calculate index information.
        int texCoordSetIndex = (localTexCoordIndex % texCoordInterval);
        int localVertexIndex = (localTexCoordIndex % (texCoordInterval * RpTriangle.VERTEX_COUNT)) / texCoordInterval;
        int triangleIndex = localTexCoordIndex / (texCoordInterval * RpTriangle.VERTEX_COUNT);
        List<RwTexCoords> texCoordSet = atomicSector.getTexCoordSets().get(texCoordSetIndex);
        // localTexCoordIndex should be (triangleIndex * RpTriangle.VERTEX_COUNT * texCoordInterval) + (localVertexIndex * texCoordInterval) + texCoordSetIndex;

        // Calculate material texture.
        RpTriangle triangle = atomicSector.getTriangles().get(triangleIndex);
        PSXShadeTextureDefinition shadeDefinition = getMesh().getShadedTextureManager().getShadedTexture(triangle);
        AtlasTexture materialTexture = getMesh().getTextureAtlas().getTextureFromSourceOrFallback(shadeDefinition);

        // Write updated texCoord.
        RwTexCoords texCoords = texCoordSet.get(triangle.getVertexIndices()[localVertexIndex]);
        this.tempVector.setXY(texCoords.getU() % 1F, texCoords.getV() % 1F);
        Vector2f localUv = getMesh().getTextureAtlas().getUV(materialTexture, this.tempVector, this.tempVector);
        entry.writeTexCoordValue(localTexCoordIndex, localUv);
    }
}