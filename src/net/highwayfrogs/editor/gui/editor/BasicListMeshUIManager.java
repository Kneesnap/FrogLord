package net.highwayfrogs.editor.gui.editor;

import javafx.collections.FXCollections;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import lombok.Getter;
import net.highwayfrogs.editor.gui.GUIEditorGrid;
import net.highwayfrogs.editor.gui.mesh.DynamicMesh;
import net.highwayfrogs.editor.system.AbstractIndexStringConverter;
import net.highwayfrogs.editor.utils.Utils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A basic mesh UI manager which allows displaying one or more contents of a list in 3D space for editing.
 * Created by Kneesnap on 9/27/2023.
 */
@Getter
public abstract class BasicListMeshUIManager<TMesh extends DynamicMesh, TValue, T3DDelegate> extends MeshUIManager<TMesh> {
    private final Map<TValue, T3DDelegate> delegatesByValue = new HashMap<>();

    // UI:
    private GUIEditorGrid mainGrid;
    private GUIEditorGrid editorGrid;
    private Label valueCountLabel;
    private ComboBox<ListDisplayType> valueDisplaySetting;
    private ComboBox<TValue> valueSelectionBox;
    private Button removeValueButton;

    private static final PhongMaterial MATERIAL_WHITE = Utils.makeSpecialMaterial(Color.WHITE);
    private static final PhongMaterial MATERIAL_YELLOW = Utils.makeSpecialMaterial(Color.YELLOW);

    public BasicListMeshUIManager(MeshViewController<TMesh> controller) {
        super(controller);
    }

    /**
     * Gets the value which is currently selected. Can be null.
     */
    public TValue getSelectedValue() {
        return this.valueSelectionBox.getValue();
    }

    /**
     * Gets the title of the UI accordion pane.
     */
    public abstract String getTitle();

    /**
     * Gets the name of the type of values edited by this manager.
     */
    public abstract String getValueName();

    /**
     * Gets the values
     */
    public abstract List<TValue> getValues();

    /**
     * Test if a value is visible due to the UI.
     */
    public boolean isValueVisibleByUI(TValue value) {
        return value != null && ((this.valueDisplaySetting.getValue() == ListDisplayType.ALL) || (this.valueDisplaySetting.getValue() == ListDisplayType.SELECTED && value == getSelectedValue()));
    }

    @Override
    public void onSetup() {
        super.onSetup();

        // Unchanging UI Fields
        VBox editorBox = this.getController().makeAccordionMenu(getTitle());
        this.mainGrid = getController().makeEditorGrid(editorBox);
        this.setupMainGridEditor(editorBox);

        // Value Creation Button
        Button addValueButton = new Button("Add " + getValueName());
        addValueButton.setOnAction(evt -> {
            TValue newValue = createNewValue();
            if (newValue == null)
                return; // Don't add a new value.

            // Register and update UI.
            getValues().add(newValue);
            createDisplay(newValue); // Creates a new display. (Run early so that when we try to access the 3D delegate from the map as part of forcing the UI to select the new one, the new 3D delegate will be available in the hashmap.)
            updateValuesInUI();
            this.valueSelectionBox.getSelectionModel().selectLast();
        });
        addValueButton.setAlignment(Pos.CENTER);
        this.mainGrid.setupNode(addValueButton);

        // Value Removal Button
        this.removeValueButton = new Button("Remove " + getValueName());
        this.removeValueButton.setOnAction(evt -> {
            if (getSelectedValue() != null)
                removeValue(getSelectedValue());
        });
        this.removeValueButton.setAlignment(Pos.CENTER);
        this.mainGrid.setupSecondNode(this.removeValueButton, false);
        this.mainGrid.addRow();

        // Separator, and grid setup.
        editorBox.getChildren().add(new Separator(Orientation.HORIZONTAL));
        this.editorGrid = this.getController().makeEditorGrid(editorBox);

        // Setup collprims.
        for (TValue value : getValues()) {
            T3DDelegate delegate = createDisplay(value);
            setVisible(value, delegate, isValueVisibleByUI(value));
        }

        // Basic updates.
        updateValuesInUI();
    }

    /**
     * Sets up the main grid editor UI.
     * @param editorBox The box to create the UI inside.
     */
    protected void setupMainGridEditor(VBox editorBox) {
        // Value count label.
        this.valueCountLabel = new Label(getValueName() + " Count: " + getValues().size());
        editorBox.getChildren().add(this.valueCountLabel);

        // Value Selection Box
        this.valueSelectionBox = this.mainGrid.addSelectionBox("Select " + getValueName(), null, getValues(), null);
        this.valueSelectionBox.valueProperty().addListener((listener, oldValue, newValue) -> {
            if (oldValue != newValue) {
                T3DDelegate oldDelegate = this.delegatesByValue.get(oldValue);
                T3DDelegate newDelegate = this.delegatesByValue.get(newValue);

                // Update value visibility.
                if (this.valueDisplaySetting.getValue() == ListDisplayType.SELECTED) {
                    if (oldValue != null)
                        setVisible(oldValue, oldDelegate, false);
                    if (newValue != null)
                        setVisible(newValue, newDelegate, true);
                }

                onSelectedValueChange(oldValue, oldDelegate, newValue, newDelegate);
                if (newValue != null)
                    expandTitlePaneFrom(this.valueSelectionBox);
                this.updateEditor(); // Refresh UI.
            }
        });
        this.valueSelectionBox.setConverter(new AbstractIndexStringConverter<>(getValues(), (index, collprim) -> getValueName() + " " + index));

        // Display Settings Checkbox.
        this.valueDisplaySetting = this.mainGrid.addEnumSelector("Value(s) Displayed", ListDisplayType.SELECTED, ListDisplayType.values(), false, newValue -> {
            if (newValue == ListDisplayType.ALL) {
                setValuesVisible(true);
            } else if (newValue == ListDisplayType.SELECTED) {
                setValuesVisible(false);

                // Make the selected value visible.
                TValue selected = getSelectedValue();
                if (selected != null)
                    setVisible(selected, this.delegatesByValue.get(selected), true);
            } else if (newValue == ListDisplayType.NONE) {
                setValuesVisible(false);
            }
        });
    }

    /**
     * Creates the 3D display for a value.
     * @param value The value to create a 3D delegate for.
     */
    public T3DDelegate createDisplay(TValue value) {
        T3DDelegate newDelegate = setupDisplay(value);
        T3DDelegate oldDelegate = getDelegatesByValue().put(value, newDelegate);
        if (oldDelegate != null)
            onDelegateRemoved(value, oldDelegate);
        return newDelegate;
    }

    /**
     * Sets up the 3D display.
     * @param value The value to setup a 3D delegate for.
     */
    protected abstract T3DDelegate setupDisplay(TValue value);

    /**
     * Update the UI for the selected value.
     * @param selectedValue The value currently selected.
     */
    protected abstract void updateEditor(TValue selectedValue);

    /**
     * Set if all the value delegates should be visible in 3D space.
     * @param valuesVisible If the values should be made visible.
     */
    protected void setValuesVisible(boolean valuesVisible) {
        getDelegatesByValue().forEach((value, delegate) -> setVisible(value, delegate, valuesVisible));
    }

    /**
     * Set if a value is visible.
     * @param value    The value to update the visibility of.
     * @param delegate The delegate to update the visibility of.
     * @param visible  Whether it should be visible.
     */
    protected abstract void setVisible(TValue value, T3DDelegate delegate, boolean visible);

    /**
     * Called when the value currently selected in the UI changes.
     * @param oldValue    The old value previously selected. (Can be null)
     * @param oldDelegate The delegate corresponding to the old value. (Can be null)
     * @param newValue    The new value now selected. (Can be null)
     * @param newDelegate The delegate corresponding to the new value. (Can be null)
     */
    protected abstract void onSelectedValueChange(TValue oldValue, T3DDelegate oldDelegate, TValue newValue, T3DDelegate newDelegate);

    /**
     * Creates a new value to go in the value list.
     */
    protected abstract TValue createNewValue();

    /**
     * Called when a delegate is removed from the list. May or may not indicate the removal of the value as well.
     * @param value    The value corresponding to the removed delegate.
     * @param delegate The 3D delegate which represented the value, if there is one.
     */
    protected abstract void onDelegateRemoved(TValue value, T3DDelegate delegate);

    /**
     * Called when a value is removed from the list.
     * @param removedValue    The value removed from the list.
     * @param removedDelegate The 3D delegate which represented the value, if there was one.
     */
    protected void onValueRemoved(TValue removedValue, T3DDelegate removedDelegate) {
        if (removedDelegate != null)
            this.onDelegateRemoved(removedValue, removedDelegate);
    }

    @Override
    public void updateEditor() {
        super.updateEditor();

        // Setup basics.
        this.editorGrid.clearEditor();
        TValue selectedValue = this.valueSelectionBox.getValue();
        this.removeValueButton.setDisable(selectedValue == null);
        if (selectedValue != null) {
            try {
                updateEditor(selectedValue);
            } catch (Throwable th) {
                th.printStackTrace();
                this.editorGrid.addBoldLabel("An error occurred while setting up the UI.");
            }
        }
    }

    /**
     * Removes a value from the list and any corresponding 3D representation in the scene.
     * @param value The value to remove.
     */
    public void removeValue(TValue value) {
        if (!getValues().remove(value))
            return;

        T3DDelegate delegate = this.delegatesByValue.remove(value);
        onValueRemoved(value, delegate);

        updateValuesInUI();
        this.valueSelectionBox.setValue(null);
        this.valueSelectionBox.getSelectionModel().clearSelection();
    }

    /**
     * Called when the number of values (or the references to the values) changes, to keep the UI up to date.
     */
    protected void updateValuesInUI() {
        int valueCount = getValues().size();
        this.valueCountLabel.setText(getValueName() + " Count: " + valueCount);

        this.valueSelectionBox.setItems(FXCollections.observableArrayList(getValues()));
        this.valueSelectionBox.setConverter(new AbstractIndexStringConverter<>(getValues(), (index, collprim) -> getValueName() + " " + index));
    }

    public enum ListDisplayType {
        NONE,
        SELECTED,
        ALL
    }
}