package net.highwayfrogs.editor.gui;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.stage.Stage;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.utils.DataSizeUnit;
import net.highwayfrogs.editor.utils.Utils;

import java.net.URL;
import java.util.ResourceBundle;

/**
 * Controls the about menu.
 * Created by Kneesnap on 1/10/2020.
 */
public class AboutController implements Initializable {
    private Stage stage;
    @FXML private Label versionLabel;
    @FXML private Label javaVersionLabel;
    @FXML private Label javaRuntimeName;
    @FXML private Label memoryLabel;

    public AboutController(Stage stage) {
        this.stage = stage;
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        Utils.closeOnEscapeKey(this.stage, null);

        versionLabel.setText("FrogLord " + Constants.VERSION);
        javaVersionLabel.setText("Java Version: " + System.getProperty("java.runtime.version"));
        javaRuntimeName.setText("Java Runtime: " + System.getProperty("java.runtime.name"));
        memoryLabel.setText("Memory Info: " + DataSizeUnit.formatSize(Runtime.getRuntime().totalMemory()) + " Total, " + DataSizeUnit.formatSize(Runtime.getRuntime().maxMemory()) + " Max");
    }

    @FXML
    private void onDone(ActionEvent evt) {
        this.stage.close();
    }

    /**
     * Opens the about menu.
     */
    public static void openAboutMenu() {
        Utils.loadFXMLTemplate("about", "About FrogLord", AboutController::new);
    }
}