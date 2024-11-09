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

    private static final float SIZE = .025f;
    private static final Vector3f[] BASE_VERTICES = {
            new Vector3f(0, -SIZE, SIZE), // Base Top Left
            new Vector3f(0, -SIZE, -SIZE), // Base Top Right
            new Vector3f(0, SIZE, SIZE), // Base Bottom Left
            new Vector3f(0, SIZE, -SIZE), // Base Bottom Right
            new Vector3f(4 * SIZE, 0, 0), // Tip
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

        // Add pyramid vertices.
        int vtxStartIndex = entry.getVertexStartIndex();
        for (int i = 0; i < BASE_VERTICES.length; i++)
            entry.addVertexValue(calculateVertexPos(bone, BASE_VERTICES[i]));

        // Add pyramid UV.
        entry.addTexCoordValue(evaluateUvs(bone));

        // Create pyramid.
        int uvIndex = entry.getTexCoordStartIndex();
        MeshUtils.addPyramidFaces(entry, false, vtxStartIndex, vtxStartIndex + 1, vtxStartIndex + 2, vtxStartIndex + 3, vtxStartIndex + 4, uvIndex);
        return entry;
    }

    @Override
    public void updateVertex(DynamicMeshTypedDataEntry entry, int localVertexIndex) {
        entry.writeVertexXYZ(localVertexIndex, calculateVertexPos(entry.getDataSource(), BASE_VERTICES[localVertexIndex]));
    }

    @Override
    public void updateTexCoord(DynamicMeshTypedDataEntry entry, int localTexCoordIndex) {
        entry.writeTexCoordValue(localTexCoordIndex, evaluateUvs(entry.getDataSource()));
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

        return getMesh().getTextureAtlas().getUV(boneTexture, this.tempUv.setXY(.5F, .5F), this.tempUv);
    }
}
