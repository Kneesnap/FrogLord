package net.highwayfrogs.editor.games.konami.greatquest.ui.mesh.map;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import net.highwayfrogs.editor.games.konami.greatquest.GreatQuestChunkedFile;
import net.highwayfrogs.editor.games.konami.greatquest.GreatQuestInstance;
import net.highwayfrogs.editor.games.konami.greatquest.ui.GreatQuestFileEditorUIController;
import net.highwayfrogs.editor.gui.editor.MeshViewController;
import net.highwayfrogs.editor.system.NameValuePair;
import net.highwayfrogs.editor.system.Tuple2;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Shows information about the map.
 * Created by Kneesnap on 4/14/2024.
 */
public class GreatQuestMapInfoController extends GreatQuestFileEditorUIController<GreatQuestChunkedFile> {
    @FXML private TableView<NameValuePair> propertyTable;
    @FXML private TableColumn<Object, Object> tableColumnFileDataName;
    @FXML private TableColumn<Object, Object> tableColumnFileDataValue;

    public GreatQuestMapInfoController(GreatQuestInstance instance) {
        super(instance);
    }

    @Override
    public void setTargetFile(GreatQuestChunkedFile map) {
        super.setTargetFile(map);

        this.propertyTable.getItems().clear();
        this.tableColumnFileDataName.setCellValueFactory(new PropertyValueFactory<>("name"));
        this.tableColumnFileDataValue.setCellValueFactory(new PropertyValueFactory<>("value"));
        List<Tuple2<String, Object>> properties = Collections.emptyList(); // TODO: map.createPropertyList();
        if (properties != null && properties.size() > 0)
            for (Tuple2<String, Object> pair : properties)
                this.propertyTable.getItems().add(new NameValuePair(pair.getA(), Objects.toString(pair.getB())));
    }

    @FXML
    private void onView(ActionEvent evt) {
        MeshViewController.setupMeshViewer(getGameInstance(), new GreatQuestMapMeshController(), new GreatQuestMapMesh(getFile()));
    }
}