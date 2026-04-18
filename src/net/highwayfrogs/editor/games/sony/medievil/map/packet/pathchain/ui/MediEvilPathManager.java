package net.highwayfrogs.editor.games.sony.medievil.map.packet.pathchain.ui;

import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Cylinder;
import lombok.Getter;
import net.highwayfrogs.editor.games.psx.math.vector.SVector;
import net.highwayfrogs.editor.games.sony.SCMath;
import net.highwayfrogs.editor.games.sony.medievil.map.mesh.MediEvilMapMesh;
import net.highwayfrogs.editor.games.sony.medievil.map.mesh.MediEvilMapMeshController;
import net.highwayfrogs.editor.games.sony.medievil.map.packet.pathchain.MediEvilMapPathChain;
import net.highwayfrogs.editor.games.sony.medievil.map.packet.pathchain.ui.MediEvilPathManager.MediEvilPathPreview;
import net.highwayfrogs.editor.games.sony.medievil.map.packet.spline.IMediEvilMapSpline;
import net.highwayfrogs.editor.games.sony.medievil.map.packet.spline.MediEvilMap2DSpline;
import net.highwayfrogs.editor.games.sony.medievil.map.packet.spline.MediEvilMap3DSpline;
import net.highwayfrogs.editor.games.sony.medievil.map.ui.MediEvilMapUIManager.MediEvilMapListManager;
import net.highwayfrogs.editor.games.sony.shared.pathing.SCPositionTracker;
import net.highwayfrogs.editor.games.sony.shared.pathing.TrackedPositionEditor;
import net.highwayfrogs.editor.games.sony.shared.spline.MRBezierCurve;
import net.highwayfrogs.editor.games.sony.shared.spline.MRBezierCurve.MRBezierCurvePointType;
import net.highwayfrogs.editor.games.sony.shared.spline.MRSplineMatrix;
import net.highwayfrogs.editor.gui.editor.DisplayList;
import net.highwayfrogs.editor.gui.editor.MeshViewController;
import net.highwayfrogs.editor.utils.Scene3DUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Manages the UI for MediEvil pathing.
 * TODO: Improve path visuals.
 *  -> Color: Dark green / lightgreen to highlight camera vs paths counterpart previews. (Eg: If you pick a path, make sure its camera spline gets colored well)
 *  -> Make splines thinner, and also add spheres to indicate start/end points, like Frogger. On spline click, that's when you can show the gizmos.
 *  -> Fix the gizmos to be more uniform. Or at the very least, smaller.
 * TODO: Go over all todos in the map packet package.
 * TODO: Profile why it feels so slow to drag paths around.
 * Created by Kneesnap on 2/14/2026.
 */
public class MediEvilPathManager extends MediEvilMapListManager<MediEvilMapPathChain, MediEvilPathPreview> {
    private DisplayList pathDisplayList;
    private MediEvilSplinePreview selectedSplinePreview;
    private final SCPositionTracker pathPositionTracker;

    private static final PhongMaterial MATERIAL_SALMON = Scene3DUtils.makeUnlitSharpMaterial(Color.SALMON);
    private static final PhongMaterial MATERIAL_PINK = Scene3DUtils.makeUnlitSharpMaterial(Color.PINK);
    private static final PhongMaterial MATERIAL_SKYBLUE = Scene3DUtils.makeUnlitSharpMaterial(Color.SKYBLUE);
    private static final PhongMaterial MATERIAL_POWDERBLUE = Scene3DUtils.makeUnlitSharpMaterial(Color.POWDERBLUE);
    private static final PhongMaterial MATERIAL_LIGHT_GREEN = Scene3DUtils.makeUnlitSharpMaterial(Color.LIGHTGREEN);
    private static final PhongMaterial MATERIAL_LIGHT_YELLOW = Scene3DUtils.makeUnlitSharpMaterial(Color.YELLOW); // TODO: DELETE

    public MediEvilPathManager(MeshViewController<MediEvilMapMesh> controller) {
        super(controller);
        this.pathPositionTracker = new SCPositionTracker(controller);
    }

    @Override
    public void onSetup() {
        this.pathDisplayList = getRenderManager().createDisplayListWithNewGroup();
        super.onSetup();
    }

    /**
     * Sets the spline preview to be for the provided spline
     * @param spline the spline to select the preview for
     */
    public void setSelectedSpline(IMediEvilMapSpline spline) {
        if (this.selectedSplinePreview == null && spline == null)
            return;
        if (this.selectedSplinePreview != null && this.selectedSplinePreview.spline == spline)
            return;

        MediEvilPathPreview pathPreview = spline != null ? getDelegatesByValue().get(spline.getPathChain()) : null;
        setSelectedSplinePreview(pathPreview != null ? pathPreview.getSplinePreview(spline) : null);
    }

    /**
     * Sets the selected spline preview.
     * @param splinePreview the spline preview to apply
     */
    public void setSelectedSplinePreview(MediEvilSplinePreview splinePreview) {
        MediEvilSplinePreview oldPreview = this.selectedSplinePreview;
        if (oldPreview == splinePreview)
            return;

        // Select/deselect this spline.
        this.selectedSplinePreview = splinePreview;

        // Select in UI.
        if (splinePreview != null) {
            getValueSelectionBox().getSelectionModel().select(splinePreview.pathPreview.pathChainNode);
            expandTitlePaneFrom(getValueSelectionBox());
        }

        // Update path UI.
        updateEditor();

        // Update path preview. (Ensure correct materials)
        if (oldPreview != null)
            oldPreview.pathPreview.updatePreview();
        if (splinePreview != null)
            splinePreview.pathPreview.updatePreview();
    }

    @Override
    public String getTitle() {
        return "Paths";
    }

    @Override
    public String getValueName() {
        return "Path";
    }

    @Override
    public List<MediEvilMapPathChain> getValues() {
        return getMap().getPathChainPacket().getPathChainNodes();
    }

    @Override
    protected boolean tryAddValue(MediEvilMapPathChain pathChain) {
        return getMap().getPathChainPacket().addPathChain(pathChain);
    }

    @Override
    protected boolean tryRemoveValue(MediEvilMapPathChain pathChain) {
        return getMap().getPathChainPacket().removePathChain(pathChain);
    }

    @Override
    protected MediEvilPathPreview setupDisplay(MediEvilMapPathChain pathChain) {
        MediEvilPathPreview pathPreview = new MediEvilPathPreview(this, pathChain);
        pathPreview.addSplinePreviews();
        return pathPreview;
    }

    @Override
    protected void updateEditor(MediEvilMapPathChain selectedValue) {
        if (selectedValue != null) {
            // TODO: Allow seeing/changing group type.
            // TODO: Allow seeing how many splines there are, how many connections, etc.
        }

        // Try to show the spline UI.
        if (this.selectedSplinePreview == null || this.selectedSplinePreview.getSpline().getPathChain() != selectedValue)
            return;

        // Basic spline data editor.
        IMediEvilMapSpline spline = this.selectedSplinePreview.getSpline();
        getEditorGrid().addLabel("Unique ID", String.valueOf(spline.getUniqueId()));
        getEditorGrid().addLabel("Spline Index", String.valueOf(spline.getId()));
        spline.setupEditor(this, getEditorGrid());

        // Basic spline operations go here.
        // TODO: Add UI for selected spline, if one is selected.
        //  -> Show/edit basic information.
        //  -> Add new node extending from the end. (Remember, there's a max of 8 connections.)/
        //   -> A node extending from the end by default should be snapped to the cursor, and have automatically generated control points based on the cursor end point / known start point.
        //  -> Unlink connections.
        //   -> Move the end point of the current line to the cursor. (If one is already on the cursor, cancel as if escape had been hit. This restriction should apply to all)
        //  -> Delete
        //  -> Add/remove camera spline?

        // TODO:
        //  -> These boxes should be placed in different situations. Sometimes per-chain, sometimes per-spline-point, etc.
        //  -> They should have the ability to update neighboring splines, neighboring path chains, splines in those path chains, etc.
        //   -> Also need to consider handling updates to camera spline.
        //  -> Updating start/end points should update neighbors.

        // Position UI.
        getEditorGrid().addSeparator();
        for (int i = 0; i < this.selectedSplinePreview.trackedPositions.length; i++) {
            IMediEvilPathSplineTrackedPosition trackedPosition = this.selectedSplinePreview.trackedPositions[i];
            TrackedPositionEditor positionEditor = this.pathPositionTracker.getEditor(trackedPosition.getPositionVector());
            if (positionEditor != null)
                positionEditor.setupUI(trackedPosition.getPointType().getDisplayName(), getEditorGrid());
        }
    }

    @Override
    protected void setVisible(MediEvilMapPathChain pathChain, MediEvilPathPreview pathPreview, boolean visible) {
        if (pathPreview != null)
            pathPreview.setVisible(visible);
    }

    @Override
    protected void onSelectedValueChange(MediEvilMapPathChain oldValue, MediEvilPathPreview oldPreview, MediEvilMapPathChain newValue, MediEvilPathPreview newPreview) {
        // Update the previews.
        if (oldPreview != null)
            oldPreview.updatePreview();
        if (newPreview != null)
            newPreview.updatePreview();
    }

    @Override
    protected MediEvilMapPathChain createNewValue() {
        // TODO: Perform setup on this so it is valid.
        return new MediEvilMapPathChain(getMap().getPathChainPacket());
    }

    @Override
    protected void onDelegateRemoved(MediEvilMapPathChain pathChain, MediEvilPathPreview pathPreview) {
        pathPreview.removeFxNodes();
    }

    /**
     * Updates the preview of an individual spline.
     * @param spline the spline to update
     */
    public void updatePreview(IMediEvilMapSpline spline) {
        MediEvilMapPathChain pathChain = spline.getPathChain();
        MediEvilPathPreview preview = getDelegatesByValue().get(pathChain);
        if (preview == null)
            return;

        MediEvilSplinePreview splinePreview = preview.getSplinePreview(spline);
        if (splinePreview != null)
            splinePreview.updateLineFxNodes();
    }

    public static class MediEvilPathPreview {
        @Getter private final MediEvilPathManager pathManager;
        @Getter private final MediEvilMapPathChain pathChainNode;
        private final List<MediEvilSplinePreview> splinePreviews = new ArrayList<>();

        private MediEvilPathPreview(MediEvilPathManager pathManager, MediEvilMapPathChain pathChainNode) {
            this.pathManager = pathManager;
            this.pathChainNode = pathChainNode;

            // Setup spline previews.
            List<MediEvilMap2DSpline> splines = pathChainNode.getPathSplines();
            for (int i = 0; i < splines.size(); i++) {
                MediEvilMap2DSpline spline = splines.get(i);
                this.splinePreviews.add(new MediEvilSplinePreview(this, spline, MATERIAL_SALMON, MATERIAL_PINK));
                MediEvilMap3DSpline cameraSpline = spline.getCameraSpline();
                if (cameraSpline != null)
                    this.splinePreviews.add(new MediEvilSplinePreview(this, cameraSpline, MATERIAL_SKYBLUE, MATERIAL_POWDERBLUE));
            }
        }

        private void addSplinePreviews() {
            DisplayList displayList = this.pathManager.pathDisplayList;
            for (int i = 0; i < this.splinePreviews.size(); i++) {
                MediEvilSplinePreview splinePreview = this.splinePreviews.get(i);
                IMediEvilPathSplineTrackedPosition[] trackedPositions = splinePreview.trackedPositions;
                for (int j = 0; j < trackedPositions.length; j++) {
                    IMediEvilPathSplineTrackedPosition trackedPosition = trackedPositions[j];
                    if (!this.pathManager.pathPositionTracker.addPositionTracker(trackedPosition, displayList))
                        throw new IllegalStateException("Could not add the tracked position " + trackedPosition + ".");
                }
            }
        }

        /**
         * Gets the preview for a particular spline
         * @param spline the spline to get the preview for
         * @return splinePreview
         */
        public MediEvilSplinePreview getSplinePreview(IMediEvilMapSpline spline) {
            for (int i = 0; i < this.splinePreviews.size(); i++) {
                MediEvilSplinePreview splinePreview = this.splinePreviews.get(i);
                if (splinePreview.getSpline() == spline)
                    return splinePreview;
            }

            return null;
        }

        /**
         * Remove the FX nodes from all spline previews.
         */
        public void updatePreview() {
            for (int i = 0; i < this.splinePreviews.size(); i++)
                this.splinePreviews.get(i).updateLineFxNodes();
        }

        /**
         * Remove the FX nodes from all spline previews.
         */
        public void removeFxNodes() {
            for (int i = 0; i < this.splinePreviews.size(); i++)
                this.splinePreviews.get(i).removeFxNodes();
        }

        /**
         * Sets whether the path preview is visible.
         * @param visible whether the path preview is visible
         */
        public void setVisible(boolean visible) {
            for (int i = 0; i < this.splinePreviews.size(); i++)
                this.splinePreviews.get(i).setVisible(visible);
        }

        /**
         * Gets the mesh controller.
         */
        public MediEvilMapMeshController getController() {
            return this.pathManager.getController();
        }
    }

    public static class MediEvilSplinePreview {
        @Getter private final MediEvilPathPreview pathPreview;
        @Getter private final IMediEvilMapSpline spline;
        private final PhongMaterial regularMaterial;
        private final PhongMaterial faintMaterial;
        private final List<Node> nodes = new ArrayList<>();
        private final List<Cylinder> lineFxNodes = new ArrayList<>();
        private final EventHandler<MouseEvent> mouseEventHandler = this::handleClick;
        @Getter private final MRBezierCurve bezierCurve; // Contains an editable version of the spline data.
        private final IMediEvilPathSplineTrackedPosition[] trackedPositions; // Contains an editable version of the spline data.
        private MRSplineMatrix splineMatrix; // Used for editing spline data.
        private boolean fxNodesVisible;

        private <TSpline extends IMediEvilMapSpline> MediEvilSplinePreview(MediEvilPathPreview pathPreview, TSpline spline, PhongMaterial regularMaterial, PhongMaterial faintMaterial) {
            this.pathPreview = pathPreview;
            this.spline = spline;
            this.regularMaterial = regularMaterial;
            this.faintMaterial = faintMaterial;
            this.bezierCurve = SCMath.createBezierCurve(this.pathPreview.pathChainNode.getGameInstance(), this.spline.getSubDivisions());

            // Create positions.
            this.trackedPositions = new IMediEvilPathSplineTrackedPosition[MRBezierCurvePointType.values().length];
            for (int i = 0; i < this.trackedPositions.length; i++)
                this.trackedPositions[i] = new MediEvilMapPathSplineTrackedPosition(pathPreview.pathManager, spline, this.bezierCurve, MRBezierCurvePointType.values()[i]);
        }

        /**
         * Removes the path nodes from existence.
         */
        public void removeFxNodes() {
            if (this.fxNodesVisible) {
                this.fxNodesVisible = false;
                this.pathPreview.pathManager.pathDisplayList.removeAll(this.nodes);
            }
        }

        /**
         * Removes the path nodes from existence.
         */
        public void addFxNodes() {
            if (!this.fxNodesVisible) {
                this.fxNodesVisible = true;
                this.nodes.forEach(this.pathPreview.pathManager.pathDisplayList::add);
            }
        }

        /**
         * Sets whether the path preview is visible.
         * @param visible whether the path preview is visible
         */
        public void setVisible(boolean visible) {
            if (!visible) {
                removeFxNodes();
                setTrackedPositionEditorVisible(false);
            } else {
                if (this.lineFxNodes.isEmpty())
                    updateLineFxNodes();
                addFxNodes();
                setTrackedPositionEditorVisible(true);
            }
        }

        private void setTrackedPositionEditorVisible(boolean visible) {
            if (this.trackedPositions == null)
                return;

            for (int i = 0; i < this.trackedPositions.length; i++) {
                IMediEvilPathSplineTrackedPosition trackedPosition = this.trackedPositions[i];
                if (trackedPosition == null)
                    continue;

                TrackedPositionEditor positionEditor = this.pathPreview.pathManager.pathPositionTracker.getEditor(trackedPosition.getPositionVector());
                if (positionEditor != null)
                    positionEditor.setVisible(visible);
            }
        }

        private void handleClick(MouseEvent event) {
            MediEvilPathManager pathManager = this.pathPreview.pathManager;
            if (pathManager.selectedSplinePreview != this) {
                pathManager.setSelectedSplinePreview(this);
            } else {
                pathManager.setSelectedSplinePreview(null);
            }
        }

        private void updateLineFxNodes() {
            List<SVector> subDivisions = this.spline.getSubDivisions();

            // TODO: I think the 2D UI doesn't update the spline subdivs.

            // Update line positions based on the subDivisions.
            SVector lastPosition = null;
            for (int i = 0; i < subDivisions.size(); i++) {
                SVector currentPosition = subDivisions.get(i);
                if (lastPosition != null) {
                    Cylinder cylinder = this.lineFxNodes.size() >= i ? this.lineFxNodes.get(i - 1) : null;
                    createOrUpdateLineNode(i, cylinder, lastPosition, currentPosition, .6);
                }

                lastPosition = currentPosition;
            }

            // Remove unnecessary nodes.
            while (this.lineFxNodes.size() > subDivisions.size() - 1) {
                Cylinder cylinder = this.lineFxNodes.remove(this.lineFxNodes.size() - 1);
                this.nodes.remove(cylinder);
                if (this.fxNodesVisible)
                    this.pathPreview.pathManager.pathDisplayList.remove(cylinder);
            }
        }

        /**
         * Creates/obtains a spline matrix used to represent the edited spline data.
         * @return splineMatrix
         */
        public MRSplineMatrix getSplineMatrix() {
            // Create a spline matrix if one does not exist.
            if (this.splineMatrix == null) {
                this.splineMatrix = this.bezierCurve.toSplineMatrix();
                updateLineFxNodes();
            }

            return this.splineMatrix;
        }

        private Cylinder createOrUpdateLineNode(int i, Cylinder cylinder, SVector startPos, SVector endPos, double radius) {
            PhongMaterial material;
            if (this == this.pathPreview.pathManager.selectedSplinePreview) {
                material = MATERIAL_LIGHT_GREEN;
            } else if (this.spline instanceof MediEvilMap3DSpline && ((MediEvilMap3DSpline) this.spline).getPathSpline() != null
                    && ((MediEvilMap3DSpline) this.spline).getPathSpline().testDeadSubDivsEnabled()
                    && (((MediEvilMap3DSpline) this.spline).getPathSpline().getNumDeadSubDivsStart() >= i || (i >= this.spline.getSubDivisions().size() - ((MediEvilMap3DSpline) this.spline).getPathSpline().getNumDeadSubDivsEnd()))) {
                material = MATERIAL_LIGHT_YELLOW;
            } else if (this.spline instanceof MediEvilMap2DSpline && ((MediEvilMap2DSpline) this.spline).testDeadSubDivsEnabled()
                    && (((MediEvilMap2DSpline) this.spline).getNumDeadSubDivsStart() >= i || i >= this.spline.getSubDivisions().size() - ((MediEvilMap2DSpline) this.spline).getNumDeadSubDivsEnd())) {
                material = MATERIAL_LIGHT_YELLOW;
            } else if (this.pathPreview.pathManager.getSelectedValue() != null && this.pathPreview.pathManager.getSelectedValue() == this.spline.getPathChain()) {
                material = this.faintMaterial;
            } else {
                material = this.regularMaterial;
            }

            if (cylinder != null) {
                Scene3DUtils.updateCylindricalLine(cylinder, startPos.getFloatX(), startPos.getFloatY(), startPos.getFloatZ(),
                        endPos.getFloatX(), endPos.getFloatY(), endPos.getFloatZ());

                // Update potentially outdated properties.
                if (cylinder.getMaterial() != material)
                    cylinder.setMaterial(material);
                if (cylinder.getRadius() != radius)
                    cylinder.setRadius(radius);
            } else {
                cylinder = Scene3DUtils.addCylindricalLine(startPos.getFloatX(), startPos.getFloatY(), startPos.getFloatZ(),
                        endPos.getFloatX(), endPos.getFloatY(), endPos.getFloatZ(), radius, material, -1);

                cylinder.setOnMouseClicked(this.mouseEventHandler);
                this.nodes.add(cylinder);
                this.lineFxNodes.add(cylinder);
                if (this.fxNodesVisible)
                    this.pathPreview.pathManager.pathDisplayList.add(cylinder);
            }

            return cylinder;
        }
    }
}
