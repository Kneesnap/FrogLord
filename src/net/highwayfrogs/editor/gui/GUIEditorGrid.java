package net.highwayfrogs.editor.gui;

import javafx.collections.FXCollections;
import javafx.geometry.HPos;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.RowConstraints;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Box;
import javafx.util.StringConverter;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.file.standard.IVector;
import net.highwayfrogs.editor.file.standard.SVector;
import net.highwayfrogs.editor.file.standard.Vector;
import net.highwayfrogs.editor.file.standard.psx.PSXMatrix;
import net.highwayfrogs.editor.gui.editor.MOFController;
import net.highwayfrogs.editor.gui.editor.MapUIController;
import net.highwayfrogs.editor.utils.Utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Creates an editor grid.
 * Created by Kneesnap on 1/20/2019.
 */
@SuppressWarnings("UnusedReturnValue")
public class GUIEditorGrid {
    private GridPane gridPane;
    private int rowIndex;

    private static final Image GRAY_IMAGE_XZ = Utils.makeColorImageNoCache(Color.GRAY, 60, 60);
    private static final Image GRAY_IMAGE_Y = Utils.makeColorImageNoCache(Color.GRAY, 15, 60);
    private static final StringConverter<Double> SLIDER_DEGREE_CONVERTER = new StringConverter<Double>() {
        @Override
        public String toString(Double num) {
            double piHalf = Math.PI / 2;
            double piQuarter = Math.PI / 4;
            if (num < -piHalf - piQuarter)
                return "-180";
            if (num < -piQuarter)
                return "-90";
            if (num < -piQuarter / 2)
                return "-45";
            if (num < piQuarter)
                return "0";
            if (num < piHalf)
                return "45";
            if (num < piQuarter + piHalf)
                return "90";
            return "180";
        }

        @Override
        public Double fromString(String string) {
            return Math.toRadians(Double.parseDouble(string) * 2);
        }
    };

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
     * @param label  The label to add.
     * @param height The desired row height.
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
     * @param text   The text to add.
     * @param height The desired row height.
     */
    public Label addNormalLabel(String text, double height) {
        Label label = setupSecondNode(new Label(text), true);
        addRow(height);
        return label;
    }

    /**
     * Add a label.
     * @param boldLabel   The bold label to add.
     * @param normalLabel The normal label to add.
     */
    public void addBoldNormalLabel(String boldLabel, String normalLabel) {
        addBoldNormalLabel(boldLabel, normalLabel, 20);
    }

    /**
     * Add a label.
     * @param boldLabel   The bold label to add.
     * @param normalLabel The normal label to add.
     * @param height      The desired row height.
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
     * @param label  The label to add.
     * @param value  The value of the label.
     * @param height The desired row height.
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
     * Add a float field.
     * @param label  The name.
     * @param number The initial number.
     * @return textField
     */
    public TextField addFloatField(String label, float number, Consumer<Float> setter, Predicate<Float> test) {
        TextField field = addTextField(label, String.valueOf(number), str -> {
            if (!Utils.isNumber(str))
                return false;

            float floatValue = Float.parseFloat(str);
            boolean testPass = test == null || test.test(floatValue);
            if (testPass)
                setter.accept(floatValue);

            return testPass;
        });
        field.setDisable(setter == null);
        return field;
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
    public <E> ComboBox<E> addEnumSelector(String label, E current, E[] values, boolean allowNull, Consumer<E> setter) {
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
    public void addFloatSVector(String text, SVector vector, MapUIController controller) {
        addFloatVector(text, vector, null, controller, vector.defaultBits());
    }

    /**
     * Add a float Vector for editing.
     * @param text   The name of the SVector.
     * @param vector The SVector itself.
     */
    public void addFloatVector(String text, Vector vector, Runnable update, MapUIController controller) {
        addFloatVector(text, vector, update, controller, vector.defaultBits());
    }

    /**
     * Add a float SVector for editing.
     * @param text   The name of the SVector.
     * @param vector The SVector itself.
     */
    public void addFloatVector(String text, Vector vector, Runnable update, MapUIController controller, int bits) {
        addFloatVector(text, vector, update, controller, bits, null, null);
    }

    /**
     * Add a float SVector for editing.
     * @param text   The name of the SVector.
     * @param vector The SVector itself.
     */
    public void addFloatVector(String text, Vector vector, Runnable update, MapUIController controller, int bits, Vector origin, Box moveBox) {
        if (controller != null && moveBox == null) {
            addBoldLabelButton(text + ":", "Toggle Display", 25,
                    () -> controller.getGeneralManager().updateMarker(controller.getGeneralManager().getShowPosition() == null || !Objects.equals(vector, controller.getGeneralManager().getShowPosition()) ? vector : null, bits, origin, null));
        } else {
            addBoldLabel(text + ":");
        }

        Runnable onPass = () -> {
            if (controller != null)
                controller.getGeneralManager().updateMarker(vector, bits, origin, moveBox);

            if (update != null)
                update.run();
            onChange();
        };

        GridPane vecPane = new GridPane();
        vecPane.addRow(0);

        // Label:
        VBox labelBox = new VBox();
        labelBox.getChildren().add(new Label("X:"));
        labelBox.getChildren().add(new Label("Y:"));
        labelBox.getChildren().add(new Label("Z:"));
        labelBox.setSpacing(10);
        vecPane.addColumn(0, labelBox);

        // XYZ:
        VBox posBox = new VBox();
        TextField xField = new TextField(String.valueOf(vector.getFloatX(bits)));
        TextField yField = new TextField(String.valueOf(vector.getFloatY(bits)));
        TextField zField = new TextField(String.valueOf(vector.getFloatZ(bits)));
        xField.setPrefWidth(60);
        yField.setPrefWidth(60);
        zField.setPrefWidth(60);
        Utils.setHandleKeyPress(xField, str -> {
            if (!Utils.isNumber(str))
                return false;

            vector.setFloatX(Float.parseFloat(str), bits);
            return true;
        }, onPass);
        Utils.setHandleKeyPress(yField, str -> {
            if (!Utils.isNumber(str))
                return false;

            vector.setFloatY(Float.parseFloat(str), bits);
            return true;
        }, onPass);
        Utils.setHandleKeyPress(zField, str -> {
            if (!Utils.isNumber(str))
                return false;

            vector.setFloatZ(Float.parseFloat(str), bits);
            return true;
        }, onPass);

        posBox.getChildren().add(xField);
        posBox.getChildren().add(yField);
        posBox.getChildren().add(zField);
        posBox.setSpacing(2);
        vecPane.addColumn(1, posBox);

        // XZ Move.
        ImageView xzView = new ImageView(GRAY_IMAGE_XZ);
        vecPane.addColumn(2, xzView);

        DragPos[] xzLastDrag = new DragPos[1];
        xzView.setOnMouseClicked(evt -> xzLastDrag[0] = new DragPos(evt.getX(), evt.getY()));
        xzView.setOnMouseDragged(evt -> {
            DragPos lastDrag = xzLastDrag[0];
            if (lastDrag == null) { // Set it up if it's not present.
                xzLastDrag[0] = new DragPos(evt.getX(), evt.getY());
                return;
            }

            double xDiff = -(lastDrag.getX() - evt.getX()) / 10;
            double zDiff = (lastDrag.getY() - evt.getY()) / 10;
            double angle = controller != null ? -Math.toRadians(controller.getCameraFPS().getCamYawProperty().doubleValue()) : Math.PI / 2;

            vector.setFloatX((float) (vector.getFloatX(bits) + (xDiff * Math.cos(angle)) - (zDiff * Math.sin(angle))), bits);
            vector.setFloatZ((float) (vector.getFloatZ(bits) + (xDiff * Math.sin(angle)) + (zDiff * Math.cos(angle))), bits);
            xField.setText(String.valueOf(vector.getFloatX(bits)));
            zField.setText(String.valueOf(vector.getFloatZ(bits)));

            onPass.run();

            lastDrag.setX(evt.getX());
            lastDrag.setY(evt.getY());
        });
        xzView.setOnMouseReleased(evt -> xzLastDrag[0] = null);

        // Y Move.
        ImageView yView = new ImageView(GRAY_IMAGE_Y);
        vecPane.addColumn(3, yView);

        DragPos[] yLastDrag = new DragPos[1];
        yView.setOnMouseClicked(evt -> yLastDrag[0] = new DragPos(evt.getX(), evt.getY()));
        yView.setOnMouseDragged(evt -> {
            DragPos lastDrag = yLastDrag[0];
            if (lastDrag == null) { // Set it up if it's not present.
                yLastDrag[0] = new DragPos(evt.getX(), evt.getY());
                return;
            }

            double yDiff = -(lastDrag.getY() - evt.getY()) / 10;
            vector.setFloatY((float) (vector.getFloatY(bits) + yDiff), bits);
            yField.setText(String.valueOf(vector.getFloatY(bits)));
            onPass.run();

            lastDrag.setX(evt.getX());
            lastDrag.setY(evt.getY());
        });
        yView.setOnMouseReleased(evt -> yLastDrag[0] = null);

        vecPane.setHgap(10);
        GridPane.setColumnSpan(vecPane, 2); // Make it take up the full space in the grid it will be added to.
        setupNode(vecPane); // Setup this in the new area.
        addRow(75);
    }

    /**
     * Add a float SVector for editing.
     * @param text   The name of the SVector.
     * @param vector The SVector itself.
     */
    public void addFloatVector(String text, Vector vector, Runnable update, MOFController controller, int bits, Vector origin, Box moveBox) {
        if (controller != null && moveBox == null) {
            addBoldLabelButton(text + ":", "Toggle Display", 25,
                    () -> controller.updateMarker(controller.getShowPosition() == null || !Objects.equals(vector, controller.getShowPosition()) ? vector : null, bits, origin, null));
        } else {
            addBoldLabel(text + ":");
        }

        Runnable onPass = () -> {
            if (controller != null)
                controller.updateMarker(vector, bits, origin, moveBox);

            if (update != null)
                update.run();
            onChange();
        };

        GridPane vecPane = new GridPane();
        vecPane.addRow(0);

        // Label:
        VBox labelBox = new VBox();
        labelBox.getChildren().add(new Label("X:"));
        labelBox.getChildren().add(new Label("Y:"));
        labelBox.getChildren().add(new Label("Z:"));
        labelBox.setSpacing(10);
        vecPane.addColumn(0, labelBox);

        // XYZ:
        VBox posBox = new VBox();
        TextField xField = new TextField(String.valueOf(vector.getFloatX(bits)));
        TextField yField = new TextField(String.valueOf(vector.getFloatY(bits)));
        TextField zField = new TextField(String.valueOf(vector.getFloatZ(bits)));
        xField.setPrefWidth(60);
        yField.setPrefWidth(60);
        zField.setPrefWidth(60);
        Utils.setHandleKeyPress(xField, str -> {
            if (!Utils.isNumber(str))
                return false;

            vector.setFloatX(Float.parseFloat(str), bits);
            return true;
        }, onPass);
        Utils.setHandleKeyPress(yField, str -> {
            if (!Utils.isNumber(str))
                return false;

            vector.setFloatY(Float.parseFloat(str), bits);
            return true;
        }, onPass);
        Utils.setHandleKeyPress(zField, str -> {
            if (!Utils.isNumber(str))
                return false;

            vector.setFloatZ(Float.parseFloat(str), bits);
            return true;
        }, onPass);

        posBox.getChildren().add(xField);
        posBox.getChildren().add(yField);
        posBox.getChildren().add(zField);
        posBox.setSpacing(2);
        vecPane.addColumn(1, posBox);

        // XZ Move.
        ImageView xzView = new ImageView(GRAY_IMAGE_XZ);
        vecPane.addColumn(2, xzView);

        DragPos[] xzLastDrag = new DragPos[1];
        xzView.setOnMouseClicked(evt -> xzLastDrag[0] = new DragPos(evt.getX(), evt.getY()));
        xzView.setOnMouseDragged(evt -> {
            DragPos lastDrag = xzLastDrag[0];
            if (lastDrag == null) { // Set it up if it's not present.
                xzLastDrag[0] = new DragPos(evt.getX(), evt.getY());
                return;
            }

            double xDiff = -(lastDrag.getX() - evt.getX()) / 10;
            double zDiff = (lastDrag.getY() - evt.getY()) / 10;
            double angle = controller != null ? -Math.toRadians(controller.getRotY().getAngle()) : Math.PI / 2;

            vector.setFloatX((float) (vector.getFloatX(bits) + (xDiff * Math.cos(angle)) - (zDiff * Math.sin(angle))), bits);
            vector.setFloatZ((float) (vector.getFloatZ(bits) + (xDiff * Math.sin(angle)) + (zDiff * Math.cos(angle))), bits);
            xField.setText(String.valueOf(vector.getFloatX(bits)));
            zField.setText(String.valueOf(vector.getFloatZ(bits)));

            onPass.run();

            lastDrag.setX(evt.getX());
            lastDrag.setY(evt.getY());
        });
        xzView.setOnMouseReleased(evt -> xzLastDrag[0] = null);

        // Y Move.
        ImageView yView = new ImageView(GRAY_IMAGE_Y);
        vecPane.addColumn(3, yView);

        DragPos[] yLastDrag = new DragPos[1];
        yView.setOnMouseClicked(evt -> yLastDrag[0] = new DragPos(evt.getX(), evt.getY()));
        yView.setOnMouseDragged(evt -> {
            DragPos lastDrag = yLastDrag[0];
            if (lastDrag == null) { // Set it up if it's not present.
                yLastDrag[0] = new DragPos(evt.getX(), evt.getY());
                return;
            }

            double yDiff = -(lastDrag.getY() - evt.getY()) / 10;
            vector.setFloatY((float) (vector.getFloatY(bits) + yDiff), bits);
            yField.setText(String.valueOf(vector.getFloatY(bits)));
            onPass.run();

            lastDrag.setX(evt.getX());
            lastDrag.setY(evt.getY());
        });
        yView.setOnMouseReleased(evt -> yLastDrag[0] = null);

        vecPane.setHgap(10);
        GridPane.setColumnSpan(vecPane, 2); // Make it take up the full space in the grid it will be added to.
        setupNode(vecPane); // Setup this in the new area.
        addRow(75);
    }

    @Getter
    @Setter
    @AllArgsConstructor
    private static class DragPos {
        private double x;
        private double y;
    }

    /**
     * Add a regular SVector for editing.
     * @param text   The name of the SVector.
     * @param bits   The amount of bits to use.
     * @param vector The SVector itself.
     */
    public void addSVector(String text, int bits, SVector vector, Runnable runnable) {
        addTextField(text, Utils.fixedPointShortToFloatNBits(vector.getX(), bits) + ", " + Utils.fixedPointShortToFloatNBits(vector.getY(), bits) + ", " + Utils.fixedPointShortToFloatNBits(vector.getZ(), bits), newText -> {
            if (!vector.loadFromFloatText(newText, bits))
                return false;

            if (runnable != null)
                runnable.run();
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
     * Adds a button with an enum selector.
     * @param buttonText   The text to show on the button.
     * @param onClick      The handler for when you click on the button.
     * @param values       The enum values.
     * @param currentValue The current enum value.
     */
    public <E> ComboBox<E> addButtonWithEnumSelection(String buttonText, Consumer<E> onClick, E[] values, E currentValue) {
        // Setup button.
        Button button = setupNode(new Button(buttonText));

        // Setup selection.
        ComboBox<E> box = setupSecondNode(new ComboBox<>(FXCollections.observableArrayList(values)), false);
        box.valueProperty().setValue(currentValue); // Set the selected value.
        box.getSelectionModel().select(currentValue); // Automatically scroll to selected value.

        AtomicBoolean firstOpen = new AtomicBoolean(true);
        box.addEventFilter(ComboBox.ON_SHOWN, event -> { // Show the selected value when the dropdown is opened.
            if (firstOpen.getAndSet(false))
                Utils.comboBoxScrollToValue(box);
        });

        button.setOnAction(evt -> {
            onClick.accept(box.getValue());
            onChange();
        });
        addRow(25);

        return box;
    }

    /**
     * Add a label and button.
     * @param labelText  The text on the label.
     * @param buttonText The text on the button.
     * @param onPress    What to do when the button is pressed.
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
     * @param labelText  The text on the label.
     * @param buttonText The text on the button.
     * @param onPress    What to do when the button is pressed.
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

    /**
     * Adds a centered image.
     * @param image The image to add.
     * @return imageView
     */
    public ImageView addCenteredImage(Image image) {
        ImageView view = new ImageView(image);
        GridPane.setHalignment(view, HPos.CENTER);
        view.setFitWidth(image.getWidth());
        view.setFitHeight(image.getHeight());
        setupSecondNode(view, true);
        addRow(image.getHeight() + 5);
        return view;
    }

    /**
     * Add a PSXMatrix to the editor grid.
     * @param matrix           The rotation matrix to add data for.
     * @param onPositionUpdate Behavior to apply when the position is updated.
     */
    public void addEntityMatrix(PSXMatrix matrix, MapUIController controller, Runnable onPositionUpdate) {
        IVector vec = new IVector(matrix.getTransform()[0], matrix.getTransform()[1], matrix.getTransform()[2]);

        addFloatVector("Position", vec, () -> {
            matrix.getTransform()[0] = vec.getX();
            matrix.getTransform()[1] = vec.getY();
            matrix.getTransform()[2] = vec.getZ();
            if (onPositionUpdate != null)
                onPositionUpdate.run(); // Run position hook.
        }, controller, 20);

        addRotationMatrix(matrix, null);
    }

    /**
     * Add PSXMatrix rotation data to the edit grid.
     * @param matrix   The rotation matrix to add data for.
     * @param onUpdate Behavior to apply when the rotation is updated.
     */
    public void addRotationMatrix(PSXMatrix matrix, Runnable onUpdate) {
        addNormalLabel("Rotation:");

        Slider yawUI = addDoubleSlider("Yaw", matrix.getYawAngle(), yaw -> {
            matrix.updateMatrix(yaw, matrix.getPitchAngle(), matrix.getRollAngle());
            if (onUpdate != null)
                onUpdate.run();
        }, -Math.PI, Math.PI);

        Slider pitchUI = addDoubleSlider("Pitch", matrix.getPitchAngle(), pitch -> {
            matrix.updateMatrix(matrix.getYawAngle(), pitch, matrix.getRollAngle());
            if (onUpdate != null)
                onUpdate.run();
        }, -Math.PI / 2, Math.PI / 2); // Cuts off at 90 degrees to prevent gymbal lock.

        Slider rollUI = addDoubleSlider("Roll", matrix.getRollAngle(), roll -> {
            matrix.updateMatrix(matrix.getYawAngle(), matrix.getPitchAngle(), roll);
            if (onUpdate != null)
                onUpdate.run();
        }, -Math.PI, Math.PI);

        yawUI.setLabelFormatter(SLIDER_DEGREE_CONVERTER);
        pitchUI.setLabelFormatter(SLIDER_DEGREE_CONVERTER);
        rollUI.setLabelFormatter(SLIDER_DEGREE_CONVERTER);
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
     * Add height to reach the next row.
     * @param height The height to add.
     */
    public void addRow(double height, double width) {
        RowConstraints newRow = new RowConstraints(height + 1);
        gridPane.getRowConstraints().add(newRow);
        this.rowIndex++;
    }

    /**
     * Add a horizontal separator.
     */
    public void addSeparator(double height) {
        setupSecondNode(new Separator(Orientation.HORIZONTAL), true);
        addRow(height);
    }

    /**
     * Called when a change occurs.
     */
    protected void onChange() {

    }

    /**
     * Add a slider to set the value.
     * @param sliderName   The name of the slider.
     * @param currentValue The current slider value.
     * @param setter       What to do with the slider value on update.
     * @param minValue     The minimum slider value.
     * @param maxValue     The maximum slider value.
     * @return slider
     */
    public Slider addIntegerSlider(String sliderName, int currentValue, Consumer<Integer> setter, int minValue, int maxValue) {
        addLabel(sliderName);
        Slider slider = setupSecondNode(new Slider(minValue, maxValue, currentValue), false);
        slider.setDisable(setter == null);
        slider.valueProperty().addListener(((observable, oldValue, newValue) -> {
            if (setter != null)
                setter.accept(newValue.intValue());
            onChange();
        }));
        slider.setMajorTickUnit((double) (maxValue - minValue) / 4D);
        slider.setShowTickLabels(true);
        slider.setShowTickMarks(true);
        slider.setMinorTickCount(0);
        slider.setBlockIncrement(1);
        addRow(40);
        return slider;
    }

    /**
     * Add a slider to set the value.
     * @param sliderName   The name of the slider.
     * @param currentValue The current slider value.
     * @param setter       What to do with the slider value on update.
     * @param minValue     The minimum slider value.
     * @param maxValue     The maximum slider value.
     * @return slider
     */
    public Slider addDoubleSlider(String sliderName, double currentValue, Consumer<Double> setter, double minValue, double maxValue) {
        return addDoubleSlider(sliderName, currentValue, setter, minValue, maxValue, false);
    }

    /**
	 * Add a slider to set the value.
	 * @param sliderName   The name of the slider.
	 * @param currentValue The current slider value.
	 * @param setter       What to do with the slider value on update.
	 * @param minValue     The minimum slider value.
	 * @param maxValue     The maximum slider value.
	 * @param onRelease    If setter should only be called when mouse is released.
	 * @return slider
	 */
    public Slider addDoubleSlider(String sliderName, double currentValue, Consumer<Double> setter, double minValue, double maxValue, boolean onRelease) {
        addLabel(sliderName);
        Slider slider = setupSecondNode(new Slider(minValue, maxValue, currentValue), false);
        slider.setDisable(setter == null);
        if (onRelease) {
           slider.valueChangingProperty().addListener(((observable, wasChanging, changing) -> {
               if (setter != null && !changing)
                   setter.accept(slider.getValue());
           }));
        }
        slider.valueProperty().addListener(((observable, oldValue, newValue) -> {
            if (setter != null && !onRelease)
                setter.accept(newValue.doubleValue());
            onChange();
        }));
        slider.setMajorTickUnit((maxValue - minValue) / 4);
        slider.setShowTickLabels(true);
        slider.setShowTickMarks(true);
        slider.setMinorTickCount(0);
        slider.setBlockIncrement(1);
        addRow(40);
        return slider;
    }
}
