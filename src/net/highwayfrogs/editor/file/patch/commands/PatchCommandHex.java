package net.highwayfrogs.editor.file.patch.commands;

import net.highwayfrogs.editor.file.patch.PatchRuntime;
import net.highwayfrogs.editor.file.patch.PatchValue;
import net.highwayfrogs.editor.file.patch.reference.PatchValueReference;
import net.highwayfrogs.editor.utils.NumberUtils;

import java.util.List;

/**
 * Turns a value into hex.
 * hex <targetVar>, targetVar should be an integer.
 * Created by Kneesnap on 1/15/2020.
 */
public class PatchCommandHex extends PatchCommand {
    public PatchCommandHex() {
        super("hex");
    }

    @Override
    public void execute(PatchRuntime runtime, List<PatchValueReference> args) {
        PatchValue value = getValue(runtime, args, 0);
        if (!value.isInteger())
            throw new RuntimeException("Can only hexify integer numbers! Not: " + value);
        value.setString(NumberUtils.toHexString(value.getAsInteger()));
    }
}
