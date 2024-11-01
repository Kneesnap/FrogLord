package net.highwayfrogs.editor.games.konami.greatquest.entity;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.konami.greatquest.generic.kcCResourceGeneric;
import net.highwayfrogs.editor.games.konami.greatquest.kcClassID;
import net.highwayfrogs.editor.games.konami.greatquest.math.kcVector4;
import net.highwayfrogs.editor.utils.NumberUtils;

/**
 * Represents the 'CharacterParams' struct.
 * Handled by CCharacter::Init
 * Created by Kneesnap on 8/21/2023.
 */
@Getter
@Setter
public class CharacterParams extends kcActorDesc {
    private CharacterType characterType = CharacterType.NONE;
    private int attributes; // Seems to be pretty much unused. 0x40000000 will setup Frogger's inventory, 0x80000000 will make a health bar. However, these flags are added at runtime if the character type is PLAYER. So, this might be completely useless unless somehow this does something on monsters.
    private final kcVector4 homePos = new kcVector4();
    private float homeRange;
    private float coreRange;
    private float visionRange;
    private float visionFov;
    private float hearRange;
    private float dialogRange = 2.5F; // Ignored when read.
    private float huntRange;
    private float defendRange;
    private float attackRange;
    private float meleeRange;
    private float missileRange;
    private int weaponMask;
    private int attackStrength;
    private int attackRate; // Overwritten to be 3000.
    private short aggression;
    private short fleePercent;
    private short guardHome;
    private short protectLike;
    private short climbHeight;
    private short fallHeight;
    private short monsterGroup;
    private short AITemp2;
    private short AITemp3;
    private short AITemp4;
    private short AITemp5;
    private short AITemp6;
    private int closeDistance;
    private short dodgePercent;
    private short tauntPercent;
    private short attackPercent;
    private short wanderPercent;
    private short sleepPercent;
    private short preferRanged;
    private short avoidWater;
    private short recoverySpeed;
    private short meleeAttackSpeed;
    private short rangedAttackSpeed;
    private short preferRun;
    private short pad1; // Might be unused. Perhaps add a skip require empty?
    private float activationRange;
    private static final int PADDING_VALUES = 56;

    public CharacterParams(@NonNull kcCResourceGeneric resource) {
        super(resource);
    }

    @Override
    protected int getTargetClassID() {
        return kcClassID.CHARACTER.getClassId();
    }

    @Override
    public void load(DataReader reader) {
        super.load(reader);
        int characterDescHash = reader.readInt();
        this.characterType = CharacterType.getType(reader.readInt(), false);
        this.attributes = reader.readInt();
        this.homePos.load(reader);
        this.homeRange = reader.readFloat();
        this.coreRange = reader.readFloat();
        this.visionRange = reader.readFloat();
        this.visionFov = reader.readFloat();
        this.hearRange = reader.readFloat();
        this.dialogRange = reader.readFloat();
        this.huntRange = reader.readFloat();
        this.defendRange = reader.readFloat();
        this.attackRange = reader.readFloat();
        this.meleeRange = reader.readFloat();
        this.missileRange = reader.readFloat();
        this.weaponMask = reader.readInt();
        this.attackStrength = reader.readInt();
        this.attackRate = reader.readInt();
        this.aggression = reader.readUnsignedByteAsShort();
        this.fleePercent = reader.readUnsignedByteAsShort();
        this.guardHome = reader.readUnsignedByteAsShort();
        this.protectLike = reader.readUnsignedByteAsShort();
        this.climbHeight = reader.readUnsignedByteAsShort();
        this.fallHeight = reader.readUnsignedByteAsShort();
        this.monsterGroup = reader.readUnsignedByteAsShort();
        this.AITemp2 = reader.readUnsignedByteAsShort();
        this.AITemp3 = reader.readUnsignedByteAsShort();
        this.AITemp4 = reader.readUnsignedByteAsShort();
        this.AITemp5 = reader.readUnsignedByteAsShort();
        this.AITemp6 = reader.readUnsignedByteAsShort();
        this.closeDistance = reader.readInt();
        this.dodgePercent = reader.readUnsignedByteAsShort();
        this.tauntPercent = reader.readUnsignedByteAsShort();
        this.attackPercent = reader.readUnsignedByteAsShort();
        this.wanderPercent = reader.readUnsignedByteAsShort();
        this.sleepPercent = reader.readUnsignedByteAsShort();
        this.preferRanged = reader.readUnsignedByteAsShort();
        this.avoidWater = reader.readUnsignedByteAsShort();
        this.recoverySpeed = reader.readUnsignedByteAsShort();
        this.meleeAttackSpeed = reader.readUnsignedByteAsShort();
        this.rangedAttackSpeed = reader.readUnsignedByteAsShort();
        this.preferRun = reader.readUnsignedByteAsShort();
        this.pad1 = reader.readUnsignedByteAsShort();
        this.activationRange = reader.readFloat();
        reader.skipBytesRequireEmpty(PADDING_VALUES * Constants.INTEGER_SIZE);

        if (characterDescHash != getParentHash().getHashNumber())
            throw new RuntimeException("The CharacterParams reported the parent chunk as " + NumberUtils.to0PrefixedHexString(characterDescHash) + ", but it was expected to be " + getParentHash().getHashNumberAsString() + ".");

    }

    @Override
    public void saveData(DataWriter writer) {
        super.saveData(writer);
        writer.writeInt(getParentHash().getHashNumber());
        writer.writeInt(this.characterType.ordinal());
        writer.writeInt(this.attributes);
        this.homePos.save(writer);
        writer.writeFloat(this.homeRange);
        writer.writeFloat(this.coreRange);
        writer.writeFloat(this.visionRange);
        writer.writeFloat(this.visionFov);
        writer.writeFloat(this.hearRange);
        writer.writeFloat(this.dialogRange);
        writer.writeFloat(this.huntRange);
        writer.writeFloat(this.defendRange);
        writer.writeFloat(this.attackRange);
        writer.writeFloat(this.meleeRange);
        writer.writeFloat(this.missileRange);
        writer.writeInt(this.weaponMask);
        writer.writeInt(this.attackStrength);
        writer.writeInt(this.attackRate);
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
        writer.writeUnsignedByte(this.AITemp5);
        writer.writeUnsignedByte(this.AITemp6);
        writer.writeInt(this.closeDistance);
        writer.writeUnsignedByte(this.dodgePercent);
        writer.writeUnsignedByte(this.tauntPercent);
        writer.writeUnsignedByte(this.attackPercent);
        writer.writeUnsignedByte(this.wanderPercent);
        writer.writeUnsignedByte(this.sleepPercent);
        writer.writeUnsignedByte(this.preferRanged);
        writer.writeUnsignedByte(this.avoidWater);
        writer.writeUnsignedByte(this.recoverySpeed);
        writer.writeUnsignedByte(this.meleeAttackSpeed);
        writer.writeUnsignedByte(this.rangedAttackSpeed);
        writer.writeUnsignedByte(this.preferRun);
        writer.writeUnsignedByte(this.pad1);
        writer.writeFloat(this.activationRange);
        writer.writeNull(PADDING_VALUES * Constants.INTEGER_SIZE);
    }

    @Override
    public void writeMultiLineInfo(StringBuilder builder, String padding) {
        super.writeMultiLineInfo(builder, padding);
        builder.append(padding).append("Character Type: ").append(this.characterType).append(Constants.NEWLINE);
        builder.append(padding).append("Character Attributes: ").append(this.attributes).append(Constants.NEWLINE);
        this.homePos.writePrefixedInfoLine(builder, "Home Location", padding);
        builder.append(padding).append("Home Range: ").append(this.homeRange).append(Constants.NEWLINE);
        builder.append(padding).append("Core Range: ").append(this.coreRange).append(Constants.NEWLINE);
        builder.append(padding).append("Vision Range: ").append(this.visionRange).append(Constants.NEWLINE);
        builder.append(padding).append("Vision FOV: ").append(this.visionFov).append(Constants.NEWLINE);
        builder.append(padding).append("Hear Range: ").append(this.hearRange).append(Constants.NEWLINE);
        builder.append(padding).append("Dialog Range: ").append(this.dialogRange).append(Constants.NEWLINE);
        builder.append(padding).append("Hunt Range: ").append(this.huntRange).append(Constants.NEWLINE);
        builder.append(padding).append("Defend Range: ").append(this.defendRange).append(Constants.NEWLINE);
        builder.append(padding).append("Attack Range: ").append(this.attackRange).append(Constants.NEWLINE);
        builder.append(padding).append("Melee Range: ").append(this.meleeRange).append(Constants.NEWLINE);
        builder.append(padding).append("Missile Range: ").append(this.missileRange).append(Constants.NEWLINE);
        builder.append(padding).append("Weapon Flags (BitMask): ").append(NumberUtils.toHexString(this.weaponMask)).append(Constants.NEWLINE);
        builder.append(padding).append("Attack Strength: ").append(this.attackStrength).append(Constants.NEWLINE);
        builder.append(padding).append("Attack Rate: ").append(this.attackRate).append(Constants.NEWLINE);
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
        builder.append(padding).append("AI Temp5: ").append(this.AITemp5).append(Constants.NEWLINE);
        builder.append(padding).append("AI Temp6: ").append(this.AITemp6).append(Constants.NEWLINE);
        builder.append(padding).append("Close Distance: ").append(this.closeDistance).append(Constants.NEWLINE);
        builder.append(padding).append("Dodge Percent: ").append(this.dodgePercent).append(Constants.NEWLINE);
        builder.append(padding).append("Taunt Percent: ").append(this.tauntPercent).append(Constants.NEWLINE);
        builder.append(padding).append("Attack Percent: ").append(this.attackPercent).append(Constants.NEWLINE);
        builder.append(padding).append("Wander Percent: ").append(this.wanderPercent).append(Constants.NEWLINE);
        builder.append(padding).append("Sleep Percent: ").append(this.sleepPercent).append(Constants.NEWLINE);
        builder.append(padding).append("Prefer Ranged: ").append(this.preferRanged).append(Constants.NEWLINE);
        builder.append(padding).append("Avoid Water: ").append(this.avoidWater).append(Constants.NEWLINE);
        builder.append(padding).append("Recovery Speed: ").append(this.recoverySpeed).append(Constants.NEWLINE);
        builder.append(padding).append("Melee Attack Speed: ").append(this.meleeAttackSpeed).append(Constants.NEWLINE);
        builder.append(padding).append("Ranged Attack Speed: ").append(this.rangedAttackSpeed).append(Constants.NEWLINE);
        builder.append(padding).append("Prefer Run: ").append(this.preferRanged).append(Constants.NEWLINE);
        builder.append(padding).append("Pad1: ").append(this.pad1).append(Constants.NEWLINE);
        builder.append(padding).append("Activation Range: ").append(this.activationRange).append(Constants.NEWLINE);
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