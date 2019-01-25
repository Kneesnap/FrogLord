package net.highwayfrogs.editor.gui.editor;

import javafx.embed.swing.SwingFXUtils;
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
import javafx.scene.paint.ImagePattern;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.CullFace;
import javafx.scene.shape.DrawMode;
import javafx.scene.shape.MeshView;
import javafx.scene.shape.Rectangle;
import javafx.scene.transform.Rotate;
import javafx.stage.Stage;
import lombok.Getter;
import lombok.SneakyThrows;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.Utils;
import net.highwayfrogs.editor.file.GameFile;
import net.highwayfrogs.editor.file.map.MAPFile;
import net.highwayfrogs.editor.file.map.entity.Entity;
import net.highwayfrogs.editor.file.map.view.CursorVertexColor;
import net.highwayfrogs.editor.file.map.view.MapMesh;
import net.highwayfrogs.editor.file.map.view.TextureMap;
import net.highwayfrogs.editor.file.standard.SVector;
import net.highwayfrogs.editor.file.standard.psx.prims.polygon.PSXPolyTexture;
import net.highwayfrogs.editor.file.standard.psx.prims.polygon.PSXPolygon;
import net.highwayfrogs.editor.file.vlo.ImageFilterSettings;
import net.highwayfrogs.editor.file.vlo.ImageFilterSettings.ImageState;
import net.highwayfrogs.editor.gui.GUIMain;
import net.highwayfrogs.editor.gui.InputMenu;
import net.highwayfrogs.editor.gui.SelectionMenu;
import net.highwayfrogs.editor.gui.mesh.MeshData;
import net.highwayfrogs.editor.system.NameValuePair;
import net.highwayfrogs.editor.system.Tuple2;

import java.util.ArrayList;
import java.util.List;

/**
 * Sets up the map editor.
 * TODO: Grid Editor.
 * TODO: Edit Vertexes
 * TODO: Edit polygons
 * TODO: Show Paths. (Can show an icon tracing the path in real-time)
 * TODO: Zones.
 * TODO: Forms?
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
    private PSXPolygon selectedPolygon;
    private PSXPolygon polygonImmuneToTarget;
    private boolean polygonSelected;
    private MapUIController mapUIController;

    private PerspectiveCamera camera;
    private List<Rectangle> entityIcons = new ArrayList<>();

    private Group root3D;
    private Rotate rotX;
    private Rotate rotY;
    private Rotate rotZ;

    private MeshData cursorData;

    private static final ImageFilterSettings IMAGE_SETTINGS = new ImageFilterSettings(ImageState.EXPORT);
    private static final Image SWAMPY = GameFile.loadIcon("swampy");

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
        addTableEntry("Base Point", "(" + map.getBasePoint().toCoordinateString() + ")");

        // Entity properties
        addTableEntry("Path Count", Integer.toString(map.getPaths().size()));
        addTableEntry("Form Count", Integer.toString(map.getForms().size()));
        addTableEntry("Entity Count", Integer.toString(map.getEntities().size()));

        // Environment properties
        addTableEntry("Zone Count", Integer.toString(map.getZones().size()));
        addTableEntry("Light Count", Integer.toString(map.getLights().size()));
        addTableEntry("Vertex Count", Integer.toString(map.getVertexes().size()));
        addTableEntry("Polygon Count", Integer.toString(map.getCachedPolygons().values().stream().mapToInt(List::size).sum()));
        addTableEntry("Animation Count", Integer.toString(map.getMapAnimations().size()));

        // Grid properties
        addTableEntry("Grid Stacks", Integer.toString(map.getGridStacks().size()));
        addTableEntry("Grid Size Count", "[" + map.getGridXCount() + ", " + map.getGridZCount() + "]");
        addTableEntry("Grid Size Length", "[" + map.getGridXLength() + ", " + map.getGridZLength() + "]");

        // Group properties
        addTableEntry("Group Count", Integer.toString(map.getGroups().size()));
        addTableEntry("Group Size Count", "[" + map.getGroupXCount() + ", " + map.getGroupZCount() + "]");
        addTableEntry("Group Size Length", "[" + map.getGroupXLength() + ", " + map.getGroupZLength() + "]");
    }

    private void addTableEntry(String name, String value) {
        tableMAPFileData.getItems().add(new NameValuePair(name, value));
    }

    @FXML
    private void onMapButtonClicked(ActionEvent event) {
        getFile().getParentMWD().promptVLOSelection(getFile().getTheme(), vlo -> {
            TextureMap textureMap = TextureMap.newTextureMap(getFile(), vlo, getMWIEntry().getDisplayName());
            getFile().setSuppliedVLO(vlo);
            getFile().setSuppliedRemapAddress(GUIMain.EXE_CONFIG.getRemapInfo(Utils.stripExtension(getMWIEntry().getDisplayName())).getA());
            setupMapViewer(GUIMain.MAIN_STAGE, new MapMesh(getFile(), textureMap), textureMap);
        }, false);
    }

    @FXML
    private void onRemapButtonClicked(ActionEvent event) {
        getFile().getParentMWD().promptVLOSelection(getFile().getTheme(), vlo -> {
            TextureMap texMap = TextureMap.newTextureMap(getFile(), vlo, null);

            InputMenu.promptInput("Please enter the address to start reading from.", str -> {
                int address;

                String levelName = Utils.getRawFileName(str);
                if (GUIMain.EXE_CONFIG.hasRemapInfo(levelName)) {
                    Tuple2<Integer, Integer> remapData = GUIMain.EXE_CONFIG.getRemapInfo(levelName);
                    address = remapData.getA() + (Constants.SHORT_SIZE * remapData.getB());
                } else {
                    try {
                        address = Integer.decode(str);
                    } catch (Exception ex) {
                        System.out.println(str + " is not formatted properly.");
                        return;
                    }
                }

                getFile().setSuppliedVLO(vlo);
                getFile().setSuppliedRemapAddress(address);
                setupMapViewer(GUIMain.MAIN_STAGE, new MapMesh(getFile(), texMap, address, vlo.getImages().size()), texMap);
            });

        }, false);
    }

    @SneakyThrows
    private void setupMapViewer(Stage stageToOverride, MapMesh mesh, TextureMap texMap) {
        this.mapMesh = mesh;

        // These cause errors if not reset.
        this.cursorData = null;

        // Create and setup material properties for rendering the level.
        PhongMaterial material = new PhongMaterial();
        material.setDiffuseMap(SwingFXUtils.toFXImage(texMap.getImage(), null));

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

        // Create and set the scene.
        mapScene = new Scene(uiPane);
        Scene defaultScene = Utils.setSceneKeepPosition(stageToOverride, mapScene);

        // Handle scaling of SubScene on stage resizing.
        mapScene.widthProperty().addListener((observable, old, newVal) -> subScene3D.setWidth(newVal.doubleValue() - mapUIController.uiRootPaneWidth()));
        subScene3D.heightProperty().bind(mapScene.heightProperty());

        // Input (key) event processing.
        mapScene.setOnKeyPressed(event -> {
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

            // [Remap Mode] Find next non-crashing remap.
            if (mesh.isRemapFinder() && event.getCode() == KeyCode.K) {
                if (!isPolygonSelected()) {
                    System.out.println("You must select a polygon to perform a remap search.");
                    return;
                }

                PSXPolygon poly = this.selectedPolygon;
                if (!(poly instanceof PSXPolyTexture)) {
                    System.out.println("This polygon is not textured.");
                    return;
                }

                int replaceTexId = ((PSXPolyTexture) poly).getTextureId();
                SelectionMenu.promptSelection("Select the replacement image.",
                        image -> mesh.findNextValidRemap(replaceTexId, image.getTextureId(), true),
                        mesh.getTextureMap().getVloArchive().getImages(),
                        image -> String.valueOf(image.getTextureId()),
                        image -> SelectionMenu.makeIcon(image.toBufferedImage(IMAGE_SETTINGS)));
            }

            if (isPolygonSelected()) {
                if (event.getCode() == KeyCode.UP) {
                    movePolygonY(MapUIController.getPropertyVertexSpeed().get());
                } else if (event.getCode() == KeyCode.DOWN) {
                    movePolygonY(-MapUIController.getPropertyVertexSpeed().get());
                } else if (event.getCode() == KeyCode.LEFT) {
                    movePolygonX(-MapUIController.getPropertyVertexSpeed().get());
                } else if (event.getCode() == KeyCode.RIGHT) {
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
            PSXPolygon clickedPoly = getMapMesh().getFacePolyMap().get(evt.getPickResult().getIntersectedFace());

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

        mesh.findNextValidRemap(0, 0, false);
        camera.setTranslateZ(-MapUIController.getPropertyMapViewScale().get());
        camera.setTranslateY(-MapUIController.getPropertyMapViewScale().get() / 7.0);
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
        ImagePattern pattern = new ImagePattern(SWAMPY);

        for (Entity entity : getFile().getEntities()) {
            float[] pos = entity.getPosition(getFile());
            Rectangle rect = makeIcon(pattern, pos[0], pos[1], pos[2]);
            rect.setOnMouseClicked(evt -> this.mapUIController.showEntityInfo(entity));
            this.entityIcons.add(rect);
        }
    }

    private Rectangle makeIcon(ImagePattern image, float x, float y, float z) {
        double width = image.getImage().getWidth();
        double height = image.getImage().getHeight();
        Rectangle rect = new Rectangle(width, height);

        rect.setTranslateX((MapUIController.getPropertyMapViewScale().get() * x) - width);
        rect.setTranslateY((MapUIController.getPropertyMapViewScale().get() * y) - height);
        rect.setTranslateZ((MapUIController.getPropertyMapViewScale().get() * z));
        rect.setFill(image);

        Rotate lightRotateX = new Rotate(0, Rotate.X_AXIS); // Up, Down,
        Rotate lightRotateY = new Rotate(0, Rotate.Y_AXIS); // Left, Right
        Rotate lightRotateZ = new Rotate(0, Rotate.Z_AXIS); // In, Out
        lightRotateX.angleProperty().bind(rotX.angleProperty());
        lightRotateY.angleProperty().bind(rotY.angleProperty());
        lightRotateZ.angleProperty().bind(rotZ.angleProperty());

        lightRotateX.setPivotY(-rect.getTranslateY());
        lightRotateX.setPivotZ(-rect.getTranslateZ()); // Depth <Closest, Furthest>
        lightRotateY.setPivotX(-rect.getTranslateX()); // <Left, Right>
        lightRotateY.setPivotZ(-rect.getTranslateZ()); // Depth <Closest, Furthest>
        lightRotateZ.setPivotX(-rect.getTranslateX()); // <Left, Right>
        lightRotateZ.setPivotY(-rect.getTranslateY()); // <Up, Down>
        rect.getTransforms().addAll(lightRotateX, lightRotateY, lightRotateZ);

        root3D.getChildren().add(rect);
        return rect;
    }

    private void movePolygonX(int amount) {
        for (short vertice : getSelectedPolygon().getVertices()) {
            SVector vertex = getFile().getVertexes().get(vertice);
            vertex.setX((short) (vertex.getX() + amount));
        }

        refreshView();
    }

    private void movePolygonY(int amount) {
        for (short vertice : getSelectedPolygon().getVertices()) {
            SVector vertex = getFile().getVertexes().get(vertice);
            vertex.setY((short) (vertex.getY() - amount));
        }

        refreshView();
    }

    private void movePolygonZ(int amount) {
        for (short vertice : getSelectedPolygon().getVertices()) {
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
    public void setCursorPolygon(PSXPolygon newPoly) {
        if (newPoly == this.selectedPolygon || newPoly == this.polygonImmuneToTarget)
            return;

        removeCursorPolygon();
        this.polygonImmuneToTarget = null;
        if (newPoly != null)
            renderCursor(this.selectedPolygon = newPoly);
    }

    private void renderCursor(PSXPolygon cursorPoly) {
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
    public void renderOverPolygon(PSXPolygon targetPoly, CursorVertexColor color) {
        int increment = mapMesh.getVertexFormat().getVertexIndexSize();
        boolean isQuad = (targetPoly.getVertices().length == PSXPolygon.QUAD_SIZE);

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
