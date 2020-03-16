package net.highwayfrogs.editor.file.patch.commands;

import net.highwayfrogs.editor.file.patch.PatchRuntime;
import net.highwayfrogs.editor.file.patch.reference.PatchValueReference;

import java.util.List;

/**
 * Allows setting a variable value.
 * set <varName> <value>
 * Created by Kneesnap on 1/15/2020.
 */
public class PatchCommandSet extends PatchCommand {
    public PatchCommandSet() {
        super("set");
    }

    @Override
    public void execute(PatchRuntime runtime, List<PatchValueReference> args) {
        runtime.getVariables().put(getValueText(args, 0), getValue(runtime, args, 1));
    }
}
