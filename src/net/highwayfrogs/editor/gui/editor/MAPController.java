package net.highwayfrogs.editor.gui.editor;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.*;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.*;
import javafx.scene.transform.Rotate;
import javafx.stage.Stage;
import lombok.Getter;
import lombok.SneakyThrows;
import net.highwayfrogs.editor.file.GameFile;
import net.highwayfrogs.editor.file.WADFile;
import net.highwayfrogs.editor.file.WADFile.WADEntry;
import net.highwayfrogs.editor.file.config.exe.general.FormEntry;
import net.highwayfrogs.editor.file.map.MAPFile;
import net.highwayfrogs.editor.file.map.entity.Entity;
import net.highwayfrogs.editor.file.map.light.Light;
import net.highwayfrogs.editor.file.map.poly.polygon.MAPPolygon;
import net.highwayfrogs.editor.file.map.view.CursorVertexColor;
import net.highwayfrogs.editor.file.map.view.MapMesh;
import net.highwayfrogs.editor.file.map.view.TextureMap;
import net.highwayfrogs.editor.file.mof.MOFHolder;
import net.highwayfrogs.editor.file.standard.SVector;
import net.highwayfrogs.editor.file.vlo.ImageFilterSettings;
import net.highwayfrogs.editor.file.vlo.ImageFilterSettings.ImageState;
import net.highwayfrogs.editor.gui.GUIMain;
import net.highwayfrogs.editor.gui.mesh.MeshData;
import net.highwayfrogs.editor.system.NameValuePair;
import net.highwayfrogs.editor.utils.Utils;

import java.util.ArrayList;
import java.util.List;

/**
 * Sets up the map editor.
 * TODO: Edit Vertexes [Add, Remove] Prevent removing a vertice if it is used by anything.
 * TODO: Edit polygons [Add, Remove] This is tricky because it will effect data structures which link to the polygons.
 * TODO: Create a path editor.
 * Created by Kneesnap on 11/22/2018.
 */
@Getter
public class MAPController extends EditorController<MAPFile> {
    @FXML private TableView<NameValuePair> tableMAPFileData;
    @FXML private TableColumn<Object, Object> tableColumnMAPFileDataName;
    @FXML private TableColumn<Object, Object> tableColumnMAPFileDataValue;

    private Scene mapScene;
    private MapMesh mapMesh;
    private MAPPolygon selectedPolygon;
    private MAPPolygon polygonImmuneToTarget;
    private boolean polygonSelected;

    private RenderManager renderManager = new RenderManager();
    private CameraFPS cameraFPS;
    private MapUIController mapUIController;

    private List<MeshView> entityIcons = new ArrayList<>();
    private List<Node> appliedLevelLights = new ArrayList<>();

    private Group root3D;
    private Rotate rotX;
    private Rotate rotY;
    private Rotate rotZ;

    private MeshData cursorData;

    private static final ImageFilterSettings IMAGE_SETTINGS = new ImageFilterSettings(ImageState.EXPORT);
    private static final Image ENTITY_ICON_IMAGE = GameFile.loadIcon("entity");

    private static final PhongMaterial MATERIAL_ENTITY_ICON = Utils.makeSpecialMaterial(ENTITY_ICON_IMAGE);
    private static final PhongMaterial MATERIAL_WHITE = Utils.makeSpecialMaterial(Color.WHITE);
    private static final PhongMaterial MATERIAL_YELLOW = Utils.makeSpecialMaterial(Color.YELLOW);
    private static final PhongMaterial MATERIAL_LIGHT_GREEN = Utils.makeSpecialMaterial(Color.LIGHTGREEN);

    private static final String DISPLAY_LIST_PATHS = "displayListPaths";

    @Override
    public void onInit(AnchorPane editorRoot) {
        super.onInit(editorRoot);
        updateLabels();
    }

    private void updateLabels() {
        MAPFile map = getFile();

        // Setup and initialise the table view
        tableMAPFileData.getItems().clear();
        tableColumnMAPFileDataName.setCellValueFactory(new PropertyValueFactory<>("name"));
        tableColumnMAPFileDataValue.setCellValueFactory(new PropertyValueFactory<>("value"));

        // General properties
        addTableEntry("MAP Theme: ", map.getTheme().toString());
        addTableEntry("Start Position", "(" + map.getStartXTile() + ", " + map.getStartZTile() + ") Rotation: " + map.getStartRotation());
        addTableEntry("Camera Source", "(" + map.getCameraSourceOffset().toFloatString() + ")");
        addTableEntry("Camera Target", "(" + map.getCameraTargetOffset().toFloatString() + ")");
        addTableEntry("Base Point", "[" + map.getBaseXTile() + ", " + map.getBaseZTile() + "]");

        // Entity properties
        addTableEntry("Path Count", Integer.toString(map.getPaths().size()));
        addTableEntry("Form Count", Integer.toString(map.getForms().size()));
        addTableEntry("Entity Count", Integer.toString(map.getEntities().size()));

        // Environment properties
        addTableEntry("Zone Count", Integer.toString(map.getZones().size()));
        addTableEntry("Light Count", Integer.toString(map.getLights().size()));
        addTableEntry("Vertex Count", Integer.toString(map.getVertexes().size()));
        addTableEntry("Polygon Count", Integer.toString(map.getPolygons().values().stream().mapToInt(List::size).sum()));
        addTableEntry("Animation Count", Integer.toString(map.getMapAnimations().size()));

        // Grid properties
        addTableEntry("Grid Stacks", Integer.toString(map.getGridStacks().size()));
        addTableEntry("Grid Size Count", "[" + map.getGridXCount() + ", " + map.getGridZCount() + "]");
        addTableEntry("Grid Size Length", "[" + map.getGridXSize() + ", " + map.getGridZSize() + "]");

        // Group properties
        addTableEntry("Group Count", Integer.toString(map.getGroupCount()));
        addTableEntry("Group Size Count", "[" + map.getGroupXCount() + ", " + map.getGroupZCount() + "]");
        addTableEntry("Group Size Length", "[" + map.getGroupXSize() + ", " + map.getGroupZSize() + "]");
    }

    private void addTableEntry(String name, String value) {
        tableMAPFileData.getItems().add(new NameValuePair(name, value));
    }

    @FXML
    private void onMapButtonClicked(ActionEvent event) {
        TextureMap textureMap = TextureMap.newTextureMap(getFile());
        setupMapViewer(GUIMain.MAIN_STAGE, new MapMesh(getFile(), textureMap), textureMap);
    }

    @SneakyThrows
    private void setupMapViewer(Stage stageToOverride, MapMesh mesh, TextureMap texMap) {
        this.mapMesh = mesh;

        // These cause errors if not reset.
        this.cursorData = null;

        // Setup the primary camera
        this.cameraFPS = new CameraFPS();

        // Create and setup material properties for rendering the level, entity icons and bounding boxes.
        PhongMaterial material = new PhongMaterial();
        material.setDiffuseMap(Utils.toFXImage(texMap.getImage(), true));

        // Create mesh view and initialise with xyz rotation transforms, materials and initial face culling policy.
        MeshView meshView = new MeshView(mesh);

        this.rotX = new Rotate(0, Rotate.X_AXIS);
        this.rotY = new Rotate(0, Rotate.Y_AXIS);
        this.rotZ = new Rotate(0, Rotate.Z_AXIS);
        meshView.getTransforms().addAll(rotX, rotY, rotZ);

        meshView.setMaterial(material);
        meshView.setCullFace(CullFace.BACK);

        // Load FXML for UI layout.
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/javafx/mapui.fxml"));
        Parent loadRoot = fxmlLoader.load();
        // Get the custom mapui controller
        this.mapUIController = fxmlLoader.getController();

        // Create the 3D elements and use them within a subscene.
        this.root3D = new Group(meshView);
        SubScene subScene3D = new SubScene(root3D, stageToOverride.getScene().getWidth() - mapUIController.uiRootPaneWidth(), stageToOverride.getScene().getHeight(), true, SceneAntialiasing.BALANCED);
        subScene3D.setFill(Color.GRAY);
        subScene3D.setCamera(cameraFPS.getCamera());

        // Ensure that the render manager has access to the root node
        this.renderManager.setRenderRoot(this.root3D);

        //  Setup mapui controller bindings, etc.
        mapUIController.setupBindings(this, subScene3D, meshView);

        // Setup the UI layout.
        BorderPane uiPane = new BorderPane();
        uiPane.setLeft(loadRoot);
        uiPane.setCenter(subScene3D);

        // Setup additional scene elements.
        setupEntities();
        MapUIController.getPropertyEntityIconSize().addListener((observable, old, newVal) -> resetEntities());

        // Create and set the scene.
        mapScene = new Scene(uiPane);
        Scene defaultScene = Utils.setSceneKeepPosition(stageToOverride, mapScene);

        // Handle scaling of SubScene on stage resizing.
        mapScene.widthProperty().addListener((observable, old, newVal) -> subScene3D.setWidth(newVal.doubleValue() - mapUIController.uiRootPaneWidth()));
        subScene3D.heightProperty().bind(mapScene.heightProperty());

        // Associate camera controls with the scene.
        cameraFPS.assignSceneControls(mapScene);
        cameraFPS.startThreadProcessing();

        mapScene.setOnKeyPressed(event -> {
            if (getMapUIController() != null && getMapUIController().onKeyPress(event))
                return; // Handled by the other controller.

            // Exit the viewer.
            if (event.getCode() == KeyCode.ESCAPE) {
                if (isPolygonSelected()) {
                    removeCursorPolygon();
                    return;
                }

                // Stop camera processing and clear up the render manager
                cameraFPS.stopThreadProcessing();
                renderManager.removeAllDisplayLists();

                Utils.setSceneKeepPosition(stageToOverride, defaultScene);
            }

            // Toggle wireframe mode.
            if (event.getCode() == KeyCode.X)
                meshView.setDrawMode(meshView.getDrawMode() == DrawMode.FILL ? DrawMode.LINE : DrawMode.FILL);

            // Toggle fullscreen mode.
            if (event.isControlDown() && event.getCode() == KeyCode.ENTER)
                stageToOverride.setFullScreen(!stageToOverride.isFullScreen());

            if (event.getCode() == KeyCode.I) {
                movePolygonY(MapUIController.getPropertyVertexSpeed().get());
            } else if (event.getCode() == KeyCode.K) {
                movePolygonY(-MapUIController.getPropertyVertexSpeed().get());
            } else if (event.getCode() == KeyCode.J) {
                movePolygonX(-MapUIController.getPropertyVertexSpeed().get());
            } else if (event.getCode() == KeyCode.L) {
                movePolygonX(MapUIController.getPropertyVertexSpeed().get());
            }
        });

        mapScene.setOnMousePressed(e -> {
            mapUIController.getAnchorPaneUIRoot().requestFocus();
            if (!isPolygonSelected())
                hideCursorPolygon();
        });

        mapScene.setOnMouseReleased(evt -> {
            hideCursorPolygon();
            renderCursor(getSelectedPolygon());
        });

        mapScene.setOnMouseMoved(evt -> {
            if (!isPolygonSelected())
                setCursorPolygon(mesh.getFacePolyMap().get(evt.getPickResult().getIntersectedFace()));
        });

        mapScene.setOnMouseClicked(evt -> {
            MAPPolygon clickedPoly = getMapMesh().getFacePolyMap().get(evt.getPickResult().getIntersectedFace());

            if (getSelectedPolygon() != null && (getSelectedPolygon() == clickedPoly)) {
                if (isPolygonSelected()) {
                    this.polygonImmuneToTarget = getSelectedPolygon();
                    removeCursorPolygon();
                } else if (mapUIController == null || !mapUIController.handleClick(evt, clickedPoly)) {
                    setCursorPolygon(clickedPoly);
                    this.polygonSelected = true;
                }
            }
        });

        // Set the initial camera position based on start position and in-game camera offset.
        SVector startPos = getFile().getCameraSourceOffset();
        float gridX = Utils.fixedPointIntToFloat4Bit(getFile().getWorldX(getFile().getStartXTile(), true));
        float baseY = -Utils.fixedPointIntToFloat4Bit(getFile().getGridStack(getFile().getStartXTile(), getFile().getStartZTile()).getHeight());
        float gridZ = Utils.fixedPointIntToFloat4Bit(getFile().getWorldZ(getFile().getStartZTile(), true));
        cameraFPS.setPos(gridX + startPos.getFloatX(), baseY + startPos.getFloatY(), gridZ + startPos.getFloatZ());
        // Set the camera to look at the start position, too.
        cameraFPS.setCameraLookAt(gridX, baseY, gridZ);

        // TODO: Tidy this up at some point, but use an action on a UI control for now [AndyEder]
        mapUIController.getCheckBoxShowAllPaths().setOnAction(evt -> this.togglePathDisplay());
        mapUIController.getBtnApplyLevelLights().setOnAction(evt -> this.applyLevelLighting());
    }

    /**
     * Toggle display of paths.
     */
    private void togglePathDisplay() {
        if (mapUIController.getCheckBoxShowAllPaths().isSelected()) {
            if (!this.renderManager.displayListExists(DISPLAY_LIST_PATHS)) {
                // Add a new display list for the paths
                this.renderManager.addDisplayList(DISPLAY_LIST_PATHS);
            }

            // Add paths via the render manager
            this.renderManager.addPaths(DISPLAY_LIST_PATHS, getFile().getPaths(), MATERIAL_WHITE, MATERIAL_YELLOW, MATERIAL_LIGHT_GREEN);
        }
        else {
            // Clear the paths display list
            this.renderManager.clearDisplayList(DISPLAY_LIST_PATHS);
        }
    }

    /**
     * Rebuild paths display list (as something has potentially changed).
     */
    public void rebuildPathDisplay() {
        if (mapUIController.getCheckBoxShowAllPaths().isSelected()) {
            if (this.renderManager.displayListExists(DISPLAY_LIST_PATHS)) {
                // Clear display list, then rebuild
                this.renderManager.clearDisplayList(DISPLAY_LIST_PATHS);
                this.renderManager.addPaths(DISPLAY_LIST_PATHS, getFile().getPaths(), MATERIAL_WHITE, MATERIAL_YELLOW, MATERIAL_LIGHT_GREEN);
            }
        }
    }

    /**
     * Reset entities as something has changed.
     */
    public void resetEntities() {
        root3D.getChildren().removeAll(this.entityIcons);
        this.entityIcons.clear();
        setupEntities();
    }

    private void setupEntities() {
        float[] pos = new float[3];
        for (Entity entity : getFile().getEntities()) {
            entity.getPosition(pos, getFile());
            MeshView meshView = makeEntityIcon(entity, pos[0], pos[1], pos[2]);
            meshView.setOnMouseClicked(evt -> this.mapUIController.showEntityInfo(entity));
            this.entityIcons.add(meshView);
        }
    }

    private MeshView makeEntityIcon(Entity entity, float x, float y, float z) {
        float entityIconSize = MapUIController.getPropertyEntityIconSize().getValue();

        FormEntry form = entity.getFormEntry();

        int wadIndex = form.getId();
        if (!form.testFlag(FormEntry.FLAG_NO_MODEL)) {
            WADFile wadFile = getFile().getFileEntry().getMapBook().getWad(getFile());

            if (wadFile.getFiles().size() > wadIndex) {
                WADEntry wadEntry = wadFile.getFiles().get(wadIndex);

                MOFHolder holder = (MOFHolder) wadEntry.getFile();
                holder.setVloFile(getFile().getVlo());
                MeshView view = setupNode(new MeshView(holder.getMofMesh()), x, y, z);
                view.setMaterial(holder.getTextureMap().getPhongMaterial());
                return view;
            }
        }

        TriangleMesh triMesh = new TriangleMesh(VertexFormat.POINT_TEXCOORD);
        triMesh.getPoints().addAll(-entityIconSize * 0.5f, entityIconSize * 0.5f, 0, -entityIconSize * 0.5f, -entityIconSize * 0.5f, 0, entityIconSize * 0.5f, -entityIconSize * 0.5f, 0, entityIconSize * 0.5f, entityIconSize * 0.5f, 0);
        triMesh.getTexCoords().addAll(0, 1, 0, 0, 1, 0, 1, 1);
        triMesh.getFaces().addAll(0, 0, 1, 1, 2, 2, 2, 2, 3, 3, 0, 0);

        MeshView triMeshView = new MeshView(triMesh);
        triMeshView.setDrawMode(DrawMode.FILL);
        triMeshView.setMaterial(MATERIAL_ENTITY_ICON);
        triMeshView.setCullFace(CullFace.NONE);

        return setupNode(triMeshView, x, y, z);
    }

    private <T extends Node> T setupNode(T node, float x, float y, float z) {
        node.setTranslateX(x);
        node.setTranslateY(y);
        node.setTranslateZ(z);

        Rotate lightRotateX = new Rotate(0, Rotate.X_AXIS); // Up, Down,
        Rotate lightRotateY = new Rotate(0, Rotate.Y_AXIS); // Left, Right
        Rotate lightRotateZ = new Rotate(0, Rotate.Z_AXIS); // In, Out
        lightRotateX.angleProperty().bind(rotX.angleProperty());
        lightRotateY.angleProperty().bind(rotY.angleProperty());
        lightRotateZ.angleProperty().bind(rotZ.angleProperty());

        lightRotateX.setPivotY(-node.getTranslateY());
        lightRotateX.setPivotZ(-node.getTranslateZ()); // Depth <Closest, Furthest>
        lightRotateY.setPivotX(-node.getTranslateX()); // <Left, Right>
        lightRotateY.setPivotZ(-node.getTranslateZ()); // Depth <Closest, Furthest>
        lightRotateZ.setPivotX(-node.getTranslateX()); // <Left, Right>
        lightRotateZ.setPivotY(-node.getTranslateY()); // <Up, Down>
        node.getTransforms().addAll(lightRotateX, lightRotateY, lightRotateZ);

        root3D.getChildren().add(node);
        return node;
    }

    private void movePolygonX(int amount) {
        if (getSelectedPolygon() != null) {
            for (int vertice : getSelectedPolygon().getVertices()) {
                SVector vertex = getFile().getVertexes().get(vertice);
                vertex.setX((short) (vertex.getX() + amount));
            }

            refreshView();
        }
    }

    private void movePolygonY(int amount) {
        if (getSelectedPolygon() != null) {
            for (int vertice : getSelectedPolygon().getVertices()) {
                SVector vertex = getFile().getVertexes().get(vertice);
                vertex.setY((short) (vertex.getY() - amount));
            }

            refreshView();
        }
    }

    private void movePolygonZ(int amount) {
        if (getSelectedPolygon() != null) {
            for (int vertice : getSelectedPolygon().getVertices()) {
                SVector vertex = getFile().getVertexes().get(vertice);
                vertex.setZ((short) (vertex.getZ() + amount));
            }

            refreshView();
        }
    }

    /**
     * Calculate geometric center point of selected polygon.
     * @return Center of selected polygon, else null.
     */
    public SVector getCenterOfSelectedPolygon()
    {
        if (getSelectedPolygon() != null) {
            int[] vertexIndices = getSelectedPolygon().getVertices();

            float x = 0.0f;
            float y = 0.0f;
            float z = 0.0f;

            for (int index : vertexIndices) {
                x += mapMesh.getVertices().get(index).getFloatX();
                y += mapMesh.getVertices().get(index).getFloatY();
                z += mapMesh.getVertices().get(index).getFloatZ();
            }

            x /= vertexIndices.length;
            y /= vertexIndices.length;
            z /= vertexIndices.length;

            return new SVector(Utils.floatToFixedPointShort4Bit(x), Utils.floatToFixedPointShort4Bit(y), Utils.floatToFixedPointShort4Bit(z));
        }

        return null;
    }

    /**
     * Supposedly removes the cursor polygon.
     */
    public void removeCursorPolygon() {
        if (this.selectedPolygon == null)
            return;

        this.polygonSelected = false;
        this.selectedPolygon = null;
        hideCursorPolygon();
    }

    /**
     * Hides the cursor polygon.
     */
    public void hideCursorPolygon() {
        if (cursorData == null)
            return;

        mapMesh.getManager().removeMesh(cursorData);
        this.cursorData = null;
    }

    /**
     * Set the polygon that the cursor is hovering over.
     * @param newPoly The poly to highlight.
     */
    public void setCursorPolygon(MAPPolygon newPoly) {
        if (newPoly == this.selectedPolygon || newPoly == this.polygonImmuneToTarget)
            return;

        removeCursorPolygon();
        this.polygonImmuneToTarget = null;
        if (newPoly != null)
            renderCursor(this.selectedPolygon = newPoly);
    }

    private void renderCursor(MAPPolygon cursorPoly) {
        if (cursorPoly == null)
            return;

        renderOverPolygon(cursorPoly, MapMesh.CURSOR_COLOR);
        cursorData = mapMesh.getManager().addMesh();
    }

    /**
     * Render over an existing polygon.
     * @param targetPoly The polygon to render over.
     * @param color      The color to render.
     */
    public void renderOverPolygon(MAPPolygon targetPoly, CursorVertexColor color) {
        int increment = mapMesh.getVertexFormat().getVertexIndexSize();
        boolean isQuad = (targetPoly.getVerticeCount() == MAPPolygon.QUAD_SIZE);

        int face = mapMesh.getPolyFaceMap().get(targetPoly) * mapMesh.getFaceElementSize();
        int v1 = mapMesh.getFaces().get(face);
        int v2 = mapMesh.getFaces().get(face + increment);
        int v3 = mapMesh.getFaces().get(face + (2 * increment));

        if (isQuad) {
            int v4 = mapMesh.getFaces().get(face + (3 * increment));
            int v5 = mapMesh.getFaces().get(face + (4 * increment));
            int v6 = mapMesh.getFaces().get(face + (5 * increment));
            mapMesh.addRectangle(color.getTextureEntry(), v1, v2, v3, v4, v5, v6);
        } else {
            mapMesh.addTriangle(color.getTextureEntry(), v1, v2, v3);
        }
    }

    /**
     * Refresh map data.
     */
    public void refreshView() {
        hideCursorPolygon();
        mapMesh.updateData();
        renderCursor(getSelectedPolygon());
    }

    /**
     * Applies the current lighting setup to the level.
     */
    public void applyLevelLighting() {
        // Clear any existing lighting information
        if (!this.appliedLevelLights.isEmpty()) {
            System.out.println("[ LIGHTING ] Clearing " + this.appliedLevelLights.size() + " lights.");
            this.root3D.getChildren().removeAll(this.appliedLevelLights);
            this.appliedLevelLights.clear();
        }

        // Iterate through each light and apply the the root scene graph node
        System.out.println("[ LIGHTING ] Adding " + getFile().getLights().size() + " lights:");
        for(Light light : getFile().getLights()) {
            switch (light.getApiType()) {
                case AMBIENT:
                    AmbientLight ambLight = new AmbientLight();
                    ambLight.setColor(Utils.fromBGR(light.getColor()));
                    this.appliedLevelLights.add(ambLight);
                    this.root3D.getChildren().add(ambLight);
                    break;

                case PARALLEL:
                    // IMPORTANT! JavaFX does NOT support parallel (directional) lights [AndyEder]
                    PointLight parallelLight = new PointLight();
                    parallelLight.setColor(Utils.fromBGR(light.getColor()));
                    // Use direction as a vector to set a position to simulate a parallel light as best as we can
                    parallelLight.setTranslateX(-light.getDirection().getFloatNormalX() * 1024);
                    parallelLight.setTranslateY(-light.getDirection().getFloatNormalY() * 1024);
                    parallelLight.setTranslateZ(-light.getDirection().getFloatNormalZ() * 1024);
                    this.appliedLevelLights.add(parallelLight);
                    this.root3D.getChildren().add(parallelLight);
                    break;

                case POINT:
                    PointLight pointLight = new PointLight();
                    pointLight.setColor(Utils.fromBGR(light.getColor()));
                    // Assuming direction is position? Are POINT lights ever used? [AndyEder]
                    pointLight.setTranslateX(light.getDirection().getFloatX());
                    pointLight.setTranslateY(light.getDirection().getFloatY());
                    pointLight.setTranslateZ(light.getDirection().getFloatZ());
                    this.appliedLevelLights.add(pointLight);
                    this.root3D.getChildren().add(pointLight);
                    break;
            }
        }
    }
}
