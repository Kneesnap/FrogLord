package net.highwayfrogs.editor.games.sony.frogger.map.packets;

import lombok.Getter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.games.sony.frogger.map.FroggerMapFile;
import net.highwayfrogs.editor.games.sony.frogger.map.data.entity.FroggerMapEntity;
import net.highwayfrogs.editor.games.sony.frogger.map.data.form.IFroggerFormEntry;
import net.highwayfrogs.editor.games.sony.frogger.map.data.path.FroggerPath;
import net.highwayfrogs.editor.gui.components.PropertyListViewerComponent.PropertyList;
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
                getLogger().warning("INVALID ENTITY[" + this.entities.indexOf(lastEntity) + "/" + Integer.toHexString(entityDataStartPointer) + "/" + lastEntity.getFormGridId() + "], REAL: " + realSize + ", READ: " + readerSize + (formEntry != null ? ", " + formEntry.getFormTypeName() + ", " + formEntry.getEntityTypeName() : ", " + lastEntity.getTypeName()));
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

    @Override
    public int getKnownStartAddress() {
        return getParentFile().getHeaderPacket().getEntityPacketAddress();
    }

    @Override
    public PropertyList addToPropertyList(PropertyList propertyList) {
        propertyList.add("Entity Count", this.entities.size());
        return propertyList;
    }

    /**
     * Gets the entities tracked by this packet.
     */
    public List<FroggerMapEntity> getEntities() {
        return this.unmodifiableEntities;
    }

    /**
     * Clears the list of tracked entities.
     */
    public void clear() {
        this.entities.clear();
        this.nextFreeEntityId = 0;
        for (FroggerPath path : getParentFile().getPathPacket().getPaths())
            path.getPathEntities().clear();
    }

    /**
     * Attempts to add an entity to the entity list.
     * @param entity the entity to add
     * @return if the entity was added successfully
     */
    public boolean addEntity(FroggerMapEntity entity) {
        if (entity == null)
            throw new NullPointerException("entity");

        if (entity.getUniqueId() >= 0) {
            if (getEntityByUniqueId(entity.getUniqueId()) != null)
                return false; // Entity already exists with the ID.

            if (entity.getUniqueId() >= this.nextFreeEntityId)
                this.nextFreeEntityId = entity.getUniqueId() + 1;
        } else {
            // Automatically generate the next free entity ID.
            entity.setUniqueID(this.nextFreeEntityId++, true);
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
}