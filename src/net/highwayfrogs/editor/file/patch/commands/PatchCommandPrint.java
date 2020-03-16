package net.highwayfrogs.editor.file.patch.commands;

import net.highwayfrogs.editor.file.patch.PatchRuntime;
import net.highwayfrogs.editor.file.patch.PatchValue;
import net.highwayfrogs.editor.file.patch.reference.PatchValueReference;

import java.util.List;

/**
 * Allow printing things to the console, likely for debugging.
 * print <message>
 * Created by Kneesnap on 1/15/2020.
 */
public class PatchCommandPrint extends PatchCommand {
    public PatchCommandPrint() {
        super("print");
    }

    @Override
    public void execute(PatchRuntime runtime, List<PatchValueReference> args) {
        PatchValue value = getValue(runtime, args, 0);
        System.out.println("[Patch/" + runtime.getPatch().getName() + "] " + (value.isString() ? value.getAsString() : value.toString()));
    }
}
