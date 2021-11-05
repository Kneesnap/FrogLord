package net.highwayfrogs.editor.games.tgq.entity;

import lombok.Getter;
import lombok.Setter;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;

/**
 * Represents a 3D entity.
 * TODO: Different on PS2.
 * Created by Kneesnap on 1/4/2021.
 */
@Getter
@Setter
public class KCEntity3DInstance extends KCEntityInstance {
    private int flags; // TODO: Values / Options
    private KCAxisType billBoardAxis;
    private float x;
    private float y;
    private float z;

    @Override
    public void load(DataReader reader) {
        super.load(reader);
        this.flags = reader.readInt();
        this.billBoardAxis = KCAxisType.values()[reader.readInt() - 1];
        this.x = reader.readFloat();
        this.y = reader.readFloat();
        this.z = reader.readFloat();
    }

    @Override
    public void save(DataWriter writer) {
        super.save(writer);
        writer.writeInt(this.flags);
        writer.writeInt(this.billBoardAxis.ordinal());
        writer.writeFloat(this.x);
        writer.writeFloat(this.y);
        writer.writeFloat(this.z);
    }
}
