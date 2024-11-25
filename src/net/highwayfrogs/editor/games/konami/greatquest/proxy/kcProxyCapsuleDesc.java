package net.highwayfrogs.editor.games.konami.greatquest.proxy;

import lombok.Getter;
import lombok.Setter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.konami.greatquest.generic.kcCResourceGeneric;
import net.highwayfrogs.editor.games.konami.greatquest.generic.kcCResourceGeneric.kcCResourceGenericType;
import net.highwayfrogs.editor.games.konami.greatquest.kcClassID;
import net.highwayfrogs.editor.system.Config;

/**
 * Implements the 'kcProxyCapsuleDesc' struct.
 * Created by Kneesnap on 8/22/2023.
 */
@Getter
@Setter
public class kcProxyCapsuleDesc extends kcProxyDesc {
    private float radius = .35F; // Default values taken from CItem::Init() Radius of 0.0 will be replaced with 0.5 by kcCActorBase::CreateCollisionProxy
    private float length = .5F;
    private float offset = .5F;

    public kcProxyCapsuleDesc(kcCResourceGeneric resource) {
        super(resource);
    }

    @Override
    public void load(DataReader reader) {
        super.load(reader);
        this.radius = reader.readFloat();
        this.length = reader.readFloat();
        this.offset = reader.readFloat();
    }

    /**
     * Gets the processed radius
     */
    public float getProcessedRadius() {
        // See kcCActorBase::CreateCollisionProxy()
        return this.radius != 0.0F ? this.radius : .5F;
    }

    /**
     * Calculates the offset to the center of the capsule.
     * @return centerOffsetY
     */
    public float calculateCenterOffsetY() {
        // Found in kcCActorBase::CreateCollisionProxy -> Calling kcCProxyCapsule::Init.
        return this.offset + getProcessedRadius() + (this.length * .5F);
    }

    /**
     * Get the Y offset to the center of the bottom sphere.
     */
    public float getBottomSphereOffsetY() {
        return this.offset + getProcessedRadius();
    }

    /**
     * Get the Y offset to the center of the top sphere.
     */
    public float getTopSphereOffsetY() {
        return getBottomSphereOffsetY() + this.length;
    }

    @Override
    public void saveData(DataWriter writer) {
        super.saveData(writer);
        writer.writeFloat(this.radius);
        writer.writeFloat(this.length);
        writer.writeFloat(this.offset);
    }

    @Override
    protected int getTargetClassID() {
        return kcClassID.PROXY_CAPSULE.getClassId();
    }

    @Override
    public kcCResourceGenericType getResourceType() {
        return kcCResourceGenericType.PROXY_CAPSULE_DESCRIPTION;
    }

    @Override
    public void writeMultiLineInfo(StringBuilder builder, String padding) {
        super.writeMultiLineInfo(builder, padding);
        builder.append(padding).append("Radius: ").append(this.radius).append(Constants.NEWLINE);
        builder.append(padding).append("Length: ").append(this.length).append(Constants.NEWLINE);
        builder.append(padding).append("Offset: ").append(this.offset).append(Constants.NEWLINE);
    }

    @Override
    public boolean isStatic() {
        return true; // Does nothing for kcProxyCapsuleDesc, but is always true in the retail game.
    }

    private static final String CONFIG_KEY_RADIUS = "radius";
    private static final String CONFIG_KEY_LENGTH = "height";
    private static final String CONFIG_KEY_OFFSET = "offset";

    @Override
    public void fromConfig(Config input) {
        super.fromConfig(input);
        this.radius = input.getOrDefaultKeyValueNode(CONFIG_KEY_RADIUS).getAsFloat(.35F);
        this.length = input.getOrDefaultKeyValueNode(CONFIG_KEY_LENGTH).getAsFloat(.5F);
        this.offset = input.getOrDefaultKeyValueNode(CONFIG_KEY_OFFSET).getAsFloat(.5F);
    }

    @Override
    public void toConfig(Config output) {
        super.toConfig(output);
        output.getOrCreateKeyValueNode(CONFIG_KEY_RADIUS).setAsFloat(this.radius);
        output.getOrCreateKeyValueNode(CONFIG_KEY_LENGTH).setAsFloat(this.length);
        output.getOrCreateKeyValueNode(CONFIG_KEY_OFFSET).setAsFloat(this.offset);
    }
}