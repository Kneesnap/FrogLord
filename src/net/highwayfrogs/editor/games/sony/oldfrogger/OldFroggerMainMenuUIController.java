package net.highwayfrogs.editor.games.sony.oldfrogger;

import javafx.scene.Node;
import net.highwayfrogs.editor.games.sony.oldfrogger.utils.OldFroggerFontGenerator;
import net.highwayfrogs.editor.games.sony.shared.ui.SCMainMenuUIController;

/**
 * Represents the main menu UI controller for pre-recode Frogger.
 * Created by Kneesnap on 2/3/2026.
 */
public class OldFroggerMainMenuUIController extends SCMainMenuUIController<OldFroggerGameInstance> {
    public OldFroggerMainMenuUIController(OldFroggerGameInstance instance) {
        super(instance);
    }

    @Override
    protected void onControllerLoad(Node rootNode) {
        super.onControllerLoad(rootNode);
        addMenuItem(this.menuBarEdit, "Generate FontForge Script", () -> OldFroggerFontGenerator.saveFontForgePythonScript(getGameInstance()));
    }
}
