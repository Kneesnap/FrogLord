package net.highwayfrogs.editor.file.mof;

import lombok.Getter;
import net.highwayfrogs.editor.file.GameObject;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;

/**
 * Represents "MR_PART_POLY_ANIMLIST_ENTRY".
 * Created by Kneesnap on 1/8/2019.
 */
@Getter
public class MOFPartPolyAnimEntry extends GameObject {
    private int imageId; // Within .TXL, resolved to global image ID.
    private int duration; // >= 1 Game Cycles.

    @Override
    public void load(DataReader reader) {
        this.imageId = reader.readUnsignedShortAsInt();
        this.duration = reader.readUnsignedShortAsInt();
    }

    @Override
    public void save(DataWriter writer) {
        writer.writeUnsignedShort(this.imageId);
        writer.writeUnsignedShort(this.duration);
    }
}
