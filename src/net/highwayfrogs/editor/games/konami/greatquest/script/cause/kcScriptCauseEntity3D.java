package net.highwayfrogs.editor.games.konami.greatquest.script.cause;

import lombok.AllArgsConstructor;
import lombok.Getter;
import net.highwayfrogs.editor.games.konami.greatquest.GreatQuestHash;
import net.highwayfrogs.editor.games.konami.greatquest.GreatQuestUtils;
import net.highwayfrogs.editor.games.konami.greatquest.chunks.kcCResourceEntityInst;
import net.highwayfrogs.editor.games.konami.greatquest.entity.kcEntityInheritanceGroup;
import net.highwayfrogs.editor.games.konami.greatquest.script.kcScript;
import net.highwayfrogs.editor.games.konami.greatquest.script.kcScriptDisplaySettings;
import net.highwayfrogs.editor.utils.StringUtils;
import net.highwayfrogs.editor.utils.objects.OptionalArguments;

import java.util.List;
import java.util.logging.Logger;

/**
 * Caused when a kcCEntity3D enters or leaves the range of another.
 * Created by Kneesnap on 8/19/2023.
 */
public class kcScriptCauseEntity3D extends kcScriptCause {
    private kcScriptCauseEntity3DStatus status = kcScriptCauseEntity3DStatus.ENTERS_TARGET_WAYPOINT_AREA;
    private final GreatQuestHash<kcCResourceEntityInst> otherEntityRef = new GreatQuestHash<>();

    public kcScriptCauseEntity3D(kcScript script) {
        super(script, kcScriptCauseType.ENTITY_3D, 4, 1);
    }

    @Override
    public int getGqsArgumentCount() {
        return super.getGqsArgumentCount() + (this.status != null && this.status.hasOtherEntityAsParam() ? 1 : 0);
    }

    @Override
    public void load(int subCauseType, List<Integer> extraValues) {
        this.status = kcScriptCauseEntity3DStatus.getStatus(subCauseType, false);
        if (this.status.hasOtherEntityAsParam()) {
            setOtherEntityHash(extraValues.get(0));
        } else if (extraValues.get(0) != 0) {
            throw new RuntimeException("Expected extra value 0 (waypoint entity) to be zero for kcScriptCauseEntity3D/" + this.status + ". (Was: " + extraValues.get(0) + ")");
        }

        if (extraValues.get(1) != 0)
            throw new RuntimeException("Expected extra value 1 to be zero for kcScriptCauseEntity3D. (Was: " + extraValues.get(1) + ")");
        if (extraValues.get(2) != 0)
            throw new RuntimeException("Expected extra value 2 to be zero for kcScriptCauseEntity3D. (Was: " + extraValues.get(2) + ")");
        if (extraValues.get(3) != 0)
            throw new RuntimeException("Expected extra value 3 to be zero for kcScriptCauseEntity3D. (Was: " + extraValues.get(3) + ")");
    }

    @Override
    public void save(List<Integer> output) {
        output.add(this.status.ordinal());
        output.add(this.status.hasOtherEntityAsParam() ? this.otherEntityRef.getHashNumber() : 0);
        output.add(0);
        output.add(0);
        output.add(0);
    }

    @Override
    protected void loadArguments(OptionalArguments arguments) {
        this.status = arguments.useNext().getAsEnumOrError(kcScriptCauseEntity3DStatus.class);
        if (this.status.hasOtherEntityAsParam())
            setOtherEntityHash(GreatQuestUtils.getAsHash(arguments.useNext(), -1));
    }

    @Override
    protected void saveArguments(OptionalArguments arguments, kcScriptDisplaySettings settings) {
        arguments.createNext().setAsEnum(this.status);
        if (this.status.hasOtherEntityAsParam())
            this.otherEntityRef.applyGqsString(arguments.createNext(), settings);
    }

    @Override
    public void printWarnings(Logger logger) {
        super.printWarnings(logger);
        if (this.status.hasOtherEntityAsParam())
            this.status.getOtherEntityGroup().logEntityTypeWarnings(logger, this, this.otherEntityRef, this.status.name());
    }

    @Override
    public void toString(StringBuilder builder, kcScriptDisplaySettings settings) {
        kcCResourceEntityInst scriptEntity = getScriptEntity();

        builder.append("When ");
        if (scriptEntity != null && !StringUtils.isNullOrEmpty(scriptEntity.getName())) {
            builder.append(scriptEntity.getName());
        } else {
            builder.append("the script entity");
        }

        builder.append(' ');
        builder.append(this.status.getDisplayAction());
        if (this.status.hasOtherEntityAsParam()) {
            builder.append(' ');
            builder.append(this.otherEntityRef.getAsGqsString(settings));
        }
    }

    /**
     * Changes the hash of the referenced entity resource.
     * @param otherEntityHash the hash to apply
     */
    public void setOtherEntityHash(int otherEntityHash) {
        GreatQuestUtils.resolveResourceHash(kcCResourceEntityInst.class, getChunkFile(), this, this.otherEntityRef, otherEntityHash, true);
    }

    @Getter
    @AllArgsConstructor
    public enum kcScriptCauseEntity3DStatus {
        ENTERS_WAYPOINT_AREA("enters the area surrounding", kcEntityInheritanceGroup.WAYPOINT), // sSendWaypointStatus - I don't know what the exact range is, but I think it's just the entity activation range.
        LEAVES_WAYPOINT_AREA("leaves the area surrounding", kcEntityInheritanceGroup.WAYPOINT), // sSendWaypointStatus
        // The following appear to be used for pathfinding, as this will only fire if the waypoint to trigger this is also the entity's target.
        ENTERS_TARGET_WAYPOINT_AREA("enters the area of its target entity (if that target is a waypoint)", null), // kcCEntity3D::Notify() where iid = kcCWaypointMgr
        LEAVES_TARGET_WAYPOINT_AREA("leaves the area of its target entity (if that target is a waypoint)", null); // kcCEntity3D::Notify() where iid = kcCWaypointMgr

        private final String displayAction;
        private final kcEntityInheritanceGroup otherEntityGroup;

        /**
         * Returns if the status has an entity parameter.
         */
        public boolean hasOtherEntityAsParam() {
            return this.otherEntityGroup != null;
        }

        /**
         * Gets the kcScriptCauseEntity3DStatus corresponding to the provided value.
         * @param value     The value to lookup.
         * @param allowNull If null is allowed.
         * @return status
         */
        public static kcScriptCauseEntity3DStatus getStatus(int value, boolean allowNull) {
            if (value < 0 || value >= values().length) {
                if (allowNull)
                    return null;

                throw new RuntimeException("Couldn't determine Entity3D status from value " + value + ".");
            }

            return values()[value];
        }
    }
}