package net.highwayfrogs.editor.file.config.script.format;

import javafx.collections.FXCollections;
import javafx.scene.Node;
import javafx.scene.control.ComboBox;
import lombok.AllArgsConstructor;
import net.highwayfrogs.editor.file.config.FroggerEXEInfo;
import net.highwayfrogs.editor.file.config.NameBank;
import net.highwayfrogs.editor.file.config.script.ScriptCommand;
import net.highwayfrogs.editor.file.config.script.ScriptParseException;
import net.highwayfrogs.editor.gui.GUIMain;
import net.highwayfrogs.editor.gui.editor.ScriptEditorController;
import net.highwayfrogs.editor.system.AbstractStringConverter;
import net.highwayfrogs.editor.utils.Utils;

import java.util.function.Function;

/**
 * Formats bank names.
 * Created by Kneesnap on 8/1/2019.
 */
@AllArgsConstructor
public class BankFormatter extends ScriptFormatter {
    private Function<FroggerEXEInfo, NameBank> getter;
    public static final BankFormatter SOUND_INSTANCE = new BankFormatter(FroggerEXEInfo::getSoundBank);
    public static final BankFormatter SCRIPT_INSTANCE = new BankFormatter(FroggerEXEInfo::getScriptBank);
    public static final BankFormatter SCRIPT_CALLBACK_INSTANCE = new BankFormatter(FroggerEXEInfo::getScriptCallbackBank);

    @Override
    public String numberToString(int number) {
        NameBank bank = getBank();
        return bank != null && bank.hasName(number) ? bank.getName(number) : super.numberToString(number);
    }

    @Override
    public int stringToNumber(String str) {
        if (Utils.isInteger(str))
            return super.stringToNumber(str);

        NameBank bank = getBank();
        int index = bank != null ? bank.getNames().indexOf(str) : -1;
        if (index == -1)
            throw new ScriptParseException("Could not find sound named '" + str + "'.");
        return index;
    }

    /**
     * Creates an editor node for this formatter.
     * @param command The command.
     * @param index   The argument index.
     * @return editorNode
     */
    public Node makeEditor(ScriptEditorController controller, ScriptCommand command, int index) {
        ComboBox<Integer> comboBox = new ComboBox<>();
        comboBox.setConverter(new AbstractStringConverter<>(getBank()::getName));
        comboBox.setItems(FXCollections.observableArrayList(Utils.getIntegerList(getBank().size())));
        comboBox.setValue(command.getArguments()[index]);
        comboBox.getSelectionModel().select(command.getArguments()[index]);
        comboBox.valueProperty().addListener(((observable, oldValue, newValue) -> {
            command.getArguments()[index] = newValue;
            controller.updateCodeDisplay();
        }));

        return comboBox;
    }

    private NameBank getBank() {
        return getter.apply(GUIMain.EXE_CONFIG);
    }
}
