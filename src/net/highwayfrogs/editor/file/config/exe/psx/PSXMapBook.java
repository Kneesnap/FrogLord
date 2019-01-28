package net.highwayfrogs.editor.file.config.exe.psx;

import net.highwayfrogs.editor.file.config.exe.MapBook;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;

/**
 * A PSX MapBook implementation.
 * Created by Kneesnap on 1/27/2019.
 */
public class PSXMapBook extends MapBook {
    private int mapId;
    private int remapPointer;
    private boolean useCaveLights;
    private int environmentTexturePointer;
    private int wadId;

    @Override
    public void load(DataReader reader) {
        this.mapId = reader.readInt();
        this.remapPointer = reader.readInt();
        this.useCaveLights = (reader.readInt() == 1);
        this.environmentTexturePointer = reader.readInt();
        this.wadId = reader.readInt();
    }

    @Override
    public void save(DataWriter writer) {
        writer.writeInt(this.mapId);
        writer.writeInt(this.remapPointer);
        writer.writeInt(this.useCaveLights ? 1 : 0);
        writer.writeInt(this.environmentTexturePointer);
        writer.writeInt(this.wadId);
    }
}
