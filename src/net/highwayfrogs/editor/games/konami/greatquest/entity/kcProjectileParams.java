package net.highwayfrogs.editor.games.konami.greatquest.entity;

import lombok.Getter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.file.GameObject;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.konami.greatquest.IInfoWriter.IMultiLineInfoWriter;
import net.highwayfrogs.editor.games.konami.greatquest.proxy.ProxyReact;
import net.highwayfrogs.editor.utils.Utils;

/**
 * Represents the 'kcProjectileParams' struct.
 * Created by Kneesnap on 8/21/2023.
 */
@Getter
public class kcProjectileParams extends GameObject implements IMultiLineInfoWriter {
    private ProxyReact proxyReact;
    private float sensorRadius;
    private int group;
    private int focus;
    private float mass;
    private float gravity;
    private int flags;
    private float retainBounce;
    private float retainSlide;
    private int damageFlags;
    private int hitStrength;
    private final int[] padProjectileParams = new int[8];

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
        for (int i = 0; i < this.padProjectileParams.length; i++)
            this.padProjectileParams[i] = reader.readInt();
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
        for (int i = 0; i < this.padProjectileParams.length; i++)
            writer.writeInt(this.padProjectileParams[i]);
    }

    @Override
    public void writeMultiLineInfo(StringBuilder builder, String padding) {
        builder.append(padding).append("Flags: ").append(Utils.toHexString(this.flags)).append(Constants.NEWLINE);
        builder.append(padding).append("Hit Strength: ").append(this.hitStrength)
                .append(", Damage Flags: ").append(Utils.toHexString(this.damageFlags)).append(Constants.NEWLINE);
        builder.append(padding).append("Collision Proxy Reaction: ").append(this.proxyReact).append(Constants.NEWLINE);
        builder.append(padding).append("Sensor Radius: ").append(this.sensorRadius).append(Constants.NEWLINE);
        builder.append(padding).append("Group: ").append(this.group).append(", Focus: ").append(this.focus).append(Constants.NEWLINE);
        builder.append(padding).append("Mass: ").append(this.mass).append(", Gravity: ").append(this.gravity).append(Constants.NEWLINE);
        builder.append(padding).append("Retain Bounce: ").append(this.retainBounce)
                .append(", Retain Slide: ").append(this.retainSlide).append(Constants.NEWLINE);
    }
}