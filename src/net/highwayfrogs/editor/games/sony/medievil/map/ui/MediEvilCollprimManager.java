package net.highwayfrogs.editor.games.sony.medievil.map.ui;

import javafx.scene.control.CheckBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.DrawMode;
import javafx.scene.shape.Shape3D;
import net.highwayfrogs.editor.games.sony.medievil.data.MediEvilMapCollprim;
import net.highwayfrogs.editor.games.sony.medievil.map.mesh.MediEvilMapMesh;
import net.highwayfrogs.editor.games.sony.shared.collprim.CollprimShapeAdapter;
import net.highwayfrogs.editor.games.sony.shared.collprim.ICollprimEditorUI;
import net.highwayfrogs.editor.games.sony.shared.collprim.MRCollprim;
import net.highwayfrogs.editor.games.sony.shared.collprim.MRCollprim.CollprimType;
import net.highwayfrogs.editor.gui.GUIEditorGrid;
import net.highwayfrogs.editor.gui.editor.DisplayList;
import net.highwayfrogs.editor.gui.editor.MeshViewController;
import net.highwayfrogs.editor.gui.editor.UISidePanel;
import net.highwayfrogs.editor.utils.Utils;

import java.util.List;

/**
 * Manages map collprims.
 * TODO: Collprim menu isn't automatically extended.
 * Cloned from a file created by Kneesnap on 3/13/2024.
 */
public class MediEvilCollprimManager extends MediEvilMapUIManager.MediEvilMapListManager<MediEvilMapCollprim, CollprimShapeAdapter<?>> implements ICollprimEditorUI {
    private final DisplayList collprimDisplayList;
    private CheckBox wireframePreviewCheckBox;

    private static final PhongMaterial MATERIAL_GREEN = Utils.makeSpecialMaterial(Color.GREEN);
    private static final PhongMaterial MATERIAL_YELLOW = Utils.makeSpecialMaterial(Color.YELLOW);
    private static final PhongMaterial MATERIAL_PINK = Utils.makeSpecialMaterial(Color.PINK);
    private static final PhongMaterial MATERIAL_BLUE = Utils.makeSpecialMaterial(Color.BLUE);
    private static final PhongMaterial MATERIAL_WHITE = Utils.makeSpecialMaterial(Color.WHITE);


    public MediEvilCollprimManager(MeshViewController<MediEvilMapMesh> controller) {
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
    public List<MediEvilMapCollprim> getValues() {
        return getMap().getCollprimsPacket().getCollprims();
    }

    @Override
    protected MediEvilMapCollprim createNewValue() {
        return new MediEvilMapCollprim(getMap());
    }

    @Override
    protected void setupMainGridEditor(UISidePanel sidePanel) {
        super.setupMainGridEditor(sidePanel);

        // Wireframe preview checkbox.
        this.wireframePreviewCheckBox = new CheckBox("Wireframe Display");
        this.wireframePreviewCheckBox.setSelected(true);
        this.wireframePreviewCheckBox.selectedProperty().addListener((listener, oldValue, newValue) -> updateCollprimWireframeState(newValue));
        sidePanel.add(this.wireframePreviewCheckBox);
    }

    @Override
    protected CollprimShapeAdapter<?> setupDisplay(MediEvilMapCollprim collprim) {
        boolean isSelected = (collprim == getValueSelectionBox().getValue());
        CollprimShapeAdapter<?> adapter = collprim.addDisplay(this, this.collprimDisplayList, isSelected ? MATERIAL_YELLOW : getCollprimMaterial(collprim.getMediEvilFunctionality()));
        adapter.getShape().setDrawMode(this.wireframePreviewCheckBox.isSelected() ? DrawMode.LINE : DrawMode.FILL);
        adapter.getShape().setOnMouseClicked(event -> getValueSelectionBox().getSelectionModel().select(collprim));

        return adapter;
    }

    protected PhongMaterial getCollprimMaterial(MediEvilMapCollprim.MediEvilCollprimFunctionality functionality)
    {
        switch(functionality)
        {
            case CAMERA:
                return MATERIAL_PINK;
            case WARP:
                return MATERIAL_BLUE;
            case COLLNEVENT:
                return MATERIAL_GREEN;
            default:
                return MATERIAL_WHITE;
        }
    }

    @Override
    protected void updateEditor(MediEvilMapCollprim selectedCollprim) {
        selectedCollprim.setupEditor(this, getDelegatesByValue().get(selectedCollprim));
    }

    @Override
    protected void setValuesVisible(boolean showCollprims) {
        super.setValuesVisible(showCollprims);
        this.collprimDisplayList.setVisible(showCollprims);
    }

    @Override
    protected void setVisible(MediEvilMapCollprim medievilMapCollprim, CollprimShapeAdapter<?> collprimShapeAdapter, boolean visible) {
        collprimShapeAdapter.getShape().setVisible(visible);
    }

    @Override
    protected void onSelectedValueChange(MediEvilMapCollprim oldValue, CollprimShapeAdapter<?> oldAdapter, MediEvilMapCollprim newValue, CollprimShapeAdapter<?> newAdapter) {
        if (oldAdapter != null) // Apply de-selected material.
            oldAdapter.getShape().setMaterial(getCollprimMaterial(oldValue.getMediEvilFunctionality()));

        if (newAdapter != null) // Apply selected material.
            newAdapter.getShape().setMaterial(MATERIAL_YELLOW);
    }

    @Override
    protected void onDelegateRemoved(MediEvilMapCollprim removedValue, CollprimShapeAdapter<?> removedAdapter) {
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
    public void onCollprimChangeType(MRCollprim collprim, CollprimShapeAdapter<?> adapter, CollprimType oldType, CollprimType newType) {
        createDisplay((MediEvilMapCollprim) collprim); // Setup display again.
    }

    @Override
    public void updateCollprimPosition(MRCollprim collprim, CollprimShapeAdapter<?> adapter) {
        // TODO: Implement.
    }

    @Override
    public void onCollprimRemove(MRCollprim collprim, CollprimShapeAdapter<?> adapter) {
        removeValue((MediEvilMapCollprim) collprim);
    }
}