package net.highwayfrogs.editor.games.sony.oldfrogger.map.entity;

import lombok.Getter;
import net.highwayfrogs.editor.file.WADFile.WADEntry;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.sony.SCGameData;
import net.highwayfrogs.editor.games.sony.oldfrogger.OldFroggerGameInstance;
import net.highwayfrogs.editor.games.sony.oldfrogger.config.OldFroggerConfig;
import net.highwayfrogs.editor.games.sony.oldfrogger.config.OldFroggerFormConfig;
import net.highwayfrogs.editor.games.sony.oldfrogger.config.OldFroggerFormConfig.OldFroggerFormConfigEntry;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.OldFroggerMapFile;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.entity.data.OldFroggerEntityData;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.entity.data.OldFroggerEntityData.OldFroggerEntityDataFactory;
import net.highwayfrogs.editor.utils.Utils;

/**
 * Represents an entity on an old Frogger map.
 * Created by Kneesnap on 12/10/2023.
 */
@Getter
public class OldFroggerMapEntity extends SCGameData<OldFroggerGameInstance> {
    private final OldFroggerMapFile map;
    private int formTypeId; // TODO: This can have flag 0x8000, or at least the game checks for it. Do we ever see it?
    private int difficulty; // TODO: Flag 0x2000 (1 << 13) is seen in DES_STAT_BALLCACTUS in DESERT.MAP, 0x8000 (1 << 15) is seen in CAVES.MAP on CAV_SLIME.XMR
    private short entityId;
    private OldFroggerEntityData<?> entityData;

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
                    System.out.println("Failed to load " + entityDataFactory.getName() + " for " + getDebugName() + " at " + Utils.toHexString(entityDataStartIndex) + " in " + getMap().getFileDisplayName());
                    System.err.println("Failed to load " + entityDataFactory.getName() + " for " + getDebugName() + " at " + Utils.toHexString(entityDataStartIndex) + " in " + getMap().getFileDisplayName());
                    th.printStackTrace();
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

    /**
     * Test if a difficulty level is enabled.
     * @param level The level to test. (0 indexed)
     * @return If the difficulty level is enabled.
     */
    public boolean isDifficultyLevelEnabled(int level) {
        if (level < 0 || level >= OldFroggerGameInstance.DIFFICULTY_LEVELS)
            return false;

        int mask = (1 << level);
        return (this.difficulty & mask) == mask;
    }

    /**
     * Set if a difficulty level is enabled.
     * @param level    The difficulty level to set. (0 indexed)
     * @param newState The new state of this difficulty level.
     */
    public void setDifficultyLevelEnabled(int level, boolean newState) {
        if (level < 0 || level >= OldFroggerGameInstance.DIFFICULTY_LEVELS)
            throw new IllegalArgumentException("Invalid difficulty level: " + level);

        int mask = (1 << level);
        if (newState) {
            this.difficulty |= mask;
        } else {
            this.difficulty &= ~mask;
        }
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
                return Utils.stripExtension(mofEntry.getDisplayName());
        }

        return "Entity #" + this.entityId;
    }
}