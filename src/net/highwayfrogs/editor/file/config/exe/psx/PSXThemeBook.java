package net.highwayfrogs.editor.file.config.exe.psx;

import lombok.Getter;
import lombok.Setter;
import net.highwayfrogs.editor.file.config.exe.ThemeBook;
import net.highwayfrogs.editor.file.config.exe.pc.PCThemeBook;
import net.highwayfrogs.editor.games.sony.SCGameFile;
import net.highwayfrogs.editor.games.sony.frogger.FroggerGameInstance;
import net.highwayfrogs.editor.games.sony.shared.mwd.WADFile;
import net.highwayfrogs.editor.games.sony.shared.vlo2.VloFile;
import net.highwayfrogs.editor.utils.data.reader.DataReader;
import net.highwayfrogs.editor.utils.data.writer.DataWriter;

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
    @Setter private int deathHeight; // For Noodle.

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
    public VloFile getVLO(boolean isMultiplayer, boolean isLowPolyMode) {
        if (isLowPolyMode)
            throw new UnsupportedOperationException("PSXThemeBook doesn't understand what isLowPolygonMode=true means.");

        return isValid() ? getGameInstance().getGameFile(isMultiplayer ? getMultiplayerVloId() : getVloId()) : null;
    }

    @Override
    public WADFile getWAD(boolean isMultiplayer, boolean isLowPolyMode) {
        if (isLowPolyMode)
            throw new UnsupportedOperationException("PSXThemeBook doesn't understand what isLowPolygonMode=true means.");

        return isValid() ? getGameInstance().getGameFile(isMultiplayer ? getMultiplayerWadId() : getWadId()) : null;
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
    public boolean isEntry(SCGameFile<?> file) {
        int resourceId = file.getFileResourceId();
        return this.wadId == resourceId || this.multiplayerWadId == resourceId
                || this.vloId == resourceId || this.multiplayerVloId == resourceId;
    }

    @Override
    public String toString() {
        return "WAD = " + getGameInstance().getResourceName(this.wadId)
                + ", VLO = " + getGameInstance().getResourceName(this.vloId)
                + ", mWAD = " + getGameInstance().getResourceName(this.multiplayerWadId)
                + ", mVLO = " + getGameInstance().getResourceName(this.multiplayerVloId)
                + ", Death Height: " + this.deathHeight;
    }

}