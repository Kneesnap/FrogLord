package net.highwayfrogs.editor.games.sony.beastwars.ui;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.ImageView;
import lombok.SneakyThrows;
import net.highwayfrogs.editor.games.sony.SCGameInstance;
import net.highwayfrogs.editor.games.sony.beastwars.BeastWarsBPPImageFile;
import net.highwayfrogs.editor.games.sony.shared.mwd.WADFile;
import net.highwayfrogs.editor.games.sony.shared.ui.SCFileEditorUIController;
import net.highwayfrogs.editor.gui.components.PropertyListViewerComponent.PropertyList;
import net.highwayfrogs.editor.system.NameValuePair;
import net.highwayfrogs.editor.utils.FXUtils;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

/**
 * Represents the UI for a .BPP image file.
 * Created by Kneesnap on 3/3/2025.
 */
public class BeastWarsBppFileUIController extends SCFileEditorUIController<SCGameInstance, BeastWarsBPPImageFile> {
    @FXML private TableView<NameValuePair> tableFileData;
    @FXML private TableColumn<Object, Object> tableColumnFileDataName;
    @FXML private TableColumn<Object, Object> tableColumnFileDataValue;
    @FXML private ImageView imageView;
    @FXML private Button backButton;

    private static final int MAX_WIDTH = 384;

    public BeastWarsBppFileUIController(SCGameInstance instance) {
        super(instance);
    }

    @Override
    public void setParentWadFile(WADFile wadFile) {
        super.setParentWadFile(wadFile);
        this.backButton.setVisible(wadFile != null);
    }

    @Override
    public void setTargetFile(BeastWarsBPPImageFile imageFile) {
        super.setTargetFile(imageFile);

        // Setup property list.
        updateProperties();

        // Show image.
        updateImage();
    }

    private void updateProperties() {
        // Update display properties.
        this.tableFileData.getItems().clear();
        this.tableColumnFileDataName.setCellValueFactory(new PropertyValueFactory<>("name"));
        this.tableColumnFileDataValue.setCellValueFactory(new PropertyValueFactory<>("value"));
        PropertyList properties = getFile().createPropertyList();
        if (properties != null)
            properties.apply(this.tableFileData);
    }

    private void updateImage() {
        BeastWarsBPPImageFile bpp = getFile();
        BufferedImage image = bpp != null ? bpp.getImage() : null;
        this.imageView.setVisible(image != null);
        if (image == null)
            return;

        int chosenWidth = Math.min(bpp.getImage().getWidth(), MAX_WIDTH);
        double aspectRatioInverse = (double) bpp.getImage().getHeight() / bpp.getImage().getWidth();
        this.imageView.setFitWidth(chosenWidth);
        this.imageView.setFitHeight((int) Math.round(aspectRatioInverse * chosenWidth));
        this.imageView.setImage(FXUtils.toFXImage(image, false));
    }

    @FXML
    private void onUIUpdateImage(ActionEvent event) {
        updateImage();
    }

    @FXML
    @SneakyThrows
    private void exportFile(ActionEvent event) {
        BeastWarsBPPImageFile bppImageFile = getFile();
        if (bppImageFile == null)
            return;

        File selectedFile = FXUtils.promptFileSave(getGameInstance(), "Specify the file to export this image as...", null, "Image Files", "png");
        if (selectedFile != null)
            ImageIO.write(bppImageFile.getImage(), "png", selectedFile);
    }

    @FXML
    @SneakyThrows
    private void importFile(ActionEvent event) {
        FXUtils.makePopUp("Importing TIM images is not supported at this time.", AlertType.ERROR);
        File selectedFile = FXUtils.promptFileOpenExtensions(getGameInstance(), "Select the image to import...", "Image Files", "png", "bmp");
        if (selectedFile == null)
            return; // Cancelled.

        BufferedImage image;
        try {
            image = ImageIO.read(selectedFile);
        } catch (IOException ex) {
            handleError(ex, true, "Failed to read image file %s", selectedFile);
            return;
        }

        try {
            getFile().setImage(image);
        } catch (Throwable th) {
            handleError(th, true, "An error occurred while importing the image.");
            return;
        }

        // After image importing, update UI.
        updateProperties();
        updateImage();
    }

    @FXML
    private void returnToWad(ActionEvent event) {
        tryReturnToParentWadFile();
    }
}