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
import javafx.scene.transform.Translate;
import javafx.stage.Stage;
import lombok.Getter;
import lombok.SneakyThrows;
import net.highwayfrogs.editor.Utils;
import net.highwayfrogs.editor.file.GameFile;
import net.highwayfrogs.editor.file.map.MAPFile;
import net.highwayfrogs.editor.file.map.entity.Entity;
import net.highwayfrogs.editor.file.map.poly.polygon.MAPPolygon;
import net.highwayfrogs.editor.file.map.view.CursorVertexColor;
import net.highwayfrogs.editor.file.map.view.MapMesh;
import net.highwayfrogs.editor.file.map.view.TextureMap;
import net.highwayfrogs.editor.file.standard.SVector;
import net.highwayfrogs.editor.file.vlo.ImageFilterSettings;
import net.highwayfrogs.editor.file.vlo.ImageFilterSettings.ImageState;
import net.highwayfrogs.editor.gui.GUIMain;
import net.highwayfrogs.editor.gui.mesh.MeshData;
import net.highwayfrogs.editor.system.NameValuePair;

import java.util.ArrayList;
import java.util.List;

/**
 * Sets up the map editor.
 * TODO: Edit Vertexes [Add, Remove] Prevent removing a vertice if it is used by anything.
 * TODO: Edit polygons [Add, Remove] This is tricky because it will effect data structures which link to the polygons.
 * TODO: Show Paths. (Can show an icon tracing the path in real-time)
 * Created by Kneesnap on 11/22/2018.
 */
@Getter
public class MAPController extends EditorController<MAPFile> {
    @FXML private TableView<NameValuePair> tableMAPFileData;
    @FXML private TableColumn<Object, Object> tableColumnMAPFileDataName;
    @FXML private TableColumn<Object, Object> tableColumnMAPFileDataValue;

    private double oldMouseX;
    private double oldMouseY;
    private double mouseX;
    private double mouseY;

    private Scene mapScene;
    private MapMesh mapMesh;
    private MAPPolygon selectedPolygon;
    private MAPPolygon polygonImmuneToTarget;
    private boolean polygonSelected;
    private MapUIController mapUIController;

    private PerspectiveCamera camera;

    private List<MeshView> entityIcons = new ArrayList<>();
    private static PhongMaterial entityIconMaterial = new PhongMaterial();

    private List<Box> boundingBoxes = new ArrayList<>();
    private static PhongMaterial boundingBoxMaterial = new PhongMaterial();

    private Group root3D;
    private Rotate rotX;
    private Rotate rotY;
    private Rotate rotZ;

    private MeshData cursorData;

    private static final ImageFilterSettings IMAGE_SETTINGS = new ImageFilterSettings(ImageState.EXPORT);
    private static final Image ENTITY_ICON_IMAGE = GameFile.loadIcon("entity");
    private static final Image BOUNDING_BOX_IMAGE = GameFile.loadIcon("yellow");

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
        addTableEntry("Start Position", "(" + map.getStartXTile() + ", " + map.getStartYTile() + ") Rotation: " + map.getStartRotation());
        addTableEntry("Camera Source", "(" + map.getCameraSourceOffset().toCoordinateString() + ")");
        addTableEntry("Camera Target", "(" + map.getCameraTargetOffset().toCoordinateString() + ")");
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

        // Create and setup material properties for rendering the level, entity icons and bounding boxes.
        PhongMaterial material = new PhongMaterial();
        material.setDiffuseMap(Utils.toFXImage(texMap.getImage(), true));

        entityIconMaterial.setDiffuseColor(Color.BLACK);
        entityIconMaterial.setSpecularColor(Color.BLACK);
        entityIconMaterial.setDiffuseMap(ENTITY_ICON_IMAGE);
        entityIconMaterial.setSelfIlluminationMap(ENTITY_ICON_IMAGE);

        boundingBoxMaterial.setDiffuseColor(Color.BLACK);
        boundingBoxMaterial.setSpecularColor(Color.BLACK);
        boundingBoxMaterial.setDiffuseMap(BOUNDING_BOX_IMAGE);
        boundingBoxMaterial.setSelfIlluminationMap(BOUNDING_BOX_IMAGE);

        // Create mesh view and initialise with xyz rotation transforms, materials and initial face culling policy.
        MeshView meshView = new MeshView(mesh);

        this.rotX = new Rotate(0, Rotate.X_AXIS);
        this.rotY = new Rotate(0, Rotate.Y_AXIS);
        this.rotZ = new Rotate(0, Rotate.Z_AXIS);
        meshView.getTransforms().addAll(rotX, rotY, rotZ);

        meshView.setMaterial(material);
        meshView.setCullFace(CullFace.NONE);

        // Setup a perspective camera through which the 3D view is realised.
        this.camera = new PerspectiveCamera(true);

        // Load FXML for UI layout.
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/javafx/mapui.fxml"));
        Parent loadRoot = fxmlLoader.load();
        // Get the custom mapui controller
        this.mapUIController = fxmlLoader.getController();

        // Create the 3D elements and use them within a subscene.
        this.root3D = new Group(this.camera, meshView);
        SubScene subScene3D = new SubScene(root3D, stageToOverride.getScene().getWidth() - mapUIController.uiRootPaneWidth(), stageToOverride.getScene().getHeight(), true, SceneAntialiasing.BALANCED);

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

        // Input (key) event processing.
        mapScene.setOnKeyPressed(event -> {
            if (getMapUIController() != null && getMapUIController().onKeyPress(event))
                return; // Handled by the other controller.

            // Exit the viewer.
            if (event.getCode() == KeyCode.ESCAPE) {
                if (isPolygonSelected()) {
                    removeCursorPolygon();
                    return;
                }

                Utils.setSceneKeepPosition(stageToOverride, defaultScene);
            }

            // Toggle wireframe mode.
            if (event.getCode() == KeyCode.X)
                meshView.setDrawMode(meshView.getDrawMode() == DrawMode.FILL ? DrawMode.LINE : DrawMode.FILL);

            // Toggle fullscreen mode.
            if (event.isControlDown() && event.getCode() == KeyCode.ENTER)
                stageToOverride.setFullScreen(!stageToOverride.isFullScreen());

            if (isPolygonSelected()) {
                if (event.getCode() == KeyCode.I) {
                    movePolygonY(MapUIController.getPropertyVertexSpeed().get());
                } else if (event.getCode() == KeyCode.K) {
                    movePolygonY(-MapUIController.getPropertyVertexSpeed().get());
                } else if (event.getCode() == KeyCode.J) {
                    movePolygonX(-MapUIController.getPropertyVertexSpeed().get());
                } else if (event.getCode() == KeyCode.L) {
                    movePolygonX(MapUIController.getPropertyVertexSpeed().get());
                }
            }
        });

        mapScene.setOnScroll(evt -> camera.setTranslateZ(camera.getTranslateZ() + (evt.getDeltaY() * MapUIController.getSpeedModifier(evt, MapUIController.getPropertyScrollSpeed()))));

        mapScene.setOnMousePressed(e -> {
            mouseX = oldMouseX = e.getSceneX();
            mouseY = oldMouseY = e.getSceneY();

            if (!isPolygonSelected())
                hideCursorPolygon();
        });

        mapScene.setOnMouseReleased(evt -> {
            hideCursorPolygon();
            renderCursor(getSelectedPolygon());
        });

        mapScene.setOnMouseDragged(e -> {
            oldMouseX = mouseX;
            oldMouseY = mouseY;
            mouseX = e.getSceneX();
            mouseY = e.getSceneY();
            double mouseXDelta = (mouseX - oldMouseX);
            double mouseYDelta = (mouseY - oldMouseY);

            if (e.isPrimaryButtonDown()) {
                rotX.setAngle(rotX.getAngle() + (mouseYDelta * MapUIController.getSpeedModifier(e, MapUIController.getPropertyRotationSpeed()))); // Rotate the object.
                rotY.setAngle(rotY.getAngle() - (mouseXDelta * MapUIController.getSpeedModifier(e, MapUIController.getPropertyRotationSpeed())));
            } else if (e.isMiddleButtonDown()) {
                camera.setTranslateX(camera.getTranslateX() - (mouseXDelta * MapUIController.getSpeedModifier(e, MapUIController.getPropertyTranslateSpeed()))); // Move the camera.
                camera.setTranslateY(camera.getTranslateY() - (mouseYDelta * MapUIController.getSpeedModifier(e, MapUIController.getPropertyTranslateSpeed())));
            }
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

        // Set the initial camera position to somewhere sensible :)
        //  - Maybe calculate this based on some metric rather than supplying arbitrary values?
        camera.setTranslateZ(-1000.0);
        camera.setTranslateY(-100.0);
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
            MeshView meshView = makeIcon(pos[0], pos[1], pos[2]);
            meshView.setOnMouseClicked(evt -> this.mapUIController.showEntityInfo(entity));
            this.entityIcons.add(meshView);
        }
    }

    private MeshView makeIcon(float x, float y, float z) {
        float entityIconSize = MapUIController.getPropertyEntityIconSize().getValue();

        TriangleMesh triMesh = new TriangleMesh(VertexFormat.POINT_TEXCOORD);
        triMesh.getPoints().addAll(-entityIconSize * 0.5f, entityIconSize * 0.5f, 0, -entityIconSize * 0.5f, -entityIconSize * 0.5f, 0, entityIconSize * 0.5f, -entityIconSize * 0.5f, 0, entityIconSize * 0.5f, entityIconSize * 0.5f, 0);
        triMesh.getTexCoords().addAll(0, 1, 0, 0, 1, 0, 1, 1);
        triMesh.getFaces().addAll(0,0, 1,1, 2,2, 2,2, 3,3, 0,0);

        MeshView triMeshView = new MeshView(triMesh);
        triMeshView.setDrawMode(DrawMode.FILL);
        triMeshView.setMaterial(entityIconMaterial);
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

    /**
     * Adds an axis-aligned bounding box.
     * @param minX The minimum x-coordinate.
     * @param minY The minimum y-coordinate.
     * @param minZ The minimum z-coordinate.
     * @param maxX The maximum x-coordinate.
     * @param maxY The maximum y-coordinate.
     * @param maxZ The maximum z-coordinate.
     */
    private void addBoundingBoxFromMinMax(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
        final double x0 = Math.min(minX, maxX);
        final double x1 = Math.max(minX, maxX);
        final double y0 = Math.min(minY, maxY);
        final double y1 = Math.max(minY, maxY);
        final double z0 = Math.min(minZ, maxZ);
        final double z1 = Math.max(minZ, maxZ);

        final double x = (x0 + x1) * 0.5;
        final double y = (y0 + y1) * 0.5;
        final double z = (z0 + z1) * 0.5;

        final double w = (x1 - x0);
        final double h = (y1 - y0);
        final double d = (z1 - z0);

        addBoundingBoxCenteredWithDimensions(x, y, z, w, h, d);
    }

    /**
     * Adds an axis-aligned bounding box.
     * @param x The x-coordinate defining the center of the box.
     * @param y The y-coordinate defining the center of the box.
     * @param z The z-coordinate defining the center of the box.
     * @param width The width (along x-axis).
     * @param height The height (along y-axis).
     * @param depth The depth (along z-axis).
     */
    private void addBoundingBoxCenteredWithDimensions(double x, double y, double z, double width, double height, double depth) {
        Box axisAlignedBoundingBox = new Box(width, height, depth);

        axisAlignedBoundingBox.setMaterial(boundingBoxMaterial);
        axisAlignedBoundingBox.setDrawMode(DrawMode.LINE);
        axisAlignedBoundingBox.setCullFace(CullFace.BACK);
        axisAlignedBoundingBox.getTransforms().addAll(rotX, rotY, rotZ, new Translate(x, y, z));

        root3D.getChildren().add(axisAlignedBoundingBox);
        this.boundingBoxes.add(axisAlignedBoundingBox);
    }

    /**
     * Removes all bounding boxes.
     */
    private void removeBoundingBoxes() {
        root3D.getChildren().removeAll(this.boundingBoxes);
        this.boundingBoxes.clear();
    }

    private void movePolygonX(int amount) {
        for (int vertice : getSelectedPolygon().getVertices()) {
            SVector vertex = getFile().getVertexes().get(vertice);
            vertex.setX((short) (vertex.getX() + amount));
        }

        refreshView();
    }

    private void movePolygonY(int amount) {
        for (int vertice : getSelectedPolygon().getVertices()) {
            SVector vertex = getFile().getVertexes().get(vertice);
            vertex.setY((short) (vertex.getY() - amount));
        }

        refreshView();
    }

    private void movePolygonZ(int amount) {
        for (int vertice : getSelectedPolygon().getVertices()) {
            SVector vertex = getFile().getVertexes().get(vertice);
            vertex.setZ((short) (vertex.getZ() + amount));
        }

        refreshView();
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
}
