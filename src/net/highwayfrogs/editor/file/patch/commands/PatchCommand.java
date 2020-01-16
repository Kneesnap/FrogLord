package net.highwayfrogs.editor.file.patch.commands;

import lombok.AllArgsConstructor;
import lombok.Getter;
import net.highwayfrogs.editor.file.patch.PatchRuntime;
import net.highwayfrogs.editor.file.patch.PatchValue;
import net.highwayfrogs.editor.file.patch.reference.PatchTextReference;
import net.highwayfrogs.editor.file.patch.reference.PatchValueReference;

import java.util.List;

/**
 * Represents a command that can be run by the patching system.
 * Created by Kneesnap on 1/15/2020.
 */
@Getter
@AllArgsConstructor
public abstract class PatchCommand {
    private String name;

    /**
     * Executes this command under a given runtime with certain arguments.
     * @param runtime The runtime to execute under.
     * @param args    The arguments to execute with.
     */
    public abstract void execute(PatchRuntime runtime, List<PatchValueReference> args);

    protected static String getValueText(List<PatchValueReference> args, int index) {
        return ((PatchTextReference) args.get(index)).getTextData();
    }

    protected static PatchValue getValue(PatchRuntime runtime, List<PatchValueReference> args, int index) {
        return args.get(index).getValue(runtime);
    }
}
