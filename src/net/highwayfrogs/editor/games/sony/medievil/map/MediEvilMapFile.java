package net.highwayfrogs.editor.games.sony.medievil.map;

import javafx.scene.Node;
import javafx.scene.image.Image;
import lombok.Getter;
import net.highwayfrogs.editor.file.WADFile;
import net.highwayfrogs.editor.file.map.MAPFile;
import net.highwayfrogs.editor.games.sony.medievil.MedievilGameInstance;
import net.highwayfrogs.editor.games.sony.medievil.map.mesh.MediEvilMapMesh;
import net.highwayfrogs.editor.games.sony.oldfrogger.OldFroggerGameInstance;
import net.highwayfrogs.editor.games.sony.oldfrogger.config.OldFroggerConfig;
import net.highwayfrogs.editor.games.sony.oldfrogger.config.OldFroggerFormConfig;
import net.highwayfrogs.editor.games.sony.oldfrogger.config.OldFroggerLevelTableEntry;
import net.highwayfrogs.editor.games.sony.oldfrogger.config.OldFroggerMapConfig;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.OldFroggerMapVersion;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.mesh.OldFroggerMapMesh;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.mesh.OldFroggerMapMeshController;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.packet.*;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.ui.OldFroggerMapController;
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
public class MediEvilMapFile extends SCChunkedFile<MedievilGameInstance> {


    // Random stuff
    private transient OldFroggerMapConfig cachedMapConfig;

    public MediEvilMapFile(MedievilGameInstance instance) {
        super(instance, false);

    }

    @Override
    protected PacketSizeType getPacketSizeForUnknownChunk(String identifier) {
        return PacketSizeType.NO_SIZE;
    }

    @Override
    public Image getIcon() {
        return MAPFile.ICON;
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
        list.add(new Tuple2<>("File Version", getMapConfig().getVersion()));
        list.add(new Tuple2<>("Comment", this.headerPacket.getComment()));
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
        list.add(new Tuple2<>("Vertices", this.vertexPacket.getVertices().size()));
        list.add(new Tuple2<>("UV Animations", this.animPacket.getUvAnimations().size()));

        if (this.cameraHeightFieldPacket != null) {
            list.add(new Tuple2<>("Camera Height Grid Dimensions", this.cameraHeightFieldPacket.getXSquareCount() + " x " + this.cameraHeightFieldPacket.getZSquareCount()));
            list.add(new Tuple2<>("Camera Height Grid Square Size", this.cameraHeightFieldPacket.getSquareXSizeAsFloat() + " x " + this.cameraHeightFieldPacket.getSquareZSizeAsFloat()));
            list.add(new Tuple2<>("Camera Height Grid Start Pos", this.cameraHeightFieldPacket.getStartXAsFloat() + ", " + this.cameraHeightFieldPacket.getStartZAsFloat()));
        }

        return list;
    }

    /**
     * Get the level table entry for this level, if one exists.
     * @return levelTableEntry
     */
    public OldFroggerLevelTableEntry getLevelTableEntry() {
        return getGameInstance().getLevelTableEntry(getIndexEntry().getResourceId());
    }

    @Override
    public OldFroggerConfig getConfig() {
        return (OldFroggerConfig) super.getConfig();
    }

    /**
     * Gets the form config utilized by this level. Can be null.
     */
    public OldFroggerFormConfig getFormConfig() {
        return getMapConfig().getFormConfig();
    }

    /**
     * Gets the map config usable by this map.
     * @return mapConfig
     */
    public OldFroggerMapConfig getMapConfig() {
        if (this.cachedMapConfig != null)
            return this.cachedMapConfig;

        OldFroggerMapConfig mapConfig = getConfig().getMapConfigs().get(getFileDisplayName());
        return this.cachedMapConfig = mapConfig != null ? mapConfig : getConfig().getDefaultMapConfig();
    }

    /**
     * Get the format version which this map was loaded from.
     */
    public OldFroggerMapVersion getFormatVersion() {
        return getMapConfig().getVersion();
    }
}