package net.highwayfrogs.editor.games.sony.frogger.map.data.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.file.config.exe.general.FormEntry;
import net.highwayfrogs.editor.games.sony.SCGameData;
import net.highwayfrogs.editor.games.sony.frogger.FroggerConfig;
import net.highwayfrogs.editor.games.sony.frogger.FroggerGameInstance;
import net.highwayfrogs.editor.games.sony.frogger.map.FroggerMapFile;
import net.highwayfrogs.editor.games.sony.frogger.map.data.entity.data.FroggerEntityData;
import net.highwayfrogs.editor.games.sony.frogger.map.data.entity.data.FroggerEntityDataMatrix;
import net.highwayfrogs.editor.games.sony.frogger.map.data.entity.data.FroggerEntityDataPathInfo;
import net.highwayfrogs.editor.games.sony.frogger.map.data.entity.data.IFroggerFlySpriteData;
import net.highwayfrogs.editor.games.sony.frogger.map.data.entity.script.FroggerEntityScriptData;
import net.highwayfrogs.editor.games.sony.frogger.map.data.form.FroggerFormGrid;
import net.highwayfrogs.editor.games.sony.frogger.map.data.form.FroggerOldMapForm;
import net.highwayfrogs.editor.games.sony.frogger.map.data.form.FroggerOldMapFormData;
import net.highwayfrogs.editor.games.sony.frogger.map.data.form.IFroggerFormEntry;
import net.highwayfrogs.editor.games.sony.frogger.map.data.path.FroggerPathInfo;
import net.highwayfrogs.editor.games.sony.frogger.map.packets.FroggerMapFilePacketEntity;
import net.highwayfrogs.editor.games.sony.frogger.map.packets.FroggerMapFilePacketForm;
import net.highwayfrogs.editor.games.sony.frogger.map.ui.editor.central.FroggerUIMapEntityManager;
import net.highwayfrogs.editor.games.sony.shared.mof2.MRModel;
import net.highwayfrogs.editor.games.sony.shared.mwd.WADFile.WADEntry;
import net.highwayfrogs.editor.gui.GUIEditorGrid;
import net.highwayfrogs.editor.system.AbstractIndexStringConverter;
import net.highwayfrogs.editor.system.AbstractStringConverter;
import net.highwayfrogs.editor.utils.DataUtils;
import net.highwayfrogs.editor.utils.FXUtils;
import net.highwayfrogs.editor.utils.StringUtils;
import net.highwayfrogs.editor.utils.Utils;
import net.highwayfrogs.editor.utils.data.reader.ArraySource;
import net.highwayfrogs.editor.utils.data.reader.DataReader;
import net.highwayfrogs.editor.utils.data.writer.DataWriter;
import net.highwayfrogs.editor.utils.logging.ILogger;
import net.highwayfrogs.editor.utils.logging.InstanceLogger.LazyInstanceLogger;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Represents the "ENTITY" struct.
 * Created by Kneesnap on 8/24/2018.
 */
public class FroggerMapEntity extends SCGameData<FroggerGameInstance> {
    @Getter private final FroggerMapFile mapFile;
    @Getter private int formGridId = -1;

    // Unique Entity IDs are per-level, and were tracked by Mappy. It seems that every time mappy needed to assign an ID, it would increment a counter (starting at zero), and assign the previous counter value.
    // These IDs were somehow able to reach into the thousands for some levels.
    // These IDs are not as important as they might seem however, as they are only used for:
    // - VOL Trigger switches (those colored switches for platforms) use it to control activate/de-activate entities.
    // - ORG Snakes that ride on logs specify the entity ID of the log they should ride.
    // - ORG Baby Frogs that ride on logs specify the entity ID of the log they should ride.
    // - sky_froggers_magical_popping_balloon to activate the bird flock
    // - When flies bounce up & down, their unique ID has the first 3 bits taken (so a number between 0 and 7), and is used as something of an initial Y offset.
    @Getter private int uniqueId = -1;
    @Getter private IFroggerFormEntry formEntry;
    @Getter private short flags;
    @Getter private FroggerEntityData entityData;
    @Getter private FroggerEntityScriptData scriptData;
    @Getter @Setter private transient byte[] rawData;
    @Getter @Setter private transient boolean invalid; // This is set if we know that the entity data we loaded was not the proper size.
    private WeakReference<ILogger> logger;

    private static final int RUNTIME_POINTERS = 4;

    public FroggerMapEntity(FroggerMapFile mapFile) {
        super(mapFile != null ? mapFile.getGameInstance() : null);
        this.mapFile = mapFile;
    }

    public FroggerMapEntity(FroggerMapFile mapFile, IFroggerFormEntry formEntry) {
        this(mapFile);
        setFormBookEntry(formEntry);
    }

    @Override
    public FroggerConfig getConfig() {
        return (FroggerConfig) super.getConfig();
    }

    @Override
    public void load(DataReader reader) {
        this.formGridId = reader.readUnsignedShortAsInt();
        this.uniqueId = reader.readUnsignedShortAsInt();

        // Warn about invalid form grid IDs.
        if (this.formGridId < 0 || this.formGridId >= Math.max(getMapFile().getFormPacket().getOldForms().size(), getMapFile().getFormPacket().getForms().size()))
            getLogger().warning("The entity uses an invalid form grid ID! (%d).", this.formGridId);

        int formId = this.formGridId;
        if (this.mapFile.getMapConfig().isOldFormFormat()) {
            this.formEntry = this.mapFile.getFormPacket().getOldForms().get(this.formGridId);
            this.flags = 0;
            reader.skipBytesRequireEmpty(2 * Constants.POINTER_SIZE); // Skip runtime pointers.
        } else {
            formId = reader.readUnsignedShortAsInt();
            this.formEntry = getGameInstance().getMapFormEntry(this.mapFile.getMapTheme(), formId);
            this.flags = reader.readShort();
            warnAboutInvalidBitFlags(this.flags, FroggerMapEntityFlag.FLAG_VALIDATION_MASK);
            reader.skipBytesRequireEmpty(RUNTIME_POINTERS * Constants.POINTER_SIZE);
        }

        if (this.formEntry == null) {
            this.entityData = new FroggerEntityDataMatrix(this.mapFile);
            getLogger().warning("Failed to find form for entity " + this.uniqueId + "/Form: " + formId + "/" + this.formGridId + ".");
        }
    }

    /**
     * Reads the entity data from the current position.
     * @param reader the reader to read it from
     */
    public void loadEntityData(DataReader reader) {
        if (this.formEntry == null)
            return; // Can't read more data. Ideally this doesn't happen, but this is a good failsafe. It's most likely to happen in early builds, and it does happen in Build 01.

        try {
            this.entityData = FroggerEntityData.makeData(this, this.formEntry);
            if (this.entityData != null) {
                this.entityData.load(reader);
                reader.alignRequireEmpty(Constants.INTEGER_SIZE); // Padding.
            }

            this.scriptData = FroggerEntityScriptData.makeData(this, this.formEntry);
            if (this.scriptData != null) {
                this.scriptData.load(reader);
                reader.alignRequireEmpty(Constants.INTEGER_SIZE); // Padding.
            }
        } catch (Throwable th) {
            Utils.handleError(getLogger(), th, false, "Failed to load entity data for entity %d/%s.", this.uniqueId, this.formEntry.getFormTypeName());
        }

        // Warnings!

        if (this.entityData instanceof FroggerEntityDataPathInfo && getPathInfo().getPath() == null && !getMapFile().isIslandOrIslandPlaceholder())
            getLogger().warning("Entity references an invalid path ID! (%d)", getPathInfo().getPathId());
        if (testFlag(FroggerMapEntityFlag.HIDDEN))
            getLogger().warning("I have the HIDDEN flag, which was expected to be runtime-only!");
    }

    @Override
    public void save(DataWriter writer) {
        // Warnings!
        if (this.formGridId < 0 || this.formGridId >= Math.max(getMapFile().getFormPacket().getOldForms().size(), getMapFile().getFormPacket().getForms().size()))
            getGameInstance().showWarning(getLogger(), "The entity uses an invalid form grid ID! (%d).\nThis may cause issues in-game!", this.formGridId);
        if (this.entityData instanceof FroggerEntityDataPathInfo && getPathInfo().getPath() == null && !getMapFile().isIslandOrIslandPlaceholder())
            getGameInstance().showWarning(getLogger(), "The entity references an invalid path ID! (%d).\nThis may cause issues in-game!", getPathInfo().getPathId());

        writer.writeUnsignedShort(this.formGridId);
        writer.writeUnsignedShort(this.uniqueId);
        if (this.mapFile.getMapConfig().isOldFormFormat()) {
            writer.writeNull(2 * Constants.POINTER_SIZE); // Skip runtime pointers.
        } else {
            writer.writeUnsignedShort(((FormEntry) this.formEntry).getMapFormId());
            writer.writeShort(this.flags);
            writer.writeNull(RUNTIME_POINTERS * Constants.POINTER_SIZE);
        }

        if (this.entityData != null) {
            this.entityData.save(writer);
            writer.align(Constants.INTEGER_SIZE);
        }
        if (this.scriptData != null) {
            this.scriptData.save(writer);
            writer.align(Constants.INTEGER_SIZE);
        }
    }

    /**
     * Gets the index of the entity within the entity table packet.
     */
    public int getEntityIndex() {
        FroggerMapFilePacketEntity entityPacket = this.mapFile.getEntityPacket();
        return entityPacket.getLoadingIndex(entityPacket.getEntities(), this);
    }

    @Override
    public ILogger getLogger() {
        ILogger logger = this.logger != null ? this.logger.get() : null;
        if (logger == null)
            this.logger = new WeakReference<>(logger = new LazyInstanceLogger(getGameInstance(), FroggerMapEntity::getLoggerInfo, this));

        return logger;
    }

    /**
     * Gets logger information to display when the logger is used.
     */
    public String getLoggerInfo() {
        return this.mapFile.getFileDisplayName() + "|Entity " + getEntityIndex() + "/" + this.uniqueId + "|" + getTypeName();
    }

    /**
     * Gets the form grid active for this entity, if there is one.
     */
    public FroggerFormGrid getFormGrid() {
        if (this.mapFile.getMapConfig().isOldFormFormat())
            return null; // Extremely early maps (April 97) use an outdated form system.

        List<FroggerFormGrid> forms = this.mapFile.getFormPacket().getForms();
        if (this.formGridId >= 0 && forms.size() > this.formGridId) {
            return forms.get(this.formGridId);
        } else { // This form is invalid, so show this as a text box.
            return null;
        }
    }

    /**
     * Setup the editor UI for the entity.
     * @param manager The manager for editing entities.
     * @param editor The editor used to create the UI.
     */
    public void setupEditor(FroggerUIMapEntityManager manager, GUIEditorGrid editor) {
        // Get a list of valid forms for the level.
        IFroggerFormEntry[] formEntries = getGameInstance().getAllowedForms(this.mapFile.getMapTheme());
        if (!Utils.contains(formEntries, this.formEntry)) // This wasn't found in this
            formEntries = getGameInstance().getFullFormBook().toArray(new FormEntry[0]);

        editor.addBoldLabel("General Information:");
        if (this.formEntry instanceof FormEntry) {
            editor.addLabel("Entity Type", getTypeName());

            editor.addEnumSelector("Form Type", this.formEntry, formEntries, false, newEntry -> {
                setFormBookEntry(newEntry);

                if (manager != null) {
                    manager.getController().getFormManager().refreshList(); // We may have added a new form to the list, so update it!
                    manager.updateEntityMesh(this);
                    manager.updateEntityPositionRotation(this);
                    manager.updateEditor();
                }
            }).setConverter(new AbstractStringConverter<>(IFroggerFormEntry::getFormTypeName));
        } else if (this.formEntry instanceof FroggerOldMapForm) {
            FroggerOldMapForm oldFormEntry = (FroggerOldMapForm) this.formEntry;
            editor.addLabel("Entity Type", this.formEntry.getEntityTypeName());
            WADEntry wadEntry = oldFormEntry.getEntityModelWadEntry(this);
            editor.addLabel("MOF Index", oldFormEntry.getMofId() + " (" + (wadEntry != null ? wadEntry.getDisplayName() : "null") + ")");
        } else if (this.formEntry != null) {
            editor.addLabel("Unsupported Form Type", Utils.getSimpleName(this.formEntry));
            editor.addLabel("Form Type Name", this.formEntry.getFormTypeName());
            editor.addLabel("Entity Type Name", this.formEntry.getEntityTypeName());
        } else {
            editor.addLabel("Entity Type", "Unknown");
        }

        editor.addUnsignedShortField("Entity ID", this.uniqueId,
                newUniqueId -> this.uniqueId == newUniqueId || (getMapFile().getEntityPacket().getEntityByUniqueId(newUniqueId) == null && getMapFile().getEntityPacket().getNextFreeEntityId() > newUniqueId && newUniqueId >= 0),
                newUniqueId -> setUniqueID(newUniqueId, false));

        // Show form data.
        List<FroggerOldMapFormData> oldForms = (this.formEntry instanceof FroggerOldMapForm) ? ((FroggerOldMapForm) this.formEntry).getFormDataEntries() : null;
        List<FroggerFormGrid> formGrids = this.mapFile.getFormPacket().getForms();
        if (this.mapFile.getMapConfig().isOldFormFormat() && this.formGridId >= 0 && oldForms != null && oldForms.size() > this.formGridId) {
            editor.addSelectionBox("Form Grid", oldForms.get(this.formGridId), oldForms, newForm -> this.formGridId = oldForms.indexOf(newForm))
                    .setConverter(new AbstractIndexStringConverter<>(oldForms, (index, form) -> "Form #" + index + " (" + DataUtils.fixedPointShortToFloat4Bit(form.getXMin()) + "," + DataUtils.fixedPointShortToFloat4Bit(form.getYMin()) + "," + DataUtils.fixedPointShortToFloat4Bit(form.getZMin()) + ")"));
        } else if (!this.mapFile.getMapConfig().isOldFormFormat() && this.formGridId >= 0 && formGrids.size() > this.formGridId) {
            // Get a list of only the available form grids.
            List<FroggerFormGrid> availableForms = new ArrayList<>();
            for (int i = 0; i < formGrids.size(); i++) {
                FroggerFormGrid formGrid = formGrids.get(i);
                if (formGrid.getFormEntry() == null || formGrid.getFormEntry() == this.formEntry)
                    availableForms.add(formGrid);
            }

            editor.addSelectionBox("Form Grid", formGrids.get(this.formGridId), availableForms, newForm -> setFroggerFormGrid(getFormEntry(), newForm))
                    .setConverter(new AbstractIndexStringConverter<>(availableForms, (index, form) -> "Form #" + formGrids.indexOf(form) + " (" + form.getXGridSquareCount() + "," + form.getZGridSquareCount() + ")"));
        } else { // This form is invalid, so show this as a text box.
            editor.addUnsignedShortField("Form Grid ID", this.formGridId, newFormGridId -> this.formGridId = newFormGridId);
        }

        editor.addBoldLabel("Flags:");
        for (FroggerMapEntityFlag flag : FroggerMapEntityFlag.values())
            if (flag.isShowInEditor())
                editor.addCheckBox(StringUtils.capitalize(flag.name()), testFlag(flag), newState -> {
                    setFlag(flag, newState);
                    manager.updateEntityPositionRotation(this);
                }).setTooltip(FXUtils.createTooltip(flag.getDescription()));

        // Copulate entity data editor.
        if (this.entityData != null) {
            editor.addSeparator(25);
            editor.addBoldLabel("Entity Data:");

            try {
                this.entityData.setupEditor(editor, manager);
            } catch (Throwable th) {
                Utils.handleError(getLogger(), th, false, "Encountered an error while setting up the entity data editor.");
                editor.addNormalLabel("Encountered an error setting up the entity data editor.");
            }
        }

        // Create script data editor.
        if (this.scriptData != null) {
            editor.addSeparator(25);
            editor.addBoldLabel("Script Data:");

            try {
                this.scriptData.setupEditor(editor, manager);
            } catch (Throwable th) {
                Utils.handleError(getLogger(), th, false, "Encountered an error while setting up the entity script data editor.");
                editor.addNormalLabel("Encountered an error setting up the entity script data editor.");
            }
        }

        // Add raw data.
        if (this.rawData != null)
            editor.addTextField("Raw Data", DataUtils.toByteString(this.rawData));
    }

    /**
     * Sets the unique ID of the entity.
     * @param newUniqueId the new unique ID to apply
     * @param skipIdChecks whether checks on the validity of the ID should be skipped.
     */
    public void setUniqueID(int newUniqueId, boolean skipIdChecks) {
        if (this.uniqueId == newUniqueId)
            return; // No change.
        if (newUniqueId < 0)
            throw new IllegalArgumentException("Invalid unique ID provided: " + newUniqueId);

        // Checks are also skipped if the entity is not registered.
        if (!skipIdChecks || getEntityIndex() < 0) {
            if (getMapFile().getEntityPacket().getEntityByUniqueId(newUniqueId) != null)
                throw new RuntimeException("Cannot apply unique ID of " + newUniqueId + ", as another entity already has this ID!");
            if (newUniqueId >= getMapFile().getEntityPacket().getNextFreeEntityId())
                throw new RuntimeException("Cannot apply unique ID of " + newUniqueId + ", as it would invalidate the next free entity ID of " + getMapFile().getEntityPacket().getNextFreeEntityId() + ".");
        }

        this.uniqueId = newUniqueId;
    }

    /**
     * Gets the name of the entity type.
     */
    public String getTypeName() {
        return this.formEntry != null ? this.formEntry.getEntityTypeName() : null;
    }

    /**
     * Gets the entity model, if there is one we know how to resolve.
     * @return modelFile
     */
    public WADEntry getEntityModelWadEntry() {
        return this.formEntry != null ? this.formEntry.getEntityModelWadEntry(this) : null;
    }

    /**
     * Gets the entity model, if there is one we know how to resolve.
     * @return entityModel
     */
    public MRModel getEntityModel() {
        return this.formEntry != null ? this.formEntry.getEntityModel(this) : null;
    }

    /**
     * Test if this entity has a particular flag.
     * @param flag The flag to test.
     * @return hasFlag
     */
    public boolean testFlag(FroggerMapEntityFlag flag) {
        return (this.flags & flag.getBitFlagMask()) == flag.getBitFlagMask();
    }

    /**
     * Set the flag state.
     * @param flag     The flag type.
     * @param newState The new state of the flag.
     */
    public void setFlag(FroggerMapEntityFlag flag, boolean newState) {
        boolean oldState = testFlag(flag);
        if (oldState == newState)
            return; // Prevents the ^ operation from breaking the value.

        if (newState) {
            this.flags |= (short) flag.getBitFlagMask();
        } else {
            this.flags &= (short) ~flag.getBitFlagMask();
        }
    }

    /**
     * Get any FroggerPathInfo owned by this entity, if it has any.
     * @return pathState
     */
    public FroggerPathInfo getPathInfo() {
        return this.entityData instanceof FroggerEntityDataPathInfo ? ((FroggerEntityDataPathInfo) this.entityData).getPathInfo() : null;
    }

    /**
     * Get any FroggerEntityDataPathInfo owned by this entity, if its entity data is of that type.
     * @return pathEntityData
     */
    public FroggerEntityDataPathInfo getPathEntityData() {
        return this.entityData instanceof FroggerEntityDataPathInfo ? ((FroggerEntityDataPathInfo) this.entityData) : null;
    }

    /**
     * Get any FroggerEntityDataMatrix owned by this entity, if its entity data is of that type.
     * @return matrixEntityData
     */
    public FroggerEntityDataMatrix getMatrixEntityData() {
        return this.entityData instanceof FroggerEntityDataMatrix ? ((FroggerEntityDataMatrix) this.entityData) : null;
    }

    /**
     * Gets the fly score type, if the entity has one.
     */
    public FroggerFlyScoreType getFlyScoreType() {
        FroggerFlyScoreType flyType = null;
        if (this.entityData instanceof IFroggerFlySpriteData) {
            flyType = ((IFroggerFlySpriteData) this.entityData).getFlyType();
        } else if (this.scriptData instanceof IFroggerFlySpriteData) {
            flyType = ((IFroggerFlySpriteData) this.scriptData).getFlyType();
        }

        return flyType;
    }

    /**
     * Get the x, y, z position of this entity.
     * @return positionAndRotationData
     */
    public float[] getPositionAndRotation(float[] positionData) {
        if (this.entityData == null)
            throw new UnsupportedOperationException("Tried to get the position of an entity without position data!");

        if (positionData == null)
            positionData = new float[6];

        return this.entityData.getPositionAndRotation(positionData);
    }

    /**
     * Creates a clone of the FroggerMapEntity.
     */
    public FroggerMapEntity clone() {
        byte[] entityData = writeDataToByteArray();
        FroggerMapEntity clonedEntity = new FroggerMapEntity(this.mapFile);
        DataReader reader = new DataReader(new ArraySource(entityData));
        clonedEntity.load(reader);
        clonedEntity.loadEntityData(reader);
        return clonedEntity;
    }

    /**
     * Set this entity's form book entry.
     * @param newEntry The new form book entry.
     */
    public void setFormBookEntry(IFroggerFormEntry newEntry) {
        Class<?> oldScriptClass = FroggerEntityScriptData.getScriptDataClass(getGameInstance(), this.formEntry);
        Class<?> newScriptClass = FroggerEntityScriptData.getScriptDataClass(getGameInstance(), newEntry);
        if (this.formEntry == null || !Objects.equals(newScriptClass, oldScriptClass))
            this.scriptData = FroggerEntityScriptData.makeData(this, newEntry);

        Class<?> oldEntityDataClass = FroggerEntityData.getEntityDataClass(getGameInstance(), this.formEntry);
        Class<?> newEntityDataClass = FroggerEntityData.getEntityDataClass(getGameInstance(), newEntry);
        if (this.formEntry == null || !Objects.equals(newEntityDataClass, oldEntityDataClass)) {
            boolean hadOldEntityData = (this.entityData != null);
            if (hadOldEntityData) // Remove any previous tracking.
                getMapFile().getPathPacket().removeEntityFromPathTracking(this);

            // Call before setting entityData to new data.
            FroggerEntityDataMatrix oldMatrixData = getMatrixEntityData();
            FroggerEntityDataPathInfo oldPathData = getPathEntityData();

            // Setup new data.
            this.entityData = FroggerEntityData.makeData(this, newEntry);

            // Copy data.
            if (this.entityData instanceof FroggerEntityDataMatrix) {
                ((FroggerEntityDataMatrix) this.entityData).copyFrom(oldMatrixData, oldPathData);
            } else if (this.entityData instanceof FroggerEntityDataPathInfo) {
                ((FroggerEntityDataPathInfo) this.entityData).copyFrom(oldMatrixData, oldPathData);
            }

            // If old entity data was present (entity isn't loading), then update the path to point to the new data
            if (hadOldEntityData)
                getMapFile().getPathPacket().addEntityToPathTracking(this);

            // Attempt to find the form compatible with the new entity data.
            if ((hadOldEntityData || this.formGridId < 0) && newEntry != null && !this.mapFile.getMapConfig().isOldFormFormat()) {
                // Create a shared form if one does not exist.
                if (newEntry.getFormGrid() == null) {
                    FroggerFormGrid newFormGrid = new FroggerFormGrid(getGameInstance());
                    newEntry.setFormGrid(newFormGrid);
                    newFormGrid.initFormEntry(newEntry);
                }

                setFroggerFormGrid(newEntry, newEntry.getFormGrid());
            }
        }

        this.formEntry = newEntry;
    }

    private void setFroggerFormGrid(IFroggerFormEntry formEntry, FroggerFormGrid formGrid) {
        if (formEntry == null)
            throw new NullPointerException("formEntry");
        if (formGrid == null)
            throw new NullPointerException("formGrid");
        if (formGrid.getMapFile() != null && formGrid.getMapFile() != this.mapFile)
            throw new IllegalArgumentException("The formGrid was for the map '" + formGrid.getMapFile().getFileDisplayName() + "', not '" + this.mapFile.getFileDisplayName() + "'.");
        if (formGrid.getFormEntry() != null && formGrid.getFormEntry() != formEntry)
            throw new IllegalArgumentException("The provided form grid is for " + formGrid.getFormEntry().getFormTypeName() + ", but the provided formEntry was for " + formEntry.getFormTypeName() + ".");

        FroggerMapFilePacketForm formPacket = this.mapFile.getFormPacket();
        int formGridIndex = formPacket.getForms().indexOf(formGrid);
        if (formGridIndex < 0) // Add the form grid to the map.
            formGridIndex = formPacket.addFormGrid(formGrid);

        this.formGridId = formGridIndex;
        formPacket.convertLocalFormToGlobalForm(formGridIndex, formEntry);
    }

    /**
     * Automatically called when a form grid at a particular index was removed.
     * Do not call this unless you are the form packet.
     * @param formGridIndex the index of the form to remove.
     */
    public void onFormGridRemoved(int formGridIndex) {
        if (formGridIndex < 0)
            throw new IllegalArgumentException("formGridIndex was " + formGridIndex);
        if (this.formGridId == formGridIndex) // Should not occur.
            throw new IllegalStateException("The formGridIndex of " + formGridIndex + " was removed while it was still in use by an entity! (" + getLoggerInfo() + ")");
        if (this.formGridId > formGridIndex)
            this.formGridId--;
    }

    @Getter
    @AllArgsConstructor
    public enum FroggerMapEntityFlag {
        HIDDEN(Constants.BIT_FLAG_0, false, "Don't create a live entity.\nThis is runtime only, and is used to for example, hide the checkpoint entities once collected."),
        NO_DISPLAY(Constants.BIT_FLAG_1, true, "Don't display any 3D model in-game."),
        NO_MOVEMENT(Constants.BIT_FLAG_2, true, "Disable entity movement."),
        NO_COLLISION(Constants.BIT_FLAG_3, true, "Collision does not apply to this entity.\nNot used by the original game maps,\n but it is applied at runtime for entities like JUN_ROPE_BRIDGE."),
        ALIGN_TO_WORLD(Constants.BIT_FLAG_4, true, "Do not face the path's direction. (Path Entity Only)"),
        PROJECT_ON_LAND(Constants.BIT_FLAG_5, true, "Snap rotation to the grid square polygon. (Path Entity Only)"),
        LOCAL_ALIGN(Constants.BIT_FLAG_6, true, "Entity position matrix is calculated \"locally\" (Pipe Slugs)");

        private final int bitFlagMask;
        private final boolean showInEditor;
        private final String description;
        public static final int FLAG_VALIDATION_MASK = 0b1111110;
    }
}