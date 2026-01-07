package net.highwayfrogs.editor.games.sony.oldfrogger.map.ui;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TreeTableColumn;
import javafx.scene.control.TreeTableView;
import net.highwayfrogs.editor.games.sony.SCGameInstance;
import net.highwayfrogs.editor.games.sony.oldfrogger.OldFroggerGameInstance;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.OldFroggerMapFile;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.mesh.OldFroggerMapMesh;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.mesh.OldFroggerMapMeshController;
import net.highwayfrogs.editor.games.sony.shared.ui.SCFileEditorUIController;
import net.highwayfrogs.editor.games.sony.shared.ui.SCRemapEditor;
import net.highwayfrogs.editor.gui.components.propertylist.PropertyListNode;
import net.highwayfrogs.editor.gui.components.propertylist.PropertyListViewerComponent;
import net.highwayfrogs.editor.gui.editor.MeshViewController;

/**
 * Manages the mof display on the main menu.
 * Created by Kneesnap on 5/23/2020.
 */
public class OldFroggerMapController extends SCFileEditorUIController<OldFroggerGameInstance, OldFroggerMapFile> {
    @FXML private TreeTableView<PropertyListNode> propertyTable;
    @FXML private TreeTableColumn<PropertyListNode, String> tableColumnFileDataName;
    @FXML private TreeTableColumn<PropertyListNode, String> tableColumnFileDataValue;
    @FXML private Label vloNameLabel;
    @FXML private Label remapListLabel;
    @FXML private ListView<Short> remapList;
    private SCRemapEditor<OldFroggerMapFile> remapEditor;
    private PropertyListViewerComponent<SCGameInstance> propertyListViewer;


    public OldFroggerMapController(OldFroggerGameInstance instance) {
        super(instance);
    }

    @Override
    protected void onControllerLoad(Node rootNode) {
        super.onControllerLoad(rootNode);
        this.propertyListViewer = new PropertyListViewerComponent<>(getGameInstance(), this.propertyTable);
        addController(this.propertyListViewer);
    }

    @Override
    public void setTargetFile(OldFroggerMapFile map) {
        super.setTargetFile(map);

        this.propertyListViewer.showProperties(map);

        if (this.remapEditor == null && this.remapList != null && this.remapListLabel != null)
            this.remapEditor = new SCRemapEditor<>(this.remapListLabel, this.remapList, this::getFile, mapFile -> mapFile.getLevelTableEntry().getMainVloFile(), mapFile -> mapFile.getLevelTableEntry().getTextureRemap());

        if (this.remapEditor != null)
            this.remapEditor.setupEditor(map.getLevelTableEntry() != null ? map : null);
    }

    @FXML
    private void onView(ActionEvent evt) {
        MeshViewController.setupMeshViewer(getGameInstance(), new OldFroggerMapMeshController(getGameInstance()), new OldFroggerMapMesh(getFile()));
    }
}