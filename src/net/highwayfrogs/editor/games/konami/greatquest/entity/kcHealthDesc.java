package net.highwayfrogs.editor.games.konami.greatquest.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.games.generic.data.GameData;
import net.highwayfrogs.editor.games.konami.IConfigData;
import net.highwayfrogs.editor.games.konami.greatquest.GreatQuestInstance;
import net.highwayfrogs.editor.gui.components.propertylist.IPropertyListCreator;
import net.highwayfrogs.editor.gui.components.propertylist.PropertyListNode;
import net.highwayfrogs.editor.system.Config;
import net.highwayfrogs.editor.utils.NumberUtils;
import net.highwayfrogs.editor.utils.data.reader.DataReader;
import net.highwayfrogs.editor.utils.data.writer.DataWriter;
import net.highwayfrogs.editor.utils.logging.ILogger;
import net.highwayfrogs.editor.utils.objects.OptionalArguments;

/**
 * Represents the kcHealthDesc struct.
 * Created by Kneesnap on 8/21/2023.
 */
@Getter
@Setter
public class kcHealthDesc extends GameData<GreatQuestInstance> implements IPropertyListCreator, IConfigData {
    private int maxHealth = DEFAULT_MAX_HEALTH; // When loaded, if this is less than 1, 100 is used. The game called this durability.
    private int startHealth = DEFAULT_MAX_HEALTH; // When loaded, if this is less than 1, 100 is used.
    // This is a bit mask which represent the types of damage which this object is immune to.
    // The flags are represented by the kcDamageType class.
    // If even a single one of these flags is seen when damage should occur, damage will be skipped.
    private int immuneMask = DEFAULT_IMMUNE_MASK;

    private static final int DEFAULT_MAX_HEALTH = 100;
    private static final int DEFAULT_IMMUNE_MASK = 0;

    public kcHealthDesc(GreatQuestInstance instance) {
        super(instance);
    }

    @Override
    public void load(DataReader reader) {
        this.maxHealth = reader.readInt();
        this.startHealth = reader.readInt();
        this.immuneMask = reader.readInt();
    }

    @Override
    public void save(DataWriter writer) {
        writer.writeInt(this.maxHealth);
        writer.writeInt(this.startHealth);
        writer.writeInt(this.immuneMask);
    }

    @Override
    public void addToPropertyList(PropertyListNode propertyList) {
        propertyList.addInteger("Max Health (Durability)", this.maxHealth)
                        .setDataHandler((entry, newMaxHealth) -> {
                            if (this.maxHealth == this.startHealth) {
                                this.startHealth = this.maxHealth = newMaxHealth;
                                entry.updateParent();
                            } else {
                                this.maxHealth = newMaxHealth;
                            }
                        });

        propertyList.addInteger("Start Health", this.startHealth, newStartHealth -> this.startHealth = newStartHealth);
        if (this.immuneMask != DEFAULT_IMMUNE_MASK)
            propertyList.add("Immune Mask", NumberUtils.toHexString(this.immuneMask));
    }

    private static final String CONFIG_KEY_MAX_HEALTH = "maxHealth";
    private static final String CONFIG_KEY_START_HEALTH = "startHealth";
    private static final String CONFIG_KEY_IMMUNE_MASK = "immuneMask";

    @Override
    public void fromConfig(ILogger logger, Config input) {
        this.maxHealth = input.getOrDefaultKeyValueNode(CONFIG_KEY_MAX_HEALTH).getAsInteger(DEFAULT_MAX_HEALTH);
        this.startHealth = input.getOrDefaultKeyValueNode(CONFIG_KEY_START_HEALTH).getAsInteger(this.startHealth);

        if (input.hasKeyValueNode(CONFIG_KEY_IMMUNE_MASK)) {
            OptionalArguments arguments = OptionalArguments.parseCommaSeparatedNamedArguments(input.getKeyValueNodeOrError(CONFIG_KEY_IMMUNE_MASK).getAsString());
            this.immuneMask = kcDamageType.getValueFromArguments(arguments);
            arguments.warnAboutUnusedArguments(getGameInstance().getLogger());
        } else {
            this.immuneMask = DEFAULT_IMMUNE_MASK;
        }
    }

    @Override
    public void toConfig(Config output) {
        if (this.maxHealth != DEFAULT_MAX_HEALTH)
            output.getOrCreateKeyValueNode(CONFIG_KEY_MAX_HEALTH).setAsInteger(this.maxHealth);
        if (this.startHealth != this.maxHealth)
            output.getOrCreateKeyValueNode(CONFIG_KEY_START_HEALTH).setAsInteger(this.startHealth);
        if (this.immuneMask != DEFAULT_IMMUNE_MASK)
            output.getOrCreateKeyValueNode(CONFIG_KEY_IMMUNE_MASK).setAsString(kcDamageType.getFlagsAsString(this.immuneMask));
    }

    /**
     * This is a registry of all different types of damage in Frogger: The Great Quest.
     * This is not from debug symbols, instead found through observation.
     * Each damage type corresponds to a bit flag, meaning there are many unused/undefined damage types too.
     */
    @Getter
    @AllArgsConstructor
    public enum kcDamageType {
        FIRE(Constants.BIT_FLAG_0, "Fire"), // 0x01, CCharacter::OnDamage() will spawn a fireball particle if this flag is set.
        ICE(Constants.BIT_FLAG_1, "Ice"), // 0x02, CCharacter::OnDamage() will spawn an ice hit & snow particle if this flag is set.
        UNNAMED_TYPE_02(Constants.BIT_FLAG_2, "UnnamedDamageType02"), // 0x04
        MELEE(Constants.BIT_FLAG_3, "Melee"), // 0x08
        RANGED(Constants.BIT_FLAG_4, "Ranged"), // 0x10
        UNNAMED_TYPE_05(Constants.BIT_FLAG_5, "UnnamedDamageType05"), // 0x20 (The Magical General has this, Bone Cruncher) -> Boss Monster? Invulnerable Source?
        UNNAMED_TYPE_06(Constants.BIT_FLAG_6, "UnnamedDamageType06"), // 0x40, Bone Cruncher, Rattle Butt, DTR Scorpion, In the Cat Dragon's Lair, HissInst002/HissInst003/HissInst004 will damage Frogger when touched with this flag. However, it is not possible to touch Hiss as he is in the sky.
        UNNAMED_TYPE_07(Constants.BIT_FLAG_7, "UnnamedDamageType07"), // 0x80, In the Cat Dragon's Lair, HissInst002/HissInst003/HissInst004 will cause "PlayMidMovie01" to occur which plays "cdrom0:\\OMOVIES\\MDRAGONF.PSS", which is not on any disc.) This cannot occur as the player is unable to touch Hiss.
        UNNAMED_TYPE_08(Constants.BIT_FLAG_8, "UnnamedDamageType08"), // 0x100
        UNNAMED_TYPE_09(Constants.BIT_FLAG_9, "UnnamedDamageType09"), // 0x200 Joy Towers: Goobler, Grim Bite, Itty Bitty, Shadow Guard, Snicker
        UNNAMED_TYPE_10(Constants.BIT_FLAG_10, "UnnamedDamageType10"), // 0x400 DTR Scorpion
        UNNAMED_TYPE_11(Constants.BIT_FLAG_11, "UnnamedDamageType11"), // 0x800
        FALL(Constants.BIT_FLAG_12, "Fall"), // 0x1000
        UNNAMED_TYPE_13(Constants.BIT_FLAG_13, "UnnamedDamageType13"), // 0x2000
        UNNAMED_TYPE_14(Constants.BIT_FLAG_14, "UnnamedDamageType14"), // 0x4000
        UNNAMED_TYPE_15(Constants.BIT_FLAG_15, "UnnamedDamageType15"), // 0x8000
        UNNAMED_TYPE_16(Constants.BIT_FLAG_16, "UnnamedDamageType16"), // 0x10000
        UNNAMED_TYPE_17(Constants.BIT_FLAG_17, "UnnamedDamageType17"),
        UNNAMED_TYPE_18(Constants.BIT_FLAG_18, "UnnamedDamageType18"),
        UNNAMED_TYPE_19(Constants.BIT_FLAG_19, "UnnamedDamageType19"),
        UNNAMED_TYPE_20(Constants.BIT_FLAG_20, "UnnamedDamageType20"),
        UNNAMED_TYPE_21(Constants.BIT_FLAG_21, "UnnamedDamageType21"),
        UNNAMED_TYPE_22(Constants.BIT_FLAG_22, "UnnamedDamageType22"),
        UNNAMED_TYPE_23(Constants.BIT_FLAG_23, "UnnamedDamageType23"),
        UNNAMED_TYPE_24(Constants.BIT_FLAG_24, "UnnamedDamageType24"),
        UNNAMED_TYPE_25(Constants.BIT_FLAG_25, "UnnamedDamageType25"),
        UNNAMED_TYPE_26(Constants.BIT_FLAG_26, "UnnamedDamageType26"),
        UNNAMED_TYPE_27(Constants.BIT_FLAG_27, "UnnamedDamageType27"),
        UNNAMED_TYPE_28(Constants.BIT_FLAG_28, "UnnamedDamageType28"),
        UNNAMED_TYPE_29(Constants.BIT_FLAG_29, "UnnamedDamageType29"),
        UNNAMED_TYPE_30(Constants.BIT_FLAG_30, "UnnamedDamageType30"),
        UNNAMED_TYPE_31(Constants.BIT_FLAG_31, "UnnamedDamageType31");

        private final int mask;
        private final String displayName;

        /**
         * Gets the flag value as a string of all the flags.
         * @param value The value to determine which flags to apply from
         */
        public static String getFlagsAsString(int value) {
            OptionalArguments arguments = new OptionalArguments();
            addFlags(value, arguments);
            return arguments.getNamedArgumentsAsCommaSeparatedString();
        }

        /**
         * Add flags to the arguments for the corresponding damage flags.
         * @param value The value to determine which flags to apply from
         * @param arguments The arguments to add the flags to
         */
        public static void addFlags(int value, OptionalArguments arguments) {
            // Write flags.
            for (int i = 0; i < values().length; i++) {
                kcDamageType flag = values()[i];
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
                kcDamageType flag = values()[i];
                if (arguments.useFlag(flag.getDisplayName()))
                    value |= flag.getMask();
            }

            return value;
        }
    }
}