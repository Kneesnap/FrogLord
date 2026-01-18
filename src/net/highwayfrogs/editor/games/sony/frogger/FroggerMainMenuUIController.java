package net.highwayfrogs.editor.games.sony.frogger;

import javafx.scene.Node;
import net.highwayfrogs.editor.games.sony.frogger.map.FroggerMapFile;
import net.highwayfrogs.editor.games.sony.frogger.ui.*;
import net.highwayfrogs.editor.games.sony.frogger.utils.FroggerVersionComparison;
import net.highwayfrogs.editor.games.sony.shared.ui.SCMainMenuUIController;
import net.highwayfrogs.editor.utils.objects.IndexBitArray;

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

        addMenuItem(this.menuBarEdit, "Edit Level Info", () -> LevelInfoController.openEditor(getGameInstance()));
        addMenuItem(this.menuBarEdit, "Edit Form Library", () -> FormEntryController.openEditor(getGameInstance()));
        addMenuItem(this.menuBarEdit, "Edit Scripts", () -> ScriptEditorController.openEditor(getGameInstance()));
        addMenuItem(this.menuBarEdit, "Edit Demo Table", () -> DemoTableEditorController.openEditor(getGameInstance()));
        addMenuItem(this.menuBarEdit, "Patches", () -> PatchController.openMenu(getGameInstance()));
        addMenuItem(this.menuBarEdit, "Make Difference Report", FroggerVersionComparison::generateReport);
        addMenuItem(this.menuBarEdit, "Find Unused Vertices", this::findUnusedVertices);
        addMenuItem(this.menuBarEdit, "Create Text Textures", () -> FroggerTextBuilderUIController.openEditor(getGameInstance()));
    }

    private void findUnusedVertices() {
        getGameInstance().getMainArchive().getAllFiles(FroggerMapFile.class).forEach(mapFile -> {
            IndexBitArray unusedVertices = mapFile.findUnusedVertexIds();
            int vertexCount = unusedVertices.getBitCount();
            if (vertexCount > 1)
                getLogger().info(" - %s has %d unused vertices.", mapFile.getFileDisplayName(), vertexCount);
        });
    }
}