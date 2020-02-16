package net.highwayfrogs.editor.file.patch.commands;

import net.highwayfrogs.editor.file.patch.PatchRuntime;
import net.highwayfrogs.editor.file.patch.PatchValue;
import net.highwayfrogs.editor.file.patch.reference.PatchValueReference;
import net.highwayfrogs.editor.file.writer.DataWriter;

import java.util.List;
import java.util.function.BiConsumer;

/**
 * Write data to the EXE.
 * Created by Kneesnap on 1/15/2020.
 */
public class PatchCommandWrite extends PatchCommand {
    private BiConsumer<DataWriter, PatchValue> writer;

    public PatchCommandWrite(String name, BiConsumer<DataWriter, PatchValue> writer) {
        super(name);
        this.writer = writer;
    }

    @Override
    public void execute(PatchRuntime runtime, List<PatchValueReference> args) {
        DataWriter dataWriter = runtime.getExeWriter();
        if (args.size() > 1) // Address is specified.
            dataWriter.setIndex(getValue(runtime, args, 1).getAsInteger());
        this.writer.accept(dataWriter, getValue(runtime, args, 0));
    }
}
