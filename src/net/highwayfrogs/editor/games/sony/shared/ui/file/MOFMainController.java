package net.highwayfrogs.editor.games.sony.shared.ui.file;

import javafx.application.ConditionalFeature;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import net.highwayfrogs.editor.file.mof.MOFHolder;
import net.highwayfrogs.editor.games.sony.SCGameInstance;
import net.highwayfrogs.editor.games.sony.shared.ui.SCFileEditorUIController;
import net.highwayfrogs.editor.gui.components.PropertyListViewerComponent.PropertyList;
import net.highwayfrogs.editor.system.NameValuePair;
import net.highwayfrogs.editor.utils.Utils;

/**
 * Manages the mof display on the main menu.
 * Created by Kneesnap on 5/23/2020.
 */
public class MOFMainController extends SCFileEditorUIController<SCGameInstance, MOFHolder> {
    @FXML private TableView<NameValuePair> mofPropertyTable;
    @FXML private TableColumn<Object, Object> tableColumnFileDataName;
    @FXML private TableColumn<Object, Object> tableColumnFileDataValue;
    @FXML private Label vloNameLabel;

    public MOFMainController(SCGameInstance instance) {
        super(instance);
    }

    @Override
    public void setTargetFile(MOFHolder mof) {
        super.setTargetFile(mof);

        this.mofPropertyTable.getItems().clear();
        this.tableColumnFileDataName.setCellValueFactory(new PropertyValueFactory<>("name"));
        this.tableColumnFileDataValue.setCellValueFactory(new PropertyValueFactory<>("value"));
        PropertyList properties = mof.createPropertyList();
        if (properties != null)
            properties.apply(this.mofPropertyTable);

        updateVLO();
    }

    @FXML
    private void onView(ActionEvent evt) {
        if (!Platform.isSupported(ConditionalFeature.SCENE3D)) {
            Utils.makePopUp("Your version of JavaFX does not support 3D, so models cannot be previewed.", AlertType.WARNING);
            return;
        }

        if (getFile().getVloFile() != null) {
            getFile().showEditor3D();
            return;
        }

        getFile().getArchive().promptVLOSelection(getFile().getTheme(), vlo -> {
            getFile().setVloFile(vlo);
            updateVLO();
            getFile().showEditor3D();
        }, false);
    }

    @FXML
    private void onChangeVLO(ActionEvent evt) {
        getFile().getArchive().promptVLOSelection(getFile().getTheme(), vlo -> {
            getFile().setVloFile(vlo);
            updateVLO();
        }, false);
    }

    private void updateVLO() {
        this.vloNameLabel.setText(getFile().getVloFile() != null ? getFile().getVloFile().getFileDisplayName() : "None");
    }
}