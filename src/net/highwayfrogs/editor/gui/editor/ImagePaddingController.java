package net.highwayfrogs.editor.gui.editor;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import net.highwayfrogs.editor.file.vlo.GameImage;
import net.highwayfrogs.editor.file.vlo.ImageFilterSettings;
import net.highwayfrogs.editor.file.vlo.ImageFilterSettings.ImageState;
import net.highwayfrogs.editor.file.vlo.ImageWorkHorse;
import net.highwayfrogs.editor.utils.Utils;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.net.URL;
import java.util.ResourceBundle;

/**
 * Controls a menu which allows changing the padding of a GameImage.
 * Created by Kneesnap on 12/2/2018.
 */
public class ImagePaddingController implements Initializable {
    @FXML private ImageView imageView;
    @FXML private Slider widthSlider;
    @FXML private Slider heightSlider;
    @FXML private ColorPicker bgColor;
    @FXML private Label labelInfoFull;
    @FXML private Label labelInfoInGame;
    @FXML private TextField textFieldUPad;
    @FXML private TextField textFieldVPad;

    private BufferedImage appliedImage;
    private VLOController controller;
    private Stage stage;
    private GameImage image;

    private final ImageFilterSettings filterSettings = new ImageFilterSettings(ImageState.EXPORT);

    public ImagePaddingController(Stage stage, VLOController controller) {
        this.stage = stage;
        this.controller = controller;
        this.image = controller.getSelectedImage();
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        this.widthSlider.setMax(this.image.getFullWidth());
        this.widthSlider.setValue(this.image.getIngameWidth());
        this.heightSlider.setMax(this.image.getFullHeight());
        this.heightSlider.setValue(this.image.getIngameHeight());
        this.bgColor.setValue(Color.BLACK);

        updateDisplay();
        Utils.closeOnEscapeKey(stage, null);

        this.bgColor.valueProperty().addListener((observable, oldValue, newValue) -> updateDisplay());
        this.widthSlider.valueProperty().addListener(((observable, oldValue, newValue) -> updateDisplay()));
        this.heightSlider.valueProperty().addListener((observable, oldValue, newValue) -> updateDisplay());
    }

    private void updateDisplay() {
        BufferedImage image = this.appliedImage = ImageWorkHorse.copyImage(this.image.toBufferedImage(filterSettings), this.appliedImage);

        Graphics2D graphics = image.createGraphics();
        graphics.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, .5F));
        graphics.setPaint(Utils.toAWTColor(this.bgColor.getValue()));

        int imgWidth = image.getWidth();
        int imgHeight = image.getHeight();
        int xClip = (imgWidth - (int) this.widthSlider.getValue()) / 2;
        int yClip = (imgHeight - (int) this.heightSlider.getValue()) / 2;
        int secondY = image.getHeight() - yClip;

        this.labelInfoFull.setText(imgWidth + "x" + imgHeight);
        this.labelInfoInGame.setText((imgWidth - (xClip * 2)) + "x" + (imgHeight - (yClip * 2)));

        this.textFieldUPad.setText(String.valueOf(xClip));
        this.textFieldVPad.setText(String.valueOf(yClip));

        graphics.fillRect(0, 0, imgWidth, yClip);
        graphics.fillRect(0, secondY, imgWidth, yClip);
        graphics.fillRect(0, yClip, xClip, secondY - yClip);
        graphics.fillRect(imgWidth - xClip, yClip, xClip, secondY - yClip);
        graphics.dispose();

        imageView.setImage(Utils.toFXImage(image, false));
    }

    @FXML
    private void cancelChanges(ActionEvent evt) {
        this.stage.close();
    }

    @FXML
    private void confirmChanges(ActionEvent evt) {
        this.image.setIngameWidth((short) this.widthSlider.getValue());
        this.image.setIngameHeight((short) this.heightSlider.getValue());
        this.controller.updateDisplay();
        this.stage.close();
    }

    /**
     * Open the padding menu for a particular image.
     * @param controller The VLO controller opening this.
     */
    public static void openPaddingMenu(VLOController controller) {
        Utils.loadFXMLTemplate(controller.getGameInstance(), "window-edit-vlo-image-padding", "Image Padding Editor", newStage -> new ImagePaddingController(newStage, controller));
    }
}