package net.highwayfrogs.editor.games.sony.medievil.map;

import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.image.Image;
import lombok.Getter;
import net.highwayfrogs.editor.games.sony.medievil.MediEvilGameInstance;
import net.highwayfrogs.editor.games.sony.medievil.MediEvilLevelTableEntry;
import net.highwayfrogs.editor.games.sony.medievil.config.MediEvilConfig;
import net.highwayfrogs.editor.games.sony.medievil.map.mesh.MediEvilMapMesh;
import net.highwayfrogs.editor.games.sony.medievil.map.mesh.MediEvilMapMeshController;
import net.highwayfrogs.editor.games.sony.medievil.map.packet.*;
import net.highwayfrogs.editor.games.sony.medievil.map.packet.grid.MediEvilMapGridPacket;
import net.highwayfrogs.editor.games.sony.medievil.map.polygrid.MediEvilPolygonGridFile;
import net.highwayfrogs.editor.games.sony.medievil.map.quadtree.MediEvilMapQuadTree;
import net.highwayfrogs.editor.games.sony.shared.ISCTextureUser;
import net.highwayfrogs.editor.games.sony.shared.SCChunkedFile;
import net.highwayfrogs.editor.games.sony.shared.SCChunkedFile.SCFilePacket.PacketSizeType;
import net.highwayfrogs.editor.games.sony.shared.TextureRemapArray;
import net.highwayfrogs.editor.gui.GameUIController;
import net.highwayfrogs.editor.gui.ImageResource;
import net.highwayfrogs.editor.gui.editor.MeshViewController;
import net.highwayfrogs.editor.utils.FileUtils;
import net.highwayfrogs.editor.utils.Utils.ProblemResponse;

import java.io.File;
import java.util.List;

/**
 * Represents a map file in MediEvil.
 * TODO:
 *  - 1) Can we figure out the skybox? It'd be very nice to have the skybox automatically load and display. (Allow turning it off though)
 *  - 2) Can we figure out lighting? (Allow turning off)
 *  - 3) Get splines working in 3D space.
 * Cloned from a file created by Kneesnap on 03/9/2024.
 */
@Getter
public class MediEvilMapFile extends SCChunkedFile<MediEvilGameInstance> implements ISCTextureUser {
    private final MediEvilMapHeaderPacket headerPacket;
    private final MediEvilMapEntitiesPacket entitiesPacket;
    private final MediEvilMapPathChainPacket pathChainPacket;
    private final MediEvilMap2DSplinePacket spline2DPacket;
    private final MediEvilMap3DSplinePacket spline3DPacket;
    private final MediEvilMapGraphicsPacket graphicsPacket;
    private final MediEvilMapCollprimsPacket collprimsPacket;
    private final MediEvilMapGridPacket gridPacket;

    public MediEvilMapFile(MediEvilGameInstance instance) {
        super(instance, false);
        addFilePacket(this.headerPacket = new MediEvilMapHeaderPacket(this));
        addFilePacket(this.entitiesPacket = new MediEvilMapEntitiesPacket(this)); // Entities
        addFilePacket(this.pathChainPacket = new MediEvilMapPathChainPacket(this));
        addFilePacket(this.spline2DPacket = new MediEvilMap2DSplinePacket(this));
        addFilePacket(this.spline3DPacket = new MediEvilMap3DSplinePacket(this));
        addFilePacket(this.graphicsPacket = new MediEvilMapGraphicsPacket(this));
        addFilePacket(this.collprimsPacket = new MediEvilMapCollprimsPacket(this)); // Collision Primitives
        addFilePacket(this.gridPacket = new MediEvilMapGridPacket(this));
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
    public GameUIController<?> makeEditorUI() {
        return null;
    }

    @Override
    public void performDefaultUIAction() {
        MeshViewController.setupMeshViewer(getGameInstance(), new MediEvilMapMeshController(getGameInstance()), new MediEvilMapMesh(this));
    }

    /**
     * Get the level table entry for this level, if one exists.
     * @return levelTableEntry
     */
    public MediEvilLevelTableEntry getLevelTableEntry() {
        return getGameInstance().getLevelTableEntry(getFileResourceId());
    }

    @Override
    public List<Short> getUsedTextureIds() {
        MediEvilLevelTableEntry levelTableEntry = getLevelTableEntry();
        if (levelTableEntry == null)
            return null;

        TextureRemapArray textureRemap = levelTableEntry.getRemap();
        return textureRemap != null ? textureRemap.getTextureIds() : null;
    }

    @Override
    public String getTextureUserName() {
        return getFileDisplayName();
    }

    @Override
    public MediEvilConfig getConfig() {
        return (MediEvilConfig) super.getConfig();
    }

    @Override
    public void setupRightClickMenuItems(ContextMenu contextMenu) {
        super.setupRightClickMenuItems(contextMenu);

        MenuItem exportMfsFile = new MenuItem("Export to .mfs file");
        contextMenu.getItems().add(exportMfsFile);
        exportMfsFile.setOnAction(event -> askUserToExportMfsFile());

        MenuItem importMfsFile = new MenuItem("Import from .mfs file");
        contextMenu.getItems().add(importMfsFile);
        importMfsFile.setOnAction(event -> askUserToImportMfsFile());
    }
    private void askUserToExportMfsFile() {
        File outputFolder = FileUtils.askUserToSelectFolder(getGameInstance(), MFSUtil.EXPORT_FOLDER);
        if (outputFolder != null)
            MFSUtil.saveMap(this, outputFolder, ProblemResponse.CREATE_POPUP);
    }

    private void askUserToImportMfsFile() {
        File importFile = FileUtils.askUserToOpenFile(getGameInstance(), MFSUtil.IMPORT_PATH);
        if (importFile != null)
            MFSUtil.importMfsFile(getLogger(), this, importFile);
    }

    /**
     * Regenerate the polygon data for this map.
     */
    public void regeneratePolygonData() {
        MediEvilLevelTableEntry levelTableEntry = getLevelTableEntry();
        if (levelTableEntry == null)
            throw new IllegalStateException("Could not find LevelTableEntry for '" + getFileDisplayName() + "'.");

        MediEvilMapQuadTree quadTree = levelTableEntry.getQuadTreeFile();
        if (quadTree == null)
            throw new IllegalStateException("No .QTR file could be found for '" + getFileDisplayName() + "'.");

        MediEvilPolygonGridFile polygonGrid = levelTableEntry.getPolygonGridFile();
        if (polygonGrid == null)
            throw new IllegalStateException("No .PGD file could be found for '" + getFileDisplayName() + "'.");

        this.gridPacket.regenerate();
        quadTree.regenerate();
        polygonGrid.regenerate();
    }
}