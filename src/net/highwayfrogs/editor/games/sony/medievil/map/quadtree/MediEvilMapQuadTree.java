package net.highwayfrogs.editor.games.sony.medievil.map.quadtree;

import javafx.scene.image.Image;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.games.sony.SCGameFile;
import net.highwayfrogs.editor.games.sony.medievil.MediEvilGameInstance;
import net.highwayfrogs.editor.games.sony.medievil.map.MediEvilMapFile;
import net.highwayfrogs.editor.games.sony.shared.mwd.mwi.MWIResourceEntry;
import net.highwayfrogs.editor.gui.DefaultFileUIController;
import net.highwayfrogs.editor.gui.GameUIController;
import net.highwayfrogs.editor.gui.ImageResource;
import net.highwayfrogs.editor.gui.components.propertylist.PropertyListNode;
import net.highwayfrogs.editor.utils.FileUtils;
import net.highwayfrogs.editor.utils.Utils;
import net.highwayfrogs.editor.utils.data.reader.DataReader;
import net.highwayfrogs.editor.utils.data.writer.DataWriter;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a QuadTree.
 * This entire quad tree system seems to have been originally intended for more widespread use than what it appears used for in the final game.
 * In the final game, it seems to be exclusively used to find all polygons which a point/vertical ray along the y-axis intersects with.
 * Created by Kneesnap on 2/3/2026.
 */
public class MediEvilMapQuadTree extends SCGameFile<MediEvilGameInstance> {
    final List<MediEvilMapGridInfo> gridLinks = new ArrayList<>(); // Every single grid square from the map file must have exactly one corresponding entry here.
    final List<MediEvilMapQuadTreeNode> nodes = new ArrayList<>();
    private MWIResourceEntry mapFileEntry;


    public static final String SIGNATURE = "SRTQ"; // 'QTRS'

    public MediEvilMapQuadTree(MediEvilGameInstance instance) {
        super(instance);
    }

    /**
     * Gets the map file which this quad tree corresponds to.
     */
    public MediEvilMapFile getMapFile() {
        // Try resolving the previous resource as the map file.
        if (this.mapFileEntry == null) {
            MWIResourceEntry lastResourceEntry = getGameInstance().getResourceEntryByID(getFileResourceId() - 1);
            if (lastResourceEntry != null && (lastResourceEntry.getTypeId() == MediEvilGameInstance.FILE_TYPE_MAP || lastResourceEntry.hasExtension("MAP")))
                this.mapFileEntry = lastResourceEntry;
        }

        // Try resolving the resource with the same name.
        if (this.mapFileEntry == null)
            this.mapFileEntry = getGameInstance().getResourceEntryByName(FileUtils.stripExtension(getFileDisplayName()) + ".MAP");

        if (this.mapFileEntry == null)
            throw new IllegalStateException("Failed to find a corresponding map file to '" + getFileDisplayName() + "'.");

        SCGameFile<?> file = this.mapFileEntry.getGameFile();
        if (!(file instanceof MediEvilMapFile))
            throw new IllegalStateException("File '" + file.getFileDisplayName() + "' could not be resolved to an actual map file! (Was: " + Utils.getSimpleName(file) + ")");

        return (MediEvilMapFile) file;
    }

    @Override
    public void load(DataReader reader) {
        reader.verifyString(SIGNATURE);
        int polygonCount = reader.readInt();
        int gridSquareCount = reader.readUnsignedShortAsInt();
        int nodeCount = reader.readUnsignedShortAsInt();

        MediEvilMapFile mapFile = getMapFile();
        int mapGridSquareCount = mapFile.getGridPacket().getGridSquares().size();
        if (gridSquareCount != mapGridSquareCount)
            getLogger().warning("The map file '%s' had %d active grid square(s), but the quadTree has %d grid link(s).", mapFile.getFileDisplayName(), mapGridSquareCount, gridSquareCount);

        // First, create node objects.
        // This is necessary so references to node objects can be resolved before a node has been read.
        this.nodes.clear();
        for (int i = 0; i < nodeCount; i++)
            this.nodes.add(new MediEvilMapQuadTreeNode(this));

        // Read grid squares.
        this.gridLinks.clear();
        for (int i = 0; i < gridSquareCount; i++) {
            MediEvilMapGridInfo groundGridInfo = new MediEvilMapGridInfo(this);
            this.gridLinks.add(groundGridInfo);
            groundGridInfo.load(reader);
        }

        // Read nodes.
        for (int i = 0; i < this.nodes.size(); i++)
            this.nodes.get(i).load(reader);

        // Read polygons.
        int polygonStartIndex = reader.getIndex();
        int expectedPolygonDataEndAddress = polygonStartIndex + (polygonCount * Constants.SHORT_SIZE);
        for (int i = 0; i < this.nodes.size(); i++)
            this.nodes.get(i).loadPolygons(reader, polygonStartIndex);
        requireReaderIndex(reader, expectedPolygonDataEndAddress, "Expected end of polygon IDs");
    }

    @Override
    public void save(DataWriter writer) {
        writer.writeStringBytes(SIGNATURE);
        writer.writeInt(calculatePolygonCount());
        writer.writeUnsignedShort(this.gridLinks.size());
        writer.writeUnsignedShort(this.nodes.size());

        // Write grid squares.
        for (int i = 0; i < this.gridLinks.size(); i++)
            this.gridLinks.get(i).save(writer);

        // Write nodes.
        for (int i = 0; i < this.nodes.size(); i++)
            this.nodes.get(i).save(writer);

        // Write polygons.
        int polygonDataStartIndex = writer.getIndex();
        for (int i = 0; i < this.nodes.size(); i++)
            this.nodes.get(i).savePolygons(writer, polygonDataStartIndex);
    }

    private int calculatePolygonCount() {
        int polygonCount = 0;
        for (int i = 0; i < this.nodes.size(); i++)
            polygonCount += this.nodes.get(i).getPolygons().size();

        return polygonCount;
    }

    @Override
    public Image getCollectionViewIcon() {
        return ImageResource.TREASURE_MAP_32.getFxImage();
    }

    @Override
    public GameUIController<?> makeEditorUI() {
        return new DefaultFileUIController<>(getGameInstance(), "Map QuadTree", ImageResource.TREASURE_MAP_16.getFxImage());
    }

    @Override
    public void addToPropertyList(PropertyListNode propertyList) {
        super.addToPropertyList(propertyList);
        propertyList.addString("Linked Map File", getMapFile().getFileDisplayName());
        propertyList.addString(this::addNodes, "Nodes", String.valueOf(this.nodes.size()));
        propertyList.addString(this::addGridSquares, "Grid Links", String.valueOf(this.gridLinks.size()));
    }

    private void addNodes(PropertyListNode propertyList) {
        for (int i = 0; i < this.nodes.size(); i++)
            propertyList.addProperties("Nodes[" + i + "]", this.nodes.get(i));
    }

    private void addGridSquares(PropertyListNode propertyList) {
        for (int i = 0; i < this.gridLinks.size(); i++)
            propertyList.addProperties("GridInfo[" + i + "]", this.gridLinks.get(i));
    }

    // TODO: Implement logic to automatically generate these files.
}
