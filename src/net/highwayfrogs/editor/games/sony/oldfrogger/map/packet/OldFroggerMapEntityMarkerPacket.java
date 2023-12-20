package net.highwayfrogs.editor.games.sony.oldfrogger.map.packet;

import lombok.Getter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.file.MWIFile.FileEntry;
import net.highwayfrogs.editor.file.WADFile.WADEntry;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.OldFroggerMapFile;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.entity.OldFroggerMapEntity;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.entity.OldFroggerMapForm;
import net.highwayfrogs.editor.utils.Utils;

import java.util.*;
import java.util.Map.Entry;

/**
 * This packed contains entity data.
 * Created by Kneesnap on 12/10/2023.
 */
@Getter
public class OldFroggerMapEntityMarkerPacket extends OldFroggerMapPacket {
    public static final String IDENTIFIER = "EMTP";
    private final List<OldFroggerMapEntity> entities = new ArrayList<>();

    public static final boolean ENABLE_FORM_GENERATOR = false;
    public static final Map<Integer, Set<FileEntry>> formIdsByMof = new HashMap<>();

    public OldFroggerMapEntityMarkerPacket(OldFroggerMapFile parentFile) {
        super(parentFile, IDENTIFIER);
    }

    @Override
    protected void loadBody(DataReader reader, int endIndex) {
        int entityCount = reader.readInt();

        int lastDataStart = -1;
        int lastDataEnd = -1;
        this.entities.clear();
        for (int i = 0; i < entityCount; i++) {
            int entityAddress = reader.readInt();

            // Check if the correct amount of data was read for the previous entity.
            if (lastDataEnd != -1 && this.entities.size() > 0)
                showIncorrectEntityDataSize(this.entities.get(this.entities.size() - 1), lastDataStart, lastDataEnd, entityAddress);

            // Read entity.
            reader.jumpTemp(entityAddress);
            OldFroggerMapEntity newEntity = new OldFroggerMapEntity(getParentFile());
            newEntity.load(reader);
            lastDataStart = entityAddress;
            lastDataEnd = reader.getIndex();
            reader.jumpReturn();
            this.entities.add(newEntity);
        }

        // If the form config generator is enabled, add entity info.
        if (ENABLE_FORM_GENERATOR && (getParentFile().getFormInstancePacket().getFormTableSize() >= 252)) {
            for (int i = 0; i < this.entities.size(); i++) {
                OldFroggerMapEntity entity = this.entities.get(i);
                OldFroggerMapForm form = entity.getForm();
                if (form == null)
                    continue;

                formIdsByMof.computeIfAbsent(entity.getFormTypeId(), key -> new HashSet<>()).add(form.getMofFileEntry().getFileEntry());
            }
        }

        // Ensure the reader is placed after the end of entity data.
        if (lastDataEnd > reader.getIndex()) // Sanity check in the case of invalid data.
            reader.setIndex(lastDataEnd);

        // Ensure the reader is placed at the correct position for the last entity's data.
        if (endIndex >= 0 && this.entities.size() > 0 && showIncorrectEntityDataSize(this.entities.get(this.entities.size() - 1), lastDataStart, lastDataEnd, endIndex))
            reader.setIndex(endIndex);
    }

    @Override
    protected void saveBodyFirstPass(DataWriter writer) {
        writer.writeInt(this.entities.size());

        // Write empty entity pointer table.
        int entityPointerStartIndex = writer.getIndex();
        for (int i = 0; i < this.entities.size(); i++)
            writer.writeNullPointer();

        // Write entity data & update table.
        for (int i = 0; i < this.entities.size(); i++) {
            writer.writeAddressTo(entityPointerStartIndex + (i * Constants.INTEGER_SIZE));
            this.entities.get(i).save(writer);
        }
    }

    private boolean showIncorrectEntityDataSize(OldFroggerMapEntity entity, int startPosition, int actualEndPosition, int expectedPosition) {
        int realSize = (expectedPosition - startPosition);
        int readSize = (actualEndPosition - startPosition);
        if (realSize == readSize)
            return false;

        // Load form.
        OldFroggerMapForm form = entity.getForm();
        WADEntry mofEntry = form != null ? form.getMofFileEntry() : null;

        // Display message.
        System.out.println("[INVALID/" + getParentFile().getFileDisplayName()
                + (mofEntry != null ? "/" + mofEntry.getDisplayName() : "")
                + "] Entity " + this.entities.indexOf(entity)
                + "/" + entity.getDifficulty()
                + (form != null ? "/" + form.getFormType() : "")
                + "/" + entity.getFormTypeId()
                + " REAL: " + realSize + ", READ: " + readSize);

        return true;
    }

    /**
     * Show form generation output, if enabled.
     */
    public static void showFormGenerationOutput() {
        if (!ENABLE_FORM_GENERATOR)
            return;

        StringBuilder builder = new StringBuilder();
        for (Entry<Integer, Set<FileEntry>> entry : formIdsByMof.entrySet()) {
            builder.append(entry.getKey()).append('=');

            boolean firstTime = true;
            for (FileEntry fileEntry : entry.getValue()) {
                if (firstTime) {
                    firstTime = false;
                } else {
                    builder.append(',');
                }

                builder.append(Utils.stripExtension(fileEntry.getDisplayName()));
            }

            System.out.println(builder.toString());
            builder.setLength(0);
        }

        formIdsByMof.clear();
    }
}