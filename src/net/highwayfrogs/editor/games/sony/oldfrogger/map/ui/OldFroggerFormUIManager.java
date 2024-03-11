package net.highwayfrogs.editor.games.sony.oldfrogger.map.ui;

import net.highwayfrogs.editor.games.sony.oldfrogger.map.entity.OldFroggerMapForm;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.mesh.OldFroggerMapMesh;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.packet.OldFroggerMapFormInstancePacket;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.ui.OldFroggerMapUIManager.OldFroggerMapListManager;
import net.highwayfrogs.editor.gui.editor.MeshViewController;
import net.highwayfrogs.editor.gui.editor.UISidePanel;
import net.highwayfrogs.editor.system.AbstractIndexStringConverter;

import java.util.List;

/**
 * Manages form user interfaces.
 * Created by Kneesnap on 12/18/2023.
 */
public class OldFroggerFormUIManager extends OldFroggerMapListManager<OldFroggerMapForm, Void> {
    public OldFroggerFormUIManager(MeshViewController<OldFroggerMapMesh> controller) {
        super(controller);
    }

    @Override
    public String getTitle() {
        return "Form Entity Definitions";
    }

    @Override
    public String getValueName() {
        return "Form";
    }

    @Override
    public List<OldFroggerMapForm> getValues() {
        return getMap().getFormInstancePacket().getForms();
    }

    @Override
    protected void setupMainGridEditor(UISidePanel sidePanel) {
        super.setupMainGridEditor(sidePanel);

        // Setup stuff.
        getValueDisplaySetting().setValue(ListDisplayType.NONE); // Forms cannot be previewed.
        getValueDisplaySetting().setDisable(true); // Forms cannot be previewed.

        // Allow changing form.
        OldFroggerMapFormInstancePacket formPacket = getMap().getFormInstancePacket();
        getMainGrid().addIntegerField("Form Table Size", formPacket.getFormTableSize(), newFormTableSize -> {
            formPacket.setFormTableSize(newFormTableSize);
            updateEditor();
        }, newFormTableSize -> newFormTableSize > 0 && newFormTableSize < 65536 && (formPacket.getForms().isEmpty() || newFormTableSize > formPacket.getForms().get(formPacket.getForms().size() - 1).getFormType()));
    }

    @Override
    protected void updateValuesInUI() {
        super.updateValuesInUI();

        // Use our own naming system.
        getValueSelectionBox().setConverter(new AbstractIndexStringConverter<>(getValues(), (index, form) -> {
            if (form != null) {
                String formName = form.getName();
                if (formName != null)
                    return formName + " (" + form.getFormType() + ")";

                return "Form ID " + form.getFormType();
            }

            return "Form " + index;
        }));
    }

    @Override
    protected Void setupDisplay(OldFroggerMapForm oldFroggerMapForm) {
        // No 3D Representation, don't need to do anything.
        return null;
    }

    @Override
    protected void updateEditor(OldFroggerMapForm selectedForm) {
        selectedForm.setupEditor(this, getEditorGrid());
    }

    @Override
    protected void setValuesVisible(boolean valuesVisible) {
        // No 3D Representation, don't need to do anything.
    }

    @Override
    protected void setVisible(OldFroggerMapForm oldFroggerMapForm, Void unused, boolean visible) {
        // No 3D representation, don't need to do anything.
    }

    @Override
    protected void onSelectedValueChange(OldFroggerMapForm oldValue, Void oldDelegate, OldFroggerMapForm newValue, Void newDelegate) {
        // No 3D Representation, don't need to do anything.
    }

    @Override
    protected OldFroggerMapForm createNewValue() {
        // We pass MAX_VALUE because we don't know which form the user wanted to make. And since it's binary sorted and the list manager will add the value to the end of the list, this should be guaranteed to be larger than any other value.
        return new OldFroggerMapForm(getMap(), Integer.MAX_VALUE);
    }

    @Override
    protected void onDelegateRemoved(OldFroggerMapForm oldFroggerMapForm, Void unused) {
        // No 3D Representation, don't need to do anything.
    }
}