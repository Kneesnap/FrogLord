package net.highwayfrogs.editor.file.mof;

import lombok.Getter;
import net.highwayfrogs.editor.file.GameObject;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;

/**
 * Represents "MR_PART_POLY_ANIM".
 * Created by Kneesnap on 1/8/2019.
 */
@Getter
public class MOFPartPolyAnim extends GameObject {
    private int type; //TODO: Port.
    private int mprimPointer; //TODO: Calculate automagically.
    private int polyOffset; //TODO: Calculate.

    @Override
    public void load(DataReader reader) {
        this.type = reader.readInt();
        this.mprimPointer = reader.readInt();
        this.polyOffset = reader.readInt();
        reader.readInt(); // Runtime.
    }

    @Override
    public void save(DataWriter writer) {
        writer.writeInt(this.type);
        writer.writeInt(this.mprimPointer);
        writer.writeInt(this.polyOffset);
        writer.writeInt(0); // Runtime.
    }
}
