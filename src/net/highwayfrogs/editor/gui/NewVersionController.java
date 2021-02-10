package net.highwayfrogs.editor.gui;

import com.sun.deploy.uitoolkit.impl.fx.HostServicesFactory;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.stage.Stage;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.gui.GUIMain.FrogLordVersion;
import net.highwayfrogs.editor.utils.Utils;

import java.net.URL;
import java.util.ResourceBundle;

/**
 * Controls the menu alerting the user of a new available version.
 * Created by Kneesnap on 1/31/2021.
 */
public class NewVersionController implements Initializable {
    private Stage stage;
    private FrogLordVersion versionInfo;
    @FXML private Label versionLabel;
    @FXML private Hyperlink downloadLink; // Should just be to the github release page.
    @FXML private Label descriptionLabel;

    public NewVersionController(Stage stage, FrogLordVersion versionInfo) {
        this.stage = stage;
        this.versionInfo = versionInfo;
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        Utils.closeOnEscapeKey(this.stage, null);

        this.downloadLink.setVisited(false);
        this.downloadLink.setOnMouseClicked(evt -> // https://stackoverflow.com/questions/16604341/how-can-i-open-the-default-system-browser-from-a-java-fx-application
                HostServicesFactory.getInstance(GUIMain.INSTANCE).showDocument(this.versionInfo.getUpdateURL()));
        this.versionLabel.setText("Version " + this.versionInfo.getVersionNumber()
                + (Constants.VERSION.equalsIgnoreCase(this.versionInfo.getVersionNumber()) ? "" : " (Current: " + Constants.VERSION + ")")); // If this happens (hiding the version) it's most likely a mistake.


        this.descriptionLabel.setWrapText(true);
        this.descriptionLabel.setText(this.versionInfo.getReleaseNotes());
    }

    @FXML
    private void onDone(ActionEvent evt) {
        this.stage.close();
    }

    /**
     * Opens the version information menu.
     */
    public static void openMenu(FrogLordVersion flVersion) {
        Utils.loadFXMLTemplate("new-version", "Version Info", stage -> new NewVersionController(stage, flVersion));
    }
}
