package net.highwayfrogs.editor.file.config.script.format;

import javafx.collections.FXCollections;
import javafx.scene.Node;
import javafx.scene.control.ComboBox;
import net.highwayfrogs.editor.file.config.NameBank;
import net.highwayfrogs.editor.file.config.script.ScriptCommand;
import net.highwayfrogs.editor.file.config.script.ScriptParseException;
import net.highwayfrogs.editor.gui.GUIMain;
import net.highwayfrogs.editor.gui.editor.ScriptEditorController;
import net.highwayfrogs.editor.system.AbstractStringConverter;
import net.highwayfrogs.editor.utils.Utils;

/**
 * Created by Kneesnap on 2/7/2023.
 */
public class SoundNameFormatter extends ScriptFormatter {
    public static final SoundNameFormatter INSTANCE = new SoundNameFormatter();

    private SoundNameFormatter() {
    }

    @Override
    public String numberToString(int number) {
        NameBank bank = getBank();
        if (bank == null)
            return super.numberToString(number);

        if (GUIMain.EXE_CONFIG.isPSX() && GUIMain.EXE_CONFIG.getBuild() == 71) { // PSX builds do lookup differently.
            NameBank childBank = bank.getChildBank("GENERIC");
            if (childBank != null && number >= childBank.size() + 5)
                number -= 5; // The PSX version has a few duplicate entries which are here to
            // TODO: In the future, we should have a separate configuration for this (Or improve the existing config file to allow entries with this kind of info), and read the sound table from ingame, so we have the actual sample rates.
        }

        return bank.hasName(number) ? bank.getName(number) : super.numberToString(number);
    }

    @Override
    public int stringToNumber(String str) {
        if (Utils.isInteger(str))
            return super.stringToNumber(str);

        NameBank bank = getBank();
        int index = bank != null ? bank.getNames().indexOf(str) : -1;
        if (index == -1)
            throw new ScriptParseException("Could not find sound named '" + str + "'.");

        if (GUIMain.EXE_CONFIG.isPSX() && GUIMain.EXE_CONFIG.getBuild() == 71) { // PSX builds do lookup differently.
            NameBank childBank = bank.getChildBank("GENERIC");
            if (childBank != null && index >= childBank.size() + 5)
                index += 5; // The PSX version has a few duplicate entries which are here to
            // TODO: In the future, we should have a separate configuration for this (Or improve the existing config file to allow entries with this kind of info), and read the sound table from ingame, so we have the actual sample rates.
        }


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
        comboBox.setConverter(new AbstractStringConverter<>(this::numberToString));
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
        return GUIMain.EXE_CONFIG.getSoundBank();
    }
}