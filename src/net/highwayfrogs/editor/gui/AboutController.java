package net.highwayfrogs.editor.gui;

import com.sun.javafx.runtime.VersionInfo;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.stage.Stage;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.games.generic.GameInstance;
import net.highwayfrogs.editor.utils.DataSizeUnit;
import net.highwayfrogs.editor.utils.Utils;

/**
 * Controls the about menu.
 * Created by Kneesnap on 1/10/2020.
 */
public class AboutController extends GameUIController<GameInstance> {
    @FXML private Label versionLabel;
    @FXML private Label javaVersionLabel;
    @FXML private Label javaRuntimeName;
    @FXML private Label memoryLabel;

    public AboutController(GameInstance instance) {
        super(instance);
    }

    @Override
    protected void onControllerLoad(Node rootNode) {
        versionLabel.setText("FrogLord " + Constants.VERSION);
        javaVersionLabel.setText("Java Version: " + System.getProperty("java.runtime.version") + ", JavaFX: " + VersionInfo.getVersion());
        javaRuntimeName.setText("Java Runtime: " + System.getProperty("java.runtime.name"));
        memoryLabel.setText("Memory Info: " + DataSizeUnit.formatSize(Runtime.getRuntime().totalMemory()) + ", Total: " + DataSizeUnit.formatSize(Runtime.getRuntime().maxMemory()) + " Max");
    }

    @Override
    public void onSceneAdd(Scene newScene) {
        super.onSceneAdd(newScene);
        Stage stage = (Stage) newScene.getWindow();
        Utils.closeOnEscapeKey(stage, null);
    }

    @FXML
    private void onDone(ActionEvent evt) {
        closeWindow();
    }

    /**
     * Opens the about menu.
     */
    public static void openAboutMenu(GameInstance instance) {
        Utils.createWindowFromFXMLTemplate("window-about", new AboutController(instance), "About FrogLord", true);
    }

    @FXML
    private void openWebsite(ActionEvent event) {
        GUIMain.getApplication().getHostServices().showDocument(Constants.HIGHWAY_FROGS_WEBSITE_URL);
    }

    @FXML
    private void openSourceCode(ActionEvent event) {
        GUIMain.getApplication().getHostServices().showDocument(Constants.SOURCE_CODE_REPOSITORY_URL);
    }
}