package net.highwayfrogs.editor.games.sony.frogger.ui;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import net.highwayfrogs.editor.file.DemoFile;
import net.highwayfrogs.editor.file.DemoFile.DemoAction;
import net.highwayfrogs.editor.file.DemoFile.DemoFrame;
import net.highwayfrogs.editor.games.sony.frogger.FroggerGameInstance;
import net.highwayfrogs.editor.games.sony.shared.ui.SCFileEditorUIController;
import net.highwayfrogs.editor.system.AbstractAttachmentCell;
import net.highwayfrogs.editor.utils.Utils;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Allows modifying demo files.
 * This is functional, but not great. The best option would be hooking into Frogger's process and reading input as it happened, so instead of doing things blind, you can edit stuff.
 * Created by Kneesnap on 3/3/2019.
 */
public class DemoController extends SCFileEditorUIController<FroggerGameInstance, DemoFile> {
    @FXML private ListView<DemoFrame> entryList;
    @FXML private ChoiceBox<DemoAction> basicSelector;
    @FXML private VBox actionBox;
    @FXML private TextField xField;
    @FXML private TextField zField;
    @FXML private TextField frameField;
    private final Map<DemoAction, CheckBox> checkBoxMap = new HashMap<>();
    private DemoFrame selectedFrame;

    public DemoController(FroggerGameInstance instance) {
        super(instance);
    }

    @Override
    public void setTargetFile(DemoFile file) {
        super.setTargetFile(file);

        xField.setText(String.valueOf(file.getStartX()));
        Utils.setHandleKeyPress(xField, newText -> {
            if (!Utils.isInteger(newText))
                return false;

            getFile().setStartX(Integer.parseInt(newText));
            return true;
        }, null);

        zField.setText(String.valueOf(file.getStartZ()));
        Utils.setHandleKeyPress(zField, newText -> {
            if (!Utils.isInteger(newText))
                return false;

            getFile().setStartZ(Integer.parseInt(newText));
            return true;
        }, null);

        frameField.setText(String.valueOf(file.getFrameCount()));
        Utils.setHandleKeyPress(frameField, newText -> {
            if (!Utils.isInteger(newText))
                return false;

            getFile().setFrameCount(Integer.parseInt(newText));
            return true;
        }, null);

        basicSelector.setItems(FXCollections.observableArrayList(Arrays.asList(DemoAction.getNonAdditives())));
        basicSelector.valueProperty().addListener(((observable, oldValue, newValue) -> {
            if (newValue != null) {
                selectedFrame.setBaseAction(newValue);
                updateFrameList();
            }
        }));

        for (DemoAction action : DemoAction.getAdditives()) {
            CheckBox toggleBox = new CheckBox(action.getInfo());
            toggleBox.selectedProperty().addListener(((observable, oldValue, newValue) -> {
                this.selectedFrame.setActionState(action, newValue);
                updateFrameList();
            }));

            actionBox.getChildren().add(toggleBox);
            checkBoxMap.put(action, toggleBox);
        }

        ObservableList<DemoFrame> frameEntries = FXCollections.observableArrayList(Arrays.asList(getFile().getFrames()));
        entryList.setItems(frameEntries);
        updateFrameList();

        entryList.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            this.selectedFrame = newValue;
            this.updateFrame();
        });

        entryList.getSelectionModel().select(0);
    }

    private void updateFrame() {
        basicSelector.getSelectionModel().select(this.selectedFrame.getBaseAction());

        // Update checkbox yes / no.
        for (Entry<DemoAction, CheckBox> entry : checkBoxMap.entrySet())
            entry.getValue().setSelected(this.selectedFrame.getActionState(entry.getKey()));
    }

    private void updateFrameList() {
        entryList.setCellFactory(null);
        entryList.setCellFactory(param ->
                new AbstractAttachmentCell<>((frame, index) -> frame != null ? "[" + index + "] " + frame.toString() : null));
    }
}