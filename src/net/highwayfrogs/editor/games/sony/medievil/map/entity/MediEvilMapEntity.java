package net.highwayfrogs.editor.games.sony.medievil.map.entity;

import lombok.Getter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.file.mof.MOFHolder;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.standard.SVector;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.sony.SCGameData;
import net.highwayfrogs.editor.games.sony.medievil.MediEvilGameInstance;
import net.highwayfrogs.editor.games.sony.medievil.MediEvilLevelTableEntry;
import net.highwayfrogs.editor.games.sony.medievil.config.MediEvilConfig;
import net.highwayfrogs.editor.games.sony.medievil.entity.MediEvilEntityDefinition.MediEvilModelEntityData;
import net.highwayfrogs.editor.games.sony.medievil.map.MediEvilMapFile;
import net.highwayfrogs.editor.games.sony.medievil.entity.MediEvilEntityDefinition;
import net.highwayfrogs.editor.games.sony.medievil.map.ui.MediEvilEntityManager;
import net.highwayfrogs.editor.gui.GUIEditorGrid;
import net.highwayfrogs.editor.gui.mesh.fxobject.TranslationGizmo.IPositionChangeListener;
import net.highwayfrogs.editor.utils.Utils;

import java.util.UUID;

/**
 * Represents an entity on a MediEvil map.
 * Created by RampantSpirit on 3/11/2024.
 */
@Getter
public class MediEvilMapEntity extends SCGameData<MediEvilGameInstance> {
    private static final UUID INITIAL_POSITION_EDITOR_ID = UUID.randomUUID();

    private final MediEvilMapFile map;
    private int entityId = -1;
    private int formId = -1;
    private short subFormId = 0xFF;
    private short baseGenericData; // Seems to mean different things in different entities? not 100% sure.
    private short rotationX;
    private short rotationY;
    private short rotationZ;
    private final SVector position = new SVector();
    private long triggerData;
    private int initFlags;
    private int destroyFlags;
    private final int[] genericData = new int[4]; // Seems it's just like old/pre-recode Frogger.

    public MediEvilMapEntity(MediEvilMapFile map) {
        super(map.getGameInstance());
        this.map = map;
    }

    @Override
    public void load(DataReader reader) {
        this.entityId = reader.readInt();
        this.formId = reader.readUnsignedShortAsInt();
        this.subFormId = reader.readUnsignedByteAsShort();
        this.baseGenericData = reader.readUnsignedByteAsShort();
        reader.skipBytesRequireEmpty(2 * Constants.POINTER_SIZE); // Skip two runtime pointers.

        this.rotationX = reader.readUnsignedByteAsShort();
        this.rotationY = reader.readUnsignedByteAsShort();
        this.rotationZ = reader.readUnsignedByteAsShort();
        reader.skipBytes(1); // Padding (1 Byte?)

        // Read positional data.
        this.position.loadWithPadding(reader);
        SVector runtimePosition = SVector.readWithPadding(reader);
        if (!this.position.equals(runtimePosition))
            getLogger().warning("Entity had a different 'runtimePosition' from initialPosition. (Initial: " + this.position + ", Runtime: " + runtimePosition + ")");

        // Read remaining data.
        this.triggerData = reader.readUnsignedIntAsLong();
        this.initFlags = reader.readInt();
        this.destroyFlags = reader.readInt();
        for (int i = 0; i < this.genericData.length; i++)
            this.genericData[i] = reader.readInt();
    }

    @Override
    public void save(DataWriter writer) {
        writer.writeInt(this.entityId);
        writer.writeUnsignedShort(this.formId);
        writer.writeUnsignedByte(this.subFormId);
        writer.writeUnsignedByte(this.baseGenericData);
        writer.writeNullPointer();
        writer.writeNullPointer();
        writer.writeUnsignedByte(this.rotationX);
        writer.writeUnsignedByte(this.rotationY);
        writer.writeUnsignedByte(this.rotationZ);
        writer.writeUnsignedByte((short) 0xFF);
        this.position.saveWithPadding(writer);
        this.position.saveWithPadding(writer);
        writer.writeUnsignedInt(this.triggerData);
        writer.writeInt(this.initFlags);
        writer.writeInt(this.destroyFlags);
        for (int i = 0; i < this.genericData.length; i++)
            writer.writeInt(this.genericData[i]);
    }

    /**
     * Gets the entity data used by this entity.
     * If the data cannot be found or accessed, null is returned.
     */
    public MediEvilEntityDefinition getEntityDefinition() {
        return getGameInstance().getEntityTable().getDefinition(this.formId);
    }

    /**
     * Gets the mof file associated with the form, if it can be found.
     */
    public MOFHolder getMof() {
        MediEvilEntityDefinition definition = getEntityDefinition();
        if (definition != null) {
            // Attempt to get the model from the model lists.
            if (definition.getModelData().size() > this.subFormId && this.subFormId != 0xFF) {
                MediEvilModelEntityData data = definition.getModelData().get(this.subFormId);
                if (data.getMofIndex() != 0) {
                    MOFHolder holder = getGameInstance().getGameFileByResourceID(data.getMofIndex(), MOFHolder.class, true);
                    if (holder != null) {
                        return holder;
                    } else {
                        getLogger().warning("Failed to find MOF from Model List's resource ID " + definition.getMofId() + "...");
                    }
                }
            }

            // Attempt to use this as a MOF file directly.
            if (definition.getMofId() != 0) {
                MOFHolder holder = definition.getMOF();
                if (holder != null) {
                    return holder;
                } else {
                    getLogger().warning("Failed to find MOF from resource ID " + definition.getMofId() + "...");
                }
            }
        }

        if (this.map == null)
            return null;

        // Attempt to look up from a form.
        MediEvilLevelTableEntry levelTableEntry = this.map.getLevelTableEntry();
        if (levelTableEntry == null) {
            getLogger().warning("Couldn't get level table entry, which prevents getting the mof file for a form.");
            return null;
        }

        // TODO: Rampant Spirit put this here, but idk what it's for / it looks like it needs to be finished.

        return null;
    }

    @Override
    public MediEvilConfig getConfig() {
        return (MediEvilConfig) super.getConfig();
    }

    /**
     * Setup the editor UI for the entity.
     */
    public void setupEditor(MediEvilEntityManager manager, GUIEditorGrid editor) {
        editor.addIntegerField("Entity ID", this.entityId, newEntityId -> this.entityId = newEntityId, null);

        MediEvilEntityDefinition definition = getEntityDefinition();
        if (definition != null) {
            editor.addLabel("Entity Type", definition.getName());
            editor.addLabel("Overlay", (definition.getOverlay() != null ? definition.getOverlay().getFilePath() : "None"));
        } else {
            editor.addIntegerField("Form ID", this.formId, newFormId -> this.formId = newFormId, null).setDisable(true);
        }

        editor.addShortField("Sub-Form ID", this.subFormId, newFormId -> this.subFormId = newFormId, value -> value >= 0 && value <= 0xFF);

        // Position & Rotation
        IPositionChangeListener updatePositionCallback = IPositionChangeListener.makeListener(() -> manager.updateEntityPositionRotation(this));
        editor.addPositionEditor(manager.getController(), INITIAL_POSITION_EDITOR_ID, "Position", this.position, updatePositionCallback);
        editor.addIntegerSlider("Rotation X", this.rotationX, newX -> {
            this.rotationX = (short) (int) newX;
            manager.updateEntityPositionRotation(this);
        }, 0, 0xFF);
        editor.addIntegerSlider("Rotation Y", this.rotationY, newY -> {
            this.rotationY = (short) (int) newY;
            manager.updateEntityPositionRotation(this);
        }, 0, 0xFF);
        editor.addIntegerSlider("Rotation Z", this.rotationZ, newZ -> {
            this.rotationZ = (short) (int) newZ;
            manager.updateEntityPositionRotation(this);
        }, 0, 0xFF);

        // Other data
        editor.addLabel("Trigger Data: ", Utils.toHexString(this.triggerData));
        editor.addIntegerField("Init Flags", this.initFlags, newFlags -> this.initFlags = newFlags, null);
        editor.addIntegerField("Destroy Flags", this.destroyFlags, newFlags -> this.destroyFlags = newFlags, null);
        editor.addShortField("Base Generic Data", this.baseGenericData, newFormId -> this.baseGenericData = newFormId, value -> value >= 0 && value <= 0xFF);
        for (int i = 0; i < this.genericData.length; i++) {
            final int index = i;
            editor.addIntegerField("Generic Data " + i, this.genericData[i], newValue -> this.genericData[index] = newValue, null);
        }
    }
}