package net.highwayfrogs.editor.gui.editor;

import javafx.collections.FXCollections;
import javafx.embed.swing.SwingFXUtils;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.util.StringConverter;
import lombok.Getter;
import net.highwayfrogs.editor.Utils;
import net.highwayfrogs.editor.file.map.MAPFile;
import net.highwayfrogs.editor.file.map.grid.GridSquare;
import net.highwayfrogs.editor.file.map.grid.GridSquareFlag;
import net.highwayfrogs.editor.file.map.grid.GridStack;
import net.highwayfrogs.editor.file.map.view.TextureMap;
import net.highwayfrogs.editor.file.map.view.TextureMap.TextureEntry;

import java.net.URL;
import java.util.LinkedList;
import java.util.List;
import java.util.ResourceBundle;

/**
 * Manages the grid editor gui.
 * TODO: ADD LAYER.
 * Created by Kneesnap on 1/24/2019.
 */
@Getter
public class GridController implements Initializable {
    @FXML private Canvas gridCanvas;
    private GraphicsContext graphics;

    @FXML private ImageView selectedImage;
    @FXML private ComboBox<Integer> layerSelector;
    @FXML private GridPane flagTable;

    private Stage stage;
    private MapUIController controller;
    private MAPFile map;

    private GridStack selectedStack;
    private int selectedLayer;
    private double tileWidth;
    private double tileHeight;

    private GridController(Stage stage, MapUIController controller, MAPFile map) {
        this.stage = stage;
        this.controller = controller;
        this.map = map;
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        layerSelector.valueProperty().addListener(((observable, oldValue, newValue) -> {
            if (newValue != null)
                setSelectedSquare(getSelectedStack(), newValue);
        }));

        layerSelector.setConverter(new StringConverter<Integer>() {
            @Override
            public String toString(Integer id) {
                return "Layer #" + (id + 1);
            }

            @Override
            public Integer fromString(String string) {
                return null;
            }
        });

        gridCanvas.setOnMousePressed(evt -> {
            int gridX = (int) (evt.getSceneX() / getTileWidth());
            int gridZ = getMap().getGridZCount() - (int) (evt.getSceneY() / getTileHeight()) - 1;
            setSelectedStack(getMap().getGridStack(gridX, gridZ));
        });

        graphics = gridCanvas.getGraphicsContext2D();
        updateCanvas();
        setSelectedStack(null);
    }

    private void updateCanvas() {
        this.tileWidth = gridCanvas.getWidth() / getMap().getGridXCount();
        this.tileHeight = gridCanvas.getHeight() / getMap().getGridZCount();

        graphics.clearRect(0, 0, gridCanvas.getWidth(), gridCanvas.getHeight());

        TextureMap texMap = getController().getMesh().getTextureMap();
        Image fxTextureImage = SwingFXUtils.toFXImage(texMap.getImage(), null);

        graphics.setFill(Color.GRAY);
        for (int z = 0; z < getMap().getGridZCount(); z++) {
            for (int x = 0; x < getMap().getGridXCount(); x++) {
                GridStack stack = getMap().getGridStack(x, z);

                double xPos = getTileWidth() * x;
                double yPos = getTileHeight() * (getMap().getGridZCount() - z - 1);

                if (stack.getSquareCount() > 0) {
                    GridSquare square = getMap().getGridSquares().get(stack.getIndex());
                    TextureEntry entry = square.getPolygon().getEntry(texMap);
                    graphics.drawImage(fxTextureImage, entry.getX(texMap), entry.getY(texMap), entry.getWidth(texMap), entry.getHeight(texMap), xPos, yPos, getTileWidth(), getTileHeight());
                } else {
                    graphics.fillRect(xPos, yPos, getTileWidth(), getTileHeight());
                }
            }
        }

        graphics.setStroke(Color.BLACK);
        for (int x = 0; x <= getMap().getGridXCount(); x++)
            graphics.strokeLine(x * getTileWidth(), 0, x * getTileWidth(), gridCanvas.getHeight());

        for (int z = 0; z <= getMap().getGridZCount(); z++)
            graphics.strokeLine(0, z * getTileHeight(), gridCanvas.getWidth(), z * getTileHeight());
    }

    @FXML
    private void choosePolygon(ActionEvent evt) {
        //TODO
    }

    @FXML
    private void removeLayer(ActionEvent evt) {
        //TODO
    }

    public void setSelectedSquare(GridStack stack, int layer) {
        this.selectedLayer = layer;

        TextureMap texMap = getController().getMesh().getTextureMap();
        int id = stack.getIndex() + layer;
        if (id == getMap().getGridSquares().size()) { //TODO
            System.out.println("There is no square for this stack yet.");
            return;
        }

        GridSquare square = getMap().getGridSquares().get(id);
        TextureEntry entry = square.getPolygon().getEntry(texMap);

        selectedImage.setImage(SwingFXUtils.toFXImage(entry.getImage(texMap), null));

        flagTable.getChildren().clear();

        int x = 0;
        int y = 0;
        for (GridSquareFlag flag : GridSquareFlag.values()) {
            if (flag.canSkip())
                continue;

            if (x == 3) {
                x = 0;
                y++;
            }

            CheckBox checkBox = new CheckBox(flag.name());
            GridPane.setRowIndex(checkBox, y);
            GridPane.setColumnIndex(checkBox, x++);
            checkBox.setSelected(square.testFlag(flag));
            checkBox.selectedProperty().addListener(((observable, oldValue, newValue) -> square.setFlag(flag, newValue)));
            flagTable.getChildren().add(checkBox);
        }
    }

    /**
     * Select the stack currently being edited.
     * @param stack The stack to select.
     */
    public void setSelectedStack(GridStack stack) {
        this.selectedStack = stack;

        if (stack != null) {
            List<Integer> layers = new LinkedList<>();
            for (int i = 0; i < stack.getSquareCount(); i++)
                layers.add(i);

            layerSelector.setItems(FXCollections.observableArrayList(layers));
            layerSelector.getSelectionModel().select(0); // Automatically calls setSquare
        }

        flagTable.setVisible(stack != null);
        selectedImage.setVisible(stack != null);
        layerSelector.setVisible(stack != null && stack.getSquareCount() > 1);
    }

    /**
     * Open the padding menu for a particular image.
     * @param controller The VLO controller opening this.
     */
    public static void openGridEditor(MapUIController controller) {
        Utils.loadFXMLTemplate("grid", "Grid Editor", newStage -> new GridController(newStage, controller, controller.getMap()));
    }
}
