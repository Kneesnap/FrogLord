package net.highwayfrogs.editor.games.sony.frogger;

import javafx.scene.Node;
import net.highwayfrogs.editor.file.map.MAPFile;
import net.highwayfrogs.editor.file.standard.SVector;
import net.highwayfrogs.editor.games.sony.frogger.ui.*;
import net.highwayfrogs.editor.games.sony.shared.ui.SCMainMenuUIController;
import net.highwayfrogs.editor.gui.GUIMain;
import net.highwayfrogs.editor.utils.FroggerVersionComparison;

import java.util.List;

/**
 * The main menu controller for Frogger.
 * Created by Kneesnap on 4/12/2024.
 */
public class FroggerMainMenuUIController extends SCMainMenuUIController<FroggerGameInstance> {
    public FroggerMainMenuUIController(FroggerGameInstance instance) {
        super(instance);
    }

    @Override
    protected void onControllerLoad(Node rootNode) {
        super.onControllerLoad(rootNode);

        addMenuItem(this.menuBarEdit, "Generate Header Files", () -> getGameInstance().exportCode(GUIMain.getWorkingDirectory()));
        addMenuItem(this.menuBarEdit, "Edit Level Info", () -> LevelInfoController.openEditor(getGameInstance()));
        addMenuItem(this.menuBarEdit, "Edit Form Library", () -> FormEntryController.openEditor(getGameInstance()));
        addMenuItem(this.menuBarEdit, "Edit Scripts", () -> ScriptEditorController.openEditor(getGameInstance()));
        addMenuItem(this.menuBarEdit, "Edit Demo Table", () -> DemoTableEditorController.openEditor(getGameInstance()));
        addMenuItem(this.menuBarEdit, "Patches", () -> PatchController.openMenu(getGameInstance()));
        addMenuItem(this.menuBarEdit, "Make Difference Report", FroggerVersionComparison::generateReport);
        addMenuItem(this.menuBarEdit, "Find Unused Textures", this::findUnusedVertices);
    }

    private void findUnusedVertices() {
        getGameInstance().getMainArchive().getAllFiles(MAPFile.class).forEach(mapFile -> {
            List<SVector> unusedVertices = mapFile.findUnusedVertices();
            if (unusedVertices.size() > 1)
                getLogger().info(" - " + mapFile.getFileDisplayName() + " has " + unusedVertices.size() + " unused vertices.");
        });
    }
}