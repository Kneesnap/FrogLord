package net.highwayfrogs.editor.games.sony.frogger.map.ui.editor.central;

import net.highwayfrogs.editor.games.sony.frogger.map.data.form.FroggerFormGrid;
import net.highwayfrogs.editor.games.sony.frogger.map.mesh.FroggerMapMesh;
import net.highwayfrogs.editor.games.sony.frogger.map.ui.editor.central.FroggerCentralUIManager.FroggerCentralMapListManager;
import net.highwayfrogs.editor.gui.editor.MeshViewController;

import java.util.List;

/**
 * Implements an editor for map forms.
 * Created by Kneesnap on 6/2/2024.
 */
public class FroggerUIMapFormManager extends FroggerCentralMapListManager<FroggerFormGrid, Void> {
    public FroggerUIMapFormManager(MeshViewController<FroggerMapMesh> controller) {
        super(controller);
    }

    @Override
    public String getTitle() {
        return "Entity Form Grids";
    }

    @Override
    public String getValueName() {
        return "Form";
    }

    @Override
    public List<FroggerFormGrid> getValues() {
        return getMap().getFormPacket().getForms();
    }

    @Override
    protected String getListDisplayName(int index, FroggerFormGrid form) {
        String value = super.getListDisplayName(index, form);
        if (form != null) {
            return value + " (" + (form.getFormEntry() != null ? form.getFormEntry().getFormTypeName() : "None") + ")";
        } else {
            return value;
        }
    }

    @Override
    protected Void setupDisplay(FroggerFormGrid form) {
        return null;
    }

    @Override
    protected void updateEditor(FroggerFormGrid form) {
        form.setupEditor(this, getEditorGrid());
    }

    @Override
    protected void setVisible(FroggerFormGrid form, Void unused, boolean visible) {
        // Do nothing.
    }

    @Override
    protected void onSelectedValueChange(FroggerFormGrid oldForm, Void oldDelegate, FroggerFormGrid newForm, Void newDelegate) {
        // Do nothing.
    }

    @Override
    protected boolean tryAddValue(FroggerFormGrid formGrid) {
        return formGrid != null && getMap().getFormPacket().addFormGrid(formGrid) >= 0;
    }

    @Override
    protected boolean tryRemoveValue(FroggerFormGrid formGrid) {
        return formGrid != null && getMap().getFormPacket().removeFormGrid(formGrid);
    }

    @Override
    protected FroggerFormGrid createNewValue() {
        return new FroggerFormGrid(getMap());
    }

    @Override
    protected void onDelegateRemoved(FroggerFormGrid form, Void unused) {
        // Do nothing.
    }
}