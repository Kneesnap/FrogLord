package net.highwayfrogs.editor.games.sony.medievil.map;

import javafx.scene.Node;
import javafx.scene.image.Image;
import lombok.Getter;
import net.highwayfrogs.editor.file.WADFile;
import net.highwayfrogs.editor.file.map.MAPFile;
import net.highwayfrogs.editor.games.sony.medievil.MediEvilGameInstance;
import net.highwayfrogs.editor.games.sony.medievil.MediEvilLevelTableEntry;
import net.highwayfrogs.editor.games.sony.medievil.config.MediEvilConfig;
import net.highwayfrogs.editor.games.sony.medievil.map.mesh.MediEvilMapMesh;
import net.highwayfrogs.editor.games.sony.medievil.map.mesh.MediEvilMapMeshController;
import net.highwayfrogs.editor.games.sony.medievil.map.packet.MediEvilMapCollprimsPacket;
import net.highwayfrogs.editor.games.sony.medievil.map.packet.MediEvilMapEntitiesPacket;
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
 * TODO:
 *  - 0) Collision Primitives
 *  - 1) Can we figure out shading?
 *  - 2) Can we figure out the skybox? It'd be very nice to have the skybox automatically load and display. (Allow turning it off though)
 *  - 3) Can we figure out lighting? (Allow turning off)
 *  - 4) Get splines working in 3D space.
 * Cloned from a file created by Kneesnap on 03/9/2024.
 */
@Getter
public class MediEvilMapFile extends SCChunkedFile<MediEvilGameInstance> {
    private final MediEvilMapHeaderPacket headerPacket;
    private final MediEvilMapGraphicsPacket graphicsPacket;
    private final MediEvilMapEntitiesPacket entitiesPacket;
    private final MediEvilMapCollprimsPacket collprimsPacket;

    public MediEvilMapFile(MediEvilGameInstance instance) {
        super(instance, false);
        addFilePacket(this.headerPacket = new MediEvilMapHeaderPacket(this));
        addFilePacket(this.entitiesPacket = new MediEvilMapEntitiesPacket(this)); // Entities
        addFilePacket(new DummyFilePacket<>(this, "NHCP", true, PacketSizeType.SIZE_INCLUSIVE)); // PCHN - Path Chain?
        addFilePacket(new DummyFilePacket<>(this, "2LPS", true, PacketSizeType.SIZE_INCLUSIVE)); // SPL2 - 2D Splines
        addFilePacket(new DummyFilePacket<>(this, "3LPS", true, PacketSizeType.SIZE_INCLUSIVE)); // SPL3 - 3D Splines
        addFilePacket(this.graphicsPacket = new MediEvilMapGraphicsPacket(this)); // PSX Graphics
        addFilePacket(this.collprimsPacket = new MediEvilMapCollprimsPacket(this)); // Collision Primitives
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
        list.add(new Tuple2<>("Entities", this.entitiesPacket.getEntities().size()));
        list.add(new Tuple2<>("Collision Primitives", this.collprimsPacket.getCollprims().size()));

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