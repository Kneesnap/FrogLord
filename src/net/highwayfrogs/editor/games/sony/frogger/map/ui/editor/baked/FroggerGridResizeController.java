package net.highwayfrogs.editor.games.sony.frogger.map.ui.editor.baked;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.TextField;
import net.highwayfrogs.editor.games.sony.frogger.FroggerGameInstance;
import net.highwayfrogs.editor.gui.GameUIController;
import net.highwayfrogs.editor.utils.FXUtils;
import net.highwayfrogs.editor.utils.NumberUtils;

/**
 * Manages the grid resize UI.
 * Created by Kneesnap on 8/12/2019.
 */
public class FroggerGridResizeController extends GameUIController<FroggerGameInstance> {
    private final FroggerUIGridManager gridController;
    @FXML private TextField xInput;
    @FXML private TextField zInput;

    public FroggerGridResizeController(FroggerUIGridManager gridController) {
        super(gridController.getGameInstance());
        this.gridController = gridController;
    }

    @Override
    protected void onControllerLoad(Node rootNode) {
        this.xInput.setText(String.valueOf(this.gridController.getMapFile().getGridPacket().getGridXCount()));
        this.zInput.setText(String.valueOf(this.gridController.getMapFile().getGridPacket().getGridZCount()));
    }

    @FXML
    private void onAccept(ActionEvent event) {
        String xText = this.xInput.getText();
        String zText = this.zInput.getText();

        if (!NumberUtils.isInteger(xText) || zText.startsWith("-")) {
            FXUtils.makePopUp("'" + xText + "' is not a valid positive number!", AlertType.ERROR);
            return;
        }

        if (!NumberUtils.isInteger(zText) || zText.startsWith("-")) {
            FXUtils.makePopUp("'" + zText + "' is not a valid positive number!", AlertType.ERROR);
            return;
        }

        int newX = Integer.parseInt(xText);
        int newZ = Integer.parseInt(zText);
        if (newX > 255 || newZ > 255) { // Engine limitation, the game sometimes does things like (x & 0xFF), which effectively means it can't go higher than 0xFF.
            FXUtils.makePopUp("The grid cannot go larger than 255 squares in either direction.", AlertType.ERROR);
            return;
        }

        if (newX < 3 || newZ < 3) { // FrogLord limitation, the camera region editor breaks under 3x3.
            FXUtils.makePopUp("The grid must have at least 3 squares in each direction.", AlertType.ERROR);
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
    public static void open(FroggerUIGridManager gridController) {
        FXUtils.createWindowFromFXMLTemplate("window-map-resize-grid", new FroggerGridResizeController(gridController), "Grid Resize", true);
    }
}