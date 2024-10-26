package net.highwayfrogs.editor.games.sony.frogger.ui;

import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import lombok.Getter;
import lombok.SneakyThrows;
import net.highwayfrogs.editor.file.config.data.MAPLevel;
import net.highwayfrogs.editor.file.config.exe.LevelInfo;
import net.highwayfrogs.editor.file.vlo.GameImage;
import net.highwayfrogs.editor.file.vlo.VLOArchive;
import net.highwayfrogs.editor.games.sony.frogger.FroggerGameInstance;
import net.highwayfrogs.editor.games.sony.frogger.map.FroggerMapFile;
import net.highwayfrogs.editor.games.sony.frogger.map.mesh.FroggerMapMesh;
import net.highwayfrogs.editor.games.sony.frogger.map.mesh.FroggerMapMeshController;
import net.highwayfrogs.editor.games.sony.shared.TextureRemapArray;
import net.highwayfrogs.editor.games.sony.shared.mwd.MWDFile;
import net.highwayfrogs.editor.games.sony.shared.ui.SCFileEditorUIController;
import net.highwayfrogs.editor.gui.GUIMain;
import net.highwayfrogs.editor.gui.InputMenu;
import net.highwayfrogs.editor.gui.SelectionMenu.AttachmentListCell;
import net.highwayfrogs.editor.gui.components.PropertyListViewerComponent;
import net.highwayfrogs.editor.gui.editor.MeshViewController;
import net.highwayfrogs.editor.utils.FXUtils;
import net.highwayfrogs.editor.utils.NumberUtils;

import javax.imageio.ImageIO;
import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

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

    public FroggerMapInfoUIController(FroggerGameInstance instance) {
        super(instance);
        this.propertyListViewer = new PropertyListViewerComponent<>(instance);
    }

    @Override
    protected void onControllerLoad(Node rootNode) {
        super.onControllerLoad(rootNode);
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
        this.propertyListViewer.showProperties(null);
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
        MAPLevel level = MAPLevel.getByName(mapFile.getFileDisplayName());
        if (level != null && !mapFile.getGameInstance().getLevelInfoMap().isEmpty()) {
            LevelInfo info = mapFile.getGameInstance().getLevelInfoMap().get(level);
            if (info != null) {
                GameImage gamePreviewImage = mapFile.getGameInstance().getImageFromPointer(info.getLevelTexturePointer());
                if (gamePreviewImage != null)
                    this.levelPreviewScreenshotView.setImage(gamePreviewImage.toFXImage());
                GameImage gameNameImage = mapFile.getGameInstance().getImageFromPointer(info.getLevelNameTexturePointer());
                if (gameNameImage != null)
                    this.levelNameImageView.setImage(gameNameImage.toFXImage());
            }
        }

        // Setup Remap Editor.
        TextureRemapArray textureRemap = mapFile.getTextureRemap();

        if (textureRemap != null) {
            List<Short> textureRemapIdList = textureRemap.getTextureIds() != null ? textureRemap.getTextureIds() : Collections.emptyList();
            this.remapListLabel.setText(textureRemap.getDebugName() + " (" + textureRemapIdList.size() + " texture" + (textureRemapIdList.size() != 1 ? "s" : "") + ")");
            this.remapList.setDisable(false);
            this.remapList.setItems(FXCollections.observableArrayList(textureRemapIdList));
            this.remapList.getSelectionModel().selectFirst();
            this.remapList.setCellFactory(param -> new AttachmentListCell<>(num -> "#" + num, num -> {
                GameImage temp = getFile().getVloFile() != null ? getFile().getVloFile().getImageByTextureId(num, false) : null;
                if (temp == null)
                    temp = getFile().getArchive().getImageByTextureId(num);

                return temp != null ? temp.toFXImage(MWDFile.VLO_ICON_SETTING) : null;
            }));

            // Handle double-click to change.
            this.remapList.setOnMouseClicked(event -> {
                if (event.getButton() == MouseButton.PRIMARY && event.getClickCount() == 2) {
                    promptChangeTexture();
                    event.consume();
                }
            });
        }

        this.saveTextureButton.setOnAction(evt -> {
            try {
                FroggerMapMesh mapMesh = new FroggerMapMesh(getFile());

                // With shading:
                mapMesh.setShadingEnabled(true);
                File file = new File(GUIMain.getWorkingDirectory(), getFile().getFileDisplayName() + "-shaded.png");
                ImageIO.write(mapMesh.getTextureAtlas().getImage(), "png", file);

                // Without shading:
                mapMesh.setShadingEnabled(false);
                file = new File(GUIMain.getWorkingDirectory(), getFile().getFileDisplayName() + "-unshaded.png");
                ImageIO.write(mapMesh.getTextureAtlas().getImage(), "png", file);
            } catch (IOException e) {
                handleError(e, true, "Failed to save all images.");
            }
        });
    }

    private void promptChangeTexture() {
        TextureRemapArray textureRemap = getFile().getTextureRemap();
        if (textureRemap == null)
            return;

        // Validate selection index.
        int selectionIndex = this.remapList.getSelectionModel().getSelectedIndex();
        if (selectionIndex < 0 || selectionIndex >= textureRemap.getTextureIds().size())
            return;

        // Ensure we've got the VLO to find textures from.
        VLOArchive vloFile = getFile().getVloFile();
        if (vloFile == null) {
            FXUtils.makePopUp("Cannot edit remaps for a map which has no associated VLO!", AlertType.WARNING);
            return;
        }

        // Ask the user which texture to apply.
        vloFile.promptImageSelection(newImage -> {
            int index = this.remapList.getSelectionModel().getSelectedIndex();
            getFile().getTextureRemap().getTextureIds().set(index, newImage.getTextureId());
            this.remapList.setItems(FXCollections.observableArrayList(getFile().getTextureRemap().getTextureIds())); // Refresh remap.
            this.remapList.getSelectionModel().select(index);
        }, false);
    }

    @FXML
    private void onMapButtonClicked(ActionEvent event) {
        MeshViewController.setupMeshViewer(getGameInstance(), new FroggerMapMeshController(getGameInstance()), new FroggerMapMesh(getFile()));
    }

    @FXML
    private void makeNewMap(ActionEvent event) {
        InputMenu.promptInput(getGameInstance(), "Please enter the grid dimensions for the cleared map.", "5,5", newText -> {
            String[] split = newText.split(",");
            if (split.length != 2) {
                FXUtils.makePopUp("'" + newText + "' was invalid.\nPlease enter two numbers separated by a comma.", AlertType.ERROR);
                return;
            }

            if (!NumberUtils.isInteger(split[0])) {
                FXUtils.makePopUp("'" + split[0] + "' is not a valid number.", AlertType.ERROR);
                return;
            }

            if (!NumberUtils.isInteger(split[1])) {
                FXUtils.makePopUp("'" + split[1] + "' is not a valid number.", AlertType.ERROR);
                return;
            }

            int x = Integer.parseInt(split[0]);
            int z = Integer.parseInt(split[1]);
            if (x < 0 || z < 0) {
                FXUtils.makePopUp("Dimensions cannot be less than zero.", AlertType.ERROR);
                return;
            }

            if (x > 255 || z > 255) { // Frogger limitation.
                FXUtils.makePopUp("The collision grid cannot go larger than 255x255.", AlertType.ERROR);
                return;
            }

            getFile().randomizeMap(x, z);
        });
    }

    @FXML
    @SneakyThrows
    private void exportToObj(ActionEvent event) {
        // TODO: IMPLEMENT
        FXUtils.makePopUp("Exporting to obj is not currently supported.", AlertType.ERROR);
        // TODO: FileUtils3D.exportMapToObj(getFile(), Utils.promptChooseDirectory(getGameInstance(), "Choose the directory to save the map to.", false));
    }
}