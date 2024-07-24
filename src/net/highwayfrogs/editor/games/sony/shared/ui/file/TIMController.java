package net.highwayfrogs.editor.games.sony.shared.ui.file;

import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.ImageView;
import lombok.SneakyThrows;
import net.highwayfrogs.editor.games.psx.PSXTIMFile;
import net.highwayfrogs.editor.games.sony.SCGameInstance;
import net.highwayfrogs.editor.games.sony.shared.mwd.WADFile;
import net.highwayfrogs.editor.games.sony.shared.ui.SCFileEditorUIController;
import net.highwayfrogs.editor.gui.components.PropertyListViewerComponent.PropertyList;
import net.highwayfrogs.editor.system.AbstractIndexStringConverter;
import net.highwayfrogs.editor.system.NameValuePair;
import net.highwayfrogs.editor.utils.Utils;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

/**
 * Controls the screen for viewing TIM files.
 * Created by Kneesnap on 9/10/2023.
 */
public class TIMController extends SCFileEditorUIController<SCGameInstance, PSXTIMFile> {
    @FXML private TableView<NameValuePair> tableFileData;
    @FXML private TableColumn<Object, Object> tableColumnFileDataName;
    @FXML private TableColumn<Object, Object> tableColumnFileDataValue;
    @FXML private CheckBox transparencyCheckBox;
    @FXML private ImageView imageView;
    @FXML private Button backButton;
    @FXML private ChoiceBox<Integer> paletteChoiceBox;

    private static final int MAX_WIDTH = 384;

    public TIMController(SCGameInstance instance) {
        super(instance);
    }

    @Override
    public void setParentWadFile(WADFile wadFile) {
        super.setParentWadFile(wadFile);
        this.backButton.setVisible(wadFile != null);
    }

    @Override
    public void setTargetFile(PSXTIMFile tim) {
        super.setTargetFile(tim);

        // Setup property list.
        updateProperties();

        // Setup palette.
        updatePalette();

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

    private void updatePalette() {
        // Update palettes.
        boolean hasPalette = getFile().hasClut() && getFile().getPalettes() != null && getFile().getPalettes().length > 0;
        this.paletteChoiceBox.setDisable(!hasPalette);
        if (hasPalette) {
            Integer[] paletteIds = new Integer[getFile().getPalettes().length];
            for (int i = 0; i < paletteIds.length; i++)
                paletteIds[i] = i;

            this.paletteChoiceBox.setItems(FXCollections.observableArrayList(paletteIds));
            this.paletteChoiceBox.setConverter(new AbstractIndexStringConverter<>(paletteIds, (index, entry) -> "Palette #" + entry));
            this.paletteChoiceBox.setValue(0);
        }
    }

    private void updateImage() {
        PSXTIMFile tim = getFile();
        BufferedImage image = getImage();
        this.imageView.setVisible(image != null);
        if (image == null)
            return;

        int chosenWidth = Math.min(tim.getImageWidth(), MAX_WIDTH);
        double aspectRatioInverse = (double) tim.getImageHeight() / tim.getImageWidth();
        this.imageView.setFitWidth(chosenWidth);
        this.imageView.setFitHeight((int) Math.round(aspectRatioInverse * chosenWidth));
        this.imageView.setImage(Utils.toFXImage(image, false));
    }

    @FXML
    private void onUIUpdateImage(ActionEvent event) {
        updateImage();
    }

    @FXML
    @SneakyThrows
    private void exportFile(ActionEvent event) {
        File selectedFile = Utils.promptFileSave(getGameInstance(), "Specify the file to export this image as...", null, "Image Files", "png");
        if (selectedFile != null)
            ImageIO.write(getImage(), "png", selectedFile);
    }

    @FXML
    @SneakyThrows
    private void importFile(ActionEvent event) {
        Utils.makePopUp("Importing TIM images is not supported at this time.", AlertType.ERROR);
        File selectedFile = Utils.promptFileOpenExtensions(getGameInstance(), "Select the image to import...", "Image Files", "png", "bmp");
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
            getFile().fromBufferedImage(image, this.transparencyCheckBox.isSelected());
        } catch (Throwable th) {
            handleError(th, true, "An error occurred while importing the image.");
            return;
        }

        // After image importing, update UI.
        updateProperties();
        updatePalette();
        updateImage();
    }

    @FXML
    private void returnToWad(ActionEvent event) {
        tryReturnToParentWadFile();
    }

    private BufferedImage getImage() {
        if (this.transparencyCheckBox.isDisabled())
            return getFile().toBufferedImage(this.transparencyCheckBox.isSelected());

        Integer palette = this.paletteChoiceBox.getValue();
        int paletteId = palette != null ? palette : 0;
        return getFile().toBufferedImage(this.transparencyCheckBox.isSelected(), paletteId);
    }
}