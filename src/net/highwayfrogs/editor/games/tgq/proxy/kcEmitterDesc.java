package net.highwayfrogs.editor.games.tgq.proxy;

import lombok.Getter;
import lombok.Setter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.tgq.kcClassID;

/**
 * Implements the 'kcEmitterDesc' struct.
 * Created by Kneesnap on 8/22/2023.
 */
@Getter
@Setter
public class kcEmitterDesc extends kcProxyCapsuleDesc {
    private int triggerType;
    private int frequency;
    private int lifeTime;
    private int spawnLimit;
    private int maxSpawn;
    private float spawnRange;
    private int entityDescHash;

    @Override
    protected int getTargetClassID() {
        return kcClassID.EMITTER.getClassId();
    }

    @Override
    public void load(DataReader reader) {
        super.load(reader);
        this.triggerType = reader.readInt();
        this.frequency = reader.readInt();
        this.lifeTime = reader.readInt();
        this.spawnLimit = reader.readInt();
        this.maxSpawn = reader.readInt();
        this.spawnRange = reader.readFloat();
        this.entityDescHash = reader.readInt();
    }

    @Override
    public void saveData(DataWriter writer) {
        super.saveData(writer);
        writer.writeInt(this.triggerType);
        writer.writeInt(this.frequency);
        writer.writeInt(this.lifeTime);
        writer.writeInt(this.spawnLimit);
        writer.writeInt(this.maxSpawn);
        writer.writeFloat(this.spawnRange);
        writer.writeInt(this.entityDescHash);
    }

    @Override
    public void writeMultiLineInfo(StringBuilder builder, String padding) {
        super.writeMultiLineInfo(builder, padding);
        builder.append("Trigger: ").append(this.triggerType).append(Constants.NEWLINE); // TODO: ENUM?
        builder.append("Frequency: ").append(this.frequency).append(Constants.NEWLINE);
        builder.append("Life Time: ").append(this.lifeTime).append(Constants.NEWLINE);
        builder.append("Spawn Limit: ").append(this.spawnLimit).append(Constants.NEWLINE);
        builder.append("Max Spawn: ").append(this.maxSpawn).append(Constants.NEWLINE);
        builder.append("Spawn Range: ").append(this.spawnRange).append(Constants.NEWLINE);
        builder.append("Max Spawn: ").append(this.maxSpawn).append(Constants.NEWLINE);
        writeAssetLine(builder, padding, "Entity Description", this.entityDescHash);
    }
}
