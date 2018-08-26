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
 * Implements the PSX "RECT32" struct.
 * Created by Kneesnap on 8/25/2018.
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class PSXRect32 extends GameObject {
    private int x; // Offset point in VRAM.
    private int y;
    private int width;
    private int height;

    public static final int BYTE_SIZE = 4 * Constants.INTEGER_SIZE;

    @Override
    public void load(DataReader reader) {
        this.x = reader.readInt();
        this.y = reader.readInt();
        this.width = reader.readInt();
        this.height = reader.readInt();
    }

    @Override
    public void save(DataWriter writer) {
        writer.writeInt(this.x);
        writer.writeInt(this.y);
        writer.writeInt(this.width);
        writer.writeInt(this.height);
    }
}
