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
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.util.StringConverter;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.file.standard.SVector;
import net.highwayfrogs.editor.file.standard.psx.PSXMatrix;
import net.highwayfrogs.editor.gui.editor.MAPController;
import net.highwayfrogs.editor.utils.Utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;
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
            if (num < piHalf)
                return "0";
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
    public void addFloatSVector(String text, SVector vector, MAPController controller) {
        addFloatSVector(text, vector, null, controller);
    }

    /**
     * Add a float SVector for editing.
     * @param text   The name of the SVector.
     * @param vector The SVector itself.
     */
    public void addFloatSVector(String text, SVector vector, Runnable update, MAPController controller) {
        //TODO: Support origin offset, for things like camera.
        //TODO: Clean up SVector, its float dealings, and basically just try to keep things nice.

        if (controller != null) {
            addBoldLabelButton(text + ":", "Toggle Display", 25,
                    () -> controller.updateMarker(controller.getShowPosition() == null || !Objects.equals(vector, controller.getShowPosition()) ? vector : null));
        } else {
            addBoldLabel(text + ":");
        }

        Runnable onPass = () -> {
            if (controller != null)
                controller.updateMarker(vector);

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
        TextField xField = new TextField(String.valueOf(vector.getFloatX()));
        TextField yField = new TextField(String.valueOf(vector.getFloatY()));
        TextField zField = new TextField(String.valueOf(vector.getFloatZ()));
        xField.setPrefWidth(60);
        yField.setPrefWidth(60);
        zField.setPrefWidth(60);
        Utils.setHandleKeyPress(xField, str -> {
            if (!Utils.isNumber(str))
                return false;

            vector.setX(Utils.floatToFixedPointShort4Bit(Float.parseFloat(str)));
            return true;
        }, onPass);
        Utils.setHandleKeyPress(yField, str -> {
            if (!Utils.isNumber(str))
                return false;

            vector.setY(Utils.floatToFixedPointShort4Bit(Float.parseFloat(str)));
            return true;
        }, onPass);
        Utils.setHandleKeyPress(zField, str -> {
            if (!Utils.isNumber(str))
                return false;

            vector.setZ(Utils.floatToFixedPointShort4Bit(Float.parseFloat(str)));
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

            vector.setX(Utils.floatToFixedPointShort4Bit((float) (vector.getFloatX() + xDiff)));
            vector.setZ(Utils.floatToFixedPointShort4Bit((float) (vector.getFloatZ() + zDiff)));
            xField.setText(String.valueOf(vector.getFloatX()));
            zField.setText(String.valueOf(vector.getFloatZ()));

            onPass.run();

            lastDrag.setX(evt.getX());
            lastDrag.setY(evt.getY());
        });
        xzView.setOnMouseReleased(xzView.getOnMouseDragged());


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
            vector.setY(Utils.floatToFixedPointShort4Bit((float) (vector.getFloatY() + yDiff)));
            yField.setText(String.valueOf(vector.getFloatY()));
            onPass.run();

            lastDrag.setX(evt.getX());
            lastDrag.setY(evt.getY());
        });
        yView.setOnMouseReleased(yView.getOnMouseDragged());

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
    public <E extends Enum<E>> void addButtonWithEnumSelection(String buttonText, Consumer<E> onClick, E[] values, E currentValue) {
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
    public void addMatrix(PSXMatrix matrix, Runnable onPositionUpdate) {
        float[] translation = new float[3];

        // Position information is in fixed point format, hence conversion to float representation.
        for (int i = 0; i < matrix.getTransform().length; i++)
            translation[i] = Utils.fixedPointIntToFloat20Bit(matrix.getTransform()[i]);

        addNormalLabel("Position:");
        addVector3D(translation, 30D, (index, newValue) -> {
            matrix.getTransform()[index] = Utils.floatToFixedPointInt20Bit(newValue);
            if (onPositionUpdate != null)
                onPositionUpdate.run();
        });

        // Transform information is in fixed point format, hence conversion to float representation.
        addNormalLabel("Rotation:");

        Slider yawUI = addDoubleSlider("Yaw", matrix.getYawAngle(), yaw -> matrix.updateMatrix(yaw, matrix.getPitchAngle(), matrix.getRollAngle()), -Math.PI, Math.PI);
        Slider pitchUI = addDoubleSlider("Pitch", matrix.getPitchAngle(), pitch -> matrix.updateMatrix(matrix.getYawAngle(), pitch, matrix.getRollAngle()), -Math.PI, Math.PI);
        Slider rollUI = addDoubleSlider("Roll", matrix.getRollAngle(), roll -> matrix.updateMatrix(matrix.getYawAngle(), matrix.getPitchAngle(), roll), -Math.PI, Math.PI);

        yawUI.setLabelFormatter(SLIDER_DEGREE_CONVERTER);
        pitchUI.setLabelFormatter(SLIDER_DEGREE_CONVERTER);
        rollUI.setLabelFormatter(SLIDER_DEGREE_CONVERTER);
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
        addLabel(sliderName);
        Slider slider = setupSecondNode(new Slider(minValue, maxValue, currentValue), false);
        slider.setDisable(setter == null);
        slider.valueProperty().addListener(((observable, oldValue, newValue) -> {
            if (setter != null)
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
