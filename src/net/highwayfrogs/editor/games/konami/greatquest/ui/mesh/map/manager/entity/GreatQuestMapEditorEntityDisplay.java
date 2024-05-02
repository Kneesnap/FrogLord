package net.highwayfrogs.editor.games.konami.greatquest.ui.mesh.map.manager.entity;

import javafx.scene.Node;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.*;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.highwayfrogs.editor.games.konami.greatquest.entity.*;
import net.highwayfrogs.editor.games.konami.greatquest.math.kcSphere;
import net.highwayfrogs.editor.games.konami.greatquest.proxy.kcProxyCapsuleDesc;
import net.highwayfrogs.editor.games.konami.greatquest.proxy.kcProxyDesc;
import net.highwayfrogs.editor.games.konami.greatquest.proxy.kcProxyTriMeshDesc;
import net.highwayfrogs.editor.games.konami.greatquest.toc.kcCResourceEntityInst;
import net.highwayfrogs.editor.games.konami.greatquest.toc.kcCResourceTriMesh;
import net.highwayfrogs.editor.games.konami.greatquest.ui.mesh.map.manager.GreatQuestEntityManager;
import net.highwayfrogs.editor.games.konami.greatquest.ui.mesh.map.manager.GreatQuestEntityManager.GreatQuestMapModelMeshCollection;
import net.highwayfrogs.editor.games.konami.greatquest.ui.mesh.map.manager.GreatQuestMapCollisionManager.GreatQuestMapCollisionMesh;
import net.highwayfrogs.editor.gui.mesh.DynamicMesh;
import net.highwayfrogs.editor.utils.Scene3DUtils;
import net.highwayfrogs.editor.utils.Utils;

/**
 * Represents a Great Quest entity displayed in the editor, and any additional 3D data with its representation.
 * Created by Kneesnap on 4/18/2024.
 */
@Getter
@RequiredArgsConstructor
public class GreatQuestMapEditorEntityDisplay {
    private final GreatQuestEntityManager entityManager;
    private final kcCResourceEntityInst entityInstance;
    private final GreatQuestMapModelMeshCollection modelViews;
    private Node collisionPreview;
    private Shape3D boundingSpherePreview;

    private static final PhongMaterial BOUNDING_OBB_MATERIAL = Utils.makeHighlightOverlayMaterial(Color.RED);
    private static final PhongMaterial BOUNDING_SPHERE_MATERIAL = Utils.makeHighlightOverlayMaterial(Color.BLUE);
    private static final PhongMaterial PROXY_SPHERE_MATERIAL = Utils.makeHighlightOverlayMaterial(Color.LIMEGREEN);

    /**
     * Setup the entity display.
     */
    public void setup() {
        for (int i = 0; i < this.modelViews.getMeshViews().size(); i++)
            setupNode(this.modelViews.getMeshViews().get(i)); // Show entity mesh.
        makeCollisionPreview();
        makeBoundingSphere(); // Larger than the collision preview, and transparent, so it should be created after the collision preview.
    }

    /**
     * Set whether the display is visible.
     * @param visible the desired visibility state
     */
    public void setVisible(boolean visible) {
        if (this.collisionPreview != null)
            this.collisionPreview.setVisible(visible && this.entityManager.getShowCollisionCheckBox().isSelected());
        if (this.boundingSpherePreview != null)
            this.boundingSpherePreview.setVisible(visible && (hasWaypointBoundingBox() ? this.entityManager.getShowBoundingBoxCheckBox().isSelected() : this.entityManager.getShowBoundingSphereCheckBox().isSelected()));
        for (int i = 0; i < this.modelViews.getMeshViews().size(); i++)
            this.modelViews.getMeshViews().get(i).setVisible(visible && this.entityManager.getShowEntityMeshCheckBox().isSelected());
    }

    /**
     * Test if this display has a waypoint bounding box type.
     */
    public boolean hasWaypointBoundingBox() {
        kcEntity3DDesc entity3DDesc = getEntity3DDescription();
        if (!(entity3DDesc instanceof kcWaypointDesc))
            return false; // Not a waypoint!

        kcWaypointDesc waypointDesc = (kcWaypointDesc) entity3DDesc;
        return waypointDesc.getType() == 0 && waypointDesc.getSubType() == 1;
    }

    /**
     * Gets the 3D entity description for the entity, if it has one.
     */
    public kcEntity3DDesc getEntity3DDescription() {
        kcEntityInst entityInstance = this.entityInstance != null ? this.entityInstance.getEntity() : null;
        return entityInstance != null ? entityInstance.getDescription(this.entityManager.getMap()) : null;
    }

    /**
     * Gets the collision proxy description for the entity, if it has any.
     */
    public kcProxyDesc getCollisionProxyDescription() {
        kcEntity3DDesc entity3DDesc = getEntity3DDescription();
        if (!(entity3DDesc instanceof kcActorBaseDesc))
            return null;

        return ((kcActorBaseDesc) entity3DDesc).getCollisionProxyDescription(this.entityManager.getMap());
    }

    /**
     * Creates a new bounding sphere.
     */
    public Node makeBoundingSphere() {
        removeBoundingSphere();

        kcEntity3DDesc entity3DDesc = getEntity3DDescription();
        if (entity3DDesc == null)
            return null;

        kcSphere boundingSphere = entity3DDesc.getBoundingSphere();
        if (boundingSphere == null)
            return null;

        Shape3D newShape;
        if (entity3DDesc instanceof kcWaypointDesc && ((kcWaypointDesc) entity3DDesc).getType() == 0 && ((kcWaypointDesc) entity3DDesc).getSubType() == 1) {
            kcWaypointDesc waypointDesc = ((kcWaypointDesc) entity3DDesc);
            // Leave it up to the Great Quest to store collision data in a field named & typed as a color. Sigh.
            // Reference: kcCWaypoint::Init, kcCWaypoint::UpdateRectangularParameters, kcCWaypoint::Intersects
            double xMagnitude = Math.max(.05, Math.min(64, waypointDesc.getColor().getRed()));
            double yMagnitude = Math.max(.05, Math.min(64, waypointDesc.getColor().getGreen()));
            double zMagnitude = Math.max(.05, Math.min(64, waypointDesc.getColor().getBlue()));
            newShape = new Box(xMagnitude * 2, yMagnitude * 2, zMagnitude * 2);
            newShape.setMaterial(BOUNDING_OBB_MATERIAL);
        } else {
            newShape = new Sphere(boundingSphere.getRadius());
            newShape.setMaterial(BOUNDING_SPHERE_MATERIAL);
        }

        newShape.setMouseTransparent(true);
        this.entityManager.getController().getMainLight().getScope().add(newShape);
        this.entityManager.getBoundingSphereDisplayList().add(newShape);
        this.boundingSpherePreview = newShape; // Do before calling setupNode()
        setupNode(newShape);

        return newShape;
    }

    /**
     * Removes the bounding sphere from existence.
     */
    public void removeBoundingSphere() {
        if (this.boundingSpherePreview == null)
            return;

        this.entityManager.getController().getMainLight().getScope().remove(this.boundingSpherePreview);
        this.entityManager.getBoundingSphereDisplayList().remove(this.boundingSpherePreview);
        this.boundingSpherePreview = null;
    }

    /**
     * Creates a new collision preview.
     */
    public Node makeCollisionPreview() {
        removeCollisionPreview();

        Node result = null;
        kcProxyDesc proxyDesc = getCollisionProxyDescription();
        if (proxyDesc instanceof kcProxyCapsuleDesc) { // TODO: VERIFY LOOKS GOOD
            kcProxyCapsuleDesc proxyCapsuleDesc = (kcProxyCapsuleDesc) proxyDesc;
            Cylinder cylinder = new Cylinder(proxyCapsuleDesc.getProcessedRadius(), proxyCapsuleDesc.getProcessedRadius() * 2);
            cylinder.setMaterial(PROXY_SPHERE_MATERIAL);
            cylinder.setMouseTransparent(true);
            result = cylinder;
        } else if (proxyDesc instanceof kcProxyTriMeshDesc) {
            kcCResourceTriMesh triMesh = ((kcProxyTriMeshDesc) proxyDesc).getTriMesh(this.entityManager.getMap());
            if (triMesh != null) {
                GreatQuestMapCollisionMesh collisionMesh = new GreatQuestMapCollisionMesh(null, triMesh);
                MeshView view = new MeshView();
                view.setCullFace(CullFace.NONE);
                collisionMesh.addView(view);
                result = view;
            }
        }

        if (result != null) {
            this.entityManager.getController().getMainLight().getScope().add(result);
            this.entityManager.getCollisionPreviewDisplayList().add(result);
            this.collisionPreview = result; // Do before calling setupNode()
            setupNode(result);
        }

        return result;
    }

    /**
     * Removes the collision preview from existence.
     */
    public void removeCollisionPreview() {
        if (this.collisionPreview == null)
            return;

        if (this.collisionPreview instanceof MeshView) {
            MeshView previewView = (MeshView) this.collisionPreview;
            this.entityManager.getController().getMainLight().getScope().remove(previewView);
            if (previewView.getMesh() instanceof DynamicMesh)
                ((DynamicMesh) previewView.getMesh()).removeView(previewView);
        }

        this.entityManager.getCollisionPreviewDisplayList().remove(this.collisionPreview);
        this.collisionPreview = null;
    }

    /**
     * Sets the position the display is shown at
     * @param x world x position
     * @param y world y position
     * @param z world z position
     */
    public void setPosition(double x, double y, double z) {
        if (this.collisionPreview != null)
            setNodePosition(this.collisionPreview, x, y, z);
        if (this.boundingSpherePreview != null)
            setNodePosition(this.boundingSpherePreview, x, y, z);
        if (this.modelViews != null)
            this.modelViews.setPosition(x, y, z);
    }

    /**
     * Sets the position the display is shown at
     * @param node the node to apply position for
     * @param x world x position
     * @param y world y position
     * @param z world z position
     */
    private void setNodePosition(Node node, double x, double y, double z) {
        if (node == this.collisionPreview) {
            kcProxyDesc proxyDesc = getCollisionProxyDescription();
            if (proxyDesc instanceof kcProxyCapsuleDesc) {
                // This is a proxy capsule, so we apply the offset.
                Scene3DUtils.setNodeRotationPivot(node, x, y, z);

                kcProxyCapsuleDesc capsuleDesc = (kcProxyCapsuleDesc) proxyDesc;
                y += capsuleDesc.getProcessedRadius() + capsuleDesc.getOffset() + (capsuleDesc.getLength() / 2); // TODO: Not sure if this is right.
            }
        } else if (node == this.boundingSpherePreview) {
            // If this is a bounding sphere, apply the bounding sphere offset.
            kcEntity3DDesc entityDescription = getEntity3DDescription();
            if (entityDescription != null) {
                Scene3DUtils.setNodeRotationPivot(node, x, y, z);
                x += entityDescription.getBoundingSphere().getPosition().getX();
                y += entityDescription.getBoundingSphere().getPosition().getY();
                z += entityDescription.getBoundingSphere().getPosition().getZ();
            }
        }

        Scene3DUtils.setNodePosition(node, x, y, z);
    }

    /**
     * Sets the display scale
     * @param x x scale
     * @param y y scale
     * @param z z scale
     */
    public void setScale(double x, double y, double z) {
        if (this.collisionPreview != null)
            setNodeScale(this.collisionPreview, x, y, z);
        if (this.boundingSpherePreview != null)
            setNodeScale(this.boundingSpherePreview, x, y, z);
        if (this.modelViews != null)
            this.modelViews.setScale(x, y, z);
    }

    /**
     * Sets the scale to display a node with
     * @param node the node to apply scale for
     * @param x world x scale
     * @param y world y scale
     * @param z world z scale
     */
    private void setNodeScale(Node node, double x, double y, double z) {
        Scene3DUtils.setNodeScale(node, x, y, z);
    }

    /**
     * Updates the rotation of all nodes.
     */
    public void updateRotation() {
        updateRotation(null);
    }

    /**
     * Updates the rotation of a single node.
     * If null is supplied, all nodes will be updated.
     */
    public void updateRotation(Node node) {
        if (this.entityInstance == null || !(this.entityInstance.getEntity() instanceof kcEntity3DInst))
            return;

        // Calculate rotation.
        kcEntity3DInst entity3D = (kcEntity3DInst) this.entityInstance.getEntity();
        kcEntity3DDesc entityDesc = entity3D.getDescription(this.entityManager.getMap());
        // TODO: Some objects if PI / 2 is subtracted from their X start looking correct. However, this breaks others. Need to figure out what's going on here.
        //  - It seems animated objects are usually what need this.
        //  - I bet the animations fix the rotations.
        boolean hasAnimationSet = (entityDesc instanceof kcActorBaseDesc) && ((kcActorBaseDesc) entityDesc).getAnimationSet(this.entityManager.getMap()) != null;
        boolean hasAnimation = (entityDesc instanceof kcActorBaseDesc) && ((kcActorBaseDesc) entityDesc).getAnimationHash() != 0 && ((kcActorBaseDesc) entityDesc).getAnimationHash() != -1;
        double xRotationOffset = Math.PI / 2;
        double xRotation = entity3D.getRotation().getX();
        double yRotation = entity3D.getRotation().getY();
        double zRotation = entity3D.getRotation().getZ();

        if (this.collisionPreview != null && (node == null || node == this.collisionPreview))
            Scene3DUtils.setNodeRotation(this.collisionPreview, xRotation + (hasAnimation ? 0 : xRotationOffset), yRotation, zRotation);
        if (this.boundingSpherePreview != null && (node == null || node == this.boundingSpherePreview))
            Scene3DUtils.setNodeRotation(this.boundingSpherePreview, xRotation, yRotation, zRotation);

        if (this.modelViews != null) {
            double offsetX = xRotation - (hasAnimationSet ? xRotationOffset : 0);

            // Pick all or a single mesh view.
            if (node == null) {
                this.modelViews.setRotation(offsetX, yRotation, zRotation);
            } else if (node instanceof MeshView) {
                int meshViewIndex = this.modelViews.getMeshViews().indexOf(node);
                if (meshViewIndex >= 0)
                    Scene3DUtils.setNodeRotation(this.modelViews.getMeshViews().get(meshViewIndex), offsetX, yRotation, zRotation);
            }
        }
    }

    /**
     * Setup the position, scale, rotation, and other basic setup, for a node
     * @param node the node to setup
     */
    public void setupNode(Node node) {
        kcEntityInst entityInstance = this.entityInstance != null ? this.entityInstance.getEntity() : null;
        if (!(entityInstance instanceof kcEntity3DInst))
            return; // No 3D information to apply.

        kcEntity3DInst entity3D = (kcEntity3DInst) entityInstance;
        setNodePosition(node, entity3D.getPosition().getX(), entity3D.getPosition().getY(), entity3D.getPosition().getZ());
        setNodeScale(node, entity3D.getScale().getX(), entity3D.getScale().getY(), entity3D.getScale().getZ());
        updateRotation(node);
    }
}