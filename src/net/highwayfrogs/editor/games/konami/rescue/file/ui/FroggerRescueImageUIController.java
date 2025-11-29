package net.highwayfrogs.editor.games.konami.rescue.file.ui;

import javafx.event.ActionEvent;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import lombok.SneakyThrows;
import net.highwayfrogs.editor.games.konami.hudson.HudsonGameInstance;
import net.highwayfrogs.editor.games.konami.rescue.file.FroggerRescueImage;
import net.highwayfrogs.editor.gui.DefaultFileUIController;
import net.highwayfrogs.editor.gui.ImageResource;
import net.highwayfrogs.editor.utils.FXUtils;
import net.highwayfrogs.editor.utils.FileUtils;

import java.awt.image.BufferedImage;

/**
 * Represents the UI for displaying/editing a FroggerRescueImage.
 * TODO: Add listener to property list viewer for selecting images, and highlighting selected image areas red.
 * TODO: Maybe a
 * Created by Kneesnap on 11/14/2025.
 */
public class FroggerRescueImageUIController extends DefaultFileUIController<HudsonGameInstance, FroggerRescueImage> {
    private final ImageView imageView;
    private final Button exportImageButton;
    private final Button importImageButton;


    public FroggerRescueImageUIController(HudsonGameInstance instance) {
        super(instance, "Image", ImageResource.PHOTO_ALBUM_16.getFxImage());
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
    public void setTargetFile(FroggerRescueImage imageFile) {
        super.setTargetFile(imageFile);
        updateImage();
    }

    @SneakyThrows
    private void exportImage(ActionEvent event) {
        FroggerRescueImage image = getFile();
        if (image != null)
            FileUtils.askUserToSaveImageFile(image.getLogger(), getGameInstance(), getImage(), image.getFullDisplayName());
    }

    @SneakyThrows
    private void importImage(ActionEvent event) {
        FroggerRescueImage image = getFile();
        if (image == null)
            return;

        BufferedImage loadedImage = FileUtils.askUserToOpenImageFile(image.getLogger(), getGameInstance());
        if (loadedImage == null)
            return; // Cancelled.

        // TODO: getFile().setImage(loadedImage);
        updateImage();
        getPropertyListViewer().showProperties(image); // Update the property list, which may have changed.
    }

    /**
     * Update the displayed image.
     */
    public void updateImage() {
        BufferedImage image = getImage();
        boolean hasImage = (image != null);
        this.imageView.setVisible(hasImage);

        if (hasImage) {
            this.imageView.setFitWidth(image.getWidth());
            this.imageView.setFitHeight(image.getHeight());
            this.imageView.setImage(FXUtils.toFXImage(image, false));
        }
    }

    private BufferedImage getImage() {
        FroggerRescueImage imageFile = getFile();
        if (imageFile == null || imageFile.getDataChunk3List().isEmpty())
            return null;

        // TODO: Allow viewing more than just the first entry.
        return imageFile.getDataChunk3List().get(0).toBufferedImage();
    }
}
