package net.highwayfrogs.editor.games.sony.beastwars.ui;

import javafx.scene.control.CheckBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.DrawMode;
import javafx.scene.shape.Shape3D;
import net.highwayfrogs.editor.games.sony.beastwars.map.data.BeastWarsMapCollprim;
import net.highwayfrogs.editor.games.sony.beastwars.map.mesh.BeastWarsMapMesh;
import net.highwayfrogs.editor.games.sony.beastwars.ui.BeastWarsMapUIManager.BeastWarsMapListManager;
import net.highwayfrogs.editor.games.sony.shared.collprim.CollprimShapeAdapter;
import net.highwayfrogs.editor.games.sony.shared.collprim.ICollprimEditorUI;
import net.highwayfrogs.editor.games.sony.shared.collprim.MRCollprim;
import net.highwayfrogs.editor.gui.GUIEditorGrid;
import net.highwayfrogs.editor.gui.editor.DisplayList;
import net.highwayfrogs.editor.gui.editor.MeshViewController;
import net.highwayfrogs.editor.gui.editor.UISidePanel;
import net.highwayfrogs.editor.utils.Utils;

import java.util.List;

/**
 * Manages map collprims.
 * TODO: Collprim menu isn't automatically extended.
 * Created by Kneesnap on 9/26/2023.
 */
public class BeastWarsCollprimManager extends BeastWarsMapListManager<BeastWarsMapCollprim, CollprimShapeAdapter<?>> implements ICollprimEditorUI<MRCollprim> {
    private final DisplayList collprimDisplayList;
    private CheckBox wireframePreviewCheckBox;

    private static final PhongMaterial MATERIAL_GREEN = Utils.makeSpecialMaterial(Color.GREEN);
    private static final PhongMaterial MATERIAL_YELLOW = Utils.makeSpecialMaterial(Color.YELLOW);


    public BeastWarsCollprimManager(MeshViewController<BeastWarsMapMesh> controller) {
        super(controller);
        this.collprimDisplayList = controller.getRenderManager().createDisplayList();
    }

    @Override
    public String getTitle() {
        return "Collision Primitives (Collprims)";
    }

    @Override
    public String getValueName() {
        return "Collprim";
    }

    @Override
    public List<BeastWarsMapCollprim> getValues() {
        return getMap().getCollprims();
    }

    @Override
    protected BeastWarsMapCollprim createNewValue() {
        return new BeastWarsMapCollprim(getMap());
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
    protected CollprimShapeAdapter<?> setupDisplay(BeastWarsMapCollprim collprim) {
        boolean isSelected = (collprim == getValueSelectionBox().getValue());
        CollprimShapeAdapter<?> adapter = collprim.addDisplay(this, this.collprimDisplayList, isSelected ? MATERIAL_YELLOW : MATERIAL_GREEN);
        adapter.getShape().setDrawMode(this.wireframePreviewCheckBox.isSelected() ? DrawMode.LINE : DrawMode.FILL);
        adapter.getShape().setOnMouseClicked(event -> getValueSelectionBox().getSelectionModel().select(collprim));

        return adapter;
    }

    @Override
    protected void updateEditor(BeastWarsMapCollprim selectedCollprim) {
        selectedCollprim.setupEditor(this, getDelegatesByValue().get(selectedCollprim));
    }

    @Override
    protected void setValuesVisible(boolean showCollprims) {
        super.setValuesVisible(showCollprims);
        this.collprimDisplayList.setVisible(showCollprims);
    }

    @Override
    protected void setVisible(BeastWarsMapCollprim beastWarsMapCollprim, CollprimShapeAdapter<?> collprimShapeAdapter, boolean visible) {
        collprimShapeAdapter.getShape().setVisible(visible);
    }

    @Override
    protected void onSelectedValueChange(BeastWarsMapCollprim oldValue, CollprimShapeAdapter<?> oldAdapter, BeastWarsMapCollprim newValue, CollprimShapeAdapter<?> newAdapter) {
        if (oldAdapter != null) // Apply de-selected material.
            oldAdapter.getShape().setMaterial(MATERIAL_GREEN);

        if (newAdapter != null) // Apply de-selected material.
            newAdapter.getShape().setMaterial(MATERIAL_YELLOW);
    }

    @Override
    protected void onDelegateRemoved(BeastWarsMapCollprim removedValue, CollprimShapeAdapter<?> removedAdapter) {
        this.collprimDisplayList.remove(removedAdapter.getShape());
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
    public void onCollprimChangeType(MRCollprim collprim, CollprimShapeAdapter<?> adapter, Object oldType, Object newType) {
        createDisplay((BeastWarsMapCollprim) collprim); // Setup display again.
    }

    @Override
    public void updateCollprimPosition(MRCollprim collprim, CollprimShapeAdapter<?> adapter) {
        // TODO: Implement.
    }

    @Override
    public void onCollprimRemove(MRCollprim collprim, CollprimShapeAdapter<?> adapter) {
        removeValue((BeastWarsMapCollprim) collprim);
    }
}