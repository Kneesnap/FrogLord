package net.highwayfrogs.editor.games.konami.greatquest.script.cause;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.highwayfrogs.editor.games.konami.greatquest.script.kcScript;
import net.highwayfrogs.editor.games.konami.greatquest.script.kcScriptDisplaySettings;
import net.highwayfrogs.editor.utils.logging.ILogger;
import net.highwayfrogs.editor.utils.objects.OptionalArguments;

import java.util.List;

/**
 * A cause relating to levels.
 * Triggers: EvLevelBegin, EvLevelEnd
 * Created by Kneesnap on 8/16/2023.
 */
@Getter
public class kcScriptCauseLevel extends kcScriptCause {
    private kcScriptCauseLevelSubType subType = kcScriptCauseLevelSubType.BEGIN;

    public kcScriptCauseLevel(kcScript script) {
        super(script, kcScriptCauseType.LEVEL, 0, 1);
    }

    @Override
    public void load(int subCauseType, List<Integer> extraValues) {
        this.subType = kcScriptCauseLevelSubType.getSubType(subCauseType, false);
    }

    @Override
    public void save(List<Integer> output) {
        output.add(this.subType.ordinal());
    }

    @Override
    protected void loadArguments(OptionalArguments arguments) {
        this.subType = arguments.useNext().getAsEnumOrError(kcScriptCauseLevelSubType.class);
    }

    @Override
    protected void saveArguments(OptionalArguments arguments, kcScriptDisplaySettings settings) {
        arguments.createNext().setAsEnum(this.subType);
    }

    @Override
    public void printWarnings(ILogger logger) {
        super.printWarnings(logger);
        if (this.subType == kcScriptCauseLevelSubType.END)
            printWarning(logger, "uses kcScriptCauseLevelSubType " + this.subType + ", which is not properly supported by the game.");
    }

    @Override
    public int hashCode() {
        return super.hashCode() ^ (this.subType.ordinal() << 24);
    }

    @Override
    public boolean equals(Object obj) {
        return super.equals(obj) && ((kcScriptCauseLevel) obj).getSubType() == this.subType;
    }

    @Override
    public void toString(StringBuilder builder, kcScriptDisplaySettings settings) {
        builder.append(this.subType.getDescription());
    }

    @Getter
    @RequiredArgsConstructor
    public enum kcScriptCauseLevelSubType {
        BEGIN("When the level begins"),
        END("When the level is ended");

        private final String description;

        /**
         * Gets the kcScriptCauseLevelSubType corresponding to the provided value.
         * @param value     The value to lookup.
         * @param allowNull If null is allowed.
         * @return instructionType
         */
        public static kcScriptCauseLevelSubType getSubType(int value, boolean allowNull) {
            if (value < 0 || value >= values().length) {
                if (allowNull)
                    return null;

                throw new RuntimeException("Couldn't determine kcScriptCauseLevelSubType from value " + value + ".");
            }

            return values()[value];
        }
    }
}