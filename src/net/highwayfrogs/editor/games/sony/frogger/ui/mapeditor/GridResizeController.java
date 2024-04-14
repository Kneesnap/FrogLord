package net.highwayfrogs.editor.games.sony.frogger.ui.mapeditor;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.TextField;
import net.highwayfrogs.editor.games.sony.frogger.FroggerGameInstance;
import net.highwayfrogs.editor.gui.GameUIController;
import net.highwayfrogs.editor.utils.Utils;

/**
 * Manages the grid resize UI.
 * Created by Kneesnap on 8/12/2019.
 */
public class GridResizeController extends GameUIController<FroggerGameInstance> {
    private final GridController gridController;
    @FXML private TextField xInput;
    @FXML private TextField zInput;

    public GridResizeController(GridController gridController) {
        super(gridController.getGameInstance());
        this.gridController = gridController;
    }

    @Override
    protected void onControllerLoad(Node rootNode) {
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
        if (newX > 255 || newZ > 255) { // Engine limitation, the game sometimes does things like (x & 0xFF), which effectively means it can't go higher than 0xFF.
            Utils.makePopUp("The grid cannot go larger than 255x255.", AlertType.ERROR);
            return;
        }

        this.gridController.handleResize(newX, newZ);
        closeWindow();
    }

    @FXML
    private void onCancel(ActionEvent evt) {
        closeWindow();
    }

    /**
     * Open the resize UI for a given map.
     * @param gridController The grid controller.
     */
    public static void open(GridController gridController) {
        Utils.createWindowFromFXMLTemplate("window-map-resize-grid", new GridResizeController(gridController), "Grid Resize", true);
    }
}