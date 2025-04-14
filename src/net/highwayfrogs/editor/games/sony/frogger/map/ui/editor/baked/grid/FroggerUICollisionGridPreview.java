package net.highwayfrogs.editor.games.sony.frogger.map.ui.editor.baked.grid;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.paint.Color;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import net.highwayfrogs.editor.file.map.view.UnknownTextureSource;
import net.highwayfrogs.editor.games.psx.shading.PSXShadeTextureDefinition;
import net.highwayfrogs.editor.games.sony.frogger.map.FroggerMapFile;
import net.highwayfrogs.editor.games.sony.frogger.map.data.grid.FroggerGridSquare;
import net.highwayfrogs.editor.games.sony.frogger.map.data.grid.FroggerGridStack;
import net.highwayfrogs.editor.games.sony.frogger.map.data.zone.FroggerMapCameraZone;
import net.highwayfrogs.editor.games.sony.frogger.map.data.zone.FroggerMapZone;
import net.highwayfrogs.editor.games.sony.frogger.map.data.zone.FroggerMapZoneRegion;
import net.highwayfrogs.editor.games.sony.frogger.map.mesh.FroggerMapMesh;
import net.highwayfrogs.editor.games.sony.frogger.map.mesh.FroggerMapMeshController;
import net.highwayfrogs.editor.games.sony.frogger.map.mesh.FroggerMapPolygon;
import net.highwayfrogs.editor.games.sony.frogger.map.packets.FroggerMapFilePacketGeneral;
import net.highwayfrogs.editor.games.sony.frogger.map.packets.FroggerMapFilePacketGrid;
import net.highwayfrogs.editor.games.sony.frogger.map.ui.editor.baked.FroggerUIGridManager;
import net.highwayfrogs.editor.gui.InputManager;
import net.highwayfrogs.editor.gui.InputManager.MouseDragState;
import net.highwayfrogs.editor.gui.InputManager.MouseInputState;
import net.highwayfrogs.editor.gui.InputManager.MouseTracker;
import net.highwayfrogs.editor.gui.editor.BakedLandscapeUIManager;
import net.highwayfrogs.editor.gui.fxobject.FXDragListener;
import net.highwayfrogs.editor.gui.texture.ITextureSource;
import net.highwayfrogs.editor.gui.texture.atlas.AtlasTexture;
import net.highwayfrogs.editor.utils.FXUtils;
import net.highwayfrogs.editor.utils.Utils;

import java.util.*;
import java.util.function.BiConsumer;

/**
 * Manages the 2D collision preview UI shown in the collision grid editor.
 * Created by Kneesnap on 4/9/2025.
 */
@RequiredArgsConstructor
public class FroggerUICollisionGridPreview {
    @Getter @NonNull private final FroggerUIGridManager gridManager;
    @Getter @NonNull private final Canvas gridCanvas;
    private final Set<FroggerGridSquare> selectedGridSquares = new HashSet<>();
    private final List<FroggerGridStack> selectedGridStacks = new ArrayList<>();
    private final Set<FroggerGridSquare> unmodifiableSelectedGridSquares = Collections.unmodifiableSet(this.selectedGridSquares);
    private final List<FroggerGridStack> unmodifiableSelectedGridStacks = Collections.unmodifiableList(this.selectedGridStacks);
    private final ChangeListener<Boolean> shadingToggleListener = this::onShadingToggle;
    private final Map<ITextureSource, Image> cachedImageMap = new HashMap<>();
    private GraphicsContext graphics;
    private double tileWidth;
    private double tileHeight;
    private ZoneRegionCoordinateTarget regionEditTargetX = null;
    private ZoneRegionCoordinateTarget regionEditTargetZ = null;

    /**
     * Get a set containing the currently selected grid squares.
     */
    public Set<FroggerGridSquare> getSelectedGridSquares() {
        return this.unmodifiableSelectedGridSquares;
    }

    /**
     * Get a list containing the currently selected grid stacks.
     */
    public List<FroggerGridStack> getSelectedGridStacks() {
        return this.unmodifiableSelectedGridStacks;
    }

    /**
     * Gets the map file which the collision grid is edited for.
     */
    public FroggerMapFile getMapFile() {
        return this.gridManager.getMapFile();
    }

    /**
     * Gets the mesh displayed by the map viewer.
     */
    public FroggerMapMesh getMapMesh() {
        return this.gridManager.getMapMeshController().getMesh();
    }

    /**
     * Gets the InputManager used for the MeshController.
     */
    public InputManager getInputManager() {
        return this.gridManager.getInputManager();
    }

    /**
     * Initializes the canvas.
     */
    public void setupCanvas() {
        if (this.graphics != null) {
            redrawEntireCanvas();
            return; // Setup has already occurred.
        }

        // Setup the basics.
        this.graphics = this.gridCanvas.getGraphicsContext2D();

        // Setup the selection rectangle.
        FXDragListener dragSelection = new FXDragListener(this.gridCanvas);
        dragSelection.applyListenersToNode();
        dragSelection.setOnDragListener(this::handleMouseDrag);

        // Setup canvas.
        redrawEntireCanvas();

        this.gridManager.getInputManager().addKeyListener(KeyCode.UP, this::handleKeyPress);
        this.gridManager.getInputManager().addKeyListener(KeyCode.DOWN, this::handleKeyPress);
        this.gridManager.getInputManager().addKeyListener(KeyCode.LEFT, this::handleKeyPress);
        this.gridManager.getInputManager().addKeyListener(KeyCode.RIGHT, this::handleKeyPress);
    }

    /**
     * Called when the gridPreview is removed from the scene.
     */
    public void onSceneAdd() {
        this.gridManager.getMapMeshController().getCheckBoxEnablePsxShading().selectedProperty().removeListener(this.shadingToggleListener); // Just to be safe.
        this.gridManager.getShadingEnabledCheckBox().setDisable(!this.gridManager.getMapMeshController().getCheckBoxEnablePsxShading().isSelected());
        this.gridManager.getMapMeshController().getCheckBoxEnablePsxShading().selectedProperty().addListener(this.shadingToggleListener);
    }

    /**
     * Called when the grid preview is added to the scene.
     */
    public void onSceneRemove() {
        this.gridManager.getMapMeshController().getCheckBoxEnablePsxShading().selectedProperty().removeListener(this.shadingToggleListener);
    }

    private void onShadingToggle(ObservableValue<? extends Boolean> observable, boolean oldValue, boolean newValue) {
        // If shading is not enabled on the map, we can't pull from the shaded textures.
        this.gridManager.getShadingEnabledCheckBox().setDisable(!newValue);
        redrawEntireCanvas();
    }

    private void validateTileWidth() {
        if (!Double.isFinite(this.tileWidth) || this.tileWidth == 0)
            throw new IllegalStateException("tileWidth has not been setup yet!");
    }

    private void validateTileHeight() {
        if (!Double.isFinite(this.tileHeight) || this.tileHeight == 0)
            throw new IllegalStateException("tileHeight has not been setup yet!");
    }

    /**
     * Gets the gridX coordinate from the x pixel coordinate within the Canvas.
     * @param canvasX x pixel coordinate relative to the canvas origin
     * @return gridX
     */
    public int getGridXFromCanvasX(double canvasX) {
        validateTileWidth();
        return Math.max(0, Math.min(getMapFile().getGridPacket().getGridXCount() - 1, (int) (canvasX / this.tileWidth)));
    }

    /**
     * Gets the x pixel coordinate relative to the canvas origin to draw the grid stack at based on a gridX coordinate.
     * @param gridX the grid X coordinate to calculate canvas pixel position from
     * @return canvasX
     */
    public double getCanvasXFromGridX(int gridX) {
        FroggerMapFilePacketGrid gridPacket = getMapFile().getGridPacket();
        if (gridX < 0 || gridX >= gridPacket.getGridXCount())
            throw new IllegalArgumentException("Invalid gridX coordinate: " + gridX);

        validateTileWidth();
        return this.tileWidth * gridX;
    }

    /**
     * Gets the gridZ coordinate from the y pixel coordinate within the Canvas.
     * @param canvasY y pixel coordinate relative to the canvas origin
     * @return gridZ
     */
    public int getGridZFromCanvasY(double canvasY) {
        validateTileHeight();
        FroggerMapFilePacketGrid gridPacket = getMapFile().getGridPacket();
        return gridPacket.getGridZCount() - Math.max(0, Math.min(gridPacket.getGridZCount() - 1, (int) (canvasY / this.tileHeight))) - 1;
    }

    /**
     * Gets the y pixel coordinate relative to the canvas origin to draw the grid stack at based on a gridZ coordinate.
     * @param gridZ the grid Z coordinate to calculate canvas pixel position from
     * @return canvasY
     */
    public double getCanvasYFromGridZ(int gridZ) {
        FroggerMapFilePacketGrid gridPacket = getMapFile().getGridPacket();
        if (gridZ < 0 || gridZ >= gridPacket.getGridZCount())
            throw new IllegalArgumentException("Invalid gridZ coordinate: " + gridZ);

        validateTileHeight();
        return this.tileHeight * (gridPacket.getGridZCount() - gridZ - 1);
    }

    /**
     * Gets the currently selected grid square layer.
     */
    public int getMaxDisplayLayer() {
        Integer layer = this.gridManager.getLayerSelector().getValue();
        return layer != null ? layer : -1;
    }

    /**
     * Clears all selected grid tiles, so no more occurs.
     */
    public void clearSelection() {
        int oldStackCount = this.selectedGridStacks.size();
        int oldSquareCount = this.selectedGridSquares.size();
        this.selectedGridSquares.clear();

        // Clears grid stacks and updates the canvas.
        for (int i = this.selectedGridStacks.size() - 1; i >= 0; i--) {
            FroggerGridStack gridStack = this.selectedGridStacks.remove(i);
            updateCanvasTile(gridStack.getX(), gridStack.getZ());

            // Remove grid square selection highlighting.
            for (int j = 0; j < gridStack.getGridSquares().size(); j++) {
                FroggerGridSquare gridSquare = gridStack.getGridSquares().get(j);
                this.gridManager.updateGridPolygonHighlighting(gridSquare, true);
            }
        }

        if (oldStackCount > 0)
            this.gridManager.updateGridStackUI();
        if (oldSquareCount > 0)
            this.gridManager.updateGridSquareUI();
    }

    /**
     * Deselects the provided grid stack.
     * Does nothing if the stack was not selected.
     * @param gridStack the stack to deselect
     * @param allowUIUpdate if true and a chance occurs, the impacted 2D UI will be updated.
     * @return true iff the grid stack was successfully deselected
     */
    public boolean deselectGridStack(FroggerGridStack gridStack, boolean allowUIUpdate) {
        if (gridStack == null)
            throw new NullPointerException("gridStack");

        if (!this.selectedGridStacks.remove(gridStack))
            return false; // Wasn't registered.

        // Removes corresponding grid squares.
        boolean deselectedSquare = false;
        for (int i = 0; i < gridStack.getGridSquares().size(); i++)
            if (deselectGridSquare(gridStack.getGridSquares().get(i), false))
                deselectedSquare = true;

        // No need to update canvas or 3D preview, as it was already called by deselectGridSquare().
        if (allowUIUpdate) {
            this.gridManager.updateGridStackUI();
            if (deselectedSquare)
                this.gridManager.updateGridSquareUI();
        }

        return true;
    }

    /**
     * Deselects the provided grid square.
     * Does nothing if the square was not selected.
     * @param gridSquare the square to deselect
     * @param allowUIUpdate if true and a chance occurs, the impacted 2D UI will be updated.
     * @return true iff the grid square was successfully deselected
     */
    public boolean deselectGridSquare(FroggerGridSquare gridSquare, boolean allowUIUpdate) {
        if (gridSquare == null)
            throw new NullPointerException("gridSquare");

        if (!this.selectedGridSquares.remove(gridSquare))
            return false; // Wasn't registered.

        // Removes corresponding grid squares.
        FroggerGridStack gridStack = gridSquare.getGridStack();
        boolean foundOtherSquares = false;
        for (int i = 0; i < gridStack.getGridSquares().size(); i++) {
            FroggerGridSquare testSquare = gridStack.getGridSquares().get(i);
            if (gridSquare != testSquare && this.selectedGridSquares.contains(testSquare)) {
                foundOtherSquares = true;
                break;
            }
        }

        if (!foundOtherSquares) { // No other squares are selected for the stack, so deselect the stack.
            this.selectedGridStacks.remove(gridStack);
            if (allowUIUpdate)
                this.gridManager.updateGridStackUI();
        }

        this.gridManager.updateGridPolygonHighlighting(gridSquare, true);
        updateCanvasTile(gridStack.getX(), gridStack.getZ());
        if (allowUIUpdate)
            this.gridManager.updateGridSquareUI();

        return true;
    }

    /**
     * Selects the grid stack at the given grid coordinates.
     * Does nothing if the stack is already selected.
     * @param gridX the grid X coordinate
     * @param gridZ the grid Z coordinate.
     * @param allowUIUpdate if true and a chance occurs, the impacted 2D UI will be updated.
     */
    public void selectGridStack(int gridX, int gridZ, boolean allowUIUpdate) {
        selectGridStack(getMapFile().getGridPacket().getGridStack(gridX, gridZ), allowUIUpdate);
    }

    /**
     * Selects the provided grid stack.
     * Does nothing if the stack is already selected.
     * @param gridStack the stack to select
     * @param allowUIUpdate if true and a chance occurs, the impacted 2D UI will be updated.
     * @return true iff the grid stack was successfully selected
     */
    public boolean selectGridStack(FroggerGridStack gridStack, boolean allowUIUpdate) {
        if (gridStack == null)
            throw new NullPointerException("gridStack");

        if (this.selectedGridStacks.contains(gridStack))
            return false;

        this.selectedGridStacks.add(gridStack);
        FroggerGridSquare selectedSquare = getDisplaySquare(gridStack);
        if (selectedSquare != null) {
            // No need to update 3D preview, as that was done by selectGridSquare() if it needed to occur. (Only grid squares need updating, not stacks)
            // The canvas tile will be updated by selecting the square.
            selectGridSquare(selectedSquare, allowUIUpdate);
        } else {
            // No need to update 3D preview, as that was done by selectGridSquare() if it needed to occur. (Only grid squares need updating, not stacks)
            updateCanvasTile(gridStack.getX(), gridStack.getZ());
        }

        if (allowUIUpdate)
            this.gridManager.updateGridStackUI();

        return true;
    }

    /**
     * Selects the provided grid stack.
     * Does nothing if the stack is already selected.
     * @param gridSquare the stack to select
     * @param allowUIUpdate if true and a chance occurs, the impacted 2D UI will be updated.
     * @return true iff the grid square was successfully selected
     */
    public boolean selectGridSquare(FroggerGridSquare gridSquare, boolean allowUIUpdate) {
        if (gridSquare == null)
            throw new NullPointerException("gridSquare");

        if (!this.selectedGridSquares.add(gridSquare))
            return false; // Already contained.

        FroggerGridStack gridStack = gridSquare.getGridStack();
        if (!this.selectedGridStacks.contains(gridStack)) {
            this.selectedGridStacks.add(gridStack);
            if (allowUIUpdate)
                this.gridManager.updateGridStackUI();
        }

        // Update canvas tile iff the added grid square is displayed.
        FroggerGridSquare displaySquare = getDisplaySquare(gridStack);
        if (displaySquare == gridSquare)
            updateCanvasTile(gridStack.getX(), gridStack.getZ());

        this.gridManager.updateGridPolygonHighlighting(gridSquare, true);
        if (allowUIUpdate)
            this.gridManager.updateGridSquareUI();
        return true;
    }

    /**
     * Redraw the entire canvas.
     */
    public void redrawEntireCanvas() {
        FroggerMapFilePacketGrid gridPacket = getMapFile().getGridPacket();
        this.tileWidth = this.gridCanvas.getWidth() / gridPacket.getGridXCount();
        this.tileHeight = this.gridCanvas.getHeight() / gridPacket.getGridZCount();

        this.graphics.clearRect(0, 0, this.gridCanvas.getWidth(), this.gridCanvas.getHeight());

        for (int z = 0; z < gridPacket.getGridZCount(); z++)
            for (int x = 0; x < gridPacket.getGridXCount(); x++)
                updateCanvasTile(x, z);
    }

    /**
     * Gets the grid square to display for the given stack.
     * @param gridStack the stack to get the display square from
     * @return displaySquare, or null if there is none.
     */
    public FroggerGridSquare getDisplaySquare(FroggerGridStack gridStack) {
        if (gridStack == null)
            throw new NullPointerException("gridStack");

        FroggerGridSquare lastSquare = null;
        for (int i = Math.min(getMaxDisplayLayer(), gridStack.getGridSquares().size() - 1); i >= 0; i--) {
            lastSquare = gridStack.getGridSquares().get(i);
            if (lastSquare.getPolygon() != null)
                return lastSquare;
        }

        return lastSquare;
    }

    /**
     * Updates the tile in the canvas at the given grid coordinates.
     * @param gridX the grid x coordinate to update
     * @param gridZ the grid z coordinate to update
     */
    public void updateCanvasTile(int gridX, int gridZ) {
        validateTileWidth();
        validateTileHeight();

        FroggerMapFilePacketGrid gridPacket = getMapFile().getGridPacket();
        FroggerGridStack gridStack = gridPacket.getGridStack(gridX, gridZ);
        FroggerMapZoneRegion currentRegion = this.gridManager.getCurrentRegion();
        double xPos = getCanvasXFromGridX(gridX);
        double yPos = getCanvasYFromGridZ(gridZ);

        FroggerMapFilePacketGeneral generalPacket = getMapFile().getGeneralPacket();
        Color outlineColor = generalPacket.getStartGridCoordX() == gridX && generalPacket.getStartGridCoordZ() == gridZ ? Color.RED : Color.BLACK;

        Color highlightColor = null;
        FroggerMapCameraZone zone;
        if (currentRegion != null && currentRegion.contains(gridX, gridZ)) {
            highlightColor = BakedLandscapeUIManager.POLYGON_HIGHLIGHT_COLOR;
            if (this.gridManager.getRegionEditorCheckBox().isSelected() && (currentRegion.isMinCorner(gridX, gridZ) || currentRegion.isMaxCorner(gridX, gridZ)))
                outlineColor = Color.YELLOW;
        } else if ((zone = getMapFile().getZonePacket().getCameraZone(gridX, gridZ)) != null && (this.gridManager.getHighlightZonesCheckBox().isSelected() || (currentRegion != null && zone == currentRegion.getParentZone()))) {
            if (currentRegion != null && zone == currentRegion.getParentZone()) {
                highlightColor = FroggerUIGridManager.MATERIAL_HIGHLIGHT_GRID_GOLD.getFxColor();
            } else {
                highlightColor = FroggerUIGridManager.MATERIAL_HIGHLIGHT_GRID_ORANGE.getFxColor();
            }
        }

        // Find best square, and if a square is found, the selection highlighting will be based on it.
        FroggerMapPolygon polygon;
        FroggerGridSquare gridSquare = getDisplaySquare(gridStack);
        if (gridSquare != null) {
            polygon = gridSquare.getPolygon();
            if (this.selectedGridSquares.contains(gridSquare)) // Only highlight when the specific square is selected.
                highlightColor = FroggerUIGridManager.MATERIAL_HIGHLIGHT_GRID_AQUA.getFxColor();
        } else {
            polygon = null;
            if (this.selectedGridStacks.contains(gridStack)) // Highlight if the stack is selected.
                highlightColor = FroggerUIGridManager.MATERIAL_HIGHLIGHT_GRID_AQUA.getFxColor();
        }

        if (polygon != null) {
            FroggerMapMeshController mapMeshController = this.gridManager.getMapMeshController();
            Image shadedTextureAtlas = mapMeshController.getMesh().getMaterialFxImage();
            boolean shadingEnabled = this.gridManager.getShadingEnabledCheckBox().isSelected();

            if (!polygon.getPolygonType().isTextured() || (shadingEnabled && mapMeshController.getCheckBoxEnablePsxShading().isSelected())) {
                PSXShadeTextureDefinition textureDefinition = mapMeshController.getMesh().getShadedTextureManager().getShadedTexture(polygon);
                AtlasTexture texture = mapMeshController.getMesh().getTextureAtlas().getTextureFromSourceOrFallback(textureDefinition);
                // TODO: STRETCH THE IMAGE. (Both here and below. I'm confident this is possible using the same method the PSXTextureShader uses to draw triangles)
                //  - We find the minimum X, Y, Z vertex, and since we know which vertices are connected, we can use that to deterministically form which vertex goes to each corner in a rectangle.
                //  - Then, we use scanline interpolation (left to right) drawing lines on the image to scale it to be a quad.
                this.graphics.drawImage(shadedTextureAtlas, texture.getX() + texture.getLeftPadding(), texture.getY() + texture.getUpPadding(), texture.getWidthWithoutPadding(), texture.getHeightWithoutPadding(), xPos, yPos, this.tileWidth - 1, this.tileHeight - 1);
            } else {
                ITextureSource textureSource = polygon.getTexture();
                if (textureSource == null)
                    textureSource = UnknownTextureSource.MAGENTA_INSTANCE;

                Image fxImage = this.cachedImageMap.computeIfAbsent(textureSource, texSource -> FXUtils.toFXImage(texSource.makeImage(), false));
                this.graphics.drawImage(fxImage, textureSource.getLeftPadding(), textureSource.getUpPadding(), textureSource.getUnpaddedWidth(), textureSource.getUnpaddedHeight(), xPos, yPos, this.tileWidth - 1, this.tileHeight - 1);
            }
        } else { // No texture was available to draw, so fill the tile with gray.
            this.graphics.setFill(Color.GRAY);
            this.graphics.fillRect(xPos, yPos, this.tileWidth - 1, this.tileHeight - 1);
        }

        // Apply highlight.
        if (highlightColor != null) {
            this.graphics.setFill(highlightColor);
            this.graphics.fillRect(xPos, yPos, this.tileWidth - 1, this.tileHeight - 1);
        }

        drawOutline(xPos, yPos, outlineColor);
    }

    private void drawOutline(double xPos, double yPos, Color outlineColor) {
        this.graphics.setStroke(outlineColor);
        this.graphics.strokeRect(xPos, yPos, this.tileWidth - 1, this.tileHeight - 1);
    }

    private void handleKeyPress(InputManager manager, KeyEvent event) {
        if (!KeyEvent.KEY_PRESSED.equals(event.getEventType()) && !KeyEvent.KEY_TYPED.equals(event.getEventType()))
            return;

        int deltaX = 0, deltaZ = 0;
        switch (event.getCode()) {
            case LEFT:
                deltaX = -1;
                break;
            case RIGHT:
                deltaX = 1;
                break;
            case UP:
                deltaZ = 1;
                break;
            case DOWN:
                deltaZ = -1;
                break;
            default: // Unsupported key press, abort!
                return;
        }

        if (moveSelectionRelative(deltaX, deltaZ))
            event.consume();
    }

    /**
     * Move the current grid selection relative to its current position.
     * @param deltaX the number of squares to move in the x direction
     * @param deltaZ the number of squares to move in the z direction
     */
    @SuppressWarnings("ExtractMethodRecommender")
    public boolean moveSelectionRelative(int deltaX, int deltaZ) {
        if (deltaX == 0 && deltaZ == 0)
            return false;

        FroggerMapFilePacketGrid gridPacket = getMapFile().getGridPacket();

        // Ensure all selected stacks are available to be moved.
        for (FroggerGridStack stack : this.selectedGridStacks) {
            int newX = stack.getX() + deltaX;
            int newZ = stack.getZ() + deltaZ;
            if (newX < 0 || newZ < 0 || newX >= gridPacket.getGridXCount() || newZ >= gridPacket.getGridZCount())
                return false; // The stacks would be moved outside the appropriate realm.
        }

        // Find the grid squares to newly become selected.
        Set<FroggerGridStack> newGridStacks = new HashSet<>();
        Set<FroggerGridSquare> newGridSquares = new HashSet<>();
        for (int i = 0; i < this.selectedGridStacks.size(); i++) {
            FroggerGridStack oldGridStack = this.selectedGridStacks.get(i);
            FroggerGridStack newGridStack = gridPacket.getGridStack(oldGridStack.getX() + deltaX, oldGridStack.getZ() + deltaZ);
            newGridStacks.add(newGridStack);

            // If all tiles in the previous one were selected, select them in the new one.
            boolean allOldSquaresSelected = true;
            for (int j = 0; j < oldGridStack.getGridSquares().size() && allOldSquaresSelected; j++)
                if (!this.selectedGridSquares.contains(oldGridStack.getGridSquares().get(j)))
                    allOldSquaresSelected = false;

            if (allOldSquaresSelected) {
                newGridSquares.addAll(newGridStack.getGridSquares());
                oldGridStack.getGridSquares().forEach(this.selectedGridSquares::remove); // Don't need to try and select these squares later.
            }
        }

        for (FroggerGridSquare gridSquare : this.selectedGridSquares) {
            FroggerGridStack oldGridStack = gridSquare.getGridStack();
            FroggerGridStack newGridStack = gridPacket.getGridStack(oldGridStack.getX() + deltaX, oldGridStack.getZ() + deltaZ);
            if (oldGridStack.getGridSquares().size() == newGridStack.getGridSquares().size()) {
                int layer = gridSquare.getLayerID();
                if (layer >= 0)
                    newGridSquares.add(newGridStack.getGridSquares().get(layer));
            } else {
                // Find the best square based on shared vertices.
                FroggerGridSquare bestSquare = null;
                FroggerGridSquare highestSquare = null;
                int bestOptionVerticesShared = 0;
                FroggerMapPolygon oldPolygon = gridSquare.getPolygon();
                for (int i = 0; i < newGridStack.getGridSquares().size(); i++) {
                    FroggerGridSquare testSquare = newGridStack.getGridSquares().get(i);
                    if (newGridSquares.contains(testSquare))
                        continue; // Already matched.

                    highestSquare = testSquare;

                    FroggerMapPolygon testPolygon = testSquare.getPolygon();
                    if ((testPolygon != oldPolygon) && (oldPolygon == null || testPolygon == null))
                        continue;

                    int verticesShared = 0;
                    for (int j = 0; j < testPolygon.getVertexCount(); j++)
                        if (Utils.indexOf(oldPolygon.getVertices(), testPolygon.getVertices()[j]) >= 0)
                            verticesShared++;

                    if (verticesShared > bestOptionVerticesShared) {
                        bestSquare = testSquare;
                        bestOptionVerticesShared = verticesShared;
                    }
                }

                // If no square was found, pick the highest currently unused square.
                if (bestSquare == null)
                    bestSquare = highestSquare;

                if (bestSquare != null)
                    newGridSquares.add(bestSquare);
            }
        }

        // Apply new selections.
        getMapMesh().pushBatchOperations();
        clearSelection();
        this.selectedGridStacks.addAll(newGridStacks);
        this.selectedGridSquares.addAll(newGridSquares);
        for (FroggerGridStack gridStack : newGridStacks)
            updateCanvasTile(gridStack.getX(), gridStack.getZ());

        // Update 3D highlighting.
        for (FroggerGridSquare gridSquare : newGridSquares)
            this.gridManager.updateGridPolygonHighlighting(gridSquare, true);

        getMapMesh().popBatchOperations();
        this.gridManager.updateGridStackUI(); // Update stack currently shown.
        this.gridManager.updateGridSquareUI(); // Update grid square UI.
        return true;
    }

    // Allows selecting grid squares with a selector.
    private void handleMouseDrag(MouseTracker dragData) {
        this.gridCanvas.requestFocus(); // Request focus so arrow keys will work.

        MouseInputState startPos = dragData.getLastDragStartMouseState();
        MouseInputState endPos = dragData.getMouseState();
        boolean isControlPressed = getInputManager().isKeyPressed(KeyCode.CONTROL);
        int startGridX = getGridXFromCanvasX(startPos.getX());
        int startGridZ = getGridZFromCanvasY(startPos.getY());
        int endGridX = getGridXFromCanvasX(endPos.getX());
        int endGridZ = getGridZFromCanvasY(endPos.getY());

        FroggerMapZoneRegion currentRegion = this.gridManager.getCurrentRegion();
        if (this.gridManager.getRegionEditorCheckBox().isSelected() && currentRegion != null && dragData.getDragState() != MouseDragState.START) {
            // Determine the corner we're editing.
            if (this.regionEditTargetX == null || this.regionEditTargetZ == null) {
                if (currentRegion.isMinCorner(startGridX, startGridZ)) {
                    this.regionEditTargetX = this.regionEditTargetZ = ZoneRegionCoordinateTarget.MINIMUM;
                } else if (currentRegion.isMaxCorner(startGridX, startGridZ)) {
                    this.regionEditTargetX = this.regionEditTargetZ = ZoneRegionCoordinateTarget.MAXIMUM;
                }
            }

            // If we're actively editing some coordinates...
            if (this.regionEditTargetX != null && this.regionEditTargetZ != null) {
                FroggerMapZone currentZone = this.gridManager.getSelectedZone();

                this.regionEditTargetX.setX(currentRegion, (short) endGridX);
                if (currentRegion.swapXIfNecessary())
                    this.regionEditTargetX = this.regionEditTargetX.getOther();

                this.regionEditTargetZ.setZ(currentRegion, (short) endGridZ);
                if (currentRegion.swapZIfNecessary())
                    this.regionEditTargetZ = this.regionEditTargetZ.getOther();

                currentZone.updateMainBoundingBox();
                redrawEntireCanvas();
                if (dragData.getDragState() == MouseDragState.COMPLETE)
                    this.regionEditTargetX = this.regionEditTargetZ = null;

                return;
            }
        }

        // The upcoming mouse drag selection logic should only run once we're confident the mouse is being dragged, and isn't just a regular single-click.
        if (startGridX == endGridX && startGridZ == endGridZ) {
            if (dragData.getDragState() == MouseDragState.START) {
                handleSingleGridSquareClick(startGridX, startGridZ, isControlPressed);
                return; // On the first click, we don't want to activate the drag logic.
            } else if (!dragData.isSignificantMouseDragRecorded()) {
                return; // If there has not been a significant mouse movement yet, we are not yet treating it as a drag, and return to avoid conflicting with the single grid square click behavior called earlier.
            }
        }

        // Add everything between the start tile and the end tile to the selection, first clearing the selection if control is not held.
        if (!isControlPressed)
            clearSelection();

        int minGridX = Math.min(startGridX, endGridX);
        int minGridZ = Math.min(startGridZ, endGridZ);
        int maxGridX = Math.max(startGridX, endGridX);
        int maxGridZ = Math.max(startGridZ, endGridZ);
        for (int x = minGridX; x <= maxGridX; x++)
            for (int z = maxGridZ; z >= minGridZ; z--)
                selectGridStack(x, z, false);

        this.gridManager.updateGridStackUI();
        this.gridManager.updateGridSquareUI();
    }

    private void handleSingleGridSquareClick(int gridX, int gridZ, boolean isControlPressed) {
        FroggerGridStack gridStack = getMapFile().getGridPacket().getGridStack(gridX, gridZ);
        if (gridStack == null)
            return;

        if (isControlPressed) { // Toggle grid stacks one at a time.
            FroggerGridSquare gridSquare = getDisplaySquare(gridStack);
            if (gridSquare != null) {
                if (!deselectGridSquare(gridSquare, true))
                    selectGridSquare(gridSquare, true);
            } else {
                if (!deselectGridStack(gridStack, true))
                    selectGridStack(gridStack, true);
            }
        } else {
            clearSelection();
            selectGridStack(gridStack, true);
        }

        // Selects zones while highlighted.
        FroggerMapZone currentZone = this.gridManager.getActiveCameraZone();
        boolean showAllZones = this.gridManager.getHighlightZonesCheckBox().isSelected();
        if (showAllZones || currentZone != null) {
            FroggerMapZone newZone;
            if (showAllZones && (currentZone == null || !currentZone.contains(gridX, gridZ))) {
                newZone = getMapFile().getZonePacket().getCameraZone(gridX, gridZ);
            } else {
                newZone = currentZone;
            }

            if (newZone != null) {
                if (currentZone != newZone)
                    this.gridManager.getZoneSelector().getSelectionModel().select(newZone);

                FroggerMapZoneRegion currentRegion = this.gridManager.getCurrentRegion();
                if (currentRegion == null || !currentRegion.contains(gridX, gridZ)) {
                    FroggerMapZoneRegion newRegion = newZone.getRegion(gridX, gridZ);
                    int index = newRegion != null ? newRegion.getRegionIndex() : -1;
                    if (index >= 0)
                        this.gridManager.getRegionSelector().getSelectionModel().select(index);
                }
            }
        }
    }

    @RequiredArgsConstructor
    private enum ZoneRegionCoordinateTarget {
        MINIMUM(FroggerMapZoneRegion::setXMin, FroggerMapZoneRegion::setZMin),
        MAXIMUM(FroggerMapZoneRegion::setXMax, FroggerMapZoneRegion::setZMax);

        private final BiConsumer<FroggerMapZoneRegion, Short> xSetter;
        private final BiConsumer<FroggerMapZoneRegion, Short> zSetter;

        /**
         * Gets the other ZoneRegionCoordinateTarget
         */
        public ZoneRegionCoordinateTarget getOther() {
            switch (this) {
                case MINIMUM:
                    return MAXIMUM;
                case MAXIMUM:
                    return MINIMUM;
                default:
                    throw new UnsupportedOperationException("Unsupported ZoneRegionCoordinateTarget: " + this);
            }
        }

        /**
         * Sets the region's X value.
         * @param region the region to apply the x value to
         */
        public void setX(@NonNull FroggerMapZoneRegion region, short newX) {
            this.xSetter.accept(region, newX);
        }

        /**
         * Sets the region's Z value.
         * @param region the region to apply the z value to
         */
        public void setZ(@NonNull FroggerMapZoneRegion region, short newZ) {
            this.zSetter.accept(region, newZ);
        }
    }
}
