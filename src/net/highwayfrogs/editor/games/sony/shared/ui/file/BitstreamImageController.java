package net.highwayfrogs.editor.games.sony.shared.ui.file;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.ImageView;
import lombok.SneakyThrows;
import net.highwayfrogs.editor.games.psx.PSXBitstreamImage;
import net.highwayfrogs.editor.games.sony.SCGameInstance;
import net.highwayfrogs.editor.games.sony.shared.mwd.WADFile;
import net.highwayfrogs.editor.games.sony.shared.ui.SCFileEditorUIController;
import net.highwayfrogs.editor.gui.components.PropertyListViewerComponent.PropertyList;
import net.highwayfrogs.editor.system.NameValuePair;
import net.highwayfrogs.editor.utils.FXUtils;

import java.awt.image.BufferedImage;

/**
 * Controls the screen for viewing .BS (PSX MDEC) files.
 * Created by Kneesnap on 9/23/2025.
 */
public class BitstreamImageController extends SCFileEditorUIController<SCGameInstance, PSXBitstreamImage> {
    @FXML private TableView<NameValuePair> tableFileData;
    @FXML private TableColumn<Object, Object> tableColumnFileDataName;
    @FXML private TableColumn<Object, Object> tableColumnFileDataValue;
    @FXML private ImageView imageView;
    @FXML private Button backButton;

    private static final int MAX_WIDTH = 384;

    public BitstreamImageController(SCGameInstance instance) {
        super(instance);
    }

    @Override
    public void setParentWadFile(WADFile wadFile) {
        super.setParentWadFile(wadFile);
        this.backButton.setVisible(wadFile != null);
    }

    @Override
    public void setTargetFile(PSXBitstreamImage bsFile) {
        super.setTargetFile(bsFile);

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
        BufferedImage image = getImage();
        this.imageView.setVisible(image != null);
        if (image == null)
            return;

        int chosenWidth = Math.min(image.getWidth(), MAX_WIDTH);
        double aspectRatioInverse = (double) image.getHeight() / image.getWidth();
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
        PSXBitstreamImage image = getFile();
        if (image != null)
            image.promptUserToExportImage();
    }

    @FXML
    @SneakyThrows
    private void importFile(ActionEvent event) {
        PSXBitstreamImage image = getFile();
        if (image != null && image.promptUserToImportImage()) {
            // After image importing, update UI.
            updateProperties();
            updateImage();
        }
    }

    @FXML
    private void returnToWad(ActionEvent event) {
        tryReturnToParentWadFile();
    }

    private BufferedImage getImage() {
        PSXBitstreamImage bsFile = getFile();
        return bsFile != null ? bsFile.getCachedImage() : null;
    }
}
