package net.highwayfrogs.editor.games.konami.greatquest.script.cause;

import lombok.AllArgsConstructor;
import lombok.Getter;
import net.highwayfrogs.editor.games.konami.greatquest.GreatQuestInstance;
import net.highwayfrogs.editor.games.konami.greatquest.script.kcScriptDisplaySettings;

import java.util.List;

/**
 * Called when the specified entity comes within range of the kcCEntity3D the script is attached to.
 * This cause should only be used in scripts attached to waypoints.
 * Created by Kneesnap on 8/18/2023.
 */
public class kcScriptCauseWaypoint extends kcScriptCause {
    private kcScriptCauseWaypointStatus status;
    private int hEntity;

    public kcScriptCauseWaypoint(GreatQuestInstance gameInstance) {
        super(gameInstance, kcScriptCauseType.WAYPOINT, 1);
    }

    @Override
    public void load(int subCauseType, List<Integer> extraValues) {
        this.status = kcScriptCauseWaypointStatus.getStatus(subCauseType, false);
        this.hEntity = extraValues.get(0);
    }

    @Override
    public void save(List<Integer> output) {
        output.add(this.status.ordinal());
        output.add(this.hEntity);
    }

    @Override
    public void toString(StringBuilder builder, kcScriptDisplaySettings settings) {
        builder.append("When ");
        builder.append(kcScriptDisplaySettings.getHashDisplay(settings, this.hEntity, true));
        builder.append(' ');
        builder.append(this.status.getDisplayAction());
    }

    @Getter
    @AllArgsConstructor
    public enum kcScriptCauseWaypointStatus {
        ENTITY_ENTERS_WAYPOINT("enters the attached waypoint entity's area"),
        ENTITY_LEAVES_WAYPOINT("leaves the attached waypoint entity's area");

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