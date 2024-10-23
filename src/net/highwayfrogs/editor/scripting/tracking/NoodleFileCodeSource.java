package net.highwayfrogs.editor.scripting.tracking;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.highwayfrogs.editor.scripting.NoodleScript;
import net.highwayfrogs.editor.utils.Utils;

import java.io.File;

/**
 * Holds the file source of noodle code.
 */
@Getter
@RequiredArgsConstructor
public class NoodleFileCodeSource extends NoodleCodeSource {
    private final NoodleScript script;
    private final File file;

    @Override
    public String getDisplay() {
        return Utils.toLocalPath(this.file, this.script.getScriptFolder(), true);
    }
}
