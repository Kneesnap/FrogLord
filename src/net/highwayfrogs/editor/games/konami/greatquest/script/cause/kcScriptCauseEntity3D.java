package net.highwayfrogs.editor.games.konami.greatquest.script.cause;

import lombok.AllArgsConstructor;
import lombok.Getter;
import net.highwayfrogs.editor.games.konami.greatquest.script.kcScriptDisplaySettings;

import java.util.List;

/**
 * Caused when a kcCEntity3D enters or leaves the range of another.
 * Created by Kneesnap on 8/19/2023.
 */
public class kcScriptCauseEntity3D extends kcScriptCause {
    private kcScriptCauseEntity3DStatus status;
    private int hEntity;

    public kcScriptCauseEntity3D() {
        super(kcScriptCauseType.ENTITY_3D, 4);
    }

    @Override
    public void load(int subCauseType, List<Integer> extraValues) {
        this.status = kcScriptCauseEntity3DStatus.getStatus(subCauseType, false);
        this.hEntity = extraValues.get(0);
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
        output.add(this.hEntity);
        output.add(0);
        output.add(0);
        output.add(0);
    }

    @Override
    public void toString(StringBuilder builder, kcScriptDisplaySettings settings) {
        if (this.status.isLeadWithAttachedEntity()) {
            builder.append("When the attached entity ");
            builder.append(this.status.getDisplayAction());
            builder.append(' ');
            builder.append(kcScriptDisplaySettings.getHashDisplay(settings, this.hEntity, true));
        } else {
            builder.append("When ");
            builder.append(kcScriptDisplaySettings.getHashDisplay(settings, this.hEntity, true));
            builder.append(' ');
            builder.append(this.status.getDisplayAction());
        }
    }

    @Getter
    @AllArgsConstructor
    public enum kcScriptCauseEntity3DStatus {
        ATTACHED_ENTITY_ENTERS_TARGET(true, "enters the area surrounding"), // I don't know what the exact range is, but I think it's just the entity activation range.
        ATTACHED_ENTITY_LEAVES_TARGET(true, "leaves the area surrounding"),
        TARGET_ENTITY_ENTERS_WAYPOINT(false, "enters the attached waypoint entity's area"),
        TARGET_ENTITY_LEAVES_WAYPOINT(false, "leaves the attached waypoint entity's area");

        private final boolean leadWithAttachedEntity;
        private final String displayAction;

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