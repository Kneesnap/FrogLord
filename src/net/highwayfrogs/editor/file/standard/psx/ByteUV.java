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
 * Holds texture UV information.
 * Created by Kneesnap on 8/25/2018.
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ByteUV extends GameObject {
    private byte u;
    private byte v;

    public static final int BYTE_SIZE = 2 * Constants.BYTE_SIZE;

    @Override
    public void load(DataReader reader) {
        this.u = reader.readByte();
        this.v = reader.readByte();
    }

    @Override
    public void save(DataWriter writer) {
        writer.writeByte(this.u);
        writer.writeByte(this.v);
    }
}
