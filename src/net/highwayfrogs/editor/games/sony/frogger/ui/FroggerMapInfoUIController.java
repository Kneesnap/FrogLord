package net.highwayfrogs.editor.games.sony.frogger.ui;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import lombok.Getter;
import lombok.SneakyThrows;
import net.highwayfrogs.editor.file.config.exe.LevelInfo;
import net.highwayfrogs.editor.file.vlo.GameImage;
import net.highwayfrogs.editor.games.sony.frogger.FroggerGameInstance;
import net.highwayfrogs.editor.games.sony.frogger.map.FroggerMapFile;
import net.highwayfrogs.editor.games.sony.frogger.map.FroggerMapLevelID;
import net.highwayfrogs.editor.games.sony.frogger.map.mesh.FroggerMapMesh;
import net.highwayfrogs.editor.games.sony.frogger.map.mesh.FroggerMapMeshController;
import net.highwayfrogs.editor.games.sony.frogger.map.ui.editor.baked.FroggerGridResizeController;
import net.highwayfrogs.editor.games.sony.frogger.utils.FFSUtil;
import net.highwayfrogs.editor.games.sony.frogger.utils.FroggerUtils;
import net.highwayfrogs.editor.games.sony.shared.ui.SCFileEditorUIController;
import net.highwayfrogs.editor.games.sony.shared.ui.SCRemapEditor;
import net.highwayfrogs.editor.gui.InputMenu;
import net.highwayfrogs.editor.gui.components.propertylist.PropertyListViewerComponent;
import net.highwayfrogs.editor.gui.editor.MeshViewController;
import net.highwayfrogs.editor.utils.FXUtils;
import net.highwayfrogs.editor.utils.FileUtils;
import net.highwayfrogs.editor.utils.FileUtils.BrowserFileType;
import net.highwayfrogs.editor.utils.FileUtils.SavedFilePath;
import net.highwayfrogs.editor.utils.NumberUtils;
import net.highwayfrogs.editor.utils.Utils.ProblemResponse;

import javax.imageio.ImageIO;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;

/**
 * Sets up the map editor.
 * Created by Kneesnap on 11/22/2018.
 */
@Getter
public class FroggerMapInfoUIController extends SCFileEditorUIController<FroggerGameInstance, FroggerMapFile> {
    private final PropertyListViewerComponent<FroggerGameInstance> propertyListViewer;
    @FXML private HBox contentBox;
    @FXML private Label remapListLabel;
    @FXML private ListView<Short> remapList;
    @FXML private ImageView levelPreviewScreenshotView;
    @FXML private ImageView levelNameImageView;
    @FXML private Button saveTextureButton;
    @FXML private Button loadFromFFS;
    @FXML private Button saveToFFS;
    @FXML private Button saveToObj;
    private SCRemapEditor<FroggerMapFile> remapEditor;

    private static final BrowserFileType FFS_FILE_TYPE = new BrowserFileType("Frogger File Sync", "ffs");
    private static final SavedFilePath FFS_IMPORT_PATH = new SavedFilePath("ffsImportPath", "Please select the map ffs file to import.", FFS_FILE_TYPE);
    private static final SavedFilePath FFS_EXPORT_FOLDER = new SavedFilePath("ffsExportPath", "Please select the folder to export the .ffs map into");
    private static final SavedFilePath OBJ_EXPORT_FOLDER = new SavedFilePath("mapObjExportPath", "Please select the folder to export the map .obj into");

    public FroggerMapInfoUIController(FroggerGameInstance instance) {
        super(instance);
        this.propertyListViewer = new PropertyListViewerComponent<>(instance);
    }

    @Override
    protected void onControllerLoad(Node rootNode) {
        super.onControllerLoad(rootNode);
        this.remapEditor = new SCRemapEditor<>(this.remapListLabel, this.remapList, this::getFile, FroggerMapFile::getVloFile, FroggerMapFile::getTextureRemap);
        if (this.contentBox != null) {
            Node propertyListViewRootNode = this.propertyListViewer.getRootNode();
            HBox.setHgrow(propertyListViewRootNode, Priority.ALWAYS);
            this.contentBox.getChildren().add(propertyListViewRootNode);
            addController(this.propertyListViewer);
        }
    }

    @Override
    public void setTargetFile(FroggerMapFile mapFile) {
        super.setTargetFile(mapFile);

        // Clear display.
        this.levelPreviewScreenshotView.setImage(null);
        this.levelNameImageView.setImage(null);
        this.propertyListViewer.clear();
        this.remapListLabel.setText("No Texture Remap");
        this.remapList.setDisable(true);
        if (this.remapList.getItems() != null)
            this.remapList.getItems().clear();

        // If there's no map file, abort!
        if (mapFile == null)
            return;

        // Show map file properties.
        this.propertyListViewer.showProperties(mapFile.createPropertyList());

        // Apply level name & screenshot to UI, if found.
        FroggerMapLevelID level = mapFile.getMapLevelID();
        if (level != null && !mapFile.getGameInstance().getLevelInfoMap().isEmpty()) {
            LevelInfo info = mapFile.getGameInstance().getLevelInfoMap().get(level);
            if (info != null) {
                GameImage gamePreviewImage = info.getLevelPreviewScreenshotImage();
                if (gamePreviewImage != null)
                    this.levelPreviewScreenshotView.setImage(gamePreviewImage.toFXImage());
                GameImage gameNameImage = info.getLevelNameImage();
                if (gameNameImage != null)
                    this.levelNameImageView.setImage(gameNameImage.toFXImage());
            }
        }

        // Setup Remap Editor.
        if (this.remapEditor != null)
            this.remapEditor.setupEditor(getFile());

        this.saveTextureButton.setOnAction(evt -> {
            try {
                FroggerMapMesh mapMesh = new FroggerMapMesh(getFile());

                // With shading:
                mapMesh.setShadingEnabled(true);
                File file = new File(getGameInstance().getMainGameFolder(), getFile().getFileDisplayName() + "-shaded.png");
                ImageIO.write(mapMesh.getTextureAtlas().getImage(), "png", file);

                // Without shading:
                mapMesh.setShadingEnabled(false);
                file = new File(getGameInstance().getMainGameFolder(), getFile().getFileDisplayName() + "-unshaded.png");
                ImageIO.write(mapMesh.getTextureAtlas().getImage(), "png", file);
            } catch (IOException e) {
                handleError(e, true, "Failed to save all images.");
            }
        });
    }

    @FXML
    private void onMapButtonClicked(ActionEvent event) {
        MeshViewController.setupMeshViewer(getGameInstance(), new FroggerMapMeshController(getGameInstance()), new FroggerMapMesh(getFile()));
    }

    @FXML
    private void makeNewMap(ActionEvent event) {
        InputMenu.promptInput(getGameInstance(), "Please enter the grid dimensions for the new map.", "5,5", newText -> {
            String[] split = newText.split(",");
            if (split.length != 2) {
                FXUtils.showPopup(AlertType.ERROR, "Invalid grid dimensions.", "'" + newText + "' was invalid.\nPlease enter two numbers separated by a comma.");
                return;
            }

            if (!NumberUtils.isInteger(split[0])) {
                FXUtils.showPopup(AlertType.ERROR, "Invalid X grid size.", "'" + split[0] + "' is not a valid number.");
                return;
            }

            if (!NumberUtils.isInteger(split[1])) {
                FXUtils.showPopup(AlertType.ERROR, "Invalid Z grid size.", "'" + split[1] + "' is not a valid number.");
                return;
            }

            int x = Integer.parseInt(split[0]);
            int z = Integer.parseInt(split[1]);
            if (FroggerGridResizeController.isGridSizeValid(x, z))
                getFile().randomizeMap(x, z);
        });
    }

    @FXML
    @SneakyThrows
    private void exportToObj(ActionEvent event) {
        File outputFolder = FileUtils.askUserToSelectFolder(getGameInstance(), OBJ_EXPORT_FOLDER);
        if (outputFolder != null)
            FroggerUtils.exportMapToObj(getFile(), outputFolder);
    }

    @FXML
    @SneakyThrows
    private void loadFromFFS(ActionEvent event) {
        File importFile = FileUtils.askUserToOpenFile(getGameInstance(), FFS_IMPORT_PATH);
        if (importFile != null)
            FFSUtil.importFFSToMap(getFile().getLogger(), getFile(), importFile);
    }

    @FXML
    @SneakyThrows
    private void exportToFFS(ActionEvent event) {
        File outputFolder = FileUtils.askUserToSelectFolder(getGameInstance(), FFS_EXPORT_FOLDER);
        if (outputFolder == null)
            return;

        FFSUtil.saveMapAsFFS(getFile(), outputFolder, ProblemResponse.CREATE_POPUP);

        InputStream blenderScriptStream = getGameInstance().getGameType().getEmbeddedResourceStream(FFSUtil.BLENDER_ADDON_FILE_NAME);
        if (blenderScriptStream != null)
            Files.write(new File(outputFolder, FFSUtil.BLENDER_ADDON_FILE_NAME).toPath(), FileUtils.readBytesFromStream(blenderScriptStream));
    }
}