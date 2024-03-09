package net.highwayfrogs.editor.games.sony.medievil.map;

import javafx.scene.Node;
import javafx.scene.image.Image;
import lombok.Getter;
import net.highwayfrogs.editor.file.WADFile;
import net.highwayfrogs.editor.file.map.MAPFile;
import net.highwayfrogs.editor.games.sony.medievil.MediEvilGameInstance;
import net.highwayfrogs.editor.games.sony.medievil.MediEvilLevelTableEntry;
import net.highwayfrogs.editor.games.sony.medievil.map.config.MediEvilConfig;
import net.highwayfrogs.editor.games.sony.medievil.map.mesh.MediEvilMapMesh;
import net.highwayfrogs.editor.games.sony.medievil.map.mesh.MediEvilMapMeshController;
import net.highwayfrogs.editor.games.sony.medievil.map.packet.MediEvilMapGraphicsPacket;
import net.highwayfrogs.editor.games.sony.medievil.map.packet.MediEvilMapHeaderPacket;
import net.highwayfrogs.editor.games.sony.shared.SCChunkedFile;
import net.highwayfrogs.editor.games.sony.shared.SCChunkedFile.SCFilePacket.PacketSizeType;
import net.highwayfrogs.editor.gui.GUIMain;
import net.highwayfrogs.editor.gui.editor.MeshViewController;
import net.highwayfrogs.editor.system.Tuple2;

import java.util.List;

/**
 * Represents a map file in MediEvil.
 * Cloned from a file created by Kneesnap on 03/9/2024.
 */
@Getter
public class MediEvilMapFile extends SCChunkedFile<MediEvilGameInstance> {
    private final MediEvilMapHeaderPacket headerPacket;
    private final MediEvilMapGraphicsPacket graphicsPacket;

    public MediEvilMapFile(MediEvilGameInstance instance) {
        super(instance, false);
        addFilePacket(this.headerPacket = new MediEvilMapHeaderPacket(this));
        addFilePacket(new DummyFilePacket<>(this, "PTME", true, PacketSizeType.SIZE_INCLUSIVE)); // EMTP - Entity Markets Table Packet?
        addFilePacket(new DummyFilePacket<>(this, "NHCP", true, PacketSizeType.SIZE_INCLUSIVE)); // PCHN - Path Chain?
        addFilePacket(new DummyFilePacket<>(this, "2LPS", true, PacketSizeType.SIZE_INCLUSIVE)); // SPL2 - 2D Splines
        addFilePacket(new DummyFilePacket<>(this, "3LPS", true, PacketSizeType.SIZE_INCLUSIVE)); // SPL3 - 3D Splines
        addFilePacket(this.graphicsPacket = new MediEvilMapGraphicsPacket(this)); // PSX Graphics
        addFilePacket(new DummyFilePacket<>(this, "PLOC", true, PacketSizeType.SIZE_INCLUSIVE)); // Collision Primitives (REFER TO BEAST WARS, THIS IS ALREADY REVERSE ENGINEERED / HAS CODE IN FROGLORD)
        addFilePacket(new DummyFilePacket<>(this, "DIRG", true, PacketSizeType.SIZE_INCLUSIVE)); // GRID - Collision Info?
    }

    @Override
    protected PacketSizeType getPacketSizeForUnknownChunk(String identifier) {
        return PacketSizeType.NO_SIZE;
    }

    @Override
    public Image getIcon() {
        return MAPFile.ICON;
    }

    @Override
    public Node makeEditor() {
        return null;
    }

//    @Override
//    public Node makeEditor() {
//        return loadEditor(new MediEvilMapController(getGameInstance()), "medievil-map", this);
//    }

    @Override
    public void handleWadEdit(WADFile parent) {
        MeshViewController.setupMeshViewer(GUIMain.MAIN_STAGE, new MediEvilMapMeshController(), new MediEvilMapMesh(this));
    }

    @Override
    public List<Tuple2<String, Object>> createPropertyList() {
        List<Tuple2<String, Object>> list = super.createPropertyList();
        list.add(new Tuple2<>("Level String", this.headerPacket.getLevelString()));
        list.add(new Tuple2<>("Chunks", String.join(", ", this.headerPacket.getHeaderIdentifiers())));
        list.add(new Tuple2<>("Vertices", this.graphicsPacket.getVertices().size()));
        list.add(new Tuple2<>("Polygons", this.graphicsPacket.getPolygons().size()));

        /*list.add(new Tuple2<>("File Version", getMapConfig().getVersion()));
        list.add(new Tuple2<>("Default Reaction", this.levelSpecificDataPacket.getDefaultReactionType()));
        list.add(new Tuple2<>("Paths", this.pathPacket.getPaths().size()));
        list.add(new Tuple2<>("Zones", this.zonePacket.getZones().size()));
        list.add(new Tuple2<>("Forms", this.formInstancePacket.getForms().size() + " (Table Size: " + this.formInstancePacket.getFormTableSize() + ")"));
        list.add(new Tuple2<>("Entities", this.entityMarkerPacket.getEntities().size()));
        list.add(new Tuple2<>("Grid Type", this.gridPacket.getType()));
        list.add(new Tuple2<>("Grid Size", this.gridPacket.getXSize() + " x " + this.gridPacket.getZSize()));
        list.add(new Tuple2<>("Grid Count", this.gridPacket.getXCount() + " x " + this.gridPacket.getZCount() + " (" + this.gridPacket.getGrids().size() + ")"));
        list.add(new Tuple2<>("Grid Base", this.gridPacket.getBasePoint().toFloatString()));
        list.add(new Tuple2<>("Lights", this.lightPacket.getLights().size()));
        list.add(new Tuple2<>("UV Animations", this.animPacket.getUvAnimations().size()));

        if (this.cameraHeightFieldPacket != null) {
            list.add(new Tuple2<>("Camera Height Grid Dimensions", this.cameraHeightFieldPacket.getXSquareCount() + " x " + this.cameraHeightFieldPacket.getZSquareCount()));
            list.add(new Tuple2<>("Camera Height Grid Square Size", this.cameraHeightFieldPacket.getSquareXSizeAsFloat() + " x " + this.cameraHeightFieldPacket.getSquareZSizeAsFloat()));
            list.add(new Tuple2<>("Camera Height Grid Start Pos", this.cameraHeightFieldPacket.getStartXAsFloat() + ", " + this.cameraHeightFieldPacket.getStartZAsFloat()));
        }*/

        return list;
    }

    /**
     * Get the level table entry for this level, if one exists.
     * @return levelTableEntry
     */
    public MediEvilLevelTableEntry getLevelTableEntry() {
        return getGameInstance().getLevelTableEntry(getIndexEntry().getResourceId());
    }

    @Override
    public MediEvilConfig getConfig() {
        return (MediEvilConfig) super.getConfig();
    }
}