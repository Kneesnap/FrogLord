package net.highwayfrogs.editor.gui;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.geometry.HPos;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.MeshView;
import javafx.scene.shape.Shape3D;
import javafx.util.StringConverter;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.games.psx.math.vector.IVector;
import net.highwayfrogs.editor.games.psx.math.vector.SVector;
import net.highwayfrogs.editor.file.standard.Vector;
import net.highwayfrogs.editor.games.psx.math.PSXMatrix;
import net.highwayfrogs.editor.gui.editor.MeshViewController;
import net.highwayfrogs.editor.gui.mesh.fxobject.ScaleGizmo;
import net.highwayfrogs.editor.gui.mesh.fxobject.ScaleGizmo.IScaleChangeListener;
import net.highwayfrogs.editor.gui.mesh.fxobject.TranslationGizmo;
import net.highwayfrogs.editor.gui.mesh.fxobject.TranslationGizmo.IPositionChangeListener;
import net.highwayfrogs.editor.system.math.Vector3f;
import net.highwayfrogs.editor.utils.*;

import java.text.DecimalFormat;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Creates an editor grid.
 * TODO: Do a cleanup on this class:
 *  - Support a baseline amount of common data structures, for example, Vector3f instead of SVector.
 *   - Per-game options like SVector can have functionality extended through a static utility class.
 *  - Complex features (such as vector position editors) should return an object which access to the various FX nodes, to make customizing individual editors (adding new nodes even), more feasible)
 * Created by Kneesnap on 1/20/2019.
 */
@SuppressWarnings("UnusedReturnValue")
public class GUIEditorGrid {
    @Getter private final GridPane gridPane;
    private int rowIndex;

    private static Vector3f LAST_COPIED_POSITION;

    private static final DecimalFormat FORMAT = new DecimalFormat("#.#######");
    private static final Image GRAY_IMAGE_XZ = ColorUtils.makeColorImageNoCache(Color.GRAY, 60, 60);
    private static final Image GRAY_IMAGE_Y = ColorUtils.makeColorImageNoCache(Color.GRAY, 15, 60);
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
     * Add a text area.
     * @param textToAdd the text to add
     * @return textArea
     */
    public TextArea addTextArea(String textToAdd) {
        TextArea textArea = new TextArea(textToAdd);
        textArea.setWrapText(false);
        textArea.setEditable(false);
        textArea.setPrefHeight(150);
        setupSecondNode(textArea, true);
        addRow(150);
        return textArea;
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
        Label secondLabel = new Label(normalLabel);
        setupSecondNode(new Label(normalLabel), false);
        addRow(height);
        copyTooltipHack(bold, secondLabel);
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
        Label firstLabel = addLabel(label);
        Label valueText = setupSecondNode(new Label(value), false);
        addRow(height);
        copyTooltipHack(firstLabel, valueText);
        return valueText;
    }

    /**
     * Add a text field.
     * @param label The field description.
     * @param value The field value.
     */
    public TextField addTextField(String label, String value) {
        Label labelObj = addLabel(label);
        TextField field = setupSecondNode(new TextField(value), false);
        addRow(25);
        copyTooltipHack(labelObj, field);
        return field;
    }

    /**
     * Add a text field.
     * @param label The field description.
     * @param value The field value.
     */
    public TextField addTextField(String label, String value, Function<String, Boolean> setter) {
        TextField field = addTextField(label, value);
        FXUtils.setHandleKeyPress(field, setter, this::onChange);
        return field;
    }

    /**
     * Add a text field allowing the edit a signed byte.
     * @param label The name of the text field.
     * @param number The initial number.
     * @param setter The handler for a new valid value getting specified.
     * @return textField
     */
    public TextField addSignedByteField(String label, byte number, Consumer<Byte> setter) {
        return this.addSignedByteField(label, number, null, setter);
    }

    /**
     * Add a text field allowing the edit a signed byte.
     * @param label The name of the text field.
     * @param number The initial number.
     * @param validityTest A callback used to test if the number is valid.
     * @param setter The handler for a new valid value getting specified.
     * @return textField
     */
    public TextField addSignedByteField(String label, byte number, Function<Byte, Boolean> validityTest, Consumer<Byte> setter) {
        return addTextField(label, String.valueOf(number), str -> {
            if (!NumberUtils.isInteger(str))
                return false;

            int parsedNumber = Integer.parseInt(str);
            if (parsedNumber > 0x7F || parsedNumber < -0x80)
                return false; // Not within the range of a signed byte.

            // Ensure the value passes the validity test, if one was provided.
            byte parsedByte = (byte) parsedNumber;
            if (validityTest != null) {
                try {
                    if (!validityTest.apply(parsedByte))
                        return false;
                } catch (Throwable th) {
                    // An exception being thrown means either something unexpected meant wrong, or it contains a message to display to the user.
                    Utils.handleError(null, th, true, "The provided value '%d' was not valid.", parsedNumber);
                    return false;
                }
            }

            try {
                setter.accept(parsedByte);
                return true;
            } catch (Throwable th) {
                Utils.handleError(null, th, true, "Failed to apply '%s' value %d.", label, parsedByte);
                return false;
            }
        });
    }

    /**
     * Add a text field allowing the edit an unsigned byte.
     * @param label The name of the text field.
     * @param number The initial number.
     * @param setter The handler for a new valid value getting specified.
     * @return textField
     */
    public TextField addUnsignedByteField(String label, short number, Consumer<Short> setter) {
        return this.addUnsignedByteField(label, number, null, setter);
    }

    /**
     * Add a text field allowing the edit an unsigned byte.
     * @param label The name of the text field.
     * @param number The initial number.
     * @param validityTest A callback used to test if the number is valid.
     * @param setter The handler for a new valid value getting specified.
     * @return textField
     */
    public TextField addUnsignedByteField(String label, short number, Function<Short, Boolean> validityTest, Consumer<Short> setter) {
        return addTextField(label, String.valueOf(number), str -> {
            if (!NumberUtils.isInteger(str))
                return false;

            int parsedNumber = Integer.parseInt(str);
            if (parsedNumber > 0xFF || parsedNumber < 0)
                return false; // Not within the range of an unsigned byte.

            // Ensure the value passes the validity test, if one was provided.
            short parsedShort = (short) parsedNumber;
            if (validityTest != null) {
                try {
                    if (!validityTest.apply(parsedShort))
                        return false;
                } catch (Throwable th) {
                    // An exception being thrown means either something unexpected meant wrong, or it contains a message to display to the user.
                    Utils.handleError(null, th, true, "The provided value '%d' was not valid.", parsedNumber);
                    return false;
                }
            }

            try {
                setter.accept(parsedShort);
                return true;
            } catch (Throwable th) {
                Utils.handleError(null, th, true, "Failed to apply '%s' value %d.", label, parsedShort);
                return false;
            }
        });
    }

    /**
     * Add a text field allowing the edit a signed short.
     * @param label The name of the text field.
     * @param number The initial number.
     * @param setter The handler for a new valid value getting specified.
     * @return textField
     */
    public TextField addSignedShortField(String label, short number, Consumer<Short> setter) {
        return this.addSignedShortField(label, number, null, setter);
    }

    /**
     * Add a text field allowing the edit a signed short.
     * @param label The name of the text field.
     * @param number The initial number.
     * @param validityTest A callback used to test if the number is valid.
     * @param setter The handler for a new valid value getting specified.
     * @return textField
     */
    public TextField addSignedShortField(String label, short number, Function<Short, Boolean> validityTest, Consumer<Short> setter) {
        return addTextField(label, String.valueOf(number), str -> {
            if (!NumberUtils.isInteger(str))
                return false;

            int parsedNumber = Integer.parseInt(str);
            if (parsedNumber > 0x7FFF || parsedNumber < -0x8000)
                return false; // Not within the range of a signed short.

            // Ensure the value passes the validity test, if one was provided.
            short parsedShort = (short) parsedNumber;
            if (validityTest != null) {
                try {
                    if (!validityTest.apply(parsedShort))
                        return false;
                } catch (Throwable th) {
                    // An exception being thrown means either something unexpected meant wrong, or it contains a message to display to the user.
                    Utils.handleError(null, th, true, "The provided value '%d' was not valid.", parsedNumber);
                    return false;
                }
            }

            try {
                setter.accept(parsedShort);
                return true;
            } catch (Throwable th) {
                Utils.handleError(null, th, true, "Failed to apply '%s' value %d.", label, parsedShort);
                return false;
            }
        });
    }

    /**
     * Add a text field allowing the edit an unsigned short.
     * @param label The name of the text field.
     * @param number The initial number.
     * @param setter The handler for a new valid value getting specified.
     * @return textField
     */
    public TextField addUnsignedShortField(String label, short number, Consumer<Short> setter) {
        return this.addUnsignedShortField(label, number, null, setter);
    }

    /**
     * Add a text field allowing the edit an unsigned short.
     * @param label The name of the text field.
     * @param number The initial number.
     * @param setter The handler for a new valid value getting specified.
     * @return textField
     */
    public TextField addUnsignedShortField(String label, int number, Consumer<Integer> setter) {
        return this.addUnsignedShortField(label, number, null, setter);
    }

    /**
     * Add a text field allowing the edit an unsigned short.
     * @param label The name of the text field.
     * @param number The initial number.
     * @param validityTest A callback used to test if the number is valid.
     * @param setter The handler for a new valid value getting specified.
     * @return textField
     */
    public TextField addUnsignedShortField(String label, short number, Function<Short, Boolean> validityTest, Consumer<Short> setter) {
        return addUnsignedShortField(label, DataUtils.shortToUnsignedInt(number),
                validityTest != null ? intValue -> validityTest.apply(DataUtils.unsignedIntToShort(intValue)) : null,
                newValue -> setter.accept(DataUtils.unsignedIntToShort(newValue)));
    }

    /**
     * Add a text field allowing the edit an unsigned short.
     * @param label The name of the text field.
     * @param number The initial number.
     * @param validityTest A callback used to test if the number is valid.
     * @param setter The handler for a new valid value getting specified.
     * @return textField
     */
    public TextField addUnsignedShortField(String label, int number, Function<Integer, Boolean> validityTest, Consumer<Integer> setter) {
        return addTextField(label, String.valueOf(number), str -> {
            if (!NumberUtils.isInteger(str))
                return false;

            int parsedNumber = Integer.parseInt(str);
            if (parsedNumber > 0xFFFF || parsedNumber < 0)
                return false; // Not within the range of an unsigned short.

            // Ensure the value passes the validity test, if one was provided.
            if (validityTest != null) {
                try {
                    if (!validityTest.apply(parsedNumber))
                        return false;
                } catch (Throwable th) {
                    // An exception being thrown means either something unexpected meant wrong, or it contains a message to display to the user.
                    Utils.handleError(null, th, true, "The provided value '%d' was not valid.", parsedNumber);
                    return false;
                }
            }

            try {
                setter.accept(parsedNumber);
                return true;
            } catch (Throwable th) {
                Utils.handleError(null, th, true, "Failed to apply '%s' value %d.", label, parsedNumber);
                return false;
            }
        });
    }

    /**
     * Add a text field allowing the edit a signed integer.
     * @param label The name of the text field.
     * @param number The initial number.
     * @param setter The handler for a new valid value getting specified.
     * @return textField
     */
    public TextField addSignedIntegerField(String label, int number, Consumer<Integer> setter) {
        return this.addSignedIntegerField(label, number, null, setter);
    }

    /**
     * Add a text field allowing the edit a signed integer.
     * @param label The name of the text field.
     * @param number The initial number.
     * @param validityTest A callback used to test if the number is valid.
     * @param setter The handler for a new valid value getting specified.
     * @return textField
     */
    public TextField addSignedIntegerField(String label, int number, Function<Integer, Boolean> validityTest, Consumer<Integer> setter) {
        return addTextField(label, String.valueOf(number), str -> {
            if (!NumberUtils.isInteger(str))
                return false;

            long parsedNumber = Long.parseLong(str);
            if (parsedNumber > 0x7FFFFFFFL || parsedNumber < -0x80000000L)
                return false; // Not within the range of a signed byte.

            // Ensure the value passes the validity test, if one was provided.
            int parsedInteger = (int) parsedNumber;
            if (validityTest != null) {
                try {
                    if (!validityTest.apply(parsedInteger))
                        return false;
                } catch (Throwable th) {
                    // An exception being thrown means either something unexpected meant wrong, or it contains a message to display to the user.
                    Utils.handleError(null, th, true, "The provided value '%d' was not valid.", parsedInteger);
                    return false;
                }
            }

            try {
                setter.accept(parsedInteger);
                return true;
            } catch (Throwable th) {
                Utils.handleError(null, th, true, "Failed to apply '%s' value %d.", label, parsedInteger);
                return false;
            }
        });
    }

    /**
     * Add a text field allowing the edit an unsigned integer.
     * @param label The name of the text field.
     * @param number The initial number.
     * @param setter The handler for a new valid value getting specified.
     * @return textField
     */
    public TextField addUnsignedIntegerField(String label, int number, Consumer<Integer> setter) {
        return addUnsignedIntegerField(label, number, null, setter);
    }

    /**
     * Add a text field allowing the edit an unsigned integer.
     * @param label The name of the text field.
     * @param number The initial number.
     * @param setter The handler for a new valid value getting specified.
     * @return textField
     */
    public TextField addUnsignedIntegerField(String label, long number, Consumer<Long> setter) {
        return this.addUnsignedIntegerField(label, number, null, setter);
    }

    /**
     * Add a text field allowing the edit an unsigned integer.
     * @param label The name of the text field.
     * @param number The initial number.
     * @param validityTest A callback used to test if the number is valid.
     * @param setter The handler for a new valid value getting specified.
     * @return textField
     */
    public TextField addUnsignedIntegerField(String label, int number, Function<Integer, Boolean> validityTest, Consumer<Integer> setter) {
        return addUnsignedIntegerField(label, DataUtils.intToUnsignedLong(number),
                validityTest != null ? longValue -> validityTest.apply(DataUtils.unsignedLongToInt(longValue)) : null,
                newValue -> setter.accept(DataUtils.unsignedLongToInt(newValue)));
    }

    /**
     * Add a text field allowing the edit an unsigned integer.
     * @param label The name of the text field.
     * @param number The initial number.
     * @param validityTest A callback used to test if the number is valid.
     * @param setter The handler for a new valid value getting specified.
     * @return textField
     */
    public TextField addUnsignedIntegerField(String label, long number, Function<Long, Boolean> validityTest, Consumer<Long> setter) {
        return addTextField(label, String.valueOf(number), str -> {
            if (!NumberUtils.isInteger(str))
                return false;

            long parsedNumber = Long.parseLong(str);
            if (parsedNumber > 0xFFFFFFFFL || parsedNumber < 0)
                return false; // Not within the range of an unsigned byte.

            // Ensure the value passes the validity test, if one was provided.
            if (validityTest != null) {
                try {
                    if (!validityTest.apply(parsedNumber))
                        return false;
                } catch (Throwable th) {
                    // An exception being thrown means either something unexpected meant wrong, or it contains a message to display to the user.
                    Utils.handleError(null, th, true, "The provided value '%d' was not valid.", parsedNumber);
                    return false;
                }
            }

            try {
                setter.accept(parsedNumber);
                return true;
            } catch (Throwable th) {
                Utils.handleError(null, th, true, "Failed to apply '%s' value %d.", label, parsedNumber);
                return false;
            }
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
            if (!NumberUtils.isNumber(str))
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
     * Add a double field.
     * @param label  The name.
     * @param number The initial number.
     * @return textField
     */
    public TextField addDoubleField(String label, double number, Consumer<Double> setter, Predicate<Double> test) {
        TextField field = addTextField(label, String.valueOf(number), str -> {
            if (!NumberUtils.isNumber(str))
                return false;

            double doubleValue = Double.parseDouble(str);
            boolean testPass = test == null || test.test(doubleValue);
            if (testPass)
                setter.accept(doubleValue);

            return testPass;
        });
        field.setDisable(setter == null);
        return field;
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
        return addSelectionBox(label, current, values, setter, 25);
    }

    /**
     * Add a selection-box.
     * @param label   The label text to add.
     * @param current The currently selected value.
     * @param values  Accepted values. (If null is acceptable, add null to this list.)
     * @param setter  The setter
     * @return comboBox
     */
    public <T> ComboBox<T> addSelectionBox(String label, T current, List<T> values, Consumer<T> setter, double height) {
        Label fxLabel = addLabel(label);
        ComboBox<T> box = setupSecondNode(new ComboBox<>(FXCollections.observableArrayList(values)), false);
        box.valueProperty().setValue(current); // Set the selected value.
        box.getSelectionModel().select(current); // Automatically scroll to selected value.

        AtomicBoolean firstOpen = new AtomicBoolean(true);
        box.addEventFilter(ComboBox.ON_SHOWN, event -> { // Show the selected value when the dropdown is opened.
            if (firstOpen.getAndSet(false))
                FXUtils.comboBoxScrollToValue(box);
        });

        if (setter != null) {
            box.valueProperty().addListener((listener, oldVal, newVal) -> {
                setter.accept(newVal);
                onChange();
            });
        }

        addRow(height);
        copyTooltipHack(fxLabel, box);
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
            if (setter != null)
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
    public void addFloatSVector(String text, SVector vector, MeshViewController<?> controller) {
        addFloatVector(text, vector, null, controller, vector.defaultBits());
    }

    /**
     * Add a float Vector for editing.
     * @param text   The name of the SVector.
     * @param vector The SVector itself.
     */
    public void addFloatVector(String text, Vector vector, Runnable update, MeshViewController<?> controller) {
        addFloatVector(text, vector, update, controller, vector.defaultBits());
    }

    /**
     * Add a float Vector for editing.
     * @param text   The name of the SVector.
     * @param vector The SVector itself.
     */
    public void addFloatVector(String text, Vector vector, Runnable update, MeshViewController<?> controller, BiConsumer<Vector, Integer> positionSelector) {
        addFloatVector(text, vector, update, controller, vector.defaultBits(), null, null, positionSelector);
    }

    /**
     * Add a float SVector for editing.
     * @param text   The name of the SVector.
     * @param vector The SVector itself.
     */
    public void addFloatVector(String text, Vector vector, Runnable update, MeshViewController<?> controller, int bits) {
        addFloatVector(text, vector, update, controller, bits, null, null, null);
    }

    /**
     * Add a float SVector for editing.
     * @param text   The name of the SVector.
     * @param vector The SVector itself.
     */
    public void addFloatVector(String text, Vector vector, Runnable update, MeshViewController<?> controller, int bits, Vector origin, Shape3D visualRepresentative, BiConsumer<Vector, Integer> positionSelector) {
        if (controller != null && visualRepresentative == null) {
            addBoldLabelButton(text + ":", "Toggle Display", 25,
                    () -> controller.getMarkerManager().updateMarker(controller.getMarkerManager().getShowPosition() == null || !Objects.equals(vector, controller.getMarkerManager().getShowPosition()) ? vector : null, bits, origin, null));
        } else {
            addBoldLabel(text + ":");
        }

        Runnable onPass = () -> {
            if (controller != null)
                controller.getMarkerManager().updateMarker(vector, bits, origin, visualRepresentative);

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
        FXUtils.setHandleKeyPress(xField, str -> {
            if (!NumberUtils.isNumber(str))
                return false;

            vector.setFloatX(Float.parseFloat(str), bits);
            return true;
        }, onPass);
        FXUtils.setHandleKeyPress(yField, str -> {
            if (!NumberUtils.isNumber(str))
                return false;

            vector.setFloatY(Float.parseFloat(str), bits);
            return true;
        }, onPass);
        FXUtils.setHandleKeyPress(zField, str -> {
            if (!NumberUtils.isNumber(str))
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
            double angle = controller != null ? -Math.toRadians(controller.getFirstPersonCamera().getCamYawProperty().doubleValue()) : Math.PI / 2;

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
        setupSecondNode(vecPane, true); // Setup this in the new area.
        addRow(85);

        GridPane vecPaneExtraButtons = new GridPane();
        vecPaneExtraButtons.addRow(0);

        // Copy the position so it can be pasted later
        Button copyButton = new Button("Copy");
        copyButton.setOnMouseClicked(evt -> {
            if (LAST_COPIED_POSITION == null)
                LAST_COPIED_POSITION = new Vector3f();

            LAST_COPIED_POSITION.setXYZ(vector.getFloatX(bits), vector.getFloatY(bits), vector.getFloatZ(bits));
        });

        vecPaneExtraButtons.addColumn(0, copyButton);

        // Paste the position that has been previously copied
        Button pasteButton = new Button("Paste");
        pasteButton.setOnMouseClicked(evt -> {
            if (LAST_COPIED_POSITION == null) {
                FXUtils.showPopup(AlertType.WARNING, "Cannot paste position.", "No position has been copied, so no position can be pasted.");
                return;
            }

            vector.setFloatX(LAST_COPIED_POSITION.getX(), bits);
            vector.setFloatY(LAST_COPIED_POSITION.getY(), bits);
            vector.setFloatZ(LAST_COPIED_POSITION.getZ(), bits);
            xField.setText(String.valueOf(vector.getFloatX(bits)));
            yField.setText(String.valueOf(vector.getFloatY(bits)));
            zField.setText(String.valueOf(vector.getFloatZ(bits)));
            onPass.run();
        });

        vecPaneExtraButtons.addColumn(1, pasteButton);

        // Relocate the position to whatever polygon is selected
        Button selectButton = new Button("Select");
        if (positionSelector != null) {
            selectButton.setOnMouseClicked(evt -> {
                Vector oldPosition = vector.clone();
                positionSelector.accept(vector, bits);

                if (!oldPosition.equals(vector)) {
                    xField.setText(String.valueOf(vector.getFloatX(bits)));
                    yField.setText(String.valueOf(vector.getFloatY(bits)));
                    zField.setText(String.valueOf(vector.getFloatZ(bits)));
                    onPass.run();
                }
            });
        } else {
            selectButton.setDisable(true); // No behavior linked.
        }

        vecPaneExtraButtons.addColumn(2, selectButton);

        vecPaneExtraButtons.setHgap(10);
        setupSecondNode(vecPaneExtraButtons, true); // Setup this in the new area.
        addRow(35);
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
        addTextField(text, DataUtils.fixedPointShortToFloatNBits(vector.getX(), bits) + ", " + DataUtils.fixedPointShortToFloatNBits(vector.getY(), bits) + ", " + DataUtils.fixedPointShortToFloatNBits(vector.getZ(), bits), newText -> {
            if (!vector.loadFromFloatText(newText, bits))
                return false;

            if (runnable != null)
                runnable.run();
            onChange();
            return true;
        });
    }

    /**
     * Add a float SVector for editing.
     * @param controller the UI controller responsible for gizmo management
     * @param identifier the UUID identifying the gizmo mesh view display
     * @param text the text representing the position
     * @param vector the vector containing positional data
     * @param listener the listener which handles a new positional update
     */
    public AtomicReference<MeshView> addPositionEditor(MeshViewController<?> controller, UUID identifier, String text, Vector vector, IPositionChangeListener listener) {
        return addPositionEditor(controller, identifier, text, vector, vector.defaultBits(), listener);
    }

    /**
     * Add a position offset vector for editing.
     * @param controller the UI controller responsible for gizmo management
     * @param identifier the UUID identifying the gizmo mesh view display
     * @param text the text representing the position
     * @param vector the vector containing positional data
     * @param listener the listener which handles a new positional update
     */
    public AtomicReference<MeshView> addPositionOffsetEditor(MeshViewController<?> controller, UUID identifier, String text, Vector vector, double offsetX, double offsetY, double offsetZ, IPositionChangeListener listener) {
        return addPositionOffsetEditor(controller, identifier, text, vector, vector.defaultBits(), offsetX, offsetY, offsetZ, listener);
    }

    /**
     * Add a position offset vector for editing.
     * @param controller the UI controller responsible for gizmo management
     * @param identifier the UUID identifying the gizmo mesh view display
     * @param text the text representing the position
     * @param vector the vector containing positional data
     * @param offset the offset to apply to the position
     * @param listener the listener which handles a new positional update
     */
    public AtomicReference<MeshView> addPositionOffsetEditor(MeshViewController<?> controller, UUID identifier, String text, Vector vector, Vector offset, IPositionChangeListener listener) {
        return addPositionOffsetEditor(controller, identifier, text, vector, vector.defaultBits(), offset, listener);
    }

    /**
     * Add a position vector for editing.
     * @param controller the UI controller responsible for gizmo management
     * @param identifier the UUID identifying the gizmo mesh view display
     * @param text the text representing the position
     * @param vector the vector containing positional data
     * @param bits the number of integer bits the vector uses for its fixed point conversions
     * @param listener the listener which handles a new positional update
     */
    public AtomicReference<MeshView> addPositionEditor(MeshViewController<?> controller, UUID identifier, String text, Vector vector, int bits, IPositionChangeListener listener) {
        double worldX = vector.getFloatX(bits);
        double worldY = vector.getFloatY(bits);
        double worldZ = vector.getFloatZ(bits);
        return addPositionEditor(controller, identifier, text, worldX, worldY, worldZ, (meshView, oldX, oldY, oldZ, newX, newY, newZ, flags) -> {
            // Update vector.
            if ((flags & TranslationGizmo.X_CHANGED_FLAG) == TranslationGizmo.X_CHANGED_FLAG)
                vector.setFloatX((float) newX, bits);
            if ((flags & TranslationGizmo.Y_CHANGED_FLAG) == TranslationGizmo.Y_CHANGED_FLAG)
                vector.setFloatY((float) newY, bits);
            if ((flags & TranslationGizmo.Z_CHANGED_FLAG) == TranslationGizmo.Z_CHANGED_FLAG)
                vector.setFloatZ((float) newZ, bits);

            // Fire listener.
            if (listener != null)
                listener.handle(meshView, oldX, oldY, oldZ, newX, newY, newZ, flags);
        });
    }

    /**
     * Add a position offset vector for editing.
     * @param controller the UI controller responsible for gizmo management
     * @param identifier the UUID identifying the gizmo mesh view display
     * @param text the text representing the position
     * @param vector the vector containing positional data
     * @param bits the number of integer bits the vector uses for its fixed point conversions
     * @param offset the offset to apply to the position
     * @param listener the listener which handles a new positional update
     */
    public AtomicReference<MeshView> addPositionOffsetEditor(MeshViewController<?> controller, UUID identifier, String text, Vector vector, int bits, Vector offset, IPositionChangeListener listener) {
        return addPositionOffsetEditor(controller, identifier, text, vector, bits, offset.getFloatX(bits), offset.getFloatY(bits), offset.getFloatZ(bits), listener);
    }

    /**
     * Add a position offset vector for editing.
     * @param controller the UI controller responsible for gizmo management
     * @param identifier the UUID identifying the gizmo mesh view display
     * @param text the text representing the position
     * @param vector the vector containing positional data
     * @param bits the number of integer bits the vector uses for its fixed point conversions
     * @param listener the listener which handles a new positional update
     */
    public AtomicReference<MeshView> addPositionOffsetEditor(MeshViewController<?> controller, UUID identifier, String text, Vector vector, int bits, double offsetX, double offsetY, double offsetZ, IPositionChangeListener listener) {
        double worldX = vector.getFloatX(bits) + offsetX;
        double worldY = vector.getFloatY(bits) + offsetY;
        double worldZ = vector.getFloatZ(bits) + offsetZ;
        return addPositionEditor(controller, identifier, text, worldX, worldY, worldZ, (meshView, oldX, oldY, oldZ, newX, newY, newZ, flags) -> {
            // Update vector.
            if ((flags & TranslationGizmo.X_CHANGED_FLAG) == TranslationGizmo.X_CHANGED_FLAG)
                vector.setFloatX((float) (newX - offsetX), bits);
            if ((flags & TranslationGizmo.Y_CHANGED_FLAG) == TranslationGizmo.Y_CHANGED_FLAG)
                vector.setFloatY((float) (newY - offsetY), bits);
            if ((flags & TranslationGizmo.Z_CHANGED_FLAG) == TranslationGizmo.Z_CHANGED_FLAG)
                vector.setFloatZ((float) (newZ - offsetZ), bits);

            // Fire listener.
            if (listener != null)
                listener.handle(meshView, oldX - offsetX, oldY - offsetY, oldZ - offsetZ, newX - offsetX, newY - offsetY, newZ - offsetZ, flags);
        });
    }

    /**
     * Creates a position editor.
     * @param controller the UI controller responsible for gizmo management
     * @param identifier the UUID identifying the gizmo mesh view display
     * @param text the text representing the position
     * @param x the x world position
     * @param y the y world position
     * @param z the z world position
     * @param listener the listener which handles a new positional update
     */
    public AtomicReference<MeshView> addPositionEditor(MeshViewController<?> controller, UUID identifier, String text, double x, double y, double z, IPositionChangeListener listener) {
        return addPositionEditor(controller, identifier, text, x, y, z, 1, listener);
    }

    /**
     * Creates a position editor.
     * @param controller the UI controller responsible for gizmo management
     * @param identifier the UUID identifying the gizmo mesh view display
     * @param text the text representing the position
     * @param x the x world position
     * @param y the y world position
     * @param z the z world position
     * @param scale the scale of the gizmo
     * @param listener the listener which handles a new positional update
     */
    public AtomicReference<MeshView> addPositionEditor(MeshViewController<?> controller, UUID identifier, String text, double x, double y, double z, double scale, IPositionChangeListener listener) {
        TextField[] textFields = new TextField[3];
        double[] positionCache = new double[]{x, y, z};

        IPositionChangeListener ourListener = (meshView, oldX, oldY, oldZ, newX, newY, newZ, flags) -> {
            if ((flags & TranslationGizmo.X_CHANGED_FLAG) == TranslationGizmo.X_CHANGED_FLAG) {
                textFields[0].setText(String.valueOf(newX));
                positionCache[0] = newX;
            }

            if ((flags & TranslationGizmo.Y_CHANGED_FLAG) == TranslationGizmo.Y_CHANGED_FLAG) {
                textFields[1].setText(String.valueOf(newY));
                positionCache[1] = newY;
            }

            if ((flags & TranslationGizmo.Z_CHANGED_FLAG) == TranslationGizmo.Z_CHANGED_FLAG) {
                textFields[2].setText(String.valueOf(newZ));
                positionCache[2] = newZ;
            }

            // Fire listener.
            if (listener != null)
                listener.handle(meshView, oldX, oldY, oldZ, newX, newY, newZ, flags);
        };


        AtomicReference<MeshView> meshViewRef = new AtomicReference<>(controller.getMarkerManager().updateGizmo(identifier, x, y, z, ourListener));
        if (controller != null) {
            addBoldLabelButton(text + ":", "Toggle Display", 25,
                    () -> meshViewRef.set(controller.getMarkerManager().toggleGizmo(identifier, x, y, z, scale, ourListener)));
        }

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
        TextField xField = textFields[0] = new TextField(String.valueOf(x));
        TextField yField = textFields[1] = new TextField(String.valueOf(y));
        TextField zField = textFields[2] = new TextField(String.valueOf(z));
        xField.setPrefWidth(60);
        yField.setPrefWidth(60);
        zField.setPrefWidth(60);
        FXUtils.setHandleKeyPress(xField, str -> {
            if (!NumberUtils.isNumber(str))
                return false;

            double newValue = Double.parseDouble(str);
            if (!Double.isFinite(newValue))
                return false;

            // Update cache.
            double oldX = positionCache[0];
            positionCache[0] = newValue;

            // Fire listener, and update gizmo position.
            MeshView meshView = meshViewRef.get();
            if (meshView != null) {
                ((TranslationGizmo) meshView.getMesh()).setPositionX(meshView, newValue, true);
            } else {
                listener.handle(null, oldX, positionCache[1], positionCache[2], newValue, positionCache[1], positionCache[2], TranslationGizmo.X_CHANGED_FLAG);
            }

            return true;
        }, this::onChange);
        FXUtils.setHandleKeyPress(yField, str -> {
            if (!NumberUtils.isNumber(str))
                return false;

            double newValue = Double.parseDouble(str);
            if (!Double.isFinite(newValue))
                return false;

            // Update cache.
            double oldY = positionCache[1];
            positionCache[1] = newValue;

            // Fire listener, and update gizmo position.
            MeshView meshView = meshViewRef.get();
            if (meshView != null) {
                ((TranslationGizmo) meshView.getMesh()).setPositionY(meshView, newValue, true);
            } else {
                listener.handle(null, positionCache[0], oldY, positionCache[2], positionCache[0], newValue, positionCache[2], TranslationGizmo.Y_CHANGED_FLAG);
            }

            return true;
        }, this::onChange);
        FXUtils.setHandleKeyPress(zField, str -> {
            if (!NumberUtils.isNumber(str))
                return false;

            double newValue = Double.parseDouble(str);
            if (!Double.isFinite(newValue))
                return false;

            // Update cache.
            double oldZ = positionCache[2];
            positionCache[2] = newValue;

            // Fire listener, and update gizmo position.
            MeshView meshView = meshViewRef.get();
            if (meshView != null) {
                ((TranslationGizmo) meshView.getMesh()).setPositionZ(meshView, newValue, true);
            } else {
                listener.handle(null, positionCache[0], positionCache[1], oldZ, positionCache[0], positionCache[1], newValue, TranslationGizmo.Z_CHANGED_FLAG);
            }

            return true;
        }, this::onChange);

        posBox.getChildren().add(xField);
        posBox.getChildren().add(yField);
        posBox.getChildren().add(zField);
        posBox.setSpacing(2);
        vecPane.addColumn(1, posBox);

        vecPane.setHgap(10);
        GridPane.setColumnSpan(vecPane, 2); // Make it take up the full space in the grid it will be added to.
        setupNode(vecPane); // Setup this in the new area.
        addRow(75);

        return meshViewRef;
    }

    /**
     * Creates a position editor.
     * @param controller the UI controller responsible for gizmo management
     * @param identifier the UUID identifying the gizmo mesh view display
     * @param text the text representing the position
     * @param x the x world position
     * @param y the y world position
     * @param z the z world position
     * @param scaleX the x scale
     * @param scaleY the y scale
     * @param scaleZ the z scale
     * @param listener the listener which handles a new positional update
     */
    public AtomicReference<MeshView> addScaleEditor(MeshViewController<?> controller, UUID identifier, String text, double x, double y, double z, double scaleX, double scaleY, double scaleZ, IScaleChangeListener listener) {
        return addScaleEditor(controller,  identifier, text, x, y, z, scaleX, scaleY, scaleZ, 1, listener);
    }

    /**
     * Creates a position editor.
     * @param controller the UI controller responsible for gizmo management
     * @param identifier the UUID identifying the gizmo mesh view display
     * @param text the text representing the position
     * @param x the x world position
     * @param y the y world position
     * @param z the z world position
     * @param scaleX the x scale
     * @param scaleY the y scale
     * @param scaleZ the z scale
     * @param scale the gizmo scale
     * @param listener the listener which handles a new positional update
     */
    public AtomicReference<MeshView> addScaleEditor(MeshViewController<?> controller, UUID identifier, String text, double x, double y, double z, double scaleX, double scaleY, double scaleZ, double scale, IScaleChangeListener listener) {
        TextField[] textFields = new TextField[3];
        double[] scaleCache = new double[]{scaleX, scaleY, scaleZ};

        IScaleChangeListener ourListener = (meshView, oldX, oldY, oldZ, newX, newY, newZ) -> {
            if (oldX != newX) {
                textFields[0].setText(String.valueOf(newX));
                scaleCache[0] = newX;
            }

            if (oldY != newY) {
                textFields[1].setText(String.valueOf(newY));
                scaleCache[1] = newY;
            }

            if (oldZ != newZ) {
                textFields[2].setText(String.valueOf(newZ));
                scaleCache[2] = newZ;
            }

            // Fire listener.
            if (listener != null)
                listener.handle(meshView, oldX, oldY, oldZ, newX, newY, newZ);
        };


        AtomicReference<MeshView> meshViewRef = new AtomicReference<>(controller.getMarkerManager().updateGizmo(identifier, x, y, z, scaleX, scaleY, scaleZ, ourListener));
        if (controller != null) {
            addBoldLabelButton(text + ":", "Toggle Display", 25,
                    () -> meshViewRef.set(controller.getMarkerManager().toggleGizmo(identifier, x, y, z, scaleX, scaleY, scaleZ, scale, ourListener)));
        }

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
        TextField xField = textFields[0] = new TextField(String.valueOf(scaleX));
        TextField yField = textFields[1] = new TextField(String.valueOf(scaleY));
        TextField zField = textFields[2] = new TextField(String.valueOf(scaleZ));
        xField.setPrefWidth(60);
        yField.setPrefWidth(60);
        zField.setPrefWidth(60);
        FXUtils.setHandleKeyPress(xField, str -> {
            if (!NumberUtils.isNumber(str))
                return false;

            double newValue = Double.parseDouble(str);
            if (!Double.isFinite(newValue))
                return false;

            // Update cache.
            double oldX = scaleCache[0];
            scaleCache[0] = newValue;

            // Fire listener, and update gizmo position.
            MeshView meshView = meshViewRef.get();
            if (meshView != null) {
                ((ScaleGizmo) meshView.getMesh()).setScaleX(newValue, true);
            } else {
                listener.handle(null, oldX, scaleCache[1], scaleCache[2], newValue, scaleCache[1], scaleCache[2]);
            }

            return true;
        }, this::onChange);
        FXUtils.setHandleKeyPress(yField, str -> {
            if (!NumberUtils.isNumber(str))
                return false;

            double newValue = Double.parseDouble(str);
            if (!Double.isFinite(newValue))
                return false;

            // Update cache.
            double oldY = scaleCache[1];
            scaleCache[1] = newValue;

            // Fire listener, and update gizmo position.
            MeshView meshView = meshViewRef.get();
            if (meshView != null) {
                ((ScaleGizmo) meshView.getMesh()).setScaleY(newValue, true);
            } else {
                listener.handle(null, scaleCache[0], oldY, scaleCache[2], scaleCache[0], newValue, scaleCache[2]);
            }

            return true;
        }, this::onChange);
        FXUtils.setHandleKeyPress(zField, str -> {
            if (!NumberUtils.isNumber(str))
                return false;

            double newValue = Double.parseDouble(str);
            if (!Double.isFinite(newValue))
                return false;

            // Update cache.
            double oldZ = scaleCache[2];
            scaleCache[2] = newValue;

            // Fire listener, and update gizmo position.
            MeshView meshView = meshViewRef.get();
            if (meshView != null) {
                ((ScaleGizmo) meshView.getMesh()).setScaleZ(newValue, true);
            } else {
                listener.handle(null, scaleCache[0], scaleCache[1], oldZ, scaleCache[0], scaleCache[1], newValue);
            }

            return true;
        }, this::onChange);

        posBox.getChildren().add(xField);
        posBox.getChildren().add(yField);
        posBox.getChildren().add(zField);
        posBox.setSpacing(2);
        vecPane.addColumn(1, posBox);

        vecPane.setHgap(10);
        GridPane.setColumnSpan(vecPane, 2); // Make it take up the full space in the grid it will be added to.
        setupNode(vecPane); // Setup this in the new area.
        addRow(75);

        return meshViewRef;
    }



    /**
     * Add a fixed point short decimal value.
     * @param text The text to add.
     * @param value The short value.
     * @param handler The setter handler.
     */
    public void addFixedShort(String text, short value, Consumer<Short> handler) {
        addFixedShort(text, value, handler, 1 << 4, Short.MIN_VALUE, Short.MAX_VALUE);
    }

    /**
     * Add a fixed point short decimal value.
     * @param text The text to add.
     * @param value The short value.
     * @param handler The setter handler.
     * @param interval The interval it takes for a single full integer to be read.
     */
    public TextField addFixedShort(String text, short value, Consumer<Short> handler, double interval) {
        return addFixedShort(text, value, handler, interval, Short.MIN_VALUE, Short.MAX_VALUE);
    }

    /**
     * Add a fixed point short decimal value.
     * @param text             The text to add.
     * @param value            The short value.
     * @param handler          The setter handler.
     * @param interval         The interval it takes for a single full integer to be read.
     * @param allowNegativeOne If -1 is allowed and valid.
     */
    public void addFixedShort(String text, short value, Consumer<Short> handler, double interval, boolean allowNegativeOne) {
        addFixedShort(text, value, handler, interval, allowNegativeOne ? -1 : 0, Short.MAX_VALUE);
    }

    /**
     * Add a fixed point short decimal value.
     * @param text     The text to add.
     * @param value    The short value.
     * @param handler  The setter handler.
     * @param interval The interval it takes for a single full integer to be read.
     * @param minValue The minimum value (IN INTEGER FORM).
     * @param maxValue The maximum value (IN INTEGER FORM).
     */
    public TextField addFixedShort(String text, short value, Consumer<Short> handler, double interval, int minValue, int maxValue) {
        boolean isNegativeOneSpecial = (minValue == -1 && maxValue > 0);
        String displayStr = (isNegativeOneSpecial && value == -1) ? "-1" : FORMAT.format((double) value / interval);

        return addTextField(text, displayStr, newText -> {
            double parsedValue;

            try {
                parsedValue = Double.parseDouble(newText);
            } catch (NumberFormatException nfe) {
                return false;
            }

            if (!Double.isFinite(parsedValue))
                return false;

            short newShortValue;
            if (isNegativeOneSpecial && parsedValue == -1) {
                newShortValue = (short) -1;
            } else {
                // Convert to short and do bounds check.
                long convertedValue = Math.round(parsedValue * interval);
                if (convertedValue < Short.MIN_VALUE || convertedValue < minValue)
                    return false;
                if (convertedValue > Short.MAX_VALUE || convertedValue > maxValue)
                    return false;

                newShortValue = (short) convertedValue;
            }

            if (handler != null)
                handler.accept(newShortValue);
            onChange();
            return true;
        });
    }

    /**
     * Add a fixed point integer decimal value.
     * @param text    The text to add.
     * @param value   The integer value.
     * @param handler The setter handler.
     */
    public TextField addFixedInt(String text, int value, Consumer<Integer> handler) {
        return addFixedInt(text, value, handler, 1 << 4, Integer.MIN_VALUE, Integer.MAX_VALUE);
    }

    /**
     * Add a fixed point integer decimal value.
     * @param text The text to add.
     * @param value The integer value.
     * @param handler The setter handler.
     * @param interval The interval it takes for a single full integer to be read.
     */
    public TextField addFixedInt(String text, int value, Consumer<Integer> handler, double interval) {
        return addFixedInt(text, value, handler, interval, Integer.MIN_VALUE, Integer.MAX_VALUE);
    }

    /**
     * Add a fixed point integer decimal value.
     * @param text     The text to add.
     * @param value    The integer value.
     * @param handler  The setter handler.
     * @param interval The interval it takes for a single full integer to be read.
     * @param minValue The minimum value (IN INTEGER FORM).
     * @param maxValue The maximum value (IN INTEGER FORM).
     */
    public TextField addFixedInt(String text, int value, Consumer<Integer> handler, double interval, int minValue, int maxValue) {
        boolean isNegativeOneSpecial = (minValue == -1 && maxValue > 0);
        String displayStr = (isNegativeOneSpecial && value == -1) ? "-1" : FORMAT.format((double) value / interval);

        return addTextField(text, displayStr, newText -> {
            double parsedValue;

            try {
                parsedValue = Double.parseDouble(newText);
            } catch (NumberFormatException nfe) {
                return false;
            }

            if (!Double.isFinite(parsedValue))
                return false;

            int newValue;
            if (isNegativeOneSpecial && parsedValue == -1) {
                newValue = -1;
            } else {
                // Convert to short and do bounds check.
                long convertedValue = Math.round(parsedValue * interval);
                if (convertedValue < minValue || convertedValue > maxValue)
                    return false;

                newValue = (int) convertedValue;
            }

            if (handler != null)
                handler.accept(newValue);
            onChange();
            return true;
        });
    }

    /**
     * Add an unsigned fixed point short decimal value.
     * @param text     The text to add.
     * @param value    The short value.
     * @param handler  The setter handler.
     * @param interval The interval it takes for a single full integer to be read.
     */
    public TextField addUnsignedFixedShort(String text, int value, Consumer<Integer> handler, double interval) {
        return addUnsignedFixedShort(text, value, handler, interval, 0x0000, 0xFFFF);
    }

    /**
     * Add an unsigned fixed point short decimal value.
     * @param text     The text to add.
     * @param value    The short value.
     * @param handler  The setter handler.
     * @param interval The interval it takes for a single full integer to be read.
     * @param minValue The minimum value (IN INTEGER FORM).
     * @param maxValue The maximum value (IN INTEGER FORM).
     */
    public TextField addUnsignedFixedShort(String text, int value, Consumer<Integer> handler, double interval, int minValue, int maxValue) {
        boolean isNegativeOneMax = ((minValue == -1 || minValue == 0) && maxValue == 0xFFFF);
        String displayStr = (isNegativeOneMax && value == 0xFFFF) ? "-1" : FORMAT.format((double) value / interval);

        return addTextField(text, displayStr, newText -> {
            double parsedValue;

            try {
                parsedValue = Double.parseDouble(newText);
            } catch (NumberFormatException nfe) {
                return false;
            }

            if (!Double.isFinite(parsedValue))
                return false;

            int newShortValue;
            if (isNegativeOneMax && parsedValue == -1) {
                newShortValue = 0xFFFF;
            } else {
                // Convert to short and do bounds check.
                long convertedValue = Math.round(parsedValue * interval);
                if (convertedValue < 0x0000 || convertedValue < minValue)
                    return false;
                if (convertedValue > 0xFFFF || convertedValue > maxValue)
                    return false;

                newShortValue = (int) convertedValue;
            }

            if (handler != null)
                handler.accept(newShortValue);
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
    public ColorPicker addColorPickerWithAlpha(String text, int color, Consumer<Integer> handler) {
        return addColorPickerWithAlpha(text, 25, color, handler);
    }

    /**
     * Allow selecting a color.
     * @param text    The description of the color.
     * @param color   The starting color.
     * @param handler What to do when a color is selected
     * @return colorPicker
     */
    public ColorPicker addColorPicker(String text, double height, int color, Consumer<Integer> handler) {
        Label fxLabel = addLabel(text);
        ColorPicker picker = setupSecondNode(new ColorPicker(ColorUtils.fromRGB(color)), false);
        picker.valueProperty().addListener((observable, oldValue, newValue) -> {
            handler.accept(ColorUtils.toARGB(newValue));
            onChange();
        });

        addRow(height);
        copyTooltipHack(fxLabel, picker);
        return picker;
    }

    /**
     * Allow selecting a color.
     * @param text    The description of the color.
     * @param color   The starting color.
     * @param handler What to do when a color is selected
     * @return colorPicker
     */
    public ColorPicker addColorPickerWithAlpha(String text, double height, int color, Consumer<Integer> handler) {
        Label fxLabel = addLabel(text);
        AtomicInteger colorArgb = new AtomicInteger(color);
        ColorPicker picker = setupSecondNode(new ColorPicker(ColorUtils.fromRGB(color)), false);
        picker.valueProperty().addListener((observable, oldValue, newValue) -> {
            colorArgb.set((ColorUtils.toARGB(newValue) & 0x00FFFFFF) | (ColorUtils.getAlphaInt(colorArgb.get()) << 24));
            handler.accept(colorArgb.get());
            onChange();
        });
        addRow(height);

        Slider slider = addIntegerSlider("Opacity (Alpha)", ColorUtils.getAlphaInt(color), newAlpha -> {
            colorArgb.set((colorArgb.get() & 0x00FFFFFF) | (newAlpha << 24));
            handler.accept(colorArgb.get());
            onChange();
        }, 0, 255);
        copyTooltipHack(fxLabel, picker);
        copyTooltipHack(slider, picker);
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
                FXUtils.comboBoxScrollToValue(box);
        });

        button.setOnAction(evt -> {
            onClick.accept(box.getValue());
            onChange();
        });
        addRow(25);

        copyTooltipHack(button, box);
        return box;
    }

    /**
     * Add a label and button.
     * @param labelText  The text on the label.
     * @param buttonText The text on the button.
     * @param onPress    What to do when the button is pressed.
     */
    public Button addLabelButton(String labelText, String buttonText, Runnable onPress) {
        return addLabelButton(labelText, buttonText, 25, onPress);
    }

    /**
     * Add a label and button.
     * @param labelText  The text on the label.
     * @param buttonText The text on the button.
     * @param onPress    What to do when the button is pressed.
     */
    public Button addLabelButton(String labelText, String buttonText, double height, Runnable onPress) {
        Label fxLabel = addLabel(labelText);
        Button button = setupSecondNode(new Button(buttonText), false);
        button.setOnAction(evt -> onPress.run());
        addRow(height);
        copyTooltipHack(fxLabel, button);
        return button;
    }

    /**
     * Add a bold label and button.
     * @param labelText  The text on the label.
     * @param buttonText The text on the button.
     * @param onPress    What to do when the button is pressed.
     */
    public Button addBoldLabelButton(String labelText, String buttonText, Runnable onPress) {
        return addBoldLabelButton(labelText, buttonText, 25, onPress);
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
        copyTooltipHack(bold, button);
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
        view.setFitWidth(dimensions);
        view.setFitHeight(dimensions);
        return addCenteredImageView(view);
    }

    /**
     * Adds a centered image.
     * @param image The image to add.
     * @return imageView
     */
    public ImageView addCenteredImage(Image image) {
        ImageView view = new ImageView(image);
        view.setFitWidth(image.getWidth());
        view.setFitHeight(image.getHeight());
        return addCenteredImageView(view);
    }

    /**
     * Adds a centered ImageView.
     * @param imageView the ImageView to add
     * @return imageView
     */
    public ImageView addCenteredImageView(ImageView imageView) {
        if (imageView.getFitWidth() < 1 && imageView.getImage() != null)
            imageView.setFitWidth(imageView.getImage().getWidth());
        if (imageView.getFitHeight() < 1 && imageView.getImage() != null)
            imageView.setFitHeight(imageView.getImage().getHeight());

        GridPane.setHalignment(imageView, HPos.CENTER);
        setupSecondNode(imageView, true);
        addRow(imageView.getFitHeight() + 5);
        return imageView;
    }

    /**
     * Add a PSXMatrix to the editor grid.
     * @param matrix           The rotation matrix to add data for.
     * @param onPositionUpdate Behavior to apply when the position is updated.
     */
    public void addMeshMatrix(PSXMatrix matrix, MeshViewController<?> controller, Runnable onPositionUpdate) {
        addMeshMatrix(matrix, controller, onPositionUpdate, false, null);
    }

    /**
     * Add a PSXMatrix to the editor grid.
     * @param matrix           The rotation matrix to add data for.
     * @param onPositionUpdate Behavior to apply when the position is updated.
     */
    public void addMeshMatrix(PSXMatrix matrix, MeshViewController<?> controller, Runnable onPositionUpdate, boolean rotationUpdates, BiConsumer<Vector, Integer> positionSelector) {
        IVector vec = new IVector(matrix.getTransform()[0], matrix.getTransform()[1], matrix.getTransform()[2]);

        addFloatVector("Position", vec, () -> {
            matrix.getTransform()[0] = vec.getX();
            matrix.getTransform()[1] = vec.getY();
            matrix.getTransform()[2] = vec.getZ();
            if (onPositionUpdate != null)
                onPositionUpdate.run(); // Run position hook.
        }, controller, 4, null, null, positionSelector);

        addRotationMatrix(matrix, rotationUpdates ? onPositionUpdate : null);
    }

    private static final DecimalFormat ANGLE_DISPLAY_FORMAT = new DecimalFormat("0.###");

    /**
     * Add PSXMatrix rotation data to the edit grid.
     * @param matrix   The rotation matrix to add data for.
     * @param onUpdate Behavior to apply when the rotation is updated.
     */
    public void addRotationMatrix(PSXMatrix matrix, Runnable onUpdate) {
        addNormalLabel("Rotation:");

        Runnable[] updateHook = new Runnable[1];

        AtomicReference<Label> pitchLabel = new AtomicReference<>();
        Slider pitchUI = addDoubleSlider("Pitch (X: " + ANGLE_DISPLAY_FORMAT.format(Math.toDegrees(matrix.getPitchAngle())) + ")", matrix.getPitchAngle(), pitch -> {
            matrix.updateMatrix(pitch, matrix.getYawAngle(), matrix.getRollAngle());
            if (updateHook[0] != null)
                updateHook[0].run();
        }, -Math.PI, Math.PI, false, pitchLabel);

        AtomicReference<Label> yawLabel = new AtomicReference<>();
        Slider yawUI = addDoubleSlider("Yaw (Y: " + ANGLE_DISPLAY_FORMAT.format(Math.toDegrees(matrix.getYawAngle())) + ")", matrix.getYawAngle(), yaw -> {
            matrix.updateMatrix(matrix.getPitchAngle(), yaw, matrix.getRollAngle());
            if (updateHook[0] != null)
                updateHook[0].run();
        }, -Math.PI / 2, Math.PI / 2, false, yawLabel); // Cuts off at 90 degrees to prevent gymbal lock.

        AtomicReference<Label> rollLabel = new AtomicReference<>();
        Slider rollUI = addDoubleSlider("Roll (Z: " + ANGLE_DISPLAY_FORMAT.format(Math.toDegrees(matrix.getRollAngle())) + ")", matrix.getRollAngle(), roll -> {
            matrix.updateMatrix(matrix.getPitchAngle(), matrix.getYawAngle(), roll);
            if (updateHook[0] != null)
                updateHook[0].run();
        }, -Math.PI, Math.PI, false, rollLabel);
        
        // Label click handlers.
        pitchLabel.get().setOnMouseClicked(event -> {
            event.consume();
            InputMenu.promptInput(null, "Please enter the new pitch angle.", String.valueOf(Math.toDegrees(matrix.getPitchAngle())), newText -> {
                float newPitch;
                try {
                    newPitch = (float) Math.toRadians(Float.parseFloat(newText));
                } catch (NumberFormatException ex) {
                    FXUtils.showPopup(AlertType.ERROR, "Non-number was given.", "Cannot interpret '" + newText + "' as a number.");
                    return;
                }

                matrix.updateMatrix(newPitch, matrix.getYawAngle(), matrix.getRollAngle());
                if (updateHook[0] != null)
                    updateHook[0].run();
            });
        });

        yawLabel.get().setOnMouseClicked(event -> {
            event.consume();
            InputMenu.promptInput(null, "Please enter the new yaw angle.", String.valueOf(Math.toDegrees(matrix.getYawAngle())), newText -> {
                float newYaw;
                try {
                    newYaw = (float) Math.toRadians(Float.parseFloat(newText));
                } catch (NumberFormatException ex) {
                    FXUtils.showPopup(AlertType.ERROR, "Non-number was given.", "Cannot interpret '" + newText + "' as a number.");
                    return;
                }

                matrix.updateMatrix(matrix.getPitchAngle(), newYaw, matrix.getRollAngle());
                if (updateHook[0] != null)
                    updateHook[0].run();
            });
        });

        rollLabel.get().setOnMouseClicked(event -> {
            event.consume();
            InputMenu.promptInput(null, "Please enter the new roll angle.", String.valueOf(Math.toDegrees(matrix.getRollAngle())), newText -> {
                float newRoll;
                try {
                    newRoll = (float) Math.toRadians(Float.parseFloat(newText));
                } catch (NumberFormatException ex) {
                    FXUtils.showPopup(AlertType.ERROR, "Non-number was given.", "Cannot interpret '" + newText + "' as a number.");
                    return;
                }

                matrix.updateMatrix(matrix.getPitchAngle(), matrix.getYawAngle(), newRoll);
                if (updateHook[0] != null)
                    updateHook[0].run();
            });
        });

        // Update hook.
        updateHook[0] = () -> {
            if (onUpdate != null)
                onUpdate.run();

            // Setting yaw to near -90/+90 can reset the other angles. It's confusing, but having the sliders update makes it seem better.
            // We try to avoid letting this reset occur, so ideally this won't matter.
            pitchUI.setValue(matrix.getPitchAngle());
            yawUI.setValue(matrix.getYawAngle());
            rollUI.setValue(matrix.getRollAngle());
            pitchLabel.get().setText("Pitch (X: " + ANGLE_DISPLAY_FORMAT.format(Math.toDegrees(matrix.getPitchAngle())) + ")");
            yawLabel.get().setText("Yaw (Y: " + ANGLE_DISPLAY_FORMAT.format(Math.toDegrees(matrix.getYawAngle())) + ")");
            rollLabel.get().setText("Roll (Z: " + ANGLE_DISPLAY_FORMAT.format(Math.toDegrees(matrix.getRollAngle())) + ")");
        };

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
     */
    public void addRow() {
        addRow(25);
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
    public void addSeparator() {
        addSeparator(25);
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
        Label fxLabel = addLabel(sliderName);
        Slider slider = setupSecondNode(new Slider(minValue, maxValue, currentValue), false);
        slider.setDisable(setter == null);
        slider.valueProperty().addListener(((observable, oldValue, newValue) -> {
            if (setter != null)
                setter.accept(newValue.intValue());
            onChange();
        }));
        if (maxValue - minValue > 0)
            slider.setMajorTickUnit((double) (maxValue - minValue) / 4D);
        slider.setShowTickLabels(true);
        slider.setShowTickMarks(true);
        slider.setMinorTickCount(0);
        slider.setBlockIncrement(1);
        addRow(40);
        copyTooltipHack(fxLabel, slider);
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
        return addDoubleSlider(sliderName, currentValue, setter, minValue, maxValue, false, null);
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
    public Slider addDoubleSlider(String sliderName, double currentValue, Consumer<Double> setter, double minValue, double maxValue, boolean onRelease, AtomicReference<Label> labelRef) {
        Label label = addLabel(sliderName);
        if (labelRef != null)
            labelRef.set(label);

        Slider slider = setupSecondNode(new Slider(minValue, maxValue, currentValue), false);
        slider.setDisable(setter == null);
        if (setter != null) {
            if (onRelease) {
                slider.valueChangingProperty().addListener(((observable, wasChanging, changing) -> {
                    if (!changing)
                        setter.accept(slider.getValue());
                }));
            }
        }

        slider.valueProperty().addListener(((observable, oldValue, newValue) -> {
            if (setter != null && !onRelease)
                setter.accept(newValue.doubleValue());
            onChange();
        }));

        if (maxValue > minValue)
            slider.setMajorTickUnit((maxValue - minValue) / 4);
        slider.setShowTickLabels(true);
        slider.setShowTickMarks(true);
        slider.setMinorTickCount(0);
        slider.setBlockIncrement(1);
        addRow(40);
        copyTooltipHack(label, slider);
        return slider;
    }

    /**
     * Creates a GridPane usable in this grid.
     * @return gridPane
     */
    public static GridPane createDefaultPane() {
        GridPane newPane = new GridPane();
        newPane.setPrefWidth(250);
        newPane.getColumnConstraints().add(new ColumnConstraints(10, 130, Region.USE_PREF_SIZE, Priority.SOMETIMES, null, true));
        newPane.getColumnConstraints().add(new ColumnConstraints(10, 120, Region.USE_PREF_SIZE, Priority.SOMETIMES, null, false));
        return newPane;
    }

    // This is a major hack that we're going to use until we have something better.
    // It manages to copy the tooltip to the label corresponding to the text field.
    private void copyTooltipHack(Control control1, Control control2) {
        if (control1 == null || control2 == null)
            return;

        Platform.runLater(() -> {
            if (control1.getTooltip() != null && control2.getTooltip() == null) {
                control2.setTooltip(control1.getTooltip());
            } else if (control1.getTooltip() == null && control2.getTooltip() != null) {
                control1.setTooltip(control2.getTooltip());
            }
        });
    }
}