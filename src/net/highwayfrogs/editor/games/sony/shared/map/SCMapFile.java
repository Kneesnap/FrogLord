package net.highwayfrogs.editor.games.sony.shared.map;

import javafx.scene.image.Image;
import lombok.Getter;
import net.highwayfrogs.editor.games.sony.SCGameInstance;
import net.highwayfrogs.editor.games.sony.shared.ISCTextureUser;
import net.highwayfrogs.editor.games.sony.shared.SCChunkedFile;
import net.highwayfrogs.editor.games.sony.shared.SCChunkedFile.SCFilePacket.PacketSizeType;
import net.highwayfrogs.editor.games.sony.shared.TextureRemapArray;
import net.highwayfrogs.editor.games.sony.shared.map.mesh.SCMapMesh;
import net.highwayfrogs.editor.games.sony.shared.map.mesh.SCMapMeshController;
import net.highwayfrogs.editor.games.sony.shared.map.packet.SCMapEntityPacket;
import net.highwayfrogs.editor.games.sony.shared.map.packet.SCMapHeaderPacket;
import net.highwayfrogs.editor.games.sony.shared.map.packet.SCMapPolygonPacket;
import net.highwayfrogs.editor.games.sony.shared.map.ui.SCMapFileUIController;
import net.highwayfrogs.editor.gui.GameUIController;
import net.highwayfrogs.editor.gui.ImageResource;
import net.highwayfrogs.editor.gui.editor.MeshViewController;

import java.util.List;

/**
 * Represents the basic v2 standardized map file format.
 * This file format seems to be used by all games after MediEvil, although it seems heavily based on MediEvil's format.
 * Different games have different data packets.
 * Created by Kneesnap on 5/7/2024.
 */
@Getter
public abstract class SCMapFile<TGameInstance extends SCGameInstance> extends SCChunkedFile<TGameInstance> implements ISCTextureUser {
    private final SCMapHeaderPacket<TGameInstance> headerPacket;
    private final SCMapPolygonPacket<TGameInstance> polygonPacket;
    private final SCMapEntityPacket<TGameInstance> entityPacket;

    public SCMapFile(TGameInstance instance) {
        super(instance, false);
        this.headerPacket = createHeaderPacket();
        this.polygonPacket = createPolygonPacket();
        this.entityPacket = createEntityPacket();
        addFilePacketsInOrder();
    }

    /**
     * Allows adding file packets in the order seen in the different games.
     * This order is based on what I saw in MediEvil 2.
     */
    protected void addFilePacketsInOrder() {
        addFilePacket(this.headerPacket); // FORG
        // PGRD
        // DGRD
        addFilePacket(this.polygonPacket); // POLY
        addFilePacket(this.entityPacket); // ENTP
        // ZONE
        // NWRK
        // SPLI
    }

    /**
     * Gets the map file which is a parent to this one.
     * Returns null if this is the parent map file.
     */
    public abstract SCMapFile<TGameInstance> getParentMap();

    /**
     * Test if this map is a parent map.
     */
    public boolean isParentMap() {
        SCMapFile<TGameInstance> parentFile = getParentMap();
        return parentFile == null || (parentFile == this);
    }

    /**
     * Creates the file header packet.
     */
    protected SCMapHeaderPacket<TGameInstance> createHeaderPacket() {
        return new SCMapHeaderPacket<>(this);
    }

    /**
     * Creates the polygon packet.
     */
    protected SCMapPolygonPacket<TGameInstance> createPolygonPacket() {
        return new SCMapPolygonPacket<>(this);
    }

    /**
     * Creates the entity packet.
     */
    protected SCMapEntityPacket<TGameInstance> createEntityPacket() {
        return new SCMapEntityPacket<>(this);
    }

    @Override
    public GameUIController<?> makeEditorUI() {
        return loadEditor(getGameInstance(), new SCMapFileUIController<>(getGameInstance()), this);
    }

    @Override
    protected PacketSizeType getPacketSizeForUnknownChunk(String identifier) {
        return PacketSizeType.SIZE_INCLUSIVE;
    }

    @Override
    public Image getCollectionViewIcon() {
        return ImageResource.TREASURE_MAP_32.getFxImage();
    }

    @Override
    public void performDefaultUIAction() {
        super.performDefaultUIAction();
        MeshViewController.setupMeshViewer(getGameInstance(), new SCMapMeshController<>(getGameInstance()), new SCMapMesh(this));
    }

    /**
     * Gets the level table entry available for the level.
     */
    public abstract ISCLevelTableEntry getLevelTableEntry();

    @Override
    public List<Short> getUsedTextureIds() {
        ISCLevelTableEntry levelTableEntry = getLevelTableEntry();
        if (levelTableEntry == null)
            return null;

        TextureRemapArray textureRemap = levelTableEntry.getRemap();
        return textureRemap != null ? textureRemap.getTextureIds() : null;
    }
}