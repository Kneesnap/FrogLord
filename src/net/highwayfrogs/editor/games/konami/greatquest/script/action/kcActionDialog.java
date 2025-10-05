package net.highwayfrogs.editor.games.konami.greatquest.script.action;

import lombok.Getter;
import net.highwayfrogs.editor.games.konami.greatquest.GreatQuestHash;
import net.highwayfrogs.editor.games.konami.greatquest.GreatQuestUtils;
import net.highwayfrogs.editor.games.konami.greatquest.generic.kcCResourceGeneric;
import net.highwayfrogs.editor.games.konami.greatquest.generic.kcCResourceGeneric.kcCResourceGenericType;
import net.highwayfrogs.editor.games.konami.greatquest.script.interim.kcParamReader;
import net.highwayfrogs.editor.games.konami.greatquest.script.interim.kcParamWriter;
import net.highwayfrogs.editor.games.konami.greatquest.script.*;
import net.highwayfrogs.editor.utils.logging.ILogger;
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
        setDialogHash(getLogger(), reader.next().getAsInteger());
    }

    @Override
    public void save(kcParamWriter writer) {
        writer.write(this.dialogRef.getHashNumber());
    }

    @Override
    protected void loadArguments(ILogger logger, OptionalArguments arguments) {
        resolveResource(logger, arguments.useNext(), kcCResourceGenericType.STRING_RESOURCE, this.dialogRef);
    }

    @Override
    protected void saveArguments(ILogger logger, OptionalArguments arguments, kcScriptDisplaySettings settings) {
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
    public void setDialogHash(ILogger logger, int newDialogHash) {
        GreatQuestUtils.resolveLevelResourceHash(logger, kcCResourceGenericType.STRING_RESOURCE, getChunkedFile(), this, this.dialogRef, newDialogHash, false);
    }
}
