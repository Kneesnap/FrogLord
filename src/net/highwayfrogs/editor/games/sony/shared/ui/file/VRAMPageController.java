package net.highwayfrogs.editor.games.sony.shared.ui.file;

import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import lombok.SneakyThrows;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.file.vlo.GameImage;
import net.highwayfrogs.editor.file.vlo.VLOArchive;
import net.highwayfrogs.editor.games.sony.SCGameInstance;
import net.highwayfrogs.editor.gui.GameUIController;
import net.highwayfrogs.editor.system.AbstractStringConverter;
import net.highwayfrogs.editor.system.Tuple2;
import net.highwayfrogs.editor.utils.FXUtils;
import net.highwayfrogs.editor.utils.NumberUtils;
import net.highwayfrogs.editor.utils.Utils;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Allows editing the arrangement of a VRAM page.
 * Created by Kneesnap on 12/2/2018.
 */
public class VRAMPageController extends GameUIController<SCGameInstance> {
    @FXML private ImageView imageView;
    @FXML private ChoiceBox<Integer> pcPageSelection;
    @FXML private GridPane psxGridPane;

    @FXML private ImageView selectedView;
    @FXML private Label xLabel;
    @FXML private Label yLabel;
    @FXML private TextField xField;
    @FXML private TextField yField;
    @FXML private Label textLabel;

    // Editor data.
    private BufferedImage fullImage;
    private BufferedImage[] splitImages;
    private final HashSet<Short> changedPages = new HashSet<>(); // A set of pages which need updating.
    private final ImageView[] splitImageViews = new ImageView[GameImage.TOTAL_PAGES]; // A set of pages which need updating.
    private final HBox[] splitHBoxes = new HBox[splitImageViews.length];
    private boolean[][] overlapGrid; // Used to test if textures overlap.
    private int selectedPage;

    // Configuration:
    private final VLOArchive vloArchive;
    private final VLOController controller;
    private GameImage selectedImage;
    private final Map<GameImage, Tuple2<Short, Short>> originalState = new HashMap<>();

    public VRAMPageController(VLOController controller) {
        super(controller.getGameInstance());
        this.vloArchive = controller.getFile();
        this.controller = controller;
    }

    @Override
    protected void onControllerLoad(Node rootNode) {
        // Choose visibility of UI based on
        this.overlapGrid = new boolean[isPsxMode() ? GameImage.PSX_FULL_PAGE_WIDTH : GameImage.PC_PAGE_WIDTH][isPsxMode() ? GameImage.PSX_PAGE_HEIGHT : GameImage.PC_PAGE_HEIGHT];
        setupImages();

        if (isPsxMode()) {
            int index = 0;
            for (int row = 0; row < psxGridPane.getRowConstraints().size(); row++) { // y
                for (int column = 0; column < psxGridPane.getColumnConstraints().size(); column++) { // x
                    HBox newBox = new HBox();
                    this.splitHBoxes[index] = newBox;
                    newBox.getChildren().add(this.splitImageViews[index++]);
                    psxGridPane.add(newBox, column, row);
                }
            }

            for (int i = 0; i < this.splitImageViews.length; i++) {
                final int finalIndex = i;

                // Handle a click.
                splitImageViews[i].setOnMousePressed(evt -> {
                    if (evt.isPrimaryButtonDown()) {
                        this.selectedPage = finalIndex;
                        updateAll();
                    }
                });

                splitImageViews[i].setOnScroll(this::handleScroll);
            }

        } else {
            pcPageSelection.setItems(FXCollections.observableArrayList(Utils.getIntegerList(this.splitImages.length)));
            pcPageSelection.setConverter(new AbstractStringConverter<>(findPage -> {
                int total = 0;
                for (int i = 0; i < vloArchive.getImages().size(); i++)
                    if (vloArchive.getImages().get(i).getMultiplierPage() == findPage)
                        total++;

                return "Texture Page #" + findPage + " [" + total + " textures]";
            }));

            pcPageSelection.setValue(this.selectedPage);
            pcPageSelection.getSelectionModel().select(this.selectedPage);
            pcPageSelection.valueProperty().addListener(((observable, oldValue, newValue) -> {
                this.selectedPage = newValue;
                updateAll();
            }));
        }

        imageView.setOnKeyPressed(this::handleKeyPress);
        imageView.setOnScroll(this::handleScroll); // The scroll wheel will scroll through the texture pages.
        imageView.setOnMousePressed(evt -> {
            if (!evt.isPrimaryButtonDown())
                return;

            double scale = imageView.getFitWidth() / (double) this.splitImages[this.selectedPage].getWidth();
            int realX = isPsxMode() ? ((this.selectedPage % GameImage.PSX_X_PAGES) * GameImage.PSX_PAGE_WIDTH) + (int) ((evt.getX() / scale) / (GameImage.PSX_FULL_PAGE_WIDTH / GameImage.PSX_PAGE_WIDTH)) : (int) evt.getX();
            int realY = isPsxMode() ? ((this.selectedPage / GameImage.PSX_X_PAGES) * GameImage.PSX_PAGE_HEIGHT) + (int) evt.getY() : ((this.selectedPage * GameImage.PC_PAGE_HEIGHT) + (int) evt.getY());
            GameImage newImage = vloArchive.getImage(realX, realY);

            if (newImage == this.selectedImage) {
                this.imageView.requestFocus(); // Allow arrow keys to be listened for, instead of moving cursor.
                return; // Has not changed.
            }

            if (newImage != null) {
                xField.setText(String.valueOf(newImage.getVramX()));
                yField.setText(String.valueOf(newImage.getVramY()));
                this.selectedView.setImage(FXUtils.toFXImage(newImage.toBufferedImage(), true));
                this.imageView.requestFocus(); // Allow arrow keys to be listened for, instead of moving cursor.
            }

            this.selectedImage = newImage;
            updateDisplay();
        });

        FXUtils.setHandleTestKeyPress(this.xField, NumberUtils::isSignedShort, newX -> setPosition(Integer.parseInt(newX), this.selectedImage.getVramY(), false));
        FXUtils.setHandleTestKeyPress(this.yField, NumberUtils::isSignedShort, newY -> setPosition(this.selectedImage.getVramX(), Integer.parseInt(newY), false));
        updateAll();
    }

    @Override
    public void onSceneAdd(Scene newScene) {
        super.onSceneAdd(newScene);

        // Run after stage / scene setup.
        Stage stage = (Stage) newScene.getWindow();
        stage.setOnCloseRequest(evt -> cancel()); // Window closed -> cancel.
        FXUtils.closeOnEscapeKey(stage, this::cancel); // Escape -> cancel.
    }

    /**
     * Tests if this is in PS1 mode.
     */
    public boolean isPsxMode() {
        return vloArchive.isPsxMode();
    }

    private void handleScroll(ScrollEvent evt) {
        if (Math.abs(evt.getDeltaY()) < 1)
            return; // Didn't move enough;

        int newPage = Math.min(Math.max(0, this.selectedPage + (evt.getDeltaY() > 0 ? -1 : 1)), this.splitImages.length - 1); // Gets the new page number.
        if (newPage != this.selectedPage) { // There is a change between the current page and the new one.
            if (!isPsxMode()) { // Update the selected menu option.
                pcPageSelection.setValue(newPage);
                pcPageSelection.getSelectionModel().select(newPage);
            }

            this.selectedPage = newPage; // Update the selected page.
            updateAll();
        }
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

        short finalX = (short) Math.min(Math.max(0, x), (this.fullImage.getWidth() - this.selectedImage.getFullWidth()));
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

        this.imageView.setImage(FXUtils.toFXImage(this.splitImages[this.selectedPage], false));
        this.imageView.setPreserveRatio(false);
        this.imageView.setFitWidth(256);
        this.imageView.setFitHeight(256);
    }

    @SneakyThrows
    private void setupImages() {
        this.fullImage = vloArchive.makeVRAMImage(this.fullImage); // Main image. (Must run first so split images have something to grab from.)

        // Setup image views.
        if (isPsxMode())
            for (int i = 0; i < this.splitImageViews.length; i++)
                this.splitImageViews[i] = new ImageView();

        // Setup images. (After views)
        int totalPages = GameImage.TOTAL_PAGES;
        this.splitImages = new BufferedImage[totalPages];
        for (int i = 0; i < this.splitImages.length; i++) {
            this.splitImages[i] = new BufferedImage(isPsxMode() ? GameImage.PSX_FULL_PAGE_WIDTH : GameImage.PC_PAGE_WIDTH, isPsxMode() ? GameImage.PSX_PAGE_HEIGHT : GameImage.PC_PAGE_HEIGHT, this.fullImage.getType());
            updateSplitImage(i);
        }
    }

    private void updateSplitImage(int splitIndex) {
        int startX = (isPsxMode() ? ((splitIndex % GameImage.PSX_X_PAGES) * GameImage.PSX_FULL_PAGE_WIDTH) : 0);
        int startY = (isPsxMode() ? ((splitIndex / GameImage.PSX_X_PAGES) * GameImage.PSX_PAGE_HEIGHT) : (splitIndex * GameImage.PC_PAGE_HEIGHT));
        int width = (isPsxMode() ? GameImage.PSX_FULL_PAGE_WIDTH : GameImage.PC_PAGE_WIDTH);
        int height = (isPsxMode() ? GameImage.PSX_PAGE_HEIGHT : GameImage.PC_PAGE_HEIGHT);

        // Draw over image.
        BufferedImage image = this.splitImages[splitIndex];
        Graphics2D graphics = image.createGraphics();
        graphics.drawImage(this.fullImage, 0, 0, image.getWidth(), image.getHeight(), startX, startY, startX + width, startY + height, null);
        graphics.dispose();

        if (isPsxMode()) { // Update displayed image.
            ImageView updateView = this.splitImageViews[splitIndex];
            updateView.setImage(FXUtils.toFXImage(image, false));
            updateView.setFitWidth(32);
            updateView.setFitHeight(128);
        }
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

        if (isPsxMode()) { // Update which ImageView is highlighted.
            for (int i = 0; i < splitHBoxes.length; i++)
                splitHBoxes[i].setStyle(null); // Clear styles.

            splitHBoxes[this.selectedPage].setStyle("-fx-border-color: red;-fx-border-width: 1;");
        }
    }

    private void updateWarning() {
        boolean hasSelectedImage = this.selectedImage != null;
        StringBuilder warning = new StringBuilder();

        // Keep within one page test.
        boolean multiPageTest = false;
        for (GameImage image : vloArchive.getImages()) {
            if (image.getMultiplierPage() != image.getEndPage()) {
                // TODO: It seems this may not actually cause issues. Perhaps it's a problem if it goes between 3+ pages? Not sure.
                warning.append("WARNING: Texture exceeds size of page ").append(image.getMultiplierPage()).append(".").append(Constants.NEWLINE);
                multiPageTest = true;
                break;
            }
        }


        // Overlap Test:
        // I couldn't find an algorithm on Google which could efficiently find if any boxes overlapped in an arbitrary list of boxes. Literally everything was in regard to testing if two boxes overlap. So, I made my own. I'm sure there's a more efficient way of doing this though.
        // Actually, something called an R-Tree may be a good data structure for this. Unfortunately, it's rather complicated.
        // https://stackoverflow.com/questions/13910287/data-structure-to-hold-list-of-rectangles
        if (!multiPageTest) { // If the previous test passes, skip this test, it will error.
            loopEnd:
            for (int i = 0; i < this.splitImages.length; i++) { // Test all the texture pages.
                // Clear grid.
                for (int y = 0; y < overlapGrid.length; y++)
                    Arrays.fill(overlapGrid[y], false);

                for (GameImage image : vloArchive.getImages()) {
                    if (image.getMultiplierPage() != i)
                        continue; // If an image's texture page is not the page we're currently looking for overlaps on, we should skip the image.

                    int baseX = (image.getVramX() % (vloArchive.isPsxMode() ? GameImage.PSX_PAGE_WIDTH * image.getWidthMultiplier() : GameImage.PC_PAGE_WIDTH));
                    int baseY = (image.getVramY() % (vloArchive.isPsxMode() ? GameImage.PSX_PAGE_HEIGHT : GameImage.PC_PAGE_HEIGHT));
                    for (int y = 0; y < image.getFullHeight(); y++) {
                        for (int x = 0; x < image.getFullWidth(); x++) {
                            int gridX = baseX + x;
                            int gridY = baseY + y;

                            if (gridY < 0 || gridY >= overlapGrid.length || gridX < 0 || gridX >= overlapGrid[gridY].length) {
                                warning.append("Bounds Error: [").append(gridX).append(", ").append(gridY).append("]").append(Constants.NEWLINE);
                                break loopEnd;
                            }

                            //getLogger().info("X: "+ (baseX + x) + ", Y: " + (baseY + y) + ", " + overlapGrid[baseY + y][baseX + x]);
                            if (overlapGrid[gridY][gridX]) {
                                warning.append("WARNING: Texture overlap on page ").append(image.getMultiplierPage()).append(" (").append(gridX).append(" ").append(gridY).append(").").append(Constants.NEWLINE);
                                break loopEnd;
                            }

                            overlapGrid[gridY][gridX] = true;
                        }
                    }
                }
            }
        }

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

        closeWindow();
    }

    @FXML
    private void confirmChanges(ActionEvent evt) {
        controller.updateDisplay();
        closeWindow();
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
        boolean isPsx = controller.getFile().isPsxMode();
        String templateName = "edit-file-vlo-vram-" + (isPsx ? "psx" : "pc");
        String windowTitle = (isPsx ? "PS1" : "PC") + " VRAM Editor";
        FXUtils.createWindowFromFXMLTemplate(templateName, new VRAMPageController(controller), windowTitle, true);
    }
}