package net.highwayfrogs.editor.gui.fxobject;

import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import lombok.Getter;
import net.highwayfrogs.editor.games.psx.CVector;
import net.highwayfrogs.editor.games.psx.polygon.PSXPolygonType;
import net.highwayfrogs.editor.games.sony.shared.SCByteTextureUV;
import net.highwayfrogs.editor.games.psx.shading.PSXShadeTextureDefinition;
import net.highwayfrogs.editor.gui.GUIEditorGrid;
import net.highwayfrogs.editor.gui.editor.MeshViewController;
import net.highwayfrogs.editor.utils.Utils;

import java.awt.image.BufferedImage;
import java.util.function.Consumer;

/**
 * Represents a 2D editor for editing shade texture definitions.
 * This is a very basic editor, and is only intended for minor edits.
 * Another editor with mouse-draggable points will be available for material editors.
 * Created by Kneesnap on 1/22/2024.
 */
public abstract class PSXShadingEditor {
    @Getter private PSXShadeTextureDefinition shadeDefinition;
    private final ImageView previewImageView;
    private final boolean registerImageView;
    private final Consumer<BufferedImage> textureChangeListener = this::onTextureChangeReceived;
    private boolean staticUISetup;
    private GUIEditorGrid dynamicEditorGrid;
    private MeshViewController<?> editorController;
    private Label polygonTypeNameLabel;
    private CheckBox polygonTypeIsQuadCheckBox;
    private CheckBox polygonTypeGouraudCheckBox;
    private CheckBox polygonTypeTexturedCheckBox;

    public PSXShadingEditor(PSXShadeTextureDefinition shadeDefinition, ImageView previewImageView) {
        this.previewImageView = previewImageView != null ? previewImageView : new ImageView();
        this.registerImageView = (previewImageView == null);
        setShadeDefinition(shadeDefinition);
    }

    /**
     * Applies a new shade definition to the editor.
     * @param shadeDefinition the new shade definition
     */
    public void setShadeDefinition(PSXShadeTextureDefinition shadeDefinition) {
        if (this.shadeDefinition != null) {
            this.shadeDefinition.getImageChangeListeners().remove(this.textureChangeListener);
            this.shadeDefinition.onDispose();
        }

        // Apply new shade definition.
        this.shadeDefinition = shadeDefinition;

        // Abort.
        if (shadeDefinition == null) {
            updateUI();
            return;
        }

        // Register new shade definition.
        shadeDefinition.onRegister();
        shadeDefinition.getImageChangeListeners().add(this.textureChangeListener);

        // Update editor UI elements, if they have been setup.
        updateUI();
        updatePreviewImage();
    }

    public void updateUI() {
        if (this.staticUISetup) {
            PSXPolygonType polygonType = this.shadeDefinition != null ? this.shadeDefinition.getPolygonType() : null;
            this.polygonTypeNameLabel.setText(polygonType != null ? polygonType.name() : "None");
            this.polygonTypeIsQuadCheckBox.setSelected(polygonType != null && polygonType.isQuad());
            this.polygonTypeGouraudCheckBox.setSelected(polygonType != null && polygonType.isGouraud());
            this.polygonTypeTexturedCheckBox.setSelected(polygonType != null && polygonType.isTextured());
        }

        // Refresh the remaining 2D UI elements.
        if (this.dynamicEditorGrid != null)
            setupDynamicUI(this.dynamicEditorGrid, this.editorController);
    }

    private void onTextureChangeReceived(BufferedImage newImage) {
        if (shouldHandleUIChanges())
            onTextureChange(newImage);
    }

    /**
     * Called when the texture changes.
     * @param newImage the new raw image to use.
     */
    protected void onTextureChange(BufferedImage newImage) {
        updatePreviewImage(newImage);
    }

    /**
     * Updates the preview image.
     */
    public void updatePreviewImage() {
        if (this.shadeDefinition != null) {
            updatePreviewImage(this.shadeDefinition.makeImage());
        } else {
            this.previewImageView.setImage(null);
        }
    }

    protected void updatePreviewImage(BufferedImage newShadedImage) {
        if (this.shadeDefinition != null && newShadedImage != null) {
            this.previewImageView.setImage(Utils.toFXImage(newShadedImage, false));
        } else {
            this.previewImageView.setImage(null);
        }
    }

    /**
     * Signals this object is no longer used.
     */
    public void dispose() {
        setShadeDefinition(null); // Cleanup shade definition listeners.
        this.dynamicEditorGrid = null;
        this.editorController = null;
    }

    /**
     * Setup the UI for the shading editor.
     * @param grid the grid used to create the editor
     */
    public void setupStaticUI(GUIEditorGrid grid) {
        if (this.staticUISetup)
            throw new IllegalStateException("The static UI has already been setup!");
        this.staticUISetup = true;

        if (this.registerImageView)
            grid.addCenteredImageView(this.previewImageView);

        // Polygon type information.
        this.polygonTypeNameLabel = grid.addLabel("Polygon Type:", "None");
        this.polygonTypeIsQuadCheckBox = grid.addCheckBox("Is a Quad?", false, null);
        this.polygonTypeGouraudCheckBox = grid.addCheckBox("Gouraud Shading", false, null);
        this.polygonTypeTexturedCheckBox = grid.addCheckBox("Has Texture", false, null);
        this.polygonTypeIsQuadCheckBox.setDisable(true);
        this.polygonTypeGouraudCheckBox.setDisable(true);
        this.polygonTypeTexturedCheckBox.setDisable(true);
    }

    /**
     * Creates the dynamic UI in the provided grid.
     * @param grid the grid to create the dynamic UI inside
     * @param controller the mesh UI controller
     */
    public void setupDynamicUI(GUIEditorGrid grid, MeshViewController<?> controller) {
        this.dynamicEditorGrid = grid;
        this.editorController = controller;
        this.dynamicEditorGrid.clearEditor();
        if (this.shadeDefinition == null)
            return;

        String[] names = this.shadeDefinition.getVertexNames();

        // Setup color editors.
        int colorCount = this.shadeDefinition.getColors().length;
        for (int i = 0; i < this.shadeDefinition.getColors().length; i++) {
            final int index = i;
            CVector color = this.shadeDefinition.getColors()[i];
            Runnable colorChangeListener = () -> this.onColorUpdateReceived(index);
            String label = (colorCount > 1 ? names[i] : "Flat") + " Color:";

            if (this.shadeDefinition.isModulated()) {
                color.setupModulatedEditor(grid, label, colorChangeListener);
            } else {
                color.setupUnmodulatedEditor(grid, label, null, colorChangeListener);
            }
        }

        // Setup UV editors.
        if (this.shadeDefinition.isTextured()) {
            for (int i = 0; i < this.shadeDefinition.getTextureUVs().length; i++) {
                final int index = i;
                SCByteTextureUV uv = this.shadeDefinition.getTextureUVs()[i];
                uv.setupEditor(names[i] + " UV:", grid, () -> this.onTextureUvUpdateReceived(index));
            }
        }
    }

    /**
     * Checks if UI changes should be handled.
     * Mostly a safeguard against unsafe cleansing.
     */
    protected boolean shouldHandleUIChanges() {
        return this.shadeDefinition != null;
    }

    private void onColorUpdateReceived(int colorIndex) {
        if (shouldHandleUIChanges()) {
            onColorUpdate(colorIndex, this.shadeDefinition.getColors()[colorIndex]);
            this.shadeDefinition.fireChangeEvent();
        }
    }

    private void onTextureUvUpdateReceived(int uvIndex) {
        if (shouldHandleUIChanges()) {
            onTextureUvUpdate(uvIndex, this.shadeDefinition.getTextureUVs()[uvIndex]);
            this.shadeDefinition.fireChangeEvent();
        }
    }

    /**
     * Called when the color is updated.
     */
    protected abstract void onColorUpdate(int colorIndex, CVector color);

    /**
     * Called when the texture UV is updated.
     */
    protected abstract void onTextureUvUpdate(int uvIndex, SCByteTextureUV uv);
}