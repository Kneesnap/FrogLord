package net.highwayfrogs.editor.file.config.exe.pc;

import lombok.Getter;
import net.highwayfrogs.editor.file.config.exe.MapBook;
import net.highwayfrogs.editor.file.config.exe.psx.PSXMapBook;
import net.highwayfrogs.editor.games.sony.SCGameFile;
import net.highwayfrogs.editor.games.sony.frogger.FroggerGameInstance;
import net.highwayfrogs.editor.games.sony.frogger.map.FroggerMapFile;
import net.highwayfrogs.editor.games.sony.shared.mwd.WADFile;
import net.highwayfrogs.editor.games.sony.shared.mwd.mwi.MWIResourceEntry;
import net.highwayfrogs.editor.utils.NumberUtils;
import net.highwayfrogs.editor.utils.data.reader.DataReader;
import net.highwayfrogs.editor.utils.data.writer.DataWriter;

import java.util.function.Function;

/**
 * PC MapBook implementation.
 * Created by Kneesnap on 1/27/2019.
 */
@Getter
public class PCMapBook extends MapBook {
    private int highMapId; // If both map ids are zero, it means this is a dummied out map entry. (For instance VOL5, SWPM, ARN1, etc)
    private int lowMapId;
    private long highRemapPointer;
    private long lowRemapPointer;
    private boolean useCaveLights;
    private long environmentTexturePointer;
    private int highWadId;
    private int lowWadId;
    private int paletteId;

    public PCMapBook(FroggerGameInstance instance) {
        super(instance);
    }

    @Override
    public void load(DataReader reader) {
        this.highMapId = reader.readInt();
        this.lowMapId = reader.readInt();
        this.highRemapPointer = reader.readUnsignedIntAsLong();
        this.lowRemapPointer = reader.readUnsignedIntAsLong();
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
        writer.writeUnsignedInt(this.highRemapPointer);
        writer.writeUnsignedInt(this.lowRemapPointer);
        writer.writeInt(this.useCaveLights ? 1 : 0);
        writer.writeUnsignedInt(this.environmentTexturePointer);
        writer.writeInt(this.highWadId);
        writer.writeInt(this.lowWadId);
        writer.writeInt(this.paletteId);
    }

    @Override
    public void addTextureRemaps(FroggerGameInstance instance) {
        addRemap(instance, this.highMapId, this.highRemapPointer, false);
        addRemap(instance, this.lowMapId, this.lowRemapPointer, true);
    }

    /**
     * Get the low map remap pointer where it will be in the executable.
     * @return fileLowRemapPointer
     */
    public int getFileLowRemapPointer() {
        return (int) (getLowRemapPointer() - getGameInstance().getRamOffset());
    }

    /**
     * Get the high map remap pointer where it will be in the executable.
     * @return fileHighRemapPointer
     */
    public int getFileHighRemapPointer() {
        return (int) (getHighRemapPointer() - getGameInstance().getRamOffset());
    }

    @Override
    public boolean isEntry(SCGameFile<?> file) {
        MWIResourceEntry mwiEntry = file.getIndexEntry();
        if (mwiEntry == null)
            return false; // There is no MWI entry.

        int fileResourceId = mwiEntry.getResourceId();
        return this.lowMapId == fileResourceId
                || this.lowWadId == fileResourceId
                || this.highMapId == fileResourceId
                || this.highWadId == fileResourceId;
    }

    @Override
    public <T> T execute(Function<PCMapBook, T> pcHandler, Function<PSXMapBook, T> psxHandler) {
        return pcHandler.apply(this);
    }

    @Override
    public WADFile getLevelWad(FroggerMapFile map) {
        int fileResourceId = map.getFileResourceId();
        if (this.lowMapId == fileResourceId)
            return getGameInstance().getGameFile(this.lowWadId);

        if (this.highMapId == fileResourceId)
            return getGameInstance().getGameFile(this.highWadId);

        return null;
    }

    @Override
    public WADFile getThemeWad(FroggerMapFile map) {
        return null; // The PC version does not have theme wads.
    }

    @Override
    public boolean isDummy() {
        return this.lowRemapPointer == 0 && this.highRemapPointer == 0;
    }

    @Override
    public String toString() {
        return "MAP[Hi: " + getGameInstance().getResourceName(highMapId) + ",Lo: " + getGameInstance().getResourceName(lowMapId)
                + "] Remap[Hi: " + NumberUtils.toHexString(getFileHighRemapPointer()) + ",Lo: " + NumberUtils.toHexString(getFileLowRemapPointer())
                + "] WAD[Hi: " + getGameInstance().getResourceName(highWadId) + ",Lo: " + getGameInstance().getResourceName(lowWadId)
                + "] PAL: " + getGameInstance().getResourceName(paletteId)
                + " ENV: " + getGameInstance().getTextureIdFromPointer(this.environmentTexturePointer);
    }
}