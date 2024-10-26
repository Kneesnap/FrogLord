package net.highwayfrogs.editor.gui.editor;

import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Box;
import javafx.scene.shape.MeshView;
import javafx.scene.shape.Shape3D;
import javafx.scene.transform.Transform;
import javafx.scene.transform.Translate;
import lombok.Getter;
import net.highwayfrogs.editor.file.standard.Vector;
import net.highwayfrogs.editor.games.konami.greatquest.math.kcVector4;
import net.highwayfrogs.editor.gui.mesh.DynamicMesh;
import net.highwayfrogs.editor.gui.mesh.fxobject.Arrow3D;
import net.highwayfrogs.editor.gui.mesh.fxobject.ScaleGizmo;
import net.highwayfrogs.editor.gui.mesh.fxobject.ScaleGizmo.IScaleChangeListener;
import net.highwayfrogs.editor.gui.mesh.fxobject.TranslationGizmo;
import net.highwayfrogs.editor.gui.mesh.fxobject.TranslationGizmo.IPositionChangeListener;
import net.highwayfrogs.editor.utils.Scene3DUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * The Mesh UI marker manager is a manager which manages the position of boxes that indicate a position in the world.
 * Created by Kneesnap on 9/26/2023.
 */
@Getter
public class MeshUIMarkerManager<TMesh extends DynamicMesh> extends MeshUIManager<TMesh> {
    private final DisplayList markerList;
    private final DisplayList gizmoMeshViews;
    private final Map<UUID, MeshView> translationGizmoViews = new HashMap<>();
    private final Map<UUID, MeshView> scaleGizmoViews = new HashMap<>();
    private Box boxVisualizer;
    private Arrow3D arrowVisualizer;
    private Vector showPosition;

    private static final double GENERIC_POS_SIZE = 3;
    private static final PhongMaterial GENERIC_POS_MATERIAL = Scene3DUtils.makeUnlitSharpMaterial(Color.YELLOW);

    public MeshUIMarkerManager(MeshViewController<TMesh> controller) {
        super(controller);
        this.markerList = controller.getRenderManager().createDisplayList();
        this.gizmoMeshViews = controller.getRenderManager().createDisplayList();
    }

    /**
     * Gets and updates a translation gizmo identified by the given identifier.
     * If there is no gizmo found with this identifier, null is returned.
     * @param identifier The gizmo identifier
     * @param x the x world position to place the gizmo at
     * @param y the y world position to place the gizmo at
     * @param z the z world position to place the gizmo at
     * @param listener the listener for position changes
     */
    public MeshView updateGizmo(UUID identifier, double x, double y, double z, IPositionChangeListener listener) {
        MeshView meshView = this.translationGizmoViews.get(identifier);
        if (meshView == null || !(meshView.getMesh() instanceof TranslationGizmo))
            return null; // Nothing to edit, abort!

        TranslationGizmo gizmo = (TranslationGizmo) meshView.getMesh();
        gizmo.setChangeListener(meshView, listener);
        gizmo.setPosition(meshView, x, y, z, false);
        return meshView;
    }

    /**
     * Gets and updates a translation gizmo identified by the given identifier.
     * If there is no gizmo found with this identifier, null is returned.
     * @param identifier The gizmo identifier
     * @param x the x world position to place the gizmo at
     * @param y the y world position to place the gizmo at
     * @param z the z world position to place the gizmo at
     * @param listener the listener for position changes
     */
    public MeshView updateGizmo(UUID identifier, double x, double y, double z, double scaleX, double scaleY, double scaleZ, IScaleChangeListener listener) {
        MeshView meshView = this.scaleGizmoViews.get(identifier);
        if (meshView == null || !(meshView.getMesh() instanceof ScaleGizmo))
            return null; // Nothing to edit, abort!

        ScaleGizmo gizmo = (ScaleGizmo) meshView.getMesh();
        gizmo.setChangeListener(meshView, listener);
        Scene3DUtils.setNodePosition(meshView, x, y, z);
        gizmo.setScaleX(scaleX, false);
        gizmo.setScaleY(scaleY, false);
        gizmo.setScaleZ(scaleZ, false);
        return meshView;
    }

    /**
     * Disables a translation gizmo identified by the given identifier.
     * @param identifier the gizmo identifier
     */
    public MeshView removeGizmo(UUID identifier) {
        MeshView meshView = onGizmoRemove(this.translationGizmoViews.remove(identifier));
        if (meshView != null)
            return meshView;

        meshView = this.scaleGizmoViews.remove(identifier);
        return meshView;
    }

    private MeshView onGizmoRemove(MeshView meshView) {
        if (meshView != null) {
            this.gizmoMeshViews.remove(meshView); // Remove gizmo.
            getController().getMainLight().getScope().remove(meshView);
        }

        return meshView;
    }

    /**
     * Enables or disables a translation gizmo identified by the given identifier.
     * @param identifier The gizmo identifier
     * @param x the x world position to place the gizmo at
     * @param y the y world position to place the gizmo at
     * @param z the z world position to place the gizmo at
     * @param listener the listener for position changes
     */
    public MeshView toggleGizmo(UUID identifier, double x, double y, double z, IPositionChangeListener listener) {
        if (removeGizmo(identifier) != null)
            return null;

        MeshView newView = new MeshView();
        TranslationGizmo newGizmo = new TranslationGizmo();
        newGizmo.addView(newView, getController(), listener);
        newGizmo.setPosition(newView, x, y, z, false);
        getController().getMainLight().getScope().add(newView);
        this.gizmoMeshViews.add(newView);
        this.translationGizmoViews.put(identifier, newView);
        return newView;
    }

    /**
     * Enables or disables a translation gizmo identified by the given identifier.
     * @param identifier The gizmo identifier
     * @param x the x world position to place the gizmo at
     * @param y the y world position to place the gizmo at
     * @param z the z world position to place the gizmo at
     * @param listener the listener for position changes
     */
    public MeshView toggleGizmo(UUID identifier, double x, double y, double z, double scaleX, double scaleY, double scaleZ, IScaleChangeListener listener) {
        if (removeGizmo(identifier) != null)
            return null;

        MeshView newView = new MeshView();
        ScaleGizmo newGizmo = new ScaleGizmo();
        newGizmo.addView(newView, getController().getFirstPersonCamera(), listener);
        Scene3DUtils.setNodePosition(newView, x, y, z);
        newGizmo.setScaleX(scaleX, false);
        newGizmo.setScaleY(scaleY, false);
        newGizmo.setScaleZ(scaleZ, false);
        getController().getMainLight().getScope().add(newView);
        this.gizmoMeshViews.add(newView);
        this.translationGizmoViews.put(identifier, newView);
        return newView;
    }

    /**
     * Updates the marker to display at the given position.
     * If null is supplied, it'll get removed.
     */
    public void updateMarker(Vector vec, int bits, Vector origin, Shape3D visualRepresentative) {
        this.showPosition = vec;

        if (vec == null)
            return;

        // Get position.
        float baseX = vec.getFloatX(bits);
        float baseY = vec.getFloatY(bits);
        float baseZ = vec.getFloatZ(bits);
        if (origin != null) {
            baseX += origin.getFloatX();
            baseY += origin.getFloatY();
            baseZ += origin.getFloatZ();
        }

        // Use existing visual representation if none other exists.
        if (visualRepresentative == null && this.boxVisualizer != null)
            visualRepresentative = this.boxVisualizer;

        // Update existing visualization, or create new one.
        if (visualRepresentative != null) {
            if (visualRepresentative.getTransforms() != null) {
                for (Transform transform : visualRepresentative.getTransforms()) {
                    if (!(transform instanceof Translate))
                        continue;

                    Translate translate = (Translate) transform;
                    translate.setX(baseX);
                    translate.setY(baseY);
                    translate.setZ(baseZ);
                }
            } else {
                visualRepresentative.setTranslateX(baseX);
                visualRepresentative.setTranslateY(baseY);
                visualRepresentative.setTranslateZ(baseZ);
            }
        } else {
            this.boxVisualizer = this.markerList.addBoundingBoxFromMinMax(baseX - GENERIC_POS_SIZE, baseY - GENERIC_POS_SIZE, baseZ - GENERIC_POS_SIZE, baseX + GENERIC_POS_SIZE, baseY + GENERIC_POS_SIZE, baseZ + GENERIC_POS_SIZE, GENERIC_POS_MATERIAL, true);
        }
    }

    /**
     * Updates the marker to display at the given position.
     * If null is supplied, it'll get removed.
     */
    public void updateArrow(Vector startPos, Vector offset, int bits) {
        if (this.arrowVisualizer == null) {
            this.arrowVisualizer = new Arrow3D();
            this.markerList.add(this.arrowVisualizer);
        }

        this.arrowVisualizer.getStartPosition().setX(startPos.getFloatX(bits));
        this.arrowVisualizer.getStartPosition().setY(startPos.getFloatY(bits));
        this.arrowVisualizer.getStartPosition().setZ(startPos.getFloatZ(bits));
        this.arrowVisualizer.getEndPosition().setX(startPos.getFloatX(bits) + offset.getFloatX(bits));
        this.arrowVisualizer.getEndPosition().setY(startPos.getFloatY(bits) + offset.getFloatY(bits));
        this.arrowVisualizer.getEndPosition().setZ(startPos.getFloatZ(bits) + offset.getFloatZ(bits));
        this.arrowVisualizer.updatePositionAndRotation();
    }

    /**
     * Updates the marker to display at the given position.
     * If null is supplied, it'll get removed.
     */
    public void updateArrow(kcVector4 startPosition, kcVector4 offset) {
        updateArrow(startPosition.getX(), startPosition.getY(), startPosition.getZ(), offset.getX(), offset.getY(), offset.getZ());
    }

    /**
     * Updates the marker to display at the given position.
     * If null is supplied, it'll get removed.
     */
    public void updateArrow(double x, double y, double z, double offsetX, double offsetY, double offsetZ) {
        if (this.arrowVisualizer == null) {
            this.arrowVisualizer = new Arrow3D();
            this.markerList.add(this.arrowVisualizer);
        }

        this.arrowVisualizer.getStartPosition().setX((float) x);
        this.arrowVisualizer.getStartPosition().setY((float) y);
        this.arrowVisualizer.getStartPosition().setZ((float) z);
        this.arrowVisualizer.getEndPosition().setX((float) (x + offsetX));
        this.arrowVisualizer.getEndPosition().setY((float) (y + offsetY));
        this.arrowVisualizer.getEndPosition().setZ((float) (z + offsetZ));
        this.arrowVisualizer.updatePositionAndRotation();
    }
}