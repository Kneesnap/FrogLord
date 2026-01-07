package net.highwayfrogs.editor.games.sony.frogger.map.ui.editor.baked;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.paint.Color;
import lombok.Getter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.games.psx.math.vector.SVector;
import net.highwayfrogs.editor.games.sony.frogger.FroggerGameInstance;
import net.highwayfrogs.editor.games.sony.frogger.map.FroggerMapFile;
import net.highwayfrogs.editor.games.sony.frogger.map.data.FroggerOffsetVectorType;
import net.highwayfrogs.editor.games.sony.frogger.map.data.grid.FroggerGridSquare;
import net.highwayfrogs.editor.games.sony.frogger.map.data.grid.FroggerGridSquareFlag;
import net.highwayfrogs.editor.games.sony.frogger.map.data.grid.FroggerGridSquareReaction;
import net.highwayfrogs.editor.games.sony.frogger.map.data.grid.FroggerGridStack;
import net.highwayfrogs.editor.games.sony.frogger.map.data.zone.FroggerCameraRotation;
import net.highwayfrogs.editor.games.sony.frogger.map.data.zone.FroggerMapCameraZone;
import net.highwayfrogs.editor.games.sony.frogger.map.data.zone.FroggerMapCameraZone.FroggerMapCameraZoneFlag;
import net.highwayfrogs.editor.games.sony.frogger.map.data.zone.FroggerMapZone;
import net.highwayfrogs.editor.games.sony.frogger.map.data.zone.FroggerMapZoneRegion;
import net.highwayfrogs.editor.games.sony.frogger.map.mesh.FroggerMapMesh;
import net.highwayfrogs.editor.games.sony.frogger.map.mesh.FroggerMapMeshController;
import net.highwayfrogs.editor.games.sony.frogger.map.mesh.FroggerMapPolygon;
import net.highwayfrogs.editor.games.sony.frogger.map.mesh.FroggerMapPolygonType;
import net.highwayfrogs.editor.games.sony.frogger.map.packets.FroggerMapFilePacketGeneral;
import net.highwayfrogs.editor.games.sony.frogger.map.packets.FroggerMapFilePacketGrid;
import net.highwayfrogs.editor.games.sony.frogger.map.ui.editor.baked.grid.FroggerUICollisionGridPreview;
import net.highwayfrogs.editor.gui.GameUIController;
import net.highwayfrogs.editor.gui.InputManager;
import net.highwayfrogs.editor.gui.mesh.DynamicMeshDataEntry;
import net.highwayfrogs.editor.gui.texture.basic.RawColorTextureSource;
import net.highwayfrogs.editor.system.AbstractAttachmentCell;
import net.highwayfrogs.editor.system.AbstractIndexStringConverter;
import net.highwayfrogs.editor.system.AbstractStringConverter;
import net.highwayfrogs.editor.system.math.Vector3f;
import net.highwayfrogs.editor.utils.FXUtils;
import net.highwayfrogs.editor.utils.NumberUtils;
import net.highwayfrogs.editor.utils.StringUtils;
import net.highwayfrogs.editor.utils.Utils;
import net.highwayfrogs.editor.utils.fx.wrapper.LazyFXListCell;

import java.util.*;
import java.util.function.Consumer;

/**
 * This manages a window displaying the collision grid in a baked Frogger map.
 * Water tiles aren't always super visible because they are hidden by the transparent water layer.
 *  -> The easiest way to see clearly is to toggle wireframe mode.
 * Created by Kneesnap on 6/6/2024.
 */
public class FroggerUIGridManager extends GameUIController<FroggerGameInstance> {
    @Getter private final FroggerMapMeshController mapMeshController;
    @Getter private final InputManager inputManager;
    @Getter private FroggerUICollisionGridPreview collisionGridPreview;
    private final CheckBox[] gridSquareFlagCheckBoxes = new CheckBox[FroggerGridSquareFlag.values().length];
    private final CheckBox[] zoneFlagMap = new CheckBox[FroggerMapCameraZoneFlag.values().length];
    private final TextField[][] cameraOffsetFields = new TextField[FroggerCameraRotation.values().length][FroggerOffsetVectorType.values().length];
    @Getter private FroggerMapZone selectedZone;
    private int selectedRegion;

    // Main Controls:
    @FXML private Label collisionGridMainLabel;
    @Getter @FXML private CheckBox shadingEnabledCheckBox;
    @Getter @FXML private ComboBox<Integer> layerSelector;
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
    @FXML private ComboBox<FroggerGridSquareReaction> flagReactionSelector;
    @FXML private GridPane flagTable;
    private boolean uiUpdateInProgressSquare;

    // Zones:
    @Getter @FXML private ComboBox<FroggerMapZone> zoneSelector;
    @FXML private Button addZoneButton;
    @FXML private Button removeZoneButton;
    @Getter @FXML private CheckBox highlightZonesCheckBox;
    @FXML private Label forcedCameraDirectionLabel;
    @FXML private ComboBox<FroggerCameraRotation> forcedCameraDirectionComboBox;
    @FXML private GridPane zoneFlagGrid;
    @FXML private GridPane cameraPane;

    // Zone Regions:
    @Getter@FXML private ComboBox<Integer> regionSelector;
    @FXML private Button addRegionButton;
    @FXML private Button removeRegionButton;
    @Getter @FXML private CheckBox regionEditorCheckBox;

    public static final RawColorTextureSource MATERIAL_HIGHLIGHT_GRID_DARK_RED = new RawColorTextureSource(Color.rgb(139, 0, 0, .75F));
    public static final RawColorTextureSource MATERIAL_HIGHLIGHT_GRID_RED = new RawColorTextureSource(Color.rgb(255, 0, 0, .65F));
    public static final RawColorTextureSource MATERIAL_HIGHLIGHT_GRID_PINK = new RawColorTextureSource(Color.rgb(255, 105, 180, .5F));
    public static final RawColorTextureSource MATERIAL_HIGHLIGHT_GRID_HOT_PINK = new RawColorTextureSource(Color.rgb(255, 0, 255, .75F));
    public static final RawColorTextureSource MATERIAL_HIGHLIGHT_GRID_PURPLE = new RawColorTextureSource(Color.rgb(255, 0, 255, .35F));
    public static final RawColorTextureSource MATERIAL_HIGHLIGHT_GRID_GREEN = new RawColorTextureSource(Color.rgb(57, 255, 20, .333F));
    public static final RawColorTextureSource MATERIAL_HIGHLIGHT_GRID_LIME_GREEN = new RawColorTextureSource(Color.rgb(57, 255, 20, .5F));
    public static final RawColorTextureSource MATERIAL_HIGHLIGHT_GRID_AQUA = new RawColorTextureSource(Color.rgb(0, 255, 255, .75F));
    public static final RawColorTextureSource MATERIAL_HIGHLIGHT_GRID_DARK_BLUE = new RawColorTextureSource(Color.rgb(0, 0, 255, .333F));
    public static final RawColorTextureSource MATERIAL_HIGHLIGHT_GRID_YELLOW = new RawColorTextureSource(Color.rgb(255, 255, 0, .65F));
    public static final RawColorTextureSource MATERIAL_HIGHLIGHT_GRID_GOLD = new RawColorTextureSource(Color.rgb(255, 165, 0, .65F));
    public static final RawColorTextureSource MATERIAL_HIGHLIGHT_GRID_ORANGE = new RawColorTextureSource(Color.rgb(255, 69, 0, .65F));
    public static final RawColorTextureSource MATERIAL_HIGHLIGHT_GREY = new RawColorTextureSource(Color.rgb(255, 255, 255, .333F));
    public static final RawColorTextureSource MATERIAL_HIGHLIGHT_LIGHT_GREY = new RawColorTextureSource(Color.rgb(255, 255, 255, .5F));

    private FroggerUIGridManager(FroggerMapMeshController controller) {
        super(controller.getMapFile().getGameInstance());
        this.mapMeshController = controller;
        this.inputManager = new InputManager(controller.getGameInstance());
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
    public void onSceneAdd(Scene scene) {
        super.onSceneAdd(scene);
        setGridPolygonHighlightingVisible(true);
        this.collisionGridPreview.onSceneAdd();
        this.inputManager.assignSceneControls(scene);
        this.inputManager.setStage(getStage());
    }

    @Override
    public void onSceneRemove(Scene scene) {
        super.onSceneRemove(scene);
        setGridPolygonHighlightingVisible(false);
        this.collisionGridPreview.onSceneRemove();
        this.inputManager.removeSceneControls(scene);
        this.inputManager.setStage(null);
    }

    @Override
    protected void onControllerLoad(Node rootNode) {
        setStageAlwaysOnTop(true);
        this.collisionGridPreview = new FroggerUICollisionGridPreview(this, this.gridCanvas);
        this.collisionGridPreview.setupCanvas();

        // Update canvas view.
        this.shadingEnabledCheckBox.selectedProperty().addListener((observable, oldValue, newValue) -> this.collisionGridPreview.redrawEntireCanvas());
        updateCollisionGridSizeLabel();

        // Setup flag selector.
        this.flagReactionSelector.setConverter(new AbstractStringConverter<>(FroggerGridSquareReaction::getDisplayName, "Custom"));
        this.flagReactionSelector.setCellFactory(listView
                -> new LazyFXListCell<>(FroggerGridSquareReaction::getDisplayName, "Custom")
                .setWithoutIndexTooltipHandler(FroggerGridSquareReaction::getTooltip));

        List<FroggerGridSquareReaction> reactionList = new ArrayList<>(FroggerGridSquareReaction.DISPLAY_ORDER);
        reactionList.add(null);
        this.flagReactionSelector.setItems(FXCollections.observableArrayList(reactionList));
        this.flagReactionSelector.valueProperty().addListener((observable, oldValue, newValue) -> {
            if (this.uiUpdateInProgressSquare || !this.flagReactionSelector.isVisible() || (oldValue == newValue))
                return;

            if (newValue != null) {
                this.collisionGridPreview.getMapMesh().pushBatchOperations();
                boolean badCliffDeathData = false;
                Vector3f temp = new Vector3f();
                for (FroggerGridSquare gridSquare : this.collisionGridPreview.getSelectedGridSquares()) {
                    gridSquare.setReaction(newValue);
                    updateGridPolygonHighlighting(gridSquare, true);

                    // This isn't perfect, but should work well enough.
                    FroggerMapPolygon polygon = gridSquare.getPolygon();
                    if (polygon != null && newValue == FroggerGridSquareReaction.CLIFF_DEATH && !badCliffDeathData) {
                        float testY = polygon.getCenterOfPolygon(temp).getY();
                        if (testY <= FroggerGridStack.CLIFF_Y_THRESHOLD_MAX || testY > FroggerGridStack.CLIFF_Y_THRESHOLD_MIN)
                            badCliffDeathData = true;
                    }
                }

                this.collisionGridPreview.getMapMesh().popBatchOperations();
                if (badCliffDeathData)
                    FXUtils.makePopUp("Cliff deaths do not work correctly below Y=" + FroggerGridStack.CLIFF_Y_THRESHOLD_MIN + " or above Y=" + FroggerGridStack.CLIFF_Y_THRESHOLD_MAX + ".", AlertType.WARNING);
            }

            updateGridFlagsUI(false);
        });

        this.layerSelector.setConverter(new AbstractStringConverter<>(id -> "Layer #" + (id + 1)));
        this.layerSelector.valueProperty().addListener(((observable, oldValue, newValue) -> {
            updateGridSquareUI(); // This changes the selected grid squares.
            this.collisionGridPreview.redrawEntireCanvas(); // This changes the canvas.
        }));

        this.highlightZonesCheckBox.selectedProperty().addListener((observable, oldValue, newValue) -> this.collisionGridPreview.redrawEntireCanvas());

        // Setup zone flags.
        int pos = 0;
        for (int i = 0; i < FroggerMapCameraZoneFlag.values().length; i++) {
            FroggerMapCameraZoneFlag flag = FroggerMapCameraZoneFlag.values()[i];
            if (flag.isDisplayHidden())
                continue; // Skip.

            CheckBox newBox = new CheckBox(flag.getDisplayName());
            GridPane.setRowIndex(newBox, pos / 2);
            GridPane.setColumnIndex(newBox, pos % 2);
            newBox.setTooltip(FXUtils.createTooltip(flag.getDescription()));
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
        this.forcedCameraDirectionComboBox.setConverter(new AbstractIndexStringConverter<>(cameraRotations, (index, rotation) -> rotation.getDisplayString(), "None"));
        this.forcedCameraDirectionComboBox.setPlaceholder(new AbstractAttachmentCell<>((rotation, index) -> "None"));
        this.forcedCameraDirectionComboBox.setCellFactory(param -> new AbstractAttachmentCell<>((rotation, index) -> rotation != null ? rotation.getDisplayString() : "None"));
        this.forcedCameraDirectionComboBox.valueProperty().addListener((observable, oldValue, newValue) -> {
            if (this.selectedZone != null) {
                ((FroggerMapCameraZone) this.selectedZone).setForcedCameraDirection(newValue);
                updateCameraZoneUI();
            }
        });

        // Setup zone UI.
        this.zoneSelector.setConverter(new AbstractStringConverter<>(zone -> "Camera Zone " + zone.getZoneIndex(), "None"));
        this.zoneSelector.setPlaceholder(new AbstractAttachmentCell<>((rotation, index) -> "None"));
        this.zoneSelector.setCellFactory(param -> new AbstractAttachmentCell<>((zone, index) -> zone != null ? "Camera Zone " + zone.getZoneIndex() : "None"));
        updateZoneList();
        this.zoneSelector.valueProperty().addListener(((observable, oldValue, newValue) -> setSelectedZone(newValue)));

        this.regionSelector.setConverter(new AbstractStringConverter<>(value -> "Region " + value));
        this.regionSelector.valueProperty().addListener((observable, oldValue, newValue) -> onRegionSelected(newValue == null ? 0 : newValue));
        this.regionEditorCheckBox.selectedProperty().addListener(((observable, oldValue, newValue) -> this.collisionGridPreview.redrawEntireCanvas()));

        // Setup canvas and update everything.
        updateLayerSelectorValues();
        setSelectedZone(null);
        onRegionSelected(0);
    }

    private void askUserToSelectPolygon(Consumer<FroggerMapPolygon> onSelect) {
        closeWindow();
        setGridPolygonHighlightingVisible(true); // Force highlighting to be visible.

        this.mapMeshController.getBakedGeometryManager().getPolygonSelector().activate(poly -> {
            onSelect.accept(poly);
            Platform.runLater(this::openWindowAndWait);
        }, () -> Platform.runLater(this::openWindowAndWait));
    }

    @FXML
    private void changePolygon(ActionEvent evt) {
        askUserToSelectPolygon(poly -> {
            for (FroggerGridSquare gridSquare : this.collisionGridPreview.getSelectedGridSquares()) {
                updateGridPolygonHighlighting(gridSquare, false); // Remove highlighting from old polygon.
                gridSquare.setPolygon(poly);
                updateGridStackIfStartPosition(gridSquare.getGridStack());
                updateGridPolygonHighlighting(gridSquare, true); // Apply highlighting to new polygon.
                FroggerGridStack gridStack = gridSquare.getGridStack();
                this.collisionGridPreview.updateCanvasTile(gridStack.getX(), gridStack.getZ());
            }

            updateGridSquareUI(); // The displayed image may have changed.
        });
    }

    private void updateGridStackIfStartPosition(FroggerGridStack gridStack) {
        FroggerMapFilePacketGeneral generalPacket = getMapFile().getGeneralPacket();
        if (gridStack.getX() == generalPacket.getStartGridCoordX() && gridStack.getZ() == generalPacket.getStartGridCoordZ())
            getMapMeshController().getGeneralManager().updatePlayerCharacter();
    }

    @FXML
    private void addGridSquare(ActionEvent evt) {
        askUserToSelectPolygon(poly -> {
            if (this.collisionGridPreview.getSelectedGridStacks().size() != 1)
                return;

            this.collisionGridPreview.getMapMesh().pushBatchOperations();
            for (FroggerGridStack gridStack : this.collisionGridPreview.getSelectedGridStacks()) { // Be careful to avoid ConcurrentModificationException.
                FroggerGridSquare newGridSquare = new FroggerGridSquare(gridStack, poly);

                gridStack.getGridSquares().add(newGridSquare);
                updateGridStackIfStartPosition(gridStack);
                this.collisionGridPreview.selectGridSquare(newGridSquare, false);

                // Deselect old squares.
                for (FroggerGridSquare oldSquare : gridStack.getGridSquares())
                    if (oldSquare != newGridSquare)
                        this.collisionGridPreview.deselectGridSquare(oldSquare, false);

                updateGridPolygonHighlighting(newGridSquare, true);
            }
            this.collisionGridPreview.getMapMesh().popBatchOperations();

            updateLayerSelectorValues();
            updateGridStackUI();
            updateGridSquareUI();
        });
    }

    @FXML
    private void removeGridSquare(ActionEvent evt) {
        this.collisionGridPreview.getMapMesh().pushBatchOperations();
        for (FroggerGridSquare gridSquare : new HashSet<>(this.collisionGridPreview.getSelectedGridSquares())) { // Avoid Concurrent modification.
            gridSquare.getGridStack().getGridSquares().remove(gridSquare); // Removed first so the updated canvas won't include it.
            updateGridStackIfStartPosition(gridSquare.getGridStack());
            this.collisionGridPreview.deselectGridSquare(gridSquare, false);
            updateGridPolygonHighlighting(gridSquare, false);
        }
        this.collisionGridPreview.getMapMesh().popBatchOperations();

        updateLayerSelectorValues();
        updateGridStackUI();
        updateGridSquareUI();
    }

    @FXML // This is no longer accessible to the average user. It has been kept around for debugging purposes, but most users will never need to edit this.
    private void onUpdateHeight(ActionEvent evt) {
        String text = this.stackHeightField.getText();
        if (NumberUtils.isNumber(text)) {
            this.stackHeightField.setStyle(null);
            this.collisionGridPreview.getSelectedGridStacks().forEach(stack -> stack.setCliffHeight(Float.parseFloat(text)));
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
        FroggerMapFilePacketGrid gridPacket = getMapFile().getGridPacket();

        // Create new region and generate default position.
        FroggerMapZoneRegion newRegion = new FroggerMapZoneRegion(this.selectedZone);
        if (this.collisionGridPreview.getSelectedGridStacks().isEmpty()) {
            // Place it in the center.
            int baseX = gridPacket.getGridXCount() / 2;
            int baseZ = gridPacket.getGridZCount() / 2;
            newRegion.setXMin((short) (baseX - 1));
            newRegion.setXMax((short) (baseX + 1));
            newRegion.setZMin((short) (baseZ - 1));
            newRegion.setZMax((short) (baseZ + 1));
        } else {
            newRegion.setXMin((short) (gridPacket.getGridXCount() - 1)); // If we don't put it here, it'll always place the min at x=0.
            newRegion.setZMin((short) (gridPacket.getGridZCount() - 1)); // If we don't put it here, it'll always place the min at z=0.
            for (FroggerGridStack gridStack : this.collisionGridPreview.getSelectedGridStacks()) {
                int x = Math.min(Math.max(1, gridStack.getX()), gridPacket.getGridXCount() - 1);
                int z = Math.min(Math.max(1, gridStack.getZ()), gridPacket.getGridZCount() - 1);
                if (x <= newRegion.getXMin())
                    newRegion.setXMin((short) (x - 1));
                if (x >= newRegion.getXMax())
                    newRegion.setXMax((short) (x + 1));
                if (z <= newRegion.getZMin())
                    newRegion.setZMin((short) (z - 1));
                if (z >= newRegion.getZMax())
                    newRegion.setZMax((short) (z + 1));
            }
        }

        this.selectedZone.addRegion(newRegion);

        // Update UI.
        int newId = this.regionSelector.getItems().size();
        this.regionSelector.getItems().add(newId);
        this.regionSelector.getSelectionModel().select(newId);
    }

    @FXML
    private void deleteRegion(ActionEvent evt) {
        this.selectedZone.removeRegion(getCurrentRegion());
        this.regionSelector.getItems().remove(this.selectedRegion);
        for (int i = this.selectedRegion; i < this.regionSelector.getItems().size(); i++)
            this.regionSelector.getItems().set(i, i);

        int oldRegion = this.selectedRegion;
        if (--this.selectedRegion < 0)
            this.selectedRegion = 0;

        this.regionSelector.getSelectionModel().select(this.selectedRegion);
        if (oldRegion == this.selectedRegion) // Seems the change listener doesn't fire if there's no change, so we'll call it manually.
            onRegionSelected(this.selectedRegion);
    }

    @FXML
    private void onResizeGrid(ActionEvent evt) {
        closeWindow(); // Without this, the grid window will show on top of the resize window, making it impossible to see.
        FroggerGridResizeController.open(this);
        getMapMeshController().getGeneralManager().updatePlayerCharacter(); // Player character may have moved.
        Platform.runLater(this::openWindowAndWait);
    }

    /**
     * Gets the currently selected grid square layer.
     */
    public int getSelectedLayer() {
        Integer layer = this.layerSelector.getValue();
        return layer != null ? layer : -1;
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
     * Gets the highlight texture source for a given grid square.
     * @param gridSquare the grid square to get the highlight texture source for
     * @return highlightColorTextureSource
     */
    public RawColorTextureSource getOverlayColorTextureSource(FroggerGridSquare gridSquare) {
        if (gridSquare == null)
            return null;

        if (this.collisionGridPreview.getSelectedGridSquares().contains(gridSquare))
            return MATERIAL_HIGHLIGHT_GRID_AQUA;

        FroggerGridSquareReaction reaction = gridSquare.getReaction();
        return reaction != null ? reaction.getHighlightTextureSource() : MATERIAL_HIGHLIGHT_GRID_HOT_PINK;
    }

    /**
     * Set the visibility of grid polygon highlighting.
     * @param visible Whether it should be visible
     */
    public void setGridPolygonHighlightingVisible(boolean visible) {
        FroggerMapMesh mesh = this.mapMeshController.getMesh();
        mesh.pushBatchOperations();
        mesh.getHighlightedGridPolygonNode().clear();

        if (visible) {
            FroggerMapFilePacketGrid gridPacket = getMapFile().getGridPacket();
            for (int z = 0; z < gridPacket.getGridZCount(); z++) {
                for (int x = 0; x < gridPacket.getGridXCount(); x++) {
                    FroggerGridStack gridStack = gridPacket.getGridStack(x, z);
                    for (int i = 0; i < gridStack.getGridSquares().size(); i++) {
                        FroggerGridSquare gridSquare = gridStack.getGridSquares().get(i);
                        updateGridPolygonHighlighting(gridSquare, true);
                    }
                }
            }
        }

        mesh.popBatchOperations();
    }

    /**
     * Set the visibility of grid polygon highlighting.
     * @param showHighlighting Whether the grid highlighting should be visible
     */
    public void updateGridPolygonHighlighting(FroggerGridSquare gridSquare, boolean showHighlighting) {
        if (gridSquare == null)
            throw new NullPointerException("gridSquare");
        if (gridSquare.getPolygon() == null)
            return;

        // NOTE: Consider highlighting if the square is selected.

        FroggerMapMesh mesh = this.mapMeshController.getMesh();
        RawColorTextureSource highlightColor = showHighlighting ? getOverlayColorTextureSource(gridSquare) : null;
        DynamicMeshDataEntry polygonEntry = mesh.getMainNode().getDataEntry(gridSquare.getPolygon());
        mesh.getHighlightedGridPolygonNode().setOverlayTexture(polygonEntry, highlightColor);
    }

    /**
     * Updates the UI for the selected grid stack(s).
     */
    public void updateGridStackUI() {
        List<FroggerGridStack> selectedStacks = this.collisionGridPreview.getSelectedGridStacks();
        int gridStackCount = selectedStacks.size();

        // Update main UI components.
        this.gridStackSelectedLabel.setDisable(gridStackCount == 0);
        this.stackHeightLabel.setDisable(gridStackCount == 0);
        this.stackHeightField.setDisable(true); // This will work if enabled, but this should not be necessary to edit by hand.
        this.addGridSquareButton.setDisable(gridStackCount != 1);
        this.removeGridSquareButton.setDisable(gridStackCount == 0 || selectedStacks.stream().mapToInt(gridStack -> gridStack.getGridSquares().size()).sum() == 0);

        // Update stack display.
        if (gridStackCount > 1) {
            this.gridStackSelectedLabel.setText(gridStackCount + " stacks selected");
            if (sameHeight(selectedStacks)) {
                this.stackHeightField.setText(String.valueOf(selectedStacks.get(0).getCliffHeightAsFloat()));
            } else {
                this.stackHeightField.setText("");
            }
        } else if (gridStackCount == 1) {
            FroggerGridStack gridStack = selectedStacks.get(0);
            this.gridStackSelectedLabel.setText("Stack[x=" + gridStack.getX() + ",z=" + gridStack.getZ() + "]");
            this.stackHeightField.setText(String.valueOf(gridStack.getCliffHeightAsFloat()));
        }
    }

    /**
     * Updates the UI for the selected grid square.
     */
    public void updateGridSquareUI() {
        this.uiUpdateInProgressSquare = true;

        // Toggle visibility of square UI.
        Set<FroggerGridSquare> gridSquares = this.collisionGridPreview.getSelectedGridSquares();
        boolean hasGridSquare = gridSquares.size() > 0;
        this.gridSquareLabel.setVisible(hasGridSquare);
        this.selectedImage.setVisible(hasGridSquare);
        this.changePolygonButton.setVisible(hasGridSquare);
        this.changePolygonButton.setDisable(gridSquares.size() != 1);
        this.flagReactionSelector.setVisible(hasGridSquare);
        this.flagTable.setVisible(hasGridSquare);

        // Update image preview.
        FroggerGridSquare firstSquare = gridSquares.size() > 0 ? gridSquares.iterator().next() : null;
        FroggerGridSquare singleSquare = gridSquares.size() == 1 ? firstSquare : null;
        if (singleSquare != null && singleSquare.getPolygon() != null && singleSquare.getPolygon().getTexture() != null) {
            this.selectedImage.setImage(singleSquare.getPolygon().getTexture().toFXImage());
        } else {
            this.selectedImage.setImage(null);
        }

        // Update square UI. Must run last as it will set this.uiUpdateInProgressSquare to false.
        updateGridFlagsUI(true);
    }

    private void updateGridFlagsUI(boolean updateFlagSelector) {
        this.uiUpdateInProgressSquare = true;
        Set<FroggerGridSquare> gridSquares = this.collisionGridPreview.getSelectedGridSquares();
        FroggerGridSquare firstSquare = gridSquares.size() > 0 ? gridSquares.iterator().next() : null;

        // Determine reaction.
        FroggerGridSquareReaction firstReaction = firstSquare != null ? firstSquare.getReaction() : null;
        boolean reactionsMatch = firstSquare != null && gridSquares.stream()
                .map(FroggerGridSquare::getReaction)
                .allMatch(reaction -> reaction == firstReaction);
        FroggerGridSquareReaction sharedReaction = reactionsMatch ? firstReaction : null;
        if (updateFlagSelector)
            this.flagReactionSelector.getSelectionModel().select(sharedReaction);

        int i = 0;
        final int columnCount = 2;
        for (FroggerGridSquareFlag flag : FroggerGridSquareFlag.values()) {
            if (!flag.isLandGridData())
                continue;

            CheckBox checkBox = this.gridSquareFlagCheckBoxes[flag.ordinal()];
            boolean isFlagSet = firstSquare != null && firstSquare.testFlag(flag);
            boolean statesMatch = gridSquares.stream()
                    .map(square -> square.testFlag(flag))
                    .allMatch(flagState -> flagState == isFlagSet);

            if (checkBox == null) {
                // Create new.
                CheckBox newCheckBox = new CheckBox(StringUtils.capitalize(flag.name()) + (flag.isUnused() ? " (Unused)" : ""));
                newCheckBox.setAllowIndeterminate(false);
                newCheckBox.setTooltip(FXUtils.createTooltip(flag.getTooltipDescription()));

                GridPane.setRowIndex(newCheckBox, i / columnCount);
                GridPane.setColumnIndex(newCheckBox, i++ % columnCount);
                this.flagTable.getChildren().add(newCheckBox);
                this.gridSquareFlagCheckBoxes[flag.ordinal()] = newCheckBox;
                newCheckBox.selectedProperty().addListener(((observable, oldValue, newValue) -> {
                    if (newCheckBox.isIndeterminate() || this.uiUpdateInProgressSquare)
                        return;

                    this.collisionGridPreview.getMapMesh().pushBatchOperations();
                    for (FroggerGridSquare gridSquare : this.collisionGridPreview.getSelectedGridSquares()) {
                        gridSquare.setFlag(flag, newValue);
                        updateGridPolygonHighlighting(gridSquare, true);
                    }

                    this.collisionGridPreview.getMapMesh().popBatchOperations();
                }));

                checkBox = newCheckBox;
            }

            // Update check box.
            // To avoid changing the flags, we're going to enable indeterminate mode.
            checkBox.setVisible(gridSquares.size() > 0);
            checkBox.setDisable(flag.isPartOfSimpleReaction() && sharedReaction != null && this.flagReactionSelector.getValue() != null);
            checkBox.setIndeterminate(true); // Ensure updates to the grid square will be skipped.
            if (statesMatch) {
                checkBox.setSelected(isFlagSet);
                checkBox.setIndeterminate(false); // We do not want the indeterminate state after changing the selection state.
            } else {
                checkBox.setSelected(false);
            }
        }

        this.uiUpdateInProgressSquare = false;

        // Update polygon label.
        int layerID = firstSquare != null ? firstSquare.getLayerID() : -1;
        boolean layersMatch = firstSquare != null && gridSquares.stream()
                .allMatch(square -> square.getLayerID() == layerID);
        String layerText = layersMatch ? "Layer #" + (layerID + 1) : null;

        FroggerMapPolygonType polygonType = firstSquare != null && firstSquare.getPolygon() != null
                ? firstSquare.getPolygon().getPolygonType() : null;
        boolean polygonTypesMatch = polygonType != null && gridSquares.stream()
                .allMatch(square -> square.getPolygon() == null || square.getPolygon().getPolygonType() == polygonType);
        String polygonTypeText = polygonTypesMatch ? polygonType.name() : null;

        // If we add more here, consider splitting this label into multiple labels which can be set independently.
        this.polygonTypeLabel.setVisible(polygonTypeText != null || layerText != null);
        this.polygonTypeLabel.setText((polygonTypeText != null ? polygonTypeText : "")
                +(polygonTypeText != null ? ", " : "") + (layerText != null ? layerText : ""));
    }

    private static boolean sameHeight(Collection<FroggerGridStack> stacks) {
        return stacks.stream()
                .mapToInt(FroggerGridStack::getRawCliffHeightValue)
                .distinct().count() == 1;
    }

    /**
     * Set the selected zone.
     * @param newZone The new zone to select.
     */
    public void setSelectedZone(FroggerMapZone newZone) {
        this.selectedZone = newZone;

        if (newZone != null) {
            List<Integer> regionIds = new ArrayList<>(Utils.getIntegerList(newZone.getRegionCount()));
            this.regionSelector.setItems(FXCollections.observableArrayList(regionIds));
            if (regionIds.size() > 0)
                this.regionSelector.getSelectionModel().selectFirst(); // Automatically calls setRegion
        }

        updateCameraZoneUI();
        this.collisionGridPreview.redrawEntireCanvas();
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
        FXUtils.setHandleKeyPress(textField, newText -> {
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
    private void onRegionSelected(int id) {
        this.selectedRegion = id;
        this.removeRegionButton.setDisable(id == 0 && (this.selectedZone == null || this.selectedZone.isBoundingRegionTreatedAsRegion()));
        this.collisionGridPreview.redrawEntireCanvas();
    }

    /**
     * Get the currently selected region.
     * @return currentRegion
     */
    public FroggerMapZoneRegion getCurrentRegion() {
        return this.selectedZone != null ? this.selectedZone.getRegions().get(this.selectedRegion) : null;
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
                if (this.collisionGridPreview.getSelectedGridStacks().contains(newStack))
                    newStacks.add(newStack);
            }
        }

        // Apply changes.
        updateLayerSelectorValues();
        this.collisionGridPreview.getMapMesh().pushBatchOperations();
        this.collisionGridPreview.clearSelection();
        newStacks.forEach(gridStack -> this.collisionGridPreview.selectGridStack(gridStack, false));
        this.collisionGridPreview.getMapMesh().popBatchOperations();
        updateGridStackUI();
        updateGridSquareUI();
    }

    /**
     * Open the collision grid viewer.
     * @param controller The mesh manager.
     */
    public static FroggerUIGridManager openGridEditor(FroggerMapMeshController controller) {
        FroggerUIGridManager newManager = new FroggerUIGridManager(controller);
        FXUtils.createWindowFromFXMLTemplate("window-edit-map-collision-grid", newManager, "Grid Editor", false);
        return newManager;
    }
}