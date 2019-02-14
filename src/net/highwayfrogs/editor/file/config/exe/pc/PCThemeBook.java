package net.highwayfrogs.editor.file.config.exe.pc;

import lombok.Getter;
import net.highwayfrogs.editor.file.MWIFile.FileEntry;
import net.highwayfrogs.editor.file.config.exe.ThemeBook;
import net.highwayfrogs.editor.file.config.exe.psx.PSXThemeBook;
import net.highwayfrogs.editor.file.map.MAPFile;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.vlo.VLOArchive;
import net.highwayfrogs.editor.file.writer.DataWriter;

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
    private int formLibraryPointer;
    private int deathHeight; // Frog drowns under this height.

    @Override
    public void load(DataReader reader) {
        this.highWadId = reader.readInt();
        this.formLibraryPointer = reader.readInt();
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
        writer.writeInt(this.highWadId);
        writer.writeInt(this.formLibraryPointer);
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
    public VLOArchive getVLO(MAPFile map) {
        if (!isValid())
            return null;

        if (map.isMultiplayer()) {
            return getConfig().getGameFile(map.isLowPolyMode() ? getLowMultiplayerVloId() : getHighMultiplayerVloId());
        } else {
            return getConfig().getGameFile(map.isLowPolyMode() ? getLowVloId() : getHighVloId());
        }
    }

    @Override
    public boolean isValid() {
        return this.highVloId != 0 || this.lowVloId != 0 || this.formLibraryPointer != 0;
    }

    @Override
    public void handleCorrection(String[] args) {
        throw new UnsupportedOperationException("This will be implemented if we find a version which requires it.");
    }

    @Override
    public boolean isEntry(FileEntry test) {
        return lowWadId == test.getLoadedId()
                || lowVloId == test.getLoadedId()
                || highWadId == test.getLoadedId()
                || highVloId == test.getLoadedId()
                || lowMultiplayerWadId == test.getLoadedId()
                || lowMultiplayerVloId == test.getLoadedId()
                || highMultiplayerWadId == test.getLoadedId()
                || highMultiplayerVloId == test.getLoadedId();
    }

    @Override
    public String toString() {
        return "WAD[Hi: " + getConfig().getResourceName(highWadId) + ",Lo: " + getConfig().getResourceName(lowWadId)
                + "] VLO[Hi: " + getConfig().getResourceName(highVloId) + ",Lo: " + getConfig().getResourceName(lowVloId)
                + "] mWAD[Hi: " + getConfig().getResourceName(highMultiplayerWadId) + ",Lo: " + getConfig().getResourceName(lowMultiplayerWadId)
                + "] mVLO[Hi: " + getConfig().getResourceName(highMultiplayerVloId) + ",Lo: " + getConfig().getResourceName(lowMultiplayerVloId)
                + "] Death Height: " + deathHeight;
    }

    @Override
    public <T> T execute(Function<PCThemeBook, T> pcHandler, Function<PSXThemeBook, T> psxHandler) {
        return pcHandler.apply(this);
    }
}
