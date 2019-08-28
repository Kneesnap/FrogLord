package net.highwayfrogs.editor.gui.editor;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import lombok.SneakyThrows;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.file.vlo.GameImage;
import net.highwayfrogs.editor.file.vlo.VLOArchive;
import net.highwayfrogs.editor.system.AbstractStringConverter;
import net.highwayfrogs.editor.system.Tuple2;
import net.highwayfrogs.editor.utils.Utils;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.ResourceBundle;

/**
 * Allows editing the arrangement of a VRAM page.
 * Created by Kneesnap on 12/2/2018.
 */
public class VRAMPageController implements Initializable {
    @FXML private ImageView imageView;
    @FXML private ChoiceBox<Integer> pageSelection;

    @FXML private ImageView selectedView;
    @FXML private Label xLabel;
    @FXML private Label yLabel;
    @FXML private TextField xField;
    @FXML private TextField yField;
    @FXML private Label textLabel;

    // Editor data.
    private BufferedImage fullImage;
    private BufferedImage[] splitImages;
    private HashSet<Short> changedPages = new HashSet<>(); // A set of pages which need updating.
    //private boolean[][] overlapGrid; // Used to test if textures overlap.
    private int selectedPage;

    // Configuration:
    private Stage stage;
    private VLOArchive vloArchive;
    private VLOController controller;
    private GameImage selectedImage;
    private Map<GameImage, Tuple2<Short, Short>> originalState = new HashMap<>();

    public VRAMPageController(Stage stage, VLOController controller) {
        this.stage = stage;
        this.vloArchive = controller.getFile();
        this.controller = controller;
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Choose visibility of UI based on
        //this.overlapGrid = new boolean[isPsx ? GameImage.PSX_PAGE_WIDTH : GameImage.PC_PAGE_WIDTH][isPsx ? GameImage.PSX_PAGE_HEIGHT : GameImage.PC_PAGE_HEIGHT];
        setupImages();

        pageSelection.setItems(FXCollections.observableArrayList(Utils.getIntegerList(this.splitImages.length)));
        pageSelection.setConverter(new AbstractStringConverter<>(findPage -> {
            int total = 0;
            for (int i = 0; i < vloArchive.getImages().size(); i++)
                if (vloArchive.getImages().get(i).getMultiplierPage() == findPage)
                    total++;

            return "Texture Page #" + findPage + " [" + total + " textures]";
        }));

        pageSelection.setValue(this.selectedPage);
        pageSelection.getSelectionModel().select(this.selectedPage);
        pageSelection.valueProperty().addListener(((observable, oldValue, newValue) -> {
            this.selectedPage = newValue;
            updateAll();
        }));


        imageView.setOnKeyPressed(this::handleKeyPress);

        imageView.setOnMousePressed(evt -> {
            if (!evt.isPrimaryButtonDown())
                return;

            double scale = imageView.getFitWidth() / (double) this.splitImages[this.selectedPage].getWidth();
            int realX = isPsxMode() ? ((this.selectedPage % GameImage.PSX_X_PAGES) * GameImage.PSX_PAGE_WIDTH) + (int) (evt.getX() / scale) : (int) evt.getX();
            int realY = isPsxMode() ? ((this.selectedPage / GameImage.PSX_X_PAGES) * GameImage.PSX_PAGE_HEIGHT) + (int) evt.getY() : ((this.selectedPage * GameImage.PC_PAGE_HEIGHT) + (int) evt.getY());
            GameImage newImage = vloArchive.getImage(realX, realY);

            if (newImage == this.selectedImage) {
                this.imageView.requestFocus(); // Allow arrow keys to be listened for, instead of moving cursor.
                return; // Has not changed.
            }

            if (newImage != null) {
                xField.setText(String.valueOf(newImage.getVramX()));
                yField.setText(String.valueOf(newImage.getVramY()));
                this.selectedView.setImage(Utils.toFXImage(newImage.toBufferedImage(), true));
                this.imageView.requestFocus(); // Allow arrow keys to be listened for, instead of moving cursor.
            }

            this.selectedImage = newImage;
            updateDisplay();
        });

        Utils.setHandleTestKeyPress(this.xField, Utils::isSignedShort, newX -> setPosition(Integer.parseInt(newX), this.selectedImage.getVramY(), false));
        Utils.setHandleTestKeyPress(this.yField, Utils::isSignedShort, newY -> setPosition(this.selectedImage.getVramX(), Integer.parseInt(newY), false));
        updateAll();

        // Run after stage / scene setup.
        Platform.runLater(() -> {
            stage.setOnCloseRequest(evt -> cancel()); // Window closed -> cancel.
            Utils.closeOnEscapeKey(stage, this::cancel); // Escape -> cancel.
        });
    }

    /**
     * Tests if this is in PS1 mode.
     */
    public boolean isPsxMode() {
        return vloArchive.isPsxMode();
    }

    private void handleKeyPress(KeyEvent evt) {
        if (evt.getCode() == KeyCode.UP) {
            moveImage(0, -1);
            evt.consume(); // Don't select text boxes.
        } else if (evt.getCode() == KeyCode.DOWN) {
            moveImage(0, 1);
            evt.consume(); // Don't select text boxes.
        } else if (evt.getCode() == KeyCode.LEFT) {
            moveImage(-1, 0);
            evt.consume(); // Don't select text boxes.
        } else if (evt.getCode() == KeyCode.RIGHT) {
            moveImage(1, 0);
            evt.consume(); // Don't select text boxes.
        }
    }

    private void moveImage(int x, int y) {
        if (this.selectedImage != null)
            setPosition(this.selectedImage.getVramX() + x, this.selectedImage.getVramY() + y, true);
    }

    private void setPosition(int x, int y, boolean updateTextFields) {
        if (this.selectedImage == null)
            return;

        short finalX = (short) Math.min(Math.max(0, x), (this.fullImage.getWidth() * (isPsxMode() ? 4 : 1)) - this.selectedImage.getFullWidth());
        short finalY = (short) Math.min(Math.max(0, y), this.fullImage.getHeight() - this.selectedImage.getFullHeight());
        updateTextFields |= ((short) x != finalX) || ((short) y != finalY);
        if (finalX == this.selectedImage.getVramX() && finalY == this.selectedImage.getVramY())
            return; // No change!

        this.changedPages.add(this.selectedImage.getMultiplierPage()); // Mark the source page for updating.
        this.changedPages.add(this.selectedImage.getEndPage()); // Make sure if the image is split among two texture pages they both get updated.
        saveOriginalPosition();

        this.selectedImage.setVramX(finalX);
        if (updateTextFields)
            xField.setText(String.valueOf(this.selectedImage.getVramX()));

        this.selectedImage.setVramY(finalY);
        if (updateTextFields)
            yField.setText(String.valueOf(this.selectedImage.getVramY()));

        this.changedPages.add(this.selectedImage.getMultiplierPage()); // Mark the destination page for updating.
        this.changedPages.add(this.selectedImage.getEndPage()); // Make sure if the image is split among two texture pages they both get updated.
        updateAll();
    }

    private void updateImage() {
        this.fullImage = vloArchive.makeVRAMImage(this.fullImage); // Update main image. (Before updating pages.)
        for (Short updatePage : this.changedPages) // Update changed pages.
            updateSplitImage(updatePage);
        this.changedPages.clear();

        this.imageView.setImage(Utils.toFXImage(this.splitImages[this.selectedPage], false));
        this.imageView.setPreserveRatio(false);
        this.imageView.setFitWidth(256);
        this.imageView.setFitHeight(256);
    }

    @SneakyThrows
    private void setupImages() {
        this.fullImage = vloArchive.makeVRAMImage(this.fullImage); // Main image. (Must run first so split images have something to grab from.)

        // Setup images. (After views)
        int totalPages = GameImage.TOTAL_PAGES;
        this.splitImages = new BufferedImage[totalPages];
        for (int i = 0; i < this.splitImages.length; i++) {
            this.splitImages[i] = new BufferedImage(isPsxMode() ? GameImage.PSX_PAGE_WIDTH : GameImage.PC_PAGE_WIDTH, isPsxMode() ? GameImage.PSX_PAGE_HEIGHT : GameImage.PC_PAGE_HEIGHT, this.fullImage.getType());
            updateSplitImage(i);
        }
    }

    private void updateSplitImage(int splitIndex) {
        int startX = (isPsxMode() ? ((splitIndex % GameImage.PSX_X_PAGES) * GameImage.PSX_PAGE_WIDTH) : 0);
        int startY = (isPsxMode() ? ((splitIndex / GameImage.PSX_X_PAGES) * GameImage.PSX_PAGE_HEIGHT) : (splitIndex * GameImage.PC_PAGE_HEIGHT));
        int width = (isPsxMode() ? GameImage.PSX_PAGE_WIDTH : GameImage.PC_PAGE_WIDTH);
        int height = (isPsxMode() ? GameImage.PSX_PAGE_HEIGHT : GameImage.PC_PAGE_HEIGHT);

        // Draw over image.
        BufferedImage image = this.splitImages[splitIndex];
        Graphics2D graphics = image.createGraphics();
        graphics.drawImage(this.fullImage, 0, 0, image.getWidth(), image.getHeight(), startX, startY, startX + width, startY + height, null);
        graphics.dispose();
    }

    private void updateAll() {
        updateImage();
        updateDisplay();
    }

    private void updateDisplay() {
        boolean hasSelectedImage = this.selectedImage != null;
        xLabel.setVisible(hasSelectedImage);
        yLabel.setVisible(hasSelectedImage);
        xField.setVisible(hasSelectedImage);
        yField.setVisible(hasSelectedImage);
        selectedView.setVisible(hasSelectedImage);
        updateWarning();
    }

    private void updateWarning() {
        boolean hasSelectedImage = this.selectedImage != null;
        StringBuilder warning = new StringBuilder();

        // Keep within one page test.
        for (GameImage image : vloArchive.getImages()) {
            if (image.getMultiplierPage() != image.getEndPage()) {
                warning.append("WARNING: Texture exceeds size of page ").append(image.getMultiplierPage()).append(".").append(Constants.NEWLINE);
                break;
            }
        }

        /*
        // Overlap Test:
        // I couldn't find an algorithm on google which could efficiently find if any boxes overlapped in an arbitrary list of boxes. Literally everything was in regards to testing if two boxes overlap. So, I made my own. I'm sure there's a more efficient way of doing this though.
        // Clear grid.
        for (int y = 0; y < overlapGrid.length; y++)
            for (int x = 0; x < overlapGrid[y].length; x++)
                overlapGrid[y][x] = false;

        loopEnd: for (GameImage image : vloArchive.getImages()) {
            int baseX = (image.getVramX() % (vloArchive.isPsxMode() ? GameImage.PSX_PAGE_WIDTH * image.getWidthMultiplier() : GameImage.PC_PAGE_WIDTH));
            int baseY = (image.getVramY() % (vloArchive.isPsxMode() ? GameImage.PSX_PAGE_HEIGHT : GameImage.PC_PAGE_HEIGHT));
            for (int y = 0; y < image.getFullHeight(); y++) {
                for (int x = 0; x < image.getFullWidth(); x++) {
                    if (overlapGrid[baseY + y][baseX + x]) {
                        warning.append("WARNING: Texture overlap on page ").append(image.getPage()).append(" (").append(x).append(" ").append(y).append(").").append(Constants.NEWLINE);
                        break loopEnd;
                    }

                    overlapGrid[baseY + y][baseX + x] = true;
                }
            }
        }
        //TODO: Fix and enable.
        */

        // Finish warning.
        if (warning.length() > 0) { // Has warning.
            textLabel.setText(warning.toString());
            textLabel.setTextFill(Color.RED);
            textLabel.setVisible(true);
        } else { // No warning.
            textLabel.setText("Select an image in the texture page.");
            textLabel.setTextFill(Color.BLACK);
            textLabel.setVisible(!hasSelectedImage);
        }
    }

    @FXML
    private void cancelChanges(ActionEvent evt) {
        cancel();
    }

    private void cancel() {
        for (Entry<GameImage, Tuple2<Short, Short>> entry : originalState.entrySet()) {
            GameImage image = entry.getKey();
            Tuple2<Short, Short> tuple = entry.getValue();
            image.setVramX(tuple.getA());
            image.setVramY(tuple.getB());
        }
        originalState.clear();

        this.stage.close();
    }

    @FXML
    private void confirmChanges(ActionEvent evt) {
        controller.updateDisplay();
        this.stage.close();
    }

    private void saveOriginalPosition() {
        if (!originalState.containsKey(this.selectedImage)) // Save original state, in case everything is cancelled.
            originalState.put(this.selectedImage, new Tuple2<>(this.selectedImage.getVramX(), this.selectedImage.getVramY()));
    }

    /**
     * Open the VRAM editor.
     * @param controller The VLO controller we'll be modifying.
     */
    public static void openEditor(VLOController controller) {
        Utils.loadFXMLTemplate("vram-editor", "VRAM Editor", newStage -> new VRAMPageController(newStage, controller));
    }
}
