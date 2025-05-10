package net.highwayfrogs.editor.utils;

import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.Point3D;
import javafx.scene.*;
import javafx.scene.image.Image;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Box;
import javafx.scene.shape.TriangleMesh;
import javafx.scene.shape.VertexFormat;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Scale;
import javafx.scene.transform.Transform;
import javafx.scene.transform.Translate;
import net.highwayfrogs.editor.FrogLordApplication;
import net.highwayfrogs.editor.games.generic.GameInstance;
import net.highwayfrogs.editor.gui.editor.FirstPersonCamera;
import net.highwayfrogs.editor.gui.mesh.DynamicMesh.DynamicMeshTextureQuality;
import net.highwayfrogs.editor.gui.mesh.fxobject.TranslationGizmo;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Contains static utilties useful in a 3D Scene.
 * Created by Kneesnap on 1/7/2024.
 */
public class Scene3DUtils {
    private static final PhongMaterial TRANSPARENT_MATERIAL = makeUnlitSharpMaterial(javafx.scene.paint.Color.TRANSPARENT);
    private static final double AXIS_PLANE_SIZE = 10000;
    private static final double VERTICAL_BOX_SIZE = 20;

    /**
     * Creates an axis plane used for flat mouse-picking.
     * @param node the node to create the axis plane relative to
     * @param group the group to add the axis plane to
     * @param camera the camera the user is viewing from
     * @param axis the axis of the plane to create
     * @return newAxisPlaneNode
     */
    public static Box createAxisPlane(Node node, Group group, FirstPersonCamera camera, Point3D axis) {
        if (Objects.equals(axis, Rotate.X_AXIS)) {
            return createAxisPlane(node, group, AXIS_PLANE_SIZE, 0, AXIS_PLANE_SIZE);
        } else if (Objects.equals(axis, Rotate.Y_AXIS)) {
            if (camera != null) {
                Box plane = createAxisPlane(node, group, AXIS_PLANE_SIZE, AXIS_PLANE_SIZE, 0);

                // Angle the plane towards the camera.
                Point3D gizmoPos = node.localToScene(0, 0, 0);
                double relativeX = camera.getCamPosXProperty().get() - gizmoPos.getX();
                double relativeZ = camera.getCamPosZProperty().get() - gizmoPos.getZ();
                double angle = Rotate.Z_AXIS.angle(relativeX, 0, relativeZ);
                if (relativeX < 0)
                    angle = -angle;

                plane.getTransforms().add(new Rotate(angle, Rotate.Y_AXIS));
                return plane;
            } else {
                // Fallback option is to create a vertical box.
                return createAxisPlane(node, group, VERTICAL_BOX_SIZE, AXIS_PLANE_SIZE, VERTICAL_BOX_SIZE);
            }
        } else if (Objects.equals(axis, Rotate.Z_AXIS)) {
            return createAxisPlane(node, group, AXIS_PLANE_SIZE, 0, AXIS_PLANE_SIZE);
        } else {
            // Axis unrecognized.
            throw new IllegalArgumentException("Unrecognized axis " + axis);
        }
    }

    private static Box createAxisPlane(Node node, Group group, double boxWidth, double boxHeight, double boxDepth) {
        // Add axis-aligned plane.
        Box axisPlane = new Box(boxWidth, boxHeight, boxDepth);
        axisPlane.setMaterial(TRANSPARENT_MATERIAL);
        axisPlane.setDepthTest(DepthTest.DISABLE);

        // Add translation.
        Translate translate = Scene3DUtils.getOptional3DTranslation(node);
        if (translate != null)
            axisPlane.getTransforms().add(translate);

        // Add rotation.
        Rotate rotation = Scene3DUtils.getOptional3DRotation(node);
        if (rotation != null)
            axisPlane.getTransforms().add(rotation);

        group.getChildren().add(axisPlane);
        return axisPlane;
    }

    /**
     * Search the provided Scene recursively for a SubScene.
     * @param scene scene to start searching from
     * @return The identified SubScene 3D group, if there was one.
     */
    public static Group getSubSceneGroup(Scene scene) {
        return scene != null ? getSubSceneGroup(scene.getRoot()) : null;
    }

    /**
     * Search the provided node recursively for a SubScene.
     * @param root node to start searching from
     * @return The identified SubScene 3D group, if there was one.
     */
    public static Group getSubSceneGroup(Parent root) {
        if (root == null)
            return null;

        List<Parent> nodesToVisit = new ArrayList<>();
        nodesToVisit.add(root);

        while (nodesToVisit.size() > 0) {
            Parent parent = nodesToVisit.remove(nodesToVisit.size() - 1);
            for (Node node : parent.getChildrenUnmodifiable()) {
                if (node instanceof SubScene && ((SubScene) node).getRoot() instanceof Group)
                    return (Group) ((SubScene) node).getRoot();
                if (node instanceof Parent)
                    nodesToVisit.add((Parent) node);
            }
        }

        return null;
    }

    /**
     * Search the provided Scene recursively for a SubScene.
     * @param scene scene to search
     * @return The identified SubScene, if there was one.
     */
    public static SubScene getSubScene(Scene scene) {
        return scene != null ? getSubScene(scene.getRoot()) : null;
    }

    /**
     * Search the provided node recursively for a SubScene.
     * @param root node to start searching from
     * @return The identified SubScene, if there was one.
     */
    public static SubScene getSubScene(Parent root) {
        if (root == null)
            return null;

        List<Parent> nodesToVisit = new ArrayList<>();
        nodesToVisit.add(root);

        while (nodesToVisit.size() > 0) {
            Parent parent = nodesToVisit.remove(nodesToVisit.size() - 1);
            for (Node node : parent.getChildrenUnmodifiable()) {
                if (node instanceof SubScene)
                    return ((SubScene) node);
                if (node instanceof Parent)
                    nodesToVisit.add((Parent) node);
            }
        }

        return null;
    }

    /**
     * Gets a Point3D representing a translation.
     * @param translate translation to convert
     */
    public static Point3D convertTranslationToPoint(Translate translate) {
        return translate != null ? new Point3D(translate.getX(), translate.getY(), translate.getZ()) : null;
    }

    /**
     * Gets the 3D position of the node.
     * @param node node to get the position from
     */
    public static Point3D get3DPosition(Node node) {
        if (node == null)
            return null;

        Translate translate = getOptional3DTranslation(node);
        if (translate != null)
            return convertTranslationToPoint(translate);

        return new Point3D(node.getTranslateX(), node.getTranslateY(), node.getTranslateZ());
    }

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
     * Applies the updated position to the node.
     * @param node the node to apply
     * @param newX the new x position
     * @param newY the new y position
     * @param newZ the new z position
     * @param flags the translation gizmo flags
     */
    public static void updateNodePosition(Node node, double newX, double newY, double newZ, int flags) {
        Translate nodePos = Scene3DUtils.get3DTranslation(node);
        if ((flags & TranslationGizmo.X_CHANGED_FLAG) == TranslationGizmo.X_CHANGED_FLAG)
            nodePos.setX(newX);
        if ((flags & TranslationGizmo.Y_CHANGED_FLAG) == TranslationGizmo.Y_CHANGED_FLAG)
            nodePos.setY(newY);
        if ((flags & TranslationGizmo.Z_CHANGED_FLAG) == TranslationGizmo.Z_CHANGED_FLAG)
            nodePos.setZ(newZ);
    }

    /**
     * Gets (or creates) the scale transform of a node in 3D space.
     * Creates the transform if it does not exist.
     * @param node node to get the scale from
     */
    public static Scale get3DScale(Node node) {
        return get3DScale(node, true);
    }

    /**
     * Gets (or creates) the scale transform of a node in 3D space.
     * Returns null if it does not exist.
     * @param node node to get the scale from
     */
    public static Scale getOptional3DScale(Node node) {
        return get3DScale(node, false);
    }

    /**
     * Gets (or creates) the scale transform of a node in 3D space.
     * @param node            node to get the scale from
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
     * @param node node to update the scale of
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
     * @param node node to update the pivot position of
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
     * @param node node to get the rotation from
     */
    public static Rotate get3DRotation(Node node) {
        return get3DRotation(node, true);
    }

    /**
     * Gets (or creates) the rotation transform of a node in 3D space.
     * Returns null if it does not exist.
     * @param node node to get the rotation from
     */
    public static Rotate getOptional3DRotation(Node node) {
        return get3DRotation(node, false);
    }

    /**
     * Gets (or creates) the rotation transform of a node in 3D space.
     * @param node            node to get the rotation from
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

    /**
     * Gets (or creates) the rotation transform of a node in 3D space.
     * Creates the transform if it does not exist.
     * @param node node to update the rotation of
     */
    public static Rotate get3DRotationWithAxis(Node node, Point3D axis) {
        return get3DRotationWithAxis(node, axis, true);
    }

    /**
     * Gets (or creates) the rotation transform of a node in 3D space.
     * Returns null if it does not exist.
     * @param node node to update the position of
     */
    public static Rotate getOptional3DRotationWithAxis(Node node, Point3D axis) {
        return get3DRotationWithAxis(node, axis, false);
    }

    /**
     * Gets (or creates) the rotation transform of a node in 3D space.
     * @param node            node to update the rotation of
     * @param createIfMissing whether the transform should be created if it doesn't exist
     */
    public static Rotate get3DRotationWithAxis(Node node, Point3D withAxis, boolean createIfMissing) {
        if (node == null)
            return null;

        for (Transform transform : node.getTransforms())
            if (transform instanceof Rotate && Objects.equals(((Rotate) transform).getAxis(), withAxis))
                return (Rotate) transform;

        if (createIfMissing) {
            Rotate newRotate = new Rotate(0, withAxis);
            node.getTransforms().add(newRotate);
            return newRotate;
        }

        return null;
    }

    /**
     * Set the rotation of a node in 3D space.
     * @param node node to update the rotation of
     * @param axis the axis to apply rotation to
     * @param angle the angle to apply
     */
    public static void setNodeAxisRotation(Node node, Point3D axis, double angle) {
        Rotate rotate = get3DRotationWithAxis(node, axis);
        rotate.setAngle(angle);
    }

    /**
     * Set the rotation of a node in 3D space.
     * @param node node to update the rotation of
     * @param x x angle in radians
     * @param y y angle in radians
     * @param z z angle in radians
     */
    public static void setNodeRotation(Node node, double x, double y, double z) {
        Rotate xRotation = null;
        Rotate yRotation = null;
        Rotate zRotation = null;
        for (Transform transform : node.getTransforms()) {
            if (!(transform instanceof Rotate))
                continue;

            Rotate rotate = (Rotate) transform;
            if (Rotate.X_AXIS.equals(rotate.getAxis())) {
                xRotation = rotate;
            } else if (Rotate.Y_AXIS.equals(rotate.getAxis())) {
                yRotation = rotate;
            } else if (Rotate.Z_AXIS.equals(rotate.getAxis())) {
                zRotation = rotate;
            }
        }

        if (zRotation != null) {
            zRotation.setAngle(Math.toDegrees(z));
        } else {
            zRotation = new Rotate(Math.toDegrees(z), Rotate.Z_AXIS);
            node.getTransforms().add(zRotation);
        }

        if (yRotation != null) {
            yRotation.setAngle(Math.toDegrees(y));
        } else {
            yRotation = new Rotate(Math.toDegrees(y), Rotate.Y_AXIS);
            node.getTransforms().add(yRotation);
        }

        if (xRotation != null) {
            xRotation.setAngle(Math.toDegrees(x));
        } else {
            xRotation = new Rotate(Math.toDegrees(x), Rotate.X_AXIS);
            node.getTransforms().add(xRotation);
        }
    }

    /**
     * Set the rotation pivot of a node in 3D space.
     * @param node node to update the rotation pivot of
     * @param x x world pivot position
     * @param y y world pivot position
     * @param z z world pivot position
     */
    public static void setNodeRotationPivot(Node node, double x, double y, double z) {
        Rotate xRotation = null;
        Rotate yRotation = null;
        Rotate zRotation = null;
        for (Transform transform : node.getTransforms()) {
            if (!(transform instanceof Rotate))
                continue;

            Rotate rotate = (Rotate) transform;
            if (Rotate.X_AXIS.equals(rotate.getAxis())) {
                xRotation = rotate;
            } else if (Rotate.Y_AXIS.equals(rotate.getAxis())) {
                yRotation = rotate;
            } else if (Rotate.Z_AXIS.equals(rotate.getAxis())) {
                zRotation = rotate;
            }
        }

        if (zRotation == null) {
            zRotation = new Rotate(0, Rotate.Z_AXIS);
            node.getTransforms().add(zRotation);
        }
        zRotation.setPivotX(x);
        zRotation.setPivotY(y);
        zRotation.setPivotZ(z);

        if (yRotation == null) {
            yRotation = new Rotate(0, Rotate.Y_AXIS);
            node.getTransforms().add(yRotation);
        }
        yRotation.setPivotX(x);
        yRotation.setPivotY(y);
        yRotation.setPivotZ(z);

        if (xRotation == null) {
            xRotation = new Rotate(0, Rotate.X_AXIS);
            node.getTransforms().add(xRotation);
        }
        xRotation.setPivotX(x);
        xRotation.setPivotY(y);
        xRotation.setPivotZ(z);
    }

    /**
     * Creates a highlighted material from another material.
     * @param material the material to create the highlighted material from
     * @return highlightedMaterial
     */
    public static PhongMaterial createHighlightMaterial(PhongMaterial material) {
        if (material == null)
            throw new NullPointerException("material");

        Image diffuseMap = material.getDiffuseMap();
        if (diffuseMap == null)
            throw new IllegalArgumentException("Cannot apply highlighting to a null diffuse map.");

        return updateHighlightMaterial(null, SwingFXUtils.fromFXImage(diffuseMap, null));
    }

    /**
     * Updates a highlighted material
     * @param material the material to update
     * @param rawTexture the image to apply highlighting to
     * @return highlightedMaterial
     */
    public static PhongMaterial updateHighlightMaterial(PhongMaterial material, BufferedImage rawTexture) {
        // Setup graphics.
        BufferedImage highlightedImage = new BufferedImage(rawTexture.getWidth(), rawTexture.getHeight(), BufferedImage.TYPE_INT_ARGB); // Using the rawTexture's type can cause weird color patterns in Frogger: The Great Quest entities.
        Graphics2D g = highlightedImage.createGraphics();
        try {
            // Clean image.
            g.setBackground(new Color(255, 255, 255, 0));
            g.clearRect(0, 0, highlightedImage.getWidth(), highlightedImage.getHeight());

            // Draw new image.
            g.drawImage(rawTexture, 0, 0, rawTexture.getWidth(), rawTexture.getHeight(), null);
            g.setColor(new Color(200, 200, 0, 128));
            g.fillRect(0, 0, highlightedImage.getWidth(), highlightedImage.getHeight());
        } finally {
            g.dispose();
        }

        if (material == null) {
            material = makeLitBlurryMaterial(FXUtils.toFXImage(highlightedImage, false));
            return material;
        }

        // Update material image.
        material.setDiffuseMap(FXUtils.toFXImage(highlightedImage, false));
        return material;
    }

    /**
     * Updates a highlighted material
     * @param material the material to update
     * @param fxImage the image to apply highlighting to
     * @return highlightedMaterial
     */
    public static PhongMaterial updateHighlightMaterial(PhongMaterial material, Image fxImage) {
        if (fxImage == null)
            throw new NullPointerException("fxImage");

        // Setup graphics.
        BufferedImage highlightedImage = SwingFXUtils.fromFXImage(fxImage, null);
        Graphics2D g = highlightedImage.createGraphics();
        try {
            // Clean image.
            g.setBackground(new Color(255, 255, 255, 0));
            g.setColor(new Color(200, 200, 0, 127));
            g.fillRect(0, 0, highlightedImage.getWidth(), highlightedImage.getHeight());
        } finally {
            g.dispose();
        }

        if (material == null) {
            material = makeLitBlurryMaterial(FXUtils.toFXImage(highlightedImage, false));
            return material;
        }

        // Update material image.
        material.setDiffuseMap(FXUtils.toFXImage(highlightedImage, false));
        return material;
    }

    /**
     * Creates a TriangleMesh (square, vertically aligned) to display a 2D sprite.
     * @param spriteSize the size of the sprite
     */
    public static TriangleMesh createSpriteMesh(float spriteSize) {
        // NOTE: Maybe this could be a single tri mesh, local to this manager, and we just update its points in updateEntities().
        TriangleMesh triMesh = new TriangleMesh(VertexFormat.POINT_TEXCOORD);
        return applySpriteMeshData(triMesh, spriteSize);
    }

    /**
     * Configures a TriangleMesh (square, vertically aligned) to display a 2D sprite.
     * @param triangleMesh the mesh to configure
     * @param spriteSize the size of the sprite
     */
    public static TriangleMesh applySpriteMeshData(TriangleMesh triangleMesh, float spriteSize) {
        if (triangleMesh == null)
            throw new NullPointerException("triangleMesh");
        if (!Float.isFinite(spriteSize) || spriteSize <= 0)
            throw new IllegalArgumentException("Invalid spriteSize: " + spriteSize);
        if (triangleMesh.getVertexFormat() != VertexFormat.POINT_TEXCOORD)
            throw new RuntimeException("The give vertex format is not currently supported!");

        // NOTE: Maybe this could be a single tri mesh, local to this manager, and we just update its points in updateEntities().
        triangleMesh.getPoints().setAll(-spriteSize * 0.5f, spriteSize * 0.5f, 0, -spriteSize * 0.5f, -spriteSize * 0.5f, 0, spriteSize * 0.5f, -spriteSize * 0.5f, 0, spriteSize * 0.5f, spriteSize * 0.5f, 0);
        triangleMesh.getTexCoords().setAll(0, 1, 0, 0, 1, 0, 1, 1);
        triangleMesh.getFaces().setAll(0, 0, 1, 1, 2, 2, 2, 2, 3, 3, 0, 0);
        return triangleMesh;
    }

    /**
     * Creates an PhongMaterial which uses nearest-neighbor texture display and is unaffected by lighting.
     * When the transparency outlines look poorly, the solution is to resize the image to use a higher resolution (while also using nearest neighbor)
     * @param texture The texture to create.
     * @return phongMaterial
     */
    public static PhongMaterial makeUnlitSharpMaterial(Image texture) {
        return updateUnlitSharpMaterial(new PhongMaterial(), texture);
    }

    /**
     * Updates a PhongMaterial which uses nearest-neighbor texture display and is unaffected by lighting.
     * When the transparency outlines look poorly, the solution is to resize the image to use a higher resolution (while also using nearest neighbor)
     * @param material The material to update.
     * @param texture The texture to apply to the material.
     * @return phongMaterial
     */
    public static PhongMaterial updateUnlitSharpMaterial(PhongMaterial material, Image texture) {
        // DirectX / Direct3D
        // When the alpha in the diffuse map is zero, "discard" occurs, causing the pixel to be transparent, regardless of what the self-illumination map says.
        // At the end Mtl1PS.hlsl in JavaFX is when the pixel RGB from the self-illumination map is added, but importantly it ignores the self-illumination alpha.
        // Since the RGB value is added and not overridden, the color should be zero (black) so that the self-illumination map can provide the color.
        // To achieve this, we set the diffuse color to be black, to multiply out the RGB values while keeping alpha intact.
        // The alpha is obtained from the previous multiplication between the diffuse texture pixel and the diffuse color.
        // I assume that the reason self-illumination is not blurry but diffuse is blurry probably means the textures are configured differently.

        if (material.getDiffuseColor() != javafx.scene.paint.Color.BLACK)
            material.setDiffuseColor(javafx.scene.paint.Color.BLACK); // When this is set to the default (WHITE), the coloring looks wrong when combined with a self-illumination map, since it's combining the light from both sources.
        if (material.getDiffuseMap() != texture)
            material.setDiffuseMap(texture); // Setting the diffuse map this seems to enable transparency, where-as it will be the diffuse color if not set.
        if (material.getSelfIlluminationMap() != texture)
            material.setSelfIlluminationMap(texture); // When this is not present, the material becomes fully black, because the diffuse color is off. If the color is changed to white, then the image does display but it's blurry and still has the same transparency problems.
        return material;
    }

    /**
     * Make the phong material with the given quality setting.
     * @param texture the texture to create a material for
     * @param quality the quality of the material
     * @return newMaterial
     */
    public static PhongMaterial makePhongMaterial(Image texture, DynamicMeshTextureQuality quality) {
        switch (quality) {
            case LIT_BLURRY:
                return makeLitBlurryMaterial(texture);
            case UNLIT_SHARP:
                return makeUnlitSharpMaterial(texture);
            default:
                throw new IllegalArgumentException("Unsupported texture quality: " + quality);
        }
    }

    /**
     * Update the phong material with the given quality setting.
     * @param material the material to update
     * @param texture the texture to update the material with
     * @param quality the quality of the material
     * @return material
     */
    public static PhongMaterial updatePhongMaterial(PhongMaterial material, Image texture, DynamicMeshTextureQuality quality) {
        switch (quality) {
            case LIT_BLURRY:
                return updateLitBlurryMaterial(material, texture);
            case UNLIT_SHARP:
                return updateUnlitSharpMaterial(material, texture);
            default:
                throw new IllegalArgumentException("Unsupported texture quality: " + quality);
        }
    }

    /**
     * Creates a lit PhongMaterial which uses some kind of blurring/smoothing for the texture but is impacted by lighting.
     * @param texture The texture to create.
     * @return phongMaterial
     */
    public static PhongMaterial makeLitBlurryMaterial(Image texture) {
        return updateLitBlurryMaterial(new PhongMaterial(), texture);
    }

    /**
     * Updates a lit PhongMaterial which uses some kind of blurring/smoothing for the texture but is impacted by lighting.
     * @param texture The texture to create.
     * @return phongMaterial
     */
    public static PhongMaterial updateLitBlurryMaterial(PhongMaterial material, Image texture) {
        // The following is what would seem to intuitively work, and it does. But the image is also blurry, which doesn't seem to be the case with self-illumination.
        // However, self-illumination is unaffected by lighting.

        if (material.getDiffuseColor() != javafx.scene.paint.Color.WHITE)
            material.setDiffuseColor(javafx.scene.paint.Color.WHITE); // The default PhongMaterial diffuseColor is WHITE, which is what we want.
        if (material.getDiffuseMap() != texture)
            material.setDiffuseMap(texture);
        if (material.getSelfIlluminationMap() != null)
            material.setSelfIlluminationMap(null);
        return material;
    }

    /**
     * Creates a PhongMaterial with a texture affected by lighting, and using only diffuse components.
     * @param texture The texture to create.
     * @return phongMaterial
     */
    public static PhongMaterial makeDiffuseMaterial(Image texture) {
        PhongMaterial material = new PhongMaterial();
        material.setDiffuseMap(texture);
        return material;
    }

    /**
     * Creates a PhongMaterial with a color unaffected by lighting which can be used for highlight overlays.
     * @param color The color to create.
     * @return phongMaterial
     */
    public static PhongMaterial makeHighlightOverlayMaterial(javafx.scene.paint.Color color) {
        BufferedImage colorImage = new BufferedImage(10, 10, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = colorImage.createGraphics();
        graphics.setColor(ColorUtils.toAWTColor(color, (byte) 0x80));
        graphics.fillRect(0, 0, colorImage.getWidth(), colorImage.getHeight());
        graphics.dispose();

        return makeUnlitSharpMaterial(FXUtils.toFXImage(colorImage, false));
    }

    /**
     * Creates a PhongMaterial with a color unaffected by lighting.
     * @param color The color to create.
     * @return phongMaterial
     */
    public static PhongMaterial makeUnlitSharpMaterial(javafx.scene.paint.Color color) {
        return makeUnlitSharpMaterial(ColorUtils.makeFxColorImage(color));
    }

    /**
     * Takes a screenshot of a given SubScene.
     * @param subScene   The subScene to take a screenshot of.
     * @param namePrefix The file name prefix to save the image as.
     */
    public static void takeScreenshot(GameInstance instance, SubScene subScene, Scene scene, String namePrefix, boolean transparentBackground) {
        javafx.scene.paint.Paint subSceneColor = subScene.getFill();

        if (transparentBackground)
            subScene.setFill(javafx.scene.paint.Color.TRANSPARENT);

        SnapshotParameters snapshotParameters = new SnapshotParameters();
        snapshotParameters.setFill(javafx.scene.paint.Color.TRANSPARENT);

        WritableImage wImage = new WritableImage((int) subScene.getWidth(), (int) subScene.getHeight());
        BufferedImage sceneImage = SwingFXUtils.fromFXImage(subScene.snapshot(snapshotParameters, wImage), null);

        if (transparentBackground)
            subScene.setFill(subSceneColor);

        // Write to file.
        int id = -1;
        while (id++ < 10000) {
            String fileName = (namePrefix != null && namePrefix.length() > 0 ? namePrefix + "-" : "")
                    + StringUtils.padStringLeft(Integer.toString(id), 4, '0') + ".png";

            File testFile = new File(FrogLordApplication.getWorkingDirectory(), fileName);
            if (!testFile.exists()) {
                try {
                    ImageIO.write(sceneImage, "png", testFile);
                    break;
                } catch (IOException ex) {
                    try {
                        // Let user pick a directory (in case current working directory is not writeable)
                        File targetDirectory = FXUtils.promptChooseDirectory(instance, "Save Screenshot", true);
                        ImageIO.write(sceneImage, "png", new File(targetDirectory, fileName));
                        break;
                    } catch (IOException ex2) {
                        Utils.handleError(instance.getLogger(), ex2, true, "Failed to write screenshot to '%s'. (No permissions to write here?)", fileName);
                        return;
                    }
                }
            }
        }
    }
}