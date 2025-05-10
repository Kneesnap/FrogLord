package net.highwayfrogs.editor.games.konami.greatquest.entity;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.games.konami.greatquest.GreatQuestUtils;
import net.highwayfrogs.editor.games.konami.greatquest.entity.kcHealthDesc.kcDamageType;
import net.highwayfrogs.editor.games.konami.greatquest.generic.kcCResourceGeneric;
import net.highwayfrogs.editor.games.konami.greatquest.math.kcVector4;
import net.highwayfrogs.editor.games.konami.greatquest.script.kcScriptDisplaySettings;
import net.highwayfrogs.editor.system.Config;
import net.highwayfrogs.editor.system.Config.ConfigValueNode;
import net.highwayfrogs.editor.utils.NumberUtils;
import net.highwayfrogs.editor.utils.data.reader.DataReader;
import net.highwayfrogs.editor.utils.data.writer.DataWriter;
import net.highwayfrogs.editor.utils.objects.OptionalArguments;

/**
 * Represents the 'CharacterParams' struct.
 * Handled by CCharacter::Init
 * Created by Kneesnap on 8/21/2023.
 */
@Getter
@Setter
public class CharacterParams extends kcActorDesc {
    private CharacterType characterType = CharacterType.NONE;
    private final kcVector4 homePos = new kcVector4(0, 0, 0, 1); // Represents the local offset of the collision proxy (CCharacter::Init).
    private float homeRange = 30F; // 144 out of 148 are set to 30. (With the others being 10, 20, 25, 100), Appears unused
    private float visionRange = 30F; // 141/148 use 30. Used by CCharacter::CanSeeTarget
    private float visionFov = 1.2217305F; // 147/148 use this value. The Bone Cruncher uses 1.5707964. Used by CCharacter::IsPointInDialogRange, CCharacter::IsTargetInMissileRange, and CCharacter::CanSeeTarget.
    private float hearRange = 30F; // 147/148 use 30F, The Bone Cruncher uses 50F. The range which the target entity will be detected within. (MonsterClass::Set_States)
    private float huntRange = 20F; // 139/148 use 20F, Hiss uses 50F. Appears unused.
    private float defendRange = 3F; // 139/148 use 3F, appears unused.
    private float attackRange = 1F; // 122/148 use 1F, 15 use 2F, 8 use 3F. Appears unused.
    private float meleeRange = 1F; // 122/148 use 1F, 15 use 2F, 8 use 3F. Used by CCharacter::IsTargetInMeleeRange
    private float missileRange = 1F; // 1F appears to be the default, on entities which are not ranged. Used by CCharacter::IsTargetInMissileRange
    private int weaponMask; // Used by CCharacter::AttackCallback
    private int attackStrength = 10; // 136/148 use 10, Used by CCharacter::AttackCallback, CFrogCtl::CheckForHealthBug. Set by MonsterClass:Do_Find() -> Seems to be set to the AI Melee Attack Damage (AITemp2)
    private short aggression; // Copied in AISystemClass::Process, ticked in MonsterClass::Set_States. This appears to be a counter. 0xFF means ALWAYS aggressive, anything else will assign a timer to this value when damage occurs, then after the timer reaches 0, the entity will no longer be aggressive.
    private short fleePercent; // 125/128, the fairies have 100, Itty Bitty/Goobler/Snicker have 10.
    private short guardHome; // Usually 0, but sometimes 50. MonsterClass::Calculate_Goal() a goal value for how
    private short protectLike; // Usually 0, sometimes 50. Appears unused.
    private short climbHeight; // Usually 0. Seems unused?
    private short fallHeight; // Usually 0. Seems unused?
    private short monsterGroup; // Usually 0. Used in AISystemClass::Process.
    private short AITemp2; // Used as MeleeAttackDamage in MonsterClass::Set_States
    private short AITemp3; // Used as MissileAttackDamage in MonsterClass::Set_States
    private short AITemp4; // Used as swim/fly speed by MonsterClass::Anim_Checks seems to be speed for swimming/flying and CCharacter::GoingToWaypoint, after it is casted to float, then divided by the distance to the target.
    private int closeDistance; // 94/148. Appears unused.
    private short dodgePercent; // 121/148 Appears unused.
    private short tauntPercent; // 116/148 MonsterClass::Do_Find -> The percentage chance (100% is always) of playing the taunt sequence (Tnt)
    private short attackGoalPercent = 100; // 90/148
    private short wanderGoalPercent; // 129/148
    private short sleepGoalPercent; // 148/148.
    private boolean preferRanged; // 144/148. Used by MonsterClass::Set_States Used in cases where an entity can do both, such as the Crossbow Goblin.
    private boolean avoidWater; // Used by MonsterClass::Set_States Cannot enter water (Such as the mosquitos who will teleport above the water)
    private short recoverySpeed = 10; // 70/148 Appears unused.
    private short meleeAttackSpeed = 10; // 71/148 Time in between melee attacks. MonsterClass::Do_Find. Units are probably 1/1000th of a second.
    private short rangedAttackSpeed = 10; // Time in between ranged attacks. MonsterClass::Do_Find Units are probably 1/100th of a second.
    private boolean preferRun; // 123/148 I couldn't figure out where this was checked, but it seems to be used, as Gooblers use their run sequence over their walk sequence, and have this set.
    private float activationRange; // 136/148 Appears unused.

    private static final int PADDING_VALUES = 56;
    private static final float DEFAULT_DIALOG_RANGE = 1F;
    private static final int DEFAULT_ATTACK_RATE = -1;
    private static final float DEFAULT_CORE_RANGE = .75F; // Appears unused

    public CharacterParams(@NonNull kcCResourceGeneric resource) {
        super(resource, kcEntityDescType.CHARACTER_PARAMS);
    }

    @Override
    public void load(DataReader reader) {
        super.load(reader);
        int characterDescHash = reader.readInt();
        this.characterType = CharacterType.getType(reader.readInt(), false);
        int attributes = reader.readInt();
        this.homePos.load(reader);
        this.homeRange = reader.readFloat();
        float coreRange = reader.readFloat();
        this.visionRange = reader.readFloat();
        this.visionFov = reader.readFloat();
        this.hearRange = reader.readFloat();
        float dialogRange = reader.readFloat(); // Ignored when read by CCharacter::Init in favor of 2.5F (Used by CCharacter::IsPointInDialogRange)
        this.huntRange = reader.readFloat();
        this.defendRange = reader.readFloat();
        this.attackRange = reader.readFloat();
        this.meleeRange = reader.readFloat();
        this.missileRange = reader.readFloat();
        this.weaponMask = reader.readInt();
        this.attackStrength = reader.readInt();
        int attackRate = reader.readInt();
        this.aggression = reader.readUnsignedByteAsShort(); // 255 is the max.
        this.fleePercent = reader.readUnsignedByteAsShort();
        this.guardHome = reader.readUnsignedByteAsShort();
        this.protectLike = reader.readUnsignedByteAsShort();
        this.climbHeight = reader.readUnsignedByteAsShort();
        this.fallHeight = reader.readUnsignedByteAsShort();
        this.monsterGroup = reader.readUnsignedByteAsShort();
        this.AITemp2 = reader.readUnsignedByteAsShort();
        this.AITemp3 = reader.readUnsignedByteAsShort();
        this.AITemp4 = reader.readUnsignedByteAsShort();
        short AITemp5 = reader.readUnsignedByteAsShort();
        short AITemp6 = reader.readUnsignedByteAsShort();
        this.closeDistance = reader.readInt();
        this.dodgePercent = reader.readUnsignedByteAsShort();
        this.tauntPercent = reader.readUnsignedByteAsShort();
        this.attackGoalPercent = reader.readUnsignedByteAsShort();
        this.wanderGoalPercent = reader.readUnsignedByteAsShort();
        this.sleepGoalPercent = reader.readUnsignedByteAsShort();
        this.preferRanged = GreatQuestUtils.readTGQByteBoolean(reader);
        this.avoidWater = GreatQuestUtils.readTGQByteBoolean(reader);
        this.recoverySpeed = reader.readUnsignedByteAsShort();
        this.meleeAttackSpeed = reader.readUnsignedByteAsShort();
        this.rangedAttackSpeed = reader.readUnsignedByteAsShort();
        this.preferRun = GreatQuestUtils.readTGQByteBoolean(reader);
        reader.skipBytesRequireEmpty(1); // pad1
        this.activationRange = reader.readFloat();
        reader.skipBytesRequireEmpty(PADDING_VALUES * Constants.INTEGER_SIZE);

        if (attributes != this.characterType.getAttributes())
            throw new RuntimeException("Expected character attributes of " + NumberUtils.toHexString(this.characterType.getAttributes()) + ", but actually got " + NumberUtils.toHexString(attributes));

        if (coreRange != DEFAULT_CORE_RANGE)
            throw new RuntimeException("Expected a core range of " + DEFAULT_CORE_RANGE + ", but got " + coreRange + " instead.");

        if (dialogRange != DEFAULT_DIALOG_RANGE)
            throw new RuntimeException("Expected default dialog range of " + DEFAULT_DIALOG_RANGE + ", but got " + dialogRange + " instead.");

        if (attackRate != DEFAULT_ATTACK_RATE)
            throw new RuntimeException("Expected an attack rate of " + DEFAULT_ATTACK_RATE + ", but got " + attackRate + " instead.");

        if (AITemp5 != 0)
            throw new RuntimeException("Expected AITemp5 to be zero, but was actually " + AITemp5 + ".");

        if (AITemp6 != 0)
            throw new RuntimeException("Expected AITemp6 to be zero, but was actually " + AITemp6 + ".");

        if (characterDescHash != getParentHash().getHashNumber())
            throw new RuntimeException("The CharacterParams reported the parent chunk as " + NumberUtils.to0PrefixedHexString(characterDescHash) + ", but it was expected to be " + getParentHash().getHashNumberAsString() + ".");
    }

    @Override
    public void saveData(DataWriter writer) {
        super.saveData(writer);
        writer.writeInt(getParentHash().getHashNumber());
        writer.writeInt(this.characterType.ordinal());
        writer.writeInt(this.characterType.getAttributes());
        this.homePos.save(writer);
        writer.writeFloat(this.homeRange);
        writer.writeFloat(DEFAULT_CORE_RANGE);
        writer.writeFloat(this.visionRange);
        writer.writeFloat(this.visionFov);
        writer.writeFloat(this.hearRange);
        writer.writeFloat(DEFAULT_DIALOG_RANGE);
        writer.writeFloat(this.huntRange);
        writer.writeFloat(this.defendRange);
        writer.writeFloat(this.attackRange);
        writer.writeFloat(this.meleeRange);
        writer.writeFloat(this.missileRange);
        writer.writeInt(this.weaponMask);
        writer.writeInt(this.attackStrength);
        writer.writeInt(DEFAULT_ATTACK_RATE);
        writer.writeUnsignedByte(this.aggression);
        writer.writeUnsignedByte(this.fleePercent);
        writer.writeUnsignedByte(this.guardHome);
        writer.writeUnsignedByte(this.protectLike);
        writer.writeUnsignedByte(this.climbHeight);
        writer.writeUnsignedByte(this.fallHeight);
        writer.writeUnsignedByte(this.monsterGroup);
        writer.writeUnsignedByte(this.AITemp2);
        writer.writeUnsignedByte(this.AITemp3);
        writer.writeUnsignedByte(this.AITemp4);
        writer.writeUnsignedByte((short) 0); // AITemp5
        writer.writeUnsignedByte((short) 0); // AITemp6
        writer.writeInt(this.closeDistance);
        writer.writeUnsignedByte(this.dodgePercent);
        writer.writeUnsignedByte(this.tauntPercent);
        writer.writeUnsignedByte(this.attackGoalPercent);
        writer.writeUnsignedByte(this.wanderGoalPercent);
        writer.writeUnsignedByte(this.sleepGoalPercent);
        GreatQuestUtils.writeTGQByteBoolean(writer, this.preferRanged);
        GreatQuestUtils.writeTGQByteBoolean(writer, this.avoidWater);
        writer.writeUnsignedByte(this.recoverySpeed);
        writer.writeUnsignedByte(this.meleeAttackSpeed);
        writer.writeUnsignedByte(this.rangedAttackSpeed);
        GreatQuestUtils.writeTGQByteBoolean(writer, this.preferRun);
        writer.writeByte(Constants.NULL_BYTE); // pad1
        writer.writeFloat(this.activationRange);
        writer.writeNull(PADDING_VALUES * Constants.INTEGER_SIZE);
    }

    @Override
    public void writeMultiLineInfo(StringBuilder builder, String padding) {
        super.writeMultiLineInfo(builder, padding);
        builder.append(padding).append("Character Type: ").append(this.characterType).append(Constants.NEWLINE);
        this.homePos.writePrefixedInfoLine(builder, "Home Location", padding);
        builder.append(padding).append("Home Range: ").append(this.homeRange).append(Constants.NEWLINE);
        builder.append(padding).append("Vision Range: ").append(this.visionRange).append(Constants.NEWLINE);
        builder.append(padding).append("Vision FOV: ").append(this.visionFov).append(Constants.NEWLINE);
        builder.append(padding).append("Hear Range: ").append(this.hearRange).append(Constants.NEWLINE);
        builder.append(padding).append("Hunt Range: ").append(this.huntRange).append(Constants.NEWLINE);
        builder.append(padding).append("Defend Range: ").append(this.defendRange).append(Constants.NEWLINE);
        builder.append(padding).append("Attack Range: ").append(this.attackRange).append(Constants.NEWLINE);
        builder.append(padding).append("Melee Range: ").append(this.meleeRange).append(Constants.NEWLINE);
        builder.append(padding).append("Missile Range: ").append(this.missileRange).append(Constants.NEWLINE);
        builder.append(padding).append("Weapon Flags (BitMask): ").append(NumberUtils.toHexString(this.weaponMask)).append(Constants.NEWLINE);
        builder.append(padding).append("Attack Strength: ").append(this.attackStrength).append(Constants.NEWLINE);
        builder.append(padding).append("Aggression: ").append(this.aggression).append(Constants.NEWLINE);
        builder.append(padding).append("Flee Percent: ").append(this.fleePercent).append(Constants.NEWLINE);
        builder.append(padding).append("Guard Home: ").append(this.guardHome).append(Constants.NEWLINE);
        builder.append(padding).append("Protect Like: ").append(this.protectLike).append(Constants.NEWLINE);
        builder.append(padding).append("Climb Height: ").append(this.climbHeight).append(Constants.NEWLINE);
        builder.append(padding).append("Fall Height: ").append(this.fallHeight).append(Constants.NEWLINE);
        builder.append(padding).append("Monster Group: ").append(this.monsterGroup).append(Constants.NEWLINE);
        builder.append(padding).append("AI Temp2: ").append(this.AITemp2).append(Constants.NEWLINE);
        builder.append(padding).append("AI Temp3: ").append(this.AITemp3).append(Constants.NEWLINE);
        builder.append(padding).append("AI Temp4: ").append(this.AITemp4).append(Constants.NEWLINE);
        builder.append(padding).append("Close Distance: ").append(this.closeDistance).append(Constants.NEWLINE);
        builder.append(padding).append("Dodge Percent: ").append(this.dodgePercent).append(Constants.NEWLINE);
        builder.append(padding).append("Taunt Percent: ").append(this.tauntPercent).append(Constants.NEWLINE);
        builder.append(padding).append("Attack Percent: ").append(this.attackGoalPercent).append(Constants.NEWLINE);
        builder.append(padding).append("Wander Percent: ").append(this.wanderGoalPercent).append(Constants.NEWLINE);
        builder.append(padding).append("Sleep Percent: ").append(this.sleepGoalPercent).append(Constants.NEWLINE);
        builder.append(padding).append("Prefer Ranged: ").append(this.preferRanged).append(Constants.NEWLINE);
        builder.append(padding).append("Avoid Water: ").append(this.avoidWater).append(Constants.NEWLINE);
        builder.append(padding).append("Recovery Speed: ").append(this.recoverySpeed).append(Constants.NEWLINE);
        builder.append(padding).append("Melee Attack Speed: ").append(this.meleeAttackSpeed).append(Constants.NEWLINE);
        builder.append(padding).append("Ranged Attack Speed: ").append(this.rangedAttackSpeed).append(Constants.NEWLINE);
        builder.append(padding).append("Prefer Run: ").append(this.preferRanged).append(Constants.NEWLINE);
        builder.append(padding).append("Activation Range: ").append(this.activationRange).append(Constants.NEWLINE);
    }

    private static final String CONFIG_KEY_CHARACTER_TYPE = "characterType";
    private static final String CONFIG_KEY_HOME_POSITION = "homePos";
    private static final String CONFIG_KEY_HOME_RANGE = "homeRange";
    private static final String CONFIG_KEY_VISION_RANGE = "visionRange";
    private static final String CONFIG_KEY_VISION_FOV = "visionFov";
    private static final String CONFIG_KEY_HEAR_RANGE = "hearRange";
    private static final String CONFIG_KEY_HUNT_RANGE = "huntRange";
    private static final String CONFIG_KEY_DEFEND_RANGE = "defendRange";
    private static final String CONFIG_KEY_ATTACK_RANGE = "attackRange";
    private static final String CONFIG_KEY_MELEE_RANGE = "meleeRange";
    private static final String CONFIG_KEY_MISSILE_RANGE = "missileRange";
    private static final String CONFIG_KEY_WEAPON_MASK = "weaponMask";
    private static final String CONFIG_KEY_ATTACK_STRENGTH = "attackStrength";
    private static final String CONFIG_KEY_AGGRESSION = "aggressionTimer";
    private static final String CONFIG_KEY_FLEE_PERCENT = "fleePercent";
    private static final String CONFIG_KEY_GUARD_HOME = "guardHome";
    private static final String CONFIG_KEY_PROTECT_LIKE = "protectLike";
    private static final String CONFIG_KEY_CLIMB_HEIGHT = "climbHeight";
    private static final String CONFIG_KEY_FALL_HEIGHT = "fallHeight";
    private static final String CONFIG_KEY_MONSTER_GROUP = "monsterGroup";
    private static final String CONFIG_KEY_AI_TEMP2 = "aiMeleeDamage";
    private static final String CONFIG_KEY_AI_TEMP3 = "aiRangeDamage";
    private static final String CONFIG_KEY_AI_TEMP4 = "flyOrSwimSpeed";
    private static final String CONFIG_KEY_CLOSE_DISTANCE = "closeDistance";
    private static final String CONFIG_KEY_DODGE_PERCENT = "dodgePercent";
    private static final String CONFIG_KEY_TAUNT_PERCENT = "tauntPercent";
    private static final String CONFIG_KEY_ATTACK_PERCENT = "attackGoalPercent";
    private static final String CONFIG_KEY_WANDER_PERCENT = "wanderGoalPercent";
    private static final String CONFIG_KEY_SLEEP_PERCENT = "sleepGoalPercent";
    private static final String CONFIG_KEY_PREFER_RANGED = "preferRanged";
    private static final String CONFIG_KEY_AVOID_WATER = "avoidWater";
    private static final String CONFIG_KEY_RECOVERY_SPEED = "recoverySpeed";
    private static final String CONFIG_KEY_MELEE_ATTACK_SPEED = "meleeAttackSpeed";
    private static final String CONFIG_KEY_RANGED_ATTACK_SPEED = "rangedAttackSpeed";
    private static final String CONFIG_KEY_PREFER_RUN = "preferRun";
    private static final String CONFIG_KEY_ACTIVATION_RANGE = "activationRange";

    private static float getOptionalFloat(Config input, String key, float defaultValue) {
        ConfigValueNode node = input.getOptionalKeyValueNode(key);
        return node != null ? node.getAsFloat() : defaultValue;
    }

    private static int getOptionalInt(Config input, String key, int defaultValue) {
        ConfigValueNode node = input.getOptionalKeyValueNode(key);
        return node != null ? node.getAsInteger() : defaultValue;
    }

    @Override
    public void fromConfig(Config input) {
        super.fromConfig(input);
        this.characterType = input.getKeyValueNodeOrError(CONFIG_KEY_CHARACTER_TYPE).getAsEnumOrError(CharacterType.class);
        this.homePos.parse(input.getKeyValueNodeOrError(CONFIG_KEY_HOME_POSITION).getAsString(), 1);
        this.homeRange = input.getKeyValueNodeOrError(CONFIG_KEY_HOME_RANGE).getAsFloat();
        this.visionRange = input.getKeyValueNodeOrError(CONFIG_KEY_VISION_RANGE).getAsFloat();
        this.visionFov = input.getKeyValueNodeOrError(CONFIG_KEY_VISION_FOV).getAsFloat();
        this.hearRange = input.getKeyValueNodeOrError(CONFIG_KEY_HEAR_RANGE).getAsFloat();
        this.huntRange = getOptionalFloat(input, CONFIG_KEY_HUNT_RANGE, this.huntRange);
        this.defendRange = getOptionalFloat(input, CONFIG_KEY_DEFEND_RANGE, this.huntRange);
        this.attackRange = getOptionalFloat(input, CONFIG_KEY_ATTACK_RANGE, this.huntRange);
        this.meleeRange = input.getKeyValueNodeOrError(CONFIG_KEY_MELEE_RANGE).getAsFloat();
        this.missileRange = input.getKeyValueNodeOrError(CONFIG_KEY_MISSILE_RANGE).getAsFloat();
        String damageFlags = input.getKeyValueNodeOrError(CONFIG_KEY_WEAPON_MASK).getAsString();
        this.weaponMask = kcDamageType.getValueFromArguments(OptionalArguments.parseCommaSeparatedNamedArguments(damageFlags));
        this.attackStrength = input.getKeyValueNodeOrError(CONFIG_KEY_ATTACK_STRENGTH).getAsInteger();
        this.aggression = (short) input.getKeyValueNodeOrError(CONFIG_KEY_AGGRESSION).getAsInteger();
        this.fleePercent = (short) input.getKeyValueNodeOrError(CONFIG_KEY_FLEE_PERCENT).getAsInteger();
        this.guardHome = (short) input.getKeyValueNodeOrError(CONFIG_KEY_GUARD_HOME).getAsInteger();
        this.protectLike = (short) getOptionalInt(input, CONFIG_KEY_PROTECT_LIKE, this.protectLike);
        this.climbHeight = (short) getOptionalInt(input, CONFIG_KEY_CLIMB_HEIGHT, this.climbHeight);
        this.fallHeight = (short) getOptionalInt(input, CONFIG_KEY_FALL_HEIGHT, this.fallHeight);
        this.monsterGroup = (short) input.getKeyValueNodeOrError(CONFIG_KEY_MONSTER_GROUP).getAsInteger();
        this.AITemp2 = (short) input.getKeyValueNodeOrError(CONFIG_KEY_AI_TEMP2).getAsInteger();
        this.AITemp3 = (short) input.getKeyValueNodeOrError(CONFIG_KEY_AI_TEMP3).getAsInteger();
        this.AITemp4 = (short) input.getKeyValueNodeOrError(CONFIG_KEY_AI_TEMP4).getAsInteger();
        this.closeDistance = (short) getOptionalInt(input, CONFIG_KEY_CLOSE_DISTANCE, this.closeDistance);
        this.dodgePercent = (short) getOptionalInt(input, CONFIG_KEY_DODGE_PERCENT, this.dodgePercent);
        this.tauntPercent = (short) input.getKeyValueNodeOrError(CONFIG_KEY_TAUNT_PERCENT).getAsInteger();
        this.attackGoalPercent = (short) input.getKeyValueNodeOrError(CONFIG_KEY_ATTACK_PERCENT).getAsInteger();
        this.wanderGoalPercent = (short) input.getKeyValueNodeOrError(CONFIG_KEY_WANDER_PERCENT).getAsInteger();
        this.sleepGoalPercent = (short) input.getKeyValueNodeOrError(CONFIG_KEY_SLEEP_PERCENT).getAsInteger();
        this.preferRanged = input.getKeyValueNodeOrError(CONFIG_KEY_PREFER_RANGED).getAsBoolean();
        this.avoidWater = input.getKeyValueNodeOrError(CONFIG_KEY_AVOID_WATER).getAsBoolean();
        this.recoverySpeed = (short) getOptionalInt(input, CONFIG_KEY_RECOVERY_SPEED, this.recoverySpeed);
        this.meleeAttackSpeed = (short) input.getKeyValueNodeOrError(CONFIG_KEY_MELEE_ATTACK_SPEED).getAsInteger();
        this.rangedAttackSpeed = (short) input.getKeyValueNodeOrError(CONFIG_KEY_RANGED_ATTACK_SPEED).getAsInteger();
        this.preferRun = input.getKeyValueNodeOrError(CONFIG_KEY_PREFER_RUN).getAsBoolean();
        this.activationRange = getOptionalFloat(input, CONFIG_KEY_ACTIVATION_RANGE, this.activationRange);
    }

    @Override
    @SuppressWarnings("CommentedOutCode") // Commented lines may potentially be enabled in the future depending on what is deemed as actually used/useful.
    public void toConfig(Config output, kcScriptDisplaySettings settings) {
        super.toConfig(output, settings);
        output.getOrCreateKeyValueNode(CONFIG_KEY_CHARACTER_TYPE).setAsEnum(this.characterType);
        output.getOrCreateKeyValueNode(CONFIG_KEY_HOME_POSITION).setAsString(this.homePos.toParseableString(1));
        output.getOrCreateKeyValueNode(CONFIG_KEY_HOME_RANGE).setAsFloat(this.homeRange);
        output.getOrCreateKeyValueNode(CONFIG_KEY_VISION_RANGE).setAsFloat(this.visionRange);
        output.getOrCreateKeyValueNode(CONFIG_KEY_VISION_FOV).setAsFloat(this.visionFov);
        output.getOrCreateKeyValueNode(CONFIG_KEY_HEAR_RANGE).setAsFloat(this.hearRange);
        //output.getOrCreateKeyValueNode(CONFIG_KEY_HUNT_RANGE).setAsFloat(this.huntRange);
        //output.getOrCreateKeyValueNode(CONFIG_KEY_DEFEND_RANGE).setAsFloat(this.defendRange);
        //output.getOrCreateKeyValueNode(CONFIG_KEY_ATTACK_RANGE).setAsFloat(this.attackRange);
        output.getOrCreateKeyValueNode(CONFIG_KEY_MELEE_RANGE).setAsFloat(this.meleeRange);
        output.getOrCreateKeyValueNode(CONFIG_KEY_MISSILE_RANGE).setAsFloat(this.missileRange);
        output.getOrCreateKeyValueNode(CONFIG_KEY_WEAPON_MASK).setAsString(kcDamageType.getFlagsAsString(this.weaponMask));
        output.getOrCreateKeyValueNode(CONFIG_KEY_ATTACK_STRENGTH).setAsInteger(this.attackStrength);
        output.getOrCreateKeyValueNode(CONFIG_KEY_AGGRESSION).setAsInteger(this.aggression);
        output.getOrCreateKeyValueNode(CONFIG_KEY_FLEE_PERCENT).setAsInteger(this.fleePercent);
        output.getOrCreateKeyValueNode(CONFIG_KEY_GUARD_HOME).setAsInteger(this.guardHome);
        //output.getOrCreateKeyValueNode(CONFIG_KEY_PROTECT_LIKE).setAsInteger(this.protectLike);
        //output.getOrCreateKeyValueNode(CONFIG_KEY_CLIMB_HEIGHT).setAsInteger(this.climbHeight);
        //output.getOrCreateKeyValueNode(CONFIG_KEY_FALL_HEIGHT).setAsInteger(this.fallHeight);
        output.getOrCreateKeyValueNode(CONFIG_KEY_MONSTER_GROUP).setAsInteger(this.monsterGroup);
        output.getOrCreateKeyValueNode(CONFIG_KEY_AI_TEMP2).setAsInteger(this.AITemp2);
        output.getOrCreateKeyValueNode(CONFIG_KEY_AI_TEMP3).setAsInteger(this.AITemp3);
        output.getOrCreateKeyValueNode(CONFIG_KEY_AI_TEMP4).setAsInteger(this.AITemp4);
        //output.getOrCreateKeyValueNode(CONFIG_KEY_CLOSE_DISTANCE).setAsInteger(this.closeDistance);
        //output.getOrCreateKeyValueNode(CONFIG_KEY_DODGE_PERCENT).setAsInteger(this.dodgePercent);
        output.getOrCreateKeyValueNode(CONFIG_KEY_TAUNT_PERCENT).setAsInteger(this.tauntPercent);
        output.getOrCreateKeyValueNode(CONFIG_KEY_ATTACK_PERCENT).setAsInteger(this.attackGoalPercent);
        output.getOrCreateKeyValueNode(CONFIG_KEY_WANDER_PERCENT).setAsInteger(this.wanderGoalPercent);
        output.getOrCreateKeyValueNode(CONFIG_KEY_SLEEP_PERCENT).setAsInteger(this.sleepGoalPercent);
        output.getOrCreateKeyValueNode(CONFIG_KEY_PREFER_RANGED).setAsBoolean(this.preferRanged);
        output.getOrCreateKeyValueNode(CONFIG_KEY_AVOID_WATER).setAsBoolean(this.avoidWater);
        //output.getOrCreateKeyValueNode(CONFIG_KEY_RECOVERY_SPEED).setAsInteger(this.recoverySpeed);
        output.getOrCreateKeyValueNode(CONFIG_KEY_MELEE_ATTACK_SPEED).setAsInteger(this.meleeAttackSpeed);
        output.getOrCreateKeyValueNode(CONFIG_KEY_RANGED_ATTACK_SPEED).setAsInteger(this.rangedAttackSpeed);
        output.getOrCreateKeyValueNode(CONFIG_KEY_PREFER_RUN).setAsBoolean(this.preferRun);
        //output.getOrCreateKeyValueNode(CONFIG_KEY_ACTIVATION_RANGE).setAsFloat(this.activationRange);
    }

    /**
     * Sets the character type.
     * @param newType The character type to apply.
     */
    @SuppressWarnings("unused") // Available to Noodle scripts.
    public void setCharacterType(CharacterType newType) {
        if (newType == null)
            throw new NullPointerException("newType");

        this.characterType = newType;
    }

    public enum CharacterType {
        NONE, PLAYER, STATIC, WALKER, FLYER, SWIMMER;

        /**
         * Gets the attributes for the character parameters.
         */
        public int getAttributes() {
            return (this == PLAYER) ? 0xC0000000 : 0;
        }

        /**
         * Gets the CharacterType corresponding to the provided tag.
         * @param value     The value to lookup.
         * @param allowNull If null is allowed.
         * @return characterType
         */
        public static CharacterType getType(int value, boolean allowNull) {
            if (value < 0 || value >= values().length) {
                if (allowNull)
                    return null;

                throw new RuntimeException("Couldn't determine the CharacterType from value " + value + ".");
            }

            return values()[value];
        }
    }
}