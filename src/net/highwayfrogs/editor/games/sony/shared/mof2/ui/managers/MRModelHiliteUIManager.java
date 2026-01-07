package net.highwayfrogs.editor.games.sony.shared.mof2.ui.managers;

import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Box;
import javafx.scene.shape.DrawMode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.highwayfrogs.editor.file.standard.SVector;
import net.highwayfrogs.editor.games.sony.shared.mof2.hilite.MRMofHilite;
import net.highwayfrogs.editor.games.sony.shared.mof2.hilite.MRMofHilite.HiliteAttachType;
import net.highwayfrogs.editor.games.sony.shared.mof2.mesh.MRMofPart;
import net.highwayfrogs.editor.games.sony.shared.mof2.mesh.MRMofPolygon;
import net.highwayfrogs.editor.games.sony.shared.mof2.mesh.MRStaticMof;
import net.highwayfrogs.editor.games.sony.shared.mof2.ui.MRModelMeshController;
import net.highwayfrogs.editor.games.sony.shared.mof2.ui.managers.MRModelHiliteUIManager.MRMofHiliteUIPreview;
import net.highwayfrogs.editor.games.sony.shared.mof2.ui.mesh.MRModelMesh;
import net.highwayfrogs.editor.gui.editor.BasicListMeshUIManager;
import net.highwayfrogs.editor.gui.editor.DisplayList;
import net.highwayfrogs.editor.gui.editor.UISidePanel;
import net.highwayfrogs.editor.gui.mesh.DynamicMeshDataEntry;
import net.highwayfrogs.editor.gui.texture.basic.OutlineColorTextureSource;
import net.highwayfrogs.editor.utils.ColorUtils;
import net.highwayfrogs.editor.utils.Scene3DUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Manages the UI for MRModel hilites.
 * Created by Kneesnap on 5/8/2025.
 */
public class MRModelHiliteUIManager extends BasicListMeshUIManager<MRModelMesh, MRMofHilite, MRMofHiliteUIPreview> {
    private final DisplayList hiliteDisplayList;
    private final List<MRMofHilite> cachedHilites = new ArrayList<>();

    public static final OutlineColorTextureSource HILITE_COLOR = new OutlineColorTextureSource(ColorUtils.toAWTColor(Color.PURPLE), java.awt.Color.BLACK);
    private static final PhongMaterial HILITE_MATERIAL = Scene3DUtils.makeUnlitSharpMaterial(Color.PURPLE);

    public MRModelHiliteUIManager(MRModelMeshController controller) {
        super(controller);
        this.hiliteDisplayList = controller.getRenderManager().createDisplayList();
    }

    @Override
    public MRModelMeshController getController() {
        return (MRModelMeshController) super.getController();
    }

    @Override
    protected void setupMainGridEditor(UISidePanel sidePanel) {
        super.setupMainGridEditor(sidePanel);
        getMainGrid().addBoldLabel("What are hilites?");
        getMainGrid().addNormalLabel("Hilites are attachment points on 3D models.");
        getMainGrid().addNormalLabel("Usually particle effects spawn from them.");
        getMainGrid().addNormalLabel("But they can have purposes such as collision.");
    }

    @Override
    public String getTitle() {
        return "Hilites";
    }

    @Override
    public String getValueName() {
        return "Hilite";
    }

    @Override
    public List<MRMofHilite> getValues() {
        this.cachedHilites.clear();
        MRStaticMof staticMof = getController().getActiveStaticMof();
        if (staticMof != null) {
            for (int i = 0; i < staticMof.getParts().size(); i++) {
                MRMofPart mofPart = staticMof.getParts().get(i);
                if (mofPart.getHilites().size() > 0)
                    this.cachedHilites.addAll(mofPart.getHilites());
            }
        }

        return this.cachedHilites;
    }

    @Override
    protected MRMofHiliteUIPreview setupDisplay(MRMofHilite hilite) {
        MRMofHiliteUIPreview newPreview = new MRMofHiliteUIPreview(this, hilite);
        newPreview.createPreview();
        return newPreview;
    }

    @Override
    protected void updateEditor(MRMofHilite hilite) {
        hilite.setupEditor(this, getEditorGrid());
        getSidePanel().getTitledPane().setExpanded(true);
    }

    @Override
    protected void setVisible(MRMofHilite hilite, MRMofHiliteUIPreview hilitePreview, boolean visible) {
        if (hilitePreview != null)
            hilitePreview.setVisible(visible);
    }

    @Override
    protected void onSelectedValueChange(MRMofHilite oldValue, MRMofHiliteUIPreview oldPreview, MRMofHilite newValue, MRMofHiliteUIPreview newPreview) {
        if (oldPreview != null && oldPreview.getVertexBoxPreview() != null)
            oldPreview.getVertexBoxPreview().setDrawMode(DrawMode.LINE);
        if (newPreview != null && newPreview.getVertexBoxPreview() != null)
            newPreview.getVertexBoxPreview().setDrawMode(DrawMode.FILL);
    }

    @Override
    protected MRMofHilite createNewValue() {
        MRStaticMof staticMof = getController().getActiveStaticMof();
        MRMofPart mofPart = staticMof != null && staticMof.getParts().size() > 0 ? staticMof.getParts().get(0) : null; // TODO: allow choosing the part instead, via selection menu?
        return mofPart != null ? new MRMofHilite(mofPart) : null;
    }

    @Override
    protected boolean tryAddValue(MRMofHilite hilite) {
        boolean success = hilite != null && hilite.getParentPart() != null
                && !hilite.getParentPart().getHilites().contains(hilite)
                && hilite.getParentPart().getHilites().add(hilite);
        if (!success)
            return false;

        trySelectVertex(hilite);
        return true;
    }

    @Override
    protected boolean tryRemoveValue(MRMofHilite hilite) {
        return hilite != null && hilite.getParentPart() != null
                && hilite.getParentPart().getHilites().remove(hilite);
    }

    @Override
    protected void onDelegateRemoved(MRMofHilite hilite, MRMofHiliteUIPreview hilitePreview) {
        hilitePreview.removePreview();
    }

    private void trySelectVertex(MRMofHilite hilite) {
        /*getController().getMainManager().getVertexSelector().activate(newVertex -> {
            MRStaticMof staticMof = getMesh().getActiveStaticMof();
            if (staticMof == null)
                return;

            int partIndex = -1;
            int partCelIndex = -1;
            int vertexIndex = -1;
            for (int i = 0; i < staticMof.getParts().size(); i++) {
                MRMofPart mofPart = staticMof.getParts().get(i);
                for (int j = 0; j < mofPart.getPartC)
            }

            // TODO: Update hilite.
        }, null);
        PhongMaterial material = Scene3DUtils.makeUnlitSharpMaterial(Color.MAGENTA);
        RenderManager manager = getController().getRenderManager();
        manager.clearDisplayList(HILITE_VERTICE_LIST);

        getController().selectingVertex = true;
        for (MOFPart part : getHolder().asStaticFile().getParts()) {
            MOFPartcel partcel = part.getCel(Math.max(0, getMofMesh().getAnimationId()), getMofMesh().getFrame());

            for (int i = 0; i < partcel.getVertices().size(); i++) {
                final int index = i;
                SVector vec = partcel.getVertices().get(i);
                Box box = manager.addBoundingBoxCenteredWithDimensions(HILITE_VERTICE_LIST, vec.getFloatX(), vec.getFloatY(), vec.getFloatZ(), 1, 1, 1, material, true);
                box.setMouseTransparent(false);
                getController().applyRotation(box);
                box.setOnMouseClicked(mouseEvt -> {
                    manager.clearDisplayList(HILITE_VERTICE_LIST);
                    getController().selectingVertex = false;

                });
            }
        }*/ // TODO: Finish implementing.
    }

    @Getter
    @RequiredArgsConstructor
    public static class MRMofHiliteUIPreview {
        private final MRModelHiliteUIManager uiManager;
        private final MRMofHilite hilite;
        private MRMofPolygon highlightedPolygon;
        private Box vertexBoxPreview;

        /**
         * Creates the preview
         */
        public void createPreview() {
            removePreview();

            if (this.hilite.getAttachType() == HiliteAttachType.PRIM) {
                MRModelMesh mesh = this.uiManager.getController().getMesh();
                DynamicMeshDataEntry dataEntry = mesh.getPolygonDataEntry(this.hilite.getPolygon());
                if (dataEntry != null) {
                    this.highlightedPolygon = this.hilite.getPolygon();
                    mesh.getHighlightedSelectionPolygonNode().setOverlayTexture(dataEntry, HILITE_COLOR);
                }
            } else if (this.hilite.getAttachType() == HiliteAttachType.VERTEX) {
                SVector vertex = this.hilite.getVertex(); // TODO: Consider if this should be updated when there's a different partCel shown.
                Box hiliteBox = this.uiManager.hiliteDisplayList.addBoundingBoxCenteredWithDimensions(vertex.getFloatX(), vertex.getFloatY(), vertex.getFloatZ(), 1, 1, 1, HILITE_MATERIAL, true);
                this.uiManager.getController().getRotationManager().applyRotation(hiliteBox);
                hiliteBox.setOnMouseClicked(evt -> this.uiManager.getValueSelectionBox().getSelectionModel().select(this.hilite));
                hiliteBox.setMouseTransparent(false);
                this.vertexBoxPreview = hiliteBox;
            } else {
                throw new UnsupportedOperationException("The HiliteAttachType '" + this.hilite.getAttachType() + "' is unsupported.");
            }
        }

        /**
         * Remove the mof hilite preview.
         */
        public void removePreview() {
            if (this.highlightedPolygon != null) {
                MRModelMesh mesh = this.uiManager.getController().getMesh();
                DynamicMeshDataEntry dataEntry = mesh.getPolygonDataEntry(this.highlightedPolygon);
                if (dataEntry != null)
                    mesh.getHighlightedSelectionPolygonNode().setOverlayTexture(dataEntry, null);
                this.highlightedPolygon = null;
            }

            if (this.vertexBoxPreview != null) {
                this.uiManager.hiliteDisplayList.remove(this.vertexBoxPreview);
                this.vertexBoxPreview = null;
            }
        }

        /**
         * Controls whether the preview is visible.
         * @param visible whether the preview should be visible
         */
        public void setVisible(boolean visible) {
            if (this.highlightedPolygon != null) {
                MRModelMesh mesh = this.uiManager.getController().getMesh();
                DynamicMeshDataEntry dataEntry = mesh.getPolygonDataEntry(this.highlightedPolygon);
                if (dataEntry != null)
                    mesh.getHighlightedSelectionPolygonNode().setOverlayTexture(dataEntry, visible ? HILITE_COLOR : null);
            }

            if (this.vertexBoxPreview != null)
                this.vertexBoxPreview.setVisible(visible);
        }
    }
}
