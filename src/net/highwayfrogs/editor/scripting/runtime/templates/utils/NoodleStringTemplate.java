package net.highwayfrogs.editor.scripting.runtime.templates.utils;

import net.highwayfrogs.editor.scripting.runtime.templates.NoodleObjectTemplate;

/**
 * Represents a String object.
 * TODO: This is a temporary template, as we can use the lazy wrapper instead later.
 * Created by Kneesnap on 10/25/2024.
 */
public class NoodleStringTemplate extends NoodleObjectTemplate<String> {
    public static final NoodleStringTemplate INSTANCE = new NoodleStringTemplate();

    public NoodleStringTemplate() {
        super(String.class, "String");
    }

    @Override
    protected void onSetup() {
        addStaticFunction("startsWith",
                (thread, args) -> thread.getStack().pushBoolean(args[0].getAsString().startsWith(args[1].getAsString())),
                "str1", "str2");
        addStaticFunction("contains",
                (thread, args) -> thread.getStack().pushBoolean(args[0].getAsString().contains(args[1].getAsString())),
                "str1", "str2");

    }

    @Override
    protected void onSetupJvmWrapper() {
        getJvmWrapper().makeFullyAccessible();
    }
}
