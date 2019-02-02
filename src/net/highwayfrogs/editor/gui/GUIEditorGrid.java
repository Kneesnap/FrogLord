package net.highwayfrogs.editor.gui;

import com.sun.javafx.scene.control.skin.ComboBoxListViewSkin;
import javafx.collections.FXCollections;
import javafx.geometry.HPos;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.RowConstraints;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.Utils;
import net.highwayfrogs.editor.file.standard.SVector;

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
        Label labelText = setupSecondNode(new Label(label), true);
        labelText.setFont(Constants.SYSTEM_BOLD_FONT);
        addRow(20);
    }

    /**
     * Adds a label
     * @param text The text to add.
     */
    public Label addNormalLabel(String text) {
        Label label = setupSecondNode(new Label(text), true);
        addRow(15);
        return label;
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
        addLabel(label);
        Label valueText = setupSecondNode(new Label(value), false);
        addRow(15);
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
        field.setOnKeyPressed(evt -> {
            KeyCode code = evt.getCode();
            if (field.getStyle().isEmpty() && (code.isLetterKey() || code.isDigitKey() || code == KeyCode.BACK_SPACE)) {
                field.setStyle("-fx-text-inner-color: darkgreen;");
            } else if (code == KeyCode.ENTER) {
                boolean pass = setter.apply(field.getText());
                if (pass)
                    onChange();

                field.setStyle(pass ? null : "-fx-text-inner-color: red;");
            }
        });

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
    @SuppressWarnings("unchecked")
    public <T> ComboBox<T> addSelectionBox(String label, T current, List<T> values, Consumer<T> setter) {
        addLabel(label);
        ComboBox<T> box = setupSecondNode(new ComboBox<>(FXCollections.observableArrayList(values)), false);
        box.valueProperty().setValue(current); // Set the selected value.
        box.getSelectionModel().select(current); // Automatically scroll to selected value.

        AtomicBoolean firstOpen = new AtomicBoolean(true);
        box.addEventFilter(ComboBox.ON_SHOWN, event -> { // Show the selected value when the dropdown is opened.
            if (firstOpen.getAndSet(false))
                ((ComboBoxListViewSkin<T>) box.getSkin()).getListView().scrollTo(box.getValue());
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
     * Add a SVector for editing.
     * @param text   The name of the SVector.
     * @param vector The SVector itself.
     */
    public void addSVector(String text, SVector vector) {
        addSVector(text, vector, null);
    }

    /**
     * Add a SVector for editing.
     * @param text   The name of the SVector.
     * @param vector The SVector itself.
     * @param update The handler for if the SVector is updated.
     */
    public void addSVector(String text, SVector vector, Runnable update) {
        addTextField(text, vector.toCoordinateString(), newText -> {
            newText = newText.replace(" ", "");
            if (!newText.contains(","))
                return false;

            String[] split = newText.split(",");
            if (split.length != 3)
                return false;

            for (String testStr : split)
                if (!Utils.isSignedShort(testStr))
                    return false;

            vector.setX(Short.parseShort(split[0]));
            vector.setY(Short.parseShort(split[1]));
            vector.setZ(Short.parseShort(split[2]));
            if (update != null)
                update.run();
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
        addLabel(text);
        ColorPicker picker = setupSecondNode(new ColorPicker(Utils.fromRGB(color)), false);
        picker.valueProperty().addListener((observable, oldValue, newValue) -> {
            handler.accept(Utils.toRGB(newValue));
            onChange();
        });

        addRow(25);
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
     * Called when a change occurs.
     */
    protected void onChange() {

    }
}
