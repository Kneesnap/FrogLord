package net.highwayfrogs.editor.utils;

import javafx.scene.Node;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Scale;
import javafx.scene.transform.Transform;
import javafx.scene.transform.Translate;

/**
 * Contains static utilties useful in a 3D Scene.
 * Created by Kneesnap on 1/7/2024.
 */
public class Scene3DUtils {
    /**
     * Gets (or creates) the translation of a node in 3D space.
     * Creates the translation if it does not exist.
     * @param node node to update the position of
     */
    public static Translate get3DTranslation(Node node) {
        return get3DTranslation(node, true);
    }

    /**
     * Gets (or creates) the translation of a node in 3D space.
     * Returns null if it does not exist.
     * @param node node to update the position of
     */
    public static Translate getOptional3DTranslation(Node node) {
        return get3DTranslation(node, false);
    }

    /**
     * Gets (or creates) the translation of a node in 3D space.
     * @param node            node to update the position of
     * @param createIfMissing whether the translation should be created if it doesn't exist
     */
    public static Translate get3DTranslation(Node node, boolean createIfMissing) {
        if (node == null)
            return null;

        for (Transform transform : node.getTransforms())
            if (transform instanceof Translate)
                return (Translate) transform;

        if (createIfMissing) {
            Translate newTranslate = new Translate();
            node.getTransforms().add(newTranslate);
            return newTranslate;
        }

        // Not created.
        return null;
    }

    /**
     * Set the position of a node in 3D space.
     * @param node node to update the position of
     * @param x    x coordinate value
     * @param y    y coordinate value
     * @param z    z coordinate value
     */
    public static void setNodePosition(Node node, double x, double y, double z) {
        for (Transform transform : node.getTransforms()) {
            if (!(transform instanceof Translate))
                continue;

            Translate translate = (Translate) transform;
            translate.setX(x);
            translate.setY(y);
            translate.setZ(z);
            return;
        }

        node.getTransforms().add(new Translate(x, y, z));
    }

    /**
     * Gets (or creates) the scale transform of a node in 3D space.
     * Creates the transform if it does not exist.
     * @param node node to update the position of
     */
    public static Scale get3DScale(Node node) {
        return get3DScale(node, true);
    }

    /**
     * Gets (or creates) the scale transform of a node in 3D space.
     * Returns null if it does not exist.
     * @param node node to update the position of
     */
    public static Scale getOptional3DScale(Node node) {
        return get3DScale(node, false);
    }

    /**
     * Gets (or creates) the scale transform of a node in 3D space.
     * @param node            node to update the position of
     * @param createIfMissing whether the transform should be created if it doesn't exist
     */
    public static Scale get3DScale(Node node, boolean createIfMissing) {
        if (node == null)
            return null;

        for (Transform transform : node.getTransforms())
            if (transform instanceof Scale)
                return (Scale) transform;

        if (createIfMissing) {
            Scale newScale = new Scale(1, 1, 1);
            node.getTransforms().add(newScale);
            return newScale;
        }

        return null;
    }

    /**
     * Set the scale of a node in 3D space.
     * @param node node to update the position of
     * @param x    x-axis scalar value
     * @param y    y-axis scalar value
     * @param z    z-axis scalar value
     */
    public static void setNodeScale(Node node, double x, double y, double z) {
        for (Transform transform : node.getTransforms()) {
            if (!(transform instanceof Scale))
                continue;

            Scale scale = (Scale) transform;
            scale.setX(x);
            scale.setY(y);
            scale.setZ(z);
            return;
        }

        node.getTransforms().add(new Scale(x, y, z));
    }

    /**
     * Set the scale of a node in 3D space.
     * @param node node to update the position of
     * @param x    x-axis pivot position value
     * @param y    y-axis pivot position value
     * @param z    z-axis pivot position value
     */
    public static void setNodeScalePivotPosition(Node node, double x, double y, double z) {
        for (Transform transform : node.getTransforms()) {
            if (!(transform instanceof Scale))
                continue;

            Scale scale = (Scale) transform;
            scale.setPivotX(x);
            scale.setPivotY(y);
            scale.setPivotZ(z);
            return;
        }

        node.getTransforms().add(new Scale(x, y, z));
    }

    /**
     * Gets (or creates) the rotation transform of a node in 3D space.
     * Creates the transform if it does not exist.
     * @param node node to update the position of
     */
    public static Rotate get3DRotation(Node node) {
        return get3DRotation(node, true);
    }

    /**
     * Gets (or creates) the rotation transform of a node in 3D space.
     * Returns null if it does not exist.
     * @param node node to update the position of
     */
    public static Rotate getOptional3DRotation(Node node) {
        return get3DRotation(node, false);
    }

    /**
     * Gets (or creates) the rotation transform of a node in 3D space.
     * @param node            node to update the position of
     * @param createIfMissing whether the transform should be created if it doesn't exist
     */
    public static Rotate get3DRotation(Node node, boolean createIfMissing) {
        if (node == null)
            return null;

        for (Transform transform : node.getTransforms())
            if (transform instanceof Rotate)
                return (Rotate) transform;

        if (createIfMissing) {
            Rotate newRotate = new Rotate();
            node.getTransforms().add(newRotate);
            return newRotate;
        }

        return null;
    }
}
