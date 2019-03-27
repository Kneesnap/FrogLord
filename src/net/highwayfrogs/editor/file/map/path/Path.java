package net.highwayfrogs.editor.file.map.path;

import lombok.Getter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.file.GameObject;
import net.highwayfrogs.editor.file.map.MAPFile;
import net.highwayfrogs.editor.file.map.entity.Entity;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.standard.psx.PSXMatrix;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.gui.GUIEditorGrid;
import net.highwayfrogs.editor.gui.editor.MapUIController;
import net.highwayfrogs.editor.utils.Utils;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * Represents the PATH struct.
 * Created by Kneesnap on 9/16/2018.
 */
@Getter
public class Path extends GameObject {
    private List<PathSegment> segments = new ArrayList<>();
    private transient int tempEntityIndexPointer;

    @Override
    public void load(DataReader reader) {
        reader.skipPointer(); // Points to a -1 terminated entity index list of entities using this path. Seems to be invalid data in many cases. Since it appears to only ever be used for the retro beaver, we auto-generate it in that scenario.

        // Read segments.
        int segmentCount = reader.readInt();
        for (int j = 0; j < segmentCount; j++) {
            reader.jumpTemp(reader.readInt());
            PathType type = PathType.values()[reader.readInt()];
            PathSegment segment = type.getMaker().get();
            segment.load(reader);
            this.segments.add(segment);
            reader.jumpReturn();
        }
    }

    @Override
    public void save(DataWriter writer) {
        this.tempEntityIndexPointer = writer.writeNullPointer();
        writer.writeInt(segments.size());

        int segmentPointer = writer.getIndex() + (Constants.POINTER_SIZE * segments.size());
        for (PathSegment segment : segments) {
            writer.writeInt(segmentPointer);

            writer.jumpTemp(segmentPointer);
            segment.save(writer);
            segmentPointer = writer.getIndex();
            writer.jumpReturn();
        }

        writer.setIndex(segmentPointer);
    }

    /**
     * Setup the editor.
     * @param controller The ui controller.
     * @param editor     The editor to setup under.
     */
    public void setupEditor(MapUIController controller, GUIEditorGrid editor) {

        for (int i = 0; i < getSegments().size(); i++) {
            final int tempIndex = i;

            editor.addBoldLabelButton("Segment #" + (i + 1) + ":", "Remove", 25, () -> {
                getSegments().remove(tempIndex);
                controller.getController().rebuildPathDisplay();
                controller.setupPathEditor();
            });

            getSegments().get(i).setupEditor(this, controller, editor);
            editor.addSeparator(25.0);
        }

        editor.addBoldLabel("Add Segment by Type:", 30);

        editor.addLabelButton(PathType.LINE.name(), "Add", 25, () -> {
            getSegments().add(PathType.LINE.getMaker().get());
            controller.setupPathEditor();
        });

        editor.addLabelButton(PathType.ARC.name(), "Add", 25, () -> {
            getSegments().add(PathType.ARC.getMaker().get());
            controller.setupPathEditor();
        });

        editor.addLabelButton(PathType.SPLINE.name(), "Add", 25, () -> {
            getSegments().add(PathType.SPLINE.getMaker().get());
            controller.setupPathEditor();
        });
    }

    /**
     * Write the entity index list.
     * @param writer he writer to write data to.
     */
    public void writeEntityList(MAPFile mapFile, DataWriter writer) {
        Utils.verify(this.tempEntityIndexPointer > 0, "Path has not been saved yet.");

        List<Entity> pathEntities = getEntities(mapFile);
        if (!shouldSave(pathEntities)) {
            this.tempEntityIndexPointer = 0;
            return;
        }

        writer.writeAddressTo(this.tempEntityIndexPointer);
        this.tempEntityIndexPointer = 0;

        for (Entity entity : pathEntities)
            writer.writeShort((short) mapFile.getEntities().indexOf(entity));
        writer.writeShort(MAPFile.MAP_ANIMATION_TEXTURE_LIST_TERMINATOR);
    }

    /**
     * Gets the position of an entity on this path.
     * @param pathInfo Information about this path.
     * @return finishedPosition
     */
    public PathResult evaluatePosition(PathInfo pathInfo) {
        return getSegments().get(pathInfo.getSegmentId()).calculatePosition(pathInfo);
    }

    private boolean shouldSave(List<Entity> pathEntities) {
        for (Entity testEntity : pathEntities)
            if ("ORG_BEAVER".equals(testEntity.getFormEntry().getFormName()))
                return true; // This is the only case where this is ever used.
        return false;
    }

    private List<Entity> getEntities(MAPFile mapFile) {
        List<Entity> pathEntities = new LinkedList<>();
        int myPathId = mapFile.getPaths().indexOf(this);

        for (Entity entity : mapFile.getEntities()) {
            PathInfo info = entity.getPathInfo();
            if (info != null && info.getPathId() == myPathId)
                pathEntities.add(entity);
        }

        return pathEntities;
    }
}
