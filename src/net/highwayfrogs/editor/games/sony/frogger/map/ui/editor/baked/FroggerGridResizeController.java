package net.highwayfrogs.editor.games.sony.frogger.map.ui.editor.baked;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.TextField;
import net.highwayfrogs.editor.games.sony.frogger.FroggerGameInstance;
import net.highwayfrogs.editor.games.sony.frogger.map.packets.FroggerMapFilePacketGrid;
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
        if (!isGridSizeValid(newX, newZ))
            return;

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

    /**
     * Test if the given grid size is valid.
     * If not, handle creating popups to let the user know.
     * @param newX the new x value to test
     * @param newZ the new z value to test
     * @return isGridSizeValid
     */
    public static boolean isGridSizeValid(int newX, int newZ) {
        // We know for sure the grid can be > 128 squares, as a 94 x 152 collision grid is seen for SWP2.MAP in PSX Build 30.
        // Map groups however, are less reliable when there are that many grid squares, and much of the level may be invisible.
        // However, at Z=192, the map groups start to break a bit, and don't fully show everything.
        // 252x252 is where the game will finally crash. I suspect this is because the game wraps around with the map groups, and something fails when you see the map group.
        // Clear map works well with the following tests: 128x128, 255x20, 191x191, 255x191
        // Clear does not work well with the following tests: 255x255, 200x200, 20x200, 192x192, 255x192
        // 255x255 doesn't save because of the number of vertices when clearing the map, but in theory I think would be the same.
        // We don't allow 255x255 because odd-number grid-squares do not work.
        if (newX >= FroggerMapFilePacketGrid.MAX_GRID_SQUARE_COUNT_X || newZ >= FroggerMapFilePacketGrid.MAX_GRID_SQUARE_COUNT_Z) { // Engine limitation, the game sometimes does things like (x & 0xFF), which effectively means it can't go higher than 0xFF.
            FXUtils.makePopUp("The grid cannot go larger than 254 squares in either direction.", AlertType.ERROR);
            return false;
        }

        if (newX < 3 || newZ < 3) { // FrogLord limitation, the camera region editor breaks under 3x3.
            FXUtils.makePopUp("The grid must have at least 3 squares in each direction.", AlertType.ERROR);
            return false;
        }

        // Warn the user if the area they've created is too large to be represented in the MAP_GROUP system without issues.
        // We allow selecting the higher values instead of fully preventing them for reasons from testing to possible eventual fixing of the system to support the full range.
        if (newZ > FroggerMapFilePacketGrid.MAX_SAFE_GRID_SQUARE_COUNT_Z)
            FXUtils.makePopUp("This selection will cause rendering issues in-game. To avoid these issues, reduce the gridZCount value to no more than " + FroggerMapFilePacketGrid.MAX_SAFE_GRID_SQUARE_COUNT_Z + ".", AlertType.WARNING);

        if (newX % 2 > 0 || newZ % 2 > 0)
            FXUtils.makePopUp("The collision grid breaks when non-even lengths are used.\nThe provided size will be automatically adjusted to account for this.", AlertType.INFORMATION);

        return true;
    }
}