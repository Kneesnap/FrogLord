package net.highwayfrogs.editor.games.sony.oldfrogger.map.ui;

import javafx.geometry.Orientation;
import javafx.geometry.Point2D;
import javafx.geometry.Point3D;
import javafx.scene.control.Separator;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.PickResult;
import javafx.scene.layout.VBox;
import javafx.scene.shape.CullFace;
import javafx.scene.shape.DrawMode;
import javafx.scene.shape.MeshView;
import javafx.scene.shape.Rectangle;
import javafx.scene.transform.Scale;
import javafx.scene.transform.Translate;
import lombok.Getter;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.mesh.OldFroggerMapMesh;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.mesh.camera.CameraHeightFieldMesh;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.packet.OldFroggerMapCameraHeightFieldPacket;
import net.highwayfrogs.editor.gui.GUIEditorGrid;
import net.highwayfrogs.editor.gui.InputManager;
import net.highwayfrogs.editor.gui.InputManager.MouseInputState;
import net.highwayfrogs.editor.gui.editor.DisplayList;
import net.highwayfrogs.editor.gui.editor.FirstPersonCamera;
import net.highwayfrogs.editor.gui.editor.MeshViewController;
import net.highwayfrogs.editor.gui.mesh.fxobject.TranslationGizmo;
import net.highwayfrogs.editor.gui.mesh.fxobject.TranslationGizmo.IPositionChangeListener;
import net.highwayfrogs.editor.system.math.Vector3f;
import net.highwayfrogs.editor.utils.Scene3DUtils;
import net.highwayfrogs.editor.utils.Utils;

import java.util.ArrayList;
import java.util.List;

/**
 * Manages editing of camera height-field data.
 * TODO:
 *  - Allow a cylindrical update. Eg: Allow entering a mode where our selected vertex / vertices are treated as the centers of circles.
 *   - Allow choosing a radius of vertices to highlight, which will fall off in terms of color intensity.
 *   - Pulling the height of the vertex upwards / downward will pull all of the vertices.
 *   - The radius controls which vertices are impacted, although I'm less certain about
 * Created by Kneesnap on 12/25/2023.
 */
public class OldFroggerCameraHeightFieldManager extends OldFroggerMapUIManager {
    private final IPositionChangeListener positionChangeListener;
    private GUIEditorGrid editorGrid;
    private DisplayList verticeDisplayList;
    private CameraHeightFieldMesh mesh;
    private MeshView meshView;
    private SelectedVertex[][] selectedVerticesGrid;
    private final List<SelectedVertex> selectedVertices = new ArrayList<>();
    private TranslationGizmo vertexTranslationGizmo;
    private Rectangle selectionArea;
    private boolean rectangleSelectionActive;

    private static final Scale TRANSLATION_GIZMO_SCALE = new Scale(.6F, .25F, .6F);

    public OldFroggerCameraHeightFieldManager(MeshViewController<OldFroggerMapMesh> controller) {
        super(controller);
        this.positionChangeListener = this::onVertexMoved;
    }

    @Override
    public void onSetup() {
        super.onSetup();

        // Setup Basics.
        OldFroggerMapCameraHeightFieldPacket packet = getMap().getCameraHeightFieldPacket();
        this.verticeDisplayList = getRenderManager().createDisplayListWithNewGroup();
        this.selectedVerticesGrid = new SelectedVertex[packet.getZSquareCount()][packet.getXSquareCount()];

        // Setup translation gizmo.
        this.vertexTranslationGizmo = new TranslationGizmo(false, true, false);

        // Unchanging UI Fields
        VBox editorBox = this.getController().makeAccordionMenu("Camera Height Field");
        GUIEditorGrid mainGrid = getController().makeEditorGrid(editorBox);
        mainGrid.addCheckBox("Preview Visible", false, visible -> {
            this.verticeDisplayList.setVisible(visible);
            this.meshView.setVisible(visible);
            updateEditor();
        });

        mainGrid.addCheckBox("Wireframe", false, wireframe ->
                this.meshView.setDrawMode(wireframe ? DrawMode.LINE : DrawMode.FILL));

        // Setup packet data editor.
        packet.setupEditor(this, mainGrid);

        // Separator, and grid setup.
        editorBox.getChildren().add(new Separator(Orientation.HORIZONTAL));
        this.editorGrid = this.getController().makeEditorGrid(editorBox);

        // Create the height-field display mesh.
        this.mesh = new CameraHeightFieldMesh(this);
        this.meshView = new MeshView();
        this.mesh.addView(this.meshView);
        this.meshView.setVisible(false);
        this.meshView.setCullFace(CullFace.NONE);
        getController().getLightManager().getLightingGroup().getChildren().add(this.meshView);

        // Future:
        // Right now we're just applying the level lights.
        // JavaFX 18 supports directional (parallel) lighting.
        // If we're ever able to upgrade, let's change the lighting here maybe.
        // We can have an ambient light of 0x555555, and a parallel light from above pointing straight down of 0x999999.
        // This should do a good job of highlighting curves.
        // The one drawback to this is that I worry we might need to start including normals in all of our meshes for this to work. Ideally those can be auto-generated by JavaFX though.

        // Handle mouse clicks and such in the mesh preview.
        this.meshView.setOnMouseClicked(evt -> {
            int meshIndex = getClosestVertex(evt.getPickResult());
            if (meshIndex < 0)
                return;

            // Handle vertex click.
            int gridX = this.mesh.getMainNode().getGridX(meshIndex);
            int gridZ = this.mesh.getMainNode().getGridZ(meshIndex);
            onClickVertex(gridX, gridZ);
        });

        // Listen for arrow key presses to move selected vertices a relative amount.
        InputManager input = getController().getInputManager();
        input.addKeyListener(KeyCode.UP, (manager, event) -> moveSelectionRelative(event, 0, 1));
        input.addKeyListener(KeyCode.DOWN, (manager, event) -> moveSelectionRelative(event, 0, -1));
        input.addKeyListener(KeyCode.LEFT, (manager, event) -> moveSelectionRelative(event, -1, 0));
        input.addKeyListener(KeyCode.RIGHT, (manager, event) -> moveSelectionRelative(event, 1, 0));

        // Displays are hidden by default.
        this.verticeDisplayList.setVisible(false);
        this.meshView.setVisible(false);

        // TODO: When doing spherical calculations, replace Sphere in the displayGrid with some class that can also hold a strength decimal value.
        // A selected vertex has a strength of 1, a vertex without any impact has a strength of zero.
        // TODO: This strength is how we'll do spherical calculations. Also, when doing a spherical check, check a vertex in each of the four cardinal directions. For each vertex found, you can know to skip vertices in that direction, since your influence on those vertices would be less than that sphere.

        // TODO: Should we generalize this to be something agnostic, so we don't have to type the full thing each time? Like we could make this a class.
        // TODO: Also let's generalize the creation of an axis plane.
        this.selectionArea = new Rectangle(5, 5, 25, 25);
        this.selectionArea.setMouseTransparent(true);
        this.selectionArea.setStyle("-fx-fill: rgb(200,200,0); -fx-opacity: 50%; -fx-stroke: black; -fx-stroke-width: 2;");

        // Setup dragging UI.
        this.meshView.setOnMousePressed(event -> {
            InputManager manager = getController().getInputManager();
            if (!manager.isKeyPressed(KeyCode.SHIFT))
                return;

            // Start dragging.
            event.consume();
            this.rectangleSelectionActive = true;
            manager.getLastDragStartMouseState().apply(event);
            this.selectionArea.setX(0);
            this.selectionArea.setY(0);
            this.selectionArea.setWidth(0);
            this.selectionArea.setHeight(0);
            getController().getSubScene2DElements().getChildren().add(this.selectionArea);
        });

        this.meshView.setOnMouseDragged(event -> {
            if (!this.rectangleSelectionActive)
                return;

            // Update preview.
            event.consume();
            InputManager manager = getController().getInputManager();
            Point2D uiOffset = getController().getSubScene().localToScene(0, 0);
            if (uiOffset.getX() >= event.getSceneX() || uiOffset.getY() >= event.getSceneY())
                return; // Don't allow moving the 3D view.

            double minX = Math.min(event.getSceneX(), manager.getLastDragStartMouseState().getX()) - uiOffset.getX();
            double maxX = Math.max(event.getSceneX(), manager.getLastDragStartMouseState().getX()) - uiOffset.getX();
            double minY = Math.min(event.getSceneY(), manager.getLastDragStartMouseState().getY()) - uiOffset.getY();
            double maxY = Math.max(event.getSceneY(), manager.getLastDragStartMouseState().getY()) - uiOffset.getY();
            this.selectionArea.setX(minX);
            this.selectionArea.setY(minY);
            this.selectionArea.setWidth(maxX - minX);
            this.selectionArea.setHeight(maxY - minY);
        });

        this.meshView.setOnMouseReleased(event -> {
            if (!this.rectangleSelectionActive)
                return;

            // Update preview.
            event.consume();
            this.rectangleSelectionActive = false;
            getController().getSubScene2DElements().getChildren().remove(this.selectionArea);

            // Handle vertex click.
            int meshIndex = getClosestVertex(event.getPickResult());
            if (meshIndex < 0)
                return;

            // Handle vertex click.
            int gridX = this.mesh.getMainNode().getGridX(meshIndex);
            int gridZ = this.mesh.getMainNode().getGridZ(meshIndex);
            onClickVertex(gridX, gridZ);
        });
    }

    private void moveSelectionRelative(KeyEvent event, int x, int z) {
        if (this.meshView.isVisible() && moveSelectionRelative(x, z))
            event.consume();
    }

    @Override
    public void onRemove() {
        super.onRemove();

        // Unregister the mesh.
        if (this.mesh != null && this.meshView != null)
            this.mesh.removeView(this.meshView);
    }

    @SuppressWarnings("StatementWithEmptyBody") // It's an else-if that prevents other if statements from running.
    private void onClickVertex(int x, int z) {
        InputManager input = getController().getInputManager();

        if (input.isKeyPressed(KeyCode.SHIFT)) {
            // Select all within the area covered by the mouse drag.
            int oldMeshIndex = getClosestVertex(input.getLastDragStartMouseState());
            if (oldMeshIndex < 0)
                return;

            int startGridX = this.mesh.getMainNode().getGridX(oldMeshIndex);
            int startGridZ = this.mesh.getMainNode().getGridZ(oldMeshIndex);

            int minX = Math.min(startGridX, x);
            int maxX = Math.max(startGridX, x);
            int minZ = Math.min(startGridZ, z);
            int maxZ = Math.max(startGridZ, z);

            // Deselect all vertices unless control is pressed.
            if (!input.isKeyPressed(KeyCode.CONTROL))
                deselectAllVertices();

            // Select all vertices in the area.
            for (int gridZ = minZ; gridZ <= maxZ; gridZ++)
                for (int gridX = minX; gridX <= maxX; gridX++)
                    selectVertex(gridX, gridZ);
        } else if (input.hasMouseMovedSinceDragStart()) {
            // If the mouse has moved meaningfully, don't do any selection.
        } else if (input.isKeyPressed(KeyCode.CONTROL)) {
            // Select / deselect a vertex, but without deselecting all others.
            if (isVertexSelected(x, z)) {
                deselectVertex(x, z);
            } else {
                selectVertex(x, z);
            }
        } else {
            // Select a single vertex, deselecting all others.
            if (isVertexSelected(x, z) && this.verticeDisplayList.size() == 1) {
                deselectAllVertices();
            } else {
                deselectAllVerticesExcept(x, z);
            }
        }
    }

    @Override
    public void updateEditor() {
        super.updateEditor();
        this.editorGrid.clearEditor();

        // If the preview is hidden, don't show stuff.
        if (this.meshView == null || !this.meshView.isVisible()) {
            this.editorGrid.addBoldLabel("The preview is currently hidden.");
            this.editorGrid.addNormalLabel("Editing is disabled until it becomes visible.");
            return;
        }

        // Determine if any vertices are selected.
        boolean anyVerticesSelected = this.selectedVertices.size() > 0;

        // If nothing is selected, include information on how to use the editor.
        if (!anyVerticesSelected) {
            this.editorGrid.addBoldLabel("There are no vertices currently selected.");
            this.editorGrid.addNormalLabel("To select a vertex, click inside the preview.");
            this.editorGrid.addNormalLabel("Shift-Click: Select vertices in hovered area.");
            this.editorGrid.addNormalLabel("Ctrl-Click: Enable selecting multiple vertices.");
            return;
        }

        this.editorGrid.addBoldLabel("Vertices Editor:");
        this.editorGrid.addNormalLabel("Use the arrow keys to move the selection.");
        this.editorGrid.addNormalLabel("Change the height by dragging the gizmo.");
        this.editorGrid.addSeparator();

        // Height.
        double averageHeight = 0;
        for (int i = 0; i < this.selectedVertices.size(); i++) {
            SelectedVertex vertex = this.selectedVertices.get(i);
            averageHeight += getPacket().getWorldY(vertex.getX(), vertex.getZ());
        }
        averageHeight /= this.selectedVertices.size();
        this.editorGrid.addNormalLabel(String.format("Average Height: %.4f", averageHeight));
        this.editorGrid.addDoubleField("Set Height", averageHeight, newHeight -> {
            short fixedNewHeight = Utils.floatToFixedPointShort4Bit((float) (double) newHeight);

            // Apply height to all selected vertices.
            for (int i = 0; i < this.selectedVertices.size(); i++) {
                SelectedVertex vertex = this.selectedVertices.get(i);
                getPacket().getHeightMap()[vertex.getZ()][vertex.getX()] = fixedNewHeight;
            }
        }, null);

        // TODO: Finish editor.
    }

    /**
     * Handles the vertex gizmo getting moved for a vertex.
     * Turns that one movement into a movement for all selected vertices.
     */
    private void onVertexMoved(MeshView meshView, double oldX, double oldY, double oldZ, double newX, double newY, double newZ) {
        // Get selected vertex.
        SelectedVertex selectedVertex = getSelectedVertexByDisplay(meshView);

        if (selectedVertex == null)
            return; // Couldn't find the vertex.

        short fixedOldHeight = getPacket().getHeightMap()[selectedVertex.getZ()][selectedVertex.getX()];
        short fixedNewHeight = Utils.floatToFixedPointShort4Bit((float) newY);
        if (fixedOldHeight == fixedNewHeight)
            return;

        // Apply offset to all selected vertices.
        int fixedOffset = (fixedNewHeight - fixedOldHeight);
        for (int i = 0; i < this.selectedVertices.size(); i++) {
            SelectedVertex tempVertex = this.selectedVertices.get(i);
            int tempOldHeight = getPacket().getHeightMap()[tempVertex.getZ()][tempVertex.getX()];
            int tempNewHeight = Math.max(Short.MIN_VALUE, Math.min(Short.MAX_VALUE, tempOldHeight + fixedOffset));
            getPacket().getHeightMap()[tempVertex.getZ()][tempVertex.getX()] = (short) tempNewHeight;
        }

        // Update the positions the vertices are displayed at.
        updateVertexPositions();
    }

    /**
     * Test if the vertex at the given camera height grid coordinates is selected.
     * @param x x grid coordinate of the vertex to check
     * @param z z grid coordinate of the vertex to check
     * @return true if the vertex is selected
     */
    public boolean isVertexSelected(int x, int z) {
        return z >= 0 && z < this.selectedVerticesGrid.length
                && x >= 0 && x < this.selectedVerticesGrid[z].length
                && this.selectedVerticesGrid[z][x] != null;
    }

    /**
     * Mark a vertex as selected.
     * This will not update the underlying mesh array.
     * @param x x grid coordinate of the vertex to select
     * @param z z grid coordinate of the vertex to select
     */
    public void selectVertex(int x, int z) {
        if (z < 0 || z >= this.selectedVerticesGrid.length)
            throw new IllegalArgumentException("The provided Z coordinate (" + z + ") is not within the camera grid.");
        if (x < 0 || x >= this.selectedVerticesGrid[z].length)
            throw new IllegalArgumentException("The provided X coordinate (" + x + ") is not within the camera grid.");
        if (this.selectedVerticesGrid[z][x] != null)
            return; // Already selected.

        // Track the vertex, and create its display.
        boolean wasEmpty = this.verticeDisplayList.isEmpty();
        SelectedVertex newVertex = new SelectedVertex(this, x, z);
        this.selectedVerticesGrid[z][x] = newVertex;
        newVertex.createDisplay();

        // Update the mesh display to the vertex as selected.
        this.mesh.getMainNode().updateTexCoord(x, z);

        // If there were no active vertices, and this is the first one, update the editor.
        if (wasEmpty)
            updateEditor();
    }

    private void onDisplayClicked(MouseEvent event) {
        MeshView meshView = (MeshView) event.getSource();
        SelectedVertex clickedVertex = getSelectedVertexByDisplay(meshView);
        if (clickedVertex != null) {
            onClickVertex(clickedVertex.getX(), clickedVertex.getZ());
            return;
        }

        // No sphere was found, which should not occur.
        throw new IllegalStateException("The vertex corresponding to the clicked display was not found.");
    }

    private SelectedVertex getSelectedVertexByDisplay(MeshView display) {
        for (int i = 0; i < this.selectedVertices.size(); i++) {
            SelectedVertex vertex = this.selectedVertices.get(i);
            if (vertex.getDisplay() == display)
                return vertex;
        }

        return null;
    }

    /**
     * Deselect all vertices which are currently selected.
     */
    public void deselectAllVertices() {
        deselectAllVerticesExcept(-1, -1);
    }

    /**
     * Deselect all vertices which are currently selected, except for the one at the coordinates specified.
     */
    public void deselectAllVerticesExcept(int skipX, int skipZ) {
        this.mesh.getEditableTexCoords().startBatchingUpdates();
        for (int z = 0; z < this.selectedVerticesGrid.length; z++)
            for (int x = 0; x < this.selectedVerticesGrid[z].length; x++)
                if (x != skipX || z != skipZ)
                    deselectVertex(x, z);

        if (skipX >= 0 && skipZ >= 0)
            selectVertex(skipX, skipZ);
        this.mesh.getEditableTexCoords().endBatchingUpdates();
    }

    /**
     * Mark a vertex as not selected.
     * @param x x grid coordinate of the vertex to select
     * @param z z grid coordinate of the vertex to select
     */
    public void deselectVertex(int x, int z) {
        if (z < 0 || z >= this.selectedVerticesGrid.length)
            throw new IllegalArgumentException("The provided Z coordinate (" + z + ") is not within the camera grid.");
        if (x < 0 || x >= this.selectedVerticesGrid[z].length)
            throw new IllegalArgumentException("The provided X coordinate (" + x + ") is not within the camera grid.");

        SelectedVertex selectedVertex = this.selectedVerticesGrid[z][x];
        if (selectedVertex == null)
            return; // Already unselected.

        // Remove the display, marking the vertex as unselected.
        selectedVertex.removeDisplay();
        this.selectedVerticesGrid[z][x] = null;

        // Update the mesh display to show the vertex as not selected.
        this.mesh.getMainNode().updateTexCoord(x, z);

        // If there are no more active vertices, update the editor.
        if (this.verticeDisplayList.isEmpty())
            updateEditor();
    }

    /**
     * Gets the map data packet containing camera height-field data.
     */
    public OldFroggerMapCameraHeightFieldPacket getPacket() {
        return getMap().getCameraHeightFieldPacket();
    }

    /**
     * Handles the change of the size of the camera height-field grid.
     * @param oldX old number of squares in the x direction
     * @param oldZ old number of squares in the z direction
     * @param newX new number of squares in the x direction
     * @param newZ new number of squares in the z direction
     */
    public void onGridSizeChange(int oldX, int oldZ, int newX, int newZ) {
        if (newX < 0)
            throw new IllegalArgumentException("The x count cannot be less than zero! (Provided: " + newX + ")");
        if (newZ < 0)
            throw new IllegalArgumentException("The z count cannot be less than zero! (Provided: " + newZ + ")");

        if (oldX == newX && oldZ == newZ)
            return; // There's no difference between the old & new sizes.

        SelectedVertex[][] newVertexDisplays = new SelectedVertex[newZ][newX];
        if (newX != 0 && newZ != 0) {
            // Copy old selections to the new grid.
            int copyWidth = Math.min(oldX, newX);
            int copyHeight = Math.min(oldZ, newZ);
            for (int z = 0; z < copyHeight; z++) {
                System.arraycopy(this.selectedVerticesGrid[z], 0, newVertexDisplays[z], 0, copyWidth);

                // Delete unused selections in the position X direction.
                for (int x = copyWidth; x < oldX; x++) {
                    SelectedVertex selectedVertex = this.selectedVerticesGrid[z][x];
                    if (selectedVertex != null)
                        selectedVertex.removeDisplay();
                }
            }

            // Delete vertex displays which are now unused, and update the positions of ones which are used.
            for (int z = 0; z < oldZ; z++) {
                for (int x = 0; x < oldX; x++) {
                    SelectedVertex selectedVertex = this.selectedVerticesGrid[z][x];
                    if (selectedVertex == null)
                        continue;

                    // This display is now unused, time to remove it.
                    if (x >= copyWidth || z >= copyHeight)
                        selectedVertex.removeDisplay();
                }
            }

            // Add & remove mesh data for the new size.
            this.mesh.getMainNode().onGridSizeChange(oldX, oldZ, newX, newZ);
        }

        // Save new display point.
        this.selectedVerticesGrid = newVertexDisplays;
    }

    /**
     * Updates the position of all vertices.
     * Should be called only if a change occurs which impacts the position of all vertices.
     */
    public void updateVertexPositions() {
        this.mesh.getMainNode().updateAllVertices();

        // Update the position of all displays.
        for (int i = 0; i < this.selectedVertices.size(); i++)
            this.selectedVertices.get(i).updateDisplayPosition();
    }

    /**
     * Gets the closest mesh vertex index given mouse FX PickResult state.
     * The value returned can be used to get the x / z grid coordinates.
     * @return vertexIndex, or -1 if one could not be found
     */
    public int getClosestVertex(PickResult result) {
        if (result == null)
            return -1;

        Point3D mousePoint = result.getIntersectedPoint();
        if (mousePoint == null)
            return -1;

        int faceIndex = result.getIntersectedFace();
        return getClosestVertex(faceIndex, mousePoint.getX(), mousePoint.getY(), mousePoint.getZ());
    }

    /**
     * Gets the closest mesh vertex index given mouse input state.
     * The value returned can be used to get the x / z grid coordinates.
     * @return vertexIndex, or -1 if one could not be found
     */
    public int getClosestVertex(MouseInputState mouseState) {
        if (mouseState == null)
            return -1;

        int faceIndex = mouseState.getIntersectedFaceIndex();
        Vector3f mousePoint = mouseState.getIntersectedPoint();
        return getClosestVertex(faceIndex, mousePoint.getX(), mousePoint.getY(), mousePoint.getZ());
    }

    /**
     * Gets the closest mesh vertex index given the arguments provided.
     * The value returned can be used to get the x / z grid coordinates.
     * @return vertexIndex, or -1 if one could not be found
     */
    public int getClosestVertex(int intersectedFaceIndex, double mouseX, double mouseY, double mouseZ) {
        if (intersectedFaceIndex < 0)
            return -1;

        OldFroggerMapCameraHeightFieldPacket packet = getPacket();
        int meshIndex = this.mesh.getMainNode().getMeshIndexFromFaceIndex(intersectedFaceIndex);
        int gridX = this.mesh.getMainNode().getGridX(meshIndex);
        int gridZ = this.mesh.getMainNode().getGridZ(meshIndex);

        // We want to test between the four vertices forming this quad, whichever vertex is closest is the one selected.
        double bestDistance = Double.MAX_VALUE;
        int closestX = -1;
        int closestZ = -1;
        for (int zOffset = 0; zOffset <= 1; zOffset++) {
            int testGridZ = gridZ + zOffset;
            for (int xOffset = 0; xOffset <= 1; xOffset++) {
                int testGridX = gridX + xOffset;
                float worldX = packet.getWorldX(testGridX);
                float worldY = packet.getWorldY(testGridX, testGridZ);
                float worldZ = packet.getWorldZ(testGridZ);
                double newDistance = ((mouseX - worldX) * (mouseX - worldX)) + ((mouseY - worldY) * (mouseY - worldY)) + ((mouseZ - worldZ) * (mouseZ - worldZ));

                if (newDistance < bestDistance) {
                    bestDistance = newDistance;
                    closestX = testGridX;
                    closestZ = testGridZ;
                }
            }
        }

        return this.mesh.getMainNode().getMeshIndex(closestX, closestZ);
    }

    /**
     * Moves the selected vertices relative from their current position
     * @param xOffset The amount to offset in the x direction
     * @param zOffset The amount of offset in the Z direction.
     * @return true, if the selection was moved successfully
     */
    public boolean moveSelectionRelative(int xOffset, int zOffset) {
        if (xOffset == 0 && zOffset == 0)
            return false; // No change.

        // Bounds Check - Ensure there are no elements that would be moved outside the grid.
        for (int z = 0; z < this.selectedVerticesGrid.length; z++) {
            for (int x = 0; x < this.selectedVerticesGrid[z].length; x++) {
                if (this.selectedVerticesGrid[z][x] == null)
                    continue; // Not selected.

                // Test bounds in the x direction.
                int newX = x + xOffset;
                if ((newX >= this.selectedVerticesGrid[z].length) || (newX < 0))
                    return false;

                // Test bounds in the z direction.
                int newZ = z + zOffset;
                if ((newZ >= this.selectedVerticesGrid.length) || (newZ < 0))
                    return false;
            }
        }

        // Batch the changes.
        this.mesh.getEditableTexCoords().startBatchingUpdates();
        this.mesh.getEditableVertices().startBatchingUpdates();

        // Perform updates.
        if (xOffset > 0) {
            for (int z = 0; z < this.selectedVerticesGrid.length; z++)
                for (int x = this.selectedVerticesGrid[z].length - xOffset - 1; x >= 0; x--)
                    moveSelection(x, z, xOffset, 0);
        } else if (xOffset < 0) {
            for (int z = 0; z < this.selectedVerticesGrid.length; z++)
                for (int x = -xOffset; x < this.selectedVerticesGrid[z].length; x++)
                    moveSelection(x, z, xOffset, 0);
        }

        if (zOffset > 0) {
            for (int z = this.selectedVerticesGrid.length - zOffset - 1; z >= 0; z--)
                for (int x = 0; x < this.selectedVerticesGrid[z].length; x++)
                    moveSelection(x, z, 0, zOffset);
        } else if (zOffset < 0) {
            for (int z = -zOffset; z < this.selectedVerticesGrid.length; z++)
                for (int x = 0; x < this.selectedVerticesGrid[z].length; x++)
                    moveSelection(x, z, 0, zOffset);
        }

        // Commit changes.
        this.mesh.getEditableTexCoords().endBatchingUpdates();
        this.mesh.getEditableVertices().endBatchingUpdates();
        return true;
    }

    private void moveSelection(int x, int z, int xOffset, int zOffset) {
        OldFroggerMapCameraHeightFieldPacket packet = getPacket();
        SelectedVertex selectedVertex = this.selectedVerticesGrid[z][x];
        if (selectedVertex == null)
            return; // Not selected.

        // Get information for the selection to move.
        short selectedHeight = packet.getHeightMap()[z][x];

        // Get information for position replaced by the selection.
        int replacedX = x + xOffset;
        int replacedZ = z + zOffset;
        SelectedVertex replacedVertex = this.selectedVerticesGrid[replacedZ][replacedX];
        short replacedHeight = packet.getHeightMap()[replacedZ][replacedX];

        // Swap height data.
        packet.getHeightMap()[replacedZ][replacedX] = selectedHeight;
        packet.getHeightMap()[z][x] = replacedHeight;

        // Update tracking & visual displays.
        this.selectedVerticesGrid[replacedZ][replacedX] = selectedVertex;
        this.selectedVerticesGrid[z][x] = replacedVertex;
        selectedVertex.setGridPosition(replacedX, replacedZ);
        if (replacedVertex != null)
            replacedVertex.setGridPosition(x, z);

        // Update display data. (Should run only after updating selection)
        this.mesh.getEditableTexCoords().startBatchingUpdates();
        this.mesh.getEditableVertices().startBatchingUpdates();

        // Update display data.
        this.mesh.getMainNode().updateTexCoord(x, z);
        this.mesh.getMainNode().updateTexCoord(replacedX, replacedZ);
        this.mesh.getMainNode().updateVertex(x, z);
        this.mesh.getMainNode().updateVertex(replacedX, replacedZ);

        // End batching.
        this.mesh.getEditableVertices().endBatchingUpdates();
        this.mesh.getEditableTexCoords().endBatchingUpdates();
    }

    @Getter
    public static class SelectedVertex {
        private final OldFroggerCameraHeightFieldManager manager;
        private MeshView display;
        private int x;
        private int z;

        public SelectedVertex(OldFroggerCameraHeightFieldManager manager, int x, int z) {
            this.manager = manager;
            this.x = x;
            this.z = z;
        }

        /**
         * Changes the position of this selected vertex in the grid.
         * @param newX new x grid coordinate
         * @param newZ new z grid coordinate
         */
        public void setGridPosition(int newX, int newZ) {
            this.x = newX;
            this.z = newZ;
            updateDisplayPosition();
        }

        /**
         * Updates the position which the 3D representation is displayed.
         */
        public void updateDisplayPosition() {
            if (this.display == null)
                return;

            OldFroggerMapCameraHeightFieldPacket packet = this.manager.getPacket();
            float xPos = packet.getWorldX(this.x);
            float yPos = packet.getWorldY(this.x, this.z);
            float zPos = packet.getWorldZ(this.z);
            Scene3DUtils.setNodePosition(this.display, xPos, yPos, zPos);
        }

        /**
         * Creates the 3D representation of this vertex.
         */
        public void createDisplay() {
            if (this.display != null)
                removeDisplay();

            // Calculate world position.
            OldFroggerMapCameraHeightFieldPacket packet = this.manager.getPacket();
            float xPos = packet.getWorldX(this.x);
            float yPos = packet.getWorldY(this.x, this.z);
            float zPos = packet.getWorldZ(this.z);

            // Create display.
            this.display = new MeshView();
            this.display.getTransforms().add(new Translate(xPos, yPos, zPos));
            this.display.getTransforms().add(TRANSLATION_GIZMO_SCALE);
            this.display.setOnMouseClicked(this.manager::onDisplayClicked);

            FirstPersonCamera camera = this.manager.getController().getFirstPersonCamera();
            this.manager.vertexTranslationGizmo.addView(this.display, camera, this.manager.positionChangeListener);
            this.manager.getController().getMainLight().getScope().add(this.display);
            this.manager.verticeDisplayList.add(this.display);
            this.manager.selectedVertices.add(this);
        }

        /**
         * Removes the 3D representation of this vertex.
         */
        public void removeDisplay() {
            if (this.display == null)
                return;

            this.manager.vertexTranslationGizmo.removeView(this.display);
            this.manager.verticeDisplayList.remove(this.display);
            this.manager.selectedVertices.remove(this);
            this.manager.getController().getMainLight().getScope().remove(this.display);
            this.display = null;
        }
    }
}