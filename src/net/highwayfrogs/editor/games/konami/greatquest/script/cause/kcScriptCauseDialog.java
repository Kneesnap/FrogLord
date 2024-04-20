package net.highwayfrogs.editor.games.konami.greatquest.script.cause;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import net.highwayfrogs.editor.games.konami.greatquest.GreatQuestInstance;
import net.highwayfrogs.editor.games.konami.greatquest.script.kcScriptDisplaySettings;

import java.util.List;

/**
 * Represents cause information for a dialog.
 * Created by Kneesnap on 8/16/2023.
 */
@Getter
@Setter
public class kcScriptCauseDialog extends kcScriptCause {
    private kcScriptDialogStage stage;
    private int dialogHash;

    public kcScriptCauseDialog(GreatQuestInstance gameInstance) {
        super(gameInstance, kcScriptCauseType.DIALOG, 1);
    }

    @Override
    public void load(int subCauseType, List<Integer> extraValues) {
        this.stage = kcScriptDialogStage.getStage(subCauseType, false);
        this.dialogHash = extraValues.get(0);
    }

    @Override
    public void save(List<Integer> output) {
        output.add(this.stage.ordinal());
        output.add(this.dialogHash);
    }

    @Override
    public void toString(StringBuilder builder, kcScriptDisplaySettings settings) {
        builder.append("When dialog definition ");
        builder.append(kcScriptDisplaySettings.getHashDisplay(settings, this.dialogHash, true));

        switch (this.stage) {
            case BEGIN:
                builder.append(" begins");
                break;
            case ADVANCE:
                builder.append(" is advanced");
                break;
            case END:
                builder.append(" is completed");
                break;
            default:
                throw new RuntimeException("Unrecognized kcScriptDialogStage " + this.stage + ".");
        }
    }

    @Getter
    @AllArgsConstructor
    public enum kcScriptDialogStage {
        BEGIN, ADVANCE, END;

        /**
         * Gets the kcScriptDialogStage corresponding to the provided value.
         * @param value     The value to lookup.
         * @param allowNull If null is allowed.
         * @return instructionType
         */
        public static kcScriptDialogStage getStage(int value, boolean allowNull) {
            if (value < 0 || value >= values().length) {
                if (allowNull)
                    return null;

                throw new RuntimeException("Couldn't determine dialog stage from value " + value + ".");
            }

            return values()[value];
        }
    }
}