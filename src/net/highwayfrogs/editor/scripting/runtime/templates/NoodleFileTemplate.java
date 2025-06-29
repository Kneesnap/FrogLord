package net.highwayfrogs.editor.scripting.runtime.templates;

import net.highwayfrogs.editor.scripting.NoodleScriptEngine;
import net.highwayfrogs.editor.scripting.runtime.NoodleObjectInstance;
import net.highwayfrogs.editor.scripting.runtime.NoodleRuntimeException;
import net.highwayfrogs.editor.scripting.runtime.NoodleThread;

import java.io.File;

/**
 * Represents a File object in Noodle.
 * In order to keep Noodle scripts sandboxed, we've taken the approach of specifically implementing all features.
 * One important design choice is to only allow representing files within the allowed sandbox.
 */
public class NoodleFileTemplate extends NoodleObjectTemplate<File> {
    public static final NoodleFileTemplate INSTANCE = new NoodleFileTemplate();

    private NoodleFileTemplate() {
        super(File.class, "File");
    }

    @Override
    protected void onSetup() {
        addConstructor((thread, args) -> {
            String relativeFilePath = args[0].getStringValue();
            File scriptFolder = thread.getScript().getScriptFolder();
            if (scriptFolder == null)
                throw new NoodleRuntimeException("The script does not have a known folder, so the file cannot be created!");

            return new File(scriptFolder, relativeFilePath);
        }, "filePath");

        addConstructor((thread, args) -> {
            File parentFile = getRequiredObjectInstance(this, args, 0);
            String relativeFilePath = args[1].getStringValue();
            return new File(parentFile, relativeFilePath);
        }, "parent", "filePath");
    }

    @Override
    protected void onSetupJvmWrapper() {
        getJvmWrapper().addFunction("getName");
        getJvmWrapper().addFunction("getParentFile");
        getJvmWrapper().addFunction("getPath");
        getJvmWrapper().addFunction("isAbsolute");
        getJvmWrapper().addFunction("getAbsolutePath");
        getJvmWrapper().addFunction("getAbsoluteFile");
        getJvmWrapper().addFunction("getCanonicalFile");
        getJvmWrapper().addFunction("canRead");
        getJvmWrapper().addFunction("canWrite");
        getJvmWrapper().addFunction("exists");
        getJvmWrapper().addFunction("isFile");
        getJvmWrapper().addFunction("isDirectory");
        getJvmWrapper().addFunction("lastModified");
        getJvmWrapper().addFunction("length");
        getJvmWrapper().addFunction("delete");
        getJvmWrapper().addFunction("listFiles");
        getJvmWrapper().addFunction("renameTo", File.class);
        getJvmWrapper().addFunction("setLastModified", long.class);
    }

    @Override
    public void onObjectAddToHeap(NoodleThread<?> thread, File file, NoodleObjectInstance instance) {
        super.onObjectAddToHeap(thread, file, instance);
        validateFilePath(thread, file);
    }

    /**
     * Validates the file by throwing an Exception if the script should be denied access to the file.
     * @param thread the thread to check
     * @param file the file to check
     */
    public static void validateFilePath(NoodleThread<?> thread, File file) {
        if (thread == null)
            throw new NullPointerException("thread");
        if (file == null)
            return; // Null is fine.

        NoodleScriptEngine engine = thread.getEngine();
        if (engine == null)
            throw new NoodleRuntimeException("The thread has no scripting engine!");

        if (!engine.isWhitelistedFilePath(file))
            throw new NoodleRuntimeException("The script tried to access '%s', but is not allowed to!", file);
    }
}
