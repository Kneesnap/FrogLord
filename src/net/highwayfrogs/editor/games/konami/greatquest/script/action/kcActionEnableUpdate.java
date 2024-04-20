package net.highwayfrogs.editor.games.konami.greatquest.script.action;

import lombok.Getter;
import lombok.Setter;
import net.highwayfrogs.editor.games.konami.greatquest.GreatQuestChunkedFile;
import net.highwayfrogs.editor.games.konami.greatquest.script.interim.kcParamReader;
import net.highwayfrogs.editor.games.konami.greatquest.script.interim.kcParamWriter;
import net.highwayfrogs.editor.games.konami.greatquest.script.kcArgument;
import net.highwayfrogs.editor.games.konami.greatquest.script.kcParam;
import net.highwayfrogs.editor.games.konami.greatquest.script.kcParamType;

/**
 * Represents the 'ENABLE_UPDATE' kcAction.
 * Created by Kneesnap on 8/24/2023.
 */
@Getter
@Setter
public class kcActionEnableUpdate extends kcAction {
    private static final kcArgument[] ARGUMENTS = kcArgument.make(kcParamType.BOOLEAN, "newState");
    private boolean newState;

    public kcActionEnableUpdate(GreatQuestChunkedFile chunkedFile) {
        super(chunkedFile, kcActionID.ENABLE_UPDATE);
    }

    @Override
    public kcArgument[] getArgumentTemplate(kcParam[] arguments) {
        return ARGUMENTS;
    }

    @Override
    public void load(kcParamReader reader) {
        this.newState = reader.next().getAsBoolean();
    }

    @Override
    public void save(kcParamWriter writer) {
        writer.write(this.newState);
    }
}