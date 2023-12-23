package net.highwayfrogs.editor.gui.editor;

import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Box;
import javafx.scene.shape.Shape3D;
import javafx.scene.transform.Transform;
import javafx.scene.transform.Translate;
import lombok.Getter;
import net.highwayfrogs.editor.file.standard.Vector;
import net.highwayfrogs.editor.games.sony.shared.fxobject.Arrow3D;
import net.highwayfrogs.editor.gui.mesh.DynamicMesh;
import net.highwayfrogs.editor.utils.Utils;

/**
 * The Mesh UI marker manager is a manager which manages the position of boxes that indicate a position in the world.
 * Created by Kneesnap on 9/26/2023.
 */
@Getter
public class MeshUIMarkerManager<TMesh extends DynamicMesh> extends MeshUIManager<TMesh> {
    private final DisplayList markerList;
    private Box boxVisualizer;
    private Arrow3D arrowVisualizer;
    private Vector showPosition;

    private static final double GENERIC_POS_SIZE = 3;
    private static final PhongMaterial GENERIC_POS_MATERIAL = Utils.makeSpecialMaterial(Color.YELLOW);

    public MeshUIMarkerManager(MeshViewController<TMesh> controller) {
        super(controller);
        this.markerList = controller.getRenderManager().createDisplayList();
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
}