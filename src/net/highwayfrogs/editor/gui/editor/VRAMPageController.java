package net.highwayfrogs.editor.gui.editor;

import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Point2D;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Pair;
import lombok.SneakyThrows;
import net.highwayfrogs.editor.Utils;
import net.highwayfrogs.editor.file.vlo.GameImage;
import net.highwayfrogs.editor.file.vlo.ImageFilterSettings;
import net.highwayfrogs.editor.file.vlo.ImageFilterSettings.ImageState;
import net.highwayfrogs.editor.file.vlo.VLOArchive;
import net.highwayfrogs.editor.gui.GUIMain;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.ResourceBundle;

/**
 * Allows editing the arrangement of a VRAM page.
 * Created by Kneesnap on 12/2/2018.
 */
public class VRAMPageController implements Initializable {
    @FXML private ImageView imageView;
    @FXML private ImageView miniView;
    @FXML private Label xLabel;
    @FXML private Label yLabel;
    @FXML private TextField xField;
    @FXML private TextField yField;
    @FXML private Button applyButton;

    private Stage stage;
    private VLOArchive vloArchive;
    private VLOController controller;
    private GameImage selectedImage;
    private Map<GameImage, Pair<Short, Short>> originalState = new HashMap<>();

    private static final ImageFilterSettings SETTINGS = new ImageFilterSettings(ImageState.EXPORT);
    private static final int EXTRA_SCROLL_BUFFER = 30;

    public VRAMPageController(Stage stage, VLOController controller) {
        this.stage = stage;
        this.vloArchive = controller.getFile();
        this.controller = controller;
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        updateImage();
        updateDisplay();

        Platform.runLater(() -> setupScene(stage.getScene()));
    }

    private void setupScene(Scene scene) {
        scene.setOnScroll(evt -> {
            Rectangle2D viewport = imageView.getViewport();

            double imgHeight = imageView.getImage().getHeight();
            double viewportHeight = Math.min(imgHeight, stage.getHeight());
            double minY = Math.min(EXTRA_SCROLL_BUFFER + imgHeight - viewportHeight, Math.max(0, viewport.getMinY() - evt.getDeltaY()));
            imageView.setViewport(new Rectangle2D(viewport.getMinX(), minY, viewport.getWidth(), viewportHeight));
        });

        stage.setOnCloseRequest(evt -> cancel());
        scene.setOnKeyPressed(evt -> {
            if (evt.getCode() == KeyCode.ESCAPE)
                cancel();
        });

        imageView.setOnMousePressed(evt -> {
            if (!evt.isPrimaryButtonDown())
                return;

            Point2D coords = getImageCoords(imageView, evt.getX(), evt.getY());
            GameImage newImage = vloArchive.getImages().stream()
                    .filter(image -> image.contains(coords.getX(), coords.getY()))
                    .findAny().orElse(null);

            if (newImage == this.selectedImage)
                return; // Has not changed.

            if (newImage != null) {
                xField.setText(String.valueOf(newImage.getVramX()));
                yField.setText(String.valueOf(newImage.getVramY()));
                miniView.setImage(SwingFXUtils.toFXImage(newImage.toBufferedImage(SETTINGS), null));
            }

            this.selectedImage = newImage;
            updateDisplay();
        });
    }

    private void updateImage() {
        BufferedImage vramImage = makeVRAMImage(vloArchive);

        this.imageView.setPreserveRatio(true);
        if (this.imageView.getViewport() == null)
            this.imageView.setViewport(new Rectangle2D(0, 0, vramImage.getWidth(), vramImage.getHeight()));
        this.imageView.setImage(SwingFXUtils.toFXImage(vramImage, null));
        this.imageView.setFitWidth(vramImage.getWidth());
        this.imageView.setFitHeight(vramImage.getHeight());
    }

    private void updateDisplay() {
        boolean hasSelectedImage = this.selectedImage != null;
        xLabel.setVisible(hasSelectedImage);
        yLabel.setVisible(hasSelectedImage);
        xField.setVisible(hasSelectedImage);
        yField.setVisible(hasSelectedImage);
        miniView.setVisible(hasSelectedImage);
        applyButton.setVisible(hasSelectedImage);
    }

    @FXML
    private void cancelChanges(ActionEvent evt) {
        cancel();
    }

    private void cancel() {
        for (Entry<GameImage, Pair<Short, Short>> entry : originalState.entrySet()) {
            GameImage image = entry.getKey();
            Pair<Short, Short> pair = entry.getValue();
            image.setVramX(pair.getKey());
            image.setVramY(pair.getValue());
        }
        originalState.clear();

        this.stage.close();
    }

    @FXML
    private void confirmChanges(ActionEvent evt) {
        controller.updateDisplay();
        this.stage.close();
    }

    @FXML
    private void applyChanges(ActionEvent evt) {
        short newX;
        short newY;

        try {
            newX = Short.parseShort(xField.getText());
        } catch (NumberFormatException nfe) {
            System.out.println(xField.getText() + " is not a valid number.");
            return;
        }

        try {
            newY = Short.parseShort(yField.getText());
        } catch (NumberFormatException nfe) {
            System.out.println(yField.getText() + " is not a valid number.");
            return;
        }

        if (!originalState.containsKey(this.selectedImage)) // Save original state, in case everything is cancelled.
            originalState.put(this.selectedImage, new Pair<>(this.selectedImage.getVramX(), this.selectedImage.getVramY()));

        this.selectedImage.setVramX(newX);
        this.selectedImage.setVramY(newY);
        updateImage();
    }

    private static Point2D getImageCoords(ImageView view, double x, double y) {
        double xScale = x / view.getBoundsInLocal().getWidth();
        double yScale = y / view.getBoundsInLocal().getHeight();

        Rectangle2D viewport = view.getViewport();
        return new Point2D(viewport.getMinX() + (xScale * viewport.getWidth()), viewport.getMinY() + (yScale * viewport.getHeight()));
    }

    /**
     * Create a BufferedImage which effectively mirrors how Frogger will structure VRAM in-game.
     * @param vloArchive The archive to make an image of.
     * @return vramImage
     */
    public static BufferedImage makeVRAMImage(VLOArchive vloArchive) {
        int maxWidth = vloArchive.getImages().stream().mapToInt(img -> img.getVramX() + img.getFullWidth()).max().orElse(0);
        int maxHeight = vloArchive.getImages().stream().mapToInt(img -> img.getVramY() + img.getFullHeight()).max().orElse(0);

        BufferedImage vramImage = new BufferedImage(maxWidth, maxHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = vramImage.createGraphics();

        graphics.setColor(Color.MAGENTA);
        graphics.fillRect(0, 0, vramImage.getWidth(), vramImage.getHeight());

        for (GameImage image : vloArchive.getImages())
            graphics.drawImage(image.toBufferedImage(SETTINGS), null, image.getVramX(), image.getVramY());

        graphics.dispose();
        return vramImage;
    }

    /**
     * Open the VRAM editor.
     * @param controller The VLO controller we'll be modifying.
     */
    @SneakyThrows
    public static void openEditor(VLOController controller) {
        FXMLLoader loader = new FXMLLoader(Utils.getResource("javafx/vram-editor.fxml"));

        Stage newStage = new Stage();
        newStage.setTitle("VRAM Editor");

        loader.setController(new VRAMPageController(newStage, controller));
        AnchorPane anchorPane = loader.load();

        newStage.setScene(new Scene(anchorPane));
        newStage.setResizable(false);

        newStage.initModality(Modality.WINDOW_MODAL);
        newStage.setAlwaysOnTop(true);
        newStage.initOwner(GUIMain.MAIN_STAGE);
        newStage.showAndWait();
    }
}
