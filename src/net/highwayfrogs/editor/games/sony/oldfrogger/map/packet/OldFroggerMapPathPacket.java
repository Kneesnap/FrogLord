package net.highwayfrogs.editor.games.sony.oldfrogger.map.packet;

import lombok.Getter;
import net.highwayfrogs.editor.games.sony.SCGameData;
import net.highwayfrogs.editor.games.sony.oldfrogger.OldFroggerGameInstance;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.OldFroggerMapFile;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.packet.path.OldFroggerSpline;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.ui.OldFroggerPathManager;
import net.highwayfrogs.editor.gui.GUIEditorGrid;
import net.highwayfrogs.editor.utils.data.reader.DataReader;
import net.highwayfrogs.editor.utils.data.writer.DataWriter;

import java.util.ArrayList;
import java.util.List;

/**
 * Contains path data for pre-recode Frogger maps.
 * Created by Kneesnap on 12/8/2023.
 */
@Getter
public class OldFroggerMapPathPacket extends OldFroggerMapPacket {
    public static final String IDENTIFIER = "PATH";
    private final List<OldFroggerMapPath> paths = new ArrayList<>();

    public OldFroggerMapPathPacket(OldFroggerMapFile parentFile) {
        super(parentFile, IDENTIFIER);
    }

    @Override
    protected void loadBody(DataReader reader, int endIndex) {
        int pathCount = reader.readUnsignedShortAsInt();
        int totalSplineCount = reader.readUnsignedShortAsInt();

        // Read paths.
        this.paths.clear();
        for (int i = 0; i < pathCount; i++) {
            OldFroggerMapPath newPath = new OldFroggerMapPath(getParentFile().getGameInstance(), i);
            newPath.load(reader);
            this.paths.add(newPath);
        }

        // Verify count.
        int realSplineCount = getSplineCount();
        if (realSplineCount != totalSplineCount)
            getLogger().warning("The number of path splines read was %d, but %d were expected.", realSplineCount, totalSplineCount);

        reader.skipBytes(OldFroggerSpline.SIZE_IN_BYTES * totalSplineCount);
    }

    /**
     * Get the number of splines found in all paths.
     */
    public int getSplineCount() {
        return this.paths.stream().mapToInt(path -> path.getSplines().size()).sum();
    }

    @Override
    protected void saveBodyFirstPass(DataWriter writer) {
        writer.writeUnsignedShort(this.paths.size());
        writer.writeUnsignedShort(getSplineCount());

        // Save path data.
        for (int i = 0; i < this.paths.size(); i++)
            this.paths.get(i).save(writer);

        // Save spline data.
        for (int i = 0; i < this.paths.size(); i++)
            this.paths.get(i).saveSplines(writer);
    }

    /**
     * Represents a map path in an old Frogger map.
     */
    public static class OldFroggerMapPath extends SCGameData<OldFroggerGameInstance> {
        @Getter private int pathId;
        @Getter private final List<OldFroggerSpline> splines = new ArrayList<>();
        private transient int splinePointerAddress;

        public OldFroggerMapPath(OldFroggerGameInstance instance, int pathId) {
            super(instance);
            this.pathId = pathId;
        }

        @Override
        public void load(DataReader reader) {
            this.pathId = reader.readUnsignedShortAsInt();
            int splineCount = reader.readUnsignedShortAsInt();
            int splineAddress = reader.readInt();

            // Read splines.
            this.splines.clear();
            reader.jumpTemp(splineAddress);
            for (int i = 0; i < splineCount; i++) {
                OldFroggerSpline newSpline = new OldFroggerSpline(getGameInstance());
                newSpline.load(reader);
                this.splines.add(newSpline);
            }
            reader.jumpReturn();
        }

        @Override
        public void save(DataWriter writer) {
            if (this.splinePointerAddress == -1)
                return;

            // Write starting data.
            writer.writeUnsignedShort(this.pathId);
            writer.writeUnsignedShort(this.splines.size());

            // Write spline pointer.
            this.splinePointerAddress = writer.writeNullPointer();
        }

        /**
         * Save spline data. (Run after main data is saved)
         * @param writer The writer to save information into.
         */
        public void saveSplines(DataWriter writer) {
            if (this.splinePointerAddress == -1)
                return;

            // Write spline pointer.
            writer.writeAddressTo(this.splinePointerAddress);
            this.splinePointerAddress = -1;

            // Write splines
            for (int i = 0; i < this.splines.size(); i++)
                this.splines.get(i).save(writer);
        }

        /**
         * Gets the length of all the segments combined.
         * @return totalLength
         */
        public int getTotalLength() {
            int totalLength = 0;
            for (int i = 0; i < this.splines.size(); i++)
                totalLength += this.splines.get(i).calculateLength();
            return totalLength;
        }

        /**
         * Sets up the editor for the path.
         * @param manager The manager to setup the UI for.
         * @param editor  The editor to add UI elements to.
         */
        public void setupEditor(OldFroggerPathManager manager, GUIEditorGrid editor) {
            for (int i = 0; i < this.splines.size(); i++)
                this.splines.get(i).setupEditor(manager.getController(), editor);
        }
    }
}