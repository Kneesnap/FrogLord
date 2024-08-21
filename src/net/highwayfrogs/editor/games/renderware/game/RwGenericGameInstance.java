package net.highwayfrogs.editor.games.renderware.game;

import net.highwayfrogs.editor.games.renderware.RwStreamChunkTypeRegistry;
import net.highwayfrogs.editor.games.renderware.RwStreamFile;
import net.highwayfrogs.editor.games.renderware.game.file.RwGenericDummyFile;
import net.highwayfrogs.editor.games.renderware.game.file.RwGenericStreamFile;
import net.highwayfrogs.editor.games.shared.basic.BasicGameInstance;
import net.highwayfrogs.editor.games.shared.basic.file.BasicGameFile;
import net.highwayfrogs.editor.games.shared.basic.file.definition.IGameFileDefinition;

import java.io.File;

/**
 * Represents a generic game using RenderWare.
 * Created by Kneesnap on 8/18/2024.
 */
public class RwGenericGameInstance extends BasicGameInstance {
    private static final RwStreamChunkTypeRegistry rwStreamChunkTypeRegistry = RwStreamChunkTypeRegistry.getDefaultRegistry().clone();

    public RwGenericGameInstance() {
        super(RwGenericGameType.INSTANCE);
    }

    @Override
    protected boolean shouldLoadAsGameFile(File file, String fileName, String extension) {
        return "rws".equalsIgnoreCase(extension);
    }

    /**
     * Gets the RenderWare stream chunk type registry to use to load files.
     */
    public RwStreamChunkTypeRegistry getRwStreamChunkTypeRegistry() {
        return rwStreamChunkTypeRegistry;
    }

    @Override
    public BasicGameFile<?> createGameFile(IGameFileDefinition fileDefinition, byte[] rawData) {
        if (RwStreamFile.isRwStreamFile(rawData) || fileDefinition.getFileName().endsWith(".rws")) {
            return new RwGenericStreamFile(fileDefinition);
        } else {
            return new RwGenericDummyFile(fileDefinition);
        }
    }
}