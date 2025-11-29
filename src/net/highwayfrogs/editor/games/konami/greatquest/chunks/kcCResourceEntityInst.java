package net.highwayfrogs.editor.games.konami.greatquest.chunks;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import net.highwayfrogs.editor.games.konami.greatquest.entity.kcEntity3DInst;
import net.highwayfrogs.editor.games.konami.greatquest.entity.kcEntityFlag.kcEntityInstanceFlag;
import net.highwayfrogs.editor.games.konami.greatquest.entity.kcEntityInst;
import net.highwayfrogs.editor.games.konami.greatquest.script.kcScriptList;
import net.highwayfrogs.editor.gui.components.propertylist.PropertyListNode;
import net.highwayfrogs.editor.system.Config;
import net.highwayfrogs.editor.utils.data.reader.DataReader;
import net.highwayfrogs.editor.utils.data.writer.DataWriter;
import net.highwayfrogs.editor.utils.logging.ILogger;

/**
 * Recreation of the 'kcCResourceEntityInst' class from the PS2 version.
 * Entity loading flow:
 *
 * modeLevelLoad()
 * ExecuteLoad()
 * -> kcCGameSystem::LoadLevel
 *  -> kcCResourceMgr::PrepareResources
 *   -> kcCResourceEntityInst::Prepare
 *    -> kcCGameSystem::CreateInstance()
 *     -> Insert new entity into kcCEntity::mpInstanceMap.
 *     -> kcCEntity::Init(kcEntityInst* pInstParams)
 *      -> kcCEntity::Init(kcBaseDesc*)
 *     -> kcCEntity3D::Reset/kcCEntity::Reset
 *      -> kcCEntity3D::ResetInt/kcCEntity::ResetInt
 *       -> kcCEntity3D::SetInstanceInfo -> Copies position, rotation, scale.
 *      -> kcCEntity3D::Update
 *      -> kcCGameSystem::SceneUpdate
 *    -> kcCGameSystem::Insert(pEntity)
 *     -> kcCEntity::Submit() # registers entity update function to kcCProcMgr.
 *  -> kcCScriptMgr::Load() # Load scripts.
 *  -> kcCHash::Traverse(_mpInstanceMap,sDisableUpdateCallback) # Disable all entities.
 * -> kcCAudioManager::LoadLevelSounds()
 * -> Trigger("LoadLevelComplete")
 * Created by Kneesnap on 8/25/2019.
 */
@Getter
@Setter
public class kcCResourceEntityInst extends kcCResource {
    private kcEntityInst instance;
    private byte[] dummyBytes;

    public kcCResourceEntityInst(GreatQuestChunkedFile parentFile) {
        super(parentFile, KCResourceID.ENTITYINST);
    }

    @Override
    public void load(DataReader reader) {
        super.load(reader);

        reader.jumpTemp(reader.getIndex());
        int sizeInBytes = reader.readInt(); // Number of bytes used for entity data.
        reader.jumpReturn();

        int calculatedSize = reader.getRemaining(); // We've returned to before the size integer was read.
        if (sizeInBytes != calculatedSize)
            throw new RuntimeException("The expected amount of entity data (" + sizeInBytes + " bytes) different from the actual amount (" + calculatedSize + " bytes).");

        this.instance = null;
        this.dummyBytes = null;

        // The game uses the entity description hash (read prematurely from the upcoming data) to determine which kind of entity to create. (See kcCGameSystem::CreateInstance)
        // All descriptions have been loaded by this point since entities are guaranteed to be last in the file.
        // Every entity except kcCParticleEmitter can be created, see kcCResourceEntityInst::Prepare() to see that it will not work. Non-entity objects could theoretically be created too, but would cause memory corruption.
        // It appears all entities we'd want to create are accessible except kcCParticleEmitter, which could theoretically be patched in.
        // The following code is not accurate to the original game, but works reliably.
        if (sizeInBytes == kcEntity3DInst.SIZE_IN_BYTES || sizeInBytes == kcEntity3DInst.SIZE_IN_BYTES_WITHOUT_PADDING) {
            this.instance = new kcEntity3DInst(this);
            this.instance.load(reader);
        } else if (sizeInBytes == kcEntityInst.SIZE_IN_BYTES) {
            // This should never occur for reasons explained above.
            getLogger().warning("A non-3D entity was found, which is expected to never occur.");
            this.instance = new kcEntityInst(this);
            this.instance.load(reader);
        } else {
            getLogger().severe("Couldn't identify the entity type for '%s' from the byte size of %d.", getName(), sizeInBytes);
            this.dummyBytes = reader.readBytes(sizeInBytes);
        }

        // Print warning.
        String targetEntityWarning = getCurrentFaceTargetEntityWarning();
        if (targetEntityWarning != null)
            getLogger().warning("%s", targetEntityWarning);
    }

    @Override
    public void save(DataWriter writer) {
        super.save(writer);
        if (this.instance != null) {
            this.instance.save(writer);
        } else if (this.dummyBytes != null) {
            writer.writeBytes(this.dummyBytes);
        }
    }

    @Override
    protected void onRemovedFromChunkFile() {
        // Delete the script associated with the entity.
        if (this.instance != null) {
            int scriptIndex = this.instance.getScriptIndex();
            if (scriptIndex >= 0) {
                kcScriptList scriptList = getParentFile().getScriptList();
                if (scriptList != null)
                    scriptList.removeScript(scriptIndex);
            }
        }

        super.onRemovedFromChunkFile();
    }

    /**
     * The game has a problem with target entities.
     * During an entity's load process, it runs kcCEntity3D::BuildLcsToWcs.
     * This function is important for the Octree, and inserting entities into the Octrees.
     * However, if --FaceTargetEntity is used, BuildLcsToWcs will try to run kcCEntity3D::BuildBillBoard instead, which will silently fail if the target entity is not found!
     * And, finding the target entity will fail if it hasn't been loaded from the file yet (Eg: the order by which entities load matters.)
     * So, this warning has been put in place to warn when such a scenario occurs.
     * Note that this is unlikely to break other usages of the target entity, since they will occur after all entities have finished loading.
     * @return warning if problem is present, otherwise null
     */
    private String getCurrentFaceTargetEntityWarning() {
        if (!(this.instance instanceof kcEntity3DInst))
            return null;

        kcEntity3DInst entityInst = (kcEntity3DInst) this.instance;
        if (!entityInst.hasFlag(kcEntityInstanceFlag.FACE_TARGET_ENTITY))
            return null; // Without a --FaceTargetEntity, this isn't known to cause any problems.

        if (entityInst.getTargetEntityRef().isHashNull())
            return null; // There is no target entity.

        kcCResourceEntityInst targetEntity = entityInst.getTargetEntityRef().getResource();
        if (targetEntity == null)
            return "Using --" + kcEntityInstanceFlag.FACE_TARGET_ENTITY.getDisplayName() + " with a non-existent target entity ("
                    + entityInst.getTargetEntityRef().getDisplayString(false) + " may cause '"
                    + getName() + "' to not display correctly in-game!";

        if (GreatQuestChunkedFile.RESOURCE_ORDERING.compare(targetEntity, this) > 0 && !kcCResource.DEFAULT_RESOURCE_NAME.equalsIgnoreCase(targetEntity.getName()))
            return "Using --" + kcEntityInstanceFlag.FACE_TARGET_ENTITY.getDisplayName() + " with the target entity '"
                    + targetEntity.getName() + "' may cause '" + getName() + "' to not display correctly in-game because '"
                    + getName() + "' is loaded before '" + targetEntity.getName() + "'.";

        return null;
    }

    @Override
    public void addToPropertyList(PropertyListNode propertyList) {
        super.addToPropertyList(propertyList);
        if (this.instance != null)
            this.instance.addToPropertyList(propertyList);
    }

    /**
     * Loads the entity instance from a gqs config.
     * @param entityInstanceCfg the entity instance config to load from
     * @return entityInst
     */
    public @NonNull kcEntityInst fromConfig(ILogger logger, Config entityInstanceCfg) {
        if (entityInstanceCfg == null)
            throw new NullPointerException("entityInstanceCfg");

        kcEntityInst entityInst = this.instance;
        if (entityInst == null)
            throw new RuntimeException("The entity instance for '" + entityInstanceCfg.getSectionName() + "' was null, so we couldn't modify its script.");
        if (logger == null)
            logger = getLogger();

        entityInst.fromConfig(logger, entityInstanceCfg);

        // Show target entity warning, if there is one.
        String targetEntityWarning = getCurrentFaceTargetEntityWarning();
        if (targetEntityWarning != null)
            logger.warning(targetEntityWarning);

        return entityInst;
    }
}