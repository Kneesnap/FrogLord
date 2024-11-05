package net.highwayfrogs.editor.games.konami.greatquest.script.cause;

import lombok.AllArgsConstructor;
import lombok.Getter;
import net.highwayfrogs.editor.Constants;
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
    private DamageFlag damageFlag;

    public kcScriptCauseDamage(kcScript script) {
        super(script, kcScriptCauseType.DAMAGE, 0, 1);
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
    protected void loadArguments(OptionalArguments arguments) {
        this.damageFlag = arguments.useNext().getAsEnumOrError(DamageFlag.class);
    }

    @Override
    protected void saveArguments(OptionalArguments arguments, kcScriptDisplaySettings settings) {
        arguments.createNext().setAsEnum(this.damageFlag);
    }

    @Override
    public void toString(StringBuilder builder, kcScriptDisplaySettings settings) {
        builder.append("The attached entity taking damage with the ").append(this.damageFlag).append(" flag set.");
    }

    @Getter
    @AllArgsConstructor
    public enum DamageFlag {
        FIRE(Constants.BIT_FLAG_0, "Fire"), // 0x01, CCharacter::OnDamage() will spawn a fireball particle if this flag is set.
        ICE(Constants.BIT_FLAG_1, "Ice"), // 0x02, CCharacter::OnDamage() will spawn an ice hit & snow particle if this flag is set.
        UNKNOWN_FLAG_2(Constants.BIT_FLAG_2, "UnusedDamageType02"), // 0x04
        MELEE(Constants.BIT_FLAG_3, "Melee"), // 0x08
        RANGED(Constants.BIT_FLAG_4, "Ranged"), // 0x10
        UNKNOWN_FLAG_5(Constants.BIT_FLAG_5, "UnusedDamageType05"), // 0x20 (The Magical General has this, Bone Cruncher)
        UNKNOWN_FLAG_6(Constants.BIT_FLAG_6, "UnusedDamageType06"), // 0x40, Bone Cruncher, Rattle Butt, DTR Scorpion, In the Cat Dragon's Lair, HissInst002/HissInst003/HissInst004 will damage Frogger when touched with this flag. However, it is not possible to touch Hiss as he is in the sky.
        UNKNOWN_FLAG_7(Constants.BIT_FLAG_7, "UnusedDamageType07"), // 0x80, In the Cat Dragon's Lair, HissInst002/HissInst003/HissInst004 will cause "PlayMidMovie01" to occur which plays "cdrom0:\\OMOVIES\\MDRAGONF.PSS", which is not on any disc.) This cannot occur as the player is unable to touch Hiss.
        UNKNOWN_FLAG_8(Constants.BIT_FLAG_8, "UnusedDamageType08"), // 0x100
        UNKNOWN_FLAG_9(Constants.BIT_FLAG_9, "UnusedDamageType09"), // 0x200 Joy Towers: Goobler, Grim Bite, Itty Bitty, Shadow Guard, Snicker
        UNKNOWN_FLAG_10(Constants.BIT_FLAG_10, "UnusedDamageType10"), // 0x400 DTR Scorpion
        UNKNOWN_FLAG_11(Constants.BIT_FLAG_11, "UnusedDamageType11"), // 0x800
        FALL(Constants.BIT_FLAG_12, "Fall"), // 0x1000
        UNKNOWN_FLAG_13(Constants.BIT_FLAG_13, "UnusedDamageType13"), // 0x2000
        UNKNOWN_FLAG_14(Constants.BIT_FLAG_14, "UnusedDamageType14"), // 0x4000
        UNKNOWN_FLAG_15(Constants.BIT_FLAG_15, "UnusedDamageType15"), // 0x8000
        UNKNOWN_FLAG_16(Constants.BIT_FLAG_16, "UnusedDamageType16"), // 0x10000
        UNKNOWN_FLAG_17(Constants.BIT_FLAG_17, "UnusedDamageType17"),
        UNKNOWN_FLAG_18(Constants.BIT_FLAG_18, "UnusedDamageType18"),
        UNKNOWN_FLAG_19(Constants.BIT_FLAG_19, "UnusedDamageType19"),
        UNKNOWN_FLAG_20(Constants.BIT_FLAG_20, "UnusedDamageType20"),
        UNKNOWN_FLAG_21(Constants.BIT_FLAG_21, "UnusedDamageType21"),
        UNKNOWN_FLAG_22(Constants.BIT_FLAG_22, "UnusedDamageType22"),
        UNKNOWN_FLAG_23(Constants.BIT_FLAG_23, "UnusedDamageType23"),

        // TODO: ?
        UNKNOWN_FLAG_24(Constants.BIT_FLAG_24, "UnusedDamageType24"),
        UNKNOWN_FLAG_25(Constants.BIT_FLAG_25, "UnusedDamageType25"),
        UNKNOWN_FLAG_26(Constants.BIT_FLAG_26, "UnusedDamageType26"),
        UNKNOWN_FLAG_27(Constants.BIT_FLAG_27, "UnusedDamageType27"),
        UNKNOWN_FLAG_28(Constants.BIT_FLAG_28, "UnusedDamageType28"),
        UNKNOWN_FLAG_29(Constants.BIT_FLAG_29, "UnusedDamageType29"),
        UNKNOWN_FLAG_30(Constants.BIT_FLAG_30, "UnusedDamageType30"),
        UNKNOWN_FLAG_31(Constants.BIT_FLAG_31, "UnusedDamageType31");

        private final int mask;
        private final String displayName;

        /**
         * Gets the flag value as a string of all the flags.
         * @param value The value to determine which flags to apply from
         */
        public static String getFlagsAsString(int value) {
            OptionalArguments arguments = new OptionalArguments();
            addFlags(value, arguments);
            return arguments.toString();
        }

        /**
         * Add flags to the arguments for the corresponding damage flags.
         * @param value The value to determine which flags to apply from
         * @param arguments The arguments to add the flags to
         */
        public static void addFlags(int value, OptionalArguments arguments) {
            // Write flags.
            for (int i = 0; i < values().length; i++) {
                DamageFlag flag = values()[i];
                if ((value & flag.getMask()) == flag.getMask())
                    arguments.getOrCreate(flag.getDisplayName());
            }
        }

        /**
         * Consume optional flag arguments to build a value containing the same flags as specified by the arguments.
         * @param arguments The arguments to create the value from.
         * @return flagArguments
         */
        public static int getValueFromArguments(OptionalArguments arguments) {
            int value = 0;
            for (int i = 0; i < values().length; i++) {
                DamageFlag flag = values()[i];
                if (arguments.useFlag(flag.getDisplayName()))
                    value |= flag.getMask();
            }

            return value;
        }
    }
}