package net.highwayfrogs.editor.games.sony.oldfrogger.map.packet;

import lombok.Getter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.OldFroggerMapFile;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.entity.OldFroggerMapEntity;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.entity.OldFroggerMapForm;
import net.highwayfrogs.editor.games.sony.shared.mwd.WADFile.WADEntry;
import net.highwayfrogs.editor.games.sony.shared.mwd.mwi.MWIResourceEntry;
import net.highwayfrogs.editor.utils.FileUtils;
import net.highwayfrogs.editor.utils.data.reader.DataReader;
import net.highwayfrogs.editor.utils.data.writer.DataWriter;

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
    private final Map<Integer, OldFroggerMapEntity> entitiesByFileOffsets = new HashMap<>();
    private final Map<OldFroggerMapEntity, Integer> entityFileOffsets = new HashMap<>();

    public static final boolean ENABLE_FORM_GENERATOR = false;
    public static final Map<Integer, Set<MWIResourceEntry>> formIdsByMof = new HashMap<>();

    public OldFroggerMapEntityMarkerPacket(OldFroggerMapFile parentFile) {
        super(parentFile, IDENTIFIER);
    }

    /**
     * Gets an entity reference from the provided file offset.
     * @param fileOffset The file offset to lookup.
     * @return entity, or null if no entity exists at this offset.
     */
    public OldFroggerMapEntity getEntityByFileOffset(int fileOffset) {
        OldFroggerMapEntity entity = this.entitiesByFileOffsets.get(fileOffset);
        if (entity == null)
            getLogger().warning("Couldn't find map entity at 0x%X in %s.", fileOffset, getParentFile().getFileDisplayName());
        return entity;
    }

    /**
     * Gets the file offset which an entity was located.
     * @param entity the entity to find the file offset from.
     * @return The entity file offset, or -1 if one does not exist.
     */
    public int getEntityFileOffset(OldFroggerMapEntity entity) {
        Integer fileOffset = this.entityFileOffsets.get(entity);
        if (fileOffset == null) {
            getLogger().warning("Couldn't find map entity file offset for entity in %s.", getParentFile().getFileDisplayName());
            return -1;
        }

        return fileOffset;
    }

    @Override
    public void clearReadWriteData() {
        super.clearReadWriteData();
        this.entitiesByFileOffsets.clear();
        this.entityFileOffsets.clear();
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
            this.entitiesByFileOffsets.put(entityAddress, newEntity);
            this.entityFileOffsets.put(newEntity, entityAddress);
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

                formIdsByMof.computeIfAbsent(entity.getFormTypeId(), key -> new HashSet<>()).add(form.getModelFileEntry().getFileEntry());
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
            OldFroggerMapEntity entity = this.entities.get(i);
            this.entitiesByFileOffsets.put(writer.getIndex(), entity);
            this.entityFileOffsets.put(entity, writer.getIndex());
            entity.save(writer);
        }
    }

    private boolean showIncorrectEntityDataSize(OldFroggerMapEntity entity, int startPosition, int actualEndPosition, int expectedPosition) {
        int realSize = (expectedPosition - startPosition);
        int readSize = (actualEndPosition - startPosition);
        if (realSize == readSize)
            return false;

        // Load form.
        OldFroggerMapForm form = entity.getForm();
        WADEntry mofEntry = form != null ? form.getModelFileEntry() : null;

        // Display message.
        getLogger().warning("[INVALID/%s] Entity %d/%d%s/%d REAL: %d, READ: %d",
                mofEntry != null ? mofEntry.getDisplayName() : "",
                this.entities.indexOf(entity), entity.getDifficulty(),
                form != null ? "/" + form.getFormType() : "", entity.getFormTypeId(),
                realSize, readSize);

        return true;
    }

    /**
     * Show form generation output, if enabled.
     */
    public static void showFormGenerationOutput() {
        if (!ENABLE_FORM_GENERATOR)
            return;

        StringBuilder builder = new StringBuilder();
        for (Entry<Integer, Set<MWIResourceEntry>> entry : formIdsByMof.entrySet()) {
            builder.append(entry.getKey()).append('=');

            boolean firstTime = true;
            for (MWIResourceEntry resourceEntry : entry.getValue()) {
                if (firstTime) {
                    firstTime = false;
                } else {
                    builder.append(',');
                }

                builder.append(FileUtils.stripExtension(resourceEntry.getDisplayName()));
            }

            System.out.println(builder);
            builder.setLength(0);
        }

        formIdsByMof.clear();
    }
}