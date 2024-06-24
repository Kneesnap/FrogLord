package net.highwayfrogs.editor.games.sony.frogger.map.mesh;

import lombok.Getter;
import net.highwayfrogs.editor.file.standard.SVector;
import net.highwayfrogs.editor.games.sony.frogger.map.FroggerMapFile;
import net.highwayfrogs.editor.games.sony.frogger.map.data.animation.FroggerMapAnimation;
import net.highwayfrogs.editor.games.sony.frogger.map.data.animation.FroggerMapAnimationTargetPolygon;
import net.highwayfrogs.editor.games.sony.frogger.map.packets.FroggerMapFilePacketAnimation;
import net.highwayfrogs.editor.games.sony.shared.mesh.SCPolygonAdapterNode;
import net.highwayfrogs.editor.system.math.Vector2f;

import java.util.List;

/**
 * Manages the terrain mesh data from a FroggerMapFile.
 * Created by Kneesnap on 5/28/2024.
 */
@Getter
public class FroggerMapMeshNode extends SCPolygonAdapterNode<FroggerMapPolygon> {
    private int animationTickCounter = ANIMATIONS_DISABLED;
    private static final int ANIMATIONS_DISABLED = -1;

    public FroggerMapMeshNode(FroggerMapMesh mesh) {
        super(mesh);
    }

    @Override
    public FroggerMapMesh getMesh() {
        return (FroggerMapMesh) super.getMesh();
    }

    @Override
    protected void onAddedToMesh() {
        super.onAddedToMesh();

        // Setup polygons.
        // First, setup the non-transparent polygons.
        for (FroggerMapPolygon polygon : getMap().getPolygonPacket().getPolygons())
            if (!polygon.isSemiTransparent())
                this.add(polygon);

        // Second, add the transparent polygons.
        for (FroggerMapPolygon polygon : getMap().getPolygonPacket().getPolygons())
            if (polygon.isSemiTransparent())
                this.add(polygon);
    }

    @Override
    protected DynamicMeshTypedDataEntry writeValuesToArrayAndCreateEntry(FroggerMapPolygon polygon) {
        DynamicMeshTypedDataEntry newEntry = new DynamicMeshTypedDataEntry(getMesh(), polygon);
        addPolygonDataToEntries(polygon, newEntry, newEntry);
        return newEntry;
    }

    @Override
    protected boolean getPolygonTextureCoordinate(FroggerMapPolygon polygon, int index, Vector2f result) {
        if (!polygon.getPolygonType().isTextured())
            return false; // No textured -> Can't get the UVs from here.

        result = polygon.getTextureUvs()[index].toVector(result);

        // Apply the animation uv offset.
        FroggerMapAnimation animation = getMesh().getAnimation(polygon);
        if (animation != null)
            result.add(polygon.getOffsetU(animation, this.animationTickCounter), polygon.getOffsetV(animation, this.animationTickCounter));

        return true;
    }

    @Override
    public List<SVector> getAllVertices() {
        return getMap().getVertexPacket().getVertices();
    }

    @Override
    protected int[] getVertices(FroggerMapPolygon polygon) {
        return polygon.getVertices();
    }

    @Override
    protected int getVertexCount(FroggerMapPolygon polygon) {
        return polygon.getVertexCount();
    }

    /**
     * Gets the map file which mesh data comes from.
     */
    public FroggerMapFile getMap() {
        return getMesh().getMap();
    }

    /**
     * Ticks the map animations.
     */
    public void tickMapAnimations() {
        this.animationTickCounter++;
        updateAnimatedPolygons();
    }

    /**
     * Ticks the map animations.
     */
    public void clearMapAnimations() {
        if (this.animationTickCounter == ANIMATIONS_DISABLED)
            return;

        this.animationTickCounter = ANIMATIONS_DISABLED;
        updateAnimatedPolygons();
    }

    /**
     * Update all animated polygons to display properly.
     */
    public void updateAnimatedPolygons() {
        // Update all pickups to show updated animation states.
        FroggerMapFilePacketAnimation animationPacket = getMap().getAnimationPacket();
        if (animationPacket == null || !animationPacket.isActive())
            return; // Animations aren't available.

        List<FroggerMapAnimation> animations = animationPacket.getAnimations();

        // Update texture sheet.
        getMesh().pushBatchOperations();
        getMesh().getTextureAtlas().startBulkOperations();
        for (int i = 0; i < animations.size(); i++) {
            FroggerMapAnimation animation = animations.get(i);
            for (int j = 0; j < animation.getTargetPolygons().size(); j++) {
                FroggerMapAnimationTargetPolygon targetPolygon = animation.getTargetPolygons().get(j);
                if (targetPolygon != null && targetPolygon.getPolygon() != null)
                    getMesh().getShadedTextureManager().updatePolygon(targetPolygon.getPolygon());
            }
        }
        getMesh().getTextureAtlas().endBulkOperations();
        getMesh().popBatchOperations();
    }

    /**
     * Tests if an animation is active for the given polygon
     * @param polygon the polygon to test
     * @return animationActive
     */
    public boolean isAnimationActive(FroggerMapPolygon polygon) {
        return this.animationTickCounter != ANIMATIONS_DISABLED && getMesh().getAnimation(polygon) != null;
    }
}