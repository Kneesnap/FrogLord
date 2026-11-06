package net.highwayfrogs.editor.games.sony.frogger.ui.mods;

import javafx.fxml.FXML;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import lombok.Getter;
import net.highwayfrogs.editor.games.shared.mods.ModInstallResult;
import net.highwayfrogs.editor.games.shared.mods.ModManifest;
import net.highwayfrogs.editor.games.shared.mods.ModManifest.ModTarget;
import net.highwayfrogs.editor.games.shared.mods.ModPackage;
import net.highwayfrogs.editor.games.shared.mods.ModPackageReader;
import net.highwayfrogs.editor.games.shared.mods.ModProblem;
import net.highwayfrogs.editor.games.sony.frogger.FroggerGameInstance;
import net.highwayfrogs.editor.games.sony.frogger.mods.FroggerBaseDifference;
import net.highwayfrogs.editor.games.sony.frogger.mods.FroggerBaseFingerprint;
import net.highwayfrogs.editor.games.sony.frogger.mods.FroggerModInstaller;
import net.highwayfrogs.editor.games.sony.frogger.mods.FroggerModManifest;
import net.highwayfrogs.editor.games.sony.shared.ui.SCMainMenuUIController;
import net.highwayfrogs.editor.gui.GameUIController;
import net.highwayfrogs.editor.utils.FXUtils;
import net.highwayfrogs.editor.utils.FileUtils;
import net.highwayfrogs.editor.utils.FileUtils.BrowserFileType;
import net.highwayfrogs.editor.utils.FileUtils.SavedFilePath;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * The dialog which reads a mod package, checks it against the open Frogger game, installs it and offers to save the result.
 * Created by jimbolina on 11/6/2026.
 */
public class FroggerModInstallController extends GameUIController<FroggerGameInstance> {
    @FXML private TextField packagePathField;
    @FXML private Button browseButton;
    @FXML private Button loadButton;
    @FXML private TextArea summaryTextArea;
    @FXML private Label statusLabel;
    @FXML private CheckBox installAnywayCheckBox;
    @FXML private Button installButton;
    @FXML private Button saveButton;
    @FXML private Button closeButton;
    @FXML private TextArea resultTextArea;
    @FXML private Label saveWarningLabel;
    @Getter private ModPackage loadedPackage;
    @Getter private ModInstallResult lastResult;
    private boolean targetAccepted;
    private List<String> baseDifferences;
    private boolean installAttempted;

    private static final String WINDOW_TEMPLATE = "window-mod-install";
    private static final String WINDOW_TITLE = "Install Mod Package";
    private static final BrowserFileType MOD_PACKAGE_FILE_TYPE = new BrowserFileType("FrogLord Mod Package", "mod");
    private static final SavedFilePath MOD_PACKAGE_PATH = new SavedFilePath("modPackageInstallPath", "Select the mod package to install...", MOD_PACKAGE_FILE_TYPE);
    private static final String SAVE_FAILURE_MARKER = "saving will fail";

    public FroggerModInstallController(FroggerGameInstance instance) {
        super(instance);
    }

    @Override
    protected void onControllerLoad(Node rootNode) {
        this.browseButton.setOnAction(event -> browseForPackage());
        this.loadButton.setOnAction(event -> loadPackage());
        this.packagePathField.setOnAction(event -> loadPackage());
        this.installButton.setOnAction(event -> installPackage());
        this.saveButton.setOnAction(event -> saveGame());
        this.closeButton.setOnAction(event -> closeWindow());
        this.installAnywayCheckBox.selectedProperty().addListener((observable, oldValue, newValue) -> updateControls());
        clearPackage();
    }

    @Override
    public void onSceneAdd(Scene newScene) {
        super.onSceneAdd(newScene);
        FXUtils.closeOnEscapeKey((Stage) newScene.getWindow(), null);
    }

    /**
     * Sets the path of the package to load, without loading it.
     * @param packageFile the package file
     */
    public void setPackageFile(File packageFile) {
        this.packagePathField.setText(packageFile != null ? packageFile.getPath() : "");
    }

    /**
     * Gets the text of the package summary pane.
     * @return summaryText
     */
    public String getSummaryText() {
        return this.summaryTextArea.getText();
    }

    /**
     * Gets the text of the install result pane.
     * @return resultText
     */
    public String getResultText() {
        return this.resultTextArea.getText();
    }

    /**
     * Tests whether the package can be installed with the current settings.
     * @return installAllowed
     */
    public boolean isInstallAllowed() {
        return !this.installAttempted && this.loadedPackage != null && !this.loadedPackage.hasErrors() && this.targetAccepted
                && (this.baseDifferences == null || this.baseDifferences.isEmpty() || this.installAnywayCheckBox.isSelected());
    }

    /**
     * Tests whether the installed result may be saved now.
     * @return saveAllowed
     */
    public boolean isSaveAllowed() {
        return this.lastResult != null && this.lastResult.isSuccess() && !isSaveBlocked(this.lastResult);
    }

    /**
     * Allows installing onto a game which already differs from the base build.
     * @param allowModifiedBase whether to allow it
     */
    public void setAllowModifiedBase(boolean allowModifiedBase) {
        this.installAnywayCheckBox.setSelected(allowModifiedBase);
    }

    private void browseForPackage() {
        File packageFile = FileUtils.askUserToOpenFile(getGameInstance(), MOD_PACKAGE_PATH);
        if (packageFile == null)
            return;

        setPackageFile(packageFile);
        loadPackage();
    }

    /**
     * Reads the package at the entered path, shows its summary and checks its targets and the base of the open game.
     */
    public void loadPackage() {
        clearPackage();
        String path = this.packagePathField.getText() != null ? this.packagePathField.getText().trim() : "";
        if (path.isEmpty()) {
            showStatus("Enter or browse for the path of a .mod package.");
            return;
        }

        File packageFile = new File(path);
        if (!packageFile.isFile()) {
            showStatus("The file '" + packageFile + "' does not exist.");
            return;
        }

        List<ModProblem> problems = new ArrayList<>();
        ModPackage modPackage;
        try {
            modPackage = new ModPackageReader(FroggerModManifest.createValidator()).read(packageFile, problems);
        } catch (IOException ex) {
            this.summaryTextArea.setText("Could not read '" + packageFile.getName() + "': " + ex.getMessage() + System.lineSeparator());
            showStatus("The file is not a readable mod package.");
            return;
        }

        StringBuilder summary = new StringBuilder();
        if (modPackage == null) {
            summary.append("No manifest could be read from '").append(packageFile.getName()).append("'.").append(System.lineSeparator());
            appendProblems(summary, "Problems", problems);
            this.summaryTextArea.setText(summary.toString());
            showStatus("The package cannot be installed.");
            return;
        }

        this.loadedPackage = modPackage;
        String buildName = getGameInstance().getVersionConfig().getInternalName();
        ModManifest manifest = modPackage.getManifest();
        ModTarget target = manifest.findTarget(buildName);
        appendPackageSummary(summary, modPackage, target);

        List<String> statusLines = new ArrayList<>();
        if (modPackage.hasErrors())
            statusLines.add("The package has errors, so it cannot be installed.");

        summary.append(System.lineSeparator()).append("Target check:").append(System.lineSeparator());
        if (target == null) {
            List<String> targetNames = new ArrayList<>();
            for (ModTarget packageTarget : manifest.getTargets())
                targetNames.add(packageTarget.getName());
            String message = "This package targets " + (targetNames.isEmpty() ? "no build" : String.join(", ", targetNames))
                    + ", not the open build '" + buildName + "'.";
            summary.append(" - Refused: ").append(message).append(System.lineSeparator());
            statusLines.add(message);
        } else {
            this.targetAccepted = true;
            summary.append(" - The open build '").append(buildName).append("' matches target ").append(target.getName())
                    .append(target.getName().equals(buildName) ? "" : " through an alias").append('.').append(System.lineSeparator());
        }

        if (this.targetAccepted && !modPackage.hasErrors())
            checkBase(summary, statusLines, buildName);

        this.summaryTextArea.setText(summary.toString());
        showStatus(statusLines.isEmpty() ? null : String.join(System.lineSeparator(), statusLines));
        updateControls();
    }

    private void checkBase(StringBuilder summary, List<String> statusLines, String buildName) {
        summary.append(System.lineSeparator()).append("Base check:").append(System.lineSeparator());
        FroggerBaseFingerprint fingerprint = FroggerBaseFingerprint.loadBundled(buildName);
        if (fingerprint == null) {
            this.baseDifferences = null;
            summary.append(" - FrogLord has no base fingerprint for '").append(buildName)
                    .append("', so changes already made to the open game cannot be detected.").append(System.lineSeparator());
        } else {
            FroggerBaseDifference difference = fingerprint.compare(getGameInstance(), null);
            if (difference.isEmpty()) {
                this.baseDifferences = new ArrayList<>();
                summary.append(" - The open game matches the retail build.").append(System.lineSeparator());
            } else {
                this.baseDifferences = difference.getSummaryLines();
                summary.append(" - Warning: the open game already differs from the retail build in:").append(System.lineSeparator());
                for (String line : this.baseDifferences)
                    summary.append("     ").append(line).append(System.lineSeparator());
                statusLines.add("The open game already differs from the retail build. Tick 'Install anyway' to install onto it; the result is the union of both and there is no undo.");
            }
        }

        if (getGameInstance().isPreviouslySavedByFrogLord()) {
            summary.append(" - Warning: the open executable was saved by FrogLord before, so it may already hold other changes.").append(System.lineSeparator());
            statusLines.add("The open executable was saved by FrogLord before.");
        }
    }

    private static void appendPackageSummary(StringBuilder summary, ModPackage modPackage, ModTarget target) {
        ModManifest manifest = modPackage.getManifest();
        appendField(summary, "Name", manifest.getName());
        appendField(summary, "Version", manifest.getVersion());
        appendField(summary, "Author", manifest.getAuthor());
        appendField(summary, "Id", manifest.getId());
        appendField(summary, "Licence", manifest.getLicence());
        appendField(summary, "Homepage", manifest.getHomepage());
        appendField(summary, "Exported by", manifest.getExportedBy());
        appendField(summary, "Minimum FrogLord version", manifest.getMinFrogLordVersion());

        List<String> targetLines = new ArrayList<>();
        for (ModTarget packageTarget : manifest.getTargets())
            targetLines.add(packageTarget.getName() + (packageTarget.getAliases().isEmpty() ? "" : " (aliases " + String.join(", ", packageTarget.getAliases()) + ")"));
        appendField(summary, "Targets", targetLines.isEmpty() ? "none" : String.join("; ", targetLines));

        String description = manifest.getDescription();
        if (description != null && !description.isEmpty())
            summary.append("Description:").append(System.lineSeparator()).append(description.trim()).append(System.lineSeparator());

        FroggerModManifest froggerManifest = new FroggerModManifest(manifest, target != null ? target.getName() : null);
        summary.append("Contents: ").append(modPackage.getEntryList().size()).append(" file(s)").append(System.lineSeparator());
        summary.append(" - Gameplay: ").append(countGameplayKeys(froggerManifest)).append(" key(s)").append(System.lineSeparator());
        summary.append(" - Level select: ").append(froggerManifest.getLevelEntries().size()).append(" level(s)")
                .append(froggerManifest.getArcadeOrder() != null ? ", arcade order" : "")
                .append(froggerManifest.getRaceOrder() != null ? ", race order" : "").append(System.lineSeparator());
        summary.append(" - Maps: ").append(froggerManifest.getMapEntries().size()).append(System.lineSeparator());
        summary.append(" - Models: ").append(froggerManifest.getModelEntries().size()).append(System.lineSeparator());
        summary.append(" - Images: ").append(froggerManifest.getImageEntries().size()).append(System.lineSeparator());
        summary.append(" - Themes: ").append(froggerManifest.getThemeEntries().size()).append(System.lineSeparator());
        summary.append(" - Demos: ").append(froggerManifest.getDemoFiles() != null ? froggerManifest.getDemoFiles().size() : 0)
                .append(" file(s), ").append(froggerManifest.getDemoRows().size()).append(" table row(s)").append(System.lineSeparator());
        summary.append(" - Patches: ").append(froggerManifest.getPatches().size()).append(System.lineSeparator());

        List<ModProblem> problems = new ArrayList<>(modPackage.getProblems());
        problems.addAll(froggerManifest.getProblems());
        if (problems.isEmpty()) {
            summary.append("Validation: no problems.").append(System.lineSeparator());
        } else {
            appendProblems(summary, "Validation problems", problems);
        }
    }

    private static int countGameplayKeys(FroggerModManifest manifest) {
        int count = 0;
        if (manifest.getInitialLevels() != null)
            count++;
        if (manifest.getVisibleLevels() != null)
            count++;
        if (manifest.getJungleSwap() != null)
            count++;
        if (manifest.getFrameRate() != null)
            count++;
        return count;
    }

    private static void appendField(StringBuilder summary, String label, String value) {
        if (value != null && !value.isEmpty())
            summary.append(label).append(": ").append(value).append(System.lineSeparator());
    }

    private static void appendProblems(StringBuilder summary, String title, List<ModProblem> problems) {
        if (problems.isEmpty())
            return;

        summary.append(title).append(':').append(System.lineSeparator());
        for (ModProblem problem : problems)
            summary.append(" - ").append(problem).append(System.lineSeparator());
    }

    /**
     * Installs the loaded package onto the open game and shows the result. Nothing is saved.
     */
    public void installPackage() {
        if (!isInstallAllowed())
            return;

        this.installAttempted = true;
        this.installButton.setDisable(true);
        Scene scene = this.installButton.getScene();
        if (scene != null)
            scene.setCursor(Cursor.WAIT);

        try {
            FroggerModInstaller installer = new FroggerModInstaller(getGameInstance(), this.loadedPackage, null);
            this.lastResult = installer.install(this.installAnywayCheckBox.isSelected(), this.baseDifferences);
        } catch (Throwable th) {
            this.lastResult = null;
            this.resultTextArea.setText("The install stopped unexpectedly: " + th + System.lineSeparator() + "Reload the game before saving." + System.lineSeparator());
            handleError(th, true, "Failed to install the mod package.");
        } finally {
            if (scene != null)
                scene.setCursor(Cursor.DEFAULT);
        }

        if (this.lastResult != null) {
            this.resultTextArea.setText(this.lastResult.describe());
            if (!this.lastResult.isSuccess() && !this.lastResult.isGameModified())
                this.installAttempted = false;

            if (this.lastResult.isSuccess() && isSaveBlocked(this.lastResult)) {
                this.saveWarningLabel.setText("Saving will fail: the replaced images do not fit VRAM. See the warnings above; the game was changed in memory only, so reload it before trying another package.");
            } else if (!this.lastResult.isSuccess() && this.lastResult.isGameModified()) {
                this.saveWarningLabel.setText("The open game was partly changed before the install stopped. Reload it before saving.");
            }
        }

        updateControls();
    }

    private static boolean isSaveBlocked(ModInstallResult result) {
        for (ModProblem warning : result.getWarnings())
            if (warning.getMessage().toLowerCase().contains(SAVE_FAILURE_MARKER))
                return true;

        return false;
    }

    private void saveGame() {
        if (!isSaveAllowed())
            return;

        SCMainMenuUIController<?> mainMenu = getGameInstance().getMainMenuController();
        if (mainMenu == null) {
            this.saveWarningLabel.setText("The main window is not open, so the game cannot be saved from here.");
            return;
        }

        try {
            mainMenu.saveMainGameData();
        } catch (Throwable th) {
            handleError(th, true, "Failed to save game data.");
        }
    }

    private void clearPackage() {
        this.loadedPackage = null;
        this.targetAccepted = false;
        this.baseDifferences = null;
        this.summaryTextArea.setText("");
        this.statusLabel.setText(null);
        if (this.lastResult == null || !this.installAttempted) {
            this.resultTextArea.setText("");
            this.saveWarningLabel.setText(null);
        }
        updateControls();
    }

    private void showStatus(String message) {
        this.statusLabel.setText(message);
    }

    private void updateControls() {
        boolean modifiedBase = this.baseDifferences != null && !this.baseDifferences.isEmpty();
        this.installAnywayCheckBox.setVisible(modifiedBase);
        this.installAnywayCheckBox.setManaged(modifiedBase);
        this.installButton.setDisable(!isInstallAllowed());
        this.saveButton.setDisable(!isSaveAllowed());
    }

    /**
     * Opens the Install Mod Package window.
     * @param instance the open game to install onto
     * @return the window's controller, or null if the window could not be opened
     */
    public static FroggerModInstallController openMenu(FroggerGameInstance instance) {
        FroggerModInstallController controller = new FroggerModInstallController(instance);
        return FXUtils.createWindowFromFXMLTemplate(WINDOW_TEMPLATE, controller, WINDOW_TITLE, false) ? controller : null;
    }
}
