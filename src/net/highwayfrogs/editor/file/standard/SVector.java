package net.highwayfrogs.editor.file.standard;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.Utils;
import net.highwayfrogs.editor.file.GameObject;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;

/**
 * Vector comprised of shorts.
 * Created by Kneesnap on 8/22/2018.
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class SVector extends GameObject {
    private short x;
    private short y;
    private short z;

    public static final int UNPADDED_BYTE_SIZE = 3 * Constants.SHORT_SIZE;
    public static final int PADDED_BYTE_SIZE = UNPADDED_BYTE_SIZE + Constants.SHORT_SIZE;

    @Override
    public void load(DataReader reader) {
        this.x = reader.readShort();
        this.y = reader.readShort();
        this.z = reader.readShort();
    }

    /**
     * Load a SVector with an extra 2 bytes of padding.
     * @param reader The reader to read from.
     */
    public void loadWithPadding(DataReader reader) {
        this.load(reader);
        reader.readShort();
    }

    @Override
    public void save(DataWriter writer) {
        writer.writeShort(getX());
        writer.writeShort(getY());
        writer.writeShort(getZ());
    }

    public String toOBJString() {
        return "v " + -Utils.unsignedShortToFloat(getX()) + " " + -Utils.unsignedShortToFloat(getY()) + " " + Utils.unsignedShortToFloat(getZ());
    }

    /**
     * Write an SVector with an extra 2 bytes of padding.
     * @param writer The writer to write data to.
     */
    public void saveWithPadding(DataWriter writer) {
        save(writer);
        writer.writeNull(Constants.SHORT_SIZE);
    }

    /**
     * Load a SVector with padding from a DataReader.
     * @param reader The data reader to read from.
     * @return vector
     */
    public static SVector readWithPadding(DataReader reader) {
        SVector vector = new SVector();
        vector.loadWithPadding(reader);
        return vector;
    }
}
