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
import net.highwayfrogs.editor.gui.components.propertylist.IPropertyListCreator;
import net.highwayfrogs.editor.gui.components.propertylist.PropertyListNode;
import net.highwayfrogs.editor.system.Config;
import net.highwayfrogs.editor.system.Config.ConfigValueNode;
import net.highwayfrogs.editor.utils.NumberUtils;
import net.highwayfrogs.editor.utils.data.reader.DataReader;
import net.highwayfrogs.editor.utils.data.writer.DataWriter;
import net.highwayfrogs.editor.utils.logging.ILogger;
import net.highwayfrogs.editor.utils.objects.OptionalArguments;

/**
 * Represents the 'CharacterParams' struct.
 * Handled by CCharacter::Init
 * Created by Kneesnap on 8/21/2023.
 */
@Getter
@Setter
public class CharacterParams extends kcActorDesc implements IPropertyListCreator {
    private CharacterType characterType = CharacterType.NONE;
    private final kcVector4 homePos = new kcVector4(0, 0, 0, 1); // Represents the local offset of the collision proxy (CCharacter::Init).
    private float homeRange = DEFAULT_HOME_RANGE; // 144 out of 148 are set to 30. (With the others being 10, 20, 25, 100), Appears unused
    private float visionRange = DEFAULT_VISION_RANGE; // 141/148 use 30. Used by CCharacter::CanSeeTarget
    private float visionFov = DEFAULT_VISION_FOV; // 147/148 use this value. The Bone Cruncher uses 1.5707964. Used by CCharacter::IsPointInDialogRange, CCharacter::IsTargetInMissileRange, and CCharacter::CanSeeTarget.
    private float hearRange = DEFAULT_HEAR_RANGE; // 147/148 use 30F, The Bone Cruncher uses 50F. The range which the target entity will be detected within. (MonsterClass::Set_States)
    private float huntRange = DEFAULT_HUNT_RANGE; // 139/148 use 20F, Hiss uses 50F. Appears unused.
    private float defendRange = DEFAULT_DEFEND_RANGE; // 139/148 use 3F, appears unused.
    private float attackRange = DEFAULT_ATTACK_RANGE; // 122/148 use 1F, 15 use 2F, 8 use 3F. Appears unused.
    private float meleeRange = 1F; // 122/148 use 1F, 15 use 2F (20F??), 8 use 3F Used by CCharacter::IsTargetInMeleeRange to determine how far away the entity should be to enter their attack animation.
    private float missileRange = DEFAULT_MISSILE_RANGE; // 1F appears to be the default, on entities which are not ranged. Used by CCharacter::IsTargetInMissileRange
    private int weaponMask; // Used by CCharacter::AttackCallback
    // This represents the health amount to heal for edible bug entities.
    // In other situations, this is either overwritten at runtime (AITemp2 for melee damage, or not used (for ranged damage).
    private int attackStrength = DEFAULT_ATTACK_STRENGTH; // 136/148 use 10, Used by CCharacter::AttackCallback, CFrogCtl::CheckForHealthBug. Set by MonsterClass:Do_Find() -> Seems to be set to the AI Melee Attack Damage (AITemp2)
    private short aggression; // Copied in AISystemClass::Process, ticked in MonsterClass::Set_States. This appears to be a counter. 0xFF means ALWAYS aggressive, anything else will assign a timer to this value when damage occurs, then after the timer reaches 0, the entity will no longer be aggressive.
    private short fleePercent; // 125/128, the fairies have 100, Itty Bitty/Goobler/Snicker have 10. Seems to indicate likelihood of running away (usually into a wall)
    private short guardHome; // Usually 0, but sometimes 50. MonsterClass::Calculate_Goal() contains a goal value for this. This doesn't seem to do anything, even if there may be some code for it.
    private short protectLike; // Usually 0, sometimes 50. Appears unused.
    private short climbHeight; // Usually 0. Seems unused?
    private short fallHeight; // Usually 0. Seems unused?
    private short monsterGroup; // Usually 0. Used in AISystemClass::Process. When this value is not zero, and the character is attacked, I believe all other characters of the same group will act as if they were also attacked.
    private short AITemp2; // Used as MeleeAttackDamage in MonsterClass::Set_States
    private short AITemp3; // Unused. It's assigned as MissileAttackDamage in MonsterClass::Set_States, but this value is never used, in favor of the projectile hit strength instead.
    private short AITemp4; // Used as swim/fly speed by MonsterClass::Anim_Checks seems to be speed for swimming/flying and CCharacter::GoingToWaypoint, after it is casted to float, then divided by the distance to the target.
    private int closeDistance; // 94/148. Appears unused.
    private short dodgePercent; // 121/148 Appears unused.
    private short tauntPercent; // 116/148 MonsterClass::Do_Find -> The percentage chance (100% is always) of playing the taunt sequence (Tnt). This is used in the vanilla game for Snicker goblins, and has been confirmed to work.
    private short attackGoalPercent = DEFAULT_ATTACK_GOAL_PERCENT; // 90/148 Used to cause an entity to start attacking.
    private short wanderGoalPercent; // 129/148 Used to cause an entity to start wandering.
    private short sleepGoalPercent; // 148/148. Not sure if this is used, but it sure doesn't seem to do anything, even on entities with sleep sequences.
    private boolean preferRanged; // 144/148. Used by MonsterClass::Set_States Used in cases where an entity can do both melee and ranged damage, such as the Crossbow Goblin or the Magical General.
    private boolean avoidWater; // Used by MonsterClass::Set_States Cannot enter water (Such as the mosquitos who will teleport above the water). This doesn't seem to do anything unless the type is FLYER.
    private short recoverySpeed = DEFAULT_RECOVERY_SPEED; // 70/148 Appears unused.
    private short meleeAttackSpeed = DEFAULT_MELEE_ATTACK_SPEED; // 71/148 Time in between melee attacks. MonsterClass::Do_Find. Units are probably 1/1000th of a second.
    private short rangedAttackSpeed = DEFAULT_RANGED_ATTACK_SPEED; // Time in between ranged attacks. MonsterClass::Do_Find Units are probably 1/100th of a second.
    private boolean preferRun; // 123/148 I couldn't figure out where this was checked, but it seems to be used, as Gooblers use their run sequence over their walk sequence, and have this set.
    private float activationRange; // 136/148 Appears unused.

    private static final int PADDING_VALUES = 56;
    private static final float DEFAULT_DIALOG_RANGE = 1F;
    private static final int DEFAULT_ATTACK_RATE = -1;
    private static final float DEFAULT_CORE_RANGE = .75F; // Appears unused

    private static final float DEFAULT_HOME_RANGE = 30F;
    private static final float DEFAULT_VISION_RANGE = 30F;
    private static final float DEFAULT_VISION_FOV = 1.2217305F;
    private static final float DEFAULT_HEAR_RANGE = 30F;
    private static final float DEFAULT_HUNT_RANGE = 20F;
    private static final float DEFAULT_DEFEND_RANGE = 3F;
    private static final float DEFAULT_ATTACK_RANGE = 1F;
    private static final float DEFAULT_MISSILE_RANGE = 20F;
    private static final short DEFAULT_ATTACK_STRENGTH = 10;
    private static final short DEFAULT_ATTACK_GOAL_PERCENT = 100;
    private static final short DEFAULT_RECOVERY_SPEED = 10;
    private static final short DEFAULT_MELEE_ATTACK_SPEED = 10;
    private static final short DEFAULT_RANGED_ATTACK_SPEED = 10;
    private static final String UNUSED_COMMENT = "This is unused by the game, but has been included here since it doesn't match the default value.";

    public CharacterParams(@NonNull kcCResourceGeneric resource) {
        super(resource, kcEntityDescType.CHARACTER);
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
    public void addToPropertyList(PropertyListNode propertyList) {
        super.addToPropertyList(propertyList);
        propertyList.add("Character Type", this.characterType);
        if (!doesVectorLookLikeOrigin(this.homePos))
            this.homePos.addToPropertyList(propertyList, "Home Location", 1F);
        if (this.homeRange != DEFAULT_HOME_RANGE)
            propertyList.addFloat("Home Range", this.homeRange);
        if (isNotDefaultValue(this.visionRange, DEFAULT_VISION_RANGE))
            propertyList.addFloat("Vision Range", this.visionRange, newValue -> this.visionRange = newValue);
        if (isNotDefaultValue(this.visionFov, DEFAULT_VISION_FOV))
            propertyList.addFloat("Vision FOV", this.visionFov, newValue -> this.visionFov = newValue);
        if (isNotDefaultValue(this.hearRange, DEFAULT_HEAR_RANGE))
            propertyList.addFloat("Hear Range (Unused)", this.hearRange, newValue -> this.hearRange = newValue);
        if (this.huntRange != DEFAULT_HUNT_RANGE)
            propertyList.addFloat("Hunt Range (Unused)", this.huntRange, newValue -> this.huntRange = newValue);
        if (this.defendRange != DEFAULT_DEFEND_RANGE)
            propertyList.addFloat("Defend Range (Unused)", this.defendRange, newValue -> this.defendRange = newValue);
        if (this.attackRange != DEFAULT_ATTACK_RANGE)
            propertyList.addFloat("Attack Range (Unused)", this.attackRange, newValue -> this.attackRange = newValue);
        propertyList.addFloat("Melee Range", this.meleeRange, newValue -> this.meleeRange = newValue);
        if (this.missileRange != DEFAULT_MISSILE_RANGE)
            propertyList.addFloat("Missile Range", this.missileRange, newValue -> this.missileRange = newValue);
        if (this.weaponMask != 0)
            propertyList.add("Weapon Flags (BitMask)", NumberUtils.toHexString(this.weaponMask));
        if (this.attackStrength != DEFAULT_ATTACK_STRENGTH || isHealthBug())
            propertyList.addInteger(isHealthBug() ? "Heal Amount" : "Attack Strength", this.attackStrength, newValue -> this.attackStrength = newValue);
        propertyList.addShort("Aggression", this.aggression, newValue -> this.aggression = newValue);
        if (this.fleePercent != 0)
            propertyList.addShort("Flee Percent", this.fleePercent, newValue -> this.fleePercent = newValue);
        if (this.guardHome != 0)
            propertyList.addShort("Guard Home (Unused)", this.guardHome, newValue -> this.guardHome = newValue);
        if (this.protectLike != 0)
            propertyList.addShort("Protect Like (Unused)", this.protectLike, newValue -> this.protectLike = newValue);
        if (this.climbHeight != 0)
            propertyList.addShort("Climb Height (Unused)", this.climbHeight, newValue -> this.climbHeight = newValue);
        if (this.fallHeight != 0)
            propertyList.addShort("Fall Height (Unused)", this.fallHeight, newValue -> this.fallHeight = newValue);
        if (this.monsterGroup != 0)
            propertyList.addShort("Monster Group", this.monsterGroup, newValue -> this.monsterGroup = newValue);
        propertyList.addShort("Melee Damage", this.AITemp2, newValue -> this.AITemp2 = newValue);
        if (this.AITemp3 != 0)
            propertyList.addShort("Range Damage", this.AITemp3, newValue -> this.AITemp3 = newValue);
        if (this.AITemp4 != 0 || this.characterType == CharacterType.FLYER || this.characterType == CharacterType.SWIMMER)
            propertyList.addShort("Fly/Swim Speed", this.AITemp4, newValue -> this.AITemp4 = newValue);
        if (this.closeDistance != 0)
            propertyList.addInteger("Close Distance (Unused)", this.closeDistance, newValue -> this.closeDistance = newValue);
        if (this.dodgePercent != 0)
            propertyList.addShort("Dodge Percent (Unused)", this.dodgePercent, newValue -> this.dodgePercent = newValue);
        if (this.tauntPercent != 0)
            propertyList.addShort("Taunt Percent", this.tauntPercent, newValue -> this.tauntPercent = newValue);
        propertyList.addShort("Attack Percent", this.attackGoalPercent, newValue -> this.attackGoalPercent = newValue);
        if (this.wanderGoalPercent != 0)
            propertyList.addShort("Wander Percent", this.wanderGoalPercent, newValue -> this.wanderGoalPercent = newValue);
        if (this.sleepGoalPercent != 0)
            propertyList.addShort("Sleep Percent (Unused)", this.sleepGoalPercent, newValue -> this.sleepGoalPercent = newValue);
        if (this.preferRanged)
            propertyList.addBoolean("Prefer Ranged", true, newValue -> this.preferRanged = newValue);
        if (this.avoidWater || (this.characterType == CharacterType.FLYER))
            propertyList.addBoolean("Avoid Water", this.avoidWater, newValue -> this.avoidWater = newValue);
        if (this.recoverySpeed != DEFAULT_RECOVERY_SPEED)
            propertyList.addShort("Recovery Speed (Unused)", this.recoverySpeed, newValue -> this.recoverySpeed = newValue);
        if (this.meleeAttackSpeed != DEFAULT_MELEE_ATTACK_SPEED)
            propertyList.addShort("Melee Attack Speed", this.meleeAttackSpeed, newValue -> this.meleeAttackSpeed = newValue);
        if (this.rangedAttackSpeed != DEFAULT_RANGED_ATTACK_SPEED)
            propertyList.addShort("Ranged Attack Speed", this.rangedAttackSpeed, newValue -> this.rangedAttackSpeed = newValue);
        if (this.preferRun || this.characterType == CharacterType.WALKER)
            propertyList.addBoolean("Prefer Run", this.preferRun, newValue -> this.preferRun = newValue);
        if (this.activationRange != 0F)
            propertyList.addFloat("Activation Range (Unused)", this.activationRange, newValue -> this.activationRange = newValue);
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

    private static boolean getOptionalBoolean(Config input, String key) {
        ConfigValueNode node = input.getOptionalKeyValueNode(key);
        return node != null && node.getAsBoolean();
    }

    @Override
    public void fromConfig(ILogger logger, Config input) {
        super.fromConfig(logger, input);
        this.characterType = input.getKeyValueNodeOrError(CONFIG_KEY_CHARACTER_TYPE).getAsEnumOrError(CharacterType.class);

        // Parse homePos if feasible.
        ConfigValueNode homePosNode = input.getOptionalKeyValueNode(CONFIG_KEY_HOME_POSITION);
        if (homePosNode != null) {
            this.homePos.parse(homePosNode.getAsString(), 1);
        } else {
            this.homePos.setXYZW(0F, 0F, 0F, 1F);
        }

        this.homeRange = getOptionalFloat(input, CONFIG_KEY_HOME_RANGE, DEFAULT_HOME_RANGE);
        this.visionRange = getOptionalFloat(input, CONFIG_KEY_VISION_RANGE, DEFAULT_VISION_RANGE);
        this.visionFov = getOptionalFloat(input, CONFIG_KEY_VISION_FOV, DEFAULT_VISION_FOV);
        this.hearRange = getOptionalFloat(input, CONFIG_KEY_HEAR_RANGE, DEFAULT_HEAR_RANGE);
        this.huntRange = getOptionalFloat(input, CONFIG_KEY_HUNT_RANGE, DEFAULT_HUNT_RANGE);
        this.defendRange = getOptionalFloat(input, CONFIG_KEY_DEFEND_RANGE, DEFAULT_DEFEND_RANGE);
        this.attackRange = getOptionalFloat(input, CONFIG_KEY_ATTACK_RANGE, DEFAULT_ATTACK_RANGE);
        this.meleeRange = input.getKeyValueNodeOrError(CONFIG_KEY_MELEE_RANGE).getAsFloat();
        this.missileRange = getOptionalFloat(input, CONFIG_KEY_MISSILE_RANGE, DEFAULT_MISSILE_RANGE);
        String damageFlags = input.getKeyValueNodeOrError(CONFIG_KEY_WEAPON_MASK).getAsString();
        this.weaponMask = kcDamageType.getValueFromArguments(OptionalArguments.parseCommaSeparatedNamedArguments(damageFlags));
        if (isHealthBug()) {
            this.attackStrength = input.getKeyValueNodeOrError(CONFIG_KEY_ATTACK_STRENGTH).getAsInteger();
        } else {
            this.attackStrength = getOptionalInt(input, CONFIG_KEY_ATTACK_STRENGTH, DEFAULT_ATTACK_STRENGTH);
        }
        this.aggression = (short) input.getKeyValueNodeOrError(CONFIG_KEY_AGGRESSION).getAsInteger();
        this.fleePercent = (short) getOptionalInt(input, CONFIG_KEY_FLEE_PERCENT, 0);
        this.guardHome = (short) getOptionalInt(input, CONFIG_KEY_GUARD_HOME, 0);
        this.protectLike = (short) getOptionalInt(input, CONFIG_KEY_PROTECT_LIKE, 0);
        this.climbHeight = (short) getOptionalInt(input, CONFIG_KEY_CLIMB_HEIGHT, 0);
        this.fallHeight = (short) getOptionalInt(input, CONFIG_KEY_FALL_HEIGHT, 0);
        this.monsterGroup = (short) getOptionalInt(input, CONFIG_KEY_MONSTER_GROUP, 0);
        this.AITemp2 = (short) input.getKeyValueNodeOrError(CONFIG_KEY_AI_TEMP2).getAsInteger();
        this.AITemp3 = (short) getOptionalInt(input, CONFIG_KEY_AI_TEMP3, 0);
        this.AITemp4 = (short) getOptionalInt(input, CONFIG_KEY_AI_TEMP4, 0);
        this.closeDistance = (short) getOptionalInt(input, CONFIG_KEY_CLOSE_DISTANCE, 0);
        this.dodgePercent = (short) getOptionalInt(input, CONFIG_KEY_DODGE_PERCENT, 0);
        this.tauntPercent = (short) getOptionalInt(input, CONFIG_KEY_TAUNT_PERCENT, 0);
        this.attackGoalPercent = (short) input.getKeyValueNodeOrError(CONFIG_KEY_ATTACK_PERCENT).getAsInteger();
        this.wanderGoalPercent = (short) getOptionalInt(input, CONFIG_KEY_WANDER_PERCENT, 0);
        this.sleepGoalPercent = (short) getOptionalInt(input, CONFIG_KEY_SLEEP_PERCENT, 0);
        this.preferRanged = getOptionalBoolean(input, CONFIG_KEY_PREFER_RANGED);
        this.avoidWater = getOptionalBoolean(input, CONFIG_KEY_AVOID_WATER);
        this.recoverySpeed = (short) getOptionalInt(input, CONFIG_KEY_RECOVERY_SPEED, DEFAULT_RECOVERY_SPEED);
        this.meleeAttackSpeed = (short) getOptionalInt(input, CONFIG_KEY_MELEE_ATTACK_SPEED, DEFAULT_MELEE_ATTACK_SPEED);
        this.rangedAttackSpeed = (short) getOptionalInt(input, CONFIG_KEY_RANGED_ATTACK_SPEED, DEFAULT_RANGED_ATTACK_SPEED);
        this.preferRun = getOptionalBoolean(input, CONFIG_KEY_PREFER_RUN);
        this.activationRange = getOptionalFloat(input, CONFIG_KEY_ACTIVATION_RANGE, 0F);
    }

    @Override
    public void toConfig(Config output, kcScriptDisplaySettings settings) {
        super.toConfig(output, settings);

        // If the value is unused OR rarely used/has unhelpful functionality, we tend to hide it by default.
        output.getOrCreateKeyValueNode(CONFIG_KEY_CHARACTER_TYPE).setAsEnum(this.characterType);
        if (!doesVectorLookLikeOrigin(this.homePos))
            output.getOrCreateKeyValueNode(CONFIG_KEY_HOME_POSITION).setAsString(this.homePos.toParseableString(1));
        createUnusedFloatConfigEntry(output, CONFIG_KEY_HOME_RANGE, this.homeRange, DEFAULT_HOME_RANGE);
        if (isNotDefaultValue(this.visionRange, DEFAULT_VISION_RANGE))
            output.getOrCreateKeyValueNode(CONFIG_KEY_VISION_RANGE).setAsFloat(this.visionRange);
        if (isNotDefaultValue(this.visionFov, DEFAULT_VISION_FOV))
            output.getOrCreateKeyValueNode(CONFIG_KEY_VISION_FOV).setAsFloat(this.visionFov);
        if (isNotDefaultValue(this.hearRange, DEFAULT_HEAR_RANGE))
            output.getOrCreateKeyValueNode(CONFIG_KEY_HEAR_RANGE).setAsFloat(this.hearRange);
        createUnusedFloatConfigEntry(output, CONFIG_KEY_HUNT_RANGE, this.huntRange, DEFAULT_HUNT_RANGE);
        createUnusedFloatConfigEntry(output, CONFIG_KEY_DEFEND_RANGE, this.defendRange, DEFAULT_DEFEND_RANGE);
        createUnusedFloatConfigEntry(output, CONFIG_KEY_ATTACK_RANGE, this.attackRange, DEFAULT_ATTACK_RANGE);
        output.getOrCreateKeyValueNode(CONFIG_KEY_MELEE_RANGE).setAsFloat(this.meleeRange);
        if (this.missileRange != DEFAULT_MISSILE_RANGE)
            output.getOrCreateKeyValueNode(CONFIG_KEY_MISSILE_RANGE).setAsFloat(this.missileRange);
        output.getOrCreateKeyValueNode(CONFIG_KEY_WEAPON_MASK).setAsString(kcDamageType.getFlagsAsString(this.weaponMask));
        if (isHealthBug()) {
            // Health bugs use this.
            output.getOrCreateKeyValueNode(CONFIG_KEY_ATTACK_STRENGTH).setAsInteger(this.attackStrength);
        } else  {
            createUnusedIntConfigEntry(output, CONFIG_KEY_ATTACK_STRENGTH, this.attackStrength, DEFAULT_ATTACK_STRENGTH);
        }
        output.getOrCreateKeyValueNode(CONFIG_KEY_AGGRESSION).setAsInteger(this.aggression);
        if (this.fleePercent != 0)
            output.getOrCreateKeyValueNode(CONFIG_KEY_FLEE_PERCENT).setAsInteger(this.fleePercent);
        createUnusedIntConfigEntry(output, CONFIG_KEY_GUARD_HOME, this.guardHome, 0);
        if (this.tauntPercent != 0)
            output.getOrCreateKeyValueNode(CONFIG_KEY_TAUNT_PERCENT).setAsInteger(this.tauntPercent);
        output.getOrCreateKeyValueNode(CONFIG_KEY_ATTACK_PERCENT).setAsInteger(this.attackGoalPercent);
        if (this.wanderGoalPercent != 0)
            output.getOrCreateKeyValueNode(CONFIG_KEY_WANDER_PERCENT).setAsInteger(this.wanderGoalPercent);
        createUnusedIntConfigEntry(output, CONFIG_KEY_SLEEP_PERCENT, this.sleepGoalPercent, 0);
        if (this.preferRanged)
            output.getOrCreateKeyValueNode(CONFIG_KEY_PREFER_RANGED).setAsBoolean(this.preferRanged);
        if (this.avoidWater || (this.characterType == CharacterType.FLYER))
            output.getOrCreateKeyValueNode(CONFIG_KEY_AVOID_WATER).setAsBoolean(this.avoidWater);
        createUnusedIntConfigEntry(output, CONFIG_KEY_PROTECT_LIKE, this.protectLike, 0);
        createUnusedIntConfigEntry(output, CONFIG_KEY_CLIMB_HEIGHT, this.climbHeight, 0);
        createUnusedIntConfigEntry(output, CONFIG_KEY_FALL_HEIGHT, this.fallHeight, 0);
        if (this.monsterGroup != 0)
            output.getOrCreateKeyValueNode(CONFIG_KEY_MONSTER_GROUP).setAsInteger(this.monsterGroup);
        output.getOrCreateKeyValueNode(CONFIG_KEY_AI_TEMP2).setAsInteger(this.AITemp2);
        createUnusedIntConfigEntry(output, CONFIG_KEY_AI_TEMP3, this.AITemp3, 0);
        if (this.AITemp4 != 0 || this.characterType == CharacterType.FLYER || this.characterType == CharacterType.SWIMMER) // flyOrSwimSpeed
            output.getOrCreateKeyValueNode(CONFIG_KEY_AI_TEMP4).setAsInteger(this.AITemp4);
        createUnusedIntConfigEntry(output, CONFIG_KEY_CLOSE_DISTANCE, this.closeDistance, 0);
        createUnusedIntConfigEntry(output, CONFIG_KEY_DODGE_PERCENT, this.dodgePercent, 0);
        createUnusedIntConfigEntry(output, CONFIG_KEY_RECOVERY_SPEED, this.recoverySpeed, DEFAULT_RECOVERY_SPEED);
        if (this.meleeAttackSpeed != DEFAULT_MELEE_ATTACK_SPEED)
            output.getOrCreateKeyValueNode(CONFIG_KEY_MELEE_ATTACK_SPEED).setAsInteger(this.meleeAttackSpeed);
        if (this.rangedAttackSpeed != DEFAULT_RANGED_ATTACK_SPEED)
            output.getOrCreateKeyValueNode(CONFIG_KEY_RANGED_ATTACK_SPEED).setAsInteger(this.rangedAttackSpeed);
        if (this.preferRun || this.characterType == CharacterType.WALKER)
            output.getOrCreateKeyValueNode(CONFIG_KEY_PREFER_RUN).setAsBoolean(this.preferRun);
        createUnusedFloatConfigEntry(output, CONFIG_KEY_ACTIVATION_RANGE, this.activationRange, 0F);
    }

    private static boolean isNotDefaultValue(float testValue, float defaultValue) {
        return !Float.isFinite(testValue) || (Math.abs(testValue - defaultValue) >= .00001);
    }

    private static void createUnusedFloatConfigEntry(Config output, String key, float testValue, float defaultValue) {
        if (isNotDefaultValue(testValue, defaultValue)) {
            ConfigValueNode node = output.getOrCreateKeyValueNode(key);
            node.setAsFloat(testValue);
            node.setComment(UNUSED_COMMENT);
        }
    }

    private static void createUnusedIntConfigEntry(Config output, String key, int testValue, int defaultValue) {
        if (testValue != defaultValue) {
            ConfigValueNode node = output.getOrCreateKeyValueNode(key);
            node.setAsInteger(testValue);
            node.setComment(UNUSED_COMMENT);
        }
    }

    private static boolean doesVectorLookLikeOrigin(kcVector4 vector) {
        return Math.abs(vector.getX()) < .00001 && Math.abs(vector.getY()) < .00001 && Math.abs(vector.getZ()) < .00001;
    }

    /**
     * Test if this describes a health bug.
     */
    public boolean isHealthBug() {
        String name = getParentHash().getOriginalString();
        if (name == null)
            return false;

        // Nabbed from CFrogCtl::CheckForHealthBug().
        return name.startsWith("GroundBug") || name.startsWith("FlyingBug") || name.startsWith("WaterBug") || name.startsWith("Tropical");
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