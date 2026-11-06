package net.highwayfrogs.editor.games.sony.frogger.mods;

import lombok.Getter;
import lombok.NonNull;
import net.highwayfrogs.editor.games.shared.mods.IModSupport;
import net.highwayfrogs.editor.games.sony.frogger.FroggerGameInstance;
import net.highwayfrogs.editor.games.sony.frogger.ui.mods.FroggerModInstallController;

/**
 * Opens the Frogger mod package dialogs for a game instance.
 * Created by jimbolina on 11/6/2026.
 */
@Getter
public class FroggerModSupport implements IModSupport {
    @NonNull private final FroggerGameInstance gameInstance;

    public FroggerModSupport(@NonNull FroggerGameInstance gameInstance) {
        this.gameInstance = gameInstance;
    }

    @Override
    public void openInstallDialog() {
        FroggerModInstallController.openMenu(this.gameInstance);
    }
}
