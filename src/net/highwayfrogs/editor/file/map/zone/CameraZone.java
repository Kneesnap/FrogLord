package net.highwayfrogs.editor.file.map.zone;

import lombok.Getter;
import lombok.Setter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.file.GameObject;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.standard.SVector;
import net.highwayfrogs.editor.file.writer.DataWriter;

/**
 * Holds data for a CameraZone.
 * Created by Kneesnap on 8/22/2018.
 */
@Getter
@Setter
public class CameraZone extends GameObject {
    private short flags;
    private short direction; // Force camera rotation to this direction. -1 is none.
    private SVector northSourceOffset;
    private SVector northTargetOffset;
    private SVector eastSourceOffset;
    private SVector eastTargetOffset;
    private SVector southSourceOffset;
    private SVector southTargetOffset;
    private SVector westSourceOffset;
    private SVector westTargetOffset;

    public static final int BYTE_SIZE = (2 * Constants.SHORT_SIZE) + (8 * SVector.PADDED_BYTE_SIZE);

    @Override
    public void load(DataReader reader) {
        this.flags = reader.readShort();
        this.direction = reader.readShort();
        this.northSourceOffset = SVector.readWithPadding(reader);
        this.northTargetOffset = SVector.readWithPadding(reader);
        this.eastSourceOffset = SVector.readWithPadding(reader);
        this.eastTargetOffset = SVector.readWithPadding(reader);
        this.southSourceOffset = SVector.readWithPadding(reader);
        this.southTargetOffset = SVector.readWithPadding(reader);
        this.westSourceOffset = SVector.readWithPadding(reader);
        this.westTargetOffset = SVector.readWithPadding(reader);
    }

    @Override
    public void save(DataWriter writer) {
        writer.writeShort(getFlags());
        writer.writeShort(getDirection());
        this.northSourceOffset.saveWithPadding(writer);
        this.northTargetOffset.saveWithPadding(writer);
        this.eastSourceOffset.saveWithPadding(writer);
        this.eastTargetOffset.saveWithPadding(writer);
        this.southSourceOffset.saveWithPadding(writer);
        this.southTargetOffset.saveWithPadding(writer);
        this.westSourceOffset.saveWithPadding(writer);
        this.westTargetOffset.saveWithPadding(writer);
    }
}
