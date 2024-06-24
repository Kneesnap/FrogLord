package net.highwayfrogs.editor.games.sony.frogger.map.ui.editor.baked;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.paint.Color;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.file.map.view.UnknownTextureSource;
import net.highwayfrogs.editor.file.standard.SVector;
import net.highwayfrogs.editor.games.psx.shading.PSXShadeTextureDefinition;
import net.highwayfrogs.editor.games.sony.frogger.FroggerGameInstance;
import net.highwayfrogs.editor.games.sony.frogger.map.FroggerMapFile;
import net.highwayfrogs.editor.games.sony.frogger.map.data.FroggerOffsetVectorType;
import net.highwayfrogs.editor.games.sony.frogger.map.data.grid.FroggerGridSquare;
import net.highwayfrogs.editor.games.sony.frogger.map.data.grid.FroggerGridSquareFlag;
import net.highwayfrogs.editor.games.sony.frogger.map.data.grid.FroggerGridStack;
import net.highwayfrogs.editor.games.sony.frogger.map.data.zone.FroggerCameraRotation;
import net.highwayfrogs.editor.games.sony.frogger.map.data.zone.FroggerMapCameraZone;
import net.highwayfrogs.editor.games.sony.frogger.map.data.zone.FroggerMapCameraZone.FroggerMapCameraZoneFlag;
import net.highwayfrogs.editor.games.sony.frogger.map.data.zone.FroggerMapZone;
import net.highwayfrogs.editor.games.sony.frogger.map.data.zone.FroggerMapZoneRegion;
import net.highwayfrogs.editor.games.sony.frogger.map.data.zone.FroggerMapZoneRegion.RegionEditState;
import net.highwayfrogs.editor.games.sony.frogger.map.mesh.FroggerMapMeshController;
import net.highwayfrogs.editor.games.sony.frogger.map.mesh.FroggerMapPolygon;
import net.highwayfrogs.editor.games.sony.frogger.map.packets.FroggerMapFilePacketGrid;
import net.highwayfrogs.editor.gui.GameUIController;
import net.highwayfrogs.editor.gui.texture.ITextureSource;
import net.highwayfrogs.editor.gui.texture.atlas.AtlasTexture;
import net.highwayfrogs.editor.system.AbstractAttachmentCell;
import net.highwayfrogs.editor.system.AbstractIndexStringConverter;
import net.highwayfrogs.editor.system.AbstractStringConverter;
import net.highwayfrogs.editor.utils.Utils;

import java.util.*;
import java.util.function.Consumer;

/**
 * This manages a window displaying the collision grid in a baked Frogger map.
 * TODO: Fix "Forced Camera Direction" selection null selection failing.
 * TODO: Replace hide option with better selector null selection failing.
 * TODO: "Add Square" breaks the MeshView (Gotta debug mesh array management.)
 * TODO: Future, make a 3D version of the zone editor. Be able to preview everything in 3D space.
 * Created by Kneesnap on 6/6/2024.
 */
public class FroggerUIGridManager extends GameUIController<FroggerGameInstance> {
    private GraphicsContext graphics;

    // Main Controls:
    @FXML private Label collisionGridMainLabel;
    @FXML private CheckBox shadingEnabledCheckBox;
    @FXML private ComboBox<Integer> layerSelector;
    @FXML private Canvas gridCanvas;

    // Grid Stack Area:
    @FXML private Label gridStackSelectedLabel;
    @FXML private Label stackHeightLabel;
    @FXML private TextField stackHeightField;
    @FXML private Button addGridSquareButton;
    @FXML private Button removeGridSquareButton;

    // Grid Square Area:
    @FXML private Label gridSquareLabel;
    @FXML private Button changePolygonButton;
    @FXML private ImageView selectedImage;
    @FXML private Label polygonTypeLabel;
    @FXML private GridPane flagTable;

    // Zones:
    @FXML private ComboBox<FroggerMapZone> zoneSelector;
    @FXML private Button addZoneButton;
    @FXML private Button removeZoneButton;
    @FXML private CheckBox highlightZonesCheckBox;
    @FXML private Label forcedCameraDirectionLabel;
    @FXML private ComboBox<FroggerCameraRotation> forcedCameraDirectionComboBox;
    @FXML private GridPane zoneFlagGrid;
    @FXML private GridPane cameraPane;

    // Zone Regions:
    @FXML private ComboBox<Integer> regionSelector;
    @FXML private Button addRegionButton;
    @FXML private Button removeRegionButton;
    @FXML private CheckBox regionEditorCheckBox;

    private final FroggerMapMeshController mapMeshController;
    private RegionEditState editState = RegionEditState.NONE_SELECTED;
    private FroggerMapZone selectedZone;
    private int selectedRegion;
    private int lastSelectionX = -1;
    private int lastSelectionZ = -1;
    private List<FroggerGridStack> selectedStacks = new ArrayList<>();
    private final List<FroggerGridSquare> cachedSelectedSquares = new ArrayList<>();
    private double tileWidth;
    private double tileHeight;
    private final CheckBox[] gridSquareFlagCheckBoxes = new CheckBox[FroggerGridSquareFlag.values().length];
    private final CheckBox[] zoneFlagMap = new CheckBox[FroggerMapCameraZoneFlag.values().length];
    private final TextField[][] cameraOffsetFields = new TextField[FroggerCameraRotation.values().length][FroggerOffsetVectorType.values().length];

    private static final int DEFAULT_REGION_ID = 0;

    private FroggerUIGridManager(FroggerMapMeshController controller) {
        super(controller.getMapFile().getGameInstance());
        this.mapMeshController = controller;
    }

    /**
     * Gets the Frogger map file.
     */
    public FroggerMapFile getMapFile() {
        return this.mapMeshController.getMapFile();
    }

    private void updateCollisionGridSizeLabel() {
        this.collisionGridMainLabel.setText("Collision Grid (" + getMapFile().getGridPacket().getGridXCount() + " x " + getMapFile().getGridPacket().getGridZCount() + "):");
    }

    @Override
    protected void onControllerLoad(Node rootNode) {
        // Update canvas view.
        this.shadingEnabledCheckBox.selectedProperty().addListener((observable, oldValue, newValue) -> updateCanvas());
        updateCollisionGridSizeLabel();

        this.layerSelector.valueProperty().addListener(((observable, oldValue, newValue) -> {
            updateGridSquareUI(); // This changes the selected grid squares.
            updateCanvas(); // This changes the canvas.
        }));

        this.highlightZonesCheckBox.selectedProperty().addListener((observable, oldValue, newValue) -> updateCanvas());

        this.layerSelector.setConverter(new AbstractStringConverter<>(id -> "Layer #" + (id + 1)));

        this.gridCanvas.setOnMousePressed(evt -> {
            int gridX = (int) (evt.getX() / this.tileWidth);
            int gridZ = (int) (evt.getY() / this.tileHeight);
            FroggerGridStack stack = getMapFile().getGridPacket().getGridStack(gridX, getMapFile().getGridPacket().getGridZCount() - gridZ - 1);
            if (stack == null)
                return;

            if (this.selectedStacks != null && this.selectedStacks.size() > 0) {
                if (evt.isControlDown()) { // Toggle grid stacks one at a time.
                    if (!this.selectedStacks.remove(stack))
                        this.selectedStacks.add(stack);
                } else if (evt.isShiftDown()) { // Cover a range.
                    this.selectedStacks.clear();
                    int minGridX = Math.min(gridX, this.lastSelectionX);
                    int minGridZ = Math.min(gridZ, this.lastSelectionZ);
                    int maxGridX = Math.max(gridX, this.lastSelectionX);
                    int maxGridZ = Math.max(gridZ, this.lastSelectionZ);
                    for (int x = minGridX; x <= maxGridX; x++)
                        for (int z = minGridZ; z <= maxGridZ; z++)
                            this.selectedStacks.add(getMapFile().getGridPacket().getGridStack(x, getMapFile().getGridPacket().getGridZCount() - z - 1));

                } else {
                    this.selectedStacks.clear();
                    this.selectedStacks.add(stack);
                }
            } else {
                this.selectedStacks = new ArrayList<>();
                this.selectedStacks.add(stack);
            }

            setSelectedStacks(this.selectedStacks);

            if (!evt.isShiftDown() || this.lastSelectionX == -1) {
                this.lastSelectionX = gridX;
                this.lastSelectionZ = gridZ;
            }

            if (this.regionEditorCheckBox.isSelected()) {
                gridZ = stack.getZ();

                if (this.editState == RegionEditState.NONE_SELECTED) {
                    if (getCurrentRegion() == null)
                        return;

                    for (RegionEditState state : RegionEditState.values()) {
                        if (state.getTester().apply(getCurrentRegion(), gridX, gridZ)) {
                            this.editState = state;
                            updateCanvas();
                            break;
                        }
                    }

                } else {
                    this.editState.setCoordinates(getCurrentRegion(), gridX, gridZ);
                    this.editState = RegionEditState.NONE_SELECTED;
                    updateCanvas();
                }

                return;
            }

            // Selects zones while highlighted.
            if (this.highlightZonesCheckBox.isSelected()) {
                gridZ = stack.getZ();

                for (FroggerMapZone zone : getMapFile().getZonePacket().getZones()) {
                    if (zone.contains(gridX, gridZ)) {
                        this.zoneSelector.valueProperty().setValue(zone);
                        this.zoneSelector.getSelectionModel().select(zone);

                        int index = zone.getRegions().indexOf(zone.getRegion(gridX, gridZ)) + 1;
                        if (index >= 1) {
                            this.regionSelector.getSelectionModel().select(index);
                            this.regionSelector.setValue(index);
                        }

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

        // Setup zone flags.
        int pos = 0;
        for (int i = 0; i < FroggerMapCameraZoneFlag.values().length; i++) {
            FroggerMapCameraZoneFlag flag = FroggerMapCameraZoneFlag.values()[i];
            if (flag.isDisplayHidden())
                continue; // Skip.

            CheckBox newBox = new CheckBox(flag.getDisplayName());
            GridPane.setRowIndex(newBox, pos / 2);
            GridPane.setColumnIndex(newBox, pos % 2);
            if (flag.getDescription() != null)
                newBox.setTooltip(new Tooltip(flag.getDescription()));
            this.zoneFlagGrid.getChildren().add(newBox);
            this.zoneFlagMap[i] = newBox;

            newBox.selectedProperty().addListener(((observable, oldValue, newValue) -> {
                boolean realFlagState = newValue ^ flag.isDisplayInverted();
                if (oldValue != newValue && ((FroggerMapCameraZone) this.selectedZone).setFlag(flag, realFlagState))
                    updateCameraZoneUI(); // These flags can change how the UI looks.
            }));
            pos++;
        }

        // Setup camera rotation selector.
        List<FroggerCameraRotation> cameraRotations = new ArrayList<>(Arrays.asList(FroggerCameraRotation.values()));
        cameraRotations.add(0, null);
        this.forcedCameraDirectionComboBox.setItems(FXCollections.observableArrayList(cameraRotations));
        this.forcedCameraDirectionComboBox.setConverter(new AbstractIndexStringConverter<>(cameraRotations, (index, rotation) -> rotation != null ? rotation.getDisplayString() : "None"));
        this.forcedCameraDirectionComboBox.setPlaceholder(new AbstractAttachmentCell<>((rotation, index) -> "None"));
        this.forcedCameraDirectionComboBox.setCellFactory(param -> new AbstractAttachmentCell<>((rotation, index) -> rotation != null ? rotation.getDisplayString() : "None"));
        this.forcedCameraDirectionComboBox.valueProperty().addListener((observable, oldValue, newValue) -> {
            if (this.selectedZone != null) {
                ((FroggerMapCameraZone) this.selectedZone).setForcedCameraDirection(newValue);
                updateCameraZoneUI();
            }
        });

        // Setup zone UI.
        this.zoneSelector.setConverter(new AbstractStringConverter<>(zone -> zone != null ? "Camera Zone " + zone.getZoneIndex() : "None"));
        this.zoneSelector.setPlaceholder(new AbstractAttachmentCell<>((rotation, index) -> "None"));
        this.zoneSelector.setCellFactory(param -> new AbstractAttachmentCell<>((zone, index) -> zone != null ? "Camera Zone " + zone.getZoneIndex() : "None"));
        updateZoneList();
        this.zoneSelector.valueProperty().addListener(((observable, oldValue, newValue) -> setSelectedZone(newValue)));

        this.regionSelector.setConverter(new AbstractStringConverter<>(value -> value == DEFAULT_REGION_ID ? "Main Region" : "Region " + value));
        this.regionSelector.valueProperty().addListener((observable, oldValue, newValue) -> setSelectedRegion(newValue == null ? DEFAULT_REGION_ID : newValue));
        this.regionEditorCheckBox.selectedProperty().addListener(((observable, oldValue, newValue) -> updateCanvas()));

        // Setup canvas and update everything.
        this.graphics = this.gridCanvas.getGraphicsContext2D();
        updateLayerSelectorValues();
        setSelectedStacks(null);
        setSelectedZone(null);
        setSelectedRegion(DEFAULT_REGION_ID);
    }

    private void updateCanvas() {
        // If shading is not enabled on the map, we can't pull from the shaded textures.
        this.shadingEnabledCheckBox.setDisable(!this.mapMeshController.getCheckBoxEnablePsxShading().isSelected());

        FroggerMapFilePacketGrid gridPacket = getMapFile().getGridPacket();
        this.tileWidth = this.gridCanvas.getWidth() / gridPacket.getGridXCount();
        this.tileHeight = this.gridCanvas.getHeight() / gridPacket.getGridZCount();

        this.graphics.clearRect(0, 0, this.gridCanvas.getWidth(), this.gridCanvas.getHeight());

        Map<ITextureSource, Image> cachedImageMap = new HashMap<>();
        Image textureImage = this.mapMeshController.getMesh().getMaterialFxImage();

        int selectedLayer = getSelectedLayer();
        FroggerMapZoneRegion currentRegion = getCurrentRegion();
        for (int z = 0; z < gridPacket.getGridZCount(); z++) {
            for (int x = 0; x < gridPacket.getGridXCount(); x++) {
                FroggerGridStack stack = gridPacket.getGridStack(x, z);

                double xPos = this.tileWidth * x;
                double yPos = this.tileHeight * (gridPacket.getGridZCount() - z - 1);

                Color fillColor;
                if (this.selectedStacks != null && this.selectedStacks.contains(stack)) {
                    fillColor = Color.AQUA;
                } else if (currentRegion != null && currentRegion.contains(x, z)) {
                    fillColor = Color.YELLOW;
                    if (this.regionEditorCheckBox.isSelected() && currentRegion.isCorner(x, z))
                        fillColor = this.editState.getTester().apply(currentRegion, x, z) ? Color.RED : Color.YELLOW;
                } else if (this.highlightZonesCheckBox.isSelected() && getMapZone(x, z) != null) {
                    fillColor = Color.MAGENTA;
                } else if (stack.getGridSquares().size() > 0) {
                    fillColor = Color.DARKGRAY;

                    // Find best square.
                    for (int i = Math.min(selectedLayer, stack.getGridSquares().size() - 1); i >= 0; i--) {
                        FroggerGridSquare square = stack.getGridSquares().get(i);
                        FroggerMapPolygon polygon = square.getPolygon();
                        if (polygon == null)
                            continue;

                        if (!polygon.getPolygonType().isTextured() || (this.shadingEnabledCheckBox.isSelected() && this.mapMeshController.getCheckBoxEnablePsxShading().isSelected())) {
                            PSXShadeTextureDefinition textureDefinition = this.mapMeshController.getMesh().getShadedTextureManager().getShadedTexture(square.getPolygon());
                            AtlasTexture texture = this.mapMeshController.getMesh().getTextureAtlas().getTextureFromSourceOrFallback(textureDefinition);
                            // TODO: STRETCH THE IMAGE. (Both here and below. I'm confident this is possible using the same method the PSXTextureShader uses to draw triangles)
                            //  - We find the minimum X, Y, Z vertex, and since we know which vertices are connected, we can use that to deterministically form which vertex goes to each corner in a rectangle.
                            //  - Then, we use scanline interpolation (left to right) drawing lines on the image to scale it to be a quad.
                            this.graphics.drawImage(textureImage, texture.getX(), texture.getY(), textureDefinition.getUnpaddedWidth(), textureDefinition.getUnpaddedHeight(), xPos, yPos, this.tileWidth, this.tileHeight);
                        } else {
                            ITextureSource textureSource = polygon.getTexture();
                            if (textureSource == null)
                                textureSource = UnknownTextureSource.MAGENTA_INSTANCE;

                            Image fxImage = cachedImageMap.computeIfAbsent(textureSource, texSource -> Utils.toFXImage(texSource.makeImage(), false));
                            this.graphics.drawImage(fxImage, textureSource.getLeftPadding(), textureSource.getUpPadding(), textureSource.getUnpaddedWidth(), textureSource.getUnpaddedHeight(), xPos, yPos, this.tileWidth, this.tileHeight);
                        }

                        fillColor = null;
                        break;
                    }
                } else {
                    fillColor = Color.GRAY;
                }

                if (fillColor != null) {
                    this.graphics.setFill(fillColor);
                    this.graphics.fillRect(xPos, yPos, this.tileWidth, this.tileHeight);
                }
            }
        }

        this.graphics.setStroke(Color.BLACK);
        for (int x = 0; x <= gridPacket.getGridXCount(); x++)
            this.graphics.strokeLine(x * this.tileWidth, 0, x * this.tileWidth, this.gridCanvas.getHeight());

        for (int z = 0; z <= gridPacket.getGridZCount(); z++)
            this.graphics.strokeLine(0, z * this.tileHeight, this.gridCanvas.getWidth(), z * this.tileHeight);

        // Draw outline of start square.
        this.graphics.setStroke(Color.RED);
        this.graphics.strokeRect(getMapFile().getGeneralPacket().getStartGridCoordX() * this.tileWidth, (gridPacket.getGridZCount() - getMapFile().getGeneralPacket().getStartGridCoordZ() - 1) * this.tileHeight, this.tileWidth, this.tileHeight);
    }

    private FroggerMapZone getMapZone(int x, int z) {
        List<FroggerMapZone> zones = getMapFile().getZonePacket().getZones();
        for (int i = 0; i < zones.size(); i++) {
            FroggerMapZone zone = zones.get(i);
            if (zone.getBoundingRegion().contains(x, z))
                return zone;
        }

        return null;
    }

    private void selectSquare(Consumer<FroggerMapPolygon> onSelect) {
        closeWindow();

        this.mapMeshController.getBakedGeometryManager().updateGridPolygonHighlighting(true);
        this.mapMeshController.getBakedGeometryManager().getPolygonSelector().activate(poly -> {
            this.mapMeshController.getBakedGeometryManager().updateGridPolygonHighlighting(false);
            onSelect.accept(poly);
            updateCanvas();
            Platform.runLater(this::openWindowAndWait);
        }, () -> {
            this.mapMeshController.getBakedGeometryManager().updateGridPolygonHighlighting(false);
            Platform.runLater(this::openWindowAndWait);
        });
    }

    @FXML
    private void changePolygon(ActionEvent evt) {
        if (this.selectedStacks == null)
            return;

        selectSquare(poly -> {
            int selectedLayer = getSelectedLayer();
            for (FroggerGridStack gridStack : this.selectedStacks) {
                if (gridStack.getGridSquares().isEmpty())
                    continue;

                FroggerGridSquare gridSquare = gridStack.getGridSquares().get(Math.min(selectedLayer, gridStack.getGridSquares().size() - 1));
                gridSquare.setPolygon(poly);
            }

            updateGridSquareUI(); // The displayed image may have changed.
            updateCanvas(); // Polygons may have changed.
        });
    }

    @FXML
    private void selectGridStackByPolygon(ActionEvent evt) {
        selectSquare(poly -> {
            int foundLayer = -1;
            List<FroggerGridStack> foundStacks = new ArrayList<>();
            for (int z = 0; z < getMapFile().getGridPacket().getGridZCount(); z++) {
                for (int x = 0; x < getMapFile().getGridPacket().getGridXCount(); x++) {
                    FroggerGridStack stack = getMapFile().getGridPacket().getGridStack(x, z);
                    if (stack.getGridSquares().isEmpty())
                        continue;

                    for (int layer = 0; layer < stack.getGridSquares().size(); layer++) {
                        FroggerGridSquare gridSquare = stack.getGridSquares().get(layer);
                        if (gridSquare.getPolygon() == poly) {
                            if (foundLayer != -1 && (layer > foundLayer || (layer < foundLayer && layer != stack.getGridSquares().size() - 1))) {
                                //
                                getLogger().warning("Skipped selecting grid square by polygon at unusable layer.");
                                continue;
                            }

                            foundStacks.add(stack);
                            foundLayer = layer;
                        }
                    }
                }
            }

            setSelectedStacks(foundStacks);
            if (foundLayer >= 0)
                this.layerSelector.getSelectionModel().select(foundLayer);
        });
    }

    @FXML
    private void addGridSquare(ActionEvent evt) {
        if (this.selectedStacks == null)
            return;

        selectSquare(poly -> {
            this.selectedStacks.forEach(stack -> stack.getGridSquares().add(new FroggerGridSquare(stack, poly)));
            updateLayerSelectorValues();
            updateGridStackUI();
        });
    }

    @FXML
    private void removeGridSquare(ActionEvent evt) {
        if (this.selectedStacks == null || this.selectedStacks.isEmpty())
            return;

        this.selectedStacks.forEach(stack -> {
            if (stack.getGridSquares().isEmpty())
                return;

            stack.getGridSquares().remove(Math.min(getSelectedLayer(), stack.getGridSquares().size() - 1));
        });

        updateLayerSelectorValues();
        updateGridStackUI();
        updateGridSquareUI();
        updateCanvas();
    }

    @FXML
    private void onUpdateHeight(ActionEvent evt) {
        String text = this.stackHeightField.getText();
        if (Utils.isNumber(text)) {
            this.stackHeightField.setStyle(null);
            this.selectedStacks.forEach(stack -> stack.setAverageWorldHeight(Float.parseFloat(text)));
        } else {
            this.stackHeightField.setStyle(Constants.FX_STYLE_INVALID_TEXT);
        }
    }

    @FXML
    private void addZone(ActionEvent evt) {
        FroggerMapZone zone = new FroggerMapCameraZone(getMapFile());
        getMapFile().getZonePacket().getZones().add(zone);

        updateZoneList();
        this.zoneSelector.getSelectionModel().select(zone);
    }

    @FXML
    private void deleteZone(ActionEvent evt) {
        getMapFile().getZonePacket().getZones().remove(this.selectedZone);
        this.selectedZone = null;
        updateZoneList();
        this.zoneSelector.getSelectionModel().select(null);
    }

    private void updateZoneList() {
        List<FroggerMapZone> mapZones = new ArrayList<>(getMapFile().getZonePacket().getZones());
        mapZones.add(0, null);
        this.zoneSelector.setItems(FXCollections.observableArrayList(mapZones));
    }

    @FXML
    private void addRegion(ActionEvent evt) {
        FroggerMapZoneRegion newRegion = new FroggerMapZoneRegion();
        this.selectedZone.getRegions().add(newRegion);
        int newId = this.regionSelector.getItems().size();
        this.regionSelector.getItems().add(newId);
        this.regionSelector.getSelectionModel().select(newId);
    }

    @FXML
    private void deleteRegion(ActionEvent evt) {
        this.selectedZone.getRegions().remove(this.selectedRegion - 1);
        this.regionSelector.getItems().remove(this.selectedRegion);
        for (int i = this.selectedRegion + 1; i < this.regionSelector.getItems().size(); i++)
            this.regionSelector.getItems().set(i, this.regionSelector.getItems().get(i) - 1);

        this.regionSelector.getSelectionModel().select(--this.selectedRegion);
    }

    @FXML
    private void onResizeGrid(ActionEvent evt) {
        FroggerGridResizeController.open(this);
    }

    /**
     * Gets the currently selected grid square layer.
     */
    public int getSelectedLayer() {
        Integer layer = this.layerSelector.getValue();
        return layer != null ? layer : -1;
    }

    /**
     * Gets the currently selected grid squares.
     */
    public List<FroggerGridSquare> getSelectedGridSquares() {
        this.cachedSelectedSquares.clear();
        if (this.selectedStacks == null || this.selectedStacks.isEmpty())
            return this.cachedSelectedSquares;

        int selectedLayer = getSelectedLayer();
        for (int i = 0; i < this.selectedStacks.size(); i++) {
            FroggerGridStack gridStack = this.selectedStacks.get(i);
            if (gridStack.getGridSquares().size() > 0)
                this.cachedSelectedSquares.add(gridStack.getGridSquares().get(Math.min(selectedLayer, gridStack.getGridSquares().size() - 1)));
        }

        return this.cachedSelectedSquares;
    }

    /**
     * Select the stacks currently being edited.
     * @param stacks The stacks to select.
     */
    public void setSelectedStacks(List<FroggerGridStack> stacks) {
        this.selectedStacks = stacks != null && stacks.size() > 0 ? stacks : null;
        updateGridStackUI();
        updateGridSquareUI();
        updateCanvas();
    }

    private void updateLayerSelectorValues() {
        int maxLayer = 0;
        FroggerMapFilePacketGrid gridPacket = getMapFile().getGridPacket();
        for (int z = 0; z < gridPacket.getGridZCount(); z++) {
            for (int x = 0; x < gridPacket.getGridXCount(); x++) {
                FroggerGridStack gridStack = gridPacket.getGridStack(x, z);
                if (gridStack.getGridSquares().size() > maxLayer)
                    maxLayer = gridStack.getGridSquares().size();
            }
        }

        int previousSize = this.layerSelector.getItems() != null ? this.layerSelector.getItems().size() : -1;
        if (previousSize == maxLayer)
            return; // No change.

        // Setup new layer selector.
        int oldSelectedLayer = getSelectedLayer();
        List<Integer> layers = Utils.getIntegerList(maxLayer);
        this.layerSelector.setItems(FXCollections.observableArrayList(layers));
        if (layers.size() > 0) { // Automatically select the new value.
            boolean usePrevious = (oldSelectedLayer >= 0 && oldSelectedLayer < maxLayer) && (previousSize - 1 > oldSelectedLayer);
            this.layerSelector.getSelectionModel().select(usePrevious ? oldSelectedLayer : maxLayer - 1);
        }
    }

    /**
     * Updates the UI for the selected grid stack(s).
     */
    public void updateGridStackUI() {
        int gridStackCount = this.selectedStacks != null ? this.selectedStacks.size() : 0;

        // Update main UI components.
        this.gridStackSelectedLabel.setDisable(gridStackCount == 0);
        this.stackHeightLabel.setDisable(gridStackCount == 0);
        this.stackHeightField.setDisable(gridStackCount == 0);
        this.addGridSquareButton.setDisable(gridStackCount == 0);
        this.removeGridSquareButton.setDisable(gridStackCount == 0 || this.selectedStacks.stream().mapToInt(gridStack -> gridStack.getGridSquares().size()).sum() == 0);

        // Update stack display.
        if (gridStackCount > 1) {
            this.gridStackSelectedLabel.setText(gridStackCount + " stacks selected");
            if (sameHeight(this.selectedStacks)) {
                this.stackHeightField.setText(String.valueOf(this.selectedStacks.get(0).getAverageWorldHeightAsFloat()));
            } else {
                this.stackHeightField.setText("");
            }
        } else if (gridStackCount == 1) {
            FroggerGridStack gridStack = this.selectedStacks.get(0);
            this.gridStackSelectedLabel.setText("Stack[x=" + gridStack.getX() + ",x=" + gridStack.getZ() + "]");
            this.stackHeightField.setText(String.valueOf(gridStack.getAverageWorldHeightAsFloat()));
        }
    }

    /**
     * Updates the UI for the selected grid square.
     */
    public void updateGridSquareUI() {
        List<FroggerGridSquare> gridSquares = getSelectedGridSquares();
        boolean hasGridSquare = gridSquares.size() > 0;
        this.gridSquareLabel.setVisible(hasGridSquare);
        this.selectedImage.setVisible(hasGridSquare);
        this.changePolygonButton.setVisible(hasGridSquare);
        this.flagTable.setVisible(hasGridSquare);

        // Update image preview.
        FroggerGridSquare firstSquare = gridSquares.size() > 0 ? gridSquares.get(0) : null;
        FroggerGridSquare singleSquare = gridSquares.size() == 1 ? gridSquares.get(0) : null;
        if (singleSquare != null && singleSquare.getPolygon() != null && singleSquare.getPolygon().getTexture() != null) {
            this.selectedImage.setImage(singleSquare.getPolygon().getTexture().toFXImage());
        } else {
            this.selectedImage.setImage(null);
        }

        // Update polygon label.
        this.polygonTypeLabel.setText(singleSquare != null && singleSquare.getPolygon() != null ? singleSquare.getPolygon().getPolygonType().name() : "");
        this.polygonTypeLabel.setVisible(singleSquare != null);

        int x = 1;
        int y = 0;
        for (FroggerGridSquareFlag flag : FroggerGridSquareFlag.values()) {
            if (!flag.isLandGridData())
                continue;

            if (x == 2) {
                x = 0;
                y++;
            }

            CheckBox checkBox = this.gridSquareFlagCheckBoxes[flag.ordinal()];
            boolean isFlagSet = firstSquare != null && firstSquare.testFlag(flag);
            boolean statesMatch = gridSquares.stream()
                    .map(square -> square.testFlag(flag))
                    .allMatch(flagState -> flagState == isFlagSet);

            if (checkBox == null) {
                // Create new.
                CheckBox newCheckBox = new CheckBox(Utils.capitalize(flag.name()));
                newCheckBox.setAllowIndeterminate(false);
                if (flag.getTooltipDescription() != null)
                    newCheckBox.setTooltip(new Tooltip(flag.getTooltipDescription()));

                GridPane.setRowIndex(newCheckBox, y);
                GridPane.setColumnIndex(newCheckBox, x++);
                this.flagTable.getChildren().add(newCheckBox);
                this.gridSquareFlagCheckBoxes[flag.ordinal()] = newCheckBox;
                newCheckBox.selectedProperty().addListener(((observable, oldValue, newValue) -> {
                    if (!newCheckBox.isIndeterminate())
                        getSelectedGridSquares().forEach(square -> square.setFlag(flag, newValue));
                }));

                checkBox = newCheckBox;
            }

            // Update check box.
            // To avoid changing the flags, we're going to enable indeterminate mode.
            checkBox.setVisible(hasGridSquare);
            checkBox.setIndeterminate(true); // Ensure updates to the grid square will be skipped.
            if (statesMatch) {
                checkBox.setSelected(isFlagSet);
                checkBox.setIndeterminate(false); // We do not want the indeterminate state after changing the selection state.
            } else {
                checkBox.setSelected(false);
            }
        }
    }

    private static boolean sameHeight(Collection<FroggerGridStack> stacks) {
        return stacks.stream()
                .mapToInt(FroggerGridStack::getAverageWorldHeight)
                .distinct().count() == 1;
    }

    /**
     * Set the selected zone.
     * @param newZone The new zone to select.
     */
    public void setSelectedZone(FroggerMapZone newZone) {
        this.selectedZone = newZone;

        if (newZone != null) {
            List<Integer> regionIds = new ArrayList<>(Utils.getIntegerList(newZone.getRegionCount() + 1));
            this.regionSelector.setItems(FXCollections.observableArrayList(regionIds));
            if (regionIds.size() > 0)
                this.regionSelector.getSelectionModel().select(DEFAULT_REGION_ID); // Automatically calls setRegion
        }

        updateCameraZoneUI();
        updateCanvas();
    }

    /**
     * Gets the selected zone as a camera zone, if there is a selected zone which is a camera zone.
     */
    public FroggerMapCameraZone getActiveCameraZone() {
        return this.selectedZone instanceof FroggerMapCameraZone ? (FroggerMapCameraZone) this.selectedZone : null;
    }

    /**
     * Updates the Camera Zone UI.
     */
    public void updateCameraZoneUI() {
        boolean hasZone = this.selectedZone != null;
        this.removeZoneButton.setDisable(!hasZone);
        this.regionSelector.setDisable(!hasZone);
        this.addRegionButton.setDisable(!hasZone);
        this.zoneFlagGrid.setDisable(!hasZone);
        this.forcedCameraDirectionComboBox.setDisable(!hasZone);
        this.cameraPane.setDisable(!hasZone);
        this.regionEditorCheckBox.setDisable(!hasZone);
        this.forcedCameraDirectionLabel.setDisable(!hasZone);

        if (this.selectedZone instanceof FroggerMapCameraZone) {
            FroggerMapCameraZone camZone = (FroggerMapCameraZone) this.selectedZone;
            for (FroggerMapCameraZoneFlag zoneFlag : FroggerMapCameraZoneFlag.values()) {
                if (!zoneFlag.isDisplayHidden()) {
                    CheckBox checkBox = this.zoneFlagMap[zoneFlag.ordinal()];
                    checkBox.setDisable(zoneFlag.isFlagDisabled(camZone));
                    checkBox.setSelected(camZone.testFlag(zoneFlag) ^ zoneFlag.isDisplayInverted());
                }
            }

            this.forcedCameraDirectionComboBox.setValue(camZone.getForcedCameraDirection());
            for (int i = 0; i < FroggerCameraRotation.values().length; i++) {
                FroggerCameraRotation cameraRotation = FroggerCameraRotation.values()[i];
                for (int j = 0; j < FroggerOffsetVectorType.values().length; j++) {
                    FroggerOffsetVectorType vectorType = FroggerOffsetVectorType.values()[j];
                    setupVectorEditor(camZone, cameraRotation, vectorType);
                }
            }
        } else {
            // Hide the camera zones if we don't have a zone selected.
            for (int i = 0; i < this.cameraOffsetFields.length; i++) {
                TextField[] cameraOffsetFields = this.cameraOffsetFields[i];
                for (int j = 0; j < cameraOffsetFields.length; j++) {
                    TextField textField = cameraOffsetFields[j];
                    if (textField == null)
                        continue;

                    textField.setVisible(false);
                    textField.setText("");
                }
            }
        }
    }

    private TextField setupVectorEditor(FroggerMapCameraZone camZone, FroggerCameraRotation rotation, FroggerOffsetVectorType vectorType) {
        TextField textField = this.cameraOffsetFields[rotation.ordinal()][vectorType.ordinal()];

        // Create new text field if it doesn't exist.
        boolean createNewTextField = (textField == null);
        if (createNewTextField) {
            textField = new TextField();
            GridPane.setRowIndex(textField, rotation.ordinal() + 1);
            GridPane.setColumnIndex(textField, vectorType.ordinal() + 1);
            this.cameraPane.getChildren().add(textField);
            this.cameraOffsetFields[rotation.ordinal()][vectorType.ordinal()] = textField;
        }

        // Apply the current offset to the text field.
        textField.setVisible(true);
        textField.setText(camZone.getCameraOffset(rotation, vectorType).toFloatString());
        textField.setDisable(camZone.isCameraRotationOffsetDisabled(rotation));

        // Handle the edits.
        // We make a new key press handler each time to avoid a partially modified text field from having its old value bleed in after a new offset is put in the text field.
        Utils.setHandleKeyPress(textField, newText -> {
            FroggerMapCameraZone safeCamZone = getActiveCameraZone();
            if (safeCamZone == null) { // Shouldn't occur.
                updateCameraZoneUI();
                return false;
            }

            // Apply the new text into the camera offset.
            SVector realTarget = safeCamZone.getCameraOffset(rotation, vectorType);
            SVector originalValues = realTarget.clone();
            if (!realTarget.loadFromFloatText(newText))
                return false;

            // Copy the value to the disabled vectors.
            for (int i = 0; i < FroggerCameraRotation.values().length; i++) {
                FroggerCameraRotation tempRotation = FroggerCameraRotation.values()[i];
                if (tempRotation != rotation && safeCamZone.isCameraRotationOffsetDisabled(tempRotation)) {
                    SVector targetCameraOffset = safeCamZone.getCameraOffset(tempRotation, vectorType);
                    if (targetCameraOffset.equals(originalValues)) // Update values if they match the main vector before it was modified.
                        targetCameraOffset.setValues(realTarget);
                }
            }

            return true;
        }, this::updateCameraZoneUI);
        // We run updateCameraZoneUI() upon success in order to ensure that the actual value currently tracked is accurately reflected.
        // But, it's not just the edited camera offset, any disabled offset text fields need updating too.

        return textField;
    }

    /**
     * Set the selected region.
     * @param id   The id of the new region.
     */
    public void setSelectedRegion(int id) {
        this.selectedRegion = id;

        boolean hasRegion = (id != DEFAULT_REGION_ID);
        this.removeRegionButton.setDisable(!hasRegion);
        updateCanvas();
    }

    /**
     * Get the currently selected region.
     * @return currentRegion
     */
    public FroggerMapZoneRegion getCurrentRegion() {
        if (this.selectedZone == null)
            return null;

        return this.selectedRegion == DEFAULT_REGION_ID
                ? this.selectedZone.getBoundingRegion()
                : this.selectedZone.getRegions().get(this.selectedRegion - 1);
    }

    /**
     * Resize the grid and updates the UI.
     * @param newX The new x grid size.
     * @param newZ The new z grid size.
     */
    public void handleResize(int newX, int newZ) {
        getMapFile().getGridPacket().resizeGrid(newX, newZ);
        updateCollisionGridSizeLabel();

        // Update the selected stacks.
        List<FroggerGridStack> newStacks = new ArrayList<>();
        for (int z = 0; z < newZ; z++) {
            for (int x = 0; x < newX; x++) {
                FroggerGridStack newStack = getMapFile().getGridPacket().getGridStack(x, z);
                if (this.selectedStacks != null && this.selectedStacks.contains(newStack))
                    newStacks.add(newStack);
            }
        }

        // Apply changes.
        updateLayerSelectorValues();
        setSelectedStacks(newStacks);
    }

    /**
     * Open the collision grid viewer.
     * @param controller The mesh manager.
     */
    public static void openGridEditor(FroggerMapMeshController controller) {
        Utils.createWindowFromFXMLTemplate("window-edit-map-collision-grid", new FroggerUIGridManager(controller), "Grid Editor", true);
    }
}