package net.highwayfrogs.editor.games.konami.greatquest.script.cause;

import lombok.Getter;
import net.highwayfrogs.editor.games.konami.greatquest.chunks.kcCResourceEntityInst;
import net.highwayfrogs.editor.games.konami.greatquest.script.kcScript;
import net.highwayfrogs.editor.games.konami.greatquest.script.kcScriptDisplaySettings;
import net.highwayfrogs.editor.utils.objects.OptionalArguments;

import java.util.List;

/**
 * Represents a cause of an actor action.
 * Created by Kneesnap on 8/19/2023.
 */
@Getter
public class kcScriptCauseActor extends kcScriptCause {
    private kcScriptCauseEntityAction action;

    public kcScriptCauseActor(kcScript script) {
        super(script, kcScriptCauseType.ACTOR, 0, 1);
    }

    @Override
    public void load(int primaryValue, List<Integer> extraValues) {
        this.action = kcScriptCauseEntityAction.getAction(primaryValue, false);
    }

    @Override
    public void save(List<Integer> output) {
        output.add(this.action.getValue());
    }

    @Override
    protected void loadArguments(OptionalArguments arguments) {
        this.action = arguments.useNext().getAsEnumOrError(kcScriptCauseEntityAction.class);
    }

    @Override
    protected void saveArguments(OptionalArguments arguments, kcScriptDisplaySettings settings) {
        arguments.createNext().setAsEnum(this.action);
    }

    @Override
    public void toString(StringBuilder builder, kcScriptDisplaySettings settings) {
        kcCResourceEntityInst targetEntity = getScriptEntity();
        if (targetEntity != null && targetEntity.getName() != null) {
            builder.append(this.action.getActorDescription().replace("the attached entity", targetEntity.getName()));
        } else {
            builder.append(this.action.getActorDescription());
        }
    }
}