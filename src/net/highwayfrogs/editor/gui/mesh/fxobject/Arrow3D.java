package net.highwayfrogs.editor.gui.mesh.fxobject;

import javafx.geometry.Point3D;
import javafx.scene.Group;
import javafx.scene.paint.Color;
import javafx.scene.paint.Material;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.CullFace;
import javafx.scene.shape.Cylinder;
import javafx.scene.shape.DrawMode;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Transform;
import javafx.scene.transform.Translate;
import lombok.Getter;
import net.highwayfrogs.editor.system.math.Vector3f;
import net.highwayfrogs.editor.utils.Utils;

/**
 * Represents a 3D arrow.
 * TODO: Add a top to the arrow.
 * Created by Kneesnap on 12/23/2023.
 */
@Getter
public class Arrow3D extends Group {
    private final double thickness;
    private final Vector3f startPosition;
    private final Vector3f endPosition;
    private final Cylinder line;

    private static final double DEFAULT_THICKNESS = 1;
    private static final PhongMaterial DEFAULT_MATERIAL = Utils.makeSpecialMaterial(Color.RED);
    private static final Point3D Y_AXIS = new Point3D(0f, 1f, 0f);

    public Arrow3D() {
        this(DEFAULT_THICKNESS);
    }

    public Arrow3D(double thickness) {
        this(thickness, new Vector3f(), new Vector3f());
    }

    public Arrow3D(Vector3f startPosition, Vector3f endPosition) {
        this(DEFAULT_THICKNESS, startPosition, endPosition);
    }

    public Arrow3D(double thickness, Vector3f startPosition, Vector3f endPosition) {
        this.thickness = thickness;
        this.startPosition = startPosition;
        this.endPosition = endPosition;

        // Setup cylinder / line.
        this.line = new Cylinder(this.thickness, 1, 3);
        this.line.setMaterial(DEFAULT_MATERIAL);
        this.line.setDrawMode(DrawMode.FILL);
        this.line.setCullFace(CullFace.BACK);
        this.line.setMouseTransparent(false);
        getChildren().add(this.line);
        updatePositionAndRotation();
    }

    /**
     * Sets the arrow material.
     * @param material The material to apply to the arrow.
     */
    public void setMaterial(Material material) {
        this.line.setMaterial(material);
    }

    /**
     * Update the position and rotation of the line.
     */
    public void updatePositionAndRotation() {
        final Point3D p0 = new Point3D(this.startPosition.getX(), this.startPosition.getY(), this.startPosition.getZ());
        final Point3D p1 = new Point3D(this.endPosition.getX(), this.endPosition.getY(), this.endPosition.getZ());
        final Point3D diff = p1.subtract(p0);
        final double length = diff.magnitude();

        final Point3D mid = p1.midpoint(p0);
        final Point3D axisOfRotation = diff.crossProduct(Y_AXIS);
        final double angle = Math.acos(diff.normalize().dotProduct(Y_AXIS));

        // Find existing.
        Rotate existingRotate = null;
        Translate existingTranslate = null;
        for (Transform transform : this.line.getTransforms()) {
            if (transform instanceof Rotate)
                existingRotate = (Rotate) transform;
            if (transform instanceof Translate)
                existingTranslate = (Translate) transform;
        }

        // Create transform & rotation if they don't exist.
        if (existingTranslate == null)
            this.line.getTransforms().add(existingTranslate = new Translate());
        if (existingRotate == null)
            this.line.getTransforms().add(existingRotate = new Rotate());

        // Update rotation.
        existingRotate.setAngle(-Math.toDegrees(angle));
        existingRotate.setAxis(axisOfRotation);

        // Update position.
        existingTranslate.setX(mid.getX());
        existingTranslate.setY(mid.getY());
        existingTranslate.setZ(mid.getZ());

        // Update length.
        this.line.setHeight(length);
    }
}