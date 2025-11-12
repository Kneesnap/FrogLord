package net.highwayfrogs.editor.games.konami.greatquest.entity;

import lombok.Getter;
import lombok.Setter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.games.generic.data.IBinarySerializable;
import net.highwayfrogs.editor.games.konami.IConfigData;
import net.highwayfrogs.editor.games.konami.greatquest.GreatQuestInstance;
import net.highwayfrogs.editor.games.konami.greatquest.GreatQuestUtils;
import net.highwayfrogs.editor.games.konami.greatquest.map.kcColor4;
import net.highwayfrogs.editor.games.konami.greatquest.math.kcVector4;
import net.highwayfrogs.editor.gui.components.propertylist.IPropertyListCreator;
import net.highwayfrogs.editor.gui.components.propertylist.PropertyListNode;
import net.highwayfrogs.editor.system.Config;
import net.highwayfrogs.editor.utils.data.reader.DataReader;
import net.highwayfrogs.editor.utils.data.writer.DataWriter;
import net.highwayfrogs.editor.utils.logging.ILogger;

/**
 * Represents the 'kcParticleParam' struct.
 * Default values obtained from 'kcParticleParam::__ct'.
 * Created by Kneesnap on 8/22/2023.
 */
@Getter
@Setter
public class kcParticleParam implements IPropertyListCreator, IBinarySerializable, IConfigData {
    private int burstMode = 1;
    private float emitAngle = .55F;
    private float emitAngleVariance = 0F;
    private int partPerSecond = 940;
    private float speed = 24.25F;
    private float speedVariance = 0F;
    private float lifeTime = 1.65F;
    private float lifeVariance = 2.1F;
    private float sizeBegin = 1.2F;
    private float sizeEnd = 1.4F;
    private float sizeVariance = .04F;
    private final kcColor4 colorBegin = new kcColor4(.7F, .94F, .94F, .97F);
    private final kcColor4 colorEnd = new kcColor4(.21F, .04F, .67F, .98F);
    private final kcColor4 colorVariance = new kcColor4(.01F, .01F, .01F, .01F);
    private final kcVector4 gravityBegin = new kcVector4(0F, .28F, 0F, 0F);
    private final kcVector4 gravityEnd = new kcVector4(0F, -0.2F, 0F, 0F);
    private float gravityVariance = 0F;
    private float lineLeft = 0F; // Range: [0, 20] (Seen in kcCParticleEmitter::SetParticleDefaults)
    private float lineRight = 0F; // Range: [0, 20] (Seen in kcCParticleEmitter::SetParticleDefaults)
    private float orientation = 0F;
    private static final int PADDING_VALUES = 5;

    @Override
    public void load(DataReader reader) {
        this.burstMode = reader.readInt();
        this.emitAngle = reader.readFloat();
        this.emitAngleVariance = reader.readFloat();
        this.partPerSecond = reader.readInt();
        this.speed = reader.readFloat();
        this.speedVariance = reader.readFloat();
        this.lifeTime = reader.readFloat();
        this.lifeVariance = reader.readFloat();
        this.sizeBegin = reader.readFloat();
        this.sizeEnd = reader.readFloat();
        this.sizeVariance = reader.readFloat();
        this.colorBegin.load(reader);
        this.colorEnd.load(reader);
        this.colorVariance.load(reader);
        this.gravityBegin.load(reader);
        this.gravityEnd.load(reader);
        this.gravityVariance = reader.readFloat();
        this.lineLeft = reader.readFloat();
        this.lineRight = reader.readFloat();
        this.orientation = reader.readFloat();
        GreatQuestUtils.skipPaddingRequireEmptyOrByte(reader, PADDING_VALUES * Constants.INTEGER_SIZE, GreatQuestInstance.PADDING_BYTE_DEFAULT);
    }

    @Override
    public void save(DataWriter writer) {
        writer.writeInt(this.burstMode);
        writer.writeFloat(this.emitAngle);
        writer.writeFloat(this.emitAngleVariance);
        writer.writeInt(this.partPerSecond);
        writer.writeFloat(this.speed);
        writer.writeFloat(this.speedVariance);
        writer.writeFloat(this.lifeTime);
        writer.writeFloat(this.lifeVariance);
        writer.writeFloat(this.sizeBegin);
        writer.writeFloat(this.sizeEnd);
        writer.writeFloat(this.sizeVariance);
        this.colorBegin.save(writer);
        this.colorEnd.save(writer);
        this.colorVariance.save(writer);
        this.gravityBegin.save(writer);
        this.gravityEnd.save(writer);
        writer.writeFloat(this.gravityVariance);
        writer.writeFloat(this.lineLeft);
        writer.writeFloat(this.lineRight);
        writer.writeFloat(this.orientation);
        writer.writeNull(PADDING_VALUES * Constants.INTEGER_SIZE); // It seems older versions used 0xCC padding bytes. However, the retail versions use null bytes. Old files still contain the old padding
    }

    @Override
    public void addToPropertyList(PropertyListNode propertyList) {
        propertyList.addInteger("Burst Mode", this.burstMode, newValue -> this.burstMode = newValue);
        propertyList.addFloat("Emit Angle", this.emitAngle, newValue -> this.emitAngle = newValue);
        propertyList.addFloat("Emit Angle Variance", this.emitAngleVariance, newValue -> this.emitAngleVariance = newValue);
        propertyList.addInteger("Part Per Second", this.partPerSecond, newValue -> this.partPerSecond = newValue);
        propertyList.addFloat("Speed", this.speed, newValue -> this.speed = newValue);
        propertyList.addFloat("Speed Variance", this.speedVariance, newValue -> this.speedVariance = newValue);
        propertyList.addFloat("Life Time", this.lifeTime, newValue -> this.lifeTime = newValue);
        propertyList.addFloat("Life Variance", this.lifeVariance, newValue -> this.lifeVariance = newValue);
        propertyList.addFloat("Size Begin", this.sizeBegin, newValue -> this.sizeBegin = newValue);
        propertyList.addFloat("Size End", this.sizeEnd, newValue -> this.sizeEnd = newValue);
        propertyList.addFloat("Size Variance", this.sizeVariance, newValue -> this.sizeVariance = newValue);
        this.colorBegin.addToPropertyList(propertyList, "Color Begin");
        this.colorEnd.addToPropertyList(propertyList, "Color End");
        this.colorVariance.addToPropertyList(propertyList, "Color Variance");
        this.gravityBegin.addToPropertyList(propertyList, "Gravity Begin");
        this.gravityEnd.addToPropertyList(propertyList, "Gravity End");
        propertyList.addFloat("Gravity Variance", this.gravityVariance, newValue -> this.gravityVariance = newValue);
        propertyList.addFloat("Line Left", this.lineLeft, newValue -> this.lineLeft = newValue);
        propertyList.addFloat("Line Right", this.lineRight, newValue -> this.lineRight = newValue);
        propertyList.addFloat("Orientation", this.orientation, newValue -> this.orientation = newValue);
    }

    private static final String CONFIG_KEY_BURST_MODE = "burstMode";
    private static final String CONFIG_KEY_EMIT_ANGLE = "emitAngle";
    private static final String CONFIG_KEY_EMIT_ANGLE_VARIANCE = "emitAngleVariance";
    private static final String CONFIG_KEY_PART_PER_SECOND = "partPerSecond";
    private static final String CONFIG_KEY_SPEED = "speed";
    private static final String CONFIG_KEY_SPEED_VARIANCE = "speedVariance";
    private static final String CONFIG_KEY_LIFE_TIME = "lifeTime";
    private static final String CONFIG_KEY_LIFE_VARIANCE = "lifeVariance";
    private static final String CONFIG_KEY_SIZE_BEGIN = "sizeBegin";
    private static final String CONFIG_KEY_SIZE_END = "sizeEnd";
    private static final String CONFIG_KEY_SIZE_VARIANCE = "sizeVariance";
    private static final String CONFIG_KEY_COLOR_BEGIN = "colorBegin";
    private static final String CONFIG_KEY_COLOR_END = "colorEnd";
    private static final String CONFIG_KEY_COLOR_VARIANCE = "colorVariance";
    private static final String CONFIG_KEY_GRAVITY_BEGIN = "gravityBegin";
    private static final String CONFIG_KEY_GRAVITY_END = "gravityEnd";
    private static final String CONFIG_KEY_GRAVITY_VARIANCE = "gravityVariance";
    private static final String CONFIG_KEY_LINE_LEFT = "lineLeft";
    private static final String CONFIG_KEY_LINE_RIGHT = "lineRight";
    private static final String CONFIG_KEY_ORIENTATION = "orientation";

    @Override
    public void fromConfig(ILogger logger, Config input) {
        this.burstMode = input.getKeyValueNodeOrError(CONFIG_KEY_BURST_MODE).getAsInteger();
        this.emitAngle = input.getKeyValueNodeOrError(CONFIG_KEY_EMIT_ANGLE).getAsFloat();
        this.emitAngleVariance = input.getKeyValueNodeOrError(CONFIG_KEY_EMIT_ANGLE_VARIANCE).getAsFloat();
        this.partPerSecond = input.getKeyValueNodeOrError(CONFIG_KEY_PART_PER_SECOND).getAsInteger();
        this.speed = input.getKeyValueNodeOrError(CONFIG_KEY_SPEED).getAsFloat();
        this.speedVariance = input.getKeyValueNodeOrError(CONFIG_KEY_SPEED_VARIANCE).getAsFloat();
        this.lifeTime = input.getKeyValueNodeOrError(CONFIG_KEY_LIFE_TIME).getAsFloat();
        this.lifeVariance = input.getKeyValueNodeOrError(CONFIG_KEY_LIFE_VARIANCE).getAsFloat();
        this.sizeBegin = input.getKeyValueNodeOrError(CONFIG_KEY_SIZE_BEGIN).getAsFloat();
        this.sizeEnd = input.getKeyValueNodeOrError(CONFIG_KEY_SIZE_END).getAsFloat();
        this.sizeVariance = input.getKeyValueNodeOrError(CONFIG_KEY_SIZE_VARIANCE).getAsFloat();
        this.colorBegin.fromARGB(input.getKeyValueNodeOrError(CONFIG_KEY_COLOR_BEGIN).getAsInteger());
        this.colorEnd.fromARGB(input.getKeyValueNodeOrError(CONFIG_KEY_COLOR_END).getAsInteger());
        this.colorVariance.fromARGB(input.getKeyValueNodeOrError(CONFIG_KEY_COLOR_VARIANCE).getAsInteger());
        this.gravityBegin.parse(input.getKeyValueNodeOrError(CONFIG_KEY_GRAVITY_BEGIN).getAsString(), 1F);
        this.gravityEnd.parse(input.getKeyValueNodeOrError(CONFIG_KEY_GRAVITY_END).getAsString(), 1F);
        this.gravityVariance = input.getKeyValueNodeOrError(CONFIG_KEY_GRAVITY_VARIANCE).getAsFloat();
        this.lineLeft = input.getKeyValueNodeOrError(CONFIG_KEY_LINE_LEFT).getAsFloat();
        this.lineRight = input.getKeyValueNodeOrError(CONFIG_KEY_LINE_RIGHT).getAsFloat();
        this.orientation = input.getKeyValueNodeOrError(CONFIG_KEY_ORIENTATION).getAsFloat();
    }

    @Override
    public void toConfig(Config output) {
        output.getOrCreateKeyValueNode(CONFIG_KEY_BURST_MODE).setAsInteger(this.burstMode);
        output.getOrCreateKeyValueNode(CONFIG_KEY_EMIT_ANGLE).setAsFloat(this.emitAngle);
        output.getOrCreateKeyValueNode(CONFIG_KEY_EMIT_ANGLE_VARIANCE).setAsFloat(this.emitAngleVariance);
        output.getOrCreateKeyValueNode(CONFIG_KEY_PART_PER_SECOND).setAsInteger(this.partPerSecond);
        output.getOrCreateKeyValueNode(CONFIG_KEY_SPEED).setAsFloat(this.speed);
        output.getOrCreateKeyValueNode(CONFIG_KEY_SPEED_VARIANCE).setAsFloat(this.speedVariance);
        output.getOrCreateKeyValueNode(CONFIG_KEY_LIFE_TIME).setAsFloat(this.lifeTime);
        output.getOrCreateKeyValueNode(CONFIG_KEY_LIFE_VARIANCE).setAsFloat(this.lifeVariance);
        output.getOrCreateKeyValueNode(CONFIG_KEY_SIZE_BEGIN).setAsFloat(this.sizeBegin);
        output.getOrCreateKeyValueNode(CONFIG_KEY_SIZE_END).setAsFloat(this.sizeEnd);
        output.getOrCreateKeyValueNode(CONFIG_KEY_SIZE_VARIANCE).setAsFloat(this.sizeVariance);
        output.getOrCreateKeyValueNode(CONFIG_KEY_COLOR_BEGIN).setAsInteger(this.colorBegin.toARGB());
        output.getOrCreateKeyValueNode(CONFIG_KEY_COLOR_END).setAsInteger(this.colorEnd.toARGB());
        output.getOrCreateKeyValueNode(CONFIG_KEY_COLOR_VARIANCE).setAsInteger(this.colorVariance.toARGB());
        output.getOrCreateKeyValueNode(CONFIG_KEY_GRAVITY_BEGIN).setAsString(this.gravityBegin.toParseableString(1F));
        output.getOrCreateKeyValueNode(CONFIG_KEY_GRAVITY_END).setAsString(this.gravityEnd.toParseableString(1F));
        output.getOrCreateKeyValueNode(CONFIG_KEY_GRAVITY_VARIANCE).setAsFloat(this.gravityVariance);
        output.getOrCreateKeyValueNode(CONFIG_KEY_LINE_LEFT).setAsFloat(this.lineLeft);
        output.getOrCreateKeyValueNode(CONFIG_KEY_LINE_RIGHT).setAsFloat(this.lineRight);
        output.getOrCreateKeyValueNode(CONFIG_KEY_ORIENTATION).setAsFloat(this.orientation);
    }
}