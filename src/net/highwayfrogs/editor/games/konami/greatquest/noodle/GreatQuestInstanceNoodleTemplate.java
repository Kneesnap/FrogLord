package net.highwayfrogs.editor.games.konami.greatquest.noodle;

import net.highwayfrogs.editor.games.generic.scripting.GameInstanceNoodleTemplate;
import net.highwayfrogs.editor.games.konami.greatquest.GreatQuestInstance;

/**
 * Exposes Frogger: The Great Quest's Game Instance to Noodle scripts.
 * Created by Kneesnap on 10/23/2024.
 */
public class GreatQuestInstanceNoodleTemplate extends GameInstanceNoodleTemplate<GreatQuestInstance> {
    public static final GreatQuestInstanceNoodleTemplate INSTANCE = new GreatQuestInstanceNoodleTemplate();

    public GreatQuestInstanceNoodleTemplate() {
        super(GreatQuestInstance.class);
    }

    @Override
    protected void onSetup() {
        // Do nothing.
    }

    @Override
    protected void onSetupJvmWrapper() {
        super.onSetupJvmWrapper();
        getJvmWrapper().addFunction("getAllFiles");
        getJvmWrapper().addFunction("getLooseFiles");
        getJvmWrapper().addFunction("getMainArchive");
        getJvmWrapper().addFunction("getSoundChunkFile");
        getJvmWrapper().addFunction("getFullSoundPath", int.class);
        getJvmWrapper().addFunction("getShortenedSoundPath", int.class, boolean.class);
        getJvmWrapper().addFunction("getSoundFileName", int.class, boolean.class);
    }
}