package net.highwayfrogs.editor.file.config.exe.psx;

import lombok.Getter;
import net.highwayfrogs.editor.file.config.exe.ThemeBook;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;

/**
 * PSX implementation of ThemeBook.
 * Created by Kneesnap on 1/27/2019.
 */
@Getter
public class PSXThemeBook extends ThemeBook {
    private int wadId;
    private int vloId;
    private int multiplayerWadId;
    private int multiplayerVloId;
    private int formLibraryPointer;
    private int deathHeight;

    @Override
    public void load(DataReader reader) {
        this.wadId = reader.readInt();
        this.formLibraryPointer = reader.readInt();
        this.vloId = reader.readInt();
        this.deathHeight = reader.readInt();
        this.multiplayerWadId = reader.readInt();
        this.multiplayerVloId = reader.readInt();
    }

    @Override
    public void save(DataWriter writer) {
        writer.writeInt(this.wadId);
        writer.writeInt(this.formLibraryPointer);
        writer.writeInt(this.vloId);
        writer.writeInt(this.deathHeight);
        writer.writeInt(this.multiplayerWadId);
        writer.writeInt(this.multiplayerVloId);
    }
}
