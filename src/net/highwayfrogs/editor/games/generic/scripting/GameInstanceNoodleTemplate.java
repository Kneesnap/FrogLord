package net.highwayfrogs.editor.games.generic.scripting;

import net.highwayfrogs.editor.games.generic.GameInstance;
import net.highwayfrogs.editor.scripting.runtime.templates.NoodleObjectTemplate;

/**
 * Represents the GameInstance.
 * Created by Kneesnap on 10/23/2024.
 */
public abstract class GameInstanceNoodleTemplate<TGameInstance extends GameInstance> extends NoodleObjectTemplate<TGameInstance> {
    public GameInstanceNoodleTemplate(Class<TGameInstance> wrappedClass) {
        super(wrappedClass, wrappedClass.getSimpleName());
    }

    @Override
    protected void onSetupJvmWrapper() {
        getJvmWrapper().addFunction("getGameType");
        getJvmWrapper().addFunction("getConfig");
        getJvmWrapper().addFunction("getMainGameFolder");
        getJvmWrapper().addFunction("getLogger");
    }
}
