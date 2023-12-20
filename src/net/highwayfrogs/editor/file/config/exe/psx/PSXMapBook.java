package net.highwayfrogs.editor.file.config.exe.psx;

import lombok.Getter;
import net.highwayfrogs.editor.file.MWIFile.FileEntry;
import net.highwayfrogs.editor.file.WADFile;
import net.highwayfrogs.editor.file.config.exe.MapBook;
import net.highwayfrogs.editor.file.config.exe.pc.PCMapBook;
import net.highwayfrogs.editor.file.map.MAPFile;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.sony.frogger.FroggerGameInstance;
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
    private long environmentTexturePointer = -1;
    private int wadId = -1;

    public PSXMapBook(FroggerGameInstance instance) {
        super(instance);
    }

    @Override
    public void load(DataReader reader) {
        this.mapId = reader.readInt();
        this.remapPointer = reader.readUnsignedIntAsLong();
        this.useCaveLights = (reader.readInt() == 1);

        if (!getConfig().isBeforeBuild1())
            this.environmentTexturePointer = reader.readUnsignedIntAsLong();

        if (!getConfig().isAtOrBeforeBuild4())
            this.wadId = reader.readInt();
    }

    @Override
    public void save(DataWriter writer) {
        writer.writeInt(this.mapId);
        writer.writeUnsignedInt(this.remapPointer);
        writer.writeInt(this.useCaveLights ? 1 : 0);
        if (!getConfig().isBeforeBuild1())
            writer.writeUnsignedInt(this.environmentTexturePointer);
        if (!getConfig().isAtOrBeforeBuild4())
            writer.writeInt(this.wadId);
    }

    @Override
    public void addTextureRemaps(FroggerGameInstance instance) {
        addRemap(instance, this.mapId, (int) this.remapPointer, false);
    }

    @Override
    public boolean isEntry(FileEntry test) {
        return this.mapId == test.getResourceId() || this.wadId == test.getResourceId();
    }

    @Override
    public boolean isDummy() {
        return this.remapPointer <= 0;
    }

    @Override
    public <T> T execute(Function<PCMapBook, T> pcHandler, Function<PSXMapBook, T> psxHandler) {
        return psxHandler.apply(this);
    }

    @Override
    public WADFile getWad(MAPFile map) {
        int wadId = this.wadId;
        if (wadId < 0) // Determine the WAD ID from the map theme if necessary.
            wadId = ((PSXThemeBook) getGameInstance().getThemeBook(map.getTheme())).getWadId();

        return getGameInstance().getGameFile(wadId);
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

    @Override
    public String toString() {
        return "MAP[" + getGameInstance().getResourceName(mapId)
                + "] Remap[" + Utils.toHexString(getFileRemapPointer())
                + "] WAD[" + getGameInstance().getResourceName(wadId)
                + "] ENV[" + getGameInstance().getTextureIdFromPointer(this.environmentTexturePointer) + "]";
    }
}