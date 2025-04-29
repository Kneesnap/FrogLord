package net.highwayfrogs.editor.games.sony.oldfrogger.map.entity;

import lombok.Getter;
import net.highwayfrogs.editor.utils.data.reader.DataReader;
import net.highwayfrogs.editor.utils.data.writer.DataWriter;
import net.highwayfrogs.editor.games.sony.SCGameData;
import net.highwayfrogs.editor.games.sony.oldfrogger.OldFroggerGameInstance;
import net.highwayfrogs.editor.games.sony.oldfrogger.config.OldFroggerConfig;
import net.highwayfrogs.editor.games.sony.oldfrogger.config.OldFroggerFormConfig;
import net.highwayfrogs.editor.games.sony.oldfrogger.config.OldFroggerFormConfig.OldFroggerFormConfigEntry;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.OldFroggerMapFile;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.entity.data.OldFroggerEntityData;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.entity.data.OldFroggerEntityData.OldFroggerEntityDataFactory;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.ui.OldFroggerEditorUtils;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.ui.OldFroggerEntityManager;
import net.highwayfrogs.editor.games.sony.shared.mwd.WADFile.WADEntry;
import net.highwayfrogs.editor.gui.GUIEditorGrid;
import net.highwayfrogs.editor.utils.FileUtils;
import net.highwayfrogs.editor.utils.NumberUtils;
import net.highwayfrogs.editor.utils.logging.ILogger;
import net.highwayfrogs.editor.utils.logging.InstanceLogger.LazyInstanceLogger;

import java.lang.ref.WeakReference;

/**
 * Represents an entity on an old Frogger map.
 * Created by Kneesnap on 12/10/2023.
 */
@Getter
public class OldFroggerMapEntity extends SCGameData<OldFroggerGameInstance> {
    private final OldFroggerMapFile map;
    private int formTypeId;
    private int difficulty;
    private short entityId;
    private OldFroggerEntityData<?> entityData;
    private WeakReference<ILogger> logger;

    public OldFroggerMapEntity(OldFroggerMapFile map) {
        super(map.getGameInstance());
        this.map = map;
    }

    @Override
    public void load(DataReader reader) {
        this.formTypeId = reader.readUnsignedShortAsInt();
        this.difficulty = reader.readUnsignedShortAsInt();
        reader.skipBytesRequireEmpty(2); // Padding
        this.entityId = reader.readShort();

        // Create entity data based on the form, and read it.
        OldFroggerFormConfig formConfig = getMap().getFormConfig();
        OldFroggerFormConfigEntry formConfigEntry = formConfig != null ? formConfig.getFormByType(this.formTypeId) : null;
        OldFroggerEntityDataFactory entityDataFactory = formConfigEntry != null ? formConfigEntry.getEntityDataFactory() : null;
        if (entityDataFactory != null) {
            this.entityData = entityDataFactory.createEntityData(this);
            if (this.entityData != null) {
                int entityDataStartIndex = reader.getIndex();
                try {
                    this.entityData.load(reader);
                } catch (Throwable th) {
                    String errorMessage = "Failed to load " + entityDataFactory.getName() + " for " + getDebugName() + " at " + NumberUtils.toHexString(entityDataStartIndex);
                    getLogger().throwing("OldFroggerMapEntity", "load", new RuntimeException(errorMessage, th));
                    // Don't throw an exception, there's a pointer table so the next entity will read from the right spot.
                }
            }
        }
    }

    @Override
    public void save(DataWriter writer) {
        writer.writeUnsignedShort(this.formTypeId);
        writer.writeUnsignedShort(this.difficulty);
        writer.writeUnsignedShort(0); // Padding
        writer.writeShort(this.entityId);
        if (this.entityData != null)
            this.entityData.save(writer);
    }

    @Override
    public OldFroggerConfig getConfig() {
        return (OldFroggerConfig) super.getConfig();
    }

    @Override
    public ILogger getLogger() {
        ILogger logger = this.logger != null ? this.logger.get() : null;
        if (logger == null)
            this.logger = new WeakReference<>(logger = new LazyInstanceLogger(getGameInstance(), OldFroggerMapEntity::getLoggerInfo, this));

        return logger;
    }

    /**
     * Gets logger information to display when the logger is used.
     */
    public String getLoggerInfo() {
        return getMap().getFileDisplayName() + "|Entity " + this.entityId + "|" + getDebugName();
    }

    /**
     * Setup the editor UI for the entity.
     * @param manager The manager for editing entities.
     * @param editor The editor used to create the UI.
     */
    public void setupEditor(OldFroggerEntityManager manager, GUIEditorGrid editor) {
        editor.addLabel("Form ID ", String.valueOf(this.formTypeId));
        editor.addLabel("Entity ID", String.valueOf(this.entityId));

        OldFroggerMapForm form = getForm();
        if (form != null && form.getMofFile() != null)
            editor.addLabel("MOF / Model", form.getMofFile().getFileDisplayName());

        OldFroggerFormConfig formConfig = getMap().getFormConfig();
        OldFroggerFormConfigEntry formConfigEntry = formConfig != null ? formConfig.getFormByType(this.formTypeId) : null;
        if (formConfigEntry != null)
            editor.addLabel("Form Name", formConfigEntry.getDisplayName());

        // Difficulty Editor
        editor.addSeparator();
        OldFroggerEditorUtils.addDifficultyEditor(editor, this.difficulty, newValue -> this.difficulty = newValue, false, manager::updateEditor);

        // Entity Data Editor
        if (this.entityData != null) {
            editor.addSeparator();
            try {
                this.entityData.setupEditor(manager, editor);
            } catch (Throwable th) {
                editor.addNormalLabel("Encountered an error setting up the editor.");
                getLogger().throwing("OldFroggerMapEntity", "setupEditor", th);
            }
        }
    }

    /**
     * Test if a difficulty level is enabled.
     * @param level The level to test. (0 indexed)
     * @return If the difficulty level is enabled.
     */
    public boolean isDifficultyLevelEnabled(int level) {
        return OldFroggerEditorUtils.isDifficultyLevelEnabled(this.difficulty, level);
    }

    /**
     * Set if a difficulty level is enabled.
     * @param level    The difficulty level to set. (0 indexed)
     * @param newState The new state of this difficulty level.
     */
    public void setDifficultyLevelEnabled(int level, boolean newState) {
        this.difficulty = OldFroggerEditorUtils.setDifficultyLevelEnabled(this.difficulty, level, newState);
    }

    /**
     * Gets the form object utilized by this entity, if there is one.
     * @return The form used by this entity, or null if one does not exist.
     */
    public OldFroggerMapForm getForm() {
        return this.map != null ? this.map.getFormInstancePacket().getFormById(this.formTypeId) : null;
    }

    /**
     * Gets the name which best identifies this entity for debug purposes.
     */
    public String getDebugName() {
        // Get configured form name.
        OldFroggerFormConfig formConfig = getMap().getFormConfig();
        OldFroggerFormConfigEntry formConfigEntry = formConfig != null ? formConfig.getFormByType(this.formTypeId) : null;
        if (formConfigEntry != null && formConfigEntry.getDisplayName() != null)
            return formConfigEntry.getDisplayName();

        // Get display name of MOF.
        OldFroggerMapForm form = getForm();
        if (form != null) {
            WADEntry mofEntry = form.getMofFileEntry();
            if (mofEntry != null)
                return FileUtils.stripExtension(mofEntry.getDisplayName());
        }

        return "Entity #" + this.entityId;
    }
}