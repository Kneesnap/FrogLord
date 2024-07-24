package net.highwayfrogs.editor.games.sony.oldfrogger.map;

import javafx.scene.image.Image;
import lombok.Getter;
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
import net.highwayfrogs.editor.games.sony.shared.mwd.WADFile;
import net.highwayfrogs.editor.gui.ImageResource;
import net.highwayfrogs.editor.gui.components.PropertyListViewerComponent.PropertyList;
import net.highwayfrogs.editor.gui.editor.MeshViewController;

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
        return ImageResource.TREASURE_MAP_32.getFxImage();
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
    public PropertyList addToPropertyList(PropertyList propertyList) {
        propertyList = super.addToPropertyList(propertyList);
        propertyList.add("File Version", getMapConfig().getVersion());
        propertyList.add("Comment", this.headerPacket.getComment());
        propertyList.add("Default Reaction", this.levelSpecificDataPacket.getDefaultReactionType());
        propertyList.add("Paths", this.pathPacket.getPaths().size());
        propertyList.add("Zones", this.zonePacket.getZones().size());
        propertyList.add("Forms", this.formInstancePacket.getForms().size() + " (Table Size: " + this.formInstancePacket.getFormTableSize() + ")");
        propertyList.add("Entities", this.entityMarkerPacket.getEntities().size());
        propertyList.add("Grid Type", this.gridPacket.getType());
        propertyList.add("Grid Size", this.gridPacket.getXSize() + " x " + this.gridPacket.getZSize());
        propertyList.add("Grid Count", this.gridPacket.getXCount() + " x " + this.gridPacket.getZCount() + " (" + this.gridPacket.getGrids().size() + ")");
        propertyList.add("Grid Base", this.gridPacket.getBasePoint().toFloatString());
        propertyList.add("Lights", this.lightPacket.getLights().size());
        propertyList.add("Vertices", this.vertexPacket.getVertices().size());
        propertyList.add("UV Animations", this.animPacket.getUvAnimations().size());

        if (this.cameraHeightFieldPacket != null) {
            propertyList.add("Camera Height Grid Dimensions", this.cameraHeightFieldPacket.getXSquareCount() + " x " + this.cameraHeightFieldPacket.getZSquareCount());
            propertyList.add("Camera Height Grid Square Size", this.cameraHeightFieldPacket.getSquareXSizeAsFloat() + " x " + this.cameraHeightFieldPacket.getSquareZSizeAsFloat());
            propertyList.add("Camera Height Grid Start Pos", this.cameraHeightFieldPacket.getStartXAsFloat() + ", " + this.cameraHeightFieldPacket.getStartZAsFloat());
        }

        return propertyList;
    }

    /**
     * Get the level table entry for this level, if one exists.
     * @return levelTableEntry
     */
    public OldFroggerLevelTableEntry getLevelTableEntry() {
        return getGameInstance().getLevelTableEntry(getFileResourceId());
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