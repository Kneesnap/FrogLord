package net.highwayfrogs.editor.games.sony.frogger.data.scripts.format;

import javafx.collections.FXCollections;
import javafx.scene.Node;
import javafx.scene.control.ComboBox;
import net.highwayfrogs.editor.games.sony.frogger.FroggerGameInstance;
import net.highwayfrogs.editor.games.sony.frogger.data.scripts.FroggerScriptCommand;
import net.highwayfrogs.editor.games.sony.frogger.data.scripts.FroggerScriptParseException;
import net.highwayfrogs.editor.games.sony.frogger.ui.ScriptEditorController;
import net.highwayfrogs.editor.games.sony.shared.utils.SCNameBank;
import net.highwayfrogs.editor.system.AbstractStringConverter;
import net.highwayfrogs.editor.utils.NumberUtils;
import net.highwayfrogs.editor.utils.Utils;

/**
 * Formats sound names for use in the Frogger entity scripting system.
 * Created by Kneesnap on 2/7/2023.
 */
public class FroggerScriptSoundFormatter extends FroggerScriptFormatter {
    public static final FroggerScriptSoundFormatter INSTANCE = new FroggerScriptSoundFormatter();

    private FroggerScriptSoundFormatter() {
    }

    @Override
    public String numberToString(FroggerGameInstance instance, int number) {
        SCNameBank bank = getBank(instance);
        if (bank == null)
            return super.numberToString(instance, number);

        number -= getNameIndexOffset(instance, bank, number);
        return bank.hasName(number) ? bank.getName(number) : super.numberToString(instance, number);
    }

    @Override
    public int stringToNumber(FroggerGameInstance instance, String str) {
        if (NumberUtils.isInteger(str))
            return super.stringToNumber(instance, str);

        SCNameBank bank = getBank(instance);
        int index = bank != null ? bank.getNames().indexOf(str) : -1;
        if (index == -1)
            throw new FroggerScriptParseException("Could not find sound named '" + str + "'.");

        index += getNameIndexOffset(instance, bank, index);
        return index;
    }

    private int getNameIndexOffset(FroggerGameInstance instance, SCNameBank bank, int sfxId) {
        // The PSX version has a few extra entries in the sound table which re-use existing waves in the .VB/.VH files.
        // These impact the IDs of names which come later because they offset the SFX IDs as they are used by scripts.
        // Sound banks however, do not have this issue.
        if (instance.isPSX()) {
            // TODO: Builds PSX Build 49 and earlier seem to have some offsets I've not accounted for.
            //  - To have multiple offsets, sum multiple function calls together.
            return getBankOffset(bank, sfxId, "GENERIC", 5);
        }

        // PC Builds validated to use offset of zero:
        //  - Build 4 (September 3, 1997)
        //  - Retail PC

        return 0;
    }

    private int getBankOffset(SCNameBank bank, int sfxId, String bankName, int offsetAmount) {
        // Resolve PSX offset.
        SCNameBank childBank = bank.getChildBank(bankName);
        if (childBank == null)
            throw new FroggerScriptParseException("The SFX ID (" + sfxId + ") was skipped for scripting because the sound bank name '" + bankName + "' could not be resolved.");

        if (sfxId >= childBank.size()) {
            if (sfxId >= childBank.size() + offsetAmount)
                return offsetAmount;

            // The SFX ID appears to correspond to the specific part of the sound bank which is skipped! Uh oh!
            throw new FroggerScriptParseException("Cannot obtain name for SFX ID (" + sfxId + "), because this portion of the SFX namespace is (intentionally) not configured!!");
        }

        return 0;
    }

    @Override
    public Node makeEditor(FroggerGameInstance instance, ScriptEditorController controller, FroggerScriptCommand command, int index) {
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