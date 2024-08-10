package net.highwayfrogs.editor.games.konami.ancientshadow;

import net.highwayfrogs.editor.games.konami.hudson.HudsonGameFile;
import net.highwayfrogs.editor.games.konami.hudson.HudsonGameInstance;
import net.highwayfrogs.editor.games.konami.hudson.IHudsonFileDefinition;
import net.highwayfrogs.editor.games.renderware.RwStreamChunkTypeRegistry;
import net.highwayfrogs.editor.utils.Utils;

/**
 * Represents an instance of Frogger Ancient Shadow.
 * Created by Kneesnap on 8/4/2024.
 */
public class AncientShadowInstance extends HudsonGameInstance {
    private static final RwStreamChunkTypeRegistry rwStreamChunkTypeRegistry = RwStreamChunkTypeRegistry.getDefaultRegistry().clone();

    public AncientShadowInstance() {
        super(AncientShadowGameType.INSTANCE);
    }

    @Override
    public RwStreamChunkTypeRegistry getRwStreamChunkTypeRegistry() {
        return rwStreamChunkTypeRegistry;
    }

    @Override
    public HudsonGameFile createGameFile(IHudsonFileDefinition fileDefinition, byte[] rawData) {
        if (Utils.testSignature(rawData, HFSFile.SIGNATURE)) {
            return new HFSFile(fileDefinition);
        } else {
            return super.createGameFile(fileDefinition, rawData);
        }
    }
}