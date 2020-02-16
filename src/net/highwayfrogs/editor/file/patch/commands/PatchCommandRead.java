package net.highwayfrogs.editor.file.patch.commands;

import net.highwayfrogs.editor.file.patch.PatchArgumentType;
import net.highwayfrogs.editor.file.patch.PatchRuntime;
import net.highwayfrogs.editor.file.patch.PatchValue;
import net.highwayfrogs.editor.file.patch.reference.PatchValueReference;
import net.highwayfrogs.editor.file.reader.DataReader;

import java.util.List;
import java.util.function.BiConsumer;

/**
 * Allow reading data from the exe.
 * Created by Kneesnap on 1/15/2020.
 */
public class PatchCommandRead extends PatchCommand {
    private BiConsumer<DataReader, PatchValue> reader;

    public PatchCommandRead(String name, BiConsumer<DataReader, PatchValue> reader) {
        super(name);
        this.reader = reader;
    }

    @Override
    public void execute(PatchRuntime runtime, List<PatchValueReference> args) {
        DataReader reader = runtime.getExeInfo().getReader();
        PatchValue value = getValue(runtime, args, 0);
        if (value == null) { // Create a new variable.
            value = new PatchValue(PatchArgumentType.BOOLEAN, false);
            runtime.getVariables().put(getValueText(args, 0), value);
        }

        if (args.size() > 1) // Address is specified.
            reader.setIndex(getValue(runtime, args, 1).getAsInteger());
        this.reader.accept(reader, value);
    }
}
