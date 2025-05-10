package net.highwayfrogs.editor.games.sony.frogger.map.packets;

import lombok.Getter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.games.sony.frogger.map.FroggerMapFile;
import net.highwayfrogs.editor.games.sony.frogger.map.data.entity.FroggerMapEntity;
import net.highwayfrogs.editor.games.sony.frogger.map.data.path.FroggerPath;
import net.highwayfrogs.editor.games.sony.frogger.map.data.path.FroggerPathInfo;
import net.highwayfrogs.editor.gui.components.PropertyListViewerComponent.PropertyList;
import net.highwayfrogs.editor.utils.Utils;
import net.highwayfrogs.editor.utils.data.reader.DataReader;
import net.highwayfrogs.editor.utils.data.writer.DataWriter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Implements the path file packet.
 * Created by Kneesnap on 5/25/2024.
 */
@Getter
public class FroggerMapFilePacketPath extends FroggerMapFilePacket {
    public static final String IDENTIFIER = "PATH";
    private final List<FroggerPath> paths = new ArrayList<>();

    public FroggerMapFilePacketPath(FroggerMapFile parentFile) {
        super(parentFile, IDENTIFIER);
    }

    @Override
    protected void loadBody(DataReader reader, int endIndex) {
        this.paths.clear();
        int pathCount = reader.readInt();

        int pathPointerListAddress = reader.getIndex();
        reader.skipBytes(pathCount * Constants.POINTER_SIZE);

        // Read the paths.
        for (int i = 0; i < pathCount; i++) {
            // Read the next path pointer.
            reader.jumpTemp(pathPointerListAddress);
            int nextPathDataStartAddress = reader.readInt();
            pathPointerListAddress = reader.getIndex();
            reader.jumpReturn();

            // Ensure position is expected, and read path data.
            reader.requireIndex(getLogger(), nextPathDataStartAddress, "Map Path " + i);
            FroggerPath path = new FroggerPath(getParentFile());
            path.load(reader);
            this.paths.add(path);
        }
    }

    /**
     * Reads the path entity lists from the reader's current position.
     * @param reader the reader to read the data from
     */
    public void loadEntityLists(DataReader reader) {
        for (int i = 0; i < this.paths.size(); i++)
            this.paths.get(i).loadEntityList(reader);
        reader.alignRequireByte((byte) 0xFF, Constants.INTEGER_SIZE);
    }

    @Override
    protected void saveBodyFirstPass(DataWriter writer) {
        recalculateAllPathEntityLists();
        writer.writeInt(this.paths.size()); // pathCount

        // Write slots for pointers to the path data.
        int pathPointerListAddress = writer.getIndex();
        for (int i = 0; i < this.paths.size(); i++)
            writer.writeNullPointer();

        // Write the paths.
        for (int i = 0; i < this.paths.size(); i++) {
            // Write the pointer to the path we're about to save.
            int nextPathStartAddress = writer.getIndex();
            writer.jumpTemp(pathPointerListAddress);
            writer.writeInt(nextPathStartAddress);
            pathPointerListAddress = writer.getIndex();
            writer.jumpReturn();

            // Write path data.
            this.paths.get(i).save(writer);
        }
    }

    /**
     * Writes the path entity lists to the writer's current position.
     * @param writer the writer to write the data to
     */
    public void saveEntityLists(DataWriter writer) {
        for (int i = 0; i < this.paths.size(); i++)
            this.paths.get(i).saveEntityList(writer);
        writer.align(Constants.INTEGER_SIZE, (byte) 0xFF);
    }

    @Override
    public int getKnownStartAddress() {
        return getParentFile().getHeaderPacket().getPathPacketAddress();
    }

    @Override
    public PropertyList addToPropertyList(PropertyList propertyList) {
        propertyList.add("Path Count", this.paths.size());
        return propertyList;
    }

    /**
     * Remove a path from this map.
     * @param path The path to remove.
     */
    public void removePath(FroggerPath path) {
        int pathIndex = this.paths.indexOf(path);
        Utils.verify(pathIndex >= 0, "Path was not registered!");
        this.paths.remove(pathIndex);

        // Unlink paths for entities.
        for (FroggerMapEntity entity : getParentFile().getEntityPacket().getEntities()) {
            FroggerPathInfo info = entity.getPathInfo();
            if (info == null)
                continue;

            if (info.getPathId() > pathIndex) {
                info.setPathId(info.getPathId() - 1, false);
            } else if (info.getPathId() == pathIndex) {
                info.setPathId(-1);
            }
        }
    }

    /**
     * Removes the entity from path tracking.
     * @param entity the entity to remove from path tracking
     */
    public boolean removeEntityFromPathTracking(FroggerMapEntity entity) {
        if (entity == null)
            return false;

        FroggerPathInfo pathState = entity.getPathInfo();
        if (pathState == null)
            return false; // Entity has no pathing info.

        int pathIndex = pathState.getPathId();
        if (pathIndex < 0 || pathIndex >= this.paths.size())
            return false; // Invalid path!

        FroggerPath path = this.paths.get(pathIndex);
        int searchIndex = Collections.binarySearch(path.getPathEntities(), entity, Comparator.comparingInt(FroggerMapEntity::getEntityIndex));
        if (searchIndex >= 0) {
            path.getPathEntities().remove(searchIndex);
            return true;
        } else {
            return false;
        }
    }

    /**
     * Adds the entity to path tracking.
     * @param entity the entity to add
     */
    public boolean addEntityToPathTracking(FroggerMapEntity entity) {
        return addEntityToPathTracking(entity, false);
    }

    /**
     * Recalculate the list of entities corresponding to each path.
     */
    public void recalculateAllPathEntityLists() {
        for (int i = 0; i < this.paths.size(); i++)
            this.paths.get(i).getPathEntities().clear();

        List<FroggerMapEntity> mapEntities = getParentFile().getEntityPacket().getEntities();
        for (int i = 0; i < mapEntities.size(); i++)
            addEntityToPathTracking(mapEntities.get(i), true);
    }

    private boolean addEntityToPathTracking(FroggerMapEntity entity, boolean forceAddToEnd) {
        if (entity == null)
            return false;

        FroggerPathInfo pathState = entity.getPathInfo();
        if (pathState == null)
            return false; // Entity has no pathing info.

        int pathIndex = pathState.getPathId();
        if (pathIndex < 0 || pathIndex >= this.paths.size()) {
            // entity.getLogger().warning("Cannot attach entity to an invalid path index of %d!", pathIndex);
            return false;
        }

        int entityIndex = entity.getEntityIndex();
        if (entityIndex < 0)
            return false; // Entity isn't registered, so the index we'd insert it to would be invalid!

        FroggerPath path = this.paths.get(pathIndex);
        if (forceAddToEnd || path.getPathEntities().isEmpty() || entityIndex > path.getPathEntities().get(path.getPathEntities().size() - 1).getEntityIndex()) {
            // Skip the expensive indexOf-based binary search if it's just going at the end.
            path.getPathEntities().add(entity);
            return true;
        } else {
            int searchIndex = Collections.binarySearch(path.getPathEntities(), entity, Comparator.comparingInt(FroggerMapEntity::getEntityIndex));
            if (searchIndex >= 0)
                return false; // Already found, so don't add it!

            path.getPathEntities().add(-(searchIndex + 1), entity);
            return true;
        }
    }
}