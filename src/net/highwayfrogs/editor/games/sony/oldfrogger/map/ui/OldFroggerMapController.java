package net.highwayfrogs.editor.games.sony.oldfrogger.map.ui;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import net.highwayfrogs.editor.games.sony.oldfrogger.OldFroggerGameInstance;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.OldFroggerMapFile;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.mesh.OldFroggerMapMesh;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.mesh.OldFroggerMapMeshController;
import net.highwayfrogs.editor.games.sony.shared.ui.SCFileEditorUIController;
import net.highwayfrogs.editor.gui.editor.MeshViewController;
import net.highwayfrogs.editor.system.NameValuePair;
import net.highwayfrogs.editor.system.Tuple2;

import java.util.List;
import java.util.Objects;

/**
 * Manages the mof display on the main menu.
 * Created by Kneesnap on 5/23/2020.
 */
public class OldFroggerMapController extends SCFileEditorUIController<OldFroggerGameInstance, OldFroggerMapFile> {
    @FXML private TableView<NameValuePair> propertyTable;
    @FXML private TableColumn<Object, Object> tableColumnFileDataName;
    @FXML private TableColumn<Object, Object> tableColumnFileDataValue;
    @FXML private Label vloNameLabel;

    public OldFroggerMapController(OldFroggerGameInstance instance) {
        super(instance);
    }

    @Override
    public void setTargetFile(OldFroggerMapFile map) {
        super.setTargetFile(map);

        this.propertyTable.getItems().clear();
        this.tableColumnFileDataName.setCellValueFactory(new PropertyValueFactory<>("name"));
        this.tableColumnFileDataValue.setCellValueFactory(new PropertyValueFactory<>("value"));
        List<Tuple2<String, Object>> properties = map.createPropertyList();
        if (properties != null && properties.size() > 0)
            for (Tuple2<String, Object> pair : properties)
                this.propertyTable.getItems().add(new NameValuePair(pair.getA(), Objects.toString(pair.getB())));
    }

    @FXML
    private void onView(ActionEvent evt) {
        MeshViewController.setupMeshViewer(getGameInstance(), new OldFroggerMapMeshController(), new OldFroggerMapMesh(getFile()));
    }
}