package net.highwayfrogs.editor.file.config.exe.pc;

import lombok.Getter;
import net.highwayfrogs.editor.file.config.exe.MapBook;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;

/**
 * PC MapBook implementation.
 * Created by Kneesnap on 1/27/2019.
 */
@Getter
public class PCMapBook extends MapBook {
    private int highMapId; // If both map ids are zero, it means this is a dummied out map entry. (For instance VOL5, SWPM, ARN1, etc)
    private int lowMapId;
    private int highRemapPointer;
    private int lowRemapPointer;
    private boolean useCaveLights;
    private int environmentTexturePointer;
    private int highWadId;
    private int lowWadId;
    private int paletteId;

    @Override
    public void load(DataReader reader) {
        this.highMapId = reader.readInt();
        this.lowMapId = reader.readInt();
        this.highRemapPointer = reader.readInt();
        this.lowRemapPointer = reader.readInt();
        this.useCaveLights = (reader.readInt() == 1);
        this.environmentTexturePointer = reader.readInt();
        this.highWadId = reader.readInt();
        this.lowWadId = reader.readInt();
        this.paletteId = reader.readInt();
    }

    @Override
    public void save(DataWriter writer) {
        writer.writeInt(this.highMapId);
        writer.writeInt(this.lowMapId);
        writer.writeInt(this.highRemapPointer);
        writer.writeInt(this.lowRemapPointer);
        writer.writeInt(this.useCaveLights ? 1 : 0);
        writer.writeInt(this.environmentTexturePointer);
        writer.writeInt(this.highWadId);
        writer.writeInt(this.lowWadId);
        writer.writeInt(this.paletteId);
    }
}
