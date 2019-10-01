package net.highwayfrogs.editor.gui.editor.map.manager;

import javafx.application.Platform;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Box;
import javafx.scene.shape.CullFace;
import javafx.scene.shape.DrawMode;
import javafx.scene.transform.Translate;
import lombok.Getter;
import net.highwayfrogs.editor.file.map.poly.MAPPolygonData;
import net.highwayfrogs.editor.file.standard.SVector;
import net.highwayfrogs.editor.gui.GUIEditorGrid;
import net.highwayfrogs.editor.gui.editor.MapUIController;
import net.highwayfrogs.editor.utils.Utils;

import java.util.ArrayList;
import java.util.List;
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
    private static final double SIZE = 1.5;
    private static final PhongMaterial NORMAL_MATERIAL = Utils.makeSpecialMaterial(Color.YELLOW);
    private static final PhongMaterial SELECT_MATERIAL = Utils.makeSpecialMaterial(Color.BLUE);
    private static final PhongMaterial PROMPT_MATERIAL = Utils.makeSpecialMaterial(Color.PINK);
    private List<Box> boxes = new ArrayList<>();
    private List<SVector> vectors = new ArrayList<>();
    private List<SVector> tempList = new ArrayList<>();

    public VertexManager(MapUIController controller) {
        super(controller);
    }

    @Override
    public void onSetup() {
        super.onSetup();
        getRenderManager().addMissingDisplayList("vertexBoxes");
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

        this.verticeEditor.addButton("Remove Unused Vertices", () -> {
            getMap().removeUnusedVertices();
            updateVisibility();
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

    private void updateBoxes(List<SVector> newBoxes) {
        // Add vector data.
        while (newBoxes.size() > this.vectors.size())
            this.vectors.add(null);

        for (int i = 0; i < this.vectors.size(); i++)
            this.vectors.set(i, (newBoxes.size() > i ? newBoxes.get(i) : null));

        // Add new boxes.
        while (newBoxes.size() > this.boxes.size()) {
            Box box = new Box(SIZE, SIZE, SIZE);
            box.setMaterial(NORMAL_MATERIAL);
            box.setDrawMode(DrawMode.LINE);
            box.setCullFace(CullFace.BACK);
            box.getTransforms().addAll(new Translate(0, 0, 0));
            box.setMouseTransparent(false);
            this.boxes.add(box);
            getRenderManager().addNode("vertexBoxes", box);

            box.setOnMouseClicked(evt -> {
                Box safeBox = (Box) evt.getSource();
                int boxIndex = this.boxes.indexOf(safeBox);
                SVector vec = this.vectors.get(boxIndex);

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
                safeBox.setMaterial(SELECT_MATERIAL);

                this.selectedVector = vec;
                this.selectedBox = safeBox;
                updateVisibility();
                setupEditor(); // Update editor / selected.
            });
        }

        // Update existing boxes.
        for (int i = 0; i < this.boxes.size(); i++) {
            Box box = this.boxes.get(i);
            boolean isUsed = i < newBoxes.size();
            box.setVisible(isUsed);

            if (isUsed) {
                SVector vec = this.vectors.get(i);
                Translate translate = (Translate) box.getTransforms().get(0);
                translate.setX(vec.getFloatX()); // Update transform.
                translate.setY(vec.getFloatY());
                translate.setZ(vec.getFloatZ());
                box.setMaterial(Objects.equals(this.selectedVector, vec) ? SELECT_MATERIAL : (isPromptActive() ? PROMPT_MATERIAL : NORMAL_MATERIAL)); // Update material.
            }
        }
    }

    /**
     * Updates the vertex display.
     */
    public void updateVisibility() {
        if ((this.shownVertices == null && getController().getShowAllVerticesToggle().isSelected()) || isPromptActive()) {
            updateBoxes(getMap().getVertexes());
        } else if (this.shownVertices != null) {
            tempList.clear();
            for (int i = 0; i < this.shownVertices.length; i++)
                tempList.add(getMap().getVertexes().get(this.shownVertices[i]));
            updateBoxes(tempList);
        } else if (this.selectedVector != null && this.selectedBox == null) {
            tempList.clear();
            tempList.add(this.selectedVector);
            updateBoxes(tempList);
            this.selectedBox = this.boxes.get(0);
        } else { // Don't show any vertices.
            tempList.clear();
            updateBoxes(tempList);
        }
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
