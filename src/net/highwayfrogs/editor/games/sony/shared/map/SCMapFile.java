package net.highwayfrogs.editor.games.sony.shared.map;

import javafx.scene.image.Image;
import net.highwayfrogs.editor.file.WADFile;
import net.highwayfrogs.editor.games.sony.SCGameInstance;
import net.highwayfrogs.editor.games.sony.shared.SCChunkedFile;
import net.highwayfrogs.editor.games.sony.shared.SCChunkedFile.SCFilePacket.PacketSizeType;
import net.highwayfrogs.editor.games.sony.shared.map.mesh.SCMapMesh;
import net.highwayfrogs.editor.games.sony.shared.map.mesh.SCMapMeshController;
import net.highwayfrogs.editor.games.sony.shared.map.packet.SCMapPolygonPacket;
import net.highwayfrogs.editor.gui.ImageResource;
import net.highwayfrogs.editor.gui.editor.MeshViewController;

/**
 * Represents the basic map file format
 * Created by Kneesnap on 5/7/2024.
 */
public abstract class SCMapFile<TGameInstance extends SCGameInstance> extends SCChunkedFile<TGameInstance> {
    public SCMapFile(TGameInstance instance) {
        super(instance, false);
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
    public void handleWadEdit(WADFile parent) {
        MeshViewController.setupMeshViewer(getGameInstance(), new SCMapMeshController(), new SCMapMesh(this));
    }

    /**
     * Gets the level table entry available for the level.
     */
    public abstract ISCLevelTableEntry getLevelTableEntry();

    /**
     * Gets the polygon packet from the map.
     */
    public abstract SCMapPolygonPacket<TGameInstance> getPolygonPacket();
}