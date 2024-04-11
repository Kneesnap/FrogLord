package net.highwayfrogs.editor.games.greatquest.entity;

import lombok.Getter;
import lombok.Setter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.greatquest.generic.kcBlend;
import net.highwayfrogs.editor.games.greatquest.kcClassID;

/**
 * Represents the 'kcParticleEmitterParam' struct.
 * Created by Kneesnap on 8/22/2023.
 */
@Getter
@Setter
public class kcParticleEmitterParam extends kcEntity3DDesc {
    private kcBlend srcBlend = kcBlend.ZERO;
    private kcBlend dstBlend = kcBlend.ZERO;
    private int textureHash;
    private int descHash = -1;
    private final kcParticleParam particleParam = new kcParticleParam();
    private float lifeTimeEmitter; // These may be garbage / unused.
    private int maxParticle;
    private final int[] padding = new int[6];

    @Override
    public int getTargetClassID() {
        return kcClassID.PARTICLE_EMITTER.getClassId();
    }

    @Override
    public void load(DataReader reader) {
        super.load(reader);
        this.srcBlend = kcBlend.getMode(reader.readInt(), false);
        this.dstBlend = kcBlend.getMode(reader.readInt(), false);
        reader.skipInt(); // Texture pointer (zero)
        this.textureHash = reader.readInt();
        this.descHash = reader.readInt();
        this.particleParam.load(reader);
        this.lifeTimeEmitter = reader.readFloat();
        this.maxParticle = reader.readInt();
        for (int i = 0; i < this.padding.length; i++)
            this.padding[i] = reader.readInt();
    }

    @Override
    public void saveData(DataWriter writer) {
        super.saveData(writer);
        writer.writeInt(this.srcBlend.getValue());
        writer.writeInt(this.dstBlend.getValue());
        writer.writeInt(0); // Texture pointer (zero)
        writer.writeInt(this.textureHash);
        writer.writeInt(this.descHash);
        this.particleParam.save(writer);
        writer.writeFloat(this.lifeTimeEmitter);
        writer.writeInt(this.maxParticle);
        for (int i = 0; i < this.padding.length; i++)
            writer.writeInt(this.padding[i]);
    }

    @Override
    public void writeMultiLineInfo(StringBuilder builder, String padding) {
        super.writeMultiLineInfo(builder, padding);
        builder.append(padding).append("Src Blend: ").append(this.srcBlend).append(Constants.NEWLINE);
        builder.append(padding).append("Dest Blend: ").append(this.dstBlend).append(Constants.NEWLINE);
        writeAssetLine(builder, padding, "Texture", this.textureHash);
        writeAssetLine(builder, padding, "Description", this.descHash);
        this.particleParam.writePrefixedMultiLineInfo(builder, "Particle Params", padding);
        builder.append(padding).append("Emitter Life Time (Garbage?): ").append(this.lifeTimeEmitter).append(Constants.NEWLINE);
        builder.append(padding).append("Max Particle (Garbage?) ").append(this.maxParticle).append(Constants.NEWLINE);
    }
}