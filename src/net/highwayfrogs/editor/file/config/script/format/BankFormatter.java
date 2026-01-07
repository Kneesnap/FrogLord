package net.highwayfrogs.editor.file.config.script.format;

import javafx.collections.FXCollections;
import javafx.scene.Node;
import javafx.scene.control.ComboBox;
import lombok.AllArgsConstructor;
import net.highwayfrogs.editor.file.config.script.ScriptCommand;
import net.highwayfrogs.editor.file.config.script.ScriptParseException;
import net.highwayfrogs.editor.games.sony.frogger.FroggerConfig;
import net.highwayfrogs.editor.games.sony.frogger.FroggerGameInstance;
import net.highwayfrogs.editor.games.sony.frogger.ui.ScriptEditorController;
import net.highwayfrogs.editor.games.sony.shared.utils.SCNameBank;
import net.highwayfrogs.editor.system.AbstractStringConverter;
import net.highwayfrogs.editor.utils.NumberUtils;
import net.highwayfrogs.editor.utils.Utils;

import java.util.function.Function;

/**
 * Formats bank names.
 * Created by Kneesnap on 8/1/2019.
 */
@AllArgsConstructor
public class BankFormatter extends ScriptFormatter {
    private final Function<FroggerConfig, SCNameBank> getter;
    public static final BankFormatter SCRIPT_INSTANCE = new BankFormatter(FroggerConfig::getScriptBank);
    public static final BankFormatter SCRIPT_CALLBACK_INSTANCE = new BankFormatter(FroggerConfig::getScriptCallbackBank);

    @Override
    public String numberToString(FroggerGameInstance instance, int number) {
        SCNameBank bank = getBank(instance);
        return bank != null && bank.hasName(number) ? bank.getName(number) : super.numberToString(instance, number);
    }

    @Override
    public int stringToNumber(FroggerGameInstance instance, String str) {
        if (NumberUtils.isInteger(str))
            return super.stringToNumber(instance, str);

        SCNameBank bank = getBank(instance);
        int index = bank != null ? bank.getNames().indexOf(str) : -1;
        if (index == -1)
            throw new ScriptParseException("Could not find bank entry named '" + str + "'.");
        return index;
    }

    @Override
    public Node makeEditor(FroggerGameInstance instance, ScriptEditorController controller, ScriptCommand command, int index) {
        ComboBox<Integer> comboBox = new ComboBox<>();
        comboBox.setConverter(new AbstractStringConverter<>(getBank(instance)::getName));
        comboBox.setItems(FXCollections.observableArrayList(Utils.getIntegerList(getBank(instance).size())));
        comboBox.setValue(command.getArguments()[index]);
        comboBox.getSelectionModel().select(command.getArguments()[index]);
        comboBox.valueProperty().addListener(((observable, oldValue, newValue) -> {
            command.getArguments()[index] = newValue;
            controller.updateCodeDisplay();
        }));

        return comboBox;
    }

    private SCNameBank getBank(FroggerGameInstance instance) {
        return getter.apply(instance.getVersionConfig());
    }
}