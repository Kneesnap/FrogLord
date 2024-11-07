package net.highwayfrogs.editor.games.konami.greatquest.script.cause;

import lombok.AllArgsConstructor;
import lombok.Getter;
import net.highwayfrogs.editor.games.konami.greatquest.GreatQuestHash;
import net.highwayfrogs.editor.games.konami.greatquest.GreatQuestUtils;
import net.highwayfrogs.editor.games.konami.greatquest.chunks.kcCResourceEntityInst;
import net.highwayfrogs.editor.games.konami.greatquest.script.kcScript;
import net.highwayfrogs.editor.games.konami.greatquest.script.kcScriptDisplaySettings;
import net.highwayfrogs.editor.utils.objects.OptionalArguments;

import java.util.List;
import java.util.logging.Logger;

/**
 * Called when the specified entity comes within range of the kcCEntity3D the script is attached to.
 * This cause should only be used in scripts attached to waypoints.
 * Created by Kneesnap on 8/18/2023.
 */
public class kcScriptCauseWaypoint extends kcScriptCause {
    private kcScriptCauseWaypointStatus status;
    private final GreatQuestHash<kcCResourceEntityInst> otherEntityRef = new GreatQuestHash<>();

    public kcScriptCauseWaypoint(kcScript script) {
        super(script, kcScriptCauseType.WAYPOINT, 1, 2);
    }

    @Override
    public void load(int subCauseType, List<Integer> extraValues) {
        this.status = kcScriptCauseWaypointStatus.getStatus(subCauseType, false);
        setOtherEntityHash(extraValues.get(0));
    }

    @Override
    public void save(List<Integer> output) {
        output.add(this.status.ordinal());
        output.add(this.otherEntityRef.getHashNumber());
    }

    @Override
    protected void loadArguments(OptionalArguments arguments) {
        this.status = arguments.useNext().getAsEnumOrError(kcScriptCauseWaypointStatus.class);
        setOtherEntityHash(GreatQuestUtils.getAsHash(arguments.useNext(), -1));
    }

    @Override
    protected void saveArguments(OptionalArguments arguments, kcScriptDisplaySettings settings) {
        arguments.createNext().setAsEnum(this.status);
        this.otherEntityRef.applyGqsString(arguments.createNext(), settings);
    }

    @Override
    public void printWarnings(Logger logger) {
        super.printWarnings(logger);
        if (this.otherEntityRef.getResource() == null) // The other entity could be a kcCEntity, there is no type-restriction.
            printWarning(logger, "will never occur because other entity could not be found.");
    }

    @Override
    public void toString(StringBuilder builder, kcScriptDisplaySettings settings) {
        builder.append("When ");
        builder.append(this.otherEntityRef.getAsGqsString(settings));
        builder.append(' ');

        kcCResourceEntityInst targetEntity = getScriptEntity();
        if (targetEntity != null) {
            builder.append(this.status.getDisplayAction().replace("the attached waypoint entity", targetEntity.getName()));
        } else {
            builder.append(this.status.getDisplayAction());
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
    public enum kcScriptCauseWaypointStatus {
        ENTITY_ENTERS("enters the attached waypoint entity's area"),
        ENTITY_LEAVES("leaves the attached waypoint entity's area");

        private final String displayAction;

        /**
         * Gets the kcScriptCauseWaypointStatus corresponding to the provided value.
         * @param value     The value to lookup.
         * @param allowNull If null is allowed.
         * @return status
         */
        public static kcScriptCauseWaypointStatus getStatus(int value, boolean allowNull) {
            if (value < 0 || value >= values().length) {
                if (allowNull)
                    return null;

                throw new RuntimeException("Couldn't determine waypoint status from value " + value + ".");
            }

            return values()[value];
        }
    }
}