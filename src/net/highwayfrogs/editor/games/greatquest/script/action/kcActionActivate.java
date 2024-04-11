package net.highwayfrogs.editor.games.greatquest.script.action;

import lombok.Setter;
import net.highwayfrogs.editor.games.greatquest.script.interim.kcParamReader;
import net.highwayfrogs.editor.games.greatquest.script.interim.kcParamWriter;
import net.highwayfrogs.editor.games.greatquest.script.kcArgument;
import net.highwayfrogs.editor.games.greatquest.script.kcParam;
import net.highwayfrogs.editor.games.greatquest.script.kcParamType;

/**
 * Represents the 'ACTIVATE' kcAction command.
 * Created by Kneesnap on 8/24/2023.
 */
@Setter
public class kcActionActivate extends kcAction {
    private static final kcArgument[] ARGUMENTS = kcArgument.make(kcParamType.BOOLEAN, "newState");
    private boolean newState;

    public kcActionActivate() {
        super(kcActionID.ACTIVATE);
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