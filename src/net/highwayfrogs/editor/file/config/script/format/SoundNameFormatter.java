package net.highwayfrogs.editor.file.config.script.format;

import javafx.collections.FXCollections;
import javafx.scene.Node;
import javafx.scene.control.ComboBox;
import net.highwayfrogs.editor.file.config.script.ScriptCommand;
import net.highwayfrogs.editor.file.config.script.ScriptParseException;
import net.highwayfrogs.editor.games.sony.frogger.FroggerGameInstance;
import net.highwayfrogs.editor.games.sony.frogger.ui.ScriptEditorController;
import net.highwayfrogs.editor.games.sony.shared.utils.SCNameBank;
import net.highwayfrogs.editor.system.AbstractStringConverter;
import net.highwayfrogs.editor.utils.NumberUtils;
import net.highwayfrogs.editor.utils.Utils;

/**
 * Created by Kneesnap on 2/7/2023.
 */
public class SoundNameFormatter extends ScriptFormatter {
    public static final SoundNameFormatter INSTANCE = new SoundNameFormatter();

    private SoundNameFormatter() {
    }

    @Override
    public String numberToString(FroggerGameInstance instance, int number) {
        SCNameBank bank = getBank(instance);
        if (bank == null)
            return super.numberToString(instance, number);

        if (instance.isPSX() && instance.getVersionConfig().getBuild() == 71) { // PSX builds do lookup differently.
            SCNameBank childBank = bank.getChildBank("GENERIC");
            if (childBank != null && number >= childBank.size() + 5)
                number -= 5; // The PSX version has a few duplicate entries which are here to
            // TODO: In the future, we should have a separate configuration for this (Or improve the existing config file to allow entries with this kind of info), and read the sound table from ingame, so we have the actual sample rates.
        }

        return bank.hasName(number) ? bank.getName(number) : super.numberToString(instance, number);
    }

    @Override
    public int stringToNumber(FroggerGameInstance instance, String str) {
        if (NumberUtils.isInteger(str))
            return super.stringToNumber(instance, str);

        SCNameBank bank = getBank(instance);
        int index = bank != null ? bank.getNames().indexOf(str) : -1;
        if (index == -1)
            throw new ScriptParseException("Could not find sound named '" + str + "'.");

        if (instance.isPSX() && instance.getVersionConfig().getBuild() == 71) { // PSX builds do lookup differently.
            SCNameBank childBank = bank.getChildBank("GENERIC");
            if (childBank != null && index >= childBank.size() + 5)
                index += 5; // The PSX version has a few duplicate entries which are here to
            // TODO: In the future, we should have a separate configuration for this (Or improve the existing config file to allow entries with this kind of info), and read the sound table from ingame, so we have the actual sample rates.
        }


        return index;
    }

    @Override
    public Node makeEditor(FroggerGameInstance instance, ScriptEditorController controller, ScriptCommand command, int index) {
        ComboBox<Integer> comboBox = new ComboBox<>();
        comboBox.setConverter(new AbstractStringConverter<>(num -> this.numberToString(instance, num)));
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
        return instance.getVersionConfig().getSoundBank();
    }
}