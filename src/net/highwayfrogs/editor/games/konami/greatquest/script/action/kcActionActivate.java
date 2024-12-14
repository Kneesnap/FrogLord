package net.highwayfrogs.editor.games.konami.greatquest.script.action;

import net.highwayfrogs.editor.games.konami.greatquest.script.interim.kcParamReader;
import net.highwayfrogs.editor.games.konami.greatquest.script.interim.kcParamWriter;
import net.highwayfrogs.editor.games.konami.greatquest.script.*;
import net.highwayfrogs.editor.utils.objects.OptionalArguments;

/**
 * Represents the 'ACTIVATE' kcAction command.
 * Created by Kneesnap on 8/24/2023.
 */
public class kcActionActivate extends kcAction {
    private static final kcArgument[] ARGUMENTS = kcArgument.make(kcParamType.BOOLEAN, "makeActive");
    private boolean makeActive;

    public kcActionActivate(kcActionExecutor executor) {
        super(executor, kcActionID.ACTIVATE);
    }

    public kcActionActivate(kcActionExecutor executor, boolean makeActive) {
        this(executor);
        this.makeActive = makeActive;
    }

    @Override
    public kcArgument[] getArgumentTemplate(kcParam[] arguments) {
        return ARGUMENTS;
    }

    @Override
    public void load(kcParamReader reader) {
        this.makeActive = reader.next().getAsBoolean();
    }

    @Override
    public void save(kcParamWriter writer) {
        writer.write(this.makeActive);
    }

    @Override
    protected void loadArguments(OptionalArguments arguments) {
        this.makeActive = arguments.useNext().getAsBoolean();
    }

    @Override
    protected void saveArguments(OptionalArguments arguments, kcScriptDisplaySettings settings) {
        arguments.createNext().setAsBoolean(this.makeActive);
    }
}