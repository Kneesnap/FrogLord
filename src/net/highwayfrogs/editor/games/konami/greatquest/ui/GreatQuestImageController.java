package net.highwayfrogs.editor.games.konami.greatquest.ui;

import javafx.event.ActionEvent;
import javafx.scene.Node;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import lombok.SneakyThrows;
import net.highwayfrogs.editor.games.konami.greatquest.GreatQuestInstance;
import net.highwayfrogs.editor.games.konami.greatquest.file.GreatQuestImageFile;
import net.highwayfrogs.editor.gui.ImageResource;
import net.highwayfrogs.editor.utils.FXUtils;
import net.highwayfrogs.editor.utils.FileUtils;

import java.awt.image.BufferedImage;

/**
 * Manages image displays for Frogger The Great Quest.
 * Created by Kneesnap on 4/14/2024.
 */
public class GreatQuestImageController extends GreatQuestFileEditorUIController<GreatQuestImageFile> {
    private final ImageView imageView;
    private final Button exportImageButton;
    private final Button importImageButton;

    public GreatQuestImageController(GreatQuestInstance instance) {
        super(instance, "Image File", ImageResource.PHOTO_ALBUM_16);
        this.imageView = new ImageView();
        this.imageView.setFitWidth(256);
        this.imageView.setFitHeight(256);
        this.imageView.setPickOnBounds(true);
        this.imageView.setPreserveRatio(true);
        AnchorPane.setRightAnchor(this.imageView, 2.0);
        AnchorPane.setTopAnchor(this.imageView, 35.0);
        this.exportImageButton = new Button("Export");
        this.exportImageButton.setOnAction(this::exportImage);
        this.importImageButton = new Button("Import");
        this.importImageButton.setOnAction(this::importImage);
    }

    @Override
    protected void onControllerLoad(Node rootNode) {
        super.onControllerLoad(rootNode);
        Region emptyRegion = new Region();
        HBox.setHgrow(emptyRegion, Priority.ALWAYS); // Ensures the buttons are aligned to the right.
        getLeftSidePanelTopBox().getChildren().addAll(emptyRegion, this.exportImageButton, this.importImageButton);
        getLeftSideAnchorPane().getChildren().add(this.imageView);
    }

    @Override
    public void setTargetFile(GreatQuestImageFile imageFile) {
        super.setTargetFile(imageFile);
        updateImage();
    }

    @SneakyThrows
    private void exportImage(ActionEvent event) {
        GreatQuestImageFile image = getFile();
        if (image != null)
            FileUtils.askUserToSaveImageFile(image.getLogger(), getGameInstance(), image.getImage(), image.getExportName(), true);
    }

    @SneakyThrows
    private void importImage(ActionEvent event) {
        /*File selectedFile = Utils.promptFileOpen(getGameInstance(), "Select the image to import...", "Image Files", "png");
        if (selectedFile == null)
            return; // Cancelled.

        getFile().replaceImage(ImageIO.read(selectedFile));
        updateImage();*/ // TODO: Implement.
        FXUtils.makePopUp("Importing images is not supported yet.", AlertType.WARNING);
    }

    /**
     * Update the displayed image.
     */
    public void updateImage() {
        BufferedImage image = getFile().getImage();
        boolean hasImage = (image != null);
        this.imageView.setVisible(hasImage);

        if (hasImage) {
            this.imageView.setFitWidth(image.getWidth());
            this.imageView.setFitHeight(image.getHeight());
            this.imageView.setImage(FXUtils.toFXImage(image, false));
        }
    }
}