package net.highwayfrogs.editor.games.konami.greatquest.script.action;

import lombok.Getter;
import net.highwayfrogs.editor.games.konami.greatquest.script.interim.kcParamReader;
import net.highwayfrogs.editor.games.konami.greatquest.script.interim.kcParamWriter;
import net.highwayfrogs.editor.games.konami.greatquest.script.*;
import net.highwayfrogs.editor.utils.objects.OptionalArguments;

/**
 * Represents the 'ENABLE_UPDATE' kcAction.
 * Created by Kneesnap on 8/24/2023.
 */
@Getter
public class kcActionEnableUpdate extends kcAction {
    private static final kcArgument[] ARGUMENTS = kcArgument.make(kcParamType.BOOLEAN, "shouldEnable");
    private boolean shouldEnable;

    public kcActionEnableUpdate(kcActionExecutor executor) {
        super(executor, kcActionID.ENABLE_UPDATE);
    }

    public kcActionEnableUpdate(kcActionExecutor executor, boolean shouldEnable) {
        this(executor);
        this.shouldEnable = shouldEnable;
    }

    @Override
    public kcArgument[] getArgumentTemplate(kcParam[] arguments) {
        return ARGUMENTS;
    }

    @Override
    public void load(kcParamReader reader) {
        this.shouldEnable = reader.next().getAsBoolean();
    }

    @Override
    public void save(kcParamWriter writer) {
        writer.write(this.shouldEnable);
    }

    @Override
    protected void loadArguments(OptionalArguments arguments) {
        this.shouldEnable = arguments.useNext().getAsBoolean();
    }

    @Override
    protected void saveArguments(OptionalArguments arguments, kcScriptDisplaySettings settings) {
        arguments.createNext().setAsBoolean(this.shouldEnable);
    }
}