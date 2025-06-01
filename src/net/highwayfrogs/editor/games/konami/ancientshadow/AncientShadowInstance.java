package net.highwayfrogs.editor.games.konami.ancientshadow;

import net.highwayfrogs.editor.games.konami.hudson.HudsonGameFile;
import net.highwayfrogs.editor.games.konami.hudson.HudsonGameInstance;
import net.highwayfrogs.editor.games.renderware.RwStreamChunkTypeRegistry;
import net.highwayfrogs.editor.games.shared.basic.file.definition.IGameFileDefinition;
import net.highwayfrogs.editor.utils.DataUtils;

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
    public HudsonGameFile createGameFile(IGameFileDefinition fileDefinition, byte[] rawData) {
        if (DataUtils.testSignature(rawData, HFSFile.SIGNATURE)) {
            return new HFSFile(fileDefinition);
        } else {
            return super.createGameFile(fileDefinition, rawData);
        }
    }

    @Override
    public boolean isShowSaveWarning() {
        return true;
    }
}