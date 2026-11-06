package net.highwayfrogs.editor.games.sony.frogger.ui.mods;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.CheckBoxTreeCell;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;
import lombok.Getter;
import lombok.NonNull;
import net.highwayfrogs.editor.games.shared.mods.ModFormatConstants;
import net.highwayfrogs.editor.games.shared.mods.ModManifest;
import net.highwayfrogs.editor.games.shared.mods.ModProblem;
import net.highwayfrogs.editor.games.sony.SCGameType;
import net.highwayfrogs.editor.games.sony.frogger.FroggerGameInstance;
import net.highwayfrogs.editor.games.sony.frogger.data.FroggerLevelSelectEntry;
import net.highwayfrogs.editor.games.sony.frogger.map.FroggerMapLevelID;
import net.highwayfrogs.editor.games.sony.frogger.mods.*;
import net.highwayfrogs.editor.games.sony.frogger.mods.FroggerBaseDifference.FroggerBaseSectionDifference;
import net.highwayfrogs.editor.games.sony.frogger.mods.FroggerBaseDifference.FroggerExeBlockChange;
import net.highwayfrogs.editor.gui.GameUIController;
import net.highwayfrogs.editor.system.Config;
import net.highwayfrogs.editor.utils.FXUtils;
import net.highwayfrogs.editor.utils.FileUtils;
import net.highwayfrogs.editor.utils.FileUtils.BrowserFileType;
import net.highwayfrogs.editor.utils.FileUtils.SavedFilePath;

import java.io.File;
import java.util.*;

/**
 * The dialog which compares the open Frogger game with its base build and exports the changes as a mod package.
 * Created by jimbolina on 11/6/2026.
 */
public class FroggerModExportController extends GameUIController<FroggerGameInstance> {
    @FXML private TextField nameField;
    @FXML private TextField versionField;
    @FXML private TextField authorField;
    @FXML private TextField licenceField;
    @FXML private TextField idField;
    @FXML private Button newIdButton;
    @FXML private TextField homepageField;
    @FXML private TextArea descriptionTextArea;
    @FXML private ComboBox<FroggerModExportBaseMode> baseModeComboBox;
    @FXML private Button chooseOriginalButton;
    @FXML private ProgressIndicator baseProgressIndicator;
    @FXML private Label baseStatusLabel;
    @FXML private Button analyseButton;
    @FXML private Label analyseStatusLabel;
    @FXML private TreeView<String> changeTreeView;
    @FXML private GridPane levelGridPane;
    @FXML private TextArea warningsTextArea;
    @FXML private CheckBox rawWritesCheckBox;
    @FXML private TextField outputPathField;
    @FXML private Button browseOutputButton;
    @FXML private Button exportButton;
    @FXML private Button closeButton;
    @FXML private Label statusLabel;
    @Getter private ModExportResult lastResult;
    @Getter private File lastExportedFile;
    @Getter private FroggerModExportBaseMode baseMode;
    private final FroggerModExportOptions options = new FroggerModExportOptions();
    private final List<FroggerChangeItem> changeItems = new ArrayList<>();
    private final Map<String, TextField> levelNameFields = new LinkedHashMap<>();
    private FroggerBaseFingerprint shippedFingerprint;
    private FroggerBaseFingerprint allContentBase;
    private FroggerGameInstance baseCopy;
    private FroggerBaseFingerprint baseCopyFingerprint;
    private File baseCopyExecutable;
    private boolean loadingBase;
    private boolean updatingBaseMode;

    private static final String WINDOW_TEMPLATE = "window-mod-export";
    private static final String WINDOW_TITLE = "Export Mod Package";
    private static final String CONFIG_SECTION = "ModExport";
    private static final String CONFIG_LEVEL_NAMES = "LevelNames";
    private static final String KEY_NAME = "name";
    private static final String KEY_VERSION = "version";
    private static final String KEY_AUTHOR = "author";
    private static final String KEY_ID = "id";
    private static final String KEY_LICENCE = "licence";
    private static final String KEY_HOMEPAGE = "homepage";
    private static final String KEY_OUTPUT = "outputPath";
    private static final String SECTION_EXE_BLOCKS = FroggerBaseFingerprint.SECTION_EXE_BLOCKS;
    private static final BrowserFileType MOD_PACKAGE_FILE_TYPE = new BrowserFileType("FrogLord Mod Package", ModFormatConstants.EXTENSION.substring(1));
    private static final SavedFilePath MOD_PACKAGE_PATH = new SavedFilePath("modPackageExportPath", "Choose where to write the mod package...", MOD_PACKAGE_FILE_TYPE);
    private static final SavedFilePath ORIGINAL_EXECUTABLE_PATH = new SavedFilePath("modExportOriginalExecutable", "Select the unmodified game executable...", SCGameType.EXECUTABLE_FILE_TYPE);

    public FroggerModExportController(FroggerGameInstance instance) {
        super(instance);
    }

    /**
     * The base the open game is compared with.
     */
    public enum FroggerModExportBaseMode {
        SHIPPED_FINGERPRINT("Shipped fingerprint"),
        ORIGINAL_FILES("Compare with original files..."),
        NONE("None: all content is exported");

        private final String displayName;

        FroggerModExportBaseMode(String displayName) {
            this.displayName = displayName;
        }

        @Override
        public String toString() {
            return this.displayName;
        }
    }

    @Override
    protected void onControllerLoad(Node rootNode) {
        this.shippedFingerprint = FroggerBaseFingerprint.loadBundled(getGameInstance().getVersionConfig().getInternalName());
        this.baseModeComboBox.getItems().setAll(FroggerModExportBaseMode.values());
        this.baseModeComboBox.valueProperty().addListener((observable, oldValue, newValue) -> onBaseModeSelected(newValue));
        this.chooseOriginalButton.setOnAction(event -> chooseOriginalFiles());
        this.newIdButton.setOnAction(event -> this.idField.setText(UUID.randomUUID().toString()));
        this.analyseButton.setOnAction(event -> analyse());
        this.browseOutputButton.setOnAction(event -> browseForOutput());
        this.exportButton.setOnAction(event -> exportPackage());
        this.closeButton.setOnAction(event -> closeWindow());
        this.rawWritesCheckBox.selectedProperty().addListener((observable, oldValue, newValue) -> {
            this.options.setIncludeRawExecutableWrites(newValue);
            updateControls();
        });
        this.changeTreeView.setCellFactory(CheckBoxTreeCell.forTreeView());
        this.changeTreeView.setRoot(new CheckBoxTreeItem<>("Changes"));
        this.baseProgressIndicator.setVisible(false);

        loadSavedMetadata();
        setBaseModeInternal(this.shippedFingerprint != null ? FroggerModExportBaseMode.SHIPPED_FINGERPRINT : FroggerModExportBaseMode.NONE);
        clearAnalysis();
    }

    @Override
    public void onSceneAdd(Scene newScene) {
        super.onSceneAdd(newScene);
        FXUtils.closeOnEscapeKey((Stage) newScene.getWindow(), null);
    }

    /**
     * Sets the metadata written to the [Mod] section.
     * @param name the mod name
     * @param version the mod version
     * @param author the author
     * @param id the id, or null to keep the current one
     * @param licence the licence, or null
     * @param homepage the homepage, or null
     * @param description the description, or null
     */
    public void setMetadata(String name, String version, String author, String id, String licence, String homepage, String description) {
        this.nameField.setText(name);
        this.versionField.setText(version);
        this.authorField.setText(author);
        if (id != null)
            this.idField.setText(id);
        this.licenceField.setText(licence);
        this.homepageField.setText(homepage);
        this.descriptionTextArea.setText(description);
    }

    /**
     * Selects the base the open game is compared with. Choosing the original files opens the copy straight away, on the calling thread.
     * @param mode the base mode
     * @param originalExecutable the unmodified executable for ORIGINAL_FILES, or null to keep the copy already opened
     * @return true iff the base is ready to compare with
     */
    public boolean setBaseMode(@NonNull FroggerModExportBaseMode mode, File originalExecutable) {
        if (mode == FroggerModExportBaseMode.ORIGINAL_FILES && originalExecutable != null) {
            try {
                acceptBaseCopy(FroggerModUtils.loadBaseCopy(getGameInstance(), originalExecutable), originalExecutable);
            } catch (Throwable th) {
                this.baseStatusLabel.setText("Could not open the original files: " + th.getMessage());
                return false;
            }
        }

        setBaseModeInternal(mode);
        return isBaseReady();
    }

    /**
     * Sets the file the package is written to.
     * @param outputFile the .mod file
     */
    public void setOutputFile(File outputFile) {
        this.outputPathField.setText(outputFile != null ? outputFile.getPath() : "");
    }

    /**
     * Sets the level name written as name= for an arcade row listed in the level table.
     * @param levelCode the level code, such as ORG1
     * @param name the name, or null for none
     * @return true iff the level is listed in the table
     */
    public boolean setLevelName(@NonNull String levelCode, String name) {
        TextField field = this.levelNameFields.get(levelCode);
        if (field == null)
            return false;

        field.setText(name != null ? name : "");
        return true;
    }

    /**
     * Ticks or unticks a detected change; unticked changes are left out of the package.
     * @param section the section of the change, as the change list shows it, such as Images
     * @param key the key of the change, such as LS_ALL:org1pic
     * @return true iff the change is in the list
     */
    public boolean setChangeIncluded(@NonNull String section, @NonNull String key, boolean included) {
        for (FroggerChangeItem item : this.changeItems) {
            if (item.section.equals(section) && item.key.equals(key)) {
                item.treeItem.setSelected(included);
                return true;
            }
        }

        return false;
    }

    /**
     * Allows or forbids packaging the unexplained executable changes as raw writes, which needs the original files.
     * @param includeRawWrites whether to package them
     */
    public void setIncludeRawWrites(boolean includeRawWrites) {
        this.rawWritesCheckBox.setSelected(includeRawWrites && !this.rawWritesCheckBox.isDisabled());
    }

    /**
     * Gets the change list as text: one headline per section and one line per change with its check box state.
     * @return changeListText
     */
    public String getChangeListText() {
        StringBuilder builder = new StringBuilder();
        for (TreeItem<String> sectionItem : this.changeTreeView.getRoot().getChildren()) {
            builder.append(sectionItem.getValue()).append(System.lineSeparator());
            for (TreeItem<String> child : sectionItem.getChildren())
                builder.append("  [").append(((CheckBoxTreeItem<String>) child).isSelected() ? 'x' : ' ').append("] ")
                        .append(child.getValue()).append(System.lineSeparator());
        }

        return builder.toString();
    }

    /**
     * Gets the text of the warnings pane.
     * @return warningsText
     */
    public String getWarningsText() {
        return this.warningsTextArea.getText();
    }

    /**
     * Gets the text of the status line under the output path.
     * @return statusText
     */
    public String getStatusText() {
        return this.statusLabel.getText();
    }

    /**
     * Tests whether the package can be exported with the current settings.
     * @return exportAllowed
     */
    public boolean isExportAllowed() {
        return getExportBlocker() == null;
    }

    /**
     * Compares the open game with the selected base and fills the change list, the level table and the warnings.
     * Nothing is written.
     * @return true iff the comparison ran
     */
    public boolean analyse() {
        if (!isBaseReady()) {
            this.analyseStatusLabel.setText("The selected base is not available.");
            return false;
        }

        FroggerBaseDifference difference;
        try {
            difference = getBaseFingerprint().compare(getGameInstance(), null);
        } catch (Throwable th) {
            this.analyseStatusLabel.setText("The comparison failed: " + th);
            handleError(th, true, "Failed to compare the game with its base.");
            return false;
        }

        Map<String, Boolean> previousStates = new HashMap<>();
        for (FroggerChangeItem item : this.changeItems)
            previousStates.put(item.section + "\n" + item.key, item.treeItem.isSelected());

        fillChangeList(difference, previousStates);
        fillLevelTable(difference);
        ModExportResult result = runExport();
        if (result == null)
            return false;

        showResult(result);
        int changeCount = this.changeItems.size();
        this.analyseStatusLabel.setText(changeCount + " change(s) detected against " + describeBase() + ", "
                + result.getModPackage().getEntryList().size() + " content file(s) would be packaged, " + result.getProblems().size() + " problem(s).");
        updateControls();
        return true;
    }

    /**
     * Exports the package with the current settings to the output path, and export.log beside it.
     * @return true iff the package was written
     */
    public boolean exportPackage() {
        if (this.lastResult == null && !analyse())
            return false;

        String blocker = getExportBlocker();
        if (blocker != null) {
            this.statusLabel.setText(blocker);
            return false;
        }

        File outputFile = getOutputFile();
        ModExportResult result = runExport();
        if (result == null)
            return false;

        showResult(result);
        if (result.isEmpty()) {
            this.statusLabel.setText("Nothing differs from the base, so there is nothing to export.");
            return false;
        }

        List<ModProblem> validationErrors = new ArrayList<>();
        for (ModProblem problem : FroggerModUtils.validateExport(result))
            if (problem.isError())
                validationErrors.add(problem);
        if (!validationErrors.isEmpty()) {
            StringBuilder builder = new StringBuilder("The manifest would not validate, so nothing was written:");
            for (ModProblem problem : validationErrors)
                builder.append(System.lineSeparator()).append(" - ").append(problem);
            this.statusLabel.setText(builder.toString());
            return false;
        }

        try {
            File logFile = FroggerModUtils.writeModPackage(result, outputFile);
            this.lastExportedFile = outputFile;
            saveMetadata(outputFile);
            this.statusLabel.setText("Wrote " + outputFile.getName() + " (" + outputFile.length() + " bytes, " + result.getModPackage().getEntryList().size()
                    + " content file(s)) and " + logFile.getName() + "." + (result.hasErrors()
                    ? " It has " + countErrors(result.getProblems()) + " error(s), so it does not fully reproduce the open game; see the warnings." : ""));
            return true;
        } catch (Throwable th) {
            this.statusLabel.setText("Failed to write the package: " + th.getMessage());
            handleError(th, true, "Failed to write the mod package.");
            return false;
        }
    }

    private ModExportResult runExport() {
        Scene scene = this.analyseButton.getScene();
        if (scene != null)
            scene.setCursor(Cursor.WAIT);

        try {
            FroggerModExporter exporter = new FroggerModExporter(getGameInstance(), null);
            ModManifest draft = createDraft();
            ModExportResult result;
            if (this.baseMode == FroggerModExportBaseMode.ORIGINAL_FILES) {
                result = exporter.export(this.baseCopy, draft, this.options);
            } else if (this.baseMode == FroggerModExportBaseMode.SHIPPED_FINGERPRINT) {
                result = exporter.export(this.shippedFingerprint, draft, this.options);
            } else {
                result = exporter.export(getBaseFingerprint(), draft, this.options);
            }

            this.lastResult = result;
            return result;
        } catch (Throwable th) {
            this.analyseStatusLabel.setText("The comparison failed: " + th);
            handleError(th, true, "Failed to compare the game with its base.");
            return null;
        } finally {
            if (scene != null)
                scene.setCursor(Cursor.DEFAULT);
        }
    }

    private FroggerBaseFingerprint getBaseFingerprint() {
        if (this.baseMode == FroggerModExportBaseMode.SHIPPED_FINGERPRINT)
            return this.shippedFingerprint;
        if (this.baseMode == FroggerModExportBaseMode.ORIGINAL_FILES) {
            if (this.baseCopyFingerprint == null)
                this.baseCopyFingerprint = FroggerBaseFingerprint.generate(this.baseCopy, null);
            return this.baseCopyFingerprint;
        }

        if (this.allContentBase == null)
            this.allContentBase = FroggerModUtils.createAllContentBase(getGameInstance(), null);
        return this.allContentBase;
    }

    private ModManifest createDraft() {
        Config section = new Config(ModFormatConstants.SECTION_MOD);
        ModManifest.setString(section, KEY_NAME, getText(this.nameField));
        ModManifest.setString(section, KEY_VERSION, getText(this.versionField));
        ModManifest.setString(section, KEY_AUTHOR, getText(this.authorField));
        ModManifest.setString(section, KEY_ID, getText(this.idField));
        ModManifest.setString(section, KEY_LICENCE, getText(this.licenceField));
        ModManifest.setString(section, KEY_HOMEPAGE, getText(this.homepageField));
        ModManifest draft = FroggerModUtils.createDraftManifest(section);
        draft.setDescription(getText(this.descriptionTextArea));
        if (getText(this.idField) == null)
            this.idField.setText(draft.getId());
        return draft;
    }

    private void fillChangeList(FroggerBaseDifference difference, Map<String, Boolean> previousStates) {
        this.changeItems.clear();
        CheckBoxTreeItem<String> root = new CheckBoxTreeItem<>("Changes");
        addSection(root, difference.getFiles(), previousStates);
        addSection(root, difference.getWadChildren(), previousStates);
        addSection(root, difference.getImages(), previousStates);
        addSection(root, difference.getLevelSelect(), previousStates);
        addSection(root, difference.getRaceSelect(), previousStates);
        addSection(root, difference.getDemos(), previousStates);
        addSection(root, difference.getThemes(), previousStates);
        addSection(root, difference.getRemaps(), previousStates);
        addSection(root, difference.getOwnedRegions(), previousStates);
        addSection(root, difference.getPatchSites(), previousStates);

        List<FroggerExeBlockChange> unexplained = difference.getUnexplainedExeBlocks();
        if (!unexplained.isEmpty()) {
            CheckBoxTreeItem<String> sectionItem = new CheckBoxTreeItem<>(SECTION_EXE_BLOCKS + ": " + unexplained.size() + " unexplained executable block(s)");
            sectionItem.setExpanded(true);
            for (FroggerExeBlockChange change : unexplained)
                addChange(sectionItem, SECTION_EXE_BLOCKS, formatRange(change), change.toString(), previousStates);
            root.getChildren().add(sectionItem);
        }

        this.changeTreeView.setRoot(root);
        Set<String> listed = new HashSet<>();
        for (FroggerChangeItem item : this.changeItems)
            listed.add(item.section + "\n" + item.key);
        for (Map.Entry<String, Set<String>> entry : this.options.getExcludedChanges().entrySet())
            for (String key : entry.getValue())
                if (!listed.contains(entry.getKey() + "\n" + key))
                    this.options.includeChange(entry.getKey(), key);
    }

    private void addSection(CheckBoxTreeItem<String> root, FroggerBaseSectionDifference section, Map<String, Boolean> previousStates) {
        if (section.isEmpty())
            return;

        CheckBoxTreeItem<String> sectionItem = new CheckBoxTreeItem<>(section.getSectionName() + ": " + section.getChanged().size() + " changed, "
                + section.getRemoved().size() + " removed, " + section.getAdded().size() + " added (" + section.getItemName() + ")");
        sectionItem.setExpanded(section.getAllKeys().size() <= 40);
        for (String key : section.getChanged())
            addChange(sectionItem, section.getSectionName(), key, "changed " + key, previousStates);
        for (String key : section.getRemoved())
            addChange(sectionItem, section.getSectionName(), key, "removed " + key, previousStates);
        for (String key : section.getAdded())
            addChange(sectionItem, section.getSectionName(), key, "added " + key, previousStates);
        root.getChildren().add(sectionItem);
    }

    private void addChange(CheckBoxTreeItem<String> sectionItem, String section, String key, String text, Map<String, Boolean> previousStates) {
        CheckBoxTreeItem<String> item = new CheckBoxTreeItem<>(text);
        Boolean previous = previousStates.get(section + "\n" + key);
        boolean selected = previous != null ? previous : !this.options.isExcluded(section, key);
        item.setSelected(selected);
        if (selected) {
            this.options.includeChange(section, key);
        } else {
            this.options.excludeChange(section, key);
        }

        item.selectedProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue) {
                this.options.includeChange(section, key);
            } else {
                this.options.excludeChange(section, key);
            }
            updateControls();
        });

        sectionItem.getChildren().add(item);
        this.changeItems.add(new FroggerChangeItem(section, key, item));
    }

    private void fillLevelTable(FroggerBaseDifference difference) {
        Map<String, String> previousNames = new HashMap<>();
        for (Map.Entry<String, TextField> entry : this.levelNameFields.entrySet())
            previousNames.put(entry.getKey(), entry.getValue().getText());

        Config savedNames = getSavedConfig().getChildConfigByName(CONFIG_LEVEL_NAMES);
        Set<String> changedRows = new HashSet<>(difference.getLevelSelect().getAllKeys());
        FroggerModNameResolver resolver = new FroggerModNameResolver(getGameInstance());
        this.levelNameFields.clear();
        this.levelGridPane.getChildren().clear();
        this.levelGridPane.add(new Label("Level"), 0, 0);
        this.levelGridPane.add(new Label("Name image"), 1, 0);
        this.levelGridPane.add(new Label("Row"), 2, 0);
        this.levelGridPane.add(new Label("Name"), 3, 0);

        int row = 1;
        for (FroggerLevelSelectEntry entry : getGameInstance().getArcadeLevelSelectEntries()) {
            FroggerMapLevelID level = entry.isTerminator() ? null : entry.getLevel();
            if (level == null || this.levelNameFields.containsKey(level.getInternalName()))
                continue;

            String code = level.getInternalName();
            String nameImage = resolver.imageNameForPointer(entry.getLevelNameTexturePointer());
            TextField nameField = new TextField();
            nameField.setPromptText("No name");
            nameField.setPrefColumnCount(20);
            nameField.textProperty().addListener((observable, oldValue, newValue) -> this.options.setLevelName(code, newValue));

            String name = previousNames.get(code);
            if (name == null && savedNames != null)
                name = ModManifest.getString(savedNames, code);
            nameField.setText(name != null ? name : "");
            this.options.setLevelName(code, nameField.getText());

            this.levelGridPane.add(new Label(code), 0, row);
            this.levelGridPane.add(new Label(nameImage != null ? nameImage : "unknown"), 1, row);
            this.levelGridPane.add(new Label(changedRows.contains(code) ? "changed" : "kept"), 2, row);
            this.levelGridPane.add(nameField, 3, row);
            this.levelNameFields.put(code, nameField);
            row++;
        }
    }

    private void showResult(ModExportResult result) {
        FroggerBaseDifference difference = result.getDifference();
        StringBuilder builder = new StringBuilder();
        List<ModProblem> problems = result.getProblems();
        builder.append("Problems (").append(problems.size()).append(", ").append(countErrors(problems)).append(" error(s)):").append(System.lineSeparator());
        for (ModProblem problem : problems)
            builder.append(" - ").append(problem).append(System.lineSeparator());

        List<FroggerExeBlockChange> unexplained = difference.getUnexplainedExeBlocks();
        builder.append("Unexplained executable blocks: ").append(unexplained.size()).append(System.lineSeparator());
        for (FroggerExeBlockChange change : unexplained)
            builder.append(" - ").append(change).append(this.options.isExcluded(SECTION_EXE_BLOCKS, formatRange(change)) ? " (left out)" : "")
                    .append(System.lineSeparator());
        if (!difference.getOwnedRegions().isEmpty()) {
            builder.append("FrogLord-owned executable regions which differ (rewritten on every save, not packaged):").append(System.lineSeparator());
            for (String key : difference.getOwnedRegions().getChanged())
                builder.append(" - ").append(key).append(System.lineSeparator());
        }
        if (!difference.getDummiedWadChildren().isEmpty())
            builder.append("Dummied WAD children (unused models, not changes): ").append(difference.getDummiedWadChildren().size()).append(System.lineSeparator());
        if (difference.isBuildMismatch())
            builder.append("The base is ").append(difference.getBaseBuild()).append(", but the open game is ").append(difference.getCurrentBuild()).append('.').append(System.lineSeparator());

        this.warningsTextArea.setText(builder.toString());
    }

    private String getExportBlocker() {
        if (this.loadingBase)
            return "The original files are still being opened.";
        if (!isBaseReady())
            return "The selected base is not available.";
        if (this.lastResult == null)
            return "Analyse first.";
        if (getOutputFile() == null)
            return "Choose the output file.";
        if (getText(this.nameField) == null || getText(this.versionField) == null || getText(this.authorField) == null)
            return "Fill in the name, version and author.";

        int pendingBlocks = 0;
        for (FroggerChangeItem item : this.changeItems)
            if (SECTION_EXE_BLOCKS.equals(item.section) && item.treeItem.isSelected())
                pendingBlocks++;
        if (pendingBlocks > 0 && !this.rawWritesCheckBox.isSelected())
            return pendingBlocks + " unexplained executable block(s) are ticked: untick them to leave them out, or package them as raw writes"
                    + (this.baseMode == FroggerModExportBaseMode.ORIGINAL_FILES ? "." : ", which needs the original files.");

        return null;
    }

    private File getOutputFile() {
        String path = getText(this.outputPathField);
        if (path == null)
            return null;

        File file = new File(path);
        if (!file.getName().toLowerCase(Locale.ROOT).endsWith(ModFormatConstants.EXTENSION))
            file = new File(file.getPath() + ModFormatConstants.EXTENSION);
        return file;
    }

    private void browseForOutput() {
        String suggestedName = getText(this.nameField);
        File outputFile = FileUtils.askUserToSaveFile(getGameInstance(), MOD_PACKAGE_PATH,
                (suggestedName != null ? suggestedName.replaceAll("[^A-Za-z0-9._-]+", "-") : "mod") + ModFormatConstants.EXTENSION);
        if (outputFile != null)
            setOutputFile(outputFile);
        updateControls();
    }

    private void onBaseModeSelected(FroggerModExportBaseMode mode) {
        if (this.updatingBaseMode || mode == null)
            return;

        if (mode == FroggerModExportBaseMode.ORIGINAL_FILES && this.baseCopy == null) {
            chooseOriginalFiles();
            return;
        }

        setBaseModeInternal(mode);
    }

    private void chooseOriginalFiles() {
        File executable = FileUtils.askUserToOpenFile(getGameInstance(), ORIGINAL_EXECUTABLE_PATH);
        if (executable == null) {
            setBaseModeInternal(this.baseMode);
            return;
        }

        this.loadingBase = true;
        this.baseProgressIndicator.setVisible(true);
        this.baseStatusLabel.setText("Opening " + executable.getName() + " and its MWD...");
        updateControls();
        FroggerGameInstance game = getGameInstance();
        Thread thread = new Thread(() -> {
            FroggerGameInstance loaded = null;
            Throwable error = null;
            try {
                loaded = FroggerModUtils.loadBaseCopy(game, executable);
            } catch (Throwable th) {
                error = th;
            }

            FroggerGameInstance finalLoaded = loaded;
            Throwable finalError = error;
            Platform.runLater(() -> {
                this.loadingBase = false;
                this.baseProgressIndicator.setVisible(false);
                if (finalLoaded != null) {
                    acceptBaseCopy(finalLoaded, executable);
                    setBaseModeInternal(FroggerModExportBaseMode.ORIGINAL_FILES);
                } else {
                    setBaseModeInternal(this.baseMode);
                    this.baseStatusLabel.setText("Could not open the original files: " + (finalError != null ? finalError.getMessage() : "unknown error"));
                }
            });
        }, "FrogLord mod export base loader");
        thread.setDaemon(true);
        thread.start();
    }

    private void acceptBaseCopy(FroggerGameInstance copy, File executable) {
        this.baseCopy = copy;
        this.baseCopyFingerprint = null;
        this.baseCopyExecutable = executable;
    }

    private void setBaseModeInternal(FroggerModExportBaseMode mode) {
        if (mode == FroggerModExportBaseMode.SHIPPED_FINGERPRINT && this.shippedFingerprint == null)
            mode = this.baseCopy != null ? FroggerModExportBaseMode.ORIGINAL_FILES : FroggerModExportBaseMode.NONE;
        if (mode == FroggerModExportBaseMode.ORIGINAL_FILES && this.baseCopy == null)
            mode = this.shippedFingerprint != null ? FroggerModExportBaseMode.SHIPPED_FINGERPRINT : FroggerModExportBaseMode.NONE;

        boolean changed = mode != this.baseMode;
        this.baseMode = mode;
        this.updatingBaseMode = true;
        try {
            this.baseModeComboBox.setValue(mode);
        } finally {
            this.updatingBaseMode = false;
        }

        if (!this.loadingBase)
            this.baseStatusLabel.setText(describeBaseStatus());
        if (mode != FroggerModExportBaseMode.ORIGINAL_FILES)
            this.rawWritesCheckBox.setSelected(false);
        if (changed)
            clearAnalysis();
        updateControls();
    }

    private String describeBaseStatus() {
        String buildName = getGameInstance().getVersionConfig().getInternalName();
        switch (this.baseMode) {
            case SHIPPED_FINGERPRINT:
                return "The fingerprint FrogLord ships for " + buildName + ".";
            case ORIGINAL_FILES:
                return "The original files at " + this.baseCopyExecutable + " (" + this.baseCopy.getMwdFile().getName() + ").";
            default:
                return "FrogLord has no fingerprint for " + buildName + (this.shippedFingerprint != null ? "" : " and no original files were chosen")
                        + ": every map, model, image, row, demo, theme and remap is exported.";
        }
    }

    private String describeBase() {
        switch (this.baseMode) {
            case SHIPPED_FINGERPRINT:
                return "the shipped fingerprint";
            case ORIGINAL_FILES:
                return "the original files";
            default:
                return "no base";
        }
    }

    private boolean isBaseReady() {
        if (this.loadingBase || this.baseMode == null)
            return false;
        if (this.baseMode == FroggerModExportBaseMode.SHIPPED_FINGERPRINT)
            return this.shippedFingerprint != null;
        if (this.baseMode == FroggerModExportBaseMode.ORIGINAL_FILES)
            return this.baseCopy != null;
        return true;
    }

    private void clearAnalysis() {
        this.lastResult = null;
        this.changeItems.clear();
        this.changeTreeView.setRoot(new CheckBoxTreeItem<>("Changes"));
        this.warningsTextArea.setText("");
        this.analyseStatusLabel.setText("Press Analyse to compare the open game with " + (this.baseMode != null ? describeBase() : "its base") + ".");
    }

    private void updateControls() {
        boolean busy = this.loadingBase;
        this.baseModeComboBox.setDisable(busy);
        this.chooseOriginalButton.setDisable(busy);
        this.analyseButton.setDisable(busy || !isBaseReady());
        this.rawWritesCheckBox.setDisable(busy || this.baseMode != FroggerModExportBaseMode.ORIGINAL_FILES);
        this.exportButton.setDisable(busy || !isBaseReady());
        String blocker = getExportBlocker();
        this.statusLabel.setText(blocker != null && this.lastResult != null ? blocker : null);
    }

    private Config getSavedConfig() {
        return getGameInstance().getUserConfig().getOrCreateChildConfigByName(CONFIG_SECTION);
    }

    private void loadSavedMetadata() {
        Config saved = getSavedConfig();
        this.nameField.setText(ModManifest.getString(saved, KEY_NAME));
        this.versionField.setText(ModManifest.getString(saved, KEY_VERSION));
        this.authorField.setText(ModManifest.getString(saved, KEY_AUTHOR));
        String id = ModManifest.getString(saved, KEY_ID);
        this.idField.setText(id != null ? id : UUID.randomUUID().toString());
        this.licenceField.setText(ModManifest.getString(saved, KEY_LICENCE));
        this.homepageField.setText(ModManifest.getString(saved, KEY_HOMEPAGE));
        List<String> description = ModManifest.getTextLines(saved);
        this.descriptionTextArea.setText(description.isEmpty() ? "" : String.join("\n", description));
        this.outputPathField.setText(ModManifest.getString(saved, KEY_OUTPUT));
    }

    private void saveMetadata(File outputFile) {
        Config saved = getSavedConfig();
        ModManifest.setString(saved, KEY_NAME, getText(this.nameField));
        ModManifest.setString(saved, KEY_VERSION, getText(this.versionField));
        ModManifest.setString(saved, KEY_AUTHOR, getText(this.authorField));
        ModManifest.setString(saved, KEY_ID, getText(this.idField));
        ModManifest.setString(saved, KEY_LICENCE, getText(this.licenceField));
        ModManifest.setString(saved, KEY_HOMEPAGE, getText(this.homepageField));
        ModManifest.setString(saved, KEY_OUTPUT, outputFile.getPath());
        String description = getText(this.descriptionTextArea);
        ModManifest.setTextLines(saved, description != null ? Arrays.asList(description.replace("\r\n", "\n").split("\n", -1)) : Collections.<String>emptyList());

        Config levelNames = saved.getOrCreateChildConfigByName(CONFIG_LEVEL_NAMES);
        for (Map.Entry<String, TextField> entry : this.levelNameFields.entrySet())
            ModManifest.setString(levelNames, entry.getKey(), getText(entry.getValue()));
    }

    private static String getText(TextInputControl control) {
        String text = control.getText();
        return text != null && !text.trim().isEmpty() ? text.trim() : null;
    }

    private static String formatRange(FroggerExeBlockChange change) {
        return String.format("0x%08X+%d", change.getBlock().getAddress(), change.getBlock().getLength());
    }

    private static int countErrors(List<ModProblem> problems) {
        int count = 0;
        for (ModProblem problem : problems)
            if (problem.isError())
                count++;
        return count;
    }

    private static class FroggerChangeItem {
        private final String section;
        private final String key;
        private final CheckBoxTreeItem<String> treeItem;

        private FroggerChangeItem(String section, String key, CheckBoxTreeItem<String> treeItem) {
            this.section = section;
            this.key = key;
            this.treeItem = treeItem;
        }
    }

    /**
     * Opens the Export Mod Package window.
     * @param instance the open game to export
     * @return the window's controller, or null if the window could not be opened
     */
    public static FroggerModExportController openMenu(FroggerGameInstance instance) {
        FroggerModExportController controller = new FroggerModExportController(instance);
        return FXUtils.createWindowFromFXMLTemplate(WINDOW_TEMPLATE, controller, WINDOW_TITLE, false) ? controller : null;
    }
}
