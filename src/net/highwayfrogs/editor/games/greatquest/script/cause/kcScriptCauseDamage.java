package net.highwayfrogs.editor.games.greatquest.script.cause;

import lombok.AllArgsConstructor;
import lombok.Getter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.games.greatquest.script.kcScriptDisplaySettings;

import java.util.List;

/**
 * Called when an entity takes damage.
 * Trigger: kcCActorBase::OnDamage
 * From doing debugging on this I found there's a fairly long cooldown between enemies taking damage, and your attacks hitting do nothing.
 * Created by Kneesnap on 8/16/2023.
 */
public class kcScriptCauseDamage extends kcScriptCause {
    private DamageFlag damageFlag;

    public kcScriptCauseDamage() {
        super(kcScriptCauseType.DAMAGE, 0);
    }

    @Override
    public void load(int subCauseType, List<Integer> extraValues) {
        this.damageFlag = DamageFlag.values()[(subCauseType & 0x1F)];
    }

    @Override
    public void save(List<Integer> output) {
        output.add(this.damageFlag.ordinal());
    }

    @Override
    public void toString(StringBuilder builder, kcScriptDisplaySettings settings) {
        builder.append("The attached entity taking damage with the ").append(this.damageFlag).append(" flag set.");
    }

    @Getter
    @AllArgsConstructor
    public enum DamageFlag {
        FIRE(Constants.BIT_FLAG_0),
        ICE(Constants.BIT_FLAG_1),
        UNKNOWN_FLAG_2(Constants.BIT_FLAG_2),
        MELEE(Constants.BIT_FLAG_3),
        RANGED(Constants.BIT_FLAG_4),
        UNKNOWN_FLAG_5(Constants.BIT_FLAG_5), // The Magical General has this.
        UNKNOWN_FLAG_6(Constants.BIT_FLAG_6),
        UNKNOWN_FLAG_7(Constants.BIT_FLAG_7),
        UNKNOWN_FLAG_8(Constants.BIT_FLAG_8),
        UNKNOWN_FLAG_9(Constants.BIT_FLAG_9),
        UNKNOWN_FLAG_10(Constants.BIT_FLAG_10),
        UNKNOWN_FLAG_11(Constants.BIT_FLAG_11),
        FALL(Constants.BIT_FLAG_12),
        UNKNOWN_FLAG_13(Constants.BIT_FLAG_13),
        UNKNOWN_FLAG_14(Constants.BIT_FLAG_14),
        UNKNOWN_FLAG_15(Constants.BIT_FLAG_15),
        UNKNOWN_FLAG_16(Constants.BIT_FLAG_16),
        UNKNOWN_FLAG_17(Constants.BIT_FLAG_17),
        UNKNOWN_FLAG_18(Constants.BIT_FLAG_18),
        UNKNOWN_FLAG_19(Constants.BIT_FLAG_19),
        UNKNOWN_FLAG_20(Constants.BIT_FLAG_20),
        UNKNOWN_FLAG_21(Constants.BIT_FLAG_21),
        UNKNOWN_FLAG_22(Constants.BIT_FLAG_22),
        UNKNOWN_FLAG_23(Constants.BIT_FLAG_23),
        UNKNOWN_FLAG_24(Constants.BIT_FLAG_24),
        UNKNOWN_FLAG_25(Constants.BIT_FLAG_25),
        UNKNOWN_FLAG_26(Constants.BIT_FLAG_26),
        UNKNOWN_FLAG_27(Constants.BIT_FLAG_27),
        UNKNOWN_FLAG_28(Constants.BIT_FLAG_28),
        UNKNOWN_FLAG_29(Constants.BIT_FLAG_29),
        UNKNOWN_FLAG_30(Constants.BIT_FLAG_30),
        UNKNOWN_FLAG_31(Constants.BIT_FLAG_31);

        private final int mask;
    }
}