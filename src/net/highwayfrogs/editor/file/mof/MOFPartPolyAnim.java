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
    private int anim; // TODO: Auto calculate.

    private transient int tempAnimAddress;

    @Override
    public void load(DataReader reader) {
        this.type = reader.readInt();
        this.mprimPointer = reader.readInt();
        reader.readInt(); // Runtime.
        this.anim = reader.readInt();
    }

    @Override
    public void save(DataWriter writer) {
        writer.writeInt(this.type);
        writer.writeInt(this.mprimPointer);
        writer.writeInt(0); // Runtime.
        this.tempAnimAddress = writer.writeNullPointer();
    }

    /**
     * Write extra data.
     * @param writer  The writer to write data to.
     * @param address The address to write.
     */
    public void saveExtra(DataWriter writer, int address) {
        writer.jumpTemp(getTempAnimAddress());
        writer.writeInt(address);
        writer.jumpReturn();
    }
}
