package net.highwayfrogs.editor.gui.editor;

import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import lombok.SneakyThrows;
import net.highwayfrogs.editor.file.PSXTIMFile;
import net.highwayfrogs.editor.file.WADFile;
import net.highwayfrogs.editor.games.sony.SCGameConfig;
import net.highwayfrogs.editor.games.sony.SCGameInstance;
import net.highwayfrogs.editor.gui.MainController;
import net.highwayfrogs.editor.system.AbstractIndexStringConverter;
import net.highwayfrogs.editor.system.NameValuePair;
import net.highwayfrogs.editor.system.Tuple2;
import net.highwayfrogs.editor.utils.Utils;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * Controls the screen for viewing TIM files.
 * Created by Kneesnap on 9/10/2023.
 */
public class TIMController extends EditorController<PSXTIMFile, SCGameInstance, SCGameConfig> {
    @FXML private TableView<NameValuePair> tableFileData;
    @FXML private TableColumn<Object, Object> tableColumnFileDataName;
    @FXML private TableColumn<Object, Object> tableColumnFileDataValue;
    @FXML private CheckBox transparencyCheckBox;
    @FXML private ImageView imageView;
    @FXML private Button backButton;
    @FXML private ChoiceBox<Integer> paletteChoiceBox;

    private WADFile parentWad;
    private double defaultEditorMaxHeight;

    private static final int MAX_WIDTH = 384;

    public TIMController(SCGameInstance instance) {
        super(instance);
    }

    @Override
    public void loadFile(PSXTIMFile tim) {
        super.loadFile(tim);

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
        List<Tuple2<String, String>> properties = getFile().createPropertyList();
        if (properties != null && properties.size() > 0)
            for (Tuple2<String, String> pair : properties)
                this.tableFileData.getItems().add(new NameValuePair(pair.getA(), pair.getB()));
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


    @Override
    public void onInit(AnchorPane editorRoot) {
        super.onInit(editorRoot);
        this.defaultEditorMaxHeight = editorRoot.getMaxHeight();
    }

    @Override
    public void onClose(AnchorPane editorRoot) {
        super.onClose(editorRoot);
        editorRoot.setMaxHeight(this.defaultEditorMaxHeight);
    }

    @FXML
    private void onUIUpdateImage(ActionEvent event) {
        updateImage();
    }

    @FXML
    @SneakyThrows
    private void exportFile(ActionEvent event) {
        File selectedFile = Utils.promptFileSave("Specify the file to export this image as...", null, "Image Files", "png");
        if (selectedFile != null)
            ImageIO.write(getImage(), "png", selectedFile);
    }

    @FXML
    @SneakyThrows
    private void importFile(ActionEvent event) {
        Utils.makePopUp("Importing TIM images is not supported at this time.", AlertType.ERROR);
        File selectedFile = Utils.promptFileOpenExtensions("Select the image to import...", "Image Files", "png", "bmp");
        if (selectedFile == null)
            return; // Cancelled.

        BufferedImage image;
        try {
            image = ImageIO.read(selectedFile);
        } catch (IOException ex) {
            ex.printStackTrace();
            Utils.makePopUp("Failed to read image file " + selectedFile, AlertType.ERROR);
            return;
        }

        try {
            getFile().fromBufferedImage(image, this.transparencyCheckBox.isSelected());
        } catch (Throwable th) {
            th.printStackTrace();
            Utils.makePopUp("An error occurred while importing the image.\n" + th.getMessage(), AlertType.ERROR);
            return;
        }

        // After image importing, update UI.
        updateProperties();
        updatePalette();
        updateImage();
    }

    @FXML
    private void returnToWad(ActionEvent event) {
        MainController.MAIN_WINDOW.openEditor(MainController.MAIN_WINDOW.getCurrentFilesList(), this.parentWad);
        ((WADController) MainController.getCurrentController()).selectFile(getFile()); // Highlight this file again.
    }

    private BufferedImage getImage() {
        if (this.transparencyCheckBox.isDisabled())
            return getFile().toBufferedImage(this.transparencyCheckBox.isSelected());

        Integer palette = this.paletteChoiceBox.getValue();
        int paletteId = palette != null ? palette : 0;
        return getFile().toBufferedImage(this.transparencyCheckBox.isSelected(), paletteId);
    }

    /**
     * Allows using the back button feature and returning to a holder wad.
     * @param wadFile the wad to return to if back is pressed.
     */
    public void setParentWad(WADFile wadFile) {
        this.parentWad = wadFile;
        this.backButton.setVisible(true);
    }
}