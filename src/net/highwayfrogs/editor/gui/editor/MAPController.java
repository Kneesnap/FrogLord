package net.highwayfrogs.editor.gui.editor;

import javafx.embed.swing.SwingFXUtils;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Group;
import javafx.scene.PerspectiveCamera;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.AnchorPane;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.CullFace;
import javafx.scene.shape.DrawMode;
import javafx.scene.shape.MeshView;
import javafx.scene.transform.Rotate;
import javafx.stage.Stage;
import javafx.util.Pair;
import lombok.SneakyThrows;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.Utils;
import net.highwayfrogs.editor.file.map.MAPFile;
import net.highwayfrogs.editor.file.map.view.MapMesh;
import net.highwayfrogs.editor.file.map.view.TextureMap;
import net.highwayfrogs.editor.file.standard.psx.prims.polygon.PSXPolygon;
import net.highwayfrogs.editor.gui.GUIMain;
import net.highwayfrogs.editor.gui.InputMenu;

import java.util.List;

/**
 * Sets up the map editor.
 * Created by Kneesnap on 11/22/2018.
 */
public class MAPController extends EditorController<MAPFile> {
    @FXML private Label themeIdLabel;
    @FXML private Label startPosLabel;
    @FXML private Label cameraSourceLabel;
    @FXML private Label cameraTargetLabel;
    @FXML private Label basePointLabel; // This is the bottom left of the map group grid.

    @FXML private Label pathCountLabel;
    @FXML private Label formCountLabel;
    @FXML private Label entityCountLabel;

    @FXML private Label gridSquareLabel;
    @FXML private Label gridStackLabel;
    @FXML private Label gridCountLabel;
    @FXML private Label gridLengthLabel;
    @FXML private Label groupLabel;
    @FXML private Label groupCountLabel;
    @FXML private Label groupLengthLabel;

    @FXML private Label zoneCountLabel;
    @FXML private Label lightCountLabel;
    @FXML private Label vertexCountLabel;
    @FXML private Label polygonCountLabel;
    @FXML private Label mapAnimCountLabel;

    private double oldMouseX;
    private double oldMouseY;
    private double mouseX;
    private double mouseY;

    private MapMesh mapMesh;
    private PSXPolygon selectedPolygon;

    private static final double ROTATION_SPEED = 0.35D;
    private static final double SCROLL_SPEED = 5;
    private static final double TRANSLATE_SPEED = 10;

    @Override
    public void onInit(AnchorPane editorRoot) {
        super.onInit(editorRoot);
        updateLabels();
    }

    private void updateLabels() {
        MAPFile map = getFile();

        themeIdLabel.setText("Theme: " + map.getTheme());
        startPosLabel.setText("Start Pos: (" + map.getStartXTile() + ", " + map.getStartYTile() + ") Rotation: " + map.getStartRotation());
        cameraSourceLabel.setText("Camera Source: (" + map.getCameraSourceOffset().toCoordinateString() + ")");
        cameraTargetLabel.setText("Camera Target: (" + map.getCameraTargetOffset().toCoordinateString() + ")");
        basePointLabel.setText("Base Point: (" + map.getBasePoint().toCoordinateString() + ")");

        // Labels in entity section.
        pathCountLabel.setText("Paths: " + map.getPaths().size());
        formCountLabel.setText("Forms: " + map.getForms().size());
        entityCountLabel.setText("Entities: " + map.getEntities().size());

        // Labels in environment section.
        zoneCountLabel.setText("Zones: " + map.getZones().size());
        lightCountLabel.setText("Lights: " + map.getLights().size());
        vertexCountLabel.setText("Vertices: " + map.getVertexes().size());
        polygonCountLabel.setText("Polygons: " + map.getCachedPolygons().values().stream().mapToInt(List::size).sum());
        mapAnimCountLabel.setText("Animations: " + map.getMapAnimations().size());

        // Grid
        gridStackLabel.setText("Grid Stacks: " + map.getGridStacks().size());
        gridSquareLabel.setText("Grid Squares: " + map.getGridSquares().size());
        gridCountLabel.setText("Count: [" + map.getGridXCount() + ", " + map.getGridZCount() + "]");
        gridLengthLabel.setText("Length: [" + map.getGridXLength() + ", " + map.getGridZLength() + "]");

        // Group
        groupLabel.setText("Groups: " + map.getGroups().size());
        groupCountLabel.setText("Count: [" + map.getGroupXCount() + ", " + map.getGroupZCount() + "]");
        groupLengthLabel.setText("Length: [" + map.getGroupXLength() + ", " + map.getGroupZLength() + "]");
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

                String levelName = str.toUpperCase();
                if (GUIMain.EXE_CONFIG.hasRemapInfo(levelName)) {
                    Pair<Integer, Integer> remapData = GUIMain.EXE_CONFIG.getRemapInfo(levelName);
                    address = remapData.getKey() + (Constants.SHORT_SIZE * remapData.getValue());
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
        MeshView meshView = new MeshView(mesh);

        PhongMaterial material = new PhongMaterial();
        material.setDiffuseMap(SwingFXUtils.toFXImage(texMap.getImage(), null));
        meshView.setMaterial(material);

        meshView.setCullFace(CullFace.NONE);
        meshView.setTranslateZ(50);
        meshView.setScaleX(Constants.MAP_VIEW_SCALE);
        meshView.setScaleY(Constants.MAP_VIEW_SCALE);
        meshView.setScaleZ(Constants.MAP_VIEW_SCALE);

        PerspectiveCamera camera = new PerspectiveCamera(true);
        camera.setFarClip(Double.MAX_VALUE);
        camera.setTranslateZ(-500);

        Group cameraGroup = new Group();
        cameraGroup.getChildren().add(meshView);
        cameraGroup.getChildren().add(camera);

        Rotate rotX = new Rotate(0, Rotate.X_AXIS);
        Rotate rotY = new Rotate(0, Rotate.Y_AXIS);
        meshView.getTransforms().addAll(rotX, rotY);

        Scene mapScene = new Scene(cameraGroup, 400, 400, true);
        mapScene.setFill(Color.GRAY);
        mapScene.setCamera(camera);

        Scene defaultScene = Utils.setSceneKeepPosition(stageToOverride, mapScene);

        mapScene.setOnKeyPressed(event -> {

            // Exit the viewer.
            if (event.getCode() == KeyCode.ESCAPE)
                Utils.setSceneKeepPosition(stageToOverride, defaultScene);

            // Toggle wireframe mode.
            if (event.getCode() == KeyCode.X)
                meshView.setDrawMode(meshView.getDrawMode() == DrawMode.FILL ? DrawMode.LINE : DrawMode.FILL);

            // [Remap Mode] Find next non-crashing remap.
            if (event.getCode() == KeyCode.K)
                mesh.findNextValidRemap();
        });

        mapScene.setOnScroll(evt -> camera.setTranslateZ(camera.getTranslateZ() + (evt.getDeltaY() * SCROLL_SPEED)));

        mapScene.setOnMousePressed(e -> {
            mouseX = oldMouseX = e.getSceneX();
            mouseY = oldMouseY = e.getSceneY();
        });

        mapScene.setOnMouseDragged(e -> {
            oldMouseX = mouseX;
            oldMouseY = mouseY;
            mouseX = e.getSceneX();
            mouseY = e.getSceneY();
            double mouseXDelta = (mouseX - oldMouseX);
            double mouseYDelta = (mouseY - oldMouseY);

            if (e.isPrimaryButtonDown()) {
                rotX.setAngle(rotX.getAngle() + (mouseYDelta * ROTATION_SPEED)); // Rotate the object.
                rotY.setAngle(rotY.getAngle() - (mouseXDelta * ROTATION_SPEED));
            } else if (e.isMiddleButtonDown()) {
                camera.setTranslateX(camera.getTranslateX() - (mouseXDelta * TRANSLATE_SPEED)); // Move the camera.
                camera.setTranslateY(camera.getTranslateY() - (mouseYDelta * TRANSLATE_SPEED));
            }
        });

        mapScene.setOnMouseMoved(evt ->
                setCursorPolygon(mesh.getFacePolyMap().get(evt.getPickResult().getIntersectedFace())));
        mesh.findNextValidRemap();
    }

    /**
     * Supposedly removes the cursor polygon.
     */
    public void removeCursorPolygon() {
        if (this.selectedPolygon == null)
            return;

        this.selectedPolygon = null;
        mapMesh.getFaces().resize(mapMesh.getFaceCount());
        mapMesh.getTexCoords().resize(mapMesh.getTextureCount());
    }

    /**
     * Set the polygon that the cursor is hovering over.
     * @param newPoly The poly to highlight.
     */
    public void setCursorPolygon(PSXPolygon newPoly) {
        if (newPoly == this.selectedPolygon)
            return;

        removeCursorPolygon();
        if (newPoly != null)
            renderCursor(this.selectedPolygon = newPoly);
    }

    private void renderCursor(PSXPolygon cursorPoly) {
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
}
