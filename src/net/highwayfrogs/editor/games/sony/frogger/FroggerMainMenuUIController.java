package net.highwayfrogs.editor.games.sony.frogger;

import javafx.application.Platform;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Alert.AlertType;
import net.highwayfrogs.editor.games.shared.mods.ModFormatConstants;
import net.highwayfrogs.editor.games.sony.SCGameType;
import net.highwayfrogs.editor.games.sony.frogger.map.FroggerMapFile;
import net.highwayfrogs.editor.games.sony.frogger.mods.FroggerBaseDifference;
import net.highwayfrogs.editor.games.sony.frogger.mods.FroggerBaseFingerprint;
import net.highwayfrogs.editor.games.sony.frogger.mods.FroggerModRoundTripCheck;
import net.highwayfrogs.editor.games.sony.frogger.mods.FroggerModRoundTripCheck.FroggerModRoundTripResult;
import net.highwayfrogs.editor.games.sony.frogger.ui.*;
import net.highwayfrogs.editor.games.sony.frogger.utils.FroggerVersionComparison;
import net.highwayfrogs.editor.games.sony.shared.ui.SCMainMenuUIController;
import net.highwayfrogs.editor.system.Config;
import net.highwayfrogs.editor.utils.FXUtils;
import net.highwayfrogs.editor.utils.FileUtils;
import net.highwayfrogs.editor.utils.FileUtils.BrowserFileType;
import net.highwayfrogs.editor.utils.FileUtils.SavedFilePath;
import net.highwayfrogs.editor.utils.Utils;
import net.highwayfrogs.editor.utils.objects.IndexBitArray;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * The main menu controller for Frogger.
 * Created by Kneesnap on 4/12/2024.
 */
public class FroggerMainMenuUIController extends SCMainMenuUIController<FroggerGameInstance> {
    private static final SavedFilePath BASE_FINGERPRINT_FILE = new SavedFilePath("baseFingerprintPath", "Specify the base fingerprint file...", Config.DEFAULT_FILE_TYPE);
    private static final SavedFilePath ROUND_TRIP_PACKAGE_FILE = new SavedFilePath("roundTripModPackagePath", "Select the mod package to check...",
            new BrowserFileType("FrogLord Mod Package", ModFormatConstants.EXTENSION.substring(1)));
    private static final SavedFilePath ROUND_TRIP_EXE_FILE = new SavedFilePath("roundTripExecutablePath", "Select the unmodified executable (its MWD must be beside it)...",
            SCGameType.EXECUTABLE_FILE_TYPE);
    private static final String DEFAULT_MWD_NAME = "FROGPSX.MWD";
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
        addMenuItem(this.menuBarDebug, "Check Mod Package Round-Trip", this::checkModPackageRoundTrip);
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

    private void checkModPackageRoundTrip() {
        File packageFile = FileUtils.askUserToOpenFile(getGameInstance(), ROUND_TRIP_PACKAGE_FILE);
        if (packageFile == null)
            return;

        File exeFile = FileUtils.askUserToOpenFile(getGameInstance(), ROUND_TRIP_EXE_FILE);
        if (exeFile == null)
            return;

        File mwdFile = findMwdBeside(exeFile);
        if (mwdFile == null) {
            FXUtils.showPopup(AlertType.ERROR, "No MWD found.", "Place the unmodified MWD (" + DEFAULT_MWD_NAME + ") beside " + exeFile.getName() + ".");
            return;
        }

        Scene scene = this.menuBar.getScene();
        if (scene != null)
            scene.setCursor(Cursor.WAIT);

        Platform.runLater(() -> {
            FroggerModRoundTripResult result = null;
            try {
                result = new FroggerModRoundTripCheck(getLogger()).check(packageFile, exeFile, mwdFile);
            } catch (Throwable th) {
                Utils.handleError(getLogger(), th, true, "Failed to check the round trip of '%s'.", packageFile.getName());
            } finally {
                if (scene != null)
                    scene.setCursor(Cursor.DEFAULT);
            }

            if (result == null)
                return;

            for (String line : result.describe().split("\\R"))
                getLogger().info(line);

            List<String> summary = result.getSummaryLines();
            StringBuilder builder = new StringBuilder();
            for (int i = 1; i < summary.size() && i <= SUMMARY_LINE_LIMIT; i++)
                builder.append(summary.get(i)).append(System.lineSeparator());
            if (summary.size() > SUMMARY_LINE_LIMIT + 1)
                builder.append("...").append(System.lineSeparator());
            builder.append("The full report is in the log.");
            FXUtils.showPopup(result.isPassed() ? AlertType.INFORMATION : AlertType.WARNING, result.getVerdict(), builder.toString());
        });
    }

    private static File findMwdBeside(File exeFile) {
        File folder = exeFile.getAbsoluteFile().getParentFile();
        File[] files = folder != null ? folder.listFiles() : null;
        if (files == null)
            return null;

        List<File> mwdFiles = new ArrayList<>();
        for (File file : files) {
            if (!file.isFile() || !file.getName().toUpperCase(Locale.ROOT).endsWith(".MWD"))
                continue;
            if (file.getName().equalsIgnoreCase(DEFAULT_MWD_NAME))
                return file;
            mwdFiles.add(file);
        }

        return mwdFiles.size() == 1 ? mwdFiles.get(0) : null;
    }
}
