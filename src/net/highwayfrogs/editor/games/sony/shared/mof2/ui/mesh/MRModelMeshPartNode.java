package net.highwayfrogs.editor.games.sony.shared.mof2.ui.mesh;

import lombok.Getter;
import lombok.NonNull;
import net.highwayfrogs.editor.file.standard.IVector;
import net.highwayfrogs.editor.file.standard.SVector;
import net.highwayfrogs.editor.file.standard.psx.PSXMatrix;
import net.highwayfrogs.editor.games.sony.shared.mesh.SCPolygonAdapterNode;
import net.highwayfrogs.editor.games.sony.shared.mof2.MRModel;
import net.highwayfrogs.editor.games.sony.shared.mof2.animation.MRAnimatedMof;
import net.highwayfrogs.editor.games.sony.shared.mof2.animation.MRAnimatedMofXarAnimation;
import net.highwayfrogs.editor.games.sony.shared.mof2.animation.texture.MRMofTextureAnimationPolygonTarget;
import net.highwayfrogs.editor.games.sony.shared.mof2.mesh.MRMofPart;
import net.highwayfrogs.editor.games.sony.shared.mof2.mesh.MRMofPartCel;
import net.highwayfrogs.editor.games.sony.shared.mof2.mesh.MRMofPolygon;
import net.highwayfrogs.editor.system.math.Vector2f;

import java.util.ArrayList;
import java.util.List;

/**
 * Manages mesh data for a mesh part.
 * Created by Kneesnap on 2/21/2025.
 */
public class MRModelMeshPartNode extends SCPolygonAdapterNode<MRMofPolygon> {
    @Getter @NonNull private final MRMofPart mofPart;
    private final List<SVector> vertexCache = new ArrayList<>();
    private int lastVertexCacheAnimationTick = Integer.MAX_VALUE;

    public MRModelMeshPartNode(MRModelMesh mesh, MRMofPart mofPart) {
        super(mesh);
        this.mofPart = mofPart;
    }

    @Override
    public MRModelMesh getMesh() {
        return (MRModelMesh) super.getMesh();
    }

    @Override
    protected void onAddedToMesh() {
        super.onAddedToMesh();

        // Setup polygons.
        // First, setup the non-transparent polygons.
        for (MRMofPolygon polygon : this.mofPart.getOrderedPolygons())
            if (!polygon.isSemiTransparent())
                this.add(polygon);

        // Second, add the transparent polygons.
        for (MRMofPolygon polygon : this.mofPart.getOrderedPolygons())
            if (polygon.isSemiTransparent())
                this.add(polygon);
    }

    @Override
    protected DynamicMeshTypedDataEntry writeValuesToArrayAndCreateEntry(MRMofPolygon polygon) {
        DynamicMeshTypedDataEntry newEntry = new DynamicMeshTypedDataEntry(getMesh(), polygon);
        addPolygonDataToEntries(polygon, newEntry, newEntry);
        return newEntry;
    }

    @Override
    protected boolean getPolygonTextureCoordinate(MRMofPolygon polygon, int index, Vector2f result) {
        if (!polygon.getPolygonType().isTextured())
            return false; // No textured -> Can't get the UVs from here.

        polygon.getTextureUvs()[index].toSnappedVector(polygon.getDefaultTexture(), result);
        return true;
    }

    @Override
    public List<SVector> getAllVertices() {
        MRModel model = getMofPart().getParentMof().getModel();
        MRModelAnimationPlayer animationPlayer = getAnimationPlayer();
        int animationTick = animationPlayer.getAnimationTick();
        if (animationTick == this.lastVertexCacheAnimationTick)
            return this.vertexCache; // Use cache.

        MRMofPartCel partCel = this.mofPart.getPartCel(animationPlayer.getFlipbookAnimationId(), animationTick);

        this.vertexCache.clear();
        MRAnimatedMofXarAnimation xarAnimation = animationPlayer.getXarAnimation();
        if (xarAnimation != null) {
            MRAnimatedMof animatedMof = xarAnimation.getParentCelSet().getParentModelSet().getParentMof();
            PSXMatrix transformMatrix = animatedMof.getTransformMatrix(this.mofPart, xarAnimation, animationTick);
            for (SVector vertex : partCel.getVertices())
                this.vertexCache.add(new SVector(PSXMatrix.MRApplyMatrix(transformMatrix, vertex, new IVector())));
        } else {
            this.vertexCache.addAll(partCel.getVertices());
        }

        // Incomplete mofs (Primarily in prototypes) have a weird vertex.
        if (model.isWeirdFrogMOF())
            this.vertexCache.add(new SVector(0, 0, 0));

        this.lastVertexCacheAnimationTick = animationTick;
        return this.vertexCache;
    }

    @Override
    protected int[] getVertices(MRMofPolygon polygon) {
        return polygon.getVertices();
    }

    @Override
    protected int getVertexCount(MRMofPolygon polygon) {
        return polygon.getVertexCount();
    }

    /**
     * Update all animated polygons to display properly.
     */
    public void updateAnimatedPolygons() {
        // Update texture animations.
        getMesh().pushBatchOperations();
        this.lastVertexCacheAnimationTick = Integer.MAX_VALUE; // Because this forces an update, reset the cache.
        updateVertices();

        getMesh().getTextureAtlas().startBulkOperations();
        for (int i = 0; i < this.mofPart.getTextureAnimationPolygonTargets().size(); i++) {
            MRMofTextureAnimationPolygonTarget targetPolygon = this.mofPart.getTextureAnimationPolygonTargets().get(i);
            if (targetPolygon != null && targetPolygon.getPolygon() != null) {
                getMesh().getShadedTextureManager().updatePolygon(targetPolygon.getPolygon());
                updateTexCoords(targetPolygon.getPolygon());
            }
        }

        getMesh().getTextureAtlas().endBulkOperations();
        getMesh().popBatchOperations();
    }

    /**
     * Gets the active animation data.
     */
    public MRModelAnimationPlayer getAnimationPlayer() {
        return getMesh().getAnimationPlayer();
    }
}