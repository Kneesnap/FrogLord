package net.highwayfrogs.editor.games.sony.frogger.map.packets;

import lombok.Getter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.sony.frogger.map.FroggerMapFile;
import net.highwayfrogs.editor.games.sony.frogger.map.data.entity.FroggerMapEntity;
import net.highwayfrogs.editor.games.sony.frogger.map.data.path.FroggerPath;
import net.highwayfrogs.editor.games.sony.frogger.map.data.path.FroggerPathInfo;
import net.highwayfrogs.editor.gui.components.PropertyListViewerComponent.PropertyList;
import net.highwayfrogs.editor.utils.Utils;

import java.util.ArrayList;
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
}