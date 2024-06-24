package net.highwayfrogs.editor.file.config.exe.psx;

import lombok.Getter;
import net.highwayfrogs.editor.file.MWIFile.FileEntry;
import net.highwayfrogs.editor.file.WADFile;
import net.highwayfrogs.editor.file.config.exe.ThemeBook;
import net.highwayfrogs.editor.file.config.exe.pc.PCThemeBook;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.vlo.VLOArchive;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.sony.frogger.FroggerGameInstance;
import net.highwayfrogs.editor.games.sony.frogger.map.FroggerMapFile;

import java.util.function.Function;

/**
 * PSX implementation of ThemeBook.
 * Created by Kneesnap on 1/27/2019.
 */
@Getter
public class PSXThemeBook extends ThemeBook {
    private int wadId;
    private int vloId;
    private int multiplayerWadId = -1;
    private int multiplayerVloId = -1;
    private long formLibraryPointer;
    private int deathHeight;

    public PSXThemeBook(FroggerGameInstance instance) {
        super(instance);
    }

    @Override
    public void load(DataReader reader) {
        this.wadId = reader.readInt();
        this.formLibraryPointer = reader.readUnsignedIntAsLong();
        this.vloId = reader.readInt();
        this.deathHeight = reader.readInt();

        if (!getConfig().isAtOrBeforeBuild1()) {
            this.multiplayerWadId = reader.readInt();
            this.multiplayerVloId = reader.readInt();
        }
    }

    @Override
    public void save(DataWriter writer) {
        super.save(writer);
        writer.writeInt(this.wadId);
        writer.writeUnsignedInt(this.formLibraryPointer);
        writer.writeInt(this.vloId);
        writer.writeInt(this.deathHeight);

        if (!getConfig().isAtOrBeforeBuild1()) {
            writer.writeInt(this.multiplayerWadId);
            writer.writeInt(this.multiplayerVloId);
        }
    }

    @Override
    public VLOArchive getVLO(FroggerMapFile map) {
        return isValid() ? getGameInstance().getGameFile(map.isMultiplayer() ? getMultiplayerVloId() : getVloId()) : null;
    }

    @Override
    public WADFile getWAD(FroggerMapFile map) {
        return isValid() ? getGameInstance().getGameFile(map.isMultiplayer() ? getMultiplayerWadId() : getWadId()) : null;
    }

    @Override
    public boolean isValid() {
        return this.vloId != 0 || this.formLibraryPointer != 0 || this.wadId != 0;
    }

    @Override
    public <T> T execute(Function<PCThemeBook, T> pcHandler, Function<PSXThemeBook, T> psxHandler) {
        return psxHandler.apply(this);
    }

    @Override
    public void handleCorrection(String[] args) {
        this.wadId = Integer.parseInt(args[0]);
        this.vloId = Integer.parseInt(args[1]);
        this.multiplayerWadId = Integer.parseInt(args[2]);
        this.multiplayerVloId = Integer.parseInt(args[3]);
        this.formLibraryPointer = Long.decode(args[4]);
        if (this.formLibraryPointer > 0)
            this.formLibraryPointer += getGameInstance().getRamOffset();
    }

    @Override
    public boolean isEntry(FileEntry test) {
        return wadId == test.getResourceId() || multiplayerWadId == test.getResourceId()
                || vloId == test.getResourceId() || multiplayerVloId == test.getResourceId();
    }

    @Override
    public String toString() {
        return "WAD = " + getGameInstance().getResourceName(wadId)
                + ", VLO = " + getGameInstance().getResourceName(vloId)
                + ", mWAD = " + getGameInstance().getResourceName(multiplayerWadId)
                + ", mVLO = " + getGameInstance().getResourceName(multiplayerVloId)
                + ", Death Height: " + deathHeight;
    }

}