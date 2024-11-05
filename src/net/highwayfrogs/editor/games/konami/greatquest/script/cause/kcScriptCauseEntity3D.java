package net.highwayfrogs.editor.games.konami.greatquest.script.cause;

import lombok.AllArgsConstructor;
import lombok.Getter;
import net.highwayfrogs.editor.games.konami.greatquest.GreatQuestHash;
import net.highwayfrogs.editor.games.konami.greatquest.GreatQuestUtils;
import net.highwayfrogs.editor.games.konami.greatquest.chunks.kcCResourceEntityInst;
import net.highwayfrogs.editor.games.konami.greatquest.script.kcScript;
import net.highwayfrogs.editor.games.konami.greatquest.script.kcScriptDisplaySettings;
import net.highwayfrogs.editor.utils.StringUtils;
import net.highwayfrogs.editor.utils.objects.OptionalArguments;

import java.util.List;

/**
 * Caused when a kcCEntity3D enters or leaves the range of another.
 * Created by Kneesnap on 8/19/2023.
 */
public class kcScriptCauseEntity3D extends kcScriptCause {
    private kcScriptCauseEntity3DStatus status;
    private final GreatQuestHash<kcCResourceEntityInst> otherEntityRef = new GreatQuestHash<>();

    public kcScriptCauseEntity3D(kcScript script) {
        super(script, kcScriptCauseType.ENTITY_3D, 4, 2);
    }

    @Override
    public void load(int subCauseType, List<Integer> extraValues) {
        this.status = kcScriptCauseEntity3DStatus.getStatus(subCauseType, false);
        setOtherEntityHash(extraValues.get(0));
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
        output.add(this.otherEntityRef.getHashNumber());
        output.add(0);
        output.add(0);
        output.add(0);
    }

    @Override
    protected void loadArguments(OptionalArguments arguments) {
        this.status = arguments.useNext().getAsEnumOrError(kcScriptCauseEntity3DStatus.class);
        setOtherEntityHash(GreatQuestUtils.getAsHash(arguments.useNext(), -1));
    }

    @Override
    protected void saveArguments(OptionalArguments arguments, kcScriptDisplaySettings settings) {
        arguments.createNext().setAsEnum(this.status);
        this.otherEntityRef.applyGqsString(arguments.createNext(), settings);
    }

    @Override
    public void toString(StringBuilder builder, kcScriptDisplaySettings settings) {
        kcCResourceEntityInst targetEntity = getScriptEntity();
        if (this.status.isLeadWithAttachedEntity()) {
            builder.append("When ");
            if (targetEntity != null && !StringUtils.isNullOrEmpty(targetEntity.getName())) {
                builder.append(targetEntity.getName());
            } else {
                builder.append("the attached entity");
            }

            builder.append(' ');
            builder.append(this.status.getDisplayAction());
            builder.append(' ');
            builder.append(this.otherEntityRef.getAsGqsString(settings));
        } else {
            builder.append("When ");
            builder.append(this.otherEntityRef.getAsGqsString(settings));
            builder.append(' ');
            if (targetEntity != null && !StringUtils.isNullOrEmpty(targetEntity.getName())) {
                builder.append(this.status.getDisplayAction().replace("the attached waypoint entity", "the waypoint " + targetEntity.getName()));
            } else {
                builder.append(this.status.getDisplayAction());
            }
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
        ATTACHED_ENTITY_ENTERS_TARGET(true, "enters the area surrounding"), // I don't know what the exact range is, but I think it's just the entity activation range.
        ATTACHED_ENTITY_LEAVES_TARGET(true, "leaves the area surrounding"),
        TARGET_ENTERS_ATTACHED_WAYPOINT(false, "enters the attached waypoint entity's area"),
        TARGET_LEAVES_ATTACHED_WAYPOINT(false, "leaves the attached waypoint entity's area");

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