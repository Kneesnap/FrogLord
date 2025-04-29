package net.highwayfrogs.editor.games.sony.frogger.map.ui.editor.central;

import net.highwayfrogs.editor.games.sony.frogger.map.data.form.FroggerMapForm;
import net.highwayfrogs.editor.games.sony.frogger.map.mesh.FroggerMapMesh;
import net.highwayfrogs.editor.games.sony.frogger.map.ui.editor.central.FroggerCentralUIManager.FroggerCentralMapListManager;
import net.highwayfrogs.editor.gui.editor.MeshViewController;

import java.util.List;

/**
 * Implements an editor for map forms.
 * Created by Kneesnap on 6/2/2024.
 */
public class FroggerUIMapFormManager extends FroggerCentralMapListManager<FroggerMapForm, Void> {
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
    public List<FroggerMapForm> getValues() {
        return getMap().getFormPacket().getForms();
    }

    @Override
    protected Void setupDisplay(FroggerMapForm form) {
        return null;
    }

    @Override
    protected void updateEditor(FroggerMapForm form) {
        form.setupEditor(this, getEditorGrid());
    }

    @Override
    protected void setVisible(FroggerMapForm form, Void unused, boolean visible) {
        // Do nothing.
    }

    @Override
    protected void onSelectedValueChange(FroggerMapForm oldForm, Void oldDelegate, FroggerMapForm newForm, Void newDelegate) {
        // Do nothing.
    }

    @Override
    protected FroggerMapForm createNewValue() {
        return new FroggerMapForm(getMap());
    }

    @Override
    protected void onDelegateRemoved(FroggerMapForm form, Void unused) {
        // Do nothing.
    }
}