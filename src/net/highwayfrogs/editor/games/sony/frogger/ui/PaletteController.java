package net.highwayfrogs.editor.games.sony.frogger.ui;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.ListView;
import javafx.scene.image.ImageView;
import javafx.scene.paint.Color;
import net.highwayfrogs.editor.games.sony.SCGameInstance;
import net.highwayfrogs.editor.games.sony.frogger.file.FroggerPaletteFile;
import net.highwayfrogs.editor.games.sony.shared.ui.SCFileEditorUIController;
import net.highwayfrogs.editor.system.AbstractAttachmentCell;
import net.highwayfrogs.editor.utils.Utils;

/**
 * Allows changing palette files.
 * Created by Kneesnap on 3/4/2019.
 */
public class PaletteController extends SCFileEditorUIController<SCGameInstance, FroggerPaletteFile> {
    @FXML private ColorPicker colorPicker;
    @FXML private ListView<Color> colorList;
    @FXML private ImageView paletteImageView;
    private int colorIndex = -1;

    private static final int IMAGE_SIZE = 256;
    private static final int COLOR_SIZE = 16;

    public PaletteController(SCGameInstance instance) {
        super(instance);
    }

    @Override
    public void setTargetFile(FroggerPaletteFile file) {
        super.setTargetFile(file);

        // If you click on a color in the palette image, select that color.
        this.paletteImageView.setOnMouseClicked(evt -> {
            int x = (int) (evt.getX() / COLOR_SIZE);
            int y = (int) (evt.getY() / COLOR_SIZE);

            int selectIndex = (y * COLOR_SIZE) + x;
            this.colorList.getSelectionModel().select(selectIndex);
            this.colorList.scrollTo(selectIndex);
        });

        updateColorEntries();
        colorList.getSelectionModel().selectedIndexProperty().addListener(((observable, oldValue, newValue) -> {
            if (newValue == null)
                return;

            int newIndex = newValue.intValue();
            if (newIndex == -1)
                return;

            this.colorIndex = -1;
            this.colorPicker.setValue(getFile().getColors().get(newIndex));
            this.colorIndex = newIndex;
        }));
        colorList.getSelectionModel().select(0);

        this.colorPicker.valueProperty().addListener(((observable, oldValue, newValue) -> {
            if (this.colorIndex != -1) {
                getFile().getColors().set(this.colorIndex, newValue);
                updateColorEntries();
            }
        }));
    }

    private void updateColorEntries() {
        paletteImageView.setImage(getFile().makeImage(IMAGE_SIZE));
        colorList.setCellFactory(null);
        colorList.setItems(FXCollections.observableArrayList(getFile().getColors()));
        colorList.setCellFactory(param -> new AbstractAttachmentCell<>((color, index) -> "Color #" + index, (color, index) -> color != null ? new ImageView(Utils.makeColorImage(color)) : null));
    }
}