package net.highwayfrogs.editor.games.sony.shared.mof2.ui.managers;

import javafx.scene.control.CheckBox;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.DrawMode;
import javafx.scene.shape.Shape3D;
import net.highwayfrogs.editor.games.sony.shared.collprim.CollprimShapeAdapter;
import net.highwayfrogs.editor.games.sony.shared.collprim.ICollprimEditorUI;
import net.highwayfrogs.editor.games.sony.shared.mof2.collision.MRMofCollprim;
import net.highwayfrogs.editor.games.sony.shared.mof2.mesh.MRMofPart;
import net.highwayfrogs.editor.games.sony.shared.mof2.mesh.MRStaticMof;
import net.highwayfrogs.editor.games.sony.shared.mof2.ui.MRModelMeshController;
import net.highwayfrogs.editor.games.sony.shared.mof2.ui.mesh.MRModelMesh;
import net.highwayfrogs.editor.gui.GUIEditorGrid;
import net.highwayfrogs.editor.gui.editor.BasicListMeshUIManager;
import net.highwayfrogs.editor.gui.editor.DisplayList;
import net.highwayfrogs.editor.gui.editor.UISidePanel;
import net.highwayfrogs.editor.utils.Scene3DUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Manages the UI for MRModel collprims.
 * Created by Kneesnap on 5/8/2025.
 */
public class MRModelCollprimUIManager extends BasicListMeshUIManager<MRModelMesh, MRMofCollprim, CollprimShapeAdapter<?>> implements ICollprimEditorUI<MRMofCollprim> {
    private final DisplayList collprimDisplayList;
    private final List<MRMofCollprim> cachedCollprims = new ArrayList<>();
    private CheckBox wireframePreviewCheckBox;

    private static final PhongMaterial COLLPRIM_MATERIAL = Scene3DUtils.makeUnlitSharpMaterial(Color.LIGHTGREEN);
    private static final PhongMaterial SELECTED_COLLPRIM_MATERIAL = Scene3DUtils.makeUnlitSharpMaterial(Color.YELLOW);

    public MRModelCollprimUIManager(MRModelMeshController controller) {
        super(controller);
        this.collprimDisplayList = controller.getRenderManager().createDisplayList();
    }

    @Override
    public MRModelMeshController getController() {
        return (MRModelMeshController) super.getController();
    }
    @Override
    protected void setupMainGridEditor(UISidePanel sidePanel) {
        super.setupMainGridEditor(sidePanel);

        // Wireframe preview checkbox.
        this.wireframePreviewCheckBox = new CheckBox("Wireframe Display");
        this.wireframePreviewCheckBox.selectedProperty().addListener((listener, oldValue, newValue) -> updateCollprimWireframeState(newValue));
        sidePanel.add(this.wireframePreviewCheckBox);
    }


    @Override
    public String getTitle() {
        return "Collision (Collprims)";
    }

    @Override
    public String getValueName() {
        return "Collprim";
    }

    @Override
    public List<MRMofCollprim> getValues() {
        this.cachedCollprims.clear();
        MRStaticMof staticMof = getController().getActiveStaticMof();
        if (staticMof != null) {
            for (int i = 0; i < staticMof.getParts().size(); i++) {
                MRMofPart mofPart = staticMof.getParts().get(i);
                if (mofPart.getCollPrims().size() > 0)
                    this.cachedCollprims.addAll(mofPart.getCollPrims());
            }
        }

        return this.cachedCollprims;
    }

    @Override
    protected CollprimShapeAdapter<?> setupDisplay(MRMofCollprim collprim) {
        if (collprim == null)
            return null;

        boolean isSelected = (collprim == getValueSelectionBox().getValue());
        CollprimShapeAdapter<?> adapter = collprim.addDisplay(this, this.collprimDisplayList, isSelected ? SELECTED_COLLPRIM_MATERIAL : COLLPRIM_MATERIAL);
        adapter.getShape().setDrawMode(this.wireframePreviewCheckBox.isSelected() ? DrawMode.LINE : DrawMode.FILL);
        adapter.getShape().setOnMouseClicked(event -> handleClick(event, collprim, adapter));
        getController().getRotationManager().applyRotation(adapter.getShape());
        return adapter;
    }

    private void handleClick(MouseEvent event, MRMofCollprim collprim, CollprimShapeAdapter<?> adapter) {
        getValueSelectionBox().getSelectionModel().select(collprim);
    }

    @Override
    protected void updateEditor(MRMofCollprim selectedCollprim) {
        selectedCollprim.setupEditor(this, getDelegatesByValue().get(selectedCollprim));
    }

    @Override
    protected void setValuesVisible(boolean visible) {
        super.setValuesVisible(visible);
        this.collprimDisplayList.setVisible(visible);
    }

    @Override
    protected void setVisible(MRMofCollprim collprim, CollprimShapeAdapter<?> adapter, boolean visible) {
        adapter.getShape().setVisible(visible);
    }

    @Override
    protected void onSelectedValueChange(MRMofCollprim oldValue, CollprimShapeAdapter<?> oldAdapter, MRMofCollprim newValue, CollprimShapeAdapter<?> newAdapter) {
        if (oldAdapter != null) // Apply de-selected material.
            oldAdapter.getShape().setMaterial(COLLPRIM_MATERIAL);

        if (newAdapter != null) // Apply selected material.
            newAdapter.getShape().setMaterial(SELECTED_COLLPRIM_MATERIAL);
    }

    @Override
    protected MRMofCollprim createNewValue() {
        MRStaticMof staticMof = getController().getActiveStaticMof();
        MRMofPart mofPart = staticMof != null && staticMof.getParts().size() > 0 ? staticMof.getParts().get(0) : null; // TODO: allow choosing the part instead, via selection menu?
        return mofPart != null ? new MRMofCollprim(mofPart) : null;
    }

    @Override
    protected boolean tryAddValue(MRMofCollprim collprim) {
        return collprim != null && collprim.getParentPart() != null
                && !collprim.getParentPart().getCollPrims().contains(collprim)
                && collprim.getParentPart().getCollPrims().add(collprim);
    }

    @Override
    protected boolean tryRemoveValue(MRMofCollprim collprim) {
        return collprim != null && collprim.getParentPart() != null
                && collprim.getParentPart().getCollPrims().remove(collprim);
    }

    @Override
    protected void onDelegateRemoved(MRMofCollprim collprim, CollprimShapeAdapter<?> adapter) {
        this.collprimDisplayList.remove(adapter.getShape());
    }

    private void updateCollprimWireframeState(boolean wireframeMode) {
        DrawMode newDrawMode = wireframeMode ? DrawMode.LINE : DrawMode.FILL;
        for (int i = 0; i < this.collprimDisplayList.size(); i++)
            ((Shape3D) this.collprimDisplayList.getNodes().get(i)).setDrawMode(newDrawMode);
    }

    @Override
    public GUIEditorGrid getCollprimEditorGrid() {
        return getEditorGrid();
    }

    @Override
    public void onCollprimChangeType(MRMofCollprim collprim, CollprimShapeAdapter<?> adapter, Object oldType, Object newType) {
        createDisplay(collprim); // Setup display again.
    }

    @Override
    public void updateCollprimPosition(MRMofCollprim collprim, CollprimShapeAdapter<?> adapter) {
        // TODO: Implement.
    }

    @Override
    public void onCollprimRemove(MRMofCollprim collprim, CollprimShapeAdapter<?> adapter) {
        removeValue(collprim);
    }
}
