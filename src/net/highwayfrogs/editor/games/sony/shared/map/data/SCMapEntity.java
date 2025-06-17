package net.highwayfrogs.editor.games.sony.shared.map.data;

import lombok.Getter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.file.standard.SVector;
import net.highwayfrogs.editor.games.sony.SCGameData.SCSharedGameData;
import net.highwayfrogs.editor.games.sony.SCGameInstance;
import net.highwayfrogs.editor.games.sony.SCGameType;
import net.highwayfrogs.editor.games.sony.medievil.entity.MediEvilEntityDefinition;
import net.highwayfrogs.editor.games.sony.shared.map.SCMapFile;
import net.highwayfrogs.editor.games.sony.shared.map.ui.SCMapEntityManager;
import net.highwayfrogs.editor.gui.GUIEditorGrid;
import net.highwayfrogs.editor.gui.mesh.fxobject.TranslationGizmo.IPositionChangeListener;
import net.highwayfrogs.editor.utils.data.reader.DataReader;
import net.highwayfrogs.editor.utils.data.writer.DataWriter;

import java.util.UUID;

/**
 * Represents a map entity as seen in MediEvil 2 & C-12. Much of this is carried over from MediEvil 1.
 * Created by Kneesnap on 5/14/2024.
 */
@Getter
public class SCMapEntity extends SCSharedGameData {
    private static final UUID INITIAL_POSITION_EDITOR_ID = UUID.randomUUID();

    private final SCMapFile<? extends SCGameInstance> map;
    private int linkIndex;
    private EntityLinkTargetType linkTarget = EntityLinkTargetType.NONE;
    private int entityId = -1;
    private int formId = -1;
    private short subFormId = 0xFF;
    private short rotationX;
    private short rotationY;
    private short rotationZ;
    private final SVector position = new SVector();
    private int initFlags;
    private int destroyFlags;
    private short group;
    private short scriptIndex;
    private int species;
    private final int[] genericData = new int[4]; // Seems it's just like old/pre-recode Frogger.

    public static final int BIT_LENGTH_SECTION_INDEX = 5;
    public static final int BIT_LENGTH_INDEX_IN_SECTION = 11;
    public static final int MAX_SECTION_INDEX = 0b11111;
    public static final int MAX_INDEX_IN_SECTION = 0b11111111111;


    public SCMapEntity(SCMapFile<? extends SCGameInstance> map) {
        super(map.getGameInstance());
        this.map = map;
    }

    @Override
    public void load(DataReader reader) {
        this.linkIndex = reader.readUnsignedShortAsInt();
        this.linkTarget = EntityLinkTargetType.values()[reader.readByte()];
        reader.skipBytesRequireEmpty(1);
        reader.skipBytesRequireEmpty(2 * Constants.POINTER_SIZE); // Skip two runtime pointers.
        this.entityId = reader.readInt();
        this.formId = reader.readInt();
        this.subFormId = reader.readUnsignedByteAsShort();

        this.rotationX = reader.readUnsignedByteAsShort();
        this.rotationY = reader.readUnsignedByteAsShort();
        this.rotationZ = reader.readUnsignedByteAsShort();

        // Read positional data.
        this.position.loadWithPadding(reader);
        SVector runtimePosition = SVector.readWithPadding(reader);
        if (!this.position.equals(runtimePosition))
            getLogger().warning("Entity had a different 'runtimePosition' from initialPosition. (Initial: %s, Runtime: %s)", this.position, runtimePosition);

        // Read remaining data.
        this.initFlags = reader.readInt();
        this.destroyFlags = reader.readInt();
        this.group = reader.readUnsignedByteAsShort();
        this.scriptIndex = reader.readUnsignedByteAsShort();
        this.species = reader.readUnsignedShortAsInt();
        for (int i = 0; i < this.genericData.length; i++)
            this.genericData[i] = reader.readInt();
    }

    @Override
    public void save(DataWriter writer) {
        writer.writeUnsignedShort(this.linkIndex);
        writer.writeByte((byte) (this.linkTarget != null ? this.linkTarget.ordinal() : EntityLinkTargetType.NONE.ordinal()));
        writer.writeNull(1);
        writer.writeNullPointer(); // Skip a runtime pointer.
        writer.writeNullPointer(); // Skip a runtime pointer.
        writer.writeInt(this.entityId);
        writer.writeInt(this.formId);
        writer.writeUnsignedByte(this.subFormId);
        writer.writeUnsignedByte(this.rotationX);
        writer.writeUnsignedByte(this.rotationY);
        writer.writeUnsignedByte(this.rotationZ);
        this.position.saveWithPadding(writer);
        this.position.saveWithPadding(writer);
        writer.writeInt(this.initFlags);
        writer.writeInt(this.destroyFlags);
        writer.writeUnsignedByte(this.group);
        writer.writeUnsignedByte(this.scriptIndex);
        writer.writeUnsignedShort(this.species);
        for (int i = 0; i < this.genericData.length; i++)
            writer.writeInt(this.genericData[i]);
    }

    /**
     * Gets the rotation X angle in radians.
     */
    public double getRotationXInRadians() {
        return (2 * Math.PI * (this.rotationX / 256F));
    }

    /**
     * Gets the rotation Y angle in radians.
     */
    public double getRotationYInRadians() {
        return  (2 * Math.PI * (this.rotationY / 256F));
    }

    /**
     * Gets the rotation Z angle in radians.
     */
    public double getRotationZInRadians() {
        return (2 * Math.PI * (this.rotationZ / 256F));
    }

    /**
     * Gets the section index which the entity belongs to.
     */
    public int getSectionIndex() {
        return (this.linkIndex >> BIT_LENGTH_INDEX_IN_SECTION) & MAX_SECTION_INDEX; // 5 bits.
    }

    /**
     * Sets the section index which the entity belongs to.
     * @param newSectionIndex the section index to apply
     */
    public void setSectionIndex(int newSectionIndex) {
        if (newSectionIndex < 0 || newSectionIndex > MAX_SECTION_INDEX)
            throw new IllegalArgumentException("The provided section index (" + newSectionIndex + ") is not valid.");

        this.linkIndex = (this.linkIndex & MAX_INDEX_IN_SECTION) | (newSectionIndex << BIT_LENGTH_INDEX_IN_SECTION);
    }

    /**
     * Gets the index of the linked entity in the section.
     */
    public int getIndexInSection() {
        return (this.linkIndex & MAX_INDEX_IN_SECTION); // 11 bits
    }

    /**
     * Sets the index of the linked entity in the section.
     * @param newIndex the index in the section to apply
     */
    public void setIndexInSection(int newIndex) {
        if (newIndex < 0 || newIndex > MAX_INDEX_IN_SECTION)
            throw new IllegalArgumentException("The provided index (" + newIndex + ") is not valid.");

        this.linkIndex = (this.linkIndex & (MAX_SECTION_INDEX << BIT_LENGTH_INDEX_IN_SECTION)) | newIndex;
    }

    /**
     * Setup the editor UI for the entity.
     */
    public void setupEditor(SCMapEntityManager<?> manager, GUIEditorGrid editor) {
        editor.addEnumSelector("Link Target Type", this.linkTarget, EntityLinkTargetType.values(), false, newTarget -> this.linkTarget = newTarget);
        editor.addSignedIntegerField("Link Level Section", getSectionIndex(), value -> value >= 0 && value <= MAX_SECTION_INDEX, this::setSectionIndex);
        editor.addSignedIntegerField("Index in Section", getIndexInSection(), value -> value >= 0 && value <= MAX_INDEX_IN_SECTION, this::setIndexInSection);
        editor.addSignedIntegerField("Entity ID", this.entityId, newEntityId -> this.entityId = newEntityId);
        MediEvilEntityDefinition definition = null; // TODO: getEntityDefinition();
        if (definition != null) {
            editor.addLabel("Entity Type", definition.getName());
            editor.addLabel("Overlay", (definition.getOverlay() != null ? definition.getOverlay().getFilePath() : "None"));
        } else {
            editor.addSignedIntegerField("Form ID", this.formId, newFormId -> this.formId = newFormId).setDisable(true);
        }

        editor.addUnsignedByteField("Sub-Form ID", this.subFormId, newFormId -> this.subFormId = newFormId);

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
        editor.addSignedIntegerField("Init Flags", this.initFlags, newFlags -> this.initFlags = newFlags);
        editor.addSignedIntegerField("Destroy Flags", this.destroyFlags, newFlags -> this.destroyFlags = newFlags);
        if (this.group != 0 || this.map.getGameInstance().getGameType() == SCGameType.C12)
            editor.addUnsignedByteField("Group", this.group, newGroup -> this.group = newGroup);
        if (this.scriptIndex != 0 || this.map.getGameInstance().getGameType() == SCGameType.C12)
            editor.addUnsignedByteField("Script Index", this.scriptIndex, newScriptIndex -> this.scriptIndex = newScriptIndex);
        editor.addUnsignedShortField("Species", this.species, newScriptIndex -> this.species = newScriptIndex);
        for (int i = 0; i < this.genericData.length; i++) {
            final int index = i;
            editor.addSignedIntegerField("Generic Data " + i, this.genericData[i], newValue -> this.genericData[index] = newValue);
        }
    }

    /**
     * Represents different things an entity can be associated with.
     */
    public enum EntityLinkTargetType {
        NONE,
        ENTITY,
        SPLINE,
        NETWORK,
        ZONE
    }
}