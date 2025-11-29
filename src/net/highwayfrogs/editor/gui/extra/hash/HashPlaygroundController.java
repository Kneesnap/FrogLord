package net.highwayfrogs.editor.gui.extra.hash;

import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import lombok.Getter;
import lombok.SneakyThrows;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.FrogLordApplication;
import net.highwayfrogs.editor.games.sony.SCGameInstance;
import net.highwayfrogs.editor.games.sony.shared.utils.SCImageTableGenerator;
import net.highwayfrogs.editor.games.sony.shared.utils.SCMsvcHashReverser.MsvcHashTarget;
import net.highwayfrogs.editor.gui.GameUIController;
import net.highwayfrogs.editor.gui.InputMenu;
import net.highwayfrogs.editor.gui.extra.hash.HashRange.HashRangeType;
import net.highwayfrogs.editor.utils.*;
import net.highwayfrogs.editor.utils.FileUtils.BrowserFileType;
import net.highwayfrogs.editor.utils.FileUtils.SavedFilePath;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Manages the hash playground.
 * Created by Kneesnap on 2/24/2022.
 */
public class HashPlaygroundController extends GameUIController<SCGameInstance> {
    private final DictionaryStringGenerator dictionaryGenerator = new DictionaryStringGenerator();
    private final PermutationStringGenerator characterGenerator = new PermutationStringGenerator(4, PermutationStringGenerator.ALLOWED_CHARACTERS_ALPHANUMERIC);
    private final PermutationStringGenerator hexadecimalGenerator = new PermutationStringGenerator(4, PermutationStringGenerator.ALLOWED_CHARACTERS_HEXADECIMAL);
    @FXML private Label assemblerHashLabel;
    @FXML private Label linkerHashLabel;
    @FXML private Label compilerHashLabel;
    @FXML private Label currentStringLabel;
    @FXML private TextField prefixTextField;
    @FXML private TextField suffixTextField;
    @FXML private TextField targetLinkerHashField;
    @FXML private TextField targetCompilerHashField;
    @FXML private CheckBox showPermutations;
    @FXML private TextField maxWordSizeField;
    @FXML private TextField searchFilterField;
    @FXML private Label stringListLabel;
    @FXML private ListView<String> stringsListView;
    @Getter private HashRange psyqTargetHashRange;
    @Getter private HashRange msvcTargetHashRange;
    private String lastHashTargetString = "suffix:?:?";
    private MsvcHashTarget[] hashTargets;

    private static final BrowserFileType TABLE_EXPORT_FILE_TYPE = new BrowserFileType("Exported BSS Table", "c");
    private static final SavedFilePath TABLE_EXPORT_FILE_PATH = new SavedFilePath("bssTableExport", "Please select the file to save the BSS table export as...", TABLE_EXPORT_FILE_TYPE);

    private HashPlaygroundController(SCGameInstance gameInstance) {
        super(gameInstance);
    }

    @Override
    protected void onControllerLoad(Node rootNode) {
        this.generateNewString();
        this.prefixTextField.textProperty().addListener((observable, oldValue, newValue) -> this.generateNewString());
        this.suffixTextField.textProperty().addListener((observable, oldValue, newValue) -> this.generateNewString());
        this.searchFilterField.textProperty().addListener((observable, oldValue, newValue) -> this.generateStrings(null));
        this.targetLinkerHashField.textProperty().addListener((observable, oldValue, newValue) -> this.updatePsyQTargetHashRange());
        this.targetCompilerHashField.textProperty().addListener((observable, oldValue, newValue) -> this.updateMsvcTargetHashRange());
        this.maxWordSizeField.textProperty().addListener((observable, oldValue, newValue) -> this.generateStrings(null));
        this.showPermutations.selectedProperty().addListener((observable, oldValue, newValue) -> this.generateStrings(null));

        this.dictionaryGenerator.onSetup(this);
        File dictionaryFile = new File(FrogLordApplication.getMainApplicationFolder(), "dictionary.txt");
        if (dictionaryFile.exists() && dictionaryFile.isFile()) {
            this.dictionaryGenerator.loadDictionaryFromFile(getLogger(), dictionaryFile);
        } else {
            FXUtils.showPopup(AlertType.WARNING, "Could not file 'dictionary.txt'.",
                    "No dictionary will be used for autocompletes.\n"
                    + "Any text file with one word per line can be used as dictionary.txt.");
        }

        this.characterGenerator.onSetup(this);
        this.hexadecimalGenerator.onSetup(this);
    }

    private void updatePsyQTargetHashRange() {
        try {
            String linkerRangeText = this.targetLinkerHashField.getText();
            if (StringUtils.isNullOrEmpty(linkerRangeText)) {
                this.psyqTargetHashRange = null;
            } else {
                this.psyqTargetHashRange = HashRange.parseRange(linkerRangeText, HashRangeType.PSYQ);
            }

            this.targetLinkerHashField.setStyle(null);
        } catch (Throwable th) {
            this.psyqTargetHashRange = null;
            this.targetLinkerHashField.setStyle(Constants.FX_STYLE_INVALID_TEXT);
            return;
        }

        this.generateStrings(null);
    }

    private void updateMsvcTargetHashRange() {
        try {
            String msvcRangeText = this.targetCompilerHashField.getText();
            if (StringUtils.isNullOrEmpty(msvcRangeText)) {
                this.msvcTargetHashRange = null;
            } else {
                this.msvcTargetHashRange = HashRange.parseRange(msvcRangeText, HashRangeType.MSVC);
            }

            this.targetCompilerHashField.setStyle(null);
        } catch (Throwable th) {
            this.msvcTargetHashRange = null;
            this.targetCompilerHashField.setStyle(Constants.FX_STYLE_INVALID_TEXT);
            return;
        }

        this.generateStrings(null);
    }

    private void generateNewString() {
        String fullStr = "";
        if (this.prefixTextField.getText() != null && this.prefixTextField.getText().length() > 0)
            fullStr = this.prefixTextField.getText();
        if (this.suffixTextField.getText() != null && this.suffixTextField.getText().length() > 0)
            fullStr += this.suffixTextField.getText();

        this.assemblerHashLabel.setText(String.valueOf(FroggerHashUtil.getPsyQAssemblerHash(fullStr)));
        this.linkerHashLabel.setText(String.valueOf(FroggerHashUtil.getPsyQLinkerHash(fullStr)));
        this.compilerHashLabel.setText(String.valueOf(FroggerHashUtil.getMsvcC1HashTableKey(fullStr)));
        this.currentStringLabel.setText("Input: '" + fullStr + "'");
        this.generateStrings(null);
    }

    @FXML
    @SneakyThrows
    private void exportTable(ActionEvent evt) {
        File outputFile = FileUtils.askUserToSaveFile(getGameInstance(), TABLE_EXPORT_FILE_PATH, "bss-export.c", false);
        if (outputFile != null)
            Files.write(outputFile.toPath(), SCImageTableGenerator.saveImageOrderingTable(getGameInstance(), null));
    }

    @FXML
    private void askUserForNewTemplate(ActionEvent event) {
        String newHashTargetString = InputMenu.promptInput(getGameInstance(), "", this.lastHashTargetString);

        try {
            if (StringUtils.isNullOrWhiteSpace(newHashTargetString)) {
                this.hashTargets = null;
            } else {
                this.hashTargets = MsvcHashTarget.parseHashTargets(newHashTargetString);
                this.lastHashTargetString = newHashTargetString;
            }
        } catch (Throwable th) {
            Utils.handleError(getLogger(), th, true, "Failed to process hash targets from '%s'.", newHashTargetString);
        }
    }

    @FXML
    private void generateStrings(ActionEvent evt) {
        String searchQuery = this.searchFilterField.getText();
        int maxWordSize = getMaxWordLength();

        List<String> output = new ArrayList<>();
        Set<String> seenAlready = new HashSet<>();
        generateStrings(output, seenAlready, this.dictionaryGenerator, maxWordSize, searchQuery);
        if (this.showPermutations.isSelected())
            generateStrings(output, seenAlready, this.characterGenerator, maxWordSize, searchQuery);
        generateStrings(output, seenAlready, this.hexadecimalGenerator, maxWordSize, searchQuery);
        this.stringsListView.setItems(FXCollections.observableArrayList(output));
    }

    private void generateStrings(List<String> results, Set<String> seenAlready, IHashStringGenerator generator, int maxWordSize, String searchQuery) {
        List<String> newStrings = generator.generateStrings(this);
        if (newStrings == null || newStrings.isEmpty())
            return;

        String prefix = getPrefix();
        for (int i = 0; i < newStrings.size(); i++) {
            String word = newStrings.get(i);
            if (maxWordSize > 0 && word.length() > maxWordSize)
                continue; // Word too long.

            if (searchQuery != null && !searchQuery.isEmpty() && !word.contains(searchQuery))
                continue; // Ensure matches search query.

            // Ensure word matches hashes.
            boolean matchedHashes = true;
            if (this.hashTargets != null) {
                String hashPrefix = prefix + word;
                for (int j = 0; j < this.hashTargets.length; j++) {
                    MsvcHashTarget hashTarget = this.hashTargets[j];
                    String hashStr = hashPrefix + hashTarget.getSuffix();
                    int psyqHash = FroggerHashUtil.getPsyQLinkerHash(hashStr);
                    int msvcHash = FroggerHashUtil.getMsvcC1HashTableKey(hashStr);

                    if (!hashTarget.getPsyqRange().isInRange(psyqHash) || !hashTarget.getMsvcRange().isInRange(msvcHash)) {
                        matchedHashes = false;
                        break;
                    }
                }
            }

            if (matchedHashes && seenAlready.add(word))
                results.add(word);
        }
    }

    /**
     * Gets the user-specified maximum word length.
     */
    public int getMaxWordLength() {
        return NumberUtils.isInteger(this.maxWordSizeField.getText()) ? Integer.parseInt(this.maxWordSizeField.getText()) : 0;
    }

    /**
     * Gets the configured search query for string generation.
     */
    public String getSearchQuery() {
        return this.searchFilterField.getText();
    }

    /**
     * Gets the configured prefix for string generation.
     */
    public String getPrefix() {
        String prefixText = this.prefixTextField != null ? this.prefixTextField.getText() : null;
        return prefixText != null ? prefixText : "";
    }

    /**
     * Gets the configured suffix for string generation.
     */
    public String getSuffix() {
        String suffixText = this.suffixTextField != null ? this.suffixTextField.getText() : null;
        return suffixText != null ? suffixText : "";
    }

    /**
     * Open the level info controller.
     */
    public static void openEditor(SCGameInstance gameInstance) {
        FXUtils.createWindowFromFXMLTemplate("window-hash-playground", new HashPlaygroundController(gameInstance), "Hash Playground", false);
    }
}