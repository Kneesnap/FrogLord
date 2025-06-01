package net.highwayfrogs.editor.file.config.exe.pc;

import lombok.Getter;
import net.highwayfrogs.editor.file.config.exe.ThemeBook;
import net.highwayfrogs.editor.file.config.exe.psx.PSXThemeBook;
import net.highwayfrogs.editor.file.vlo.VLOArchive;
import net.highwayfrogs.editor.games.sony.SCGameFile;
import net.highwayfrogs.editor.games.sony.frogger.FroggerGameInstance;
import net.highwayfrogs.editor.games.sony.shared.mwd.WADFile;
import net.highwayfrogs.editor.utils.data.reader.DataReader;
import net.highwayfrogs.editor.utils.data.writer.DataWriter;

import java.util.function.Function;

/**
 * A PC ThemeBook implementation.
 * Created by Kneesnap on 1/27/2019.
 */
@Getter
public class PCThemeBook extends ThemeBook {
    private int lowWadId;
    private int lowVloId;
    private int highWadId;
    private int highVloId;
    private int lowMultiplayerWadId;
    private int lowMultiplayerVloId;
    private int highMultiplayerWadId;
    private int highMultiplayerVloId;
    private long formLibraryPointer;
    private int deathHeight; // Frog drowns under this height.

    public PCThemeBook(FroggerGameInstance instance) {
        super(instance);
    }

    @Override
    public void load(DataReader reader) {
        this.highWadId = reader.readInt();
        this.formLibraryPointer = reader.readUnsignedIntAsLong();
        this.highVloId = reader.readInt();
        this.deathHeight = reader.readInt();
        this.highMultiplayerWadId = reader.readInt();
        this.highMultiplayerVloId = reader.readInt();
        this.lowWadId = reader.readInt();
        this.lowVloId = reader.readInt();
        this.lowMultiplayerWadId = reader.readInt();
        this.lowMultiplayerVloId = reader.readInt();
    }

    @Override
    public void save(DataWriter writer) {
        super.save(writer);
        writer.writeInt(this.highWadId);
        writer.writeUnsignedInt(this.formLibraryPointer);
        writer.writeInt(this.highVloId);
        writer.writeInt(this.deathHeight);
        writer.writeInt(this.highMultiplayerWadId);
        writer.writeInt(this.highMultiplayerVloId);
        writer.writeInt(this.lowWadId);
        writer.writeInt(this.lowVloId);
        writer.writeInt(this.lowMultiplayerWadId);
        writer.writeInt(this.lowMultiplayerVloId);
    }

    @Override
    public VLOArchive getVLO(boolean isMultiplayer, boolean isLowPolyMode) {
        if (!isValid())
            return null;

        if (isMultiplayer) {
            return getGameInstance().getGameFile(isLowPolyMode ? getLowMultiplayerVloId() : getHighMultiplayerVloId());
        } else {
            return getGameInstance().getGameFile(isLowPolyMode ? getLowVloId() : getHighVloId());
        }
    }

    @Override
    public WADFile getWAD(boolean isMultiplayer, boolean isLowPolyMode) {
        if (!isValid())
            return null;

        if (isMultiplayer) {
            return getGameInstance().getGameFile(isLowPolyMode ? getLowMultiplayerWadId() : getHighMultiplayerWadId());
        } else {
            return getGameInstance().getGameFile(isLowPolyMode ? getLowWadId() : getHighWadId());
        }
    }

    @Override
    public boolean isValid() {
        return this.highVloId != 0 || this.lowVloId != 0 || this.formLibraryPointer != 0;
    }

    @Override
    public boolean isEntry(SCGameFile<?> file) {
        int resourceId = file.getFileResourceId();
        return this.lowWadId == resourceId
                || this.lowVloId == resourceId
                || this.highWadId == resourceId
                || this.highVloId == resourceId
                || this.lowMultiplayerWadId == resourceId
                || this.lowMultiplayerVloId == resourceId
                || this.highMultiplayerWadId == resourceId
                || this.highMultiplayerVloId == resourceId;
    }

    @Override
    public String toString() {
        return "WAD[Hi: " + getGameInstance().getResourceName(this.highWadId) + ",Lo: " + getGameInstance().getResourceName(this.lowWadId)
                + "] VLO[Hi: " + getGameInstance().getResourceName(this.highVloId) + ",Lo: " + getGameInstance().getResourceName(this.lowVloId)
                + "] mWAD[Hi: " + getGameInstance().getResourceName(this.highMultiplayerWadId) + ",Lo: " + getGameInstance().getResourceName(this.lowMultiplayerWadId)
                + "] mVLO[Hi: " + getGameInstance().getResourceName(this.highMultiplayerVloId) + ",Lo: " + getGameInstance().getResourceName(this.lowMultiplayerVloId)
                + "] Death Height: " + this.deathHeight;
    }

    @Override
    public <T> T execute(Function<PCThemeBook, T> pcHandler, Function<PSXThemeBook, T> psxHandler) {
        return pcHandler.apply(this);
    }
}