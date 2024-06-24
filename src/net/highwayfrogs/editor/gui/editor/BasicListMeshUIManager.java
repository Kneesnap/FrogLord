package net.highwayfrogs.editor.gui.editor;

import javafx.collections.FXCollections;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import lombok.Getter;
import net.highwayfrogs.editor.gui.GUIEditorGrid;
import net.highwayfrogs.editor.gui.mesh.DynamicMesh;
import net.highwayfrogs.editor.utils.Utils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

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
    private Button addValueButton;
    private Button removeValueButton;

    private static final PhongMaterial MATERIAL_WHITE = Utils.makeUnlitSharpMaterial(Color.WHITE);
    private static final PhongMaterial MATERIAL_YELLOW = Utils.makeUnlitSharpMaterial(Color.YELLOW);

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
     * Gets the index of the selected value in the value list.
     * @return selectedValueIndex
     */
    public int getSelectedValueIndex() {
        return this.valueSelectionBox.getSelectionModel().getSelectedIndex();
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
        UISidePanel sidePanel = getController().createSidePanel(getTitle());
        this.mainGrid = sidePanel.makeEditorGrid();
        this.setupMainGridEditor(sidePanel);

        // Value Creation Button
        this.addValueButton = new Button("Add " + getValueName());
        this.addValueButton.setOnAction(evt -> {
            TValue newValue = createNewValue();
            if (newValue == null)
                return; // Don't add a new value.

            // Register and update UI.
            getValues().add(newValue);
            createDisplay(newValue); // Creates a new display. (Run early so that when we try to access the 3D delegate from the map as part of forcing the UI to select the new one, the new 3D delegate will be available in the hashmap.)
            updateValuesInUI();
            this.valueSelectionBox.getSelectionModel().selectLast();
        });
        this.addValueButton.setAlignment(Pos.CENTER);
        this.mainGrid.setupNode(this.addValueButton);

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
        sidePanel.add(new Separator(Orientation.HORIZONTAL));
        this.editorGrid = sidePanel.makeEditorGrid();

        // Setup collprims.
        for (TValue value : getValues()) {
            T3DDelegate delegate = createDisplay(value);
            setVisible(value, delegate, isValueVisibleByUI(value));
        }

        // Basic updates.
        updateValuesInUI();
    }

    /**
     * Gets the name of the value as it displays in the list.
     * @param index the index of the value
     * @param value the value to get the name of
     * @return listDisplayName
     */
    protected String getListDisplayName(int index, TValue value) {
        return getValueName() + " " + index;
    }

    /**
     * Gets the style of the value as it displays in the list.
     * @param index the index of the value
     * @param value the value to get the style of
     * @return listDisplayStyle
     */
    protected String getListDisplayStyle(int index, TValue value) {
        return null;
    }

    /**
     * Gets the image to display in the list.
     * @param index the index of the value
     * @param value the value to get the style of
     * @return listImage
     */
    protected Image getListDisplayImage(int index, TValue value) {
        return null;
    }

    /**
     * Sets up the main grid editor UI.
     * @param sidePanel The side panel to add UI elements to.
     */
    protected void setupMainGridEditor(UISidePanel sidePanel) {
        // Value count label.
        this.valueCountLabel = new Label(getValueName() + " Count: " + getValues().size());
        sidePanel.add(this.valueCountLabel);

        // Value Selection Box
        this.valueSelectionBox = this.mainGrid.addSelectionBox("Select " + getValueName(), null, getValues(), null, 30);
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
        this.valueSelectionBox.setButtonCell(new BasicListEntryCell<>(this));
        this.valueSelectionBox.setCellFactory(param -> new BasicListEntryCell<>(this));

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
        if (Objects.equals(getSelectedValue(), value))
            onSelectedValueChange(value, oldDelegate, value, newDelegate);
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
     * Handles the click of a list element's 3D delegate.
     * @param event The event to handle the click.
     * @param value The value whose delegate was clicked.
     */
    protected void handleClick(MouseEvent event, TValue value) {
        event.consume();
        if (value == getSelectedValue()) {
            getValueSelectionBox().getSelectionModel().clearSelection();
        } else {
            getValueSelectionBox().getSelectionModel().select(value);
        }
    }

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
     * Updates the visibility state of all values to what the UI says they should be.
     */
    public void updateValueVisibility() {
        for (TValue value : getValues())
            setVisible(value, getDelegatesByValue().get(value), isValueVisibleByUI(value));
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
                Utils.handleError(getLogger(), th, false, "An error occurred while setting up the UI.");
                this.editorGrid.addBoldLabel("An error occurred while setting up the UI.");
            }
        }
    }

    /**
     * Removes a value from the list and any corresponding 3D representation in the scene.
     * @param value The value to remove.
     */
    public void removeValue(TValue value) {
        if (!tryRemoveValue(value))
            return;

        T3DDelegate delegate = this.delegatesByValue.remove(value);
        onValueRemoved(value, delegate);

        updateValuesInUI();
        this.valueSelectionBox.setValue(null);
        this.valueSelectionBox.getSelectionModel().clearSelection();
    }

    /**
     * Attempts to remove the value from the list.
     * @param value the value to remove
     * @return removed successfully
     */
    protected boolean tryRemoveValue(TValue value) {
        return getValues().remove(value);
    }

    /**
     * Called when the number of values (or the references to the values) changes, to keep the UI up to date.
     */
    protected void updateValuesInUI() {
        int valueCount = getValues().size();
        this.valueCountLabel.setText(getValueName() + " Count: " + valueCount);

        this.valueSelectionBox.setItems(FXCollections.observableArrayList(getValues()));
    }

    public enum ListDisplayType {
        NONE,
        SELECTED,
        ALL
    }

    private static class BasicListEntryCell<TValue> extends ListCell<TValue> {
        private final BasicListMeshUIManager<?, TValue, ?> listManager;

        public BasicListEntryCell(BasicListMeshUIManager<?, TValue, ?> listManager) {
            this.listManager = listManager;
        }

        @Override
        public void updateItem(TValue value, boolean empty) {
            super.updateItem(value, empty);
            int index = this.listManager.valueSelectionBox.getItems().indexOf(value);
            setText(this.listManager.getListDisplayName(index, value));
            setStyle(this.listManager.getListDisplayStyle(index, value));

            Image image = this.listManager.getListDisplayImage(index, value);
            ImageView imageView = null;
            if (image != null) {
                imageView = new ImageView(image);
                imageView.setFitWidth(20);
                imageView.setFitHeight(20);
            }

            setGraphic(imageView);
        }
    }
}