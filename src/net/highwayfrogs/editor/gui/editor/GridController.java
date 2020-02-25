package net.highwayfrogs.editor.gui.editor;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.GridPane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import lombok.Getter;
import net.highwayfrogs.editor.file.map.MAPFile;
import net.highwayfrogs.editor.file.map.grid.GridSquare;
import net.highwayfrogs.editor.file.map.grid.GridSquareFlag;
import net.highwayfrogs.editor.file.map.grid.GridStack;
import net.highwayfrogs.editor.file.map.poly.polygon.MAPPolygon;
import net.highwayfrogs.editor.file.map.view.MapMesh;
import net.highwayfrogs.editor.file.map.view.TextureMap;
import net.highwayfrogs.editor.file.map.view.TextureMap.TextureTreeNode;
import net.highwayfrogs.editor.file.map.zone.CameraZone;
import net.highwayfrogs.editor.file.map.zone.CameraZone.CameraZoneFlag;
import net.highwayfrogs.editor.file.map.zone.Zone;
import net.highwayfrogs.editor.file.map.zone.ZoneRegion;
import net.highwayfrogs.editor.file.map.zone.ZoneRegion.RegionEditState;
import net.highwayfrogs.editor.file.standard.SVector;
import net.highwayfrogs.editor.gui.editor.map.manager.GeometryManager;
import net.highwayfrogs.editor.gui.mesh.MeshData;
import net.highwayfrogs.editor.system.AbstractStringConverter;
import net.highwayfrogs.editor.utils.Utils;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.ResourceBundle;
import java.util.function.Consumer;

/**
 * Manages the grid editor gui.
 * TODO: Height Debugging
 * Created by Kneesnap on 1/24/2019.
 */
@Getter
public class GridController implements Initializable {
    @FXML private Canvas gridCanvas;
    private GraphicsContext graphics;

    @FXML private ImageView selectedImage;
    @FXML private ComboBox<Integer> layerSelector;
    @FXML private Button choosePolygonButton;
    @FXML private Button removeLayerButton;
    @FXML private Button addLayerButton;
    @FXML private GridPane flagTable;
    @FXML private Label stackIdLabel;
    @FXML private Label stackHeightLabel;
    @FXML private TextField stackHeightField;

    @FXML private AnchorPane stackPane;
    @FXML private ComboBox<Zone> zoneSelector;
    @FXML private ComboBox<Integer> regionSelector;
    @FXML private Button addZoneButton;
    @FXML private Button removeZoneButton;
    @FXML private Button addRegionButton;
    @FXML private Button removeRegionButton;
    @FXML private CheckBox zoneEditorCheckBox;
    @FXML private CheckBox zoneFinderCheckBox;
    @FXML private Label directionLabel;
    @FXML private TextField directionTextField;
    @FXML private GridPane cameraPane;
    @FXML private GridPane flagGrid;
    @FXML private Button hideZoneButton;

    private Stage stage;
    private GeometryManager manager;
    private MAPFile map;

    private RegionEditState editState = RegionEditState.NONE_SELECTED;
    private Zone selectedZone;
    private int selectedRegion;
    private int lastSelectionX = -1;
    private int lastSelectionZ = -1;
    private List<GridStack> selectedStacks = new ArrayList<>();
    private int selectedLayer;
    private double tileWidth;
    private double tileHeight;
    private CheckBox[] zoneFlagMap = new CheckBox[CameraZoneFlag.values().length];

    private static final int DEFAULT_REGION_ID = 0;
    private static final int DEFAULT_ZONE_ID = 0;

    private GridController(Stage stage, GeometryManager manager) {
        this.stage = stage;
        this.manager = manager;
        this.map = manager.getMap();
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        layerSelector.valueProperty().addListener(((observable, oldValue, newValue) -> {
            if (newValue != null)
                setSelectedSquares(getSelectedStacks(), newValue);
        }));

        layerSelector.setConverter(new AbstractStringConverter<>(id -> "Layer #" + (id + 1)));

        gridCanvas.setOnMousePressed(evt -> {
            int gridX = (int) (evt.getSceneX() / getTileWidth());
            int gridZ = (int) (evt.getSceneY() / getTileHeight());
            GridStack stack = getMap().getGridStack(gridX, getMap().getGridZCount() - gridZ - 1);
            if (this.selectedStacks != null && this.selectedStacks.size() > 0) {
                if (evt.isControlDown()) { // Toggle grid stacks one at a time.
                    if (!this.selectedStacks.remove(stack))
                        this.selectedStacks.add(stack);
                } else if (evt.isShiftDown()) { // Cover a range.
                    this.selectedStacks.clear();
                    int minGridX = Math.min(gridX, lastSelectionX);
                    int minGridZ = Math.min(gridZ, lastSelectionZ);
                    int maxGridX = Math.max(gridX, lastSelectionX);
                    int maxGridZ = Math.max(gridZ, lastSelectionZ);
                    for (int x = minGridX; x <= maxGridX; x++)
                        for (int z = minGridZ; z <= maxGridZ; z++)
                            this.selectedStacks.add(getMap().getGridStack(x, getMap().getGridZCount() - z - 1));

                } else {
                    this.selectedStacks.clear();
                    this.selectedStacks.add(stack);
                }
            } else {
                this.selectedStacks = new ArrayList<>();
                this.selectedStacks.add(stack);
            }

            setSelectedStacks(getSelectedStacks());

            if (!evt.isShiftDown() || lastSelectionX == -1) {
                lastSelectionX = gridX;
                lastSelectionZ = gridZ;
            }

            if (this.zoneEditorCheckBox.isSelected()) {
                gridZ = getMap().getGridZ(stack);

                if (editState == RegionEditState.NONE_SELECTED) {
                    if (getCurrentRegion() == null)
                        return;

                    for (RegionEditState state : RegionEditState.values()) {
                        if (state.getTester().apply(getCurrentRegion(), gridX, gridZ)) {
                            editState = state;
                            updateCanvas();
                            break;
                        }
                    }

                } else {
                    editState.setCoordinates(getCurrentRegion(), gridX, gridZ);
                    editState = RegionEditState.NONE_SELECTED;
                    updateCanvas();
                }

                return;
            }

            if (this.zoneFinderCheckBox.isSelected()) {
                gridZ = getMap().getGridZ(stack);

                for (Zone zone : getMap().getZones()) {
                    if (zone.contains(gridX, gridZ)) {
                        zoneSelector.valueProperty().setValue(zone);
                        zoneSelector.getSelectionModel().select(zone);

                        int index = zone.getRegions().indexOf(zone.getRegion(gridX, gridZ)) + 1;
                        if (index >= 1) {
                            regionSelector.getSelectionModel().select(index);
                            regionSelector.setValue(index);
                        }

                        this.zoneFinderCheckBox.setSelected(false);
                        return;
                    }
                }

                return;
            }

            if (evt.isSecondaryButtonDown()) { // Remove.
                stack.getGridSquares().clear();
                updateCanvas();
            }
        });

        for (int i = 0; i < CameraZoneFlag.values().length; i++) {
            CameraZoneFlag flag = CameraZoneFlag.values()[i];
            int row = (i / 2);
            int column = (i % 2);

            CheckBox newBox = new CheckBox(flag.getDisplayName());
            GridPane.setRowIndex(newBox, row);
            GridPane.setColumnIndex(newBox, column);
            flagGrid.getChildren().add(newBox);
            zoneFlagMap[i] = newBox;

            newBox.selectedProperty().addListener(((observable, oldValue, newValue) -> getSelectedZone().getCameraZone().setFlag(flag, newValue)));
        }

        Utils.setHandleKeyPress(this.directionTextField, text -> {
            if (!Utils.isSignedShort(text))
                return false;
            getSelectedZone().getCameraZone().setForceDirection(Short.parseShort(text));
            return true;
        }, null);

        zoneSelector.setItems(FXCollections.observableArrayList(getMap().getZones()));
        zoneSelector.valueProperty().addListener(((observable, oldValue, newValue) -> setSelectedZone(newValue)));
        zoneSelector.setConverter(new AbstractStringConverter<>(zone -> "Camera Zone #" + (getMap().getZones().indexOf(zone) + 1)));
        regionSelector.setConverter(new AbstractStringConverter<>(value -> value == DEFAULT_REGION_ID ? "Main Region" : "Region #" + value));
        regionSelector.valueProperty().addListener((observable, oldValue, newValue) -> setSelectedRegion(getSelectedZone(), newValue == null ? 0 : newValue));
        zoneEditorCheckBox.selectedProperty().addListener(((observable, oldValue, newValue) -> updateCanvas()));
        hideZoneButton.setOnAction(evt -> {
            zoneSelector.valueProperty().setValue(null);
            zoneSelector.getSelectionModel().select(null);
            updateCanvas();
        });

        graphics = gridCanvas.getGraphicsContext2D();
        updateCanvas();
        setSelectedStacks(null);
        setSelectedZone(null);
        setSelectedRegion(null, DEFAULT_REGION_ID);
    }

    private void updateCanvas() {
        this.tileWidth = gridCanvas.getWidth() / getMap().getGridXCount();
        this.tileHeight = gridCanvas.getHeight() / getMap().getGridZCount();

        graphics.clearRect(0, 0, gridCanvas.getWidth(), gridCanvas.getHeight());

        TextureMap texMap = getManager().getMesh().getTextureMap();
        Image fxTextureImage = Utils.toFXImage(texMap.getTextureTree().getImage(), false);

        ZoneRegion currentRegion = getCurrentRegion();
        for (int z = 0; z < getMap().getGridZCount(); z++) {
            for (int x = 0; x < getMap().getGridXCount(); x++) {
                GridStack stack = getMap().getGridStack(x, z);

                double xPos = getTileWidth() * x;
                double yPos = getTileHeight() * (getMap().getGridZCount() - z - 1);

                if (this.selectedStacks != null && this.selectedStacks.contains(stack)) {
                    graphics.setFill(Color.AQUA);
                    graphics.fillRect(xPos, yPos, getTileWidth(), getTileHeight());
                } else if (currentRegion != null && currentRegion.contains(x, z)) {
                    graphics.setFill(Color.MAGENTA);
                    if (zoneEditorCheckBox.isSelected() && currentRegion.isCorner(x, z)) {
                        graphics.setFill(Color.YELLOW);
                        if (editState.getTester().apply(currentRegion, x, z))
                            graphics.setFill(Color.RED);
                    }

                    graphics.fillRect(xPos, yPos, getTileWidth(), getTileHeight());
                } else if (stack.getGridSquares().size() > 0) {
                    GridSquare square = stack.getGridSquares().get(stack.getGridSquares().size() - 1);
                    TextureTreeNode entry = square.getPolygon().getNode(texMap);
                    graphics.drawImage(fxTextureImage, entry.getX(), entry.getY(), entry.getWidth(), entry.getHeight(), xPos, yPos, getTileWidth(), getTileHeight());
                } else {
                    graphics.setFill(Color.GRAY);
                    graphics.fillRect(xPos, yPos, getTileWidth(), getTileHeight());
                }
            }
        }

        graphics.setStroke(Color.BLACK);
        for (int x = 0; x <= getMap().getGridXCount(); x++)
            graphics.strokeLine(x * getTileWidth(), 0, x * getTileWidth(), gridCanvas.getHeight());

        for (int z = 0; z <= getMap().getGridZCount(); z++)
            graphics.strokeLine(0, z * getTileHeight(), gridCanvas.getWidth(), z * getTileHeight());

        // Draw outline of start square.
        graphics.setStroke(Color.RED);
        graphics.strokeRect(getMap().getStartXTile() * getTileWidth(), (getMap().getGridZCount() - getMap().getStartZTile() - 1) * getTileHeight(), getTileWidth(), getTileHeight());
    }

    private void selectSquare(Consumer<MAPPolygon> onSelect) {
        stage.close();

        for (GridStack stack : getMap().getGridStacks())
            for (GridSquare square : stack.getGridSquares())
                getManager().getController().renderOverPolygon(square.getPolygon(), MapMesh.GRID_COLOR);
        MeshData data = getManager().getMesh().getManager().addMesh();

        getManager().getController().getGeometryManager().selectPolygon(poly -> {
            getManager().getMesh().getManager().removeMesh(data);
            onSelect.accept(poly);
            updateCanvas();
            Platform.runLater(stage::showAndWait);
        }, () -> {
            getManager().getMesh().getManager().removeMesh(data);
            Platform.runLater(stage::showAndWait);
        });
    }

    @FXML
    private void choosePolygon(ActionEvent evt) {
        if (getSelectedStacks() == null)
            return;

        selectSquare(poly -> {
            getSelectedStacks().forEach(stack -> stack.getGridSquares().get(getSelectedLayer()).setPolygon(poly));
            setSelectedSquares(getSelectedStacks(), getSelectedLayer());
        });
    }

    @FXML
    private void selectPolygon(ActionEvent evt) {
        selectSquare(poly -> {
            int index = -1;
            List<GridStack> matched = new ArrayList<>();
            for(GridStack stack : getMap().getGridStacks()) {
                for(GridSquare square : stack.getGridSquares()) {
                    if(square.getPolygon() == poly) {
                        matched.add(stack);
                        index = stack.getGridSquares().indexOf(square);
                    }
                }
            }
            if(matched.isEmpty() || !sameGridSquareListSize(matched)) {
                setSelectedStacks(null);
                return;
            }
            setSelectedStacks(matched);
            setSelectedSquares(matched, index);
        });
    }

    @FXML
    private void addLayer(ActionEvent evt) {
        if (getSelectedStacks() == null)
            return;

        selectSquare(poly -> {
            if (!sameLayerCount(getSelectedStacks()))
                return;
            getSelectedStacks().forEach(stack ->
                    stack.getGridSquares().add(new GridSquare(poly, getMap()))
            );
            setSelectedStacks(getSelectedStacks());
            setSelectedSquares(getSelectedStacks(), getSelectedStacks().iterator().next().getGridSquares().size() - 1);
        });
    }

    @FXML
    private void removeLayer(ActionEvent evt) {
        if (getSelectedStacks() == null)
            return;
        getSelectedStacks().forEach(stack -> {
            if (stack.getGridSquares().isEmpty()) return;
            stack.getGridSquares().remove(this.selectedLayer);
        });
        setSelectedStacks(getSelectedStacks());
        updateCanvas();
    }

    @FXML
    private void onUpdateHeight(ActionEvent evt) {
        String text = stackHeightField.getText();
        if (Utils.isSignedShort(text))
            this.selectedStacks.forEach(stack -> stack.setAverageHeight(Short.parseShort(text)));    }

    @FXML
    private void addZone(ActionEvent evt) {
        Zone zone = new Zone();
        getMap().getZones().add(zone);
        this.zoneSelector.setItems(FXCollections.observableArrayList(getMap().getZones()));
        this.zoneSelector.getSelectionModel().select(zone);
    }

    @FXML
    private void deleteZone(ActionEvent evt) {
        getMap().getZones().remove(this.selectedZone);
        this.selectedZone = null;
        this.zoneSelector.setItems(FXCollections.observableArrayList(getMap().getZones()));
        this.zoneSelector.getSelectionModel().select(null);
    }

    @FXML
    private void addRegion(ActionEvent evt) {
        ZoneRegion newRegion = new ZoneRegion();
        this.selectedZone.getRegions().add(newRegion);
        int newId = this.regionSelector.getItems().size();
        this.regionSelector.getItems().add(newId);
        this.regionSelector.getSelectionModel().select(newId);
    }

    @FXML
    private void deleteRegion(ActionEvent evt) {
        getSelectedZone().getRegions().remove(this.selectedRegion - 1);
        this.regionSelector.getItems().remove(this.selectedRegion);
        for (int i = this.selectedRegion + 1; i < this.regionSelector.getItems().size(); i++)
            this.regionSelector.getItems().set(i, this.regionSelector.getItems().get(i) - 1);

        this.regionSelector.getSelectionModel().select(--this.selectedRegion);
    }

    @FXML
    private void onResizeGrid(ActionEvent evt) {
        GridResizeController.open(this);
    }

    /**
     * Select squares.
     * @param stacks The stacks the squares belong to.
     * @param layer The layer.
     */
    public void setSelectedSquares(Collection<GridStack> stacks, int layer) {
        this.selectedLayer = layer;
        TextureMap texMap = getManager().getMesh().getTextureMap();
        GridStack stack = stacks.iterator().next();
        GridSquare square = stack.getGridSquares().get(layer);
        TextureTreeNode entry = square.getPolygon().getNode(texMap);
        selectedImage.setImage(sameLayerTypes(stacks, layer) ? Utils.toFXImage(entry.getImage(), false) : null);
        int x = 1;
        int y = 0;
        flagTable.getChildren().clear();
        for(GridSquareFlag flag : GridSquareFlag.values()) {
            if(x == 2) {
                x = 0;
                y++;
            }
            CheckBox checkBox = new CheckBox(Utils.capitalize(flag.name()));
            GridPane.setRowIndex(checkBox, y);
            GridPane.setColumnIndex(checkBox, x++);
            boolean state = square.testFlag(flag);
            boolean statesMatch = stacks.stream()
                    .map(stk -> stk.getGridSquares().get(layer).testFlag(flag))
                    .allMatch(gflag -> gflag == state);
            if (statesMatch) {
                checkBox.setSelected(state);
            } else {
                checkBox.indeterminateProperty().set(true);
                checkBox.setAllowIndeterminate(false);
            }
            checkBox.selectedProperty().addListener(((observable, oldValue, newValue)
                    -> stacks.forEach(sq -> sq.getGridSquares().get(layer).setFlag(flag, newValue))));
            flagTable.getChildren().add(checkBox);
        }
    }

    /**
     * Select the stacks currently being edited.
     * @param stacks The stacks to select.
     */
    public void setSelectedStacks(List<GridStack> stacks) {
        this.selectedStacks = stacks;
        int squareCount = 0;
        boolean noStack = stacks == null || stacks.isEmpty();
        boolean differingLayerCount = false;
        if (stacks != null && !stacks.isEmpty()) {
            int min = getMinCommonSquareListSize(stacks);
            differingLayerCount = !sameLayerCount(stacks);
            if (min > 0) {
                squareCount = min;
                List<Integer> layers = Utils.getIntegerList(squareCount);
                layerSelector.setItems(FXCollections.observableArrayList(layers));
                if (layers.size() > 0) layerSelector.getSelectionModel().select(0); // Automatically calls setSquare
            }
        }
        boolean disable = (squareCount == 0);
        flagTable.setDisable(disable);
        selectedImage.setVisible(!disable);
        layerSelector.setDisable(squareCount <= 1);
        stackIdLabel.setDisable(noStack);
        stackHeightField.setDisable(noStack);
        stackHeightLabel.setDisable(noStack);
        choosePolygonButton.setDisable(disable);
        addLayerButton.setDisable(squareCount > 1);
        removeLayerButton.setDisable(disable || differingLayerCount);
        if (stacks != null && !stacks.isEmpty()) {
            GridStack stack = stacks.iterator().next();
            if (stacks.size() == 1) {
                stackIdLabel.setText("Stack ID: #" + getMap().getGridStacks().indexOf(stack) + " [X: " + getMap().getGridX(stack) + ",Z: " + getMap().getGridZ(stack) + "]");
                stackHeightField.setText(String.valueOf(stack.getAverageHeight()));
            } else {
                stackIdLabel.setText(stacks.size() + " stacks selected");
                if(sameHeight(stacks))
                    stackHeightField.setText(String.valueOf(stack.getAverageHeight()));
                else
                    stackHeightField.setText("");
            }
        }
        updateCanvas();
    }

    private static int getMinCommonSquareListSize(Collection<GridStack> stacks) {
        return stacks.stream()
                .mapToInt(stack -> stack.getGridSquares().size())
                .min().orElse(0);
    }

    private static boolean sameGridSquareListSize(Collection<GridStack> stacks) {
        return stacks.stream()
                .mapToInt(stack -> stack.getGridSquares().size())
                .distinct().count() == 1;
    }

    private static boolean sameHeight(Collection<GridStack> stacks) {
        return stacks.stream()
                .mapToInt(GridStack::getAverageHeight)
                .distinct().count() == 1;
    }

    private static boolean sameLayerCount(Collection<GridStack> stacks) {
        return stacks.stream()
                .mapToInt(stack -> stack.getGridSquares().size())
                .distinct().count() == 1;
    }

    private static boolean sameLayerTypes(Collection<GridStack> stacks, int layer) {
        return stacks.stream()
                .map(stack -> stack.getGridSquares().get(layer).getPolygon().getType())
                .distinct().count() == 1;
    }

    /**
     * Set the selected zone.
     * @param newZone The new zone to select.
     */
    public void setSelectedZone(Zone newZone) {
        this.selectedZone = newZone;

        boolean hasZone = (newZone != null);
        if (hasZone) {
            List<Integer> regionIds = new ArrayList<>(Utils.getIntegerList(newZone.getRegionCount() + 1));
            regionSelector.setItems(FXCollections.observableArrayList(regionIds));
            if (regionIds.size() > 0)
                regionSelector.getSelectionModel().select(DEFAULT_REGION_ID); // Automatically calls setRegion
        }

        removeZoneButton.setDisable(!hasZone);
        regionSelector.setDisable(!hasZone);
        addRegionButton.setDisable(!hasZone);
        flagGrid.setDisable(!hasZone);
        directionTextField.setDisable(!hasZone);
        cameraPane.setDisable(!hasZone);
        hideZoneButton.setDisable(!hasZone);
        zoneEditorCheckBox.setDisable(!hasZone);
        directionLabel.setDisable(!hasZone);

        if (hasZone) {
            CameraZone camZone = newZone.getCameraZone();
            for (CameraZoneFlag zoneFlag : CameraZoneFlag.values())
                zoneFlagMap[zoneFlag.ordinal()].setSelected(camZone.testFlag(zoneFlag));

            directionTextField.setText(Short.toString(camZone.getForceDirection()));
            setupVectorEditor(1, 1, camZone.getNorthSourceOffset());
            setupVectorEditor(2, 1, camZone.getNorthTargetOffset());
            setupVectorEditor(1, 2, camZone.getEastSourceOffset());
            setupVectorEditor(2, 2, camZone.getEastTargetOffset());
            setupVectorEditor(1, 3, camZone.getSouthSourceOffset());
            setupVectorEditor(2, 3, camZone.getSouthTargetOffset());
            setupVectorEditor(1, 4, camZone.getWestSourceOffset());
            setupVectorEditor(2, 4, camZone.getWestTargetOffset());
        }
    }

    private void setupVectorEditor(int x, int y, SVector toEdit) {
        TextField newField = new TextField(toEdit.toFloatString());
        GridPane.setRowIndex(newField, y);
        GridPane.setColumnIndex(newField, x);
        cameraPane.getChildren().add(newField);
        Utils.setHandleKeyPress(newField, toEdit::loadFromFloatText, null);
    }

    /**
     * Set the selected region.
     * @param zone The zone to select a region from.
     * @param id   The id of the new region.
     */
    public void setSelectedRegion(Zone zone, int id) {
        this.selectedRegion = id;

        boolean hasRegion = (id != DEFAULT_REGION_ID);
        removeRegionButton.setDisable(!hasRegion);
        updateCanvas();
    }

    /**
     * Get the currently selected region.
     * @return currentRegion
     */
    public ZoneRegion getCurrentRegion() {
        if (getSelectedZone() == null)
            return null;

        return this.selectedRegion == DEFAULT_REGION_ID
                ? getSelectedZone().getMainRegion()
                : getSelectedZone().getRegions().get(this.selectedRegion - 1);
    }

    /**
     * Resizes the grid and updates the UI.
     * @param newX The new x grid size.
     * @param newZ The new z grid size.
     */
    public void handleResize(int newX, int newZ) {
        getMap().resizeGrid(newX, newZ);
        // Update the selected stack.
        List<GridStack> newStacks = getMap().getGridStacks().containsAll(selectedStacks) ? selectedStacks : null;
        setSelectedStacks(newStacks);
        if (newStacks != null)
            setSelectedSquares(newStacks, this.selectedLayer);
        updateCanvas();
    }

    /**
     * Open the padding menu for a particular image.
     * @param manager The geometry manager.
     */
    public static void openGridEditor(GeometryManager manager) {
        Utils.loadFXMLTemplate("grid", "Grid Editor", newStage -> new GridController(newStage, manager));
    }
}
