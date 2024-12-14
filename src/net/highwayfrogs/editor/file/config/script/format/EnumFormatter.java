package net.highwayfrogs.editor.file.config.script.format;

import javafx.collections.FXCollections;
import javafx.scene.Node;
import javafx.scene.control.ComboBox;
import net.highwayfrogs.editor.file.config.script.ScriptCommand;
import net.highwayfrogs.editor.file.config.script.ScriptParseException;
import net.highwayfrogs.editor.file.config.script.constants.*;
import net.highwayfrogs.editor.games.sony.frogger.FroggerGameInstance;
import net.highwayfrogs.editor.games.sony.frogger.ui.ScriptEditorController;
import net.highwayfrogs.editor.system.AbstractStringConverter;
import net.highwayfrogs.editor.utils.NumberUtils;

/**
 * Formats enums.
 * Created by Kneesnap on 8/1/2019.
 */
public class EnumFormatter<E extends Enum<E>> extends ScriptFormatter {
    private final E[] enumContents;

    public static final EnumFormatter<ScriptConstantRegister> FORMAT_REGISTER_IDS = new EnumFormatter<>(ScriptConstantRegister.values());
    public static final EnumFormatter<ScriptConstantRegisterToggle> FORMAT_REGISTER_TOGGLE = new EnumFormatter<>(ScriptConstantRegisterToggle.values());
    public static final EnumFormatter<ScriptConstantCallback> FORMAT_CALLBACK = new EnumFormatter<>(ScriptConstantCallback.values());
    public static final EnumFormatter<ConstantScriptOption> FORMAT_SCRIPT_OPTION = new EnumFormatter<>(ConstantScriptOption.values());
    public static final EnumFormatter<ScriptConditionType> FORMAT_CONDITIONS = new EnumFormatter<>(ScriptConditionType.values());
    public static final EnumFormatter<ScriptConstantEntityType> FORMAT_ENTITY_TYPE = new EnumFormatter<>(ScriptConstantEntityType.values());
    public static final EnumFormatter<ScriptConstantCallBackId> FORMAT_CALLBACK_IDS = new EnumFormatter<>(ScriptConstantCallBackId.values());
    public static final EnumFormatter<ScriptConstantDirection> FORMAT_DIRECTION = new EnumFormatter<>(ScriptConstantDirection.values());

    public EnumFormatter(E[] enumContents) {
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
        throw new ScriptParseException("Unknown value '" + str + "'.");
    }

    @Override
    public Node makeEditor(FroggerGameInstance instance, ScriptEditorController controller, ScriptCommand command, int index) {
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