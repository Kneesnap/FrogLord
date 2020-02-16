package net.highwayfrogs.editor.file.patch.argtypes;

import javafx.scene.Node;
import javafx.scene.control.ColorPicker;
import javafx.scene.paint.Color;
import net.highwayfrogs.editor.file.patch.PatchArgument;
import net.highwayfrogs.editor.file.patch.PatchValue;
import net.highwayfrogs.editor.gui.editor.PatchController;
import net.highwayfrogs.editor.utils.Utils;

/**
 * Allows editing colors.
 * Created by Kneesnap on 2/15/2020.
 */
public class ColorArgument extends PatchArgumentBehavior<Color> {
    @Override
    public Color parseString(String text) {
        int rgbColor = text.startsWith("0x") ? Integer.parseInt(text.substring(2), 16) : Integer.parseInt(text);
        return Utils.fromRGB(rgbColor);
    }

    @Override
    public boolean isValidString(String text) {
        return Utils.isInteger(text) || Utils.isHexInteger(text);
    }

    @Override
    public String valueToString(Color color) {
        return Utils.toHexString(Utils.toRGB(color));
    }

    @Override
    public boolean isCorrectType(Object obj) {
        return obj instanceof Color;
    }

    @Override
    protected boolean isValidValueInternal(Color value, PatchArgument argument) {
        return value != null;
    }

    @Override
    public boolean isTrueValue(PatchValue value) {
        return value != null;
    }

    @Override
    public Node createEditor(PatchController controller, PatchArgument argument, PatchValue variable) {
        ColorPicker colorPicker = new ColorPicker(variable.getAsColor());
        colorPicker.valueProperty().addListener((observable, oldValue, newValue) -> {
            variable.setColor(newValue);
            controller.updatePatchDisplay();
        });
        return colorPicker;
    }
}
