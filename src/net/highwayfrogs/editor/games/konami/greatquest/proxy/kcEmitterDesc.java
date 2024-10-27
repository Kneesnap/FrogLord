package net.highwayfrogs.editor.games.konami.greatquest.proxy;

import lombok.Getter;
import lombok.Setter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.konami.greatquest.GreatQuestHash;
import net.highwayfrogs.editor.games.konami.greatquest.GreatQuestUtils;
import net.highwayfrogs.editor.games.konami.greatquest.generic.kcCResourceGeneric;
import net.highwayfrogs.editor.games.konami.greatquest.generic.kcCResourceGeneric.kcCResourceGenericType;
import net.highwayfrogs.editor.games.konami.greatquest.kcClassID;

/**
 * Implements the 'kcEmitterDesc' struct.
 * This may seem unused, but it's actually used once in Mushroom Valley, the 'TEST' value. Which... may not actually be used, I'm not sure.
 * Loaded by kcCEmitter::Init
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
    private final GreatQuestHash<kcCResourceGeneric> entityDescRef;

    public kcEmitterDesc(kcCResourceGeneric resource) {
        super(resource);
        this.entityDescRef = new GreatQuestHash<>();
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
        int entityDescHash = reader.readInt();

        GreatQuestUtils.resolveResourceHash(kcCResourceGeneric.class, this, this.entityDescRef, entityDescHash, true);
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
        writer.writeInt(this.entityDescRef.getHashNumber());
    }

    @Override
    protected int getTargetClassID() {
        return kcClassID.EMITTER.getClassId();
    }

    @Override
    public kcCResourceGenericType getResourceType() {
        return kcCResourceGenericType.EMITTER_DESCRIPTION;
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
        writeAssetLine(builder, padding, "Entity Description", this.entityDescRef);
    }
}