package net.highwayfrogs.editor.file.config.exe.pc;

import lombok.Getter;
import net.highwayfrogs.editor.file.MWIFile.FileEntry;
import net.highwayfrogs.editor.file.config.FroggerEXEInfo;
import net.highwayfrogs.editor.file.config.exe.MapBook;
import net.highwayfrogs.editor.file.config.exe.psx.PSXMapBook;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.utils.Utils;

import java.util.function.Function;

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
    private long environmentTexturePointer;
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
        this.environmentTexturePointer = reader.readUnsignedIntAsLong();
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
        writer.writeUnsignedInt(this.environmentTexturePointer);
        writer.writeInt(this.highWadId);
        writer.writeInt(this.lowWadId);
        writer.writeInt(this.paletteId);
    }

    @Override
    public void readRemapData(FroggerEXEInfo config) {
        this.readRemap(config, this.highMapId, this.highRemapPointer);
        this.readRemap(config, this.lowMapId, this.lowRemapPointer);
    }

    @Override
    public void saveRemapData(DataWriter writer, FroggerEXEInfo config) {
        this.saveRemap(writer, config, this.highMapId, this.highRemapPointer);
        this.saveRemap(writer, config, this.lowMapId, this.lowRemapPointer);
    }

    /**
     * Get the low map remap pointer where it will be in the executable.
     * @return fileLowRemapPointer
     */
    public int getFileLowRemapPointer() {
        return (int) (getLowRemapPointer() - getConfig().getRamPointerOffset());
    }

    /**
     * Get the high map remap pointer where it will be in the executable.
     * @return fileHighRemapPointer
     */
    public int getFileHighRemapPointer() {
        return (int) (getHighRemapPointer() - getConfig().getRamPointerOffset());
    }

    @Override
    public boolean isEntry(FileEntry test) {
        return this.lowMapId == test.getLoadedId()
                || this.lowWadId == test.getLoadedId()
                || this.highMapId == test.getLoadedId()
                || this.highWadId == test.getLoadedId();
    }

    @Override
    public <T> T execute(Function<PCMapBook, T> pcHandler, Function<PSXMapBook, T> psxHandler) {
        return pcHandler.apply(this);
    }

    @Override
    public boolean isDummy() {
        return this.lowRemapPointer == 0 && this.highRemapPointer == 0;
    }

    @Override
    public String toString() {
        return "MAP[Hi: " + getConfig().getResourceName(highMapId) + ",Lo: " + getConfig().getResourceName(lowMapId)
                + "] Remap[Hi: " + Utils.toHexString(getFileHighRemapPointer()) + ",Lo: " + Utils.toHexString(getFileLowRemapPointer())
                + "] WAD[Hi: " + getConfig().getResourceName(highWadId) + ",Lo: " + getConfig().getResourceName(lowWadId)
                + "] PAL: " + getConfig().getResourceName(paletteId)
                + " ENV: " + getConfig().getTextureIdFromPointer(this.environmentTexturePointer);
    }
}
