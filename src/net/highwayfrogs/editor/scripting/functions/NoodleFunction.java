package net.highwayfrogs.editor.scripting.functions;

import lombok.Getter;
import net.highwayfrogs.editor.scripting.NoodleScript;
import net.highwayfrogs.editor.scripting.runtime.NoodlePrimitive;
import net.highwayfrogs.editor.scripting.runtime.NoodleRuntimeException;
import net.highwayfrogs.editor.scripting.runtime.NoodleThread;
import net.highwayfrogs.editor.utils.Utils;

/**
 * Represents a basic noodle function.
 */
@Getter
public abstract class NoodleFunction {
    private final String label;
    private final String usage;
    private final int minArgs;

    public NoodleFunction(String label, String usage) {
        this.label = label;
        this.usage = usage;
        this.minArgs = calculateMinimumArgumentCount(usage);
    }

    /**
     * Function handling.
     */
    public abstract NoodlePrimitive execute(NoodleThread<? extends NoodleScript> thread, NoodlePrimitive[] args);

    /**
     * Gets the syntax to use this function.
     * @return functionSyntax
     */
    public String getSyntax() {
        return this.label + " " + this.usage;
    }

    /**
     * Gets the provided thread converted to the specified type.
     * @param threadClass The class to cast the thread to.
     * @param thread The thread to cast.
     * @return castedThread
     * @param <TThread> The type of thread we want to get back.
     */
    protected static <TThread> TThread getThread(Class<TThread> threadClass, NoodleThread<? extends NoodleScript> thread) {
        if (!threadClass.isInstance(thread))
            throw new NoodleRuntimeException("The %s thread was not a(n) %s, but the function expected it to be.", Utils.getSimpleName(thread), Utils.getSimpleName(threadClass));
        return threadClass.cast(thread);
    }

    /**
     * Gets the minimum argument count for a given usage string.
     * @param rawUsage The usage definition.
     * @return minArgs
     */
    public static int calculateMinimumArgumentCount(String rawUsage) {
        int argumentsSeen = 0;

        boolean insideOptionalArg = false;
        boolean insideRequiredArg = false;
        for (int i = 0; i < rawUsage.length(); i++) {
            char temp = rawUsage.charAt(i);
            if (insideRequiredArg) {
                if (temp == '>') {
                    insideRequiredArg = false;
                    argumentsSeen++;
                }
            } else if (insideOptionalArg) {
                if (temp == ']')
                    insideOptionalArg = false;
            } else if (temp == '<') {
                insideRequiredArg = true;
            } else if (temp == '[') {
                insideOptionalArg = true;
            }
        }

        return argumentsSeen;
    }
}