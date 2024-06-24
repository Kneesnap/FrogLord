package net.highwayfrogs.editor.games.sony.frogger.map.packets;

import net.highwayfrogs.editor.file.config.FroggerMapConfig;
import net.highwayfrogs.editor.games.sony.frogger.FroggerConfig;
import net.highwayfrogs.editor.games.sony.frogger.FroggerGameInstance;
import net.highwayfrogs.editor.games.sony.frogger.map.FroggerMapFile;
import net.highwayfrogs.editor.games.sony.shared.SCChunkedFile.SCFilePacket;
import net.highwayfrogs.editor.gui.components.PropertyListViewerComponent.IPropertyListCreator;

/**
 * Represents a Frogger map file packet.
 * Created by Kneesnap on 5/25/2024.
 */
public abstract class FroggerMapFilePacket extends SCFilePacket<FroggerMapFile, FroggerGameInstance> implements IPropertyListCreator {
    public FroggerMapFilePacket(FroggerMapFile parentFile, String identifier) {
        this(parentFile, identifier, true, PacketSizeType.NO_SIZE);
    }

    public FroggerMapFilePacket(FroggerMapFile parentFile, String identifier, boolean required) {
        this(parentFile, identifier, required, PacketSizeType.NO_SIZE);
    }

    public FroggerMapFilePacket(FroggerMapFile parentFile, String identifier, PacketSizeType sizeType) {
        this(parentFile, identifier, true, sizeType);
    }

    public FroggerMapFilePacket(FroggerMapFile parentFile, String identifier, boolean required, PacketSizeType sizeType) {
        super(parentFile, identifier, required, sizeType);
    }

    @Override
    public FroggerConfig getConfig() {
        return (FroggerConfig) super.getConfig();
    }

    /**
     * Gets the map config for the map.
     */
    public FroggerMapConfig getMapConfig() {
        return getParentFile().getMapConfig();
    }
}