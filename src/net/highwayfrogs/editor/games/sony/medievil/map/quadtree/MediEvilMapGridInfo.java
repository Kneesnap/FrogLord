package net.highwayfrogs.editor.games.sony.medievil.map.quadtree;

import lombok.Getter;
import net.highwayfrogs.editor.games.sony.SCGameData;
import net.highwayfrogs.editor.games.sony.medievil.MediEvilGameInstance;
import net.highwayfrogs.editor.games.sony.medievil.map.MediEvilMapFile;
import net.highwayfrogs.editor.games.sony.medievil.map.packet.MediEvilMapGridPacket.MediEvilMapGridSquare;
import net.highwayfrogs.editor.gui.components.propertylist.IPropertyListCreator;
import net.highwayfrogs.editor.gui.components.propertylist.PropertyListNode;
import net.highwayfrogs.editor.utils.Utils;
import net.highwayfrogs.editor.utils.data.reader.DataReader;
import net.highwayfrogs.editor.utils.data.writer.DataWriter;

/**
 * Represents ground grid information.
 * Created by Kneesnap on 2/3/2026.
 */
@Getter
public class MediEvilMapGridInfo extends SCGameData<MediEvilGameInstance> implements IPropertyListCreator {
    private final MediEvilMapQuadTree quadTree;
    private MediEvilMapGridSquare gridSquare;
    private MediEvilMapQuadTreeNode quadTreeNode;

    public MediEvilMapGridInfo(MediEvilMapQuadTree quadTree) {
        super(quadTree.getGameInstance());
        this.quadTree = quadTree;
    }

    @Override
    public void load(DataReader reader) {
        int gridStorageIndex = reader.readUnsignedShortAsInt();
        int nodeIndex = reader.readUnsignedShortAsInt();

        MediEvilMapFile mapFile = this.quadTree.getMapFile();
        this.gridSquare = mapFile.getGridPacket().getGridSquareByStorageIndex(gridStorageIndex);

        if (nodeIndex < 0 || nodeIndex >= this.quadTree.nodes.size())
            throw new IllegalArgumentException("Invalid nodeIndex: " + nodeIndex);

        this.quadTreeNode = this.quadTree.nodes.get(nodeIndex);
    }

    @Override
    public void save(DataWriter writer) {
        writer.writeUnsignedShort(this.gridSquare.calculateStorageIndex());
        writer.writeUnsignedShort(this.quadTreeNode.getIndex());
    }

    @Override
    public void addToPropertyList(PropertyListNode propertyList) {
        propertyList.addString(this.quadTreeNode, Utils.getSimpleName(this.quadTreeNode), "ID: " + this.quadTreeNode.getIndex());
        propertyList.addString(this.gridSquare, Utils.getSimpleName(this.gridSquare), "ID: " + this.gridSquare.calculateStorageIndex());
    }
}
