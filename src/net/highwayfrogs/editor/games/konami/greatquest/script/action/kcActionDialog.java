package net.highwayfrogs.editor.games.konami.greatquest.script.action;

import lombok.Getter;
import net.highwayfrogs.editor.games.konami.greatquest.GreatQuestHash;
import net.highwayfrogs.editor.games.konami.greatquest.GreatQuestUtils;
import net.highwayfrogs.editor.games.konami.greatquest.generic.kcCResourceGeneric;
import net.highwayfrogs.editor.games.konami.greatquest.script.interim.kcParamReader;
import net.highwayfrogs.editor.games.konami.greatquest.script.interim.kcParamWriter;
import net.highwayfrogs.editor.games.konami.greatquest.script.*;
import net.highwayfrogs.editor.utils.objects.OptionalArguments;

/**
 * Shows dialog to the player when run.
 * Created by Kneesnap on 10/31/2024.
 */
@Getter
public class kcActionDialog extends kcAction {
    private final GreatQuestHash<kcCResourceGeneric> dialogRef = new GreatQuestHash<>();

    private static final kcArgument[] ARGUMENTS = kcArgument.make(kcParamType.HASH_NULL_IS_ZERO, "dialogRes");


    public kcActionDialog(kcActionExecutor executor) {
        super(executor, kcActionID.DIALOG);
        this.dialogRef.setNullRepresentedAsZero();
    }

    @Override
    public kcArgument[] getArgumentTemplate(kcParam[] arguments) {
        return ARGUMENTS;
    }

    @Override
    public void load(kcParamReader reader) {
        setDialogHash(reader.next().getAsInteger());
    }

    @Override
    public void save(kcParamWriter writer) {
        writer.write(this.dialogRef.getHashNumber());
    }

    @Override
    protected void loadArguments(OptionalArguments arguments) {
        setDialogHash(GreatQuestUtils.getAsHash(arguments.useNext(), 0, this.dialogRef));
    }

    @Override
    protected void saveArguments(OptionalArguments arguments, kcScriptDisplaySettings settings) {
        this.dialogRef.applyGqsString(arguments.createNext(), settings);
    }

    @Override
    public String getEndOfLineComment() {
        if (this.dialogRef.getResource() != null) {
            return this.dialogRef.getResource().getAsString();
        } else {
            return super.getEndOfLineComment();
        }
    }

    /**
     * Resolves the hash of a dialog string and applies it to become the string shown when this action is executed.
     * @param newDialogHash the hash of the new dialog.
     */
    public void setDialogHash(int newDialogHash) {
        GreatQuestUtils.resolveResourceHash(kcCResourceGeneric.class, getChunkedFile(), this, this.dialogRef, newDialogHash, false);
    }
}
