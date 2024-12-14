package net.highwayfrogs.editor.games.sony.medievil.map.ui;

import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Box;
import javafx.scene.shape.DrawMode;
import javafx.scene.shape.Shape3D;
import net.highwayfrogs.editor.games.sony.medievil.map.MediEvilMapCollprim;
import net.highwayfrogs.editor.games.sony.medievil.map.MediEvilMapCollprim.MediEvilMapCollprimType;
import net.highwayfrogs.editor.games.sony.medievil.map.mesh.MediEvilMapMesh;
import net.highwayfrogs.editor.games.sony.medievil.map.ui.MediEvilMapUIManager.MediEvilMapListManager;
import net.highwayfrogs.editor.gui.editor.DisplayList;
import net.highwayfrogs.editor.gui.editor.MeshViewController;
import net.highwayfrogs.editor.gui.editor.UISidePanel;
import net.highwayfrogs.editor.system.AbstractStringConverter;
import net.highwayfrogs.editor.utils.Scene3DUtils;

import java.util.List;

/**
 * Manages map collprims.
 * Cloned from a file created by Kneesnap on 3/13/2024.
 */
public class MediEvilCollprimManager extends MediEvilMapListManager<MediEvilMapCollprim, Box> {
    private final DisplayList collprimDisplayList;
    private CheckBox wireframePreviewCheckBox;
    private ComboBox<MediEvilMapCollprimType> collprimTypeFilterComboBox;

    private static final PhongMaterial MATERIAL_GREEN = Scene3DUtils.makeUnlitSharpMaterial(Color.GREEN);
    private static final PhongMaterial MATERIAL_LIME = Scene3DUtils.makeUnlitSharpMaterial(Color.LIME);
    private static final PhongMaterial MATERIAL_YELLOW = Scene3DUtils.makeUnlitSharpMaterial(Color.YELLOW);
    private static final PhongMaterial MATERIAL_PINK = Scene3DUtils.makeUnlitSharpMaterial(Color.PINK);
    private static final PhongMaterial MATERIAL_SALMON = Scene3DUtils.makeUnlitSharpMaterial(Color.LIGHTSALMON);
    private static final PhongMaterial MATERIAL_BLUE = Scene3DUtils.makeUnlitSharpMaterial(Color.BLUE);
    private static final PhongMaterial MATERIAL_WHITE = Scene3DUtils.makeUnlitSharpMaterial(Color.WHITE);

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

        // Shown Types.
        MediEvilMapCollprimType[] strippedTypes = new MediEvilMapCollprimType[MediEvilMapCollprimType.values().length - 1];
        for (int i = 0, ordinal = 0; i < MediEvilMapCollprimType.values().length; i++) {
            MediEvilMapCollprimType testType = MediEvilMapCollprimType.values()[i];
            if (testType != MediEvilMapCollprimType.NONE)
                strippedTypes[ordinal++] = testType;
        }

        this.collprimTypeFilterComboBox = getMainGrid().addEnumSelector("Type Shown", null, strippedTypes, true, newType -> updateValueVisibility());
        this.collprimTypeFilterComboBox.setConverter(new AbstractStringConverter<>(Enum::name, "ALL"));

        // Wireframe preview checkbox.
        this.wireframePreviewCheckBox = getMainGrid().addCheckBox("Wireframe Display", true, this::updateCollprimWireframeState);
    }

    @Override
    public boolean isValueVisibleByUI(MediEvilMapCollprim collprim) {
        return super.isValueVisibleByUI(collprim) && (collprim.getType() == this.collprimTypeFilterComboBox.getValue() || this.collprimTypeFilterComboBox.getValue() == null);
    }

    @Override
    protected Box setupDisplay(MediEvilMapCollprim collprim) {
        boolean isSelected = (collprim == getValueSelectionBox().getValue());
        Box box = collprim.addDisplay(this, this.collprimDisplayList, isSelected ? MATERIAL_YELLOW : getCollprimMaterial(collprim));
        box.setDrawMode(this.wireframePreviewCheckBox.isSelected() ? DrawMode.LINE : DrawMode.FILL);
        box.setOnMouseClicked(event -> handleClick(event, collprim));
        return box;
    }

    private static PhongMaterial getCollprimMaterial(MediEvilMapCollprim collprim) {
        switch(collprim.getType()) {
            case CAMERA:
                return collprim.testFlagMask(MediEvilMapCollprim.CAMERA_PLUGIN_ID_MASK)  ? MATERIAL_PINK : MATERIAL_SALMON;
            case WARP:
                return MATERIAL_BLUE;
            case NORMAL:
                return collprim.testFlagMask(MediEvilMapCollprim.NORMAL_FLAG_FIRES_EVENT) ? MATERIAL_LIME : MATERIAL_GREEN;
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
    protected void setVisible(MediEvilMapCollprim collprim, Box box, boolean visible) {
        box.setVisible(visible);
    }

    @Override
    protected void onSelectedValueChange(MediEvilMapCollprim oldValue, Box oldBox, MediEvilMapCollprim newValue, Box newBox) {
        if (oldBox != null) // Apply de-selected material.
            oldBox.setMaterial(getCollprimMaterial(oldValue));

        if (newBox != null) // Apply selected material.
            newBox.setMaterial(MATERIAL_YELLOW);
    }

    @Override
    protected void onDelegateRemoved(MediEvilMapCollprim removedValue, Box removedBox) {
        this.collprimDisplayList.remove(removedBox);
    }

    private void updateCollprimWireframeState(boolean wireframeMode) {
        DrawMode newDrawMode = wireframeMode ? DrawMode.LINE : DrawMode.FILL;
        for (int i = 0; i < this.collprimDisplayList.size(); i++)
            ((Shape3D) this.collprimDisplayList.getNodes().get(i)).setDrawMode(newDrawMode);
    }
}