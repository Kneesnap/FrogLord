package net.highwayfrogs.editor.gui.editor;

import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Modality;
import javafx.stage.Stage;
import lombok.SneakyThrows;
import net.highwayfrogs.editor.Utils;
import net.highwayfrogs.editor.file.vlo.GameImage;
import net.highwayfrogs.editor.file.vlo.ImageFilterSettings;
import net.highwayfrogs.editor.file.vlo.ImageFilterSettings.ImageState;
import net.highwayfrogs.editor.gui.GUIMain;

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
    @FXML private Label fullLabel;
    @FXML private Label gameLabel;
    @FXML private CheckBox scaleCheckBox;

    private VLOController controller;
    private Stage stage;
    private GameImage image;
    private short width;
    private short height;
    private int colorIndex;

    private static final ImageFilterSettings SETTINGS = new ImageFilterSettings(ImageState.EXPORT).setAllowFlip(true);
    private static final Color[] COLORS = {
            Color.BLACK,
            Color.MAGENTA,
            Color.YELLOW,
            Color.GREEN,
    };

    public ImagePaddingController(Stage stage, VLOController controller) {
        this.stage = stage;
        this.controller = controller;
        this.image = controller.getSelectedImage();
        this.width = image.getIngameWidth();
        this.height = image.getIngameHeight();
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        updateDisplay();

        Platform.runLater(() -> stage.getScene().setOnKeyPressed(evt -> {
            if (evt.getCode() == KeyCode.UP) {
                moveVertical(1);
            } else if (evt.getCode() == KeyCode.DOWN) {
                moveVertical(-1);
            } else if (evt.getCode() == KeyCode.LEFT) {
                moveHorizontal(-1);
            } else if (evt.getCode() == KeyCode.RIGHT) {
                moveHorizontal(1);
            } else if (evt.getCode() == KeyCode.ESCAPE) {
                this.stage.close();
            }
        }));
    }

    private void updateDisplay() {
        fullLabel.setText("Full: [" + image.getFullWidth() + ", " + image.getFullHeight() + "]");
        gameLabel.setText("Game: [" + this.width + ", " + this.height + "]");

        BufferedImage image = this.image.toBufferedImage(SETTINGS);

        Graphics2D graphics = image.createGraphics();
        graphics.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, .5F));
        graphics.setPaint(COLORS[this.colorIndex]);

        int imgWidth = image.getWidth();
        int imgHeight = image.getHeight();
        int xClip = (imgWidth - this.width) / 2;
        int yClip = (imgHeight - this.height) / 2;
        int secondY = image.getHeight() - yClip;

        graphics.fillRect(0, 0, imgWidth, yClip);
        graphics.fillRect(0, secondY, imgWidth, yClip);
        graphics.fillRect(0, yClip, xClip, secondY - yClip);
        graphics.fillRect(imgWidth - xClip, yClip, xClip, secondY - yClip);
        graphics.dispose();

        boolean scale = this.scaleCheckBox.isSelected();
        imageView.setFitWidth(scale ? GameImage.MAX_DIMENSION : imgWidth);
        imageView.setFitHeight(scale ? GameImage.MAX_DIMENSION : imgHeight);
        imageView.setImage(SwingFXUtils.toFXImage(image, null));
    }

    @FXML
    private void cancelChanges(ActionEvent evt) {
        this.stage.close();
    }

    @FXML
    private void confirmChanges(ActionEvent evt) {
        this.image.setIngameWidth(this.width);
        this.image.setIngameHeight(this.height);
        this.controller.updateDisplay();
        this.stage.close();
    }

    @FXML
    private void onScaleUpdate(ActionEvent evt) {
        updateDisplay();
    }

    @FXML
    private void changeColor(ActionEvent evt) {
        this.colorIndex++;
        if (this.colorIndex == COLORS.length)
            this.colorIndex = 0;

        updateDisplay();
    }

    @FXML
    public void onUpArrow(ActionEvent evt) {
        moveVertical(1);
    }

    @FXML
    public void onDownArrow(ActionEvent evt) {
        moveVertical(-1);
    }

    @FXML
    public void onLeftArrow(ActionEvent evt) {
        moveHorizontal(-1);
    }

    @FXML
    public void onRightArrow(ActionEvent evt) {
        moveHorizontal(1);
    }

    private void moveVertical(int amount) {
        short newVal = (short) (this.height + amount);
        if (newVal >= 0 && image.getFullHeight() >= newVal) {
            this.height = newVal;
            updateDisplay();
        }
    }

    private void moveHorizontal(int amount) {
        short newVal = (short) (this.width + amount);
        if (newVal >= 0 && image.getFullWidth() >= newVal) {
            this.width = newVal;
            updateDisplay();
        }
    }

    /**
     * Open the padding menu for a particular image.
     * @param controller The VLO controller opening this.
     */
    @SneakyThrows
    public static void openPaddingMenu(VLOController controller) {
        FXMLLoader loader = new FXMLLoader(Utils.getResource("javafx/image-padding.fxml"));

        Stage newStage = new Stage();
        newStage.setTitle("Image Padding Editor");

        loader.setController(new ImagePaddingController(newStage, controller));
        AnchorPane anchorPane = loader.load();

        newStage.setScene(new Scene(anchorPane));
        newStage.setResizable(false);

        newStage.initModality(Modality.WINDOW_MODAL);
        newStage.setAlwaysOnTop(true);
        newStage.initOwner(GUIMain.MAIN_STAGE);
        newStage.showAndWait();
    }
}
