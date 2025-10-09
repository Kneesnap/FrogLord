package net.highwayfrogs.editor.games.konami.greatquest.proxy;

import lombok.Getter;
import lombok.Setter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.games.konami.greatquest.GreatQuestHash;
import net.highwayfrogs.editor.games.konami.greatquest.GreatQuestUtils;
import net.highwayfrogs.editor.games.konami.greatquest.generic.kcCResourceGeneric;
import net.highwayfrogs.editor.games.konami.greatquest.generic.kcCResourceGeneric.kcCResourceGenericType;
import net.highwayfrogs.editor.games.konami.greatquest.generic.kcCResourceGeneric.kcCResourceGenericTypeGroup;
import net.highwayfrogs.editor.games.konami.greatquest.kcClassID;
import net.highwayfrogs.editor.utils.data.reader.DataReader;
import net.highwayfrogs.editor.utils.data.writer.DataWriter;

/**
 * Implements the 'kcEmitterDesc' struct.
 * Note that this is not related to particles as I previously thought, it is an entity spawner.
 * This can be used to spawn entities with the named formatted as String.format("%s%d", emitterName, entitiesSpawnedByEmitter++).
 * This may seem unused, but it's actually used once in Mushroom Valley, the 'TEST' value. Which... may not actually be used, I'm not sure.
 * Loaded by kcCEmitter::Init
 * Created by Kneesnap on 8/22/2023.
 */
@Getter
@Setter
public class kcEmitterDesc extends kcProxyCapsuleDesc {
    // The following default data has been pulled from 'TEST' in mushroom valley, which as mentioned before, is likely unused.
    private int triggerType = 2;
    private int frequency = 10000;
    private int lifeTime = -1;
    private int spawnLimit = 1;
    private int maxSpawn = 5;
    private float spawnRange = 2F;
    private final GreatQuestHash<kcCResourceGeneric> entityDescRef;

    public static final String NAME_SUFFIX = "EmitterDesc";

    public kcEmitterDesc(kcCResourceGeneric resource) {
        super(resource, kcProxyDescType.EMITTER);
        this.entityDescRef = new GreatQuestHash<>();
        applyNameToDescriptions();
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

        GreatQuestUtils.resolveLevelResourceHash(kcCResourceGenericTypeGroup.ENTITY_DESCRIPTION, getParentFile(), this, this.entityDescRef, entityDescHash, true);
        applyNameToDescriptions();
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
        builder.append("Trigger: ").append(this.triggerType).append(Constants.NEWLINE);
        builder.append("Frequency: ").append(this.frequency).append(Constants.NEWLINE);
        builder.append("Life Time: ").append(this.lifeTime).append(Constants.NEWLINE);
        builder.append("Spawn Limit: ").append(this.spawnLimit).append(Constants.NEWLINE);
        builder.append("Max Spawn: ").append(this.maxSpawn).append(Constants.NEWLINE);
        builder.append("Spawn Range: ").append(this.spawnRange).append(Constants.NEWLINE);
        writeAssetLine(builder, padding, "Entity Description", this.entityDescRef);
    }

    private void applyNameToDescriptions() {
        if (getResource() == null)
            return;

        // If we resolve the model successfully, our goal is to generate the name of any corresponding collision mesh.
        String baseName = getResource().getName();
        if (baseName.endsWith(NAME_SUFFIX))
            baseName = baseName.substring(0, baseName.length() - NAME_SUFFIX.length());

        int testHash = GreatQuestUtils.hash(baseName);
        if (this.entityDescRef.getHashNumber() == testHash && this.entityDescRef.getResource() != null)
            this.entityDescRef.getResource().getSelfHash().setOriginalString(baseName);

        String emitterDescName = baseName + NAME_SUFFIX;
        testHash = GreatQuestUtils.hash(emitterDescName);
        if (getResource() != null && getResource().getHash() == testHash)
            getResource().getSelfHash().setOriginalString(emitterDescName);
    }
}