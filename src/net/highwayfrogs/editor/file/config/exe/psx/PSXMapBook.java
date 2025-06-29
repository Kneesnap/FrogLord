package net.highwayfrogs.editor.file.config.exe.psx;

import lombok.Getter;
import lombok.Setter;
import net.highwayfrogs.editor.file.config.exe.MapBook;
import net.highwayfrogs.editor.file.config.exe.pc.PCMapBook;
import net.highwayfrogs.editor.games.sony.SCGameFile;
import net.highwayfrogs.editor.games.sony.frogger.FroggerGameInstance;
import net.highwayfrogs.editor.games.sony.frogger.map.FroggerMapFile;
import net.highwayfrogs.editor.games.sony.shared.mwd.WADFile;
import net.highwayfrogs.editor.utils.NumberUtils;
import net.highwayfrogs.editor.utils.data.reader.DataReader;
import net.highwayfrogs.editor.utils.data.writer.DataWriter;

import java.util.function.Function;

/**
 * A PSX MapBook implementation.
 * Created by Kneesnap on 1/27/2019.
 */
@Getter
@Setter
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
        addRemap(instance, this.mapId, this.remapPointer, false);
    }

    @Override
    public boolean isEntry(SCGameFile<?> file) {
        int resourceId = file.getFileResourceId();
        return this.mapId == resourceId || this.wadId == resourceId;
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
    public WADFile getLevelWad(FroggerMapFile map) {
        int wadId = this.wadId;

        WADFile wadFile = wadId >= 0 ? getGameInstance().getGameFile(wadId) : null;
        if (wadFile != null)
            return wadFile;

        // PSX Build 6 is the first build seen to have per-level wad files.
        if (getGameInstance().getVersionConfig().isAtOrBeforeBuild4()) {
            wadFile = getThemeWad(map);
            if (wadFile != null)
                return wadFile;
        }

        throw new RuntimeException("Could not resolve wad file for '" + map.getFileDisplayName() + "'.");
    }

    @Override
    public WADFile getThemeWad(FroggerMapFile map) {
        int wadId = this.wadId;

        // When the map reports a particular theme, I think it's reliable.
        if (map != null && map.getMapTheme() != null && !map.isMultiplayer()) {
            PSXThemeBook themeBook = ((PSXThemeBook) getGameInstance().getThemeBook(map.getMapTheme()));
            if (themeBook != null && themeBook.getWadId() != wadId)
                return getGameInstance().getGameFile(themeBook.getWadId());
        }

        return getGameInstance().getGameFile(wadId);
    }

    @Override
    public void handleCorrection(String[] args) {
        this.mapId = Integer.parseInt(args[0]);
        this.remapPointer = Long.decode(args[1]) + getGameInstance().getRamOffset();
        this.wadId = Integer.parseInt(args[2]);
    }

    /**
     * Get the map remap pointer where it will be in the executable.
     * @return fileRemapPointer
     */
    public int getFileRemapPointer() {
        return (int) (getRemapPointer() - getGameInstance().getRamOffset());
    }

    @Override
    public String toString() {
        return "MAP[" + getGameInstance().getResourceName(this.mapId)
                + "] Remap[" + NumberUtils.toHexString(getFileRemapPointer())
                + "] WAD[" + getGameInstance().getResourceName(this.wadId)
                + "] ENV[" + getGameInstance().getTextureIdFromPointer(this.environmentTexturePointer) + "]";
    }
}