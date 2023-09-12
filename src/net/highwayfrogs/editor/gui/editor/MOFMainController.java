package net.highwayfrogs.editor.gui.editor;

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
import net.highwayfrogs.editor.games.sony.SCGameConfig;
import net.highwayfrogs.editor.games.sony.SCGameInstance;
import net.highwayfrogs.editor.gui.MainController;
import net.highwayfrogs.editor.system.NameValuePair;
import net.highwayfrogs.editor.system.Tuple2;
import net.highwayfrogs.editor.utils.Utils;

import java.util.List;

/**
 * Manages the mof display on the main menu.
 * Created by Kneesnap on 5/23/2020.
 */
public class MOFMainController extends EditorController<MOFHolder, SCGameInstance, SCGameConfig> {
    @FXML private TableView<NameValuePair> mofPropertyTable;
    @FXML private TableColumn<Object, Object> tableColumnFileDataName;
    @FXML private TableColumn<Object, Object> tableColumnFileDataValue;
    @FXML private Label vloNameLabel;

    public MOFMainController(SCGameInstance instance) {
        super(instance);
    }

    @Override
    public void loadFile(MOFHolder mof) {
        super.loadFile(mof);

        this.mofPropertyTable.getItems().clear();
        this.tableColumnFileDataName.setCellValueFactory(new PropertyValueFactory<>("name"));
        this.tableColumnFileDataValue.setCellValueFactory(new PropertyValueFactory<>("value"));
        List<Tuple2<String, String>> properties = mof.showWadProperties(null, null);
        if (properties != null && properties.size() > 0)
            for (Tuple2<String, String> pair : properties)
                this.mofPropertyTable.getItems().add(new NameValuePair(pair.getA(), pair.getB()));

        updateVLO();
    }

    @FXML
    private void onView(ActionEvent evt) {
        if (!Platform.isSupported(ConditionalFeature.SCENE3D)) {
            Utils.makePopUp("Your version of JavaFX does not support 3D, so models cannot be previewed.", AlertType.WARNING);
            return;
        }

        if (getFile().getVloFile() != null) {
            MainController.MAIN_WINDOW.openEditor(new MOFController(getGameInstance()), getFile());
            return;
        }

        getFile().getArchive().promptVLOSelection(getFile().getTheme(), vlo -> {
            getFile().setVloFile(vlo);
            updateVLO();
            MainController.MAIN_WINDOW.openEditor(new MOFController(getGameInstance()), getFile());
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
        this.vloNameLabel.setText(getFile().getVloFile() != null ? getFile().getVloFile().getIndexEntry().getDisplayName() : "None");
    }
}