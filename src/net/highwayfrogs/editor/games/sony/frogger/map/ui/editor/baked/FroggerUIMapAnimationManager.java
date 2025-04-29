package net.highwayfrogs.editor.games.sony.frogger.map.ui.editor.baked;


import javafx.scene.control.CheckBox;
import javafx.scene.control.Slider;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import lombok.Getter;
import net.highwayfrogs.editor.file.map.view.RawColorTextureSource;
import net.highwayfrogs.editor.file.map.view.UnknownTextureSource;
import net.highwayfrogs.editor.file.vlo.GameImage;
import net.highwayfrogs.editor.file.vlo.VLOArchive;
import net.highwayfrogs.editor.games.sony.frogger.FroggerGameInstance;
import net.highwayfrogs.editor.games.sony.frogger.map.data.animation.FroggerMapAnimation;
import net.highwayfrogs.editor.games.sony.frogger.map.data.animation.FroggerMapAnimationTargetPolygon;
import net.highwayfrogs.editor.games.sony.frogger.map.mesh.FroggerMapMesh;
import net.highwayfrogs.editor.games.sony.frogger.map.mesh.FroggerMapPolygon;
import net.highwayfrogs.editor.games.sony.frogger.map.ui.editor.baked.FroggerBakedUIManager.FroggerBakedMapListManager;
import net.highwayfrogs.editor.games.sony.shared.TextureRemapArray;
import net.highwayfrogs.editor.games.sony.shared.mwd.MWDFile;
import net.highwayfrogs.editor.gui.editor.MeshViewController;
import net.highwayfrogs.editor.gui.editor.MeshViewFrameTimer.MeshViewFixedFrameRateTimerTask;
import net.highwayfrogs.editor.gui.editor.UISidePanel;
import net.highwayfrogs.editor.gui.mesh.DynamicMeshOverlayNode.OverlayTarget;
import net.highwayfrogs.editor.utils.FXUtils;

import java.awt.image.BufferedImage;
import java.util.List;

/**
 * Allows viewing/editing map animations.
 * SUB2/CAV4/JUN1 is spending a MASSIVE amount of time on "com.sun.javafx.tk.quantum.GlassScene.waitForRenderingToComplete" when map animations are previewed.
 *  -> A large chunk of this is just sending the updated texture to the GPU, which likely can't be avoided. The slowdown hits these levels hard since they have 4096x4096 textures. (D3DTexture.update).
 * Created by Kneesnap on 5/28/2024.
 */
public class FroggerUIMapAnimationManager extends FroggerBakedMapListManager<FroggerMapAnimation, Void> {
    private CheckBox showAnimationsOnMapCheckBox;
    @Getter private CheckBox editAnimationPolygonTargetsCheckBox;
    private ImageView animationPreview;
    private Slider animationPreviewFrameSlider;
    private boolean animationPreviewRunning;

    private static final double DEFAULT_IMAGE_PREVIEW_DIMENSIONS = 150;
    public static final RawColorTextureSource MATERIAL_POLYGON_HIGHLIGHT = new RawColorTextureSource(javafx.scene.paint.Color.rgb(57, 255, 20, .333F));

    public FroggerUIMapAnimationManager(MeshViewController<FroggerMapMesh> controller) {
        super(controller);
    }

    @Override
    public FroggerGameInstance getGameInstance() {
        return (FroggerGameInstance) super.getGameInstance();
    }

    @Override
    public void onSetup() {
        super.onSetup();
        getFrameTimer(getGameInstance().getFPS()).addTask(1, this, FroggerUIMapAnimationManager::updatePolygonsWithAnimations);
    }

    @Override
    protected void setupMainGridEditor(UISidePanel sidePanel) {
        this.showAnimationsOnMapCheckBox = getMainGrid().addCheckBox("Preview Animations on Map", false, newState -> {
            if (!newState)
                getMesh().getMainNode().clearMapAnimations();
        });
        this.editAnimationPolygonTargetsCheckBox = getMainGrid().addCheckBox("Apply animation to clicked polygons.", false, null);
        super.setupMainGridEditor(sidePanel);
        createAnimationPreviewUI();
        updatePreviewImage();
    }

    private void createAnimationPreviewUI() {
        getMainGrid().addBoldLabel("Preview (Click to animate):");
        this.animationPreview = getMainGrid().addCenteredImage(null, DEFAULT_IMAGE_PREVIEW_DIMENSIONS);
        this.animationPreview.setDisable(true);

        this.animationPreviewFrameSlider = getMainGrid().addIntegerSlider("Animation Frame", 0,
                newFrame -> updatePreviewImage(), 0, 1);
        this.animationPreviewFrameSlider.setDisable(true);

        this.animationPreview.setOnMouseClicked(evt -> {
            this.animationPreviewRunning = !this.animationPreviewRunning;
            updatePreviewUI(); // Update if the slider is usable or not.
            updatePreviewImage();
        });
    }

    @Override
    protected Image getListDisplayImage(int index, FroggerMapAnimation animation) {
        if (animation == null)
            return null;

        TextureRemapArray remap = getMap().getTextureRemap();
        if (remap == null)
            return FXUtils.toFXImage(UnknownTextureSource.CYAN_INSTANCE.makeImage(), true);

        Short textureId;
        if (animation.getTextureIds().size() > 0) {
            textureId = animation.getTextureIds().get(0);
        } else if (animation.getTargetPolygons().size() > 0 && animation.getTargetPolygons().get(0).getPolygon() != null) {
            textureId = animation.getTargetPolygons().get(0).getPolygon().getTextureId();
        } else {
            // No texture set.
            return FXUtils.toFXImage(UnknownTextureSource.MAGENTA_INSTANCE.makeImage(), true);
        }

        // Find remapped texture.
        Short realTextureId = remap.getRemappedTextureId(textureId);
        if (realTextureId != null) {
            VLOArchive vloFile = getMap().getVloFile();
            GameImage gameImage = vloFile != null ? vloFile.getImageByTextureId(realTextureId) : null;
            if (gameImage != null)
                return gameImage.toFXImage(MWDFile.VLO_ICON_SETTING.setTrimEdges(true));
        }

        // The texture couldn't be found.
        return FXUtils.toFXImage(UnknownTextureSource.CYAN_INSTANCE.makeImage(), true);
    }

    @Override
    public String getTitle() {
        return "Map Animations";
    }

    @Override
    public String getValueName() {
        return "Animation";
    }

    @Override
    public List<FroggerMapAnimation> getValues() {
        return getMap().getAnimationPacket().getAnimations();
    }

    @Override
    protected Void setupDisplay(FroggerMapAnimation animation) {
        return null;
    }

    @Override
    protected void updateEditor(FroggerMapAnimation animation) {
        animation.setupEditor(this, getEditorGrid());
    }

    @Override
    protected void setVisible(FroggerMapAnimation froggerMapAnimation, Void preview, boolean visible) {
        updateAnimatedPolygonHighlighting();
    }

    @Override
    public void updateEditor() {
        super.updateEditor();
        updatePreviewImage();
    }

    @Override
    protected void onSelectedValueChange(FroggerMapAnimation oldAnimation, Void oldPreview, FroggerMapAnimation newAnimation, Void newPreview) {
        updatePreviewUI();
    }

    @Override
    protected FroggerMapAnimation createNewValue() {
        return new FroggerMapAnimation(getMap());
    }

    @Override
    protected void onDelegateRemoved(FroggerMapAnimation animation, Void preview) {
        // Do nothing for now.
    }

    /**
     * Updates the polygon mesh data for polygons with animations.
     */
    public void updatePolygonsWithAnimations(MeshViewFixedFrameRateTimerTask<?> timerTask) {
        if (this.animationPreviewRunning) {
            FroggerMapAnimation animation = getSelectedValue();
            if (animation != null) {
                int maxFrameCount = animation.getFrameCount();
                if (maxFrameCount > 0) {
                    int nextFrame = ((int) this.animationPreviewFrameSlider.getValue()) + timerTask.getDeltaFrames();
                    if (nextFrame >= this.animationPreviewFrameSlider.getMax())
                        nextFrame %= (int) this.animationPreviewFrameSlider.getMax();

                    this.animationPreviewFrameSlider.setValue(nextFrame);
                }
            }
        }

        if (this.showAnimationsOnMapCheckBox.isSelected())
            getMesh().getMainNode().tickMapAnimations(timerTask.getDeltaFrames());
    }

    /**
     * Updates the animation preview UI.
     */
    public void updatePreviewUI() {
        FroggerMapAnimation animation = getSelectedValue();

        this.animationPreviewFrameSlider.setDisable(true);
        if (animation != null) {
            int newFrameCount = animation.getFrameCount();
            boolean frameCountChanged = Math.abs((this.animationPreviewFrameSlider.getMax() + 1) - newFrameCount) > .01;
            this.animationPreviewFrameSlider.setDisable(this.animationPreviewRunning || (newFrameCount == 0));
            if (frameCountChanged && newFrameCount > 0) {
                this.animationPreviewFrameSlider.setMax(newFrameCount - 1);
                this.animationPreviewFrameSlider.setValue(0);
                this.animationPreviewFrameSlider.setMajorTickUnit(newFrameCount / 4D);
            }
        }
    }

    /**
     * Updates the preview image.
     */
    public void updatePreviewImage() {
        FroggerMapAnimation animation = getSelectedValue();
        if (animation == null) {
            this.animationPreview.setImage(null);
            this.animationPreview.setDisable(true);
            return;
        }

        BufferedImage newImage = animation.getAnimationFrame(null, (int) this.animationPreviewFrameSlider.getValue() - 1, true);
        this.animationPreview.setImage(FXUtils.toFXImage(newImage, false));
        this.animationPreview.setDisable(false);
    }

    /**
     * Update the animated polygon highlighting.
     */
    public void updateAnimatedPolygonHighlighting() {
        getMesh().getHighlightedAnimatedPolygonsNode().clear();

        List<FroggerMapAnimation> animations = getMap().getAnimationPacket().getAnimations();
        for (int i = 0; i < animations.size(); i++) {
            FroggerMapAnimation animation = animations.get(i);
            if (!isValueVisibleByUI(animation))
                continue;

            for (int j = 0; j < animation.getTargetPolygons().size(); j++) {
                FroggerMapAnimationTargetPolygon targetPolygon = animation.getTargetPolygons().get(j);
                FroggerMapPolygon polygon = targetPolygon != null ? targetPolygon.getPolygon() : null;
                if (polygon == null)
                    continue;

                OverlayTarget target = new OverlayTarget(getMesh().getMainNode().getDataEntry(polygon), FroggerUIMapAnimationManager.MATERIAL_POLYGON_HIGHLIGHT);
                getMesh().getHighlightedAnimatedPolygonsNode().add(target);
            }
        }
    }
}