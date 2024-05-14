package net.highwayfrogs.editor.games.shared.sound;

import net.highwayfrogs.editor.utils.IGameObject;

import java.util.List;

/**
 * Many games keep an audio header separate from audio body data.
 * This interface represents a game-agnostic interface for these files.
 * Created by Kneesnap on 5/13/2024.
 */
public interface ISoundBank extends IGameObject {
    /**
     * Gets a list of sounds available in the sound bank.
     */
    List<? extends ISoundSample> getSounds();
}