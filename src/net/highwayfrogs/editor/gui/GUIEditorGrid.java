package net.highwayfrogs.editor.gui;

import javafx.collections.FXCollections;
import javafx.geometry.HPos;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.RowConstraints;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.file.standard.SVector;
import net.highwayfrogs.editor.utils.Utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Creates an editor grid.
 * Created by Kneesnap on 1/20/2019.
 */
@SuppressWarnings("UnusedReturnValue")
public class GUIEditorGrid {
    private GridPane gridPane;
    private int rowIndex;

    public GUIEditorGrid(GridPane pane) {
        this.gridPane = pane;
    }

    /**
     * Clear everything from this editor.
     */
    public void clearEditor() {
        this.rowIndex = 0;
        gridPane.getChildren().clear();
        gridPane.getRowConstraints().clear();
    }

    /**
     * Add a label.
     * @param label The label to add.
     */
    public void addBoldLabel(String label) {
        addBoldLabel(label, 20);
    }

    /**
     * Add a label.
     * @param label     The label to add.
     * @param height    The desired row height.
     */
    public void addBoldLabel(String label, double height) {
        Label labelText = setupSecondNode(new Label(label), true);
        labelText.setFont(Constants.SYSTEM_BOLD_FONT);
        addRow(height);
    }

    /**
     * Adds a label
     * @param text The text to add.
     */
    public Label addNormalLabel(String text) {
        return addNormalLabel(text, 15);
    }

    /**
     * Adds a label
     * @param text      The text to add.
     * @param height    The desired row height.
     */
    public Label addNormalLabel(String text, double height) {
        Label label = setupSecondNode(new Label(text), true);
        addRow(height);
        return label;
    }

    /**
     * Add a label.
     * @param boldLabel     The bold label to add.
     * @param normalLabel   The normal label to add.
     */
    public void addBoldNormalLabel(String boldLabel, String normalLabel) {
        addBoldNormalLabel(boldLabel, normalLabel, 20);
    }

    /**
     * Add a label.
     * @param boldLabel     The bold label to add.
     * @param normalLabel   The normal label to add.
     * @param height    The desired row height.
     */
    public void addBoldNormalLabel(String boldLabel, String normalLabel, double height) {
        Label bold = addLabel(boldLabel);
        bold.setFont(Constants.SYSTEM_BOLD_FONT);
        setupSecondNode(new Label(normalLabel), false);
        addRow(height);
    }

    private Label addLabel(String text) {
        return setupNode(new Label(text));
    }

    /**
     * Add a label.
     * @param label The label to add.
     * @param value The value of the label.
     */
    public Label addLabel(String label, String value) {
        return addLabel(label, value, 15);
    }

    /**
     * Add a label.
     * @param label     The label to add.
     * @param value     The value of the label.
     * @param height    The desired row height.
     */
    public Label addLabel(String label, String value, double height) {
        addLabel(label);
        Label valueText = setupSecondNode(new Label(value), false);
        addRow(height);
        return valueText;
    }

    /**
     * Add a text field.
     * @param label The field description.
     * @param value The field value.
     */
    public TextField addTextField(String label, String value) {
        addLabel(label);
        TextField field = setupSecondNode(new TextField(value), false);
        addRow(25);
        return field;
    }

    /**
     * Add a text field.
     * @param label The field description.
     * @param value The field value.
     */
    public TextField addTextField(String label, String value, Function<String, Boolean> setter) {
        TextField field = addTextField(label, value);
        Utils.setHandleKeyPress(field, setter, this::onChange);
        return field;
    }

    /**
     * Add an integer field.
     * @param label  The name.
     * @param number The initial number.
     * @param setter The success behavior.
     * @param test   Whether the number is valid.
     * @return textField
     */
    public TextField addIntegerField(String label, int number, Consumer<Integer> setter, Function<Integer, Boolean> test) {
        return addTextField(label, String.valueOf(number), str -> {
            if (!Utils.isInteger(str))
                return false;

            int value = Integer.parseInt(str);
            boolean testPass = test == null || test.apply(value);

            if (testPass)
                setter.accept(value);

            return testPass;
        });
    }

    /**
     * Add a float field.
     * @param label  The name.
     * @param number The initial number.
     * @return textField
     */
    public TextField addFloatField(String label, float number) {
        return addTextField(label, String.valueOf(number));
    }

    /**
     * Add a short field.
     * @param label  The name.
     * @param number The initial number.
     * @param setter The success behavior.
     * @param test   Whether the number is valid.
     * @return textField
     */
    public TextField addShortField(String label, short number, Consumer<Short> setter, Function<Short, Boolean> test) {
        return addTextField(label, String.valueOf(number), str -> {
            if (!Utils.isInteger(str))
                return false;

            int intValue = Integer.parseInt(str);
            if (intValue < Short.MIN_VALUE || intValue > Short.MAX_VALUE)
                return false;

            short shortValue = (short) intValue;
            boolean testPass = test == null || test.apply(shortValue);

            if (testPass)
                setter.accept(shortValue);

            return testPass;
        });
    }

    /**
     * Add a selection-box.
     * @param label   The label text to add.
     * @param current The currently selected value.
     * @param values  Accepted values. (If null is acceptable, add null to this list.)
     * @param setter  The setter
     * @return comboBox
     */
    public <T> ComboBox<T> addSelectionBox(String label, T current, List<T> values, Consumer<T> setter) {
        addLabel(label);
        ComboBox<T> box = setupSecondNode(new ComboBox<>(FXCollections.observableArrayList(values)), false);
        box.valueProperty().setValue(current); // Set the selected value.
        box.getSelectionModel().select(current); // Automatically scroll to selected value.

        AtomicBoolean firstOpen = new AtomicBoolean(true);
        box.addEventFilter(ComboBox.ON_SHOWN, event -> { // Show the selected value when the dropdown is opened.
            if (firstOpen.getAndSet(false))
                Utils.comboBoxScrollToValue(box);
        });

        box.valueProperty().addListener((listener, oldVal, newVal) -> {
            setter.accept(newVal);
            onChange();
        });

        addRow(25);
        return box;
    }

    /**
     * Add a selection-box.
     * @param label   The label text to add.
     * @param current The currently selected value.
     * @param values  Accepted values. (If null is acceptable, add null to this list.)
     * @param setter  The setter
     * @return comboBox
     */
    public <E extends Enum<E>> ComboBox<E> addEnumSelector(String label, E current, E[] values, boolean allowNull, Consumer<E> setter) {
        List<E> enumList = new ArrayList<>(Arrays.asList(values));
        if (allowNull)
            enumList.add(0, null);

        return addSelectionBox(label, current, enumList, setter);
    }

    /**
     * Add a checkbox.
     * @param label        The check-box label.
     * @param currentState The current state of the check-box.
     * @param setter       What to do when the checkbox is changed.
     * @return checkBox
     */
    public CheckBox addCheckBox(String label, boolean currentState, Consumer<Boolean> setter) {
        CheckBox box = setupSecondNode(new CheckBox(label), true);
        box.setSelected(currentState);
        box.selectedProperty().addListener((listener, oldVal, newVal) -> {
            setter.accept(newVal);
            onChange();
        });

        addRow(20);
        return box;
    }

    /**
     * Add a float SVector for editing.
     * @param text   The name of the SVector.
     * @param vector The SVector itself.
     */
    public void addFloatSVector(String text, SVector vector) {
        addFloatSVector(text, vector, null);
    }

    /**
     * Add a float SVector for editing.
     * @param text   The name of the SVector.
     * @param vector The SVector itself.
     */
    public void addFloatSVector(String text, SVector vector, Runnable update) {
        addTextField(text, vector.toFloatString(), newText -> {
            if (!vector.loadFromFloatText(newText))
                return false;

            if (update != null)
                update.run();
            onChange();
            return true;
        });
    }

    /**
     * Add a float SVector (representing a normal) for editing.
     * @param text   The name of the SVector.
     * @param vector The SVector itself.
     */
    public void addFloatNormalSVector(String text, SVector vector) {
        addTextField(text, vector.toFloatNormalString());
    }

    /**
     * Add a regular SVector for editing.
     * @param text   The name of the SVector.
     * @param vector The SVector itself.
     */
    public void addRegularSVector(String text, SVector vector) {
        addTextField(text, vector.toRegularString(), newText -> {
            if (!vector.loadFromRegularText(newText))
                return false;

            onChange();
            return true;
        });
    }

    /**
     * Allow selecting a color.
     * @param text    The description of the color.
     * @param color   The starting color.
     * @param handler What to do when a color is selected
     * @return colorPicker
     */
    public ColorPicker addColorPicker(String text, int color, Consumer<Integer> handler) {
        return addColorPicker(text, 25, color, handler);
    }

    /**
     * Allow selecting a color.
     * @param text    The description of the color.
     * @param color   The starting color.
     * @param handler What to do when a color is selected
     * @return colorPicker
     */
    public ColorPicker addColorPicker(String text, double height, int color, Consumer<Integer> handler) {
        addLabel(text);
        ColorPicker picker = setupSecondNode(new ColorPicker(Utils.fromRGB(color)), false);
        picker.valueProperty().addListener((observable, oldValue, newValue) -> {
            handler.accept(Utils.toRGB(newValue));
            onChange();
        });

        addRow(height);
        return picker;
    }

    /**
     * Add a button.
     * @param text    The text on the button.
     * @param onPress What to do when the button is pressed.
     */
    public Button addButton(String text, Runnable onPress) {
        Button button = setupSecondNode(new Button(text), true);
        button.setOnAction(evt -> onPress.run());
        addRow(25);
        return button;
    }

    /**
     * Add a label and button.
     * @param labelText     The text on the label.
     * @param buttonText    The text on the button.
     * @param onPress What to do when the button is pressed.
     */
    public Button addLabelButton(String labelText, String buttonText, double height, Runnable onPress) {
        addLabel(labelText);
        Button button = setupSecondNode(new Button(buttonText), false);
        button.setOnAction(evt -> onPress.run());
        addRow(height);
        return button;
    }

    /**
     * Add a bold label and button.
     * @param labelText     The text on the label.
     * @param buttonText    The text on the button.
     * @param onPress What to do when the button is pressed.
     */
    public Button addBoldLabelButton(String labelText, String buttonText, double height, Runnable onPress) {
        Label bold = addLabel(labelText);
        bold.setFont(Constants.SYSTEM_BOLD_FONT);
        Button button = setupSecondNode(new Button(buttonText), false);
        button.setOnAction(evt -> onPress.run());
        addRow(height);
        return button;
    }

    /**
     * Adds a centered image.
     * @param image      The image to add.
     * @param dimensions The dimensions to display at.
     * @return imageView
     */
    public ImageView addCenteredImage(Image image, double dimensions) {
        ImageView view = new ImageView(image);
        GridPane.setHalignment(view, HPos.CENTER);
        view.setFitWidth(dimensions);
        view.setFitHeight(dimensions);
        setupSecondNode(view, true);
        addRow(dimensions + 5);
        return view;
    }

    public HBox addVector3D(float[] vec3D, double height, BiConsumer<Integer, Float> handler) {
        HBox container;

        // Check to ensure we are receiving a 3-component vector
        if (vec3D.length != 3) {
            container = setupSecondNode(new HBox(new Label("*Invalid vector length*")), true);
        } else {
            HBox hBox = new HBox(1D);

            for (int i = 0; i < vec3D.length; i++) {
                final int tempIndex = i;
                TextField field = new TextField(Float.toString(vec3D[i]));
                field.setAlignment(Pos.CENTER_RIGHT);
                field.textProperty().addListener((observable, oldValue, newValue) -> {
                    if (Utils.isNumber(newValue) && handler != null)
                        handler.accept(tempIndex, Float.parseFloat(newValue));
                });
                hBox.getChildren().add(field);
            }

            container = setupSecondNode(hBox, true);
        }

        addRow(height);
        return container;
    }

    /**
     * Setup the first node.
     * @param node The node to setup.
     * @return node
     */
    public <T extends Node> T setupNode(T node) {
        GridPane.setRowIndex(node, this.rowIndex);
        gridPane.getChildren().add(node);
        return node;
    }

    /**
     * Setup a second node.
     * @param node     The node to setup.
     * @param fullSpan Whether it takes up two columns.
     * @return node
     */
    public <T extends Node> T setupSecondNode(T node, boolean fullSpan) {
        if (fullSpan) {
            GridPane.setColumnSpan(node, 2);
        } else {
            GridPane.setColumnIndex(node, 1);
        }

        return setupNode(node);
    }

    /**
     * Add height to reach the next row.
     * @param height The height to add.
     */
    public void addRow(double height) {
        RowConstraints newRow = new RowConstraints(height + 1);
        gridPane.getRowConstraints().add(newRow);
        this.rowIndex++;
    }

    /**
     * Add a horizontal separator.
     */
    public void addSeparator(double height) {
        Separator sep = setupSecondNode(new Separator(Orientation.HORIZONTAL), true);
        addRow(height);
    }

    /**
     * Called when a change occurs.
     */
    protected void onChange() {

    }
}
