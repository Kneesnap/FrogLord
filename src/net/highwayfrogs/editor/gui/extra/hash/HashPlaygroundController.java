package net.highwayfrogs.editor.gui.extra.hash;

import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import net.highwayfrogs.editor.games.sony.SCGameInstance;
import net.highwayfrogs.editor.gui.GameUIController;
import net.highwayfrogs.editor.utils.Utils;

import java.io.File;
import java.util.List;

/**
 * Manages the hash playground.
 * Created by Kneesnap on 2/24/2022.
 */
public class HashPlaygroundController extends GameUIController<SCGameInstance> {
    @FXML private Label assemblerHashLabel;
    @FXML private Label fullAssemblerHashLabel;
    @FXML private Label linkerHashLabel;
    @FXML private Label fullLinkerHashLabel;
    @FXML private Label currentStringLabel;
    @FXML private TextField prefixTextField;
    @FXML private TextField suffixTextField;
    @FXML private TextField targetLinkerHashField;
    @FXML private TextField searchQueryField;
    @FXML private Label stringListLabel;
    @FXML private ListView<String> stringsListView;
    private final IHashStringGenerator stringGenerator;

    private HashPlaygroundController(SCGameInstance gameInstance) {
        super(gameInstance);
        DictionaryStringGenerator gen = new DictionaryStringGenerator(); // TODO: This is temporary.
        gen.loadDictionaryFromFile(new File("dictionary.txt"));
        this.stringGenerator = gen;
    }

    @Override
    protected void onControllerLoad(Node rootNode) {
        this.generateNewString();
        this.prefixTextField.textProperty().addListener((observable, oldValue, newValue) -> this.generateNewString());
        this.suffixTextField.textProperty().addListener((observable, oldValue, newValue) -> this.generateNewString());
        this.searchQueryField.textProperty().addListener((observable, oldValue, newValue) -> this.generateStrings(null));
        this.targetLinkerHashField.textProperty().addListener((observable, oldValue, newValue) -> this.generateStrings(null));
    }

    private void generateNewString() {
        String fullStr = "";
        if (this.prefixTextField.getText() != null && this.prefixTextField.getText().length() > 0)
            fullStr = this.prefixTextField.getText();
        if (this.suffixTextField.getText() != null && this.suffixTextField.getText().length() > 0)
            fullStr += this.suffixTextField.getText();

        this.assemblerHashLabel.setText(String.valueOf(FroggerHashUtil.getAssemblerHash(fullStr)));
        this.fullAssemblerHashLabel.setText(String.valueOf(FroggerHashUtil.getFullAssemblerHash(fullStr)));
        this.linkerHashLabel.setText(String.valueOf(FroggerHashUtil.getLinkerHash(fullStr)));
        this.fullLinkerHashLabel.setText(String.valueOf(FroggerHashUtil.getFullLinkerHash(fullStr)));
        this.currentStringLabel.setText("Input: '" + fullStr + "'");
        this.generateStrings(null);
    }

    @FXML
    private void generateStrings(ActionEvent evt) {
        if (!Utils.isInteger(this.targetLinkerHashField.getText()))
            return;

        int targetLinkerHash = Integer.parseInt(this.targetLinkerHashField.getText());
        if (targetLinkerHash < 0 || targetLinkerHash >= FroggerHashUtil.LINKER_HASH_TABLE_SIZE) {
            Utils.makePopUp("The target linker hash must be within the range [0, " + FroggerHashUtil.LINKER_HASH_TABLE_SIZE + ").", AlertType.WARNING);
            return;
        }

        String prefix = this.prefixTextField.getText();
        String suffix = this.suffixTextField.getText();
        if (prefix != null)
            targetLinkerHash = FroggerHashUtil.getLinkerHashWithoutSubstring(prefix, targetLinkerHash);
        if (suffix != null)
            targetLinkerHash = FroggerHashUtil.getLinkerHashWithoutSubstring(suffix, targetLinkerHash);

        List<String> output = this.stringGenerator.generateStrings(targetLinkerHash, this.searchQueryField.getText());
        this.stringsListView.setItems(FXCollections.observableArrayList(output));
    }


    // TODO - Feature Target (Priority):
    // - Dictionary & String List (w/Search)
    // - Load precomputed hash file.
    // - Apply nice query search & strings from precomputed dictionary.
    // - Maybe eventually a way to select an image, view valid hash range, and verify order is followed.

    // TODO: Here's how to perform a search well. This effectively doubles our target string length.
    // If the hashsum has a precomputed string table, you don't need anything fancy.
    // If the hashsum does not have a precomputed list of count maps, get all of the pairs where both of the sums do have precomputed hashes.
    // Instead of an O(n ^ 2) operation where we'll try each count map from one with one count map from the other,
    // we'll put all of the count maps into two hash maps (one for each of the sums). The key will be a countmap that describes the parts of the query which have been found.
    // Thus, for each count map, we can determine the count map which the other count map(s) matching must be.
    // This is more accurate the more query was provided.

    /**
     * Open the level info controller.
     */
    public static void openEditor(SCGameInstance gameInstance) {
        Utils.createWindowFromFXMLTemplate("window-hash-playground", new HashPlaygroundController(gameInstance), "Hash Playground", true);
    }
}