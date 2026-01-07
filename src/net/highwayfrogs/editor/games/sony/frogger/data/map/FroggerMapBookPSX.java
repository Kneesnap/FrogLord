package net.highwayfrogs.editor.games.sony.frogger.data.map;

import lombok.Getter;
import lombok.Setter;
import net.highwayfrogs.editor.games.sony.SCGameFile;
import net.highwayfrogs.editor.games.sony.frogger.FroggerGameInstance;
import net.highwayfrogs.editor.games.sony.frogger.FroggerTextureRemap;
import net.highwayfrogs.editor.games.sony.frogger.data.theme.FroggerThemeBookPSX;
import net.highwayfrogs.editor.games.sony.frogger.map.FroggerMapFile;
import net.highwayfrogs.editor.games.sony.shared.mwd.WADFile;
import net.highwayfrogs.editor.utils.NumberUtils;
import net.highwayfrogs.editor.utils.data.reader.DataReader;
import net.highwayfrogs.editor.utils.data.writer.DataWriter;

import java.util.function.Function;

/**
 * A PSX FroggerMapBook implementation.
 * Created by Kneesnap on 1/27/2019.
 */

public class FroggerMapBookPSX extends FroggerMapBook {
    @Getter @Setter private int mapId;
    @Getter @Setter private boolean useCaveLights;
    @Getter @Setter private long environmentTexturePointer = -1;
    @Getter @Setter private int wadId = -1;
    @Getter @Setter private FroggerTextureRemap textureRemap;
    private long tempRemapPointer;

    public FroggerMapBookPSX(FroggerGameInstance instance) {
        super(instance);
    }

    @Override
    public void load(DataReader reader) {
        this.mapId = reader.readInt();
        this.tempRemapPointer = reader.readUnsignedIntAsLong();
        this.useCaveLights = (reader.readInt() == 1);

        if (!getConfig().isBeforeBuild1())
            this.environmentTexturePointer = reader.readUnsignedIntAsLong();

        if (hasPerLevelWadFiles())
            this.wadId = reader.readInt();
    }

    @Override
    public void save(DataWriter writer) {
        writer.writeInt(this.mapId);
        writer.writeUnsignedInt(this.textureRemap != null ? this.textureRemap.getLoadAddress() : this.tempRemapPointer);
        writer.writeInt(this.useCaveLights ? 1 : 0);
        if (!getConfig().isBeforeBuild1())
            writer.writeUnsignedInt(this.environmentTexturePointer);
        if (hasPerLevelWadFiles())
            writer.writeInt(this.wadId);
    }

    @Override
    public void addTextureRemaps(FroggerGameInstance instance) {
        FroggerTextureRemap remap = addRemap(instance, this.mapId, this.tempRemapPointer);
        if (remap != null) {
            this.textureRemap = remap;
            this.tempRemapPointer = -1;
        }
    }

    @Override
    public boolean isEntry(SCGameFile<?> file) {
        int resourceId = file.getFileResourceId();
        return this.mapId == resourceId || this.wadId == resourceId;
    }

    @Override
    public boolean isDummy() {
        return this.tempRemapPointer < 0 && this.textureRemap == null;
    }

    @Override
    public <T> T execute(Function<FroggerMapBookPC, T> pcHandler, Function<FroggerMapBookPSX, T> psxHandler) {
        return psxHandler.apply(this);
    }

    @Override
    public WADFile getLevelWad(FroggerMapFile map) {
        int wadId = this.wadId;

        WADFile wadFile = wadId >= 0 ? getGameInstance().getGameFile(wadId) : null;
        if (wadFile != null)
            return wadFile;

        // PSX Build 6 is the first build seen to have per-level wad files.
        if (!hasPerLevelWadFiles()) {
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
            FroggerThemeBookPSX themeBook = ((FroggerThemeBookPSX) getGameInstance().getThemeBook(map.getMapTheme()));
            if (themeBook != null && themeBook.getWadId() != wadId)
                return getGameInstance().getGameFile(themeBook.getWadId());
        }

        return getGameInstance().getGameFile(wadId);
    }

    @Override
    public void handleCorrection(String[] args) {
        this.mapId = Integer.parseInt(args[0]);
        this.tempRemapPointer = Long.decode(args[1]) + getGameInstance().getRamOffset();
        this.wadId = Integer.parseInt(args[2]);
    }

    /**
     * Gets the pointer to the texture remap.
     */
    public long getRemapPointer() {
        return this.textureRemap != null ? this.textureRemap.getLoadAddress() : this.tempRemapPointer;
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