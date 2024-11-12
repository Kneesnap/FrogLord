package net.highwayfrogs.editor.games.konami.greatquest.ui.mesh.map.manager.entity;

import javafx.scene.Node;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.*;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.highwayfrogs.editor.games.konami.greatquest.GreatQuestUtils;
import net.highwayfrogs.editor.games.konami.greatquest.chunks.kcCResourceEntityInst;
import net.highwayfrogs.editor.games.konami.greatquest.chunks.kcCResourceTriMesh;
import net.highwayfrogs.editor.games.konami.greatquest.entity.*;
import net.highwayfrogs.editor.games.konami.greatquest.entity.kcWaypointDesc.kcWaypointType;
import net.highwayfrogs.editor.games.konami.greatquest.math.kcSphere;
import net.highwayfrogs.editor.games.konami.greatquest.proxy.kcProxyCapsuleDesc;
import net.highwayfrogs.editor.games.konami.greatquest.proxy.kcProxyDesc;
import net.highwayfrogs.editor.games.konami.greatquest.proxy.kcProxyTriMeshDesc;
import net.highwayfrogs.editor.games.konami.greatquest.ui.mesh.map.manager.GreatQuestEntityManager;
import net.highwayfrogs.editor.games.konami.greatquest.ui.mesh.map.manager.GreatQuestEntityManager.GreatQuestMapModelMeshCollection;
import net.highwayfrogs.editor.games.konami.greatquest.ui.mesh.map.manager.GreatQuestMapCollisionManager.GreatQuestMapCollisionMesh;
import net.highwayfrogs.editor.games.konami.greatquest.ui.mesh.model.GreatQuestActionSequencePlayback;
import net.highwayfrogs.editor.games.konami.greatquest.ui.mesh.model.GreatQuestModelMesh;
import net.highwayfrogs.editor.gui.mesh.DynamicMesh;
import net.highwayfrogs.editor.utils.FXUtils;
import net.highwayfrogs.editor.utils.Scene3DUtils;

import java.util.Comparator;

/**
 * Represents a Great Quest entity displayed in the editor, and any additional 3D data with its representation.
 * TODO: Sort collision representations by area.
 * Created by Kneesnap on 4/18/2024.
 */
@Getter
@RequiredArgsConstructor
public class GreatQuestMapEditorEntityDisplay {
    private final GreatQuestEntityManager entityManager;
    private final GreatQuestModelMesh modelMesh;
    private final kcCResourceEntityInst entityInstance;
    private final GreatQuestMapModelMeshCollection modelViews;
    private final GreatQuestActionSequencePlayback sequencePlayback;
    private Node collisionPreview;
    private Shape3D boundingSpherePreview;

    private static final PhongMaterial BOUNDING_OBB_MATERIAL = Scene3DUtils.makeHighlightOverlayMaterial(Color.RED);
    private static final PhongMaterial BOUNDING_SPHERE_MATERIAL = Scene3DUtils.makeHighlightOverlayMaterial(Color.BLUE);
    private static final PhongMaterial WAYPOINT_BOUNDING_SPHERE_MATERIAL = Scene3DUtils.makeHighlightOverlayMaterial(Color.YELLOW);
    private static final PhongMaterial WAYPOINT_ALTERNATE_BOUNDING_SPHERE_MATERIAL = Scene3DUtils.makeHighlightOverlayMaterial(Color.PURPLE);
    private static final PhongMaterial PROXY_SPHERE_MATERIAL = Scene3DUtils.makeHighlightOverlayMaterial(Color.LIMEGREEN);

    private static final Comparator<? super Node> SORT_BY_VOLUME = Comparator.comparingDouble(FXUtils::calculateVolume);

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
        if (this.boundingSpherePreview != null) {
            if (hasWaypointBoundingBox()) {
                boolean isSelectedEntity = this.entityManager.getSelectedValue() == this.entityInstance;
                this.boundingSpherePreview.setVisible(visible && (isSelectedEntity || this.entityManager.getShowBoundingBoxCheckBox().isSelected()));
            } else {
                this.boundingSpherePreview.setVisible(visible && this.entityManager.getShowBoundingSphereCheckBox().isSelected());
            }
        }
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
        return waypointDesc.getType() == kcWaypointType.BOUNDING_BOX;
    }

    /**
     * Gets the 3D entity description for the entity, if it has one.
     */
    public kcEntity3DDesc getEntity3DDescription() {
        kcEntityInst entityInstance = this.entityInstance != null ? this.entityInstance.getInstance() : null;
        return entityInstance != null ? entityInstance.getDescription() : null;
    }

    /**
     * Gets the collision proxy description for the entity, if it has any.
     */
    public kcProxyDesc getCollisionProxyDescription() {
        kcEntity3DDesc entity3DDesc = getEntity3DDescription();
        if (!(entity3DDesc instanceof kcActorBaseDesc))
            return null;

        return ((kcActorBaseDesc) entity3DDesc).getCollisionProxyDescription();
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
        kcWaypointType type = entity3DDesc instanceof kcWaypointDesc ? ((kcWaypointDesc) entity3DDesc).getType() : null;
        if (type == kcWaypointType.BOUNDING_BOX) {
            kcWaypointDesc waypointDesc = ((kcWaypointDesc) entity3DDesc);
            // Leave it up to the Great Quest to store collision data in a field named & typed as a color. Sigh.
            // Reference: kcCWaypoint::Init, kcCWaypoint::UpdateRectangularParameters, kcCWaypoint::Intersects
            double xMagnitude = Math.max(kcWaypointDesc.MINIMUM_BOUNDING_BOX_SIZE, Math.min(kcWaypointDesc.MAXIMUM_BOUNDING_BOX_SIZE, waypointDesc.getBoundingBoxDimensions().getX()));
            double yMagnitude = Math.max(kcWaypointDesc.MINIMUM_BOUNDING_BOX_SIZE, Math.min(kcWaypointDesc.MAXIMUM_BOUNDING_BOX_SIZE, waypointDesc.getBoundingBoxDimensions().getY()));
            double zMagnitude = Math.max(kcWaypointDesc.MINIMUM_BOUNDING_BOX_SIZE, Math.min(kcWaypointDesc.MAXIMUM_BOUNDING_BOX_SIZE, waypointDesc.getBoundingBoxDimensions().getZ()));
            newShape = new Box(xMagnitude * 2, yMagnitude * 2, zMagnitude * 2);
            newShape.setMaterial(BOUNDING_OBB_MATERIAL);
        } else {
            newShape = new Sphere(boundingSphere.getRadius());
            if (type == kcWaypointType.BOUNDING_SPHERE) {
                newShape.setMaterial(WAYPOINT_BOUNDING_SPHERE_MATERIAL);
            } else if (type == kcWaypointType.APPLY_WATER_CURRENT) {
                newShape.setMaterial(WAYPOINT_ALTERNATE_BOUNDING_SPHERE_MATERIAL);
            } else { // A non-waypoint entity.
                newShape.setMaterial(BOUNDING_SPHERE_MATERIAL);
            }
        }

        newShape.setMouseTransparent(true);
        this.entityManager.getController().getMainLight().getScope().add(newShape);
        this.entityManager.getBoundingShapeDisplayList().add(newShape, SORT_BY_VOLUME); // Sorting ensures smaller collision shapes appear inside larger ones.
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
        this.entityManager.getBoundingShapeDisplayList().remove(this.boundingSpherePreview);
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
            kcCResourceTriMesh triMesh = ((kcProxyTriMeshDesc) proxyDesc).getMeshRef().getResource();
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
        if (this.entityInstance == null || !(this.entityInstance.getInstance() instanceof kcEntity3DInst))
            return;

        // Calculate rotation.
        kcEntity3DInst entity3D = (kcEntity3DInst) this.entityInstance.getInstance();
        kcEntity3DDesc entityDesc = entity3D.getDescription();
        boolean hasSkeleton = (entityDesc instanceof kcActorBaseDesc) && ((kcActorBaseDesc) entityDesc).getSkeleton() != null; // kcCActorBase::Render() will choose which method to render with based on if the skeleton is set or not.
        double xRotation = entity3D.getRotation().getX();
        double yRotation = entity3D.getRotation().getY();
        double zRotation = entity3D.getRotation().getZ();
        boolean isCollisionNode = (node == this.collisionPreview);
        boolean isBoundingSphereNode = (node == this.boundingSpherePreview);

        if (this.collisionPreview != null && (node == null || isCollisionNode)) // NOTE: The behavior of rotations has not been fully understood. Eg: We've not found the exact code cause for this in the original code yet.
            GreatQuestUtils.setEntityRotation(this.collisionPreview, xRotation, yRotation, zRotation, false);
        if (this.boundingSpherePreview != null && (node == null || isBoundingSphereNode))
            GreatQuestUtils.setEntityRotation(this.boundingSpherePreview, xRotation, yRotation, zRotation, false);

        if (this.modelViews != null && !isCollisionNode && !isBoundingSphereNode) {
            // Pick all or a single mesh view.
            if (node == null) {
                this.modelViews.setRotation(xRotation, yRotation, zRotation, hasSkeleton);
            } else if (node instanceof MeshView) {
                GreatQuestUtils.setEntityRotation(node, xRotation, yRotation, zRotation, hasSkeleton);
            }
        }
    }

    /**
     * Setup the position, scale, rotation, and other basic setup, for a node
     * @param node the node to setup
     */
    public void setupNode(Node node) {
        kcEntityInst entityInstance = this.entityInstance != null ? this.entityInstance.getInstance() : null;
        if (!(entityInstance instanceof kcEntity3DInst))
            return; // No 3D information to apply.

        kcEntity3DInst entity3D = (kcEntity3DInst) entityInstance;
        setNodePosition(node, entity3D.getPosition().getX(), entity3D.getPosition().getY(), entity3D.getPosition().getZ());
        setNodeScale(node, entity3D.getScale().getX(), entity3D.getScale().getY(), entity3D.getScale().getZ());
        updateRotation(node);
    }
}