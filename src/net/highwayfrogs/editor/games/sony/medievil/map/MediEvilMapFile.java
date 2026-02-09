package net.highwayfrogs.editor.games.sony.medievil.map;

import javafx.scene.image.Image;
import lombok.Getter;
import net.highwayfrogs.editor.games.sony.medievil.MediEvilGameInstance;
import net.highwayfrogs.editor.games.sony.medievil.MediEvilLevelTableEntry;
import net.highwayfrogs.editor.games.sony.medievil.config.MediEvilConfig;
import net.highwayfrogs.editor.games.sony.medievil.map.mesh.MediEvilMapMesh;
import net.highwayfrogs.editor.games.sony.medievil.map.mesh.MediEvilMapMeshController;
import net.highwayfrogs.editor.games.sony.medievil.map.packet.*;
import net.highwayfrogs.editor.games.sony.medievil.map.packet.grid.MediEvilMapGridPacket;
import net.highwayfrogs.editor.games.sony.shared.ISCTextureUser;
import net.highwayfrogs.editor.games.sony.shared.SCChunkedFile;
import net.highwayfrogs.editor.games.sony.shared.SCChunkedFile.SCFilePacket.PacketSizeType;
import net.highwayfrogs.editor.games.sony.shared.TextureRemapArray;
import net.highwayfrogs.editor.gui.GameUIController;
import net.highwayfrogs.editor.gui.ImageResource;
import net.highwayfrogs.editor.gui.editor.MeshViewController;

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
}