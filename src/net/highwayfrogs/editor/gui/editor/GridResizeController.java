package net.highwayfrogs.editor.gui.editor;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import net.highwayfrogs.editor.utils.Utils;

import java.net.URL;
import java.util.ResourceBundle;

/**
 * Manages the grid resize UI.
 * Created by Kneesnap on 8/12/2019.
 */
public class GridResizeController implements Initializable {
    private Stage stage;
    private GridController gridController;
    @FXML private TextField xInput;
    @FXML private TextField zInput;

    public GridResizeController(Stage stage, GridController gridController) {
        this.stage = stage;
        this.gridController = gridController;
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        this.xInput.setText(String.valueOf(this.gridController.getMap().getGridXCount()));
        this.zInput.setText(String.valueOf(this.gridController.getMap().getGridZCount()));
    }

    @FXML
    private void onAccept(ActionEvent event) {
        String xText = this.xInput.getText();
        String zText = this.zInput.getText();

        if (!Utils.isInteger(xText) || zText.startsWith("-")) {
            Utils.makePopUp("'" + xText + "' is not a valid positive number!", AlertType.ERROR);
            return;
        }

        if (!Utils.isInteger(zText) || zText.startsWith("-")) {
            Utils.makePopUp("'" + zText + "' is not a valid positive number!", AlertType.ERROR);
            return;
        }

        int newX = Integer.parseInt(xText);
        int newZ = Integer.parseInt(zText);
        this.gridController.handleResize(newX, newZ);
        this.stage.close();
    }

    @FXML
    private void onCancel(ActionEvent evt) {
        this.stage.close();
    }

    /**
     * Open the resize UI for a given map.
     * @param gridController The grid controller.
     */
    public static void open(GridController gridController) {
        Utils.loadFXMLTemplate("grid-resize", "Grid Resize", newStage -> new GridResizeController(newStage, gridController));
    }
}
