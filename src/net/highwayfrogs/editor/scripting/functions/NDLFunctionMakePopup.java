package net.highwayfrogs.editor.scripting.functions;

import javafx.scene.control.Alert.AlertType;
import net.highwayfrogs.editor.scripting.NoodleScript;
import net.highwayfrogs.editor.scripting.runtime.NoodlePrimitive;
import net.highwayfrogs.editor.scripting.runtime.NoodleThread;
import net.highwayfrogs.editor.utils.FXUtils;

/**
 * Creates and displays popup window.
 * Created by Kneesnap on 10/23/2024.
 */
public class NDLFunctionMakePopup extends NoodleFunction {
    public static final NDLFunctionMakePopup INSTANCE = new NDLFunctionMakePopup();

    public NDLFunctionMakePopup() {
        super("makePopup", "<message> [alertType=INFORMATION]");
    }

    @Override
    public NoodlePrimitive execute(NoodleThread<? extends NoodleScript> thread, NoodlePrimitive[] args) {
        String message = args[0].getAsString();
        AlertType alertType = args.length > 1 ? args[1].getStringValueAsEnum(AlertType.class) : AlertType.INFORMATION;
        FXUtils.makePopUp(message, alertType);
        return null;
    }
}
