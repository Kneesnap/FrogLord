package net.highwayfrogs.editor.games.konami.greatquest;

import net.highwayfrogs.editor.games.generic.GameInstance;
import net.highwayfrogs.editor.games.generic.IGameType;

/**
 * Represents all game instances of "Frogger: The Great Quest".
 * Created by Kneesnap on 4/10/2024.
 */
public class TGQGameType implements IGameType {
    public static final TGQGameType INSTANCE = new TGQGameType();

    @Override
    public String getDisplayName() {
        return "Frogger: The Great Quest";
    }

    @Override
    public String getIdentifier() {
        return "greatquest";
    }

    @Override
    public GameInstance createGameInstance() {
        // TODO: IMPLEMENT.
        return null;
    }
}