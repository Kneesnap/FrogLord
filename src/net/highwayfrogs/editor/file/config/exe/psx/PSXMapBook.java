package net.highwayfrogs.editor.file.config.exe.psx;

import lombok.Getter;
import net.highwayfrogs.editor.file.MWIFile.FileEntry;
import net.highwayfrogs.editor.file.WADFile;
import net.highwayfrogs.editor.file.config.FroggerEXEInfo;
import net.highwayfrogs.editor.file.config.exe.MapBook;
import net.highwayfrogs.editor.file.config.exe.pc.PCMapBook;
import net.highwayfrogs.editor.file.map.MAPFile;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.utils.Utils;

import java.util.function.Function;

/**
 * A PSX MapBook implementation.
 * Created by Kneesnap on 1/27/2019.
 */
@Getter
public class PSXMapBook extends MapBook {
    private int mapId;
    private long remapPointer;
    private boolean useCaveLights;
    private long environmentTexturePointer;
    private int wadId;

    @Override
    public void load(DataReader reader) {
        this.mapId = reader.readInt();
        this.remapPointer = reader.readUnsignedIntAsLong();
        this.useCaveLights = (reader.readInt() == 1);
        this.environmentTexturePointer = reader.readUnsignedIntAsLong();
        this.wadId = reader.readInt();
    }

    @Override
    public void save(DataWriter writer) {
        writer.writeInt(this.mapId);
        writer.writeUnsignedInt(this.remapPointer);
        writer.writeInt(this.useCaveLights ? 1 : 0);
        writer.writeUnsignedInt(this.environmentTexturePointer);
        writer.writeInt(this.wadId);
    }

    @Override
    public void readRemapData(FroggerEXEInfo config) {
        this.readRemap(config, this.mapId, this.remapPointer);
    }

    @Override
    public void saveRemapData(DataWriter writer, FroggerEXEInfo config) {
        this.saveRemap(writer, config, this.mapId, this.remapPointer);
    }

    @Override
    public boolean isEntry(FileEntry test) {
        return this.mapId == test.getLoadedId() || this.wadId == test.getLoadedId();
    }

    @Override
    public boolean isDummy() {
        return this.remapPointer == 0;
    }

    @Override
    public <T> T execute(Function<PCMapBook, T> pcHandler, Function<PSXMapBook, T> psxHandler) {
        return psxHandler.apply(this);
    }

    @Override
    public WADFile getWad(MAPFile map) {
        return getConfig().getGameFile(this.wadId);
    }

    @Override
    public void handleCorrection(String[] args) {
        this.mapId = Integer.parseInt(args[0]);
        this.remapPointer = Long.decode(args[1]) + getConfig().getRamPointerOffset();
        this.wadId = Integer.parseInt(args[2]);
    }

    /**
     * Get the map remap pointer where it will be in the executable.
     * @return fileRemapPointer
     */
    public int getFileRemapPointer() {
        return (int) (getRemapPointer() - getConfig().getRamPointerOffset());
    }

    /**
     * Gets the map's file entry.
     * @return mapFileEntry
     */
    public FileEntry getMapEntry() {
        return getConfig().getResourceEntry(this.mapId);
    }

    @Override
    public String toString() {
        return "MAP[" + getConfig().getResourceName(mapId)
                + "] Remap[" + Utils.toHexString(getFileRemapPointer())
                + "] WAD[" + getConfig().getResourceName(wadId)
                + "] ENV[" + getConfig().getTextureIdFromPointer(this.environmentTexturePointer) + "]";
    }
}