package net.highwayfrogs.editor.file.standard.psx;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.file.GameObject;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;

/**
 * Implements the PSX "RECT" struct.
 * Created by Kneesnap on 8/25/2018.
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class PSXRect extends GameObject {
    private short x; // Offset point in VRAM.
    private short y;
    private short width;
    private short height;

    public static final int BYTE_SIZE = 4 * Constants.SHORT_SIZE;

    @Override
    public void load(DataReader reader) {
        this.x = reader.readShort();
        this.y = reader.readShort();
        this.width = reader.readShort();
        this.height = reader.readShort();
    }

    @Override
    public void save(DataWriter writer) {
        writer.writeShort(this.x);
        writer.writeShort(this.y);
        writer.writeShort(this.width);
        writer.writeShort(this.height);
    }
}
