package net.highwayfrogs.editor.games.sony.frogger.map.data.path;

import lombok.Getter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.sony.SCGameData;
import net.highwayfrogs.editor.games.sony.frogger.FroggerGameInstance;
import net.highwayfrogs.editor.games.sony.frogger.map.FroggerMapFile;
import net.highwayfrogs.editor.games.sony.frogger.map.data.entity.FroggerMapEntity;
import net.highwayfrogs.editor.games.sony.frogger.map.data.path.segments.FroggerPathSegment;
import net.highwayfrogs.editor.games.sony.frogger.map.packets.FroggerMapFilePacketPath;
import net.highwayfrogs.editor.games.sony.frogger.map.ui.editor.central.FroggerUIMapPathManager.FroggerPathPreview;
import net.highwayfrogs.editor.gui.GUIEditorGrid;
import net.highwayfrogs.editor.utils.Utils;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Represents the PATH struct.
 * Created by Kneesnap on 9/16/2018.
 */
public class FroggerPath extends SCGameData<FroggerGameInstance> {
    @Getter private final FroggerMapFile mapFile;
    @Getter private final List<FroggerPathSegment> segments = new ArrayList<>();
    @Getter private final List<FroggerMapEntity> pathEntities = new ArrayList<>(); // Sorted by entity index. TODO: ADD / REMOVE AUTOMATICALLY..?
    private transient int tempEntityIndexPointer = -1;

    private static final short ENTITY_LIST_TERMINATOR = -1;

    public FroggerPath(FroggerMapFile mapFile) {
        super(mapFile != null ? mapFile.getGameInstance() : null);
        this.mapFile = mapFile;
    }

    @Override
    public void load(DataReader reader) {
        // Points to a -1 terminated entity index list of entities using this path. Seems to be invalid data in many cases.
        // Since it appears to only ever be used for the retro beaver, we auto-generate it.
        this.tempEntityIndexPointer = isOldPathFormatEnabled() ? 0 : reader.readInt();

        // Prepare to read segments.
        int segmentCount = reader.readInt();
        int pointerTableList = reader.getIndex();
        reader.skipBytes(Constants.POINTER_SIZE * segmentCount);

        // Read segments.
        this.segments.clear();
        for (int j = 0; j < segmentCount; j++) {
            // Get the pointer to the next path segment.
            reader.jumpTemp(pointerTableList);
            int nextPathSegmentStartAddress = reader.readInt();
            pointerTableList = reader.getIndex();
            reader.jumpReturn();

            // Read next path segment.
            reader.requireIndex(getLogger(), nextPathSegmentStartAddress, "Expected FroggerPathSegment");
            FroggerPathSegmentType type = FroggerPathSegmentType.values()[reader.readInt()];
            FroggerPathSegment segment = type.makeNew(this);
            this.segments.add(segment);
            segment.load(reader);
        }
    }

    /**
     * Reads & validates the entity index list from the current reader position.
     * @param reader the reader to read data from
     */
    public void loadEntityList(DataReader reader) {
        if (this.tempEntityIndexPointer == 0) {
            this.tempEntityIndexPointer = -1;
            return;
        }
        if (this.tempEntityIndexPointer <= 0)
            throw new RuntimeException("Cannot read path entity list, the pointer " + Utils.toHexString(this.tempEntityIndexPointer) + " is invalid.");

        // The entity packet must have been read.
        if (this.mapFile.getEntityPacket() == null || !this.mapFile.getEntityPacket().isActive())
            throw new RuntimeException("The entity table packet has not been read yet, so it is currently impossible to read path entity lists.");

        // Validate index.
        if (this.mapFile.getGroupPacket().shouldSkipStaticEntityLists()) {
            reader.setIndex(this.tempEntityIndexPointer);
        } else {
            reader.requireIndex(getLogger(), this.tempEntityIndexPointer, "Expected path entity id list");
        }

        // Read the path ids from the data.
        List<Short> pathEntityIds = new ArrayList<>();
        short tempPathId;
        while ((tempPathId = reader.readShort()) != ENTITY_LIST_TERMINATOR)
            pathEntityIds.add(tempPathId);

        // Calculate the entity list ourselves, and verify that the entity list we've calculated matches the one we've read. (Sanity check our behavior)
        recalculateListOfEntitiesUsingPath();
        if (this.pathEntities.size() == pathEntityIds.size()) {
            for (int i = 0; i < this.pathEntities.size(); i++) {
                if (this.pathEntities.get(i).getEntityIndex() != pathEntityIds.get(i)) {
                    warnEntityListMismatch(pathEntityIds);
                    break;
                }
            }
        } else {
            warnEntityListMismatch(pathEntityIds);
        }


        this.tempEntityIndexPointer = -1;
    }

    private void warnEntityListMismatch(List<Short> entityPathIds) {
        if (this.mapFile.isExtremelyEarlyMapFormat())
            return; // Ignore warnings from island placeholders about mismatched entity paths due to this being expected from the old format.

        getLogger().warning("The path's entity list did not match what FrogLord expected!");
        getLogger().warning("This most likely suggests a bug with FrogLord!");

        StringBuilder arrayBuilder = new StringBuilder("[");
        for (int i = 0; i < entityPathIds.size(); i++) {
            if (i > 0)
                arrayBuilder.append(", ");
            arrayBuilder.append(entityPathIds.get(i));
        }

        getLogger().warning(" - List in Map File (" + entityPathIds.size() + "): " + arrayBuilder.append(']'));

        // Generate FrogLord one:
        arrayBuilder.setLength(1);
        for (int i = 0; i < this.pathEntities.size(); i++) {
            if (i > 0)
                arrayBuilder.append(", ");
            arrayBuilder.append(this.pathEntities.get(i).getEntityIndex());
        }

        getLogger().warning(" - FrogLord's List (" + this.pathEntities.size() + "):  " + arrayBuilder.append(']'));
    }

    @Override
    public void save(DataWriter writer) {
        this.tempEntityIndexPointer = isOldPathFormatEnabled() ? 0 : writer.writeNullPointer();
        writer.writeInt(this.segments.size());

        // Write pointer table placeholder values.
        int pointerTableList = writer.getIndex();
        writer.writeNull(Constants.POINTER_SIZE * this.segments.size());

        // Write path segments.
        for (int i = 0; i < this.segments.size(); i++) {
            // Write to the pointer table.
            int nextPathStartAddress = writer.getIndex();
            writer.jumpTemp(pointerTableList);
            writer.writeInt(nextPathStartAddress);
            pointerTableList = writer.getIndex();
            writer.jumpReturn();

            // Write path segment.
            this.segments.get(i).save(writer);
        }
    }

    /**
     * Write the entity index list to the current writer position.
     * @param writer the writer to write data to
     */
    public void saveEntityList(DataWriter writer) {
        if (this.tempEntityIndexPointer == 0) {
            this.tempEntityIndexPointer = -1;
            return;
        }
        if (this.tempEntityIndexPointer <= 0)
            throw new RuntimeException("Cannot write path entity list, the pointer " + Utils.toHexString(this.tempEntityIndexPointer) + " is invalid.");

        // Test if there are any entities which would make us save the entity list.
        List<FroggerMapEntity> pathEntities = recalculateListOfEntitiesUsingPath();

        // Write entity data.
        writer.writeAddressTo(this.tempEntityIndexPointer);
        for (int i = 0; i < pathEntities.size(); i++)
            writer.writeUnsignedShort(pathEntities.get(i).getEntityIndex());
        writer.writeShort(ENTITY_LIST_TERMINATOR);

        this.tempEntityIndexPointer = -1;
    }

    /**
     * Recalculate the list of entities using this path.
     */
    public List<FroggerMapEntity> recalculateListOfEntitiesUsingPath() {
        this.pathEntities.clear();

        int thisPathIndex = getPathIndex();
        List<FroggerMapEntity> mapEntities = this.mapFile.getEntityPacket().getEntities();
        for (int i = 0; i < mapEntities.size(); i++) {
            FroggerMapEntity entity = mapEntities.get(i);
            FroggerPathInfo pathState = entity.getPathInfo();
            if (pathState != null && pathState.getPathId() == thisPathIndex)
                this.pathEntities.add(entity);
        }

        return this.pathEntities;
    }

    /**
     * Gets the index of the path within the map path list.
     */
    public int getPathIndex() {
        FroggerMapFilePacketPath pathPacket = this.mapFile.getPathPacket();
        return pathPacket.getLoadingIndex(pathPacket.getPaths(), this);
    }

    /**
     * Gets information about the logger.
     */
    public String getLoggerInfo() {
        return this.mapFile != null ? this.mapFile.getFileDisplayName() + "|FroggerPath{" + getPathIndex() + "}" : Utils.getSimpleName(this);
    }

    @Override
    public Logger getLogger() {
        return Logger.getLogger(getLoggerInfo());
    }

    /**
     * Setup the editor.
     * @param pathPreview The path preview.
     * @param editor The editor to setup under.
     */
    public void setupEditor(FroggerPathPreview pathPreview, GUIEditorGrid editor) {
        for (int i = 0; i < this.segments.size(); i++) {
            final int tempIndex = i;

            editor.addBoldLabelButton("Segment #" + (i + 1) + ":", "Remove", 25, () -> {
                this.segments.remove(tempIndex);

                // Fix entities attached to segments after this.
                for (FroggerMapEntity entity : this.mapFile.getEntityPacket().getEntities()) {
                    if (entity.getPathInfo() == null)
                        continue;

                    FroggerPathInfo info = entity.getPathInfo();
                    if (info.getSegmentId() > tempIndex) {
                        info.setSegmentId(info.getSegmentId() - 1);
                    } else if (info.getSegmentId() == tempIndex) {
                        info.setSegmentId(0);
                        info.setSegmentDistance(0);
                    } else {
                        continue;
                    }

                    pathPreview.getController().getEntityManager().updateEntityPositionRotation(entity);
                }

                pathPreview.getPathManager().updateEditor();
            });

            this.segments.get(i).setupEditor(pathPreview, editor);
            editor.addSeparator(25.0);
        }

        editor.addButtonWithEnumSelection("Add Segment", pathType -> {
            FroggerPathSegment newSegment = pathType.makeNew(this);
            newSegment.setupNewSegment();
            this.segments.add(newSegment);
            pathPreview.getPathManager().updateEditor();
        }, FroggerPathSegmentType.values(), FroggerPathSegmentType.LINE);
    }

    /**
     * Gets the position of an entity on this path.
     * @param pathInfo Information about this path.
     * @return finishedPosition
     */
    public FroggerPathResult evaluatePosition(FroggerPathInfo pathInfo) {
        return this.segments.get(pathInfo.getSegmentId()).calculatePosition(pathInfo);
    }

    /**
     * Gets the length of all the segments combined.
     * @return totalLength
     */
    public int calculateTotalLength() {
        int totalLength = 0;
        for (int i = 0; i < this.segments.size(); i++)
            totalLength += this.segments.get(i).getLength();
        return totalLength;
    }

    /**
     * Gets the length of all the segments combined as a floating point number.
     * @return totalLength
     */
    public float calculateTotalLengthFloat() {
        return Utils.fixedPointIntToFloat4Bit(calculateTotalLength());
    }

    /**
     * Returns true iff the old path format is used.
     * The only path format likely was replaced in early May 1997.
     */
    public boolean isOldPathFormatEnabled() {
        return this.mapFile != null && this.mapFile.getMapConfig().isOldPathFormat();
    }
}