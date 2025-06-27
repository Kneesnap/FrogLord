package net.highwayfrogs.editor.games.sony.shared.map;

import net.highwayfrogs.editor.games.sony.SCGameInstance;
import net.highwayfrogs.editor.games.sony.shared.SCChunkedFile;
import net.highwayfrogs.editor.games.sony.shared.SCChunkedFile.SCFilePacket;

/**
 * Represents a packet of data in a map file.
 * Created by Kneesnap on 5/7/2024.
 */
public abstract class SCMapFilePacket<TMapFile extends SCMapFile<TGameInstance>, TGameInstance extends SCGameInstance> extends SCFilePacket<TMapFile, TGameInstance> {
    public SCMapFilePacket(TMapFile parentFile, String identifier) {
        this(parentFile, identifier, false, PacketSizeType.SIZE_INCLUSIVE);
    }

    public SCMapFilePacket(TMapFile parentFile, String identifier, boolean required) {
        this(parentFile, identifier, required, PacketSizeType.SIZE_INCLUSIVE);
    }

    public SCMapFilePacket(TMapFile parentFile, String identifier, PacketSizeType sizeType) {
        this(parentFile, identifier, false, sizeType);
    }

    public SCMapFilePacket(TMapFile parentFile, String identifier, boolean required, PacketSizeType sizeType) {
        super(parentFile, identifier, required, sizeType);
    }

    @Override
    public void copyAndConvertData(SCFilePacket<? extends SCChunkedFile<TGameInstance>, TGameInstance> newChunk) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}