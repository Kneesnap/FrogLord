package net.highwayfrogs.editor.games.sony.medievil.map.packet;

import net.highwayfrogs.editor.games.sony.medievil.MediEvilGameInstance;
import net.highwayfrogs.editor.games.sony.medievil.map.MediEvilMapFile;
import net.highwayfrogs.editor.games.sony.shared.SCChunkedFile.SCFilePacket;

/**
 * Represents a MediEvil map file packet.
 * Created by Kneesnap on 3/8/2024.
 */
public abstract class MediEvilMapPacket extends SCFilePacket<MediEvilMapFile, MediEvilGameInstance> {
    public MediEvilMapPacket(MediEvilMapFile parentFile, String identifier) {
        this(parentFile, identifier, true, PacketSizeType.SIZE_INCLUSIVE);
    }

    public MediEvilMapPacket(MediEvilMapFile parentFile, String identifier, boolean required) {
        this(parentFile, identifier, required, PacketSizeType.SIZE_INCLUSIVE);
    }

    public MediEvilMapPacket(MediEvilMapFile parentFile, String identifier, PacketSizeType sizeType) {
        this(parentFile, identifier, true, sizeType);
    }

    public MediEvilMapPacket(MediEvilMapFile parentFile, String identifier, boolean required, PacketSizeType sizeType) {
        super(parentFile, identifier, required, sizeType);
    }
}