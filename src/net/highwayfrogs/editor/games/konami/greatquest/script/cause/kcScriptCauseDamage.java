package net.highwayfrogs.editor.games.konami.greatquest.script.cause;

import net.highwayfrogs.editor.games.konami.greatquest.chunks.kcCResourceEntityInst;
import net.highwayfrogs.editor.games.konami.greatquest.entity.kcHealthDesc.kcDamageType;
import net.highwayfrogs.editor.games.konami.greatquest.script.kcScript;
import net.highwayfrogs.editor.games.konami.greatquest.script.kcScriptDisplaySettings;
import net.highwayfrogs.editor.utils.objects.OptionalArguments;

import java.util.List;

/**
 * Called when an entity takes damage.
 * Trigger: kcCActorBase::OnDamage
 * From doing debugging on this I found there's a fairly long cooldown between enemies taking damage, and your attacks hitting do nothing.
 * Created by Kneesnap on 8/16/2023.
 */
public class kcScriptCauseDamage extends kcScriptCause {
    private kcDamageType damageType;

    public kcScriptCauseDamage(kcScript script) {
        super(script, kcScriptCauseType.DAMAGE, 0, 1);
    }

    @Override
    public void load(int subCauseType, List<Integer> extraValues) {
        this.damageType = kcDamageType.values()[(subCauseType & 0x1F)];
    }

    @Override
    public void save(List<Integer> output) {
        output.add(this.damageType.ordinal());
    }

    @Override
    protected void loadArguments(OptionalArguments arguments) {
        this.damageType = arguments.useNext().getAsEnumOrError(kcDamageType.class);
    }

    @Override
    protected void saveArguments(OptionalArguments arguments, kcScriptDisplaySettings settings) {
        arguments.createNext().setAsEnum(this.damageType);
    }

    @Override
    public void toString(StringBuilder builder, kcScriptDisplaySettings settings) {
        kcCResourceEntityInst targetEntity = getScriptEntity();
        builder.append("When ")
                .append(targetEntity != null ? targetEntity.getName() : "the attacked entity")
                .append(" takes ").append(this.damageType).append(" damage");
    }
}