package net.highwayfrogs.editor.gui.editor.map.manager;

import javafx.scene.control.ComboBox;
import lombok.Getter;
import net.highwayfrogs.editor.file.map.form.Form;
import net.highwayfrogs.editor.gui.GUIEditorGrid;
import net.highwayfrogs.editor.gui.editor.MapUIController;
import net.highwayfrogs.editor.system.AbstractStringConverter;

/**
 * Manages map forms.
 * Created by Kneesnap on 8/20/2019.
 */
@Getter
public class FormManager extends MapManager {
    private GUIEditorGrid formEditor;
    private Form selectedForm;

    public FormManager(MapUIController controller) {
        super(controller);
    }

    @Override
    public void setupEditor() {
        if (this.formEditor == null)
            this.formEditor = new GUIEditorGrid(getController().getFormGridPane());

        if (this.selectedForm == null && !getMap().getForms().isEmpty())
            this.selectedForm = getMap().getForms().get(0);

        this.formEditor.clearEditor();

        if (this.selectedForm != null) {
            ComboBox<Form> box = this.formEditor.addSelectionBox("Form", getSelectedForm(), getMap().getForms(), newForm -> {
                this.selectedForm = newForm;
                setupEditor();
            });

            box.setConverter(new AbstractStringConverter<>(form -> "Form #" + getMap().getForms().indexOf(form)));
        }

        this.formEditor.addBoldLabel("Management:");
        this.formEditor.addButton("Add Form", () -> {
            this.selectedForm = new Form();
            getMap().getForms().add(this.selectedForm);
            setupEditor();
        });

        if (this.selectedForm != null) {
            this.formEditor.addButton("Remove Form", () -> {
                getMap().getForms().remove(getSelectedForm());
                this.selectedForm = null;
                setupEditor();
            });

            getSelectedForm().setupEditor(this, this.formEditor);
        }
    }
}
