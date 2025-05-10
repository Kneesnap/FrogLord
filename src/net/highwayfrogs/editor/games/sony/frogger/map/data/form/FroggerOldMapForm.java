package net.highwayfrogs.editor.games.sony.frogger.map.data.form;

import lombok.Getter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.games.sony.SCGameData;
import net.highwayfrogs.editor.games.sony.frogger.FroggerConfig;
import net.highwayfrogs.editor.games.sony.frogger.FroggerGameInstance;
import net.highwayfrogs.editor.games.sony.frogger.map.FroggerMapFile;
import net.highwayfrogs.editor.games.sony.frogger.map.FroggerMapTheme;
import net.highwayfrogs.editor.games.sony.frogger.map.data.entity.FroggerMapEntity;
import net.highwayfrogs.editor.games.sony.shared.mwd.WADFile;
import net.highwayfrogs.editor.games.sony.shared.mwd.WADFile.WADEntry;
import net.highwayfrogs.editor.utils.data.reader.DataReader;
import net.highwayfrogs.editor.utils.data.writer.DataWriter;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a form definition in a very early format, from roughly around April 1997.
 * Created by Kneesnap on 2/1/2023.
 */
@Getter
public class FroggerOldMapForm extends SCGameData<FroggerGameInstance> implements IFroggerFormEntry {
    private final FroggerMapFile mapFile;
    private int entityTypeId; // Index into entities file
    private int mofId; // Index into MOF text file
    private final List<FroggerOldMapFormData> formDataEntries = new ArrayList<>();

    public FroggerOldMapForm(FroggerMapFile mapFile) {
        super(mapFile.getGameInstance());
        this.mapFile = mapFile;
    }

    @Override
    public FroggerConfig getConfig() {
        return (FroggerConfig) super.getConfig();
    }

    @Override
    public void load(DataReader reader) {
        this.entityTypeId = reader.readUnsignedShortAsInt();
        this.mofId = reader.readUnsignedShortAsInt();
        int formDataEntryCount = reader.readUnsignedShortAsInt();
        reader.skipBytesRequireEmpty(Constants.SHORT_SIZE); // Padding.

        // Skip pointer table.
        int formDataPointerList = reader.getIndex();
        reader.skipBytes(formDataEntryCount * Constants.POINTER_SIZE);

        // Read form data entries.
        this.formDataEntries.clear();
        for (int i = 0; i < formDataEntryCount; i++) {
            // Read the pointer table to verify the start of the next form data entry.
            reader.jumpTemp(formDataPointerList);
            int nextFormDataEntryStartAddress = reader.readInt();
            formDataPointerList = reader.getIndex();
            reader.jumpReturn();

            requireReaderIndex(reader, nextFormDataEntryStartAddress, "Expected FroggerOldMapFormData list entry " + i);
            FroggerOldMapFormData newFormDataEntry = new FroggerOldMapFormData();
            this.formDataEntries.add(newFormDataEntry);
            newFormDataEntry.load(reader);
        }
    }

    @Override
    public void save(DataWriter writer) {
        writer.writeUnsignedShort(this.entityTypeId);
        writer.writeUnsignedShort(this.mofId);
        writer.writeUnsignedShort(this.formDataEntries.size());
        writer.writeNull(Constants.SHORT_SIZE); // Padding.

        // Write placeholder table.
        int formDataPointerList = writer.getIndex();
        for (int i = 0; i < this.formDataEntries.size(); i++)
            writer.writeNullPointer();

        // Write form data entries.
        for (int i = 0; i < this.formDataEntries.size(); i++) {
            // Write the pointer table to the start of the next form data entry.
            int nextFormDataEntryStartAddress = writer.getIndex();
            writer.jumpTemp(formDataPointerList);
            writer.writeInt(nextFormDataEntryStartAddress);
            formDataPointerList = writer.getIndex();
            writer.jumpReturn();

            // Write form data entry.
            this.formDataEntries.get(i).save(writer);
        }
    }

    @Override
    public String getEntityTypeName() {
        return getConfig().getEntityBank().getName(this.entityTypeId);
    }

    @Override
    public String getFormTypeName() {
        return getEntityTypeName();
    }

    @Override
    public FroggerFormGrid getFormGrid() {
        return null;
    }

    @Override
    public void setFormGrid(FroggerFormGrid formGrid) {
        if (formGrid != null)
            throw new UnsupportedOperationException("FroggerOldMapForm does not support setFormGrid()!");
    }

    @Override
    public WADEntry getEntityModelWadEntry(FroggerMapEntity entity) {
        int resourceId = getGameInstance().getResourceEntryByName("MAP_RUSHED.WAD").getResourceId();
        WADFile wadFile = getGameInstance().getGameFile(resourceId);
        return wadFile.getFiles().get(this.mofId + 1);
    }

    @Override
    public FroggerMapTheme getTheme() {
        return null;
    }
}