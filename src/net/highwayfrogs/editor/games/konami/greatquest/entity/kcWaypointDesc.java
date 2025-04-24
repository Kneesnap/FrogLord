package net.highwayfrogs.editor.games.konami.greatquest.entity;

import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.utils.data.reader.DataReader;
import net.highwayfrogs.editor.utils.data.writer.DataWriter;
import net.highwayfrogs.editor.games.konami.greatquest.GreatQuestHash;
import net.highwayfrogs.editor.games.konami.greatquest.GreatQuestUtils;
import net.highwayfrogs.editor.games.konami.greatquest.chunks.kcCResourceEntityInst;
import net.highwayfrogs.editor.games.konami.greatquest.generic.kcCResourceGeneric;
import net.highwayfrogs.editor.games.konami.greatquest.generic.kcCResourceGeneric.kcCResourceGenericType;
import net.highwayfrogs.editor.games.konami.greatquest.math.kcVector3;
import net.highwayfrogs.editor.games.konami.greatquest.script.kcScriptDisplaySettings;
import net.highwayfrogs.editor.system.Config;
import net.highwayfrogs.editor.system.Config.ConfigValueNode;
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
    @NonNull private kcWaypointType type = kcWaypointType.BOUNDING_SPHERE;
    private final GreatQuestHash<kcCResourceEntityInst> previousWaypointEntityRef; // NOTE: -1 when not found, kcCWaypoint::RenderDebug, kcCWaypoint::__ct, MonsterClass::Do_Guard
    private final GreatQuestHash<kcCResourceEntityInst> nextWaypointEntityRef; // NOTE: -1 when not found, kcCWaypoint::RenderDebug, kcCWaypoint::__ct, MonsterClass::Do_Guard
    private final kcVector3 boundingBoxDimensions = new kcVector3(); // The game calls this a "color", both in terms of using kcColor4 and through the field name. However, this actually appears to be used as the bounding box.
    private float strength; // Controls how strongly the water current pushes swimming players in CFrogCtl::AutoTarget.
    private static final int PADDING_VALUES = 7;
    private static final String NAME_SUFFIX = "WayptDesc";

    public static final float MINIMUM_BOUNDING_BOX_SIZE = .05F; // kcCWaypoint::UpdateRectangularParameters
    public static final float MAXIMUM_BOUNDING_BOX_SIZE = 64F; // kcCWaypoint::UpdateRectangularParameters

    public kcWaypointDesc(kcCResourceGeneric resource) {
        super(resource, kcEntityDescType.WAYPOINT);
        this.parentHash = new GreatQuestHash<>(resource);
        this.previousWaypointEntityRef = new GreatQuestHash<>();
        this.nextWaypointEntityRef = new GreatQuestHash<>();
        GreatQuestUtils.applySelfNameSuffixAndToFutureNameChanges(resource, NAME_SUFFIX);
    }

    @Override
    public void load(DataReader reader) {
        super.load(reader);
        int type = reader.readUnsignedShortAsInt();
        int subType = reader.readUnsignedShortAsInt();
        this.type = kcWaypointType.getWaypointType(type, subType);
        int prevEntityHash = reader.readInt();
        int nextEntityHash = reader.readInt();
        int waypointFlags = reader.readInt(); // Always zero.
        this.boundingBoxDimensions.load(reader);
        float colorAlpha = reader.readFloat(); // Always zero.
        this.strength = reader.readFloat();
        reader.skipBytesRequireEmpty(PADDING_VALUES * Constants.INTEGER_SIZE);

        // Resolve entities. Keep in mind that there are a good number of entities which were removed/no longer resolve.
        GreatQuestUtils.resolveResourceHash(kcCResourceEntityInst.class, this, this.previousWaypointEntityRef, prevEntityHash, false);
        GreatQuestUtils.resolveResourceHash(kcCResourceEntityInst.class, this, this.nextWaypointEntityRef, nextEntityHash, false);
        if (waypointFlags != 0)
            throw new RuntimeException("Found a kcWaypointDesc which had non-zero waypointFlags! (Had: " + NumberUtils.toHexString(waypointFlags) + ")");
        if (colorAlpha != 0)
            throw new RuntimeException("Found a kcWaypointDesc which had non-zero colorAlpha! (Had: " + colorAlpha + ")");
    }

    @Override
    public void saveData(DataWriter writer) {
        super.saveData(writer);
        writer.writeUnsignedShort(this.type.getWaypointType());
        writer.writeUnsignedShort(this.type.getWaypointSubType());
        writer.writeInt(this.previousWaypointEntityRef.getHashNumber());
        writer.writeInt(this.nextWaypointEntityRef.getHashNumber());
        writer.writeInt(0); // waypointFlags
        this.boundingBoxDimensions.save(writer);
        writer.writeFloat(0); // colorAlpha
        writer.writeFloat(this.strength);
        writer.writeNull(PADDING_VALUES * Constants.INTEGER_SIZE);
    }

    @Override
    public void writeMultiLineInfo(StringBuilder builder, String padding) {
        super.writeMultiLineInfo(builder, padding);
        builder.append(padding).append("Type: ").append(this.type).append(Constants.NEWLINE);
        writeAssetLine(builder, padding, "Previous", this.previousWaypointEntityRef);
        writeAssetLine(builder, padding, "Next", this.nextWaypointEntityRef);
        if (this.type == kcWaypointType.BOUNDING_BOX || this.boundingBoxDimensions.getX() != 0 || this.boundingBoxDimensions.getY() != 0 || this.boundingBoxDimensions.getZ() != 0)
            this.boundingBoxDimensions.writePrefixedInfoLine(builder, "Bounding Box Dimensions", padding);
        if (this.type == kcWaypointType.APPLY_WATER_CURRENT || this.strength != 0)
            builder.append(padding).append("Strength: ").append(this.strength).append(Constants.NEWLINE);
    }

    private static final String CONFIG_KEY_TYPE = "waypointType";
    private static final String CONFIG_KEY_PREV_WAYPOINT = "prevWaypoint";
    private static final String CONFIG_KEY_NEXT_WAYPOINT = "nextWaypoint";
    private static final String CONFIG_KEY_BOUNDING_BOX_DIMENSIONS = "boundingBoxDimensions";
    private static final String CONFIG_KEY_STRENGTH = "strength";

    @Override
    public void fromConfig(Config input) {
        super.fromConfig(input);
        this.type = input.getKeyValueNodeOrError(CONFIG_KEY_TYPE).getAsEnumOrError(kcWaypointType.class);
        resolve(input.getKeyValueNodeOrError(CONFIG_KEY_PREV_WAYPOINT), kcCResourceEntityInst.class, this.previousWaypointEntityRef);
        resolve(input.getKeyValueNodeOrError(CONFIG_KEY_NEXT_WAYPOINT), kcCResourceEntityInst.class, this.nextWaypointEntityRef);

        // Read the bounding box data.
        ConfigValueNode boundingBoxNode = (this.type == kcWaypointType.BOUNDING_BOX)
                ? input.getKeyValueNodeOrError(CONFIG_KEY_BOUNDING_BOX_DIMENSIONS)
                : input.getOptionalKeyValueNode(CONFIG_KEY_BOUNDING_BOX_DIMENSIONS);
        if (boundingBoxNode != null)
            this.boundingBoxDimensions.parse(boundingBoxNode.getAsString());

        // Read the strength data.
        ConfigValueNode strengthNode = (this.type == kcWaypointType.APPLY_WATER_CURRENT)
                ? input.getKeyValueNodeOrError(CONFIG_KEY_STRENGTH)
                : input.getOptionalKeyValueNode(CONFIG_KEY_STRENGTH);
        if (strengthNode != null)
            this.strength = strengthNode.getAsFloat();
    }

    @Override
    public void toConfig(Config output, kcScriptDisplaySettings settings) {
        super.toConfig(output, settings);
        output.getOrCreateKeyValueNode(CONFIG_KEY_TYPE).setAsEnum(this.type);
        output.getOrCreateKeyValueNode(CONFIG_KEY_PREV_WAYPOINT).setAsString(this.previousWaypointEntityRef.getAsGqsString(settings));
        output.getOrCreateKeyValueNode(CONFIG_KEY_NEXT_WAYPOINT).setAsString(this.nextWaypointEntityRef.getAsGqsString(settings));
        if (this.type == kcWaypointType.BOUNDING_BOX || this.boundingBoxDimensions.getX() != 0 || this.boundingBoxDimensions.getY() != 0 || this.boundingBoxDimensions.getZ() != 0)
            output.getOrCreateKeyValueNode(CONFIG_KEY_BOUNDING_BOX_DIMENSIONS).setAsString(this.boundingBoxDimensions.toParseableString());
        if (this.type == kcWaypointType.APPLY_WATER_CURRENT || this.strength != 0)
            output.getOrCreateKeyValueNode(CONFIG_KEY_STRENGTH).setAsFloat(this.strength);
    }

    @Override
    public kcCResourceGenericType getResourceType() {
        return kcCResourceGenericType.WAYPOINT_DESCRIPTION;
    }

    @Getter
    @RequiredArgsConstructor
    public enum kcWaypointType {
        BOUNDING_SPHERE(0, 0), // kcCWaypoint::IntersectsExpanded, kcCWaypoint::Intersects, kcCWaypoint::RenderDebug. This is the default.
        BOUNDING_BOX(0, 1), // kcCWaypoint::IntersectsExpanded, kcCWaypoint::Intersects, kcCWaypoint::RenderDebug, kcCWaypoint::UpdateRectangularParameters
        APPLY_WATER_CURRENT(1, 0); // CFrogCtl::AutoTarget Only appears in Rolling Rapids Creek. This forces the player in the direction of the water animation while they are in water/swimming.

        private final int waypointType;
        private final int waypointSubType;

        /**
         * Gets the kcWaypointType based on the type/subtype.
         * @param waypointType the waypoint type
         * @param subType the waypoint subtype.
         * @return waypointType
         */
        public static kcWaypointType getWaypointType(int waypointType, int subType) {
            for (int i = 0; i < values().length; i++) {
                kcWaypointType type = values()[i];
                if (type.getWaypointType() == waypointType && type.getWaypointSubType() == subType)
                    return type;
            }

            throw new RuntimeException("Could not find kcWaypointType for waypointType=" + waypointType + ", subType=" + subType);
        }
    }
}