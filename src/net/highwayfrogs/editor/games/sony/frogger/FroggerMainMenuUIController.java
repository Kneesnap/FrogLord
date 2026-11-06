package net.highwayfrogs.editor.games.sony.frogger;

import javafx.scene.Node;
import javafx.scene.control.Alert.AlertType;
import net.highwayfrogs.editor.games.sony.frogger.map.FroggerMapFile;
import net.highwayfrogs.editor.games.sony.frogger.mods.FroggerBaseDifference;
import net.highwayfrogs.editor.games.sony.frogger.mods.FroggerBaseFingerprint;
import net.highwayfrogs.editor.games.sony.frogger.ui.*;
import net.highwayfrogs.editor.games.sony.frogger.utils.FroggerVersionComparison;
import net.highwayfrogs.editor.games.sony.shared.ui.SCMainMenuUIController;
import net.highwayfrogs.editor.system.Config;
import net.highwayfrogs.editor.utils.FXUtils;
import net.highwayfrogs.editor.utils.FileUtils;
import net.highwayfrogs.editor.utils.FileUtils.SavedFilePath;
import net.highwayfrogs.editor.utils.Utils;
import net.highwayfrogs.editor.utils.objects.IndexBitArray;

import java.io.File;
import java.util.List;

/**
 * The main menu controller for Frogger.
 * Created by Kneesnap on 4/12/2024.
 */
public class FroggerMainMenuUIController extends SCMainMenuUIController<FroggerGameInstance> {
    private static final SavedFilePath BASE_FINGERPRINT_FILE = new SavedFilePath("baseFingerprintPath", "Specify the base fingerprint file...", Config.DEFAULT_FILE_TYPE);
    private static final int SUMMARY_LINE_LIMIT = 20;

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
        addMenuItem(this.menuBarDebug, "Make Difference Report", FroggerVersionComparison::generateReport);
        addMenuItem(this.menuBarDebug, "Find Unused Vertices", this::findUnusedVertices);
        addMenuItem(this.menuBarDebug, "Generate Base Fingerprint", this::generateBaseFingerprint);
        addMenuItem(this.menuBarDebug, "Compare With Base Fingerprint", this::compareWithBaseFingerprint);
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

    private void generateBaseFingerprint() {
        String buildName = getGameInstance().getVersionConfig().getInternalName();
        File outputFile = FileUtils.askUserToSaveFile(getGameInstance(), BASE_FINGERPRINT_FILE, buildName + "." + Config.DEFAULT_EXTENSION, true);
        if (outputFile == null)
            return;

        try {
            FroggerBaseFingerprint fingerprint = FroggerBaseFingerprint.generate(getGameInstance(), getLogger());
            fingerprint.save(outputFile);
            getLogger().info("Saved the base fingerprint of %s to '%s'.", buildName, outputFile.getName());
            FXUtils.showPopup(AlertType.INFORMATION, "Generated the base fingerprint.", "Saved the base fingerprint of " + buildName + " to " + outputFile.getName() + ".");
        } catch (Throwable th) {
            Utils.handleError(getLogger(), th, true, "Failed to generate the base fingerprint of %s.", buildName);
        }
    }

    private void compareWithBaseFingerprint() {
        String buildName = getGameInstance().getVersionConfig().getInternalName();
        try {
            FroggerBaseFingerprint fingerprint = FroggerBaseFingerprint.loadBundled(buildName);
            if (fingerprint == null) {
                File inputFile = FileUtils.askUserToOpenFile(getGameInstance(), BASE_FINGERPRINT_FILE);
                if (inputFile == null)
                    return;

                fingerprint = FroggerBaseFingerprint.load(Config.loadConfigFromTextFile(inputFile, false));
            }

            FroggerBaseDifference difference = fingerprint.compare(getGameInstance(), getLogger());
            List<String> lines = difference.describe();
            for (String line : lines)
                getLogger().info(line);

            List<String> summary = difference.getSummaryLines();
            StringBuilder builder = new StringBuilder();
            for (int i = 0; i < summary.size() && i < SUMMARY_LINE_LIMIT; i++)
                builder.append(summary.get(i)).append(System.lineSeparator());
            if (summary.size() > SUMMARY_LINE_LIMIT)
                builder.append("...").append(System.lineSeparator());
            builder.append("The full list is in the log.");

            String header = difference.isEmpty() ? "No differences from the base fingerprint of " + fingerprint.getBuildName() + "."
                    : "Differences from the base fingerprint of " + fingerprint.getBuildName() + ".";
            FXUtils.showPopup(AlertType.INFORMATION, header, builder.toString());
        } catch (Throwable th) {
            Utils.handleError(getLogger(), th, true, "Failed to compare the game with the base fingerprint of %s.", buildName);
        }
    }
}
