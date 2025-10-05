package net.highwayfrogs.editor.games.konami.greatquest.entity;

import lombok.Getter;
import lombok.Setter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.games.generic.data.GameData;
import net.highwayfrogs.editor.games.konami.IConfigData;
import net.highwayfrogs.editor.games.konami.greatquest.GreatQuestInstance;
import net.highwayfrogs.editor.games.konami.greatquest.IInfoWriter.IMultiLineInfoWriter;
import net.highwayfrogs.editor.games.konami.greatquest.proxy.ProxyReact;
import net.highwayfrogs.editor.system.Config;
import net.highwayfrogs.editor.utils.NumberUtils;
import net.highwayfrogs.editor.utils.data.reader.DataReader;
import net.highwayfrogs.editor.utils.data.writer.DataWriter;
import net.highwayfrogs.editor.utils.logging.ILogger;

/**
 * Represents the 'kcProjectileParams' struct.
 * Created by Kneesnap on 8/21/2023.
 */
@Getter
@Setter
public class kcProjectileParams extends GameData<GreatQuestInstance> implements IMultiLineInfoWriter, IConfigData {
    private ProxyReact proxyReact = ProxyReact.NOTIFY;
    private float sensorRadius;
    private int group; // Bone ID? Not sure.
    private int focus;
    private float mass;
    private float gravity;
    private int flags;
    private float retainBounce;
    private float retainSlide;
    private int damageFlags;
    private int hitStrength;
    private static final int PADDING_VALUES = 8;

    public kcProjectileParams(GreatQuestInstance instance) {
        super(instance);
    }

    @Override
    public void load(DataReader reader) {
        this.proxyReact = ProxyReact.getReaction(reader.readInt(), false);
        this.sensorRadius = reader.readFloat();
        this.group = reader.readInt();
        this.focus = reader.readInt();
        this.mass = reader.readFloat();
        this.gravity = reader.readFloat();
        this.flags = reader.readInt();
        this.retainBounce = reader.readFloat();
        this.retainSlide = reader.readFloat();
        this.damageFlags = reader.readInt();
        this.hitStrength = reader.readInt();
        reader.skipBytesRequireEmpty(PADDING_VALUES * Constants.INTEGER_SIZE);
    }

    @Override
    public void save(DataWriter writer) {
        writer.writeInt(this.proxyReact.ordinal());
        writer.writeFloat(this.sensorRadius);
        writer.writeInt(this.group);
        writer.writeInt(this.focus);
        writer.writeFloat(this.mass);
        writer.writeFloat(this.gravity);
        writer.writeInt(this.flags);
        writer.writeFloat(this.retainBounce);
        writer.writeFloat(this.retainSlide);
        writer.writeInt(this.damageFlags);
        writer.writeInt(this.hitStrength);
        writer.writeNull(PADDING_VALUES * Constants.INTEGER_SIZE);
    }

    @Override
    public void writeMultiLineInfo(StringBuilder builder, String padding) {
        builder.append(padding).append("Flags: ").append(NumberUtils.toHexString(this.flags)).append(Constants.NEWLINE);
        builder.append(padding).append("Hit Strength: ").append(this.hitStrength)
                .append(", Damage Flags: ").append(NumberUtils.toHexString(this.damageFlags)).append(Constants.NEWLINE);
        builder.append(padding).append("Collision Proxy Reaction: ").append(this.proxyReact).append(Constants.NEWLINE);
        builder.append(padding).append("Sensor Radius: ").append(this.sensorRadius).append(Constants.NEWLINE);
        builder.append(padding).append("Group: ").append(this.group).append(", Focus: ").append(this.focus).append(Constants.NEWLINE);
        builder.append(padding).append("Mass: ").append(this.mass).append(", Gravity: ").append(this.gravity).append(Constants.NEWLINE);
        builder.append(padding).append("Retain Bounce: ").append(this.retainBounce)
                .append(", Retain Slide: ").append(this.retainSlide).append(Constants.NEWLINE);
    }

    /**
     * Sets the reaction type.
     * @param reactionType The reaction type to apply.
     */
    @SuppressWarnings("unused") // Available to Noodle scripts.
    public void setReactionType(ProxyReact reactionType) {
        if (reactionType == null)
            throw new NullPointerException("reactionType");

        this.proxyReact = reactionType;
    }

    @Override
    public void fromConfig(ILogger logger, Config input) {
        this.proxyReact = input.getKeyValueNodeOrError(CONFIG_KEY_PROXY_REACT).getAsEnumOrError(ProxyReact.class);
        this.sensorRadius = input.getKeyValueNodeOrError(CONFIG_KEY_SENSOR_RADIUS).getAsFloat();
        this.group = input.getKeyValueNodeOrError(CONFIG_KEY_GROUP).getAsInteger();
        this.focus = input.getKeyValueNodeOrError(CONFIG_KEY_FOCUS).getAsInteger();
        this.mass = input.getKeyValueNodeOrError(CONFIG_KEY_MASS).getAsFloat();
        this.gravity = input.getKeyValueNodeOrError(CONFIG_KEY_GRAVITY).getAsFloat();
        this.flags = input.getKeyValueNodeOrError(CONFIG_KEY_FLAGS).getAsInteger();
        this.retainBounce = input.getKeyValueNodeOrError(CONFIG_KEY_RETAIN_BOUNCE).getAsFloat();
        this.retainSlide = input.getKeyValueNodeOrError(CONFIG_KEY_RETAIN_SLIDE).getAsFloat();
        this.damageFlags = input.getKeyValueNodeOrError(CONFIG_KEY_DAMAGE_FLAGS).getAsInteger();
        this.hitStrength = input.getKeyValueNodeOrError(CONFIG_KEY_HIT_STRENGTH).getAsInteger();
    }

    private static final String CONFIG_KEY_PROXY_REACT = "proxyReact";
    private static final String CONFIG_KEY_SENSOR_RADIUS = "sensorRadius";
    private static final String CONFIG_KEY_GROUP = "group";
    private static final String CONFIG_KEY_FOCUS = "focus";
    private static final String CONFIG_KEY_MASS = "mass";
    private static final String CONFIG_KEY_GRAVITY = "gravity";
    private static final String CONFIG_KEY_FLAGS = "flags";
    private static final String CONFIG_KEY_RETAIN_BOUNCE = "retainBounce";
    private static final String CONFIG_KEY_RETAIN_SLIDE = "retainSlide";
    private static final String CONFIG_KEY_DAMAGE_FLAGS = "damageFlags";
    private static final String CONFIG_KEY_HIT_STRENGTH = "hitStrength";

    @Override
    public void toConfig(Config output) {
        output.getOrCreateKeyValueNode(CONFIG_KEY_PROXY_REACT).setAsEnum(this.proxyReact);
        output.getOrCreateKeyValueNode(CONFIG_KEY_SENSOR_RADIUS).setAsFloat(this.sensorRadius);
        output.getOrCreateKeyValueNode(CONFIG_KEY_GROUP).setAsInteger(this.group);
        output.getOrCreateKeyValueNode(CONFIG_KEY_FOCUS).setAsInteger(this.focus);
        output.getOrCreateKeyValueNode(CONFIG_KEY_MASS).setAsFloat(this.mass);
        output.getOrCreateKeyValueNode(CONFIG_KEY_GRAVITY).setAsFloat(this.gravity);
        output.getOrCreateKeyValueNode(CONFIG_KEY_FLAGS).setAsInteger(this.flags);
        output.getOrCreateKeyValueNode(CONFIG_KEY_RETAIN_BOUNCE).setAsFloat(this.retainBounce);
        output.getOrCreateKeyValueNode(CONFIG_KEY_RETAIN_SLIDE).setAsFloat(this.retainSlide);
        output.getOrCreateKeyValueNode(CONFIG_KEY_DAMAGE_FLAGS).setAsInteger(this.damageFlags);
        output.getOrCreateKeyValueNode(CONFIG_KEY_HIT_STRENGTH).setAsInteger(this.hitStrength);
    }
}