package net.highwayfrogs.editor.file.patch.argtypes;

import javafx.scene.Node;
import javafx.scene.control.CheckBox;
import net.highwayfrogs.editor.file.patch.PatchArgument;
import net.highwayfrogs.editor.file.patch.PatchValue;
import net.highwayfrogs.editor.games.sony.frogger.ui.PatchController;

/**
 * Created by Kneesnap on 2/15/2020.
 */
public class BooleanArgument extends PatchArgumentBehavior<Boolean> {
    @Override
    public Boolean parseString(String text) {
        return text.equalsIgnoreCase("true");
    }

    @Override
    public boolean isValidString(String text) {
        return text.equalsIgnoreCase("true") || text.equalsIgnoreCase("false");
    }

    @Override
    public String valueToString(Boolean object) {
        return object ? "true" : "false";
    }

    @Override
    public boolean isCorrectType(Object obj) {
        return obj instanceof Boolean;
    }

    @Override
    protected boolean isValidValueInternal(Boolean value, PatchArgument argument) {
        return true;
    }

    @Override
    public boolean isTrueValue(PatchValue value) {
        return value.getAsBoolean();
    }

    @Override
    public Node createEditor(PatchController controller, PatchArgument argument, PatchValue variable) {
        CheckBox checkBox = new CheckBox();
        checkBox.setSelected(variable.getAsBoolean());
        checkBox.selectedProperty().addListener(((observable, oldValue, newValue) -> {
            variable.setBoolean(newValue);
            controller.updatePatchDisplay();
        }));
        return checkBox;
    }
}