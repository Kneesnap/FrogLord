package net.highwayfrogs.editor.games.sony.oldfrogger.map.ui;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import net.highwayfrogs.editor.games.sony.oldfrogger.OldFroggerGameInstance;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.OldFroggerMapFile;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.mesh.OldFroggerMapMesh;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.mesh.OldFroggerMapMeshController;
import net.highwayfrogs.editor.games.sony.shared.ui.SCFileEditorUIController;
import net.highwayfrogs.editor.games.sony.shared.ui.SCRemapEditor;
import net.highwayfrogs.editor.gui.components.PropertyListViewerComponent.PropertyList;
import net.highwayfrogs.editor.gui.editor.MeshViewController;
import net.highwayfrogs.editor.system.NameValuePair;

/**
 * Manages the mof display on the main menu.
 * Created by Kneesnap on 5/23/2020.
 */
public class OldFroggerMapController extends SCFileEditorUIController<OldFroggerGameInstance, OldFroggerMapFile> {
    @FXML private TableView<NameValuePair> propertyTable;
    @FXML private TableColumn<Object, Object> tableColumnFileDataName;
    @FXML private TableColumn<Object, Object> tableColumnFileDataValue;
    @FXML private Label vloNameLabel;
    @FXML private Label remapListLabel;
    @FXML private ListView<Short> remapList;
    private SCRemapEditor<OldFroggerMapFile> remapEditor;


    public OldFroggerMapController(OldFroggerGameInstance instance) {
        super(instance);
    }

    @Override
    public void setTargetFile(OldFroggerMapFile map) {
        super.setTargetFile(map);

        this.propertyTable.getItems().clear();
        this.tableColumnFileDataName.setCellValueFactory(new PropertyValueFactory<>("name"));
        this.tableColumnFileDataValue.setCellValueFactory(new PropertyValueFactory<>("value"));
        PropertyList properties = map.createPropertyList();
        if (properties != null)
            properties.apply(this.propertyTable);

        if (this.remapEditor == null && this.remapList != null && this.remapListLabel != null)
            this.remapEditor = new SCRemapEditor<>(this.remapListLabel, this.remapList, this::getFile, mapFile -> mapFile.getLevelTableEntry().getMainVLOArchive(), mapFile -> mapFile.getLevelTableEntry().getTextureRemap());
        if (this.remapEditor != null)
            this.remapEditor.setupEditor(map);
    }

    @FXML
    private void onView(ActionEvent evt) {
        MeshViewController.setupMeshViewer(getGameInstance(), new OldFroggerMapMeshController(), new OldFroggerMapMesh(getFile()));
    }
}