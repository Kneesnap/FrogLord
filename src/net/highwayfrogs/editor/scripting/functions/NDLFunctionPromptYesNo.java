package net.highwayfrogs.editor.scripting.functions;

import net.highwayfrogs.editor.scripting.NoodleScript;
import net.highwayfrogs.editor.scripting.runtime.NoodlePrimitive;
import net.highwayfrogs.editor.scripting.runtime.NoodleThread;
import net.highwayfrogs.editor.utils.FXUtils;

/**
 * Prompts the user to answer a yes/no question.
 * Created by Kneesnap on 11/17/2025.
 */
public class NDLFunctionPromptYesNo extends NoodleFunction {
    public static NDLFunctionPromptYesNo INSTANCE = new NDLFunctionPromptYesNo();

    private NDLFunctionPromptYesNo() {
        super("promptYesNo", "<headerText> <message>");
    }

    @Override
    public NoodlePrimitive execute(NoodleThread<? extends NoodleScript> thread, NoodlePrimitive[] args) {
        String headerText = args[0].getAsString();
        String message = args[1].getAsString();
        boolean response = FXUtils.makePopUpYesNo(headerText, message);
        return thread.getStack().pushBoolean(response);
    }
}
