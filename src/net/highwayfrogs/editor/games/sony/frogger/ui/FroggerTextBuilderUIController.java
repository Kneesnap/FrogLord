package net.highwayfrogs.editor.games.sony.frogger.ui;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.file.vlo.GameImage;
import net.highwayfrogs.editor.file.vlo.ImageWorkHorse;
import net.highwayfrogs.editor.file.vlo.ImageWorkHorse.BlackFilter;
import net.highwayfrogs.editor.file.vlo.ImageWorkHorse.TransparencyFilter;
import net.highwayfrogs.editor.games.sony.frogger.FroggerGameInstance;
import net.highwayfrogs.editor.games.sony.frogger.utils.FroggerUtils;
import net.highwayfrogs.editor.gui.GameUIController;
import net.highwayfrogs.editor.utils.ColorUtils;
import net.highwayfrogs.editor.utils.FXUtils;
import net.highwayfrogs.editor.utils.FileUtils;
import net.highwayfrogs.editor.utils.NumberUtils;

import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * Manages the UI for generating Frogger text images.
 * Created by Kneesnap on 7/2/2025.
 */
public class FroggerTextBuilderUIController extends GameUIController<FroggerGameInstance> {
    @FXML private ImageView imagePreview;
    @FXML private TextField widthTextField;
    @FXML private TextField heightTextField;
    @FXML private TextField fontSizeField;
    @FXML private TextArea displayTextField;
    @FXML private ColorPicker colorPicker;
    private BufferedImage cachedImage;
    private double startHeight;
    private double startImageHeight;

    private static final int MAX_IMAGE_SIZE = GameImage.MAX_DIMENSION;

    public FroggerTextBuilderUIController(FroggerGameInstance instance) {
        super(instance);
    }

    @Override
    public VBox getRootNode() {
        return (VBox) super.getRootNode();
    }

    @Override
    protected void onControllerLoad(Node rootNode) {
        this.colorPicker.setValue(ColorUtils.fromARGB(FroggerUtils.DEFAULT_TEXT_COLOR_RGB));
        this.widthTextField.textProperty().addListener((observable, oldValue, newValue) -> updateImage());
        this.heightTextField.textProperty().addListener((observable, oldValue, newValue) -> updateImage());
        this.fontSizeField.textProperty().addListener((observable, oldValue, newValue) -> updateImage());
        this.displayTextField.textProperty().addListener((observable, oldValue, newValue) -> updateImage());
        this.colorPicker.valueProperty().addListener((observable, oldValue, newValue) -> updateImage());
        updateImage();

        // Ensure height updates.
        Platform.runLater(() -> {
            this.startHeight = getStage().getHeight();
            this.startImageHeight = this.imagePreview.getFitHeight();
            updateWindowHeight();
            this.imagePreview.fitHeightProperty().addListener((observable, oldValue, newValue) -> updateWindowHeight());
        });
    }

    private void updateWindowHeight() {
        getStage().setHeight(this.startHeight + (this.imagePreview.getFitHeight() - this.startImageHeight));
    }

    @FXML
    private void saveImage(ActionEvent event) {
        BufferedImage transparentImage = ImageWorkHorse.applyFilter(this.cachedImage, TransparencyFilter.INSTANCE);
        FileUtils.askUserToSaveImageFile(getLogger(), getGameInstance(), transparentImage, "text-image");
    }

    private void updateImage() {
        int imageWidth;
        String widthText = this.widthTextField.getText();
        if (!NumberUtils.isInteger(widthText) || (imageWidth = Integer.parseInt(widthText)) <= 0 || imageWidth > MAX_IMAGE_SIZE) {
            this.widthTextField.setStyle(Constants.FX_STYLE_INVALID_TEXT);
            return;
        } else {
            this.widthTextField.setStyle(null);
        }

        int imageHeight;
        String heightText = this.heightTextField.getText();
        if (!NumberUtils.isInteger(heightText) || (imageHeight = Integer.parseInt(heightText)) <= 0 || imageHeight > MAX_IMAGE_SIZE) {
            this.heightTextField.setStyle(Constants.FX_STYLE_INVALID_TEXT);
            return;
        } else {
            this.heightTextField.setStyle(null);
        }

        float fontSize;
        String fontSizeText = this.fontSizeField.getText();
        if (!NumberUtils.isNumber(fontSizeText) || (fontSize = Float.parseFloat(fontSizeText)) < 1) {
            this.fontSizeField.setStyle(Constants.FX_STYLE_INVALID_TEXT);
            return;
        } else {
            this.fontSizeField.setStyle(null);
        }

        // Color.
        Color awtColor = ColorUtils.toAWTColor(this.colorPicker.getValue(), (byte) 0xFF);
        this.cachedImage = FroggerUtils.writeFroggerText(this.cachedImage, imageWidth, imageHeight, this.displayTextField.getText(), fontSize, awtColor, getGameInstance().isPSX());
        this.imagePreview.setPreserveRatio(false);
        this.imagePreview.setFitWidth(this.cachedImage.getWidth());
        this.imagePreview.setFitHeight(this.cachedImage.getHeight());
        BufferedImage displayImage = ImageWorkHorse.applyFilter(this.cachedImage, BlackFilter.INSTANCE);
        this.imagePreview.setImage(FXUtils.toFXImage(displayImage, false));
    }

    /**
     * Open the level info controller.
     */
    public static void openEditor(FroggerGameInstance gameInstance) {
        FXUtils.createWindowFromFXMLTemplate("window-text-maker", new FroggerTextBuilderUIController(gameInstance), "Text Image Generator", false);
    }
}
