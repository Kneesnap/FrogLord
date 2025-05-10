package net.highwayfrogs.editor.games.konami.rescue;

import net.highwayfrogs.editor.games.konami.hudson.HudsonGameFile;
import net.highwayfrogs.editor.games.konami.hudson.HudsonGameInstance;
import net.highwayfrogs.editor.games.konami.rescue.file.FroggerRescueSoundBank;
import net.highwayfrogs.editor.games.renderware.RwStreamChunkTypeRegistry;
import net.highwayfrogs.editor.games.shared.basic.file.definition.IGameFileDefinition;
import net.highwayfrogs.editor.utils.DataUtils;

import java.io.File;

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
    public RwStreamChunkTypeRegistry getRwStreamChunkTypeRegistry() {
        return rwStreamChunkTypeRegistry;
    }

    @Override
    protected boolean shouldLoadAsGameFile(File file, String fileName, String extension) {
        return "bnk".equalsIgnoreCase(extension) || super.shouldLoadAsGameFile(file, fileName, extension);
    }

    @Override
    public HudsonGameFile createGameFile(IGameFileDefinition fileDefinition, byte[] rawData) {
        if (DataUtils.testSignature(rawData, HFSFile.SIGNATURE)) {
            return new HFSFile(fileDefinition);
        } else if (DataUtils.testSignature(rawData, FroggerRescueSoundBank.SIGNATURE)) {
            return new FroggerRescueSoundBank(fileDefinition);
        } else {
            return super.createGameFile(fileDefinition, rawData);
        }
    }

    @Override
    public boolean isShowSaveWarning() {
        return true;
    }
}