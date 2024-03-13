package net.highwayfrogs.editor.games.sony.medievil.entity;

import lombok.Getter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.file.mof.MOFHolder;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.sony.SCGameData;
import net.highwayfrogs.editor.games.sony.SCUtils;
import net.highwayfrogs.editor.games.sony.medievil.MediEvilGameInstance;
import net.highwayfrogs.editor.games.sony.medievil.config.MediEvilConfig;
import net.highwayfrogs.editor.games.sony.shared.overlay.SCOverlayTableEntry;

import java.util.ArrayList;
import java.util.List;


/**
 * Represents entity configuration applicable to all entity types in MediEvil.
 * Created by RampantSpirit on 3/11/2024.
 */
@Getter
public class MediEvilEntityDefinition extends SCGameData<MediEvilGameInstance> {
    private final SCOverlayTableEntry overlay;
    private final List<MediEvilModelEntityData> modelData = new ArrayList<>();
    private String name;
    private int subtypeCount;
    private long mofId = -1;

    public MediEvilEntityDefinition(MediEvilGameInstance instance, SCOverlayTableEntry overlay) {
        super(instance);
        this.overlay = overlay;
    }

    @Override
    public void load(DataReader reader) {
        this.loadMainEntityData(reader);
    }

    @Override
    public void save(DataWriter writer) {
        //this.saveMainEntityData(writer);
        // TODO: Implement.
    }

    /**
     * Gets the MOF file registered to this entity definition.
     */
    public MOFHolder getMOF() {
        return this.mofId != 0 ? getGameInstance().getGameFileByResourceID((int) this.mofId, MOFHolder.class, true) : null;
    }

    /**
     * Load the main entity data from the reader.
     * @param reader The reader to read data from.
     */
    protected void loadMainEntityData(DataReader reader) {
        long offset = this.overlay != null ? this.overlay.getOverlayStartPointer() : getGameInstance().getRamOffset();

        // Read the entity name.
        long entityNamePtr = reader.readUnsignedIntAsLong();
        reader.jumpTemp((int) (entityNamePtr - offset));
        this.name = reader.readNullTerminatedString();
        reader.jumpReturn();
        this.subtypeCount = reader.readUnsignedShortAsInt();

        // ECTS Pre-Alpha entities
        if (getGameInstance().getConfig().getEntityTableSize() <= 111) {
            reader.skipBytes(186);
        }
        // All others
        else {
            reader.skipBytes(170); // TODO: Read more of this instead of skipping.
        }
        this.mofId = reader.readUnsignedIntAsLong();
        // Read model data.
        this.modelData.clear();
        if (SCUtils.isValidLookingPointer(getGameInstance().getPlatform(), this.mofId)) {
            reader.jumpTemp((int) (this.mofId - offset));

            // Assume there is at least one entry if subtype count is zero
            int iterator = this.subtypeCount > 0 ? this.subtypeCount : 1;

            for (int i = 0; i < iterator; i++) {
                MediEvilModelEntityData newData = new MediEvilModelEntityData(getGameInstance());
                newData.load(reader);
                this.modelData.add(newData);
            }
            reader.jumpReturn();
        }
    }

    @Override
    public MediEvilConfig getConfig() {
        return (MediEvilConfig) super.getConfig();
    }

    @Getter
    public static class MediEvilModelEntityData extends SCGameData<MediEvilGameInstance> {
        private int mofIndex;
        private int modelSet;
        private int model;

        public MediEvilModelEntityData(MediEvilGameInstance instance) {
            super(instance);
        }

        @Override
        public void load(DataReader reader) {
            this.mofIndex = reader.readUnsignedShortAsInt();
            this.modelSet = reader.readUnsignedShortAsInt();
            this.model = reader.readUnsignedShortAsInt();
            reader.skipBytesRequireEmpty(Constants.SHORT_SIZE);
        }

        @Override
        public void save(DataWriter writer) {
            writer.writeUnsignedShort(this.mofIndex);
            writer.writeUnsignedShort(this.modelSet);
            writer.writeUnsignedShort(this.model);
            writer.writeNull(Constants.SHORT_SIZE);
        }
    }
}