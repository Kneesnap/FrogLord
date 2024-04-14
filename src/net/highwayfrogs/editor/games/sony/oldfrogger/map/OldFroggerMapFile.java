package net.highwayfrogs.editor.games.sony.oldfrogger.map;

import javafx.scene.image.Image;
import lombok.Getter;
import net.highwayfrogs.editor.file.WADFile;
import net.highwayfrogs.editor.file.map.MAPFile;
import net.highwayfrogs.editor.games.sony.oldfrogger.OldFroggerGameInstance;
import net.highwayfrogs.editor.games.sony.oldfrogger.config.OldFroggerConfig;
import net.highwayfrogs.editor.games.sony.oldfrogger.config.OldFroggerFormConfig;
import net.highwayfrogs.editor.games.sony.oldfrogger.config.OldFroggerLevelTableEntry;
import net.highwayfrogs.editor.games.sony.oldfrogger.config.OldFroggerMapConfig;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.mesh.OldFroggerMapMesh;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.mesh.OldFroggerMapMeshController;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.packet.*;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.ui.OldFroggerMapController;
import net.highwayfrogs.editor.games.sony.shared.SCChunkedFile;
import net.highwayfrogs.editor.games.sony.shared.SCChunkedFile.SCFilePacket.PacketSizeType;
import net.highwayfrogs.editor.gui.editor.MeshViewController;
import net.highwayfrogs.editor.system.Tuple2;

import java.util.List;

/**
 * Represents a map file in pre-recode frogger.
 * TODO: Support early format.
 * Created by Kneesnap on 12/8/2023.
 */
@Getter
public class OldFroggerMapFile extends SCChunkedFile<OldFroggerGameInstance> {
    private final OldFroggerMapHeaderPacket headerPacket;
    private final OldFroggerMapLevelSpecificPacket levelSpecificDataPacket;
    private final OldFroggerMapPathPacket pathPacket;
    private final OldFroggerMapZonePacket zonePacket;
    private final OldFroggerMapFormInstancePacket formInstancePacket;
    private final OldFroggerMapEntityMarkerPacket entityMarkerPacket;
    private final OldFroggerMapGraphicalHeaderPacket graphicalHeaderPacket;
    private final OldFroggerMapStandardPacket standardPacket;
    private final OldFroggerMapLightPacket lightPacket;
    private final OldFroggerMapGridHeaderPacket gridPacket;
    private final OldFroggerMapQuadPacket quadPacket;
    private final OldFroggerMapVertexPacket vertexPacket;
    private final OldFroggerMapAnimPacket animPacket;
    private final OldFroggerMapCameraHeightFieldPacket cameraHeightFieldPacket;

    // Random stuff
    private transient OldFroggerMapConfig cachedMapConfig;

    public OldFroggerMapFile(OldFroggerGameInstance instance) {
        super(instance, false);
        addFilePacket(this.headerPacket = new OldFroggerMapHeaderPacket(this));
        addFilePacket(this.levelSpecificDataPacket = new OldFroggerMapLevelSpecificPacket(this));
        addFilePacket(this.pathPacket = new OldFroggerMapPathPacket(this));
        addFilePacket(this.zonePacket = new OldFroggerMapZonePacket(this));
        addFilePacket(this.formInstancePacket = new OldFroggerMapFormInstancePacket(this));
        addFilePacket(this.entityMarkerPacket = new OldFroggerMapEntityMarkerPacket(this));
        addFilePacket(this.standardPacket = new OldFroggerMapStandardPacket(this));
        addFilePacket(this.graphicalHeaderPacket = new OldFroggerMapGraphicalHeaderPacket(this));
        addFilePacket(this.lightPacket = new OldFroggerMapLightPacket(this));
        addFilePacket(this.gridPacket = new OldFroggerMapGridHeaderPacket(this));
        addFilePacket(this.quadPacket = new OldFroggerMapQuadPacket(this));
        addFilePacket(this.vertexPacket = new OldFroggerMapVertexPacket(this));
        addFilePacket(this.animPacket = new OldFroggerMapAnimPacket(this));
        addFilePacket(this.cameraHeightFieldPacket = new OldFroggerMapCameraHeightFieldPacket(this));
    }

    @Override
    protected PacketSizeType getPacketSizeForUnknownChunk(String identifier) {
        return PacketSizeType.NO_SIZE;
    }

    @Override
    public Image getCollectionViewIcon() {
        return MAPFile.ICON;
    }

    @Override
    public OldFroggerMapController makeEditorUI() {
        return loadEditor(getGameInstance(), "edit-file-map", new OldFroggerMapController(getGameInstance()), this);
    }

    @Override
    public void handleWadEdit(WADFile parent) {
        MeshViewController.setupMeshViewer(getGameInstance(), new OldFroggerMapMeshController(), new OldFroggerMapMesh(this));
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