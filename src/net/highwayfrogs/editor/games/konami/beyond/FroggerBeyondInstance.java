package net.highwayfrogs.editor.games.konami.beyond;

import net.highwayfrogs.editor.games.konami.beyond.file.FroggerBeyondDummyFile;
import net.highwayfrogs.editor.games.konami.beyond.file.FroggerBeyondRwStreamFile;
import net.highwayfrogs.editor.games.renderware.RwStreamChunkTypeRegistry;
import net.highwayfrogs.editor.games.renderware.RwStreamFile;
import net.highwayfrogs.editor.games.shared.basic.BasicGameInstance;
import net.highwayfrogs.editor.games.shared.basic.file.BasicGameFile;
import net.highwayfrogs.editor.games.shared.basic.file.definition.IGameFileDefinition;

import java.io.File;

/**
 * Represents an instance of "Frogger Beyond".
 * Created by Kneesnap on 8/12/2024.
 */
public class FroggerBeyondInstance extends BasicGameInstance {
    private static final RwStreamChunkTypeRegistry rwStreamChunkTypeRegistry = RwStreamChunkTypeRegistry.getDefaultRegistry().clone();

    public FroggerBeyondInstance() {
        super(FroggerBeyondGameType.INSTANCE);
    }

    @Override
    protected boolean shouldLoadAsGameFile(File file, String fileName, String extension) {
        return "bin".equalsIgnoreCase(extension);
    }

    /**
     * Gets the RenderWare stream chunk type registry to use to load files.
     */
    public RwStreamChunkTypeRegistry getRwStreamChunkTypeRegistry() {
        return rwStreamChunkTypeRegistry;
    }

    @Override
    public BasicGameFile<?> createGameFile(IGameFileDefinition fileDefinition, byte[] rawData) {
        if (RwStreamFile.isRwStreamFile(rawData) || fileDefinition.getFileName().endsWith(".bin")) {
            // TODO: Why does the if statement fail if I exclude the test?
            return new FroggerBeyondRwStreamFile(fileDefinition);
        } else {
            return new FroggerBeyondDummyFile(fileDefinition);
        }
    }
}