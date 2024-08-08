package net.highwayfrogs.editor.games.konami.rescue;

import net.highwayfrogs.editor.games.konami.hudson.HudsonGameFile;
import net.highwayfrogs.editor.games.konami.hudson.HudsonGameInstance;
import net.highwayfrogs.editor.games.konami.hudson.IHudsonFileDefinition;
import net.highwayfrogs.editor.games.konami.hudson.IHudsonFileSystem;
import net.highwayfrogs.editor.games.renderware.RwStreamChunkTypeRegistry;
import net.highwayfrogs.editor.utils.Utils;

/**
 * Represents an instance of Frogger's Adventures: The Rescue.
 * Created by Kneesnap on 8/8/2024.
 */
public class FroggerRescueInstance extends HudsonGameInstance {
    private static final RwStreamChunkTypeRegistry rwStreamChunkTypeRegistry = RwStreamChunkTypeRegistry.getDefaultRegistry().clone();

    public FroggerRescueInstance() {
        super(FroggerRescueGameType.INSTANCE);
    }

    @Override
    protected IHudsonFileSystem createHfsFile(IHudsonFileDefinition fileDefinition) {
        return new HFSFile(fileDefinition);
    }

    @Override
    public RwStreamChunkTypeRegistry getRwStreamChunkTypeRegistry() {
        return rwStreamChunkTypeRegistry;
    }

    @Override
    public HudsonGameFile createGameFile(IHudsonFileDefinition fileDefinition, byte[] rawData) {
        if (Utils.testSignature(rawData, HFSFile.SIGNATURE)) {
            return new net.highwayfrogs.editor.games.konami.ancientshadow.HFSFile(fileDefinition);
        } else {
            return super.createGameFile(fileDefinition, rawData);
        }
    }
}