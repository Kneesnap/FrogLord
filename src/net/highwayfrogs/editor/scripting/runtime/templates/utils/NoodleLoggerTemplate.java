package net.highwayfrogs.editor.scripting.runtime.templates.utils;

import net.highwayfrogs.editor.scripting.runtime.templates.NoodleObjectTemplate;

import java.util.logging.Logger;

/**
 * Makes core Logger functionality accessible to Noodle.
 * Created by Kneesnap on 10/24/2024.
 */
public class NoodleLoggerTemplate extends NoodleObjectTemplate<Logger> {
    public static final NoodleLoggerTemplate INSTANCE = new NoodleLoggerTemplate();

    public NoodleLoggerTemplate() {
        super(Logger.class, "Logger");
    }

    @Override
    protected void onSetup() {
        // Don't need to add anything for now.
    }

    @Override
    protected void onSetupJvmWrapper() {
        getJvmWrapper().addFunction("severe", String.class);
        getJvmWrapper().addFunction("warning", String.class);
        getJvmWrapper().addFunction("info", String.class);
        getJvmWrapper().addFunction("config", String.class);
        getJvmWrapper().addFunction("fine", String.class);
        getJvmWrapper().addFunction("finer", String.class);
        getJvmWrapper().addFunction("finest", String.class);
        getJvmWrapper().addFunction("getName");
        getJvmWrapper().addFunction("getParent");
        getJvmWrapper().addFunction("setParent", Logger.class);
    }
}
