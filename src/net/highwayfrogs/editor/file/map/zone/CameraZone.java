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
    private int flags;
    private short forceDirection; // Force camera rotation to this direction. -1 is none.
    private SVector northSourceOffset = new SVector();
    private SVector northTargetOffset = new SVector();
    private SVector eastSourceOffset = new SVector();
    private SVector eastTargetOffset = new SVector();
    private SVector southSourceOffset = new SVector();
    private SVector southTargetOffset = new SVector();
    private SVector westSourceOffset = new SVector();
    private SVector westTargetOffset = new SVector();

    public static final int FLAG_OUTRO = Constants.BIT_FLAG_1;
    public static final int FLAG_SEMIFORCED = Constants.BIT_FLAG_2;
    public static final int FLAG_ABSOLUTE_Y = Constants.BIT_FLAG_3; // Use y offsets as world y position.
    public static final int FLAG_CHECKPOINT = Constants.BIT_FLAG_4;

    public static final int BYTE_SIZE = (2 * Constants.SHORT_SIZE) + (8 * SVector.PADDED_BYTE_SIZE);

    @Override
    public void load(DataReader reader) {
        this.flags = reader.readUnsignedShortAsInt();
        this.forceDirection = reader.readShort();
        this.northSourceOffset.loadWithPadding(reader);
        this.northTargetOffset.loadWithPadding(reader);
        this.eastSourceOffset.loadWithPadding(reader);
        this.eastTargetOffset.loadWithPadding(reader);
        this.southSourceOffset.loadWithPadding(reader);
        this.southTargetOffset.loadWithPadding(reader);
        this.westSourceOffset.loadWithPadding(reader);
        this.westTargetOffset.loadWithPadding(reader);
    }

    @Override
    public void save(DataWriter writer) {
        writer.writeUnsignedShort(getFlags());
        writer.writeShort(getForceDirection());
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
