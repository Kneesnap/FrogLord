package net.highwayfrogs.editor.games.sony.frogger.map.packets;

import javafx.scene.control.Alert.AlertType;
import lombok.Getter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.file.config.exe.MapBook;
import net.highwayfrogs.editor.file.config.exe.general.FormEntry;
import net.highwayfrogs.editor.file.config.exe.general.FormEntry.FormLibFlag;
import net.highwayfrogs.editor.games.sony.SCGameFile;
import net.highwayfrogs.editor.games.sony.frogger.FroggerGameInstance;
import net.highwayfrogs.editor.games.sony.frogger.map.FroggerMapFile;
import net.highwayfrogs.editor.games.sony.frogger.map.data.entity.FroggerMapEntity;
import net.highwayfrogs.editor.games.sony.frogger.map.data.form.FroggerFormGrid;
import net.highwayfrogs.editor.games.sony.frogger.map.data.form.IFroggerFormEntry;
import net.highwayfrogs.editor.games.sony.frogger.map.data.path.FroggerPath;
import net.highwayfrogs.editor.games.sony.shared.SCChunkedFile;
import net.highwayfrogs.editor.games.sony.shared.SCChunkedFile.SCFilePacket;
import net.highwayfrogs.editor.games.sony.shared.mof2.MRModel;
import net.highwayfrogs.editor.games.sony.shared.mwd.WADFile;
import net.highwayfrogs.editor.games.sony.shared.mwd.WADFile.WADEntry;
import net.highwayfrogs.editor.gui.components.propertylist.PropertyListNode;
import net.highwayfrogs.editor.utils.FXUtils;
import net.highwayfrogs.editor.utils.Utils;
import net.highwayfrogs.editor.utils.data.reader.DataReader;
import net.highwayfrogs.editor.utils.data.writer.DataWriter;

import java.util.*;

/**
 * Represents an entity table marker packet.
 * Created by Kneesnap on 5/25/2024.
 */
public class FroggerMapFilePacketEntity extends FroggerMapFilePacket {
    public static final String IDENTIFIER = "EMTP";
    private final List<FroggerMapEntity> entities = new ArrayList<>();
    private final List<FroggerMapEntity> unmodifiableEntities = Collections.unmodifiableList(this.entities);
    @Getter private int nextFreeEntityId;

    public FroggerMapFilePacketEntity(FroggerMapFile parentFile) {
        super(parentFile, IDENTIFIER, true, PacketSizeType.SIZE_INCLUSIVE);
    }

    @Override
    protected void loadBody(DataReader reader, int endIndex) {
        this.entities.clear();
        this.nextFreeEntityId = 0;
        if (getMapConfig().isIslandPlaceholder()) { // QB.MAP & ISLAND.MAP are allowed, it's just the placeholders which aren't.
            reader.setIndex(endIndex);
            return;
        }

        int entityCount = reader.readUnsignedShortAsInt();
        reader.skipBytesRequireEmpty(Constants.SHORT_SIZE); // Padding.

        // Read entities.
        FroggerMapEntity lastEntity = null;
        int entityPointerList = reader.getIndex();
        int lastEntityScriptDataStartAddress = -1;
        Set<Integer> seenEntityIds = new HashSet<>();
        reader.setIndex(entityPointerList + (entityCount * Constants.POINTER_SIZE));
        for (int i = 0; i < entityCount; i++) {
            // Read from the pointer list.
            reader.jumpTemp(entityPointerList);
            int nextEntityStartAddress = reader.readInt();
            entityPointerList = reader.getIndex();
            reader.jumpReturn();
            printInvalidEntityReadDetection(reader, lastEntity, lastEntityScriptDataStartAddress, nextEntityStartAddress);

            // Read entity.
            if (getParentFile().isQB() || getParentFile().isIslandOrIslandPlaceholder()) {
                reader.setIndex(nextEntityStartAddress);
            } else {
                reader.requireIndex(getLogger(), nextEntityStartAddress, "Expected FroggerMapEntity");
            }

            try {
                FroggerMapEntity entity = new FroggerMapEntity(getParentFile());
                this.entities.add(entity);
                entity.load(reader);
                if (entity.getUniqueId() >= this.nextFreeEntityId)
                    this.nextFreeEntityId = entity.getUniqueId() + 1;
                if (!seenEntityIds.add(entity.getUniqueId()))
                    getLogger().warning("Found multiple entities with the supposedly unique ID of %d!", entity.getUniqueId());

                lastEntity = entity;
                lastEntityScriptDataStartAddress = reader.getIndex();
                entity.loadEntityData(reader);
            } catch (Throwable th) {
                lastEntity = null;
                lastEntityScriptDataStartAddress = -1;
                Utils.handleError(getLogger(), th, false, "Failed to load an entity.");
            }
        }

        printInvalidEntityReadDetection(reader, lastEntity, lastEntityScriptDataStartAddress, getParentFile().getHeaderPacket().getGraphicalPacketAddress()); // Validate the last entity.

        // Setup forms.
        resolveMapFormGrids();
    }

    @Override
    protected void saveBodyFirstPass(DataWriter writer) {
        writer.writeUnsignedShort(this.entities.size()); // entityCount
        writer.writeNull(Constants.SHORT_SIZE); // Padding.

        // Write slots for pointers to the entity data.
        int entityPointerListAddress = writer.getIndex();
        for (int i = 0; i < this.entities.size(); i++)
            writer.writeNullPointer();

        // Write the entities.
        for (int i = 0; i < this.entities.size(); i++) {
            // Write the pointer to the entity we're about to save.
            int nextEntityStartAddress = writer.getIndex();
            writer.jumpTemp(entityPointerListAddress);
            writer.writeInt(nextEntityStartAddress);
            entityPointerListAddress = writer.getIndex();
            writer.jumpReturn();

            // Write entity data.
            this.entities.get(i).save(writer);
        }

        ensureValidLevelWadFile(true);
    }

    @Override
    public void clear() {
        this.entities.clear();
        this.nextFreeEntityId = 0;
        for (FroggerPath path : getParentFile().getPathPacket().getPaths())
            path.getPathEntities().clear();
    }

    @Override
    public void copyAndConvertData(SCFilePacket<? extends SCChunkedFile<FroggerGameInstance>, FroggerGameInstance> newChunk) {
        if (!(newChunk instanceof FroggerMapFilePacketEntity))
            throw new ClassCastException("The provided chunk was of type " + Utils.getSimpleName(newChunk) + " when " + FroggerMapFilePacketEntity.class.getSimpleName() + " was expected.");

        FroggerMapFilePacketEntity newEntityChunk = (FroggerMapFilePacketEntity) newChunk;
        for (int i = 0; i < this.entities.size(); i++) {
            FroggerMapEntity oldEntity = this.entities.get(i);
            FroggerMapEntity newEntity = oldEntity.clone(newEntityChunk.getParentFile());
            newEntityChunk.addEntity(newEntity);
        }

        getParentFile().getPathPacket().recalculateAllPathEntityLists();
    }

    private void printInvalidEntityReadDetection(DataReader reader, FroggerMapEntity lastEntity, int entityDataStartPointer, int endPointer) {
        if (lastEntity == null || entityDataStartPointer < 0)
            return;

        int realSize = (endPointer - entityDataStartPointer);
        int readerSize = (reader.getIndex() - entityDataStartPointer);
        if (realSize != readerSize) {
            lastEntity.setInvalid(true);

            IFroggerFormEntry formEntry = lastEntity.getFormEntry();
            if (!getParentFile().isIslandOrIslandPlaceholder() && !getParentFile().isQB()) { // No need to print these errors on island placeholders.
                getLogger().warning("INVALID ENTITY[%d/%x/%d], REAL: %d, READ: %d%s",
                        this.entities.indexOf(lastEntity), entityDataStartPointer,  lastEntity.getFormGridId(), realSize, readerSize,
                        formEntry != null ? ", " + formEntry.getFormTypeName() + ", " + formEntry.getEntityTypeName() : ", " + lastEntity.getTypeName());
                reader.setIndex(endPointer);
            }

            // Restore reader.
            if (realSize < 1024 && realSize >= 0) {
                reader.jumpTemp(entityDataStartPointer);
                lastEntity.setRawData(reader.readBytes(realSize));
                reader.jumpReturn();
            }
        }
    }

    private void resolveMapFormGrids() {
        if (getMapConfig().isOldFormFormat())
            return; // Old format doesn't do stuff here.

        FroggerMapFilePacketForm formPacket = getParentFile().getFormPacket();
        for (int i = 0; i < this.entities.size(); i++) {
            FroggerMapEntity entity = this.entities.get(i);
            IFroggerFormEntry formEntry = entity.getFormEntry();
            if (formEntry == null)
                continue;

            // Abort if there's no form grid!
            FroggerFormGrid formGrid = entity.getFormGrid();
            if (formGrid == null) {
                entity.getLogger().warning("Entity has no form grid linked!");
                continue;
            }

            formPacket.convertLocalFormToGlobalForm(entity.getFormGridId(), formEntry);
        }
    }

    @Override
    public int getKnownStartAddress() {
        return getParentFile().getHeaderPacket().getEntityPacketAddress();
    }

    @Override
    public void addToPropertyList(PropertyListNode propertyList) {
        propertyList.add("Entity Count", this.entities.size());
    }

    /**
     * Gets the entities tracked by this packet.
     */
    public List<FroggerMapEntity> getEntities() {
        return this.unmodifiableEntities;
    }

    /**
     * Attempts to add an entity to the entity list.
     * @param entity the entity to add
     * @return if the entity was added successfully
     */
    public boolean addEntity(FroggerMapEntity entity) {
        if (entity == null)
            throw new NullPointerException("entity");

        FroggerMapEntity existingEntityWithId = getEntityByUniqueId(entity.getUniqueId());
        if (existingEntityWithId == entity)
            return false; // Entity already registered.

        if (existingEntityWithId != null || entity.getUniqueId() < 0) { // Entity needs a new ID.
            // Automatically generate the next free entity ID.
            entity.setUniqueID(this.nextFreeEntityId++, true);
        } else if (entity.getUniqueId() >= this.nextFreeEntityId) { // Expand new ID.
            this.nextFreeEntityId = entity.getUniqueId() + 1;
        }

        this.entities.add(entity);
        getParentFile().getPathPacket().addEntityToPathTracking(entity);
        return true;
    }

    /**
     * Remove an entity from this map.
     * @param entity The entity to remove.
     */
    public boolean removeEntity(FroggerMapEntity entity) {
        getParentFile().getPathPacket().removeEntityFromPathTracking(entity);
        return this.entities.remove(entity); // Can only occur after removing from path tracking.
    }

    /**
     * Finds an entity by its unique ID.
     * @param uniqueId the unique id to lookup
     * @return entity with the unique ID.
     */
    public FroggerMapEntity getEntityByUniqueId(int uniqueId) {
        for (int i = 0; i < this.entities.size(); i++) {
            FroggerMapEntity testEntity = this.entities.get(i);
            if (testEntity.getUniqueId() == uniqueId)
                return testEntity;
        }

        return null;
    }

    /**
     * Each level has its own WAD file containing models for the specific level.
     * If these wad files do not contain the models used by entities in the level, the game will crash when the level loads.
     * This method attempts to update the per-level WAD to contain the necessary models, or warn if it can't be done.
     */
    public void ensureValidLevelWadFile(boolean showPopup) {
        // PSX Build 6 is the first build seen to have per-level wad files.
        if (getGameInstance().getVersionConfig().isAtOrBeforeBuild4())
            return;

        FroggerMapFile map = getParentFile();
        MapBook mapBook = getGameInstance().getMapBook(map.getMapLevelID());
        if (mapBook == null) {
            if (getParentFile().isIsland() || getParentFile().isQB())
                return; // These maps don't have mapBook entries.

            throw new RuntimeException("Could not resolve mapBook for '" + map.getFileDisplayName() + "'.");
        }

        if (!mapBook.hasPerLevelWadFiles())
            return; // There's no per-level wad files, so there's nothing to do.

        WADFile levelWad = mapBook.getLevelWad(map);
        if (levelWad == null)
            throw new RuntimeException("Could not resolve the per-level WAD file for '" + map.getFileDisplayName() + "'.");

        // Add missing entries.
        boolean showWarning = false;
        StringBuilder warningBuilder = new StringBuilder("Level: ").append(levelWad.getFileDisplayName()).append(Constants.NEWLINE)
                .append("WAD: ").append(levelWad.getFileDisplayName()).append(Constants.NEWLINE)
                .append("Issues: ").append(Constants.NEWLINE);
        Set<WADEntry> usedWadEntries = new HashSet<>();
        for (int i = 0; i < this.entities.size(); i++) {
            FroggerMapEntity entity = this.entities.get(i);
            IFroggerFormEntry rawFormEntry = entity.getFormEntry();
            if (!(rawFormEntry instanceof FormEntry)) {
                warningBuilder.append("- ").append(entity.getLoggerInfo()).append(" has an unsupported form entry!").append(Constants.NEWLINE);
                showWarning = true;
                continue;
            }

            FormEntry formEntry = (FormEntry) rawFormEntry;
            if (formEntry.testFlag(FormLibFlag.NO_MODEL))
                continue; // The NO_MODEL flags means the wadFile will not be resolved, and thus we should skip it.

            // Attempt to resolve the wadEntry.
            WADEntry wadEntry = formEntry.getModel(map, true);
            if (wadEntry != null) {
                usedWadEntries.add(wadEntry);
            } else {
                warningBuilder.append("- ").append(entity.getLoggerInfo()).append(" is missing from ")
                        .append(levelWad.getFileDisplayName()).append("! This will cause the game to crash.").append(Constants.NEWLINE);
                showWarning = true;
            }
        }

        if (showWarning) {
            String warningMessage = warningBuilder.toString();
            getLogger().warning("Unable to fully validate the level WAD file.%n%s", warningMessage);
            if (showPopup)
                FXUtils.makePopUp(warningMessage, AlertType.ERROR);
        }

        // Remove unused entries.
        for (int i = 0; i < levelWad.getFiles().size(); i++) {
            WADEntry testWadEntry = levelWad.getFiles().get(i);
            if (usedWadEntries.contains(testWadEntry))
                continue;

            SCGameFile<?> file = testWadEntry.getFile();
            if (!(file instanceof MRModel) || ((MRModel) file).isDummy())
                continue;

            levelWad.getLogger().info("Cleared file '%s' which went unused by '%s'.", file.getFileDisplayName(), map.getFileDisplayName());
            ((MRModel) file).setDummy();
        }
    }
}