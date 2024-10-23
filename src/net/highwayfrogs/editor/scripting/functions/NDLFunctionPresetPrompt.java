package net.highwayfrogs.editor.scripting.functions;

import net.highwayfrogs.editor.gui.SelectionMenu;
import net.highwayfrogs.editor.scripting.NoodleScript;
import net.highwayfrogs.editor.scripting.runtime.NoodlePrimitive;
import net.highwayfrogs.editor.scripting.runtime.NoodleThread;
import net.highwayfrogs.editor.scripting.runtime.NoodleYieldReference;
import net.highwayfrogs.editor.utils.Utils;

import java.util.Arrays;


/**
 * Prompts the player(s) a callback question.
 */
public class NDLFunctionPresetPrompt extends NoodleFunction {
    public static final NDLFunctionPresetPrompt INSTANCE = new NDLFunctionPresetPrompt();

    public NDLFunctionPresetPrompt() {
        super("presetPrompt", "<question> <args...>");
    }

    @Override
    public NoodlePrimitive execute(NoodleThread<? extends NoodleScript> thread, NoodlePrimitive[] args) {
        // Read arguments.
        String question = args[0].getStringValue();
        String[] presetResponses = new String[args.length - 2];
        for (int i = 0; i < presetResponses.length; i++)
            presetResponses[i] = args[i + 1].getStringValue();

        // Register callback.
        NoodleYieldReference yieldRef = thread.yieldThread();

        // Constantly show the question to players in the quest.
        SelectionMenu.promptSelectionAllowNull(thread.getGameInstance(), question,
                response -> yieldRef.resume(new NoodlePrimitive(Utils.indexOf(presetResponses, response))),
                Arrays.asList(presetResponses), value -> value, null);

        return null; // Do not push anything on the stack, since the value pushed on the stack will happen later.
    }
}