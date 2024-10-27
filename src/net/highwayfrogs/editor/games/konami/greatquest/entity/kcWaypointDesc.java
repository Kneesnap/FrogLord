package net.highwayfrogs.editor.games.konami.greatquest.entity;

import lombok.Getter;
import lombok.Setter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.konami.greatquest.GreatQuestHash;
import net.highwayfrogs.editor.games.konami.greatquest.GreatQuestUtils;
import net.highwayfrogs.editor.games.konami.greatquest.chunks.kcCResourceEntityInst;
import net.highwayfrogs.editor.games.konami.greatquest.generic.kcCResourceGeneric;
import net.highwayfrogs.editor.games.konami.greatquest.generic.kcCResourceGeneric.kcCResourceGenericType;
import net.highwayfrogs.editor.games.konami.greatquest.kcClassID;
import net.highwayfrogs.editor.games.konami.greatquest.map.kcColor4;
import net.highwayfrogs.editor.utils.NumberUtils;

/**
 * Represents the kcWaypointDesc struct.
 * Loaded by kcCWaypoint::Init
 * Created by Kneesnap on 8/21/2023.
 */
@Getter
@Setter
public class kcWaypointDesc extends kcEntity3DDesc {
    private final GreatQuestHash<kcCResourceGeneric> parentHash;
    private short type; // TODO: I think this is always 0. Extensions of kcCWaypoint might have other types, but in terms of what is stored in the game data, I think it's just this.
    private short subType; // TODO: FOR game data, it's either 0 or 1. (Replace with enum later?)
    private final GreatQuestHash<kcCResourceEntityInst> previousWaypointEntityRef; // NOTE: -1 when not found, kcCWaypoint::RenderDebug, kcCWaypoint::__ct, MonsterClass::Do_Guard
    private final GreatQuestHash<kcCResourceEntityInst> nextWaypointEntityRef; // NOTE: -1 when not found, kcCWaypoint::RenderDebug, kcCWaypoint::__ct, MonsterClass::Do_Guard
    private int waypointFlags; // TODO: What are these? Editor?
    private final kcColor4 color = new kcColor4(); // When this is type 0 subType 1, THIS IS NOT ACTUALLY A COLOR. TODO: REAL EDITOR.
    private float strength;
    private static final int PADDING_VALUES = 7;
    private static final String NAME_SUFFIX = "WayptDesc";

    public kcWaypointDesc(kcCResourceGeneric resource) {
        super(resource);
        this.parentHash = new GreatQuestHash<>(resource);
        this.previousWaypointEntityRef = new GreatQuestHash<>();
        this.nextWaypointEntityRef = new GreatQuestHash<>();
        GreatQuestUtils.applySelfNameSuffixAndToFutureNameChanges(resource, NAME_SUFFIX);
    }

    @Override
    public void load(DataReader reader) {
        super.load(reader);
        this.type = reader.readShort();
        this.subType = reader.readShort();
        int prevEntityHash = reader.readInt();
        int nextEntityHash = reader.readInt();
        this.waypointFlags = reader.readInt();
        this.color.load(reader);
        this.strength = reader.readFloat();
        reader.skipBytesRequireEmpty(PADDING_VALUES * Constants.INTEGER_SIZE);

        // Resolve entities. Keep in mind that there are a good number of entities which were removed/no longer resolve.
        GreatQuestUtils.resolveResourceHash(kcCResourceEntityInst.class, this, this.previousWaypointEntityRef, prevEntityHash, false);
        GreatQuestUtils.resolveResourceHash(kcCResourceEntityInst.class, this, this.nextWaypointEntityRef, nextEntityHash, false);
    }

    @Override
    public void saveData(DataWriter writer) {
        super.saveData(writer);
        writer.writeShort(this.type);
        writer.writeShort(this.subType);
        writer.writeInt(this.previousWaypointEntityRef.getHashNumber());
        writer.writeInt(this.nextWaypointEntityRef.getHashNumber());
        writer.writeInt(this.waypointFlags);
        this.color.save(writer);
        writer.writeFloat(this.strength);
        writer.writeNull(PADDING_VALUES * Constants.INTEGER_SIZE);
    }

    @Override
    public void writeMultiLineInfo(StringBuilder builder, String padding) {
        super.writeMultiLineInfo(builder, padding);
        builder.append(padding).append("Type: ").append(this.type).append(Constants.NEWLINE);
        builder.append(padding).append("SubType: ").append(this.subType).append(Constants.NEWLINE);
        writeAssetLine(builder, padding, "Previous", this.previousWaypointEntityRef);
        writeAssetLine(builder, padding, "Next", this.nextWaypointEntityRef);
        builder.append(padding).append("Waypoint Flags: ").append(NumberUtils.toHexString(this.waypointFlags)).append(Constants.NEWLINE);
        this.color.writePrefixedInfoLine(builder, "Color", padding);
        builder.append(padding).append("Strength: ").append(this.strength).append(Constants.NEWLINE);
    }

    @Override
    public int getTargetClassID() {
        return kcClassID.WAYPOINT.getClassId();
    }

    @Override
    public kcCResourceGenericType getResourceType() {
        return kcCResourceGenericType.WAYPOINT_DESCRIPTION;
    }
}