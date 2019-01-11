package net.highwayfrogs.editor.gui.editor;

import javafx.beans.value.ChangeListener;
import javafx.embed.swing.SwingFXUtils;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.*;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.paint.ImagePattern;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.*;
import javafx.scene.transform.Rotate;
import javafx.stage.Stage;
import lombok.Getter;
import lombok.SneakyThrows;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.Utils;
import net.highwayfrogs.editor.file.GameFile;
import net.highwayfrogs.editor.file.map.MAPFile;
import net.highwayfrogs.editor.file.map.entity.Entity;
import net.highwayfrogs.editor.file.map.path.Path;
import net.highwayfrogs.editor.file.map.path.PathInfo;
import net.highwayfrogs.editor.file.map.view.MapMesh;
import net.highwayfrogs.editor.file.map.view.TextureMap;
import net.highwayfrogs.editor.file.standard.SVector;
import net.highwayfrogs.editor.file.standard.psx.PSXMatrix;
import net.highwayfrogs.editor.file.standard.psx.prims.polygon.PSXPolyTexture;
import net.highwayfrogs.editor.file.standard.psx.prims.polygon.PSXPolygon;
import net.highwayfrogs.editor.file.vlo.ImageFilterSettings;
import net.highwayfrogs.editor.file.vlo.ImageFilterSettings.ImageState;
import net.highwayfrogs.editor.gui.GUIMain;
import net.highwayfrogs.editor.gui.InputMenu;
import net.highwayfrogs.editor.gui.SelectionMenu;
import net.highwayfrogs.editor.system.NameValuePair;
import net.highwayfrogs.editor.system.Tuple2;

import java.util.List;

/**
 * Sets up the map editor.
 * TODO: Icon for lighting.
 * TODO: Icon for animations, if we're unable to make them render in our tool.
 * TODO: Grid mode,
 * TODO: Edit Vertexes
 * TODO: Edit polygons
 * TODO: Show Paths.
 * TODO: Zones.
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

    private PerspectiveCamera camera;

    private static final ImageFilterSettings IMAGE_SETTINGS = new ImageFilterSettings(ImageState.EXPORT);
    private static final Image LIGHT_BULB = GameFile.loadIcon("lightbulb");
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
        addTableEntry("Grid Squares", Integer.toString(map.getGridSquares().size()));
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

                setupMapViewer(GUIMain.MAIN_STAGE, new MapMesh(getFile(), texMap, address, vlo.getImages().size()), texMap);
            });

        }, false);
    }


    @SneakyThrows
    private void setupMapViewer(Stage stageToOverride, MapMesh mesh, TextureMap texMap) {
        this.mapMesh = mesh;

        // Create and setup material properties for rendering the level.
        PhongMaterial material = new PhongMaterial();
        material.setDiffuseMap(SwingFXUtils.toFXImage(texMap.getImage(), null));

        // Create mesh view and initialise with xyz rotation transforms, materials and initial face culling policy.
        MeshView meshView = new MeshView(mesh);

        Rotate rotX = new Rotate(0, Rotate.X_AXIS);
        Rotate rotY = new Rotate(0, Rotate.Y_AXIS);
        Rotate rotZ = new Rotate(0, Rotate.Z_AXIS);
        meshView.getTransforms().addAll(rotX, rotY, rotZ);

        meshView.setMaterial(material);
        meshView.setCullFace(CullFace.NONE);

        // Setup a perspective camera through which the 3D view is realised.
        this.camera = new PerspectiveCamera(true);

        // Load FXML for UI layout.
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/javafx/mapui.fxml"));
        Parent loadRoot = fxmlLoader.load();
        // Get the custom mapui controller
        MapUIController mapUIController = fxmlLoader.getController();

        // Create the 3D elements and use them within a subscene.
        Group root3D = new Group(this.camera, meshView);
        SubScene subScene3D = new SubScene(root3D, stageToOverride.getScene().getWidth() - mapUIController.uiRootPaneWidth(), stageToOverride.getScene().getHeight(), true, SceneAntialiasing.BALANCED);

        //  Setup mapui controller bindings, etc.
        mapUIController.setupBindings(subScene3D, this.mapMesh, meshView, rotX, rotY, rotZ, this.camera);

        // Setup the UI layout.
        BorderPane uiPane = new BorderPane();
        uiPane.setLeft(loadRoot);
        uiPane.setCenter(subScene3D);

        // Setup additional scene elements.
        setupLights(root3D, rotX, rotY, rotZ);
        setupEntities(root3D, rotX, rotY, rotZ);

        // Create and set the scene.
        mapScene = new Scene(uiPane);
        Scene defaultScene = Utils.setSceneKeepPosition(stageToOverride, mapScene);

        // Listener that attempts to (partially) handle window resizing. Does not support Maximize / Fullscreen (yet).
        ChangeListener<Number> stageSizeListener = (observable, oldValue, newValue) -> {
            System.out.println("Height: " + stageToOverride.getHeight() + " Width: " + stageToOverride.getWidth());
            subScene3D.setWidth(stageToOverride.getScene().getWidth() - mapUIController.uiRootPaneWidth());
            subScene3D.setHeight(stageToOverride.getScene().getHeight());
        };

        stageToOverride.widthProperty().addListener(stageSizeListener);
        stageToOverride.heightProperty().addListener(stageSizeListener);

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

            // Toggle mesh visibility.
            if (event.getCode() == KeyCode.V)
                meshView.setVisible(!meshView.isVisible());

            // Cycle through face culling modes (NONE, BACK, FRONT).
            if (event.getCode() == KeyCode.C)
                meshView.setCullFace(CullFace.values()[(meshView.getCullFace().ordinal() + 1) % CullFace.values().length]);

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

        mapScene.setOnScroll(evt -> camera.setTranslateZ(camera.getTranslateZ() + (evt.getDeltaY() * MapUIController.getSpeedModifier(evt.isControlDown(), evt.isAltDown(), MapUIController.getPropertyScrollSpeed().get()))));

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
                rotX.setAngle(rotX.getAngle() + (mouseYDelta * MapUIController.getSpeedModifier(e.isControlDown(), e.isAltDown(), MapUIController.getPropertyRotationSpeed().get()))); // Rotate the object.
                rotY.setAngle(rotY.getAngle() - (mouseXDelta * MapUIController.getSpeedModifier(e.isControlDown(), e.isAltDown(), MapUIController.getPropertyRotationSpeed().get())));
            } else if (e.isMiddleButtonDown()) {
                camera.setTranslateX(camera.getTranslateX() - (mouseXDelta * MapUIController.getSpeedModifier(e.isControlDown(), e.isAltDown(), MapUIController.getPropertyTranslateSpeed().get()))); // Move the camera.
                camera.setTranslateY(camera.getTranslateY() - (mouseYDelta * MapUIController.getSpeedModifier(e.isControlDown(), e.isAltDown(), MapUIController.getPropertyTranslateSpeed().get())));
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
                } else {
                    setCursorPolygon(clickedPoly);
                    this.polygonSelected = true;
                }
            }
        });

        mesh.findNextValidRemap(0, 0, false);
        camera.setTranslateZ(-MapUIController.getPropertyMapViewScale().get());
        camera.setTranslateY(-MapUIController.getPropertyMapViewScale().get() / 7.0);
    }

    private void setupLights(Group root3D, Rotate rotX, Rotate rotY, Rotate rotZ) {
        /*ImagePattern pattern = new ImagePattern(LIGHT_BULB);

        for (Light light : getFile().getLights()) {
            SVector position = light.getPosition();
            makeIcon(root3D, pattern, rotX, rotY, Utils.unsignedShortToFloat(position.getX()), Utils.unsignedShortToFloat(position.getY()), Utils.unsignedShortToFloat(position.getZ()));
        }*/
    }

    private void setupEntities(Group root3D, Rotate rotX, Rotate rotY, Rotate rotZ) {
        ImagePattern pattern = new ImagePattern(SWAMPY);

        for (Entity entity : getFile().getEntities()) {
            PSXMatrix matrix = entity.getMatrixInfo();
            if (matrix != null) {
                int[] pos = matrix.getTransform();
                float x = Utils.unsignedIntToFloat(pos[0]);
                float y = Utils.unsignedIntToFloat(pos[1]);
                float z = Utils.unsignedIntToFloat(pos[2]);

                Rectangle rect = makeIcon(root3D, pattern, rotX, rotY, rotZ, x, y, z);
                rect.setOnMouseClicked(evt -> {
                    System.out.println("Hello, I am a " + entity.getFormBook());

                    System.out.println("Base: [" + evt.getX() + ", " + evt.getY() + ", " + evt.getZ() + "]");
                    System.out.println("Scene: [" + evt.getSceneX() + ", " + evt.getSceneY() + "]");
                    System.out.println("Screen: [" + evt.getScreenX() + ", " + evt.getScreenY() + "]");
                });
            }

            PathInfo pathInfo = entity.getPathInfo();
            if (pathInfo != null) {
                Path path = getFile().getPaths().get(pathInfo.getPathId());
                SVector end = path.evaluatePosition(pathInfo);

                float x = Utils.unsignedShortToFloat(end.getX());
                float y = Utils.unsignedShortToFloat(end.getY());
                float z = Utils.unsignedShortToFloat(end.getZ());

                Rectangle rect = makeIcon(root3D, pattern, rotX, rotY, rotZ, x, y, z);
                rect.setOnMouseClicked(evt -> System.out.println("Hello, I am a " + entity.getFormBook()));
            }
        }
    }

    private Rectangle makeIcon(Group root3D, ImagePattern image, Rotate rotX, Rotate rotY, Rotate rotZ, float x, float y, float z) {
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
        mapMesh.getFaces().resize(mapMesh.getFaceCount());
        mapMesh.getTexCoords().resize(mapMesh.getTextureCount());
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

        int increment = mapMesh.getVertexFormat().getVertexIndexSize();
        boolean isQuad = (cursorPoly.getVertices().length == PSXPolygon.QUAD_SIZE);

        int face = mapMesh.getPolyFaceMap().get(cursorPoly) * mapMesh.getFaceElementSize();
        int v1 = mapMesh.getFaces().get(face);
        int v2 = mapMesh.getFaces().get(face + increment);
        int v3 = mapMesh.getFaces().get(face + (2 * increment));

        if (isQuad) {
            int v4 = mapMesh.getFaces().get(face + (3 * increment));
            int v5 = mapMesh.getFaces().get(face + (4 * increment));
            int v6 = mapMesh.getFaces().get(face + (5 * increment));
            mapMesh.addRectangle(MapMesh.CURSOR_COLOR.getTextureEntry(), v1, v2, v3, v4, v5, v6);
        } else {
            mapMesh.addTriangle(MapMesh.CURSOR_COLOR.getTextureEntry(), v1, v2, v3);
        }
    }

    /**
     * Refresh map data.
     */
    public void refreshView() {
        mapMesh.updateData();
        renderCursor(getSelectedPolygon());
    }
}
