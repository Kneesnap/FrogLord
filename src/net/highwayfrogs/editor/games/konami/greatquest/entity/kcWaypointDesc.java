package net.highwayfrogs.editor.games.konami.greatquest.entity;

import lombok.Getter;
import lombok.Setter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.konami.greatquest.kcClassID;
import net.highwayfrogs.editor.games.konami.greatquest.map.kcColor4;
import net.highwayfrogs.editor.utils.Utils;

/**
 * Represents the kcWaypointDesc struct.
 * Created by Kneesnap on 8/21/2023.
 */
@Getter
@Setter
public class kcWaypointDesc extends kcEntity3DDesc {
    private short type;
    private short subType;
    private int prevHash;
    private int nextHash;
    private int waypointFlags;
    private final kcColor4 color = new kcColor4();
    private float strength;
    private final int[] padWaypoint = new int[7];

    @Override
    public int getTargetClassID() {
        return kcClassID.WAYPOINT.getClassId();
    }

    @Override
    public void load(DataReader reader) {
        super.load(reader);
        this.type = reader.readShort();
        this.subType = reader.readShort();
        this.prevHash = reader.readInt();
        this.nextHash = reader.readInt();
        this.waypointFlags = reader.readInt();
        this.color.load(reader);
        this.strength = reader.readFloat();
        for (int i = 0; i < this.padWaypoint.length; i++)
            this.padWaypoint[i] = reader.readInt();
    }

    @Override
    public void saveData(DataWriter writer) {
        super.saveData(writer);
        writer.writeShort(this.type);
        writer.writeShort(this.subType);
        writer.writeInt(this.prevHash);
        writer.writeInt(this.nextHash);
        writer.writeInt(this.waypointFlags);
        this.color.save(writer);
        writer.writeFloat(this.strength);
        for (int i = 0; i < this.padWaypoint.length; i++)
            writer.writeInt(this.padWaypoint[i]);
    }

    @Override
    public void writeMultiLineInfo(StringBuilder builder, String padding) {
        super.writeMultiLineInfo(builder, padding);
        builder.append(padding).append("Type: ").append(this.type).append(Constants.NEWLINE);
        builder.append(padding).append("SubType: ").append(this.subType).append(Constants.NEWLINE);
        writeAssetLine(builder, padding, "Previous", this.prevHash);
        writeAssetLine(builder, padding, "Next", this.nextHash);
        builder.append(padding).append("Waypoint Flags: ").append(Utils.toHexString(this.waypointFlags)).append(Constants.NEWLINE);
        this.color.writePrefixedInfoLine(builder, "Color", padding);
        builder.append(padding).append("Strength: ").append(this.strength).append(Constants.NEWLINE);
    }
}