package net.highwayfrogs.editor.games.konami.greatquest.ui.mesh.model;

import net.highwayfrogs.editor.games.konami.greatquest.chunks.kcCResourceSkeleton;
import net.highwayfrogs.editor.games.konami.greatquest.chunks.kcCResourceSkeleton.kcNode;
import net.highwayfrogs.editor.gui.mesh.DynamicMeshAdapterNode;
import net.highwayfrogs.editor.gui.mesh.fxobject.MeshUtils;
import net.highwayfrogs.editor.gui.texture.atlas.AtlasTexture;
import net.highwayfrogs.editor.system.math.Matrix4x4f;
import net.highwayfrogs.editor.system.math.Vector2f;
import net.highwayfrogs.editor.system.math.Vector3f;

/**
 * View the position of all nodes.
 * Created by Kneesnap on 10/9/2024.
 */
public class GreatQuestModelSkeletonMeshNode extends DynamicMeshAdapterNode<kcNode> {
    private final Vector2f tempUv = new Vector2f();
    private final Vector3f tempVertex = new Vector3f();

    private static final float SIZE = .00625f;
    private static final float NODE_LENGTH = 4 * SIZE;
    private static final Vector3f[] BASE_VERTICES = {
            new Vector3f(0, -SIZE, SIZE), // Base Top Left
            new Vector3f(0, -SIZE, -SIZE), // Base Top Right
            new Vector3f(0, SIZE, SIZE), // Base Bottom Left
            new Vector3f(0, SIZE, -SIZE), // Base Bottom Right
            new Vector3f(NODE_LENGTH, 0, 0), // Tip
    };

    private static final float OUTLINE_SIZE_MULTIPLIER = 1.2f;
    private static final float OUTLINE_SIZE = SIZE * OUTLINE_SIZE_MULTIPLIER;
    private static final float OUTLINE_X = (NODE_LENGTH * (OUTLINE_SIZE_MULTIPLIER - 1f)) * .25f;
    private static final Vector3f[] OUTLINE_VERTICES = {
            new Vector3f(-OUTLINE_X, -OUTLINE_SIZE, OUTLINE_SIZE), // Base Top Left
            new Vector3f(-OUTLINE_X, -OUTLINE_SIZE, -OUTLINE_SIZE), // Base Top Right
            new Vector3f(-OUTLINE_X, OUTLINE_SIZE, OUTLINE_SIZE), // Base Bottom Left
            new Vector3f(-OUTLINE_X, OUTLINE_SIZE, -OUTLINE_SIZE), // Base Bottom Right
            new Vector3f(NODE_LENGTH + OUTLINE_X, 0, 0), // Tip
    };

    public GreatQuestModelSkeletonMeshNode(GreatQuestModelSkeletonMesh mesh) {
        super(mesh);
    }

    @Override
    public GreatQuestModelSkeletonMesh getMesh() {
        return (GreatQuestModelSkeletonMesh) super.getMesh();
    }

    @Override
    protected void onAddedToMesh() {
        super.onAddedToMesh();

        // Setup vertex buffers matching this material.
        kcCResourceSkeleton skeleton = getMesh().getFullMesh().getSkeleton();
        if (skeleton != null)
            skeleton.getAllNodes().forEach(this::add);
    }

    @Override
    protected DynamicMeshTypedDataEntry writeValuesToArrayAndCreateEntry(kcNode bone) {
        DynamicMeshTypedDataEntry entry = new DynamicMeshTypedDataEntry(getMesh(), bone);

        // Add outline pyramid vertices.
        int vtxOutlineStartIndex = entry.getPendingVertexStartIndex();
        for (int i = 0; i < BASE_VERTICES.length; i++)
            entry.addVertexValue(calculateVertexPos(bone, OUTLINE_VERTICES[i]));

        // Add main pyramid vertices.
        int vtxMainStartIndex = vtxOutlineStartIndex + entry.getPendingVertexCount();
        for (int i = 0; i < BASE_VERTICES.length; i++)
            entry.addVertexValue(calculateVertexPos(bone, BASE_VERTICES[i]));

        // Add pyramid UV.
        entry.addTexCoordValue(evaluateUvs(bone));
        entry.addTexCoordValue(getColorTextureUv(getMesh().getOutlineBoneTexture()));

        // Create pyramid.
        int uvIndex = entry.getPendingTexCoordStartIndex();
        MeshUtils.addPyramidFaces(entry, false, vtxOutlineStartIndex, vtxOutlineStartIndex + 1, vtxOutlineStartIndex + 2, vtxOutlineStartIndex + 3, vtxOutlineStartIndex + 4, uvIndex + 1);
        MeshUtils.addPyramidFaces(entry, false, vtxMainStartIndex, vtxMainStartIndex + 1, vtxMainStartIndex + 2, vtxMainStartIndex + 3, vtxMainStartIndex + 4, uvIndex);
        return entry;
    }

    @Override
    public void updateVertex(DynamicMeshTypedDataEntry entry, int localVertexIndex) {
        Vector3f newVertexPos;
        if (localVertexIndex >= OUTLINE_VERTICES.length) {
            newVertexPos = calculateVertexPos(entry.getDataSource(), BASE_VERTICES[localVertexIndex - BASE_VERTICES.length]);
        } else {
            newVertexPos = calculateVertexPos(entry.getDataSource(), OUTLINE_VERTICES[localVertexIndex]);
        }

        entry.writeVertexXYZ(localVertexIndex, newVertexPos);
    }

    @Override
    public void updateTexCoord(DynamicMeshTypedDataEntry entry, int localTexCoordIndex) {
        if (localTexCoordIndex == 0) {
            entry.writeTexCoordValue(localTexCoordIndex, evaluateUvs(entry.getDataSource()));
        } else if (localTexCoordIndex == 1) {
            entry.writeTexCoordValue(localTexCoordIndex, getColorTextureUv(getMesh().getOutlineBoneTexture()));
        }
        // Unknown texCoord index otherwise.
    }

    private Vector3f calculateVertexPos(kcNode bone, Vector3f input) {
        Matrix4x4f boneTransform = getMesh().getFullMesh().getBoneTransform(bone.getTag());
        return boneTransform.multiply(input, this.tempVertex);
    }

    private Vector2f evaluateUvs(kcNode bone) {
        kcNode selectedBone = getMesh().getSelectedBone();

        AtlasTexture boneTexture = getMesh().getDefaultBoneTexture();
        if (selectedBone != null) {
            if (selectedBone == bone) {
                boneTexture = getMesh().getSelectedBoneTexture();
            } else if (selectedBone.getParent() == bone) {
                boneTexture = getMesh().getParentBoneTexture();
            } else if (selectedBone.getChildren().contains(bone)) {
                boneTexture = getMesh().getChildBoneTexture();
            }
        }

        return getColorTextureUv(boneTexture);
    }

    private Vector2f getColorTextureUv(AtlasTexture texture) {
        return getMesh().getTextureAtlas().getUV(texture, this.tempUv.setXY(.5F, .5F), this.tempUv);
    }
}
