package net.highwayfrogs.editor.gui.editor;

import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.*;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.BorderPane;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.CullFace;
import javafx.scene.shape.DrawMode;
import javafx.scene.shape.MeshView;
import javafx.stage.Stage;
import lombok.Getter;
import lombok.SneakyThrows;
import net.highwayfrogs.editor.file.MWDFile;
import net.highwayfrogs.editor.file.config.data.MAPLevel;
import net.highwayfrogs.editor.file.config.exe.LevelInfo;
import net.highwayfrogs.editor.file.map.MAPFile;
import net.highwayfrogs.editor.file.map.poly.polygon.MAPPolygon;
import net.highwayfrogs.editor.file.map.view.CursorVertexColor;
import net.highwayfrogs.editor.file.map.view.MapMesh;
import net.highwayfrogs.editor.file.map.view.TextureMap;
import net.highwayfrogs.editor.file.standard.SVector;
import net.highwayfrogs.editor.file.standard.Vector;
import net.highwayfrogs.editor.file.vlo.GameImage;
import net.highwayfrogs.editor.gui.GUIMain;
import net.highwayfrogs.editor.gui.SelectionMenu.AttachmentListCell;
import net.highwayfrogs.editor.gui.mesh.MeshData;
import net.highwayfrogs.editor.utils.Utils;

import java.util.List;

/**
 * Sets up the map editor.
 * Created by Kneesnap on 11/22/2018.
 */
@Getter
public class MAPController extends EditorController<MAPFile> {
    @FXML private ListView<Short> remapList;
    @FXML private ImageView previewImage;
    @FXML private ImageView nameImage;
    @FXML private ImageView remapImage;
    @FXML private Button changeTextureButton;
    private Scene mapScene;
    private MapMesh mapMesh;
    private MAPPolygon selectedPolygon;
    private MAPPolygon polygonImmuneToTarget;
    private boolean polygonSelected;
    private boolean showGroupBounds;
    private Vector showPosition;

    private RenderManager renderManager = new RenderManager();
    private CameraFPS cameraFPS;
    private MapUIController mapUIController;

    private Group root3D;
    private MeshData cursorData;

    private static final String GENERIC_POS_LIST = "genericPositionList";

    private static final double GENERIC_POS_SIZE = 3;
    private static final PhongMaterial GENERIC_POS_MATERIAL = Utils.makeSpecialMaterial(Color.YELLOW);

    @Override
    public void loadFile(MAPFile mapFile) {
        super.loadFile(mapFile);

        List<Short> remapTable = mapFile.getConfig().getRemapTable(mapFile.getFileEntry());
        if (remapTable == null) {
            changeTextureButton.setDisable(true);
            remapList.setDisable(true);
            return; // Empty.
        }

        // Display Level Name & Image.
        previewImage.setImage(null);
        nameImage.setImage(null);

        MAPLevel level = MAPLevel.getByName(mapFile.getFileEntry().getDisplayName());
        if (level != null && !mapFile.getConfig().getLevelInfoMap().isEmpty()) {
            LevelInfo info = mapFile.getConfig().getLevelInfoMap().get(level);
            if (info != null) {
                previewImage.setImage(mapFile.getConfig().getImageFromPointer(info.getLevelTexturePointer()).toFXImage());
                nameImage.setImage(mapFile.getConfig().getImageFromPointer(info.getLevelNameTexturePointer()).toFXImage());
            }
        }

        // Setup Remap Editor.
        this.remapList.setItems(FXCollections.observableArrayList(remapTable));
        this.remapList.setCellFactory(param -> new AttachmentListCell<>(num -> "#" + num, num -> {
            GameImage temp = getFile().getVlo() != null ? getFile().getVlo().getImageByTextureId(num, false) : null;
            if (temp == null)
                temp = getFile().getMWD().getImageByTextureId(num);

            return temp != null ? temp.toFXImage(MWDFile.VLO_ICON_SETTING) : null;
        }));

        this.remapList.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue == null)
                return;

            GameImage temp = getFile().getVlo() != null ? getFile().getVlo().getImageByTextureId(newValue, false) : null;
            if (temp == null)
                temp = getFile().getMWD().getImageByTextureId(newValue);
            if (temp != null)
                this.remapImage.setImage(temp.toFXImage(MWDFile.VLO_ICON_SETTING));
        });
        this.remapList.getSelectionModel().selectFirst();
    }

    @FXML
    private void onChangeTexture(ActionEvent event) {
        if (getFile().getVlo() == null) {
            System.out.println("Cannot edit remaps for a map which has no associated VLO!");
            return;
        }

        getFile().getVlo().promptImageSelection(newImage -> {
            int index = this.remapList.getSelectionModel().getSelectedIndex();
            getFile().getConfig().getRemapTable(getFile().getFileEntry()).set(index, newImage.getTextureId());
            this.remapList.setItems(FXCollections.observableArrayList(getFile().getConfig().getRemapTable(getFile().getFileEntry()))); // Refresh remap.
            this.remapList.getSelectionModel().select(index);
        }, false);
    }

    @FXML
    private void onMapButtonClicked(ActionEvent event) {
        TextureMap textureMap = TextureMap.newTextureMap(getFile());
        setupMapViewer(GUIMain.MAIN_STAGE, new MapMesh(getFile(), textureMap), textureMap);
    }

    @FXML
    private void onFixIslandClicked(ActionEvent event) {
        getFile().fixAsIslandMap();
    }

    @FXML
    private void makeNewMap(ActionEvent event) {
        getFile().randomizeMap();
    }

    @SneakyThrows
    private void setupMapViewer(Stage stageToOverride, MapMesh mesh, TextureMap texMap) {
        this.mapMesh = mesh;

        // These cause errors if not reset.
        this.cursorData = null;

        // Setup the primary camera
        this.cameraFPS = new CameraFPS();

        if (this.showPosition != null)
            this.showPosition = null;
        if (getRenderManager().getDisplayListCache().containsKey(GENERIC_POS_LIST))
            getRenderManager().clearDisplayList(GENERIC_POS_LIST);

        // Create and setup material properties for rendering the level, entity icons and bounding boxes.
        PhongMaterial material = new PhongMaterial();
        material.setDiffuseMap(Utils.toFXImage(texMap.getImage(), true));

        // Create mesh view and initialise with xyz rotation transforms, materials and initial face culling policy.
        MeshView meshView = new MeshView(mesh);
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
        updateGroupView();

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
                if (isPolygonSelected()) { // If there's a polygon selected, deselect it.
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
                    if (getMapUIController() != null && getMapUIController().getCheckBoxFaceRemoveMode().isSelected()) {
                        getFile().removeFace(getSelectedPolygon());
                        removeCursorPolygon();
                        refreshView();
                        getMapUIController().getAnimationManager().setupEditor();
                    } else {
                        setCursorPolygon(clickedPoly);
                        this.polygonSelected = true;
                    }
                }
            }
        });

        // Set the initial camera position based on start position and in-game camera offset.
        SVector startPos = getFile().getCameraSourceOffset();
        float gridX = Utils.fixedPointIntToFloat4Bit(getFile().getWorldX(getFile().getStartXTile(), true));
        float baseY = -Utils.fixedPointIntToFloat4Bit(getFile().getGridStack(getFile().getStartXTile(), getFile().getStartZTile()).getHeight());
        float gridZ = Utils.fixedPointIntToFloat4Bit(getFile().getWorldZ(getFile().getStartZTile(), true));
        cameraFPS.setPos(gridX + startPos.getFloatX(), baseY + startPos.getFloatY(), gridZ + startPos.getFloatZ());
        cameraFPS.setCameraLookAt(gridX, baseY, gridZ); // Set the camera to look at the start position, too.
    }

    /**
     * Updates the marker to display at the given position.
     * If null is supplied, it'll get removed.
     */
    public void updateMarker(Vector vec, int bits, Vector origin) {
        getRenderManager().addMissingDisplayList(GENERIC_POS_LIST);
        getRenderManager().clearDisplayList(GENERIC_POS_LIST);
        this.showPosition = vec;
        if (vec != null) {
            float baseX = vec.getFloatX(bits);
            float baseY = vec.getFloatY(bits);
            float baseZ = vec.getFloatZ(bits);
            if (origin != null) {
                baseX += origin.getFloatX();
                baseY += origin.getFloatY();
                baseZ += origin.getFloatZ();
            }

            getRenderManager().addBoundingBoxFromMinMax(GENERIC_POS_LIST, baseX - GENERIC_POS_SIZE, baseY - GENERIC_POS_SIZE, baseZ - GENERIC_POS_SIZE, baseX + GENERIC_POS_SIZE, baseY + GENERIC_POS_SIZE, baseZ + GENERIC_POS_SIZE, GENERIC_POS_MATERIAL, true);
        }
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

        boolean showRemoveColor = !isPolygonSelected() && getMapUIController().getCheckBoxFaceRemoveMode().isSelected();
        renderOverPolygon(cursorPoly, showRemoveColor ? MapMesh.REMOVE_FACE_COLOR : MapMesh.CURSOR_COLOR);
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
     * Sets whether or not to show group bounds.
     * @param newState The new show state.
     */
    public void setShowGroupBounds(boolean newState) {
        this.showGroupBounds = newState;
        updateGroupView();
    }

    /**
     * Update the group display.
     */
    public void updateGroupView() {
        getRenderManager().addMissingDisplayList("groupOutline");
        getRenderManager().clearDisplayList("groupOutline");

        if (!isShowGroupBounds())
            return;

        SVector basePoint = getFile().makeBasePoint();
        float maxX = Utils.fixedPointShortToFloat4Bit((short) (basePoint.getX() + (getFile().getGroupXSize()) * getFile().getGroupXCount()));
        float maxZ = Utils.fixedPointShortToFloat4Bit((short) (basePoint.getZ() + (getFile().getGroupZSize()) * getFile().getGroupZCount()));
        getRenderManager().addBoundingBoxFromMinMax("groupOutline", basePoint.getFloatX(), 0, basePoint.getFloatZ(), maxX, 0, maxZ, Utils.makeSpecialMaterial(Color.YELLOW), true);
    }
}
