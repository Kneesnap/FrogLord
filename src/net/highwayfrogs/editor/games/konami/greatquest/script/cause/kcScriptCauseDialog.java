package net.highwayfrogs.editor.games.konami.greatquest.script.cause;

import lombok.AllArgsConstructor;
import lombok.Getter;
import net.highwayfrogs.editor.games.konami.greatquest.GreatQuestHash;
import net.highwayfrogs.editor.games.konami.greatquest.GreatQuestUtils;
import net.highwayfrogs.editor.games.konami.greatquest.generic.kcCResourceGeneric;
import net.highwayfrogs.editor.games.konami.greatquest.script.action.kcActionDialog;
import net.highwayfrogs.editor.games.konami.greatquest.script.action.kcActionID;
import net.highwayfrogs.editor.games.konami.greatquest.script.kcScript;
import net.highwayfrogs.editor.games.konami.greatquest.script.kcScriptDisplaySettings;
import net.highwayfrogs.editor.games.konami.greatquest.script.kcScriptValidationData;
import net.highwayfrogs.editor.utils.logging.ILogger;
import net.highwayfrogs.editor.utils.objects.OptionalArguments;

import java.util.List;

/**
 * Represents cause information for a dialog.
 * Created by Kneesnap on 8/16/2023.
 */
@Getter
public class kcScriptCauseDialog extends kcScriptCause {
    private kcScriptDialogStage stage;
    private final GreatQuestHash<kcCResourceGeneric> dialogRef;

    public kcScriptCauseDialog(kcScript script) {
        super(script, kcScriptCauseType.DIALOG, 1, 2);
        this.dialogRef = new GreatQuestHash<>();
        this.dialogRef.setNullRepresentedAsZero();
    }

    @Override
    public void load(int subCauseType, List<Integer> extraValues) {
        this.stage = kcScriptDialogStage.getStage(subCauseType, false);
        setDialogHash(extraValues.get(0));
    }

    @Override
    public void save(List<Integer> output) {
        output.add(this.stage.ordinal());
        output.add(this.dialogRef.getHashNumber());
    }

    @Override
    protected void loadArguments(OptionalArguments arguments) {
        this.stage = arguments.useNext().getAsEnum(kcScriptDialogStage.class);
        setDialogHash(GreatQuestUtils.getAsHash(arguments.useNext(), 0, this.dialogRef));
    }

    @Override
    protected void saveArguments(OptionalArguments arguments, kcScriptDisplaySettings settings) {
        arguments.createNext().setAsEnum(this.stage);
        this.dialogRef.applyGqsString(arguments.createNext(), settings);
    }

    @Override
    public void printWarnings(ILogger logger) {
        super.printWarnings(logger);
        if (this.stage == kcScriptDialogStage.END)
            printWarning(logger, "executes on dialog stage '" + this.stage + "', which is not supported by the game.");
        if (this.dialogRef.getResource() == null)
            printWarning(logger, "will never occur because FrogLord cannot resolve the dialog string reference '" + this.dialogRef.getAsGqsString(null) + "'.");
    }

    @Override
    public void printAdvancedWarnings(kcScriptValidationData data) {
        super.printAdvancedWarnings(data);
        if (!data.anyActionsMatch(kcActionID.DIALOG, this::isExpectedDialogShown)) {
            printWarning(data.getLogger(), data.getEntityName() + " never shows " + this.dialogRef.getAsString() + ".");
        } else if (!data.anyActionsMatch(kcActionID.DIALOG, (kcActionDialog action) -> isExpectedDialogShown(action) && !kcScriptCause.isEntityTerminated(action))) {
            printWarning(data.getLogger(), data.getEntityName() + " will be terminated before '" + this.dialogRef.getAsString() + "' is advanced.");
        }
    }

    private boolean isExpectedDialogShown(kcActionDialog action) {
        return action != null && action.getDialogRef().equals(this.dialogRef);
    }

    @Override
    public int hashCode() {
        return super.hashCode() ^ (this.stage.ordinal() << 24) ^ this.dialogRef.getHashNumber();
    }

    @Override
    public boolean equals(Object obj) {
        return super.equals(obj) && ((kcScriptCauseDialog) obj).getStage() == this.stage
                && ((kcScriptCauseDialog) obj).getDialogRef().getHashNumber() == this.dialogRef.getHashNumber();
    }

    @Override
    public void toString(StringBuilder builder, kcScriptDisplaySettings settings) {
        builder.append("When dialog definition ");
        builder.append(this.dialogRef.getAsGqsString(settings));

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

    /**
     * Changes the hash of the referenced dialog resource.
     * @param dialogHash the hash to apply
     */
    public void setDialogHash(int dialogHash) {
        GreatQuestUtils.resolveResourceHash(kcCResourceGeneric.class, getChunkFile(), this, this.dialogRef, dialogHash, true);
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