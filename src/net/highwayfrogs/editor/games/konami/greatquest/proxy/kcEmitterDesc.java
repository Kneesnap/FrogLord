package net.highwayfrogs.editor.games.konami.greatquest.proxy;

import lombok.Getter;
import lombok.Setter;
import net.highwayfrogs.editor.games.konami.greatquest.GreatQuestHash;
import net.highwayfrogs.editor.games.konami.greatquest.GreatQuestUtils;
import net.highwayfrogs.editor.games.konami.greatquest.generic.kcCResourceGeneric;
import net.highwayfrogs.editor.games.konami.greatquest.generic.kcCResourceGeneric.kcCResourceGenericType;
import net.highwayfrogs.editor.games.konami.greatquest.generic.kcCResourceGeneric.kcCResourceGenericTypeGroup;
import net.highwayfrogs.editor.games.konami.greatquest.kcClassID;
import net.highwayfrogs.editor.gui.components.propertylist.PropertyListNode;
import net.highwayfrogs.editor.system.Config;
import net.highwayfrogs.editor.utils.data.reader.DataReader;
import net.highwayfrogs.editor.utils.data.writer.DataWriter;
import net.highwayfrogs.editor.utils.logging.ILogger;

/**
 * Implements the 'kcEmitterDesc' struct.
 * Note that this is not related to particles as I previously thought, it is an entity spawner.
 * This can be used to spawn entities with the named formatted as String.format("%s%d", emitterName, entitiesSpawnedByEmitter++).
 * This may seem unused, but it's actually used once in Mushroom Valley, the 'TEST' value. Which... may not actually be used, I'm not sure.
 * Loaded by kcCEmitter::Init.
 * I tried to get this to work in-game.
 *  #1) Using this as a collision proxy crashed the game.
 *  #2) Using this as an entity description did seem to work, but the emitter didn't actually do anything in-game.
 * Therefore, this feature is treated as unsupported by FrogLord, as it does not appear to work.
 * Created by Kneesnap on 8/22/2023.
 */
@Getter
@Setter
public class kcEmitterDesc extends kcProxyCapsuleDesc {
    // The following default data has been pulled from 'TEST' in mushroom valley, which as mentioned before, is likely unused.
    private int triggerType = DEFAULT_TRIGGER_TYPE; // Either 1 or 2.
    private int frequency = DEFAULT_FREQUENCY; // How frequently the timer
    private int lifeTime = DEFAULT_LIFE_TIME;
    private int spawnLimit = DEFAULT_SPAWN_LIMIT;
    private int maxSpawn = DEFAULT_MAX_SPAWN;
    private float spawnRange = DEFAULT_SPAWN_RANGE;
    private final GreatQuestHash<kcCResourceGeneric> entityDescRef;

    private static final int DEFAULT_TRIGGER_TYPE = 2;
    private static final int DEFAULT_FREQUENCY = 10000;
    private static final int DEFAULT_LIFE_TIME = -1;
    private static final int DEFAULT_SPAWN_LIMIT = 1;
    private static final int DEFAULT_MAX_SPAWN = 5;
    private static final float DEFAULT_SPAWN_RANGE = 2F;

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

    private void applyNameToDescriptions() {
        if (getResource() == null)
            return;

        // If we resolve the model successfully, our goal is to generate the name of any corresponding collision mesh.
        String baseName = getResourceName();
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

    @Override
    public void addToPropertyList(PropertyListNode propertyList) {
        super.addToPropertyList(propertyList);
        propertyList.addInteger("Trigger", this.triggerType, newValue -> this.triggerType = newValue);
        propertyList.addInteger("Frequency", this.frequency, newValue -> this.frequency = newValue);
        propertyList.addInteger("Life Time", this.lifeTime, newValue -> this.lifeTime = newValue);
        propertyList.addInteger("Spawn Limit", this.spawnLimit, newValue -> this.spawnLimit = newValue);
        propertyList.addInteger("Max Spawn", this.maxSpawn, newValue -> this.maxSpawn = newValue);
        propertyList.addFloat("Spawn Range", this.spawnRange, newValue -> this.spawnRange = newValue);
        this.entityDescRef.addToPropertyList(propertyList, "Entity Description", getParentFile(), kcCResourceGenericTypeGroup.ENTITY_DESCRIPTION);
    }

    private static final String CONFIG_KEY_TRIGGER_TYPE = "triggerType";
    private static final String CONFIG_KEY_FREQUENCY = "frequency";
    private static final String CONFIG_KEY_LIFE_TIME = "lifeTime";
    private static final String CONFIG_KEY_SPAWN_LIMIT = "spawnLimit";
    private static final String CONFIG_KEY_MAX_SPAWN = "maxSpawn";
    private static final String CONFIG_KEY_SPAWN_RANGE = "spawnRange";
    private static final String CONFIG_KEY_ENTITY_DESCRIPTION = "entityDesc";

    @Override
    public void fromConfig(ILogger logger, Config input) {
        super.fromConfig(logger, input);
        this.triggerType = input.getOrDefaultKeyValueNode(CONFIG_KEY_TRIGGER_TYPE).getAsInteger(DEFAULT_TRIGGER_TYPE);
        this.frequency = input.getOrDefaultKeyValueNode(CONFIG_KEY_FREQUENCY).getAsInteger(DEFAULT_FREQUENCY);
        this.lifeTime = input.getOrDefaultKeyValueNode(CONFIG_KEY_LIFE_TIME).getAsInteger(DEFAULT_LIFE_TIME);
        this.spawnLimit = input.getOrDefaultKeyValueNode(CONFIG_KEY_SPAWN_LIMIT).getAsInteger(DEFAULT_SPAWN_LIMIT);
        this.maxSpawn = input.getOrDefaultKeyValueNode(CONFIG_KEY_MAX_SPAWN).getAsInteger(DEFAULT_MAX_SPAWN);
        this.spawnRange = input.getOrDefaultKeyValueNode(CONFIG_KEY_SPAWN_RANGE).getAsFloat(DEFAULT_SPAWN_RANGE);
        GreatQuestUtils.resolveLevelResource(logger, input.getKeyValueNodeOrError(CONFIG_KEY_ENTITY_DESCRIPTION), kcCResourceGenericTypeGroup.ENTITY_DESCRIPTION, getParentFile(), this, this.entityDescRef, true);
    }

    @Override
    public void toConfig(Config output) {
        super.toConfig(output);
        output.getOrCreateKeyValueNode(CONFIG_KEY_TRIGGER_TYPE).setAsInteger(this.triggerType);
        output.getOrCreateKeyValueNode(CONFIG_KEY_FREQUENCY).setAsInteger(this.frequency);
        output.getOrCreateKeyValueNode(CONFIG_KEY_LIFE_TIME).setAsInteger(this.lifeTime);
        output.getOrCreateKeyValueNode(CONFIG_KEY_SPAWN_LIMIT).setAsInteger(this.spawnLimit);
        output.getOrCreateKeyValueNode(CONFIG_KEY_MAX_SPAWN).setAsInteger(this.maxSpawn);
        output.getOrCreateKeyValueNode(CONFIG_KEY_SPAWN_RANGE).setAsFloat(this.spawnRange);
        output.getOrCreateKeyValueNode(CONFIG_KEY_ENTITY_DESCRIPTION).setAsString(this.entityDescRef.getAsGqsString(null));
    }
}