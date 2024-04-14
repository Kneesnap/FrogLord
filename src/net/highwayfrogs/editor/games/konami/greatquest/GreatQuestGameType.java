package net.highwayfrogs.editor.games.konami.greatquest;

import net.highwayfrogs.editor.games.generic.GameInstance;
import net.highwayfrogs.editor.games.generic.IGameType;

/**
 * Represents all game instances of "Frogger: The Great Quest".
 * Created by Kneesnap on 4/10/2024.
 */
public class GreatQuestGameType implements IGameType {
    public static final GreatQuestGameType INSTANCE = new GreatQuestGameType();

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
        return new GreatQuestInstance();
    }
}