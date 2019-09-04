package net.highwayfrogs.editor.gui.editor.map.manager;

import javafx.application.Platform;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Box;
import lombok.Getter;
import net.highwayfrogs.editor.file.map.poly.MAPPolygonData;
import net.highwayfrogs.editor.file.standard.SVector;
import net.highwayfrogs.editor.gui.GUIEditorGrid;
import net.highwayfrogs.editor.gui.editor.MapUIController;
import net.highwayfrogs.editor.utils.Utils;

import java.util.Objects;
import java.util.function.Consumer;

/**
 * Manages vertices.
 * Created by Kneesnap on 9/1/2019.
 */
@Getter
public class VertexManager extends MapManager {
    private GUIEditorGrid verticeEditor;
    private SVector selectedVector;
    private Box selectedBox;
    private Consumer<SVector> promptHandler;
    private int[] shownVertices;
    private int polyIndex;
    private static final String VERTICE_LIST = "shownVertices";
    private static final double SIZE = 1.5;
    private static final PhongMaterial NORMAL_MATERIAL = Utils.makeSpecialMaterial(Color.YELLOW);
    private static final PhongMaterial SELECT_MATERIAL = Utils.makeSpecialMaterial(Color.BLUE);
    private static final PhongMaterial PROMPT_MATERIAL = Utils.makeSpecialMaterial(Color.PINK);

    public VertexManager(MapUIController controller) {
        super(controller);
    }

    @Override
    public void onSetup() {
        super.onSetup();
        getController().getShowAllVerticesToggle().setOnAction(evt -> {
            this.shownVertices = null;
            updateVisibility();
        });
    }

    @Override
    public void setupEditor() {
        super.setupEditor();
        if (this.verticeEditor == null)
            this.verticeEditor = new GUIEditorGrid(getController().getVtxGridPane());

        this.verticeEditor.clearEditor();
        this.verticeEditor.addButton("Add Vertex", () -> selectVertex(cloneVtx -> { // Select the vertex to clone.
            SVector newVertex = new SVector(cloneVtx);
            getMap().getVertexes().add(newVertex);

            this.selectedVector = newVertex;
            updateVisibility();
            setupEditor(); // Allow editing this vertex.
        }, null));
        this.verticeEditor.addButton("Add Polygon", () -> {
            this.polyIndex = 0;
            chooseNextPoly();
        });

        if (this.selectedVector != null) {
            this.verticeEditor.addFloatVector("Vertex", this.selectedVector,
                    () -> getController().getGeometryManager().refreshView(), getController(), this.selectedVector.defaultBits(), null, this.selectedBox);
        }
    }

    private void chooseNextPoly() {
        MAPPolygonData data = getController().getGeometryManager().getPolygonData();

        if (this.polyIndex == data.getVerticeCount()) { // Done, add the polygon.
            getMap().addPolygon(data);
            getController().getGeometryManager().refreshView(); // Update the view so the polygon shows up.
            return;
        }

        // Select the next vertex.
        selectVertex(vertex -> {
            MAPPolygonData polyData = getController().getGeometryManager().getPolygonData();
            polyData.getVertices()[this.polyIndex++] = getMap().getVertexes().indexOf(vertex);
            Platform.runLater(this::chooseNextPoly);
        }, null);
    }

    /**
     * Updates the vertex display.
     */
    public void updateVisibility() {
        getRenderManager().addMissingDisplayList(VERTICE_LIST);
        getRenderManager().clearDisplayList(VERTICE_LIST);

        if ((this.shownVertices == null && getController().getShowAllVerticesToggle().isSelected()) || isPromptActive()) {
            for (SVector vec : getMap().getVertexes())
                setupVertex(vec);
        } else if (this.shownVertices != null) {
            for (int id : this.shownVertices)
                setupVertex(getMap().getVertexes().get(id));
        } else if (this.selectedVector != null && this.selectedBox == null) {
            this.selectedBox = setupVertex(this.selectedVector);
        }
    }

    private Box setupVertex(SVector vec) {
        PhongMaterial material = (Objects.equals(this.selectedVector, vec) ? SELECT_MATERIAL : (isPromptActive() ? PROMPT_MATERIAL : NORMAL_MATERIAL));
        Box box = getRenderManager().addBoundingBoxCenteredWithDimensions(VERTICE_LIST, vec.getFloatX(), vec.getFloatY(), vec.getFloatZ(), SIZE, SIZE, SIZE, material, true);
        box.setMouseTransparent(false);
        box.setOnMouseClicked(evt -> {
            if (isPromptActive()) {
                acceptPrompt(vec);
                return;
            }

            if (Objects.equals(this.selectedVector, vec)) { // Clicked the selected vertex. Deselect!
                this.selectedVector = null;
                this.selectedBox = null;
                updateVisibility();
                setupEditor();
                return;
            }

            if (this.selectedBox != null)
                this.selectedBox.setMaterial(NORMAL_MATERIAL);
            box.setMaterial(SELECT_MATERIAL);

            this.selectedVector = vec;
            this.selectedBox = box;
            updateVisibility();
            setupEditor(); // Update editor / selected.
        });

        return box;
    }

    /**
     * Shows only some specific vertices.
     * @param vertices The vertices to show.
     */
    public void showVertices(int[] vertices) {
        this.selectedVector = null;
        this.selectedBox = null;
        this.shownVertices = vertices;
        updateVisibility();
        setupEditor();
    }

    /**
     * Prompts the user for a vertex.
     * @param handler  The handler to accept a prompt with.
     * @param onCancel A callback to run upon cancelling.
     */
    public void selectVertex(Consumer<SVector> handler, Runnable onCancel) {
        this.promptHandler = handler;
        activatePrompt(onCancel);
        updateVisibility(); // Update visibility.
    }

    /**
     * Accept the data for the prompt.
     * @param vertex The vertex to accept.
     */
    public void acceptPrompt(SVector vertex) {
        if (this.promptHandler != null)
            this.promptHandler.accept(vertex);
        onPromptFinish();
    }

    @Override
    protected void cleanChildPrompt() {
        super.cleanChildPrompt();
        this.promptHandler = null;
        updateVisibility(); // Don't show the select color anymore.
    }
}
