package net.highwayfrogs.editor.games.renderware.mesh.clump;

import net.highwayfrogs.editor.games.psx.shading.PSXShadeTextureDefinition;
import net.highwayfrogs.editor.games.renderware.chunks.RwGeometryChunk;
import net.highwayfrogs.editor.games.renderware.chunks.RwGeometryChunk.RpMorphTarget;
import net.highwayfrogs.editor.games.renderware.struct.types.RpTriangle;
import net.highwayfrogs.editor.games.renderware.struct.types.RwTexCoords;
import net.highwayfrogs.editor.games.renderware.struct.types.RwV3d;
import net.highwayfrogs.editor.gui.mesh.DynamicMeshAdapterNode;
import net.highwayfrogs.editor.gui.texture.atlas.AtlasTexture;
import net.highwayfrogs.editor.system.math.Vector2f;

import java.util.List;

/**
 * Implements the main mesh node for rendering an RwClump.
 * Unfortunately, similar to Frogger: The Great Quest, the UVs are somewhat tough to deal with as these textures repeat themselves.
 * So, while most models will display fine, others such as water models must use a more sophisticated renderer.
 * TODO: Allow morphing.
 * TODO: Preview texCoord animations.
 * TODO: Can we get 3D models to show up in levels?
 * TODO: Figure out how to properly handle frames and atomics, as I think they are a key to properly setting things up.
 *  -> Are models duplicated for each level?
 * TODO: Texture resolution should be a little more flexible, there are tons of models and maps which don't resolve textures that I think are registered, just not in the limited places FrogLord checks.
 * Created by Kneesnap on 8/26/2024.
 */
public class RwClumpMeshNode extends DynamicMeshAdapterNode<RwGeometryChunk> {
    private int texCoordSetIndex;
    private int morphTargetIndex;
    private final Vector2f tempVector = new Vector2f();

    public RwClumpMeshNode(RwClumpMesh mesh) {
        super(mesh);
    }

    @Override
    public RwClumpMesh getMesh() {
        return (RwClumpMesh) super.getMesh();
    }

    @Override
    protected void onAddedToMesh() {
        super.onAddedToMesh();

        // Setup buffers by traversing the tree.
        getMesh().getClump().getGeometryList().getGeometries().forEach(this::add);
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
        for (int i = 0; i < triangles.size(); i++) {
            RpTriangle triangle = triangles.get(i);

            PSXShadeTextureDefinition shadeDefinition = getMesh().getShadedTextureManager().getShadedTexture(triangle);
            AtlasTexture materialTexture = getMesh().getTextureAtlas().getTextureFromSourceOrFallback(shadeDefinition);

            for (int j = 0; j < triangle.getVertexIndices().length; j++) {
                RwTexCoords texCoords = texCoordSet.get(triangle.getVertexIndices()[j]);
                this.tempVector.setXY(texCoords.getU() % 1F, texCoords.getV() % 1F);
                Vector2f localUv = getMesh().getTextureAtlas().getUV(materialTexture, this.tempVector, this.tempVector);
                entry.addTexCoordValue(localUv);
            }

            int uvStartIndex = entry.getTexCoordStartIndex() + (i * RpTriangle.VERTEX_COUNT);
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
        int faceCount = geometry.getTriangles().size();
        if (localTexCoordIndex < 0 || localTexCoordIndex >= RpTriangle.VERTEX_COUNT * faceCount)
            throw new IllegalArgumentException("Unsupported local texCoordIndex " + localTexCoordIndex);

        // Calculate index information.
        int localVertexIndex = (localTexCoordIndex % RpTriangle.VERTEX_COUNT);
        int triangleIndex = localTexCoordIndex / RpTriangle.VERTEX_COUNT;
        List<RwTexCoords> texCoordSet = getTexCoordSet(geometry);

        // Calculate material texture.
        RpTriangle triangle = geometry.getTriangles().get(triangleIndex);
        PSXShadeTextureDefinition shadeDefinition = getMesh().getShadedTextureManager().getShadedTexture(triangle);
        AtlasTexture materialTexture = getMesh().getTextureAtlas().getTextureFromSourceOrFallback(shadeDefinition);

        // Write updated texCoord.
        RwTexCoords texCoords = texCoordSet.get(triangle.getVertexIndices()[localVertexIndex]);
        this.tempVector.setXY(texCoords.getU() % 1F, texCoords.getV() % 1F);
        Vector2f localUv = getMesh().getTextureAtlas().getUV(materialTexture, this.tempVector, this.tempVector);
        entry.writeTexCoordValue(localTexCoordIndex, localUv);
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