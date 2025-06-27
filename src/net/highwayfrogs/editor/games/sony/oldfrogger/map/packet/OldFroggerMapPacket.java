package net.highwayfrogs.editor.games.sony.oldfrogger.map.packet;

import net.highwayfrogs.editor.games.sony.oldfrogger.OldFroggerGameInstance;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.OldFroggerMapFile;
import net.highwayfrogs.editor.games.sony.shared.SCChunkedFile;
import net.highwayfrogs.editor.games.sony.shared.SCChunkedFile.SCFilePacket;

/**
 * Represents a basic map packet for old Frogger.
 * Created by Kneesnap on 12/8/2023.
 */
public abstract class OldFroggerMapPacket extends SCFilePacket<OldFroggerMapFile, OldFroggerGameInstance> {
    public OldFroggerMapPacket(OldFroggerMapFile parentFile, String identifier) {
        this(parentFile, identifier, true, PacketSizeType.SIZE_INCLUSIVE);
    }

    public OldFroggerMapPacket(OldFroggerMapFile parentFile, String identifier, boolean required) {
        this(parentFile, identifier, required, PacketSizeType.SIZE_INCLUSIVE);
    }

    public OldFroggerMapPacket(OldFroggerMapFile parentFile, String identifier, PacketSizeType sizeType) {
        this(parentFile, identifier, true, sizeType);
    }

    public OldFroggerMapPacket(OldFroggerMapFile parentFile, String identifier, boolean required, PacketSizeType sizeType) {
        super(parentFile, identifier, required, sizeType);
    }

    @Override
    public OldFroggerGameInstance getGameInstance() {
        return (OldFroggerGameInstance) super.getGameInstance();
    }

    @Override
    public void copyAndConvertData(SCFilePacket<? extends SCChunkedFile<OldFroggerGameInstance>, OldFroggerGameInstance> newChunk) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}