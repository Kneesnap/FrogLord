package net.highwayfrogs.editor.games.sony.frogger.data.scripts.format;

import javafx.collections.FXCollections;
import javafx.scene.Node;
import javafx.scene.control.ComboBox;
import net.highwayfrogs.editor.games.sony.frogger.FroggerGameInstance;
import net.highwayfrogs.editor.games.sony.frogger.data.scripts.FroggerScriptCommand;
import net.highwayfrogs.editor.games.sony.frogger.data.scripts.FroggerScriptParseException;
import net.highwayfrogs.editor.games.sony.frogger.data.scripts.constants.*;
import net.highwayfrogs.editor.games.sony.frogger.ui.ScriptEditorController;
import net.highwayfrogs.editor.system.AbstractStringConverter;
import net.highwayfrogs.editor.utils.NumberUtils;

/**
 * Formats enum names for use in the Frogger entity scripting system.
 * Created by Kneesnap on 8/1/2019.
 */
public class FroggerScriptEnumFormatter<E extends Enum<E>> extends FroggerScriptFormatter {
    private final E[] enumContents;

    public static final FroggerScriptEnumFormatter<FroggerScriptRegister> FORMAT_REGISTER_IDS = new FroggerScriptEnumFormatter<>(FroggerScriptRegister.values());
    public static final FroggerScriptEnumFormatter<FroggerScriptBoolEnableRegisters> FORMAT_REGISTER_TOGGLE = new FroggerScriptEnumFormatter<>(FroggerScriptBoolEnableRegisters.values());
    public static final FroggerScriptEnumFormatter<FroggerScriptCallbackType> FORMAT_CALLBACK = new FroggerScriptEnumFormatter<>(FroggerScriptCallbackType.values());
    public static final FroggerScriptEnumFormatter<FroggerScriptBranchType> FORMAT_SCRIPT_OPTION = new FroggerScriptEnumFormatter<>(FroggerScriptBranchType.values());
    public static final FroggerScriptEnumFormatter<FroggerScriptConditionType> FORMAT_CONDITIONS = new FroggerScriptEnumFormatter<>(FroggerScriptConditionType.values());
    public static final FroggerScriptEnumFormatter<FroggerScriptEntityType> FORMAT_ENTITY_TYPE = new FroggerScriptEnumFormatter<>(FroggerScriptEntityType.values());
    public static final FroggerScriptEnumFormatter<FroggerScriptCallback> FORMAT_CALLBACK_IDS = new FroggerScriptEnumFormatter<>(FroggerScriptCallback.values());
    public static final FroggerScriptEnumFormatter<FroggerScriptAxisDirection> FORMAT_DIRECTION = new FroggerScriptEnumFormatter<>(FroggerScriptAxisDirection.values());

    public FroggerScriptEnumFormatter(E[] enumContents) {
        this.enumContents = enumContents;
    }

    @Override
    public String numberToString(FroggerGameInstance instance, int number) {
        return (number >= 0 && number < enumContents.length) ? enumContents[number].name() : super.numberToString(instance, number);
    }

    @Override
    public int stringToNumber(FroggerGameInstance instance, String str) {
        if (NumberUtils.isInteger(str))
            return super.stringToNumber(instance, str);

        for (E value : enumContents)
            if (value.name().equalsIgnoreCase(str))
                return value.ordinal();
        throw new FroggerScriptParseException("Unknown value '" + str + "'.");
    }

    @Override
    public Node makeEditor(FroggerGameInstance instance, ScriptEditorController controller, FroggerScriptCommand command, int index) {
        int value = command.getArguments()[index];
        if (value < 0 || value >= enumContents.length)
            return super.makeEditor(instance, controller, command, index);

        ComboBox<E> comboBox = new ComboBox<>();
        comboBox.setConverter(new AbstractStringConverter<>(E::name));
        comboBox.setItems(FXCollections.observableArrayList(enumContents));
        comboBox.setValue(enumContents[value]);
        comboBox.getSelectionModel().select(enumContents[value]);
        comboBox.valueProperty().addListener(((observable, oldValue, newValue) -> {
            command.getArguments()[index] = newValue.ordinal();
            controller.updateCodeDisplay();
        }));

        return comboBox;
    }
}