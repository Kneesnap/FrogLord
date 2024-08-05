package net.highwayfrogs.editor.games.konami.hudson;

import lombok.Getter;
import lombok.NonNull;
import net.highwayfrogs.editor.games.generic.GameInstance;
import net.highwayfrogs.editor.games.generic.GameObject;
import net.highwayfrogs.editor.utils.Utils;

import java.io.File;

/**
 * Represents a Hudson file definition which came from the user's filesystem.
 * Created by Kneesnap on 8/4/2024.
 */
public class HudsonFileUserFSDefinition extends GameObject<GameInstance> implements IHudsonFileDefinition {
    @Getter @NonNull private final File file;
    private String cachedFullFileName;

    public HudsonFileUserFSDefinition(GameInstance instance, @NonNull File file) {
        super(instance);
        this.file = file;
    }

    @Override
    public String getFileName() {
        return this.file.getName();
    }

    @Override
    public String getFullFileName() {
        if (this.cachedFullFileName == null)
            this.cachedFullFileName = Utils.toLocalPath(getGameInstance().getMainGameFolder(), this.file, false);

        return this.cachedFullFileName;
    }
}