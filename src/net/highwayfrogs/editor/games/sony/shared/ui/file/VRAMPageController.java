package net.highwayfrogs.editor.games.sony.shared.ui.file;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import lombok.SneakyThrows;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.games.psx.image.PsxVram;
import net.highwayfrogs.editor.games.psx.image.PsxVramBox;
import net.highwayfrogs.editor.games.sony.SCGameInstance;
import net.highwayfrogs.editor.games.sony.shared.utils.SCImageUtils;
import net.highwayfrogs.editor.games.sony.shared.vlo2.VloClut;
import net.highwayfrogs.editor.games.sony.shared.vlo2.VloFile;
import net.highwayfrogs.editor.games.sony.shared.vlo2.VloImage;
import net.highwayfrogs.editor.games.sony.shared.vlo2.VloUtils;
import net.highwayfrogs.editor.games.sony.shared.vlo2.vram.VloVramSnapshot;
import net.highwayfrogs.editor.gui.GameUIController;
import net.highwayfrogs.editor.system.Tuple2;
import net.highwayfrogs.editor.utils.FXUtils;
import net.highwayfrogs.editor.utils.NumberUtils;

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
    @FXML private GridPane pagePane;

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
    private final ImageView[] splitImageViews; // A set of pages which need updating.
    private final HBox[] splitHBoxes;
    private final boolean[][] overlapGrid; // Used to test if textures overlap.
    private int selectedPage;

    // Configuration:
    private final VloFile vloArchive;
    private final VLOController controller;
    private VloImage selectedImage;
    private final Map<VloImage, Tuple2<Short, Short>> originalState = new HashMap<>();

    private static final int PC_PAGE_PREVIEW_WIDTH = 76;
    private static final int PSX_PAGE_PREVIEW_WIDTH = 32;
    private static final int PAGE_PREVIEW_HEIGHT = 128;
    public static final int VRAM_EXPORT = VloImage.DEFAULT_IMAGE_NOT_TRANSPARENT_EXPORT_SETTINGS;

    public VRAMPageController(VLOController controller) {
        super(controller.getGameInstance());
        this.vloArchive = controller.getFile();
        this.controller = controller;

        boolean psxMode = this.vloArchive.isPsxMode();
        this.splitImageViews = new ImageView[VloUtils.getPageCount(psxMode)];
        this.overlapGrid = new boolean[VloUtils.getVramMaxPositionY(psxMode)][VloUtils.getVramUnitMaxPositionX(psxMode)];
        this.splitHBoxes = new HBox[this.splitImageViews.length];
    }

    @Override
    protected void onControllerLoad(Node rootNode) {
        // Choose visibility of UI based on

        setupImages();

        // Setup page pane.
        boolean psxMode = isPsxMode();
        int columnCount = VloUtils.getPageCount(psxMode) / 2;
        this.pagePane.getColumnConstraints().clear();
        int columnWidth = psxMode ? PSX_PAGE_PREVIEW_WIDTH : PC_PAGE_PREVIEW_WIDTH;
        for (int i = 0; i < columnCount; i++)
            this.pagePane.getColumnConstraints().add(new ColumnConstraints(columnWidth, columnWidth, columnWidth));

        int index = 0;
        for (int row = 0; row < this.pagePane.getRowConstraints().size(); row++) { // y
            for (int column = 0; column < this.pagePane.getColumnConstraints().size(); column++) { // x
                HBox newBox = new HBox();
                this.splitHBoxes[index] = newBox;
                newBox.getChildren().add(this.splitImageViews[index++]);
                this.pagePane.add(newBox, column, row);
            }
        }

        for (int i = 0; i < this.splitImageViews.length; i++) {
            final int finalIndex = i;

            // Handle a click.
            this.splitImageViews[i].setOnMousePressed(evt -> {
                if (evt.isPrimaryButtonDown()) {
                    this.selectedPage = finalIndex;
                    updateAll();
                }
            });

            this.splitImageViews[i].setOnScroll(this::handleScroll);
        }

        this.imageView.setOnKeyPressed(this::handleKeyPress);
        this.imageView.setOnScroll(this::handleScroll); // The scroll wheel will scroll through the texture pages.
        this.imageView.setOnMousePressed(evt -> {
            if (!evt.isPrimaryButtonDown())
                return;

            double scale = this.imageView.getFitWidth() / (double) this.splitImages[this.selectedPage].getWidth();
            int realX = VloUtils.getPageExpandedStartX(isPsxMode(), this.selectedPage) + (int) (evt.getX() / scale);
            int realY = VloUtils.getPageStartY(isPsxMode(), this.selectedPage) + (int) evt.getY();
            VloImage newImage = getImage(this.vloArchive, realX, realY);

            if (newImage == this.selectedImage) {
                this.imageView.requestFocus(); // Allow arrow keys to be listened for, instead of moving cursor.
                return; // Has not changed.
            }

            if (newImage != null) {
                this.xField.setText(String.valueOf(newImage.getVramX()));
                this.yField.setText(String.valueOf(newImage.getVramY()));
                this.selectedView.setImage(FXUtils.toFXImage(newImage.toBufferedImage(), true));
                this.imageView.requestFocus(); // Allow arrow keys to be listened for, instead of moving cursor.
            }

            if (this.selectedImage == newImage)
                return;

            // Update image selection.
            Graphics2D graphics = this.fullImage.createGraphics();
            try {
                if (this.selectedImage != null) {
                    writeVramImage(graphics, this.selectedImage, false);
                    markPagesChanged(this.selectedImage);
                }

                if (newImage != null) {
                    writeVramImage(graphics, newImage, true);
                    markPagesChanged(newImage);
                }
            } finally {
                graphics.dispose();
            }

            this.selectedImage = newImage;
            updateImage();
            updateDisplay();
        });

        FXUtils.setHandleTestKeyPress(this.xField, NumberUtils::isSignedShort, newX -> setPosition(Integer.parseInt(newX), this.selectedImage.getVramY(), false));
        FXUtils.setHandleTestKeyPress(this.yField, NumberUtils::isSignedShort, newY -> setPosition(this.selectedImage.getVramX(), Integer.parseInt(newY), false));
        updateAll();
    }

    private static VloImage getImage(VloFile file, double x, double y) {
        for (VloImage image : file.getImages()) {
            int width = image.getPaddedWidth();
            if (file.isPsxMode())
                width *= getWidthDrawMultiplier(image);

            int startX = image.getExpandedVramX();
            int endX = startX + width;
            int startY = image.getVramY();
            int endY = startY + image.getPaddedHeight();
            if (x >= startX && x < endX && y >= startY && y < endY)
                return image;
        }

        return null;
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
        return this.vloArchive.isPsxMode();
    }

    private void handleScroll(ScrollEvent evt) {
        if (Math.abs(evt.getDeltaY()) < 1)
            return; // Didn't move enough;

        int newPage = Math.min(Math.max(0, this.selectedPage + (evt.getDeltaY() > 0 ? -1 : 1)), this.splitImages.length - 1); // Gets the new page number.
        if (newPage != this.selectedPage) { // There is a change between the current page and the new one.
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

        short finalX = (short) Math.min(Math.max(0, x), (this.fullImage.getWidth() - this.selectedImage.getPaddedWidth()));
        short finalY = (short) Math.min(Math.max(0, y), this.fullImage.getHeight() - this.selectedImage.getPaddedHeight());
        updateTextFields |= ((short) x != finalX) || ((short) y != finalY);
        if (finalX == this.selectedImage.getVramX() && finalY == this.selectedImage.getVramY())
            return; // No change!

        markPagesChanged(this.selectedImage);
        saveOriginalPosition();

        this.selectedImage.setVramX(finalX);
        if (updateTextFields)
            this.xField.setText(String.valueOf(this.selectedImage.getVramX()));

        this.selectedImage.setVramY(finalY);
        if (updateTextFields)
            this.yField.setText(String.valueOf(this.selectedImage.getVramY()));

        markPagesChanged(this.selectedImage);
        updateAll();
    }

    private void markPagesChanged(VloImage image) {
        short startPage = image.getPage();
        short endPage = image.getEndPage();

        boolean psxMode = this.vloArchive.isPsxMode();
        int startPageX = VloUtils.getPageGridX(psxMode, startPage);
        int startPageY = VloUtils.getPageGridY(psxMode, startPage);
        int endPageX = VloUtils.getPageGridX(psxMode, endPage);
        int endPageY = VloUtils.getPageGridY(psxMode, endPage);
        for (int y = startPageY; y <= endPageY; y++)
            for (int x = startPageX; x <= endPageX; x++)
                this.changedPages.add((short) VloUtils.getPageFromGridPos(psxMode, x, y));
    }

    private void updateImage() {
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
        updateFullImage(); // Main image. (Must run first so split images have something to grab from.)

        // Setup image views.
        boolean psxMode = isPsxMode();
        for (int i = 0; i < this.splitImageViews.length; i++) {
            ImageView newImageView = new ImageView();
            newImageView.setFitWidth(psxMode ? PSX_PAGE_PREVIEW_WIDTH : PC_PAGE_PREVIEW_WIDTH);
            newImageView.setFitHeight(PAGE_PREVIEW_HEIGHT);
            this.splitImageViews[i] = newImageView;
        }

        // Setup images. (After views)
        int totalPages = VloUtils.getPageCount(psxMode);
        int expandedPageWidth = VloUtils.getExpandedPageWidth(psxMode);
        int pageHeight = VloUtils.getPageHeight(psxMode);
        this.splitImages = new BufferedImage[totalPages];
        for (int i = 0; i < this.splitImages.length; i++) {
            this.splitImages[i] = new BufferedImage(expandedPageWidth, pageHeight, this.fullImage.getType());
            updateSplitImage(i);
        }
    }

    private void updateFullImage() {
        this.fullImage = makeVRAMImage(this.vloArchive, this.fullImage, this.selectedImage);
    }

    private void updateSplitImage(int page) {
        boolean psxMode = isPsxMode();
        int startX = VloUtils.getPageExpandedStartX(psxMode, page);
        int startY = VloUtils.getPageStartY(psxMode, page);
        int pageWidth = VloUtils.getExpandedPageWidth(psxMode);
        int pageHeight = VloUtils.getPageHeight(psxMode);

        // Draw over image.
        BufferedImage image = this.splitImages[page];
        Graphics2D graphics = image.createGraphics();
        graphics.drawImage(this.fullImage, 0, 0, image.getWidth(), image.getHeight(), startX, startY, startX + pageWidth, startY + pageHeight, null);
        graphics.dispose();

        // Update displayed image.
        ImageView updateView = this.splitImageViews[page];
        updateView.setImage(FXUtils.toFXImage(image, false));
    }

    private void updateAll() {
        updateFullImage(); // Update main image. (Before updating pages.)
        updateImage();
        updateDisplay();
    }

    private void updateDisplay() {
        boolean hasSelectedImage = this.selectedImage != null;
        this.xLabel.setVisible(hasSelectedImage);
        this.yLabel.setVisible(hasSelectedImage);
        this.xField.setVisible(hasSelectedImage);
        this.yField.setVisible(hasSelectedImage);
        this.selectedView.setVisible(hasSelectedImage);
        updateWarning();

        for (int i = 0; i < this.splitHBoxes.length; i++)
            this.splitHBoxes[i].setStyle(null); // Clear styles.

        this.splitHBoxes[this.selectedPage].setStyle("-fx-border-color: red;-fx-border-width: 1;");
    }

    private void updateWarning() {
        boolean hasSelectedImage = this.selectedImage != null;
        StringBuilder warning = new StringBuilder();

        if (hasSelectedImage) {
            String textureName = this.selectedImage.getName();
            if (textureName != null)
                warning.append("Texture: ").append(textureName).append(Constants.NEWLINE);

            warning.append("Texture ID: ").append(this.selectedImage.getTextureId()).append(", Local ID: ")
                    .append(this.selectedImage.getLocalImageID()).append(Constants.NEWLINE);

            VloClut clut = this.selectedImage.getClut();
            if (clut != null)
                warning.append("Clut ").append(this.selectedImage.getParent().getClutList().getCluts().indexOf(clut))
                    .append(": (").append(clut.getX()).append(", ").append(clut.getY()).append(")").append(Constants.NEWLINE);
        }

        // Keep within one page test.
        boolean multiPageTest = false;
        for (VloImage image : this.vloArchive.getImages()) {
            if (image.getPage() != image.getEndPage()) {
                // TODO: It seems this may not actually cause issues. Perhaps it's a problem if it goes between 3+ pages? Not sure.
                warning.append("WARNING: Texture exceeds size of page ").append(image.getPage()).append(".").append(Constants.NEWLINE);
                multiPageTest = true;
                break;
            }
        }


        // Overlap Test:
        // I couldn't find an algorithm on Google which could efficiently find if any boxes overlapped in an arbitrary list of boxes. Literally everything was in regard to testing if two boxes overlap. So, I made my own. I'm sure there's a more efficient way of doing this though.
        // Actually, something called an R-Tree may be a good data structure for this. Unfortunately, it's rather complicated.
        // https://stackoverflow.com/questions/13910287/data-structure-to-hold-list-of-rectangles
        if (!multiPageTest) { // If the previous test passes, skip this test, it will error.
            // Clear grid.
            for (int y = 0; y < this.overlapGrid.length; y++)
                Arrays.fill(this.overlapGrid[y], false);

            loopEnd:
            for (VloImage image : this.vloArchive.getImages()) {
                int startX = image.getVramX();
                int startY = image.getVramY();
                for (int y = 0; y < image.getPaddedHeight(); y++) {
                    for (int x = 0; x < image.getUnitWidth(); x++) {
                        int pixelX = startX + x;
                        int pixelY = startY + y;
                        if (this.overlapGrid[pixelY][pixelX]) {
                            warning.append("WARNING: Texture overlap on page ").append(image.getPage()).append(" (").append(pixelX).append(" ").append(pixelY).append(").").append(Constants.NEWLINE);
                            break loopEnd;
                        }

                        this.overlapGrid[pixelY][pixelX] = true;
                    }
                }
            }
        }

        // Finish warning.
        if (warning.length() > 0) { // Has warning.
            this.textLabel.setText(warning.toString());
            this.textLabel.setTextFill(Color.RED);
            this.textLabel.setVisible(true);
        } else { // No warning.
            this.textLabel.setText("Select an image in the texture page.");
            this.textLabel.setTextFill(Color.BLACK);
            this.textLabel.setVisible(!hasSelectedImage);
        }
    }

    @FXML
    private void cancelChanges(ActionEvent evt) {
        cancel();
    }

    private void cancel() {
        for (Entry<VloImage, Tuple2<Short, Short>> entry : this.originalState.entrySet()) {
            VloImage image = entry.getKey();
            Tuple2<Short, Short> tuple = entry.getValue();
            image.setVramX(tuple.getA());
            image.setVramY(tuple.getB());
        }
        this.originalState.clear();

        closeWindow();
    }

    @FXML
    private void confirmChanges(ActionEvent evt) {
        this.controller.updateDisplay();
        closeWindow();
    }

    private void saveOriginalPosition() {
        if (!this.originalState.containsKey(this.selectedImage)) // Save original state, in case everything is cancelled.
            this.originalState.put(this.selectedImage, new Tuple2<>(this.selectedImage.getVramX(), this.selectedImage.getVramY()));
    }

    /**
     * Open the VRAM editor.
     * @param controller The VLO controller we'll be modifying.
     */
    public static void openEditor(VLOController controller) {
        String windowTitle = "VRAM Viewer [" + controller.getFile().getFileDisplayName() + "]";
        FXUtils.createWindowFromFXMLTemplate("edit-file-vlo-vram", new VRAMPageController(controller), windowTitle, false);
    }

    /**
     * Create a BufferedImage which effectively mirrors how Frogger will layout a VLO in memory.
     * One pixel maps to one nibble of VRAM data. (One four-bit clut pixel, or 1/4th of a 16-bit pixel)
     * On the PS1, multiple pixels can be stored in a single byte, so there is a loss of quality.
     * @param vloFile The vlo file to write images from
     * @param resultImage The image to write the data onto. If the image is not the right dimensions, it will make a new image and use that one.
     * @return resultImage
     */
    public static BufferedImage makeVRAMImage(VloFile vloFile, BufferedImage resultImage, VloImage selectedImage) {
        if (vloFile.getGameInstance().getVloTree() != null) {
            VloVramSnapshot snapshot = vloFile.getGameInstance().getVloTree().getVramSnapshot(vloFile);
            if (snapshot != null)
                return snapshot.toBufferedImage(resultImage, null); // TODO: selectedImage fix? Nah, this needs a rewrite to use entries.
        }

        boolean psxMode = vloFile.isPsxMode();
        int vramWidth = VloUtils.getVramExpandedMaxPositionX(psxMode);
        int vramHeight = VloUtils.getVramMaxPositionY(psxMode);
        if (resultImage == null || (vramWidth != resultImage.getWidth() || vramHeight != resultImage.getHeight()))
            resultImage = new BufferedImage(vramWidth, vramHeight, BufferedImage.TYPE_INT_ARGB);

        // Draw on image.
        Graphics2D graphics = resultImage.createGraphics();

        // Fill background.
        graphics.setColor(Constants.COLOR_TURQUOISE);
        graphics.fillRect(0, 0, resultImage.getWidth(), resultImage.getHeight());

        if (psxMode) {
            // Draw screen-buffer as a different color.
            drawVramSquare(graphics, vloFile.getGameInstance().getPrimaryFrameBuffer(), Constants.COLOR_DEEP_GREEN);
            drawVramSquare(graphics, vloFile.getGameInstance().getSecondaryFrameBuffer(), Constants.COLOR_DARK_YELLOW);

            // Draw cluts.
            for (VloClut clut : vloFile.getClutList().getCluts())
                graphics.drawImage(clut.makeImage(), null, clut.getX() * PsxVram.PSX_VRAM_MAX_PIXELS_PER_UNIT, clut.getY());
        }

        // Draw images.
        for (VloImage vloImage : vloFile.getImages())
            writeVramImage(graphics, vloImage, vloImage == selectedImage);

        graphics.dispose(); // Cleanup.
        return resultImage;
    }

    private static void drawVramSquare(Graphics2D graphics, PsxVramBox size, java.awt.Color color) {
        if (size == null)
            return;

        graphics.setColor(color); // Next frame.
        graphics.fillRect(size.getX(), size.getY(), size.getWidth() * PsxVram.PSX_VRAM_MAX_PIXELS_PER_UNIT, size.getHeight());
    }

    private static int getWidthDrawMultiplier(VloImage vloImage) {
        if (!vloImage.getParent().isPsxMode())
            return 1;

        return PsxVram.PSX_VRAM_MAX_PIXELS_PER_UNIT / vloImage.getWidthMultiplier();
    }

    /**
     * Writes the vram image to the graphics object.
     * The image is scaled to expanded vram form for proper previewing
     * @param graphics the graphics object to write to
     * @param vloImage the image to draw
     * @param selected whether the image is selected by the user
     */
    public static void writeVramImage(Graphics2D graphics, VloImage vloImage, boolean selected) {
        BufferedImage awtImage = vloImage.toBufferedImage(VRAM_EXPORT);

        // Vram scaling.
        awtImage = SCImageUtils.scaleWidth(awtImage, getWidthDrawMultiplier(vloImage));

        // Draw image.
        int x = vloImage.getExpandedVramX();
        int y = vloImage.getVramY();
        graphics.drawImage(awtImage, null, x, y);

        if (selected) {
            graphics.setColor(java.awt.Color.RED);
            int endX = x + awtImage.getWidth() - 1, endY = y + awtImage.getHeight() - 1;

            graphics.drawLine(x, y, endX, y);
            graphics.drawLine(x, y + 1, x, endY - 1);
            graphics.drawLine(x, endY, endX, endY);
            graphics.drawLine(endX, y + 1, endX, endY - 1);
        }
    }
}