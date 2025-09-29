package net.highwayfrogs.editor.games.konami.greatquest.chunks;

import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import net.highwayfrogs.editor.games.konami.greatquest.entity.kcEntity3DInst;
import net.highwayfrogs.editor.games.konami.greatquest.entity.kcEntityFlag.kcEntityInstanceFlag;
import net.highwayfrogs.editor.games.konami.greatquest.entity.kcEntityInst;
import net.highwayfrogs.editor.gui.components.PropertyListViewerComponent.PropertyList;
import net.highwayfrogs.editor.system.Config;
import net.highwayfrogs.editor.utils.FileUtils;
import net.highwayfrogs.editor.utils.FileUtils.SavedFilePath;
import net.highwayfrogs.editor.utils.data.reader.DataReader;
import net.highwayfrogs.editor.utils.data.writer.DataWriter;

import java.io.File;

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

    private static final String ENTITY_FILE_PATH_KEY = "entityCfgFilePath";
    private static final SavedFilePath ENTITY_EXPORT_PATH = new SavedFilePath(ENTITY_FILE_PATH_KEY, "Select the directory to export the entity to", Config.DEFAULT_FILE_TYPE);
    private static final SavedFilePath ENTITY_IMPORT_PATH = new SavedFilePath(ENTITY_FILE_PATH_KEY, "Select the directory to import entity data from", Config.DEFAULT_FILE_TYPE);

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
        if (sizeInBytes == kcEntity3DInst.SIZE_IN_BYTES) {
            this.instance = new kcEntity3DInst(this);
            this.instance.load(reader);
        } else if (sizeInBytes == kcEntityInst.SIZE_IN_BYTES) {
            // This appears supported, although I am unsure in what circumstance this would ever be useful, and the game doesn't seem to have any entities like this.
            getLogger().warning("A non-3D entity was found! This is unusual, and may not work correctly.");
            this.instance = new kcEntityInst(this);
            this.instance.load(reader);
        } else {
            // TODO: Let's reverse engineer this.
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

        if (entityInst.getTargetEntityRef().getHashNumber() == -1)
            return null; // There is no target entity.

        kcCResourceEntityInst targetEntity = entityInst.getTargetEntityRef().getResource();
        if (targetEntity == null)
            return "Using --" + kcEntityInstanceFlag.FACE_TARGET_ENTITY.getDisplayName() + " with a non-existent target entity ("
                    + entityInst.getTargetEntityRef().getDisplayString(false) + " may cause '"
                    + getName() + "' to not display correctly in-game!";

        if (GreatQuestChunkedFile.RESOURCE_ORDERING.compare(targetEntity, this) > 0)
            return "Using --" + kcEntityInstanceFlag.FACE_TARGET_ENTITY.getDisplayName() + " with the target entity '"
                    + targetEntity.getName() + "' may cause '" + getName() + "' to not display correctly in-game because '"
                    + getName() + "' is loaded before '" + targetEntity.getName() + "'.";

        return null;
    }

    @Override
    public PropertyList addToPropertyList(PropertyList propertyList) {
        propertyList = super.addToPropertyList(propertyList);
        // TODO: ADD ENTITY DATA INTO PROPERTY LIST!
        return propertyList;
    }

    @Override
    public void setupRightClickMenuItems(ContextMenu contextMenu) {
        super.setupRightClickMenuItems(contextMenu);

        MenuItem exportEntityItem = new MenuItem("Export Entity");
        contextMenu.getItems().add(exportEntityItem);
        exportEntityItem.setOnAction(event -> {
            File outputFile = FileUtils.askUserToSaveFile(getGameInstance(), ENTITY_EXPORT_PATH, getName() + "." + Config.DEFAULT_EXTENSION, true);
            if (outputFile != null) {
                this.instance.toConfig().saveTextFile(outputFile);
                getLogger().info("Saved '%s' as '%s'.", getName(), outputFile.getName());
            }
        });
        exportEntityItem.setDisable(this.instance == null);

        MenuItem importEntityItem = new MenuItem("Import Entity");
        contextMenu.getItems().add(importEntityItem);
        importEntityItem.setOnAction(event -> {
            File inputFile = FileUtils.askUserToOpenFile(getGameInstance(), ENTITY_IMPORT_PATH);
            if (inputFile == null)
                return;

            Config entityCfg = Config.loadConfigFromTextFile(inputFile, false);
            if (this.instance == null)
                this.instance = new kcEntity3DInst(this);

            this.instance.fromConfig(entityCfg);
            getLogger().info("Loaded '%s from '%s'.", getName(), inputFile.getName());
        });
    }

    /**
     * Loads the entity instance from a gqs config.
     * @param entityInstanceCfg the entity instance config to load from
     * @return entityInst
     */
    public @NonNull kcEntityInst fromConfig(Config entityInstanceCfg) {
        if (entityInstanceCfg == null)
            throw new NullPointerException("entityInstanceCfg");

        kcEntityInst entityInst = this.instance;
        if (entityInst == null)
            throw new RuntimeException("The entity instance for '" + entityInstanceCfg.getSectionName() + "' was null, so we couldn't modify its script.");

        entityInst.fromConfig(entityInstanceCfg);

        // Show target entity warning, if there is one.
        String targetEntityWarning = getCurrentFaceTargetEntityWarning();
        if (targetEntityWarning != null)
            getLogger().warning(targetEntityWarning);

        return entityInst;
    }
}