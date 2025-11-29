package net.highwayfrogs.editor.games.konami.greatquest.entity;

import lombok.Getter;
import lombok.Setter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.games.generic.data.GameData;
import net.highwayfrogs.editor.games.konami.IConfigData;
import net.highwayfrogs.editor.games.konami.greatquest.GreatQuestInstance;
import net.highwayfrogs.editor.games.konami.greatquest.proxy.ProxyReact;
import net.highwayfrogs.editor.gui.components.propertylist.IPropertyListCreator;
import net.highwayfrogs.editor.gui.components.propertylist.PropertyListNode;
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
public class kcProjectileParams extends GameData<GreatQuestInstance> implements IPropertyListCreator, IConfigData {
    private ProxyReact proxyReact = ProxyReact.PENETRATE;
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
    public void addToPropertyList(PropertyListNode propertyList) {
        propertyList.add("Flags", NumberUtils.toHexString(this.flags));
        propertyList.addInteger("Hit Strength", this.hitStrength, newValue -> this.hitStrength = newValue);
        propertyList.add("Damage Flags", NumberUtils.toHexString(this.damageFlags));
        propertyList.addEnum("Collision Proxy Reaction", this.proxyReact, ProxyReact.class, newValue -> this.proxyReact = newValue, false);
        propertyList.addFloat("Sensor Radius", this.sensorRadius, newValue -> this.sensorRadius = newValue);
        propertyList.addInteger("Group", this.group, newValue -> this.group = newValue);
        propertyList.addInteger("Focus", this.focus, newValue -> this.focus = newValue);
        propertyList.addFloat("Mass", this.mass, newValue -> this.mass = newValue);
        propertyList.addFloat("Gravity", this.gravity, newValue -> this.gravity = newValue);
        propertyList.addFloat("Retain Bounce", this.retainBounce, newValue -> this.retainBounce = newValue);
        propertyList.addFloat("Retain Slide", this.retainSlide, newValue -> this.retainSlide = newValue);
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