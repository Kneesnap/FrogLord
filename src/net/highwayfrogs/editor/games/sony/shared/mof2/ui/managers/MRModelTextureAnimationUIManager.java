package net.highwayfrogs.editor.games.sony.shared.mof2.ui.managers;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.scene.Node;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.util.Duration;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.highwayfrogs.editor.file.map.view.CursorVertexColor;
import net.highwayfrogs.editor.file.vlo.GameImage;
import net.highwayfrogs.editor.file.vlo.ImageFilterSettings;
import net.highwayfrogs.editor.file.vlo.ImageFilterSettings.ImageState;
import net.highwayfrogs.editor.file.vlo.VLOArchive;
import net.highwayfrogs.editor.games.sony.SCGameInstance;
import net.highwayfrogs.editor.games.sony.shared.mof2.animation.texture.MRMofTextureAnimation;
import net.highwayfrogs.editor.games.sony.shared.mof2.animation.texture.MRMofTextureAnimationEntry;
import net.highwayfrogs.editor.games.sony.shared.mof2.animation.texture.MRMofTextureAnimationPolygonTarget;
import net.highwayfrogs.editor.games.sony.shared.mof2.mesh.MRMofPart;
import net.highwayfrogs.editor.games.sony.shared.mof2.mesh.MRMofPolygon;
import net.highwayfrogs.editor.games.sony.shared.mof2.mesh.MRStaticMof;
import net.highwayfrogs.editor.games.sony.shared.mof2.ui.MRModelMeshController;
import net.highwayfrogs.editor.games.sony.shared.mof2.ui.managers.MRModelTextureAnimationUIManager.MRModelTextureAnimationPreview;
import net.highwayfrogs.editor.games.sony.shared.mof2.ui.mesh.MRModelMesh;
import net.highwayfrogs.editor.games.sony.shared.mwd.MWDFile;
import net.highwayfrogs.editor.gui.GUIEditorGrid;
import net.highwayfrogs.editor.gui.editor.BasicListMeshUIManager;
import net.highwayfrogs.editor.gui.mesh.DynamicMeshDataEntry;
import net.highwayfrogs.editor.utils.FXUtils;
import net.highwayfrogs.editor.utils.NumberUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Manages UI for MRModel texture animations.
 * Created by Kneesnap on 5/8/2025.
 */
public class MRModelTextureAnimationUIManager extends BasicListMeshUIManager<MRModelMesh, MRMofTextureAnimation, MRModelTextureAnimationPreview> {
    private final List<MRMofTextureAnimation> cachedTextureAnimations = new ArrayList<>();

    public static final CursorVertexColor ANIMATION_COLOR = new CursorVertexColor(java.awt.Color.MAGENTA, java.awt.Color.BLACK);
    public static final ImageFilterSettings PREVIEW_SETTINGS = new ImageFilterSettings(ImageState.EXPORT).setTrimEdges(true);

    public MRModelTextureAnimationUIManager(MRModelMeshController controller) {
        super(controller);
    }

    @Override
    public MRModelMeshController getController() {
        return (MRModelMeshController) super.getController();
    }

    @Override
    public SCGameInstance getGameInstance() {
        return (SCGameInstance) super.getGameInstance();
    }

    @Override
    public String getTitle() {
        return "Texture Animations";
    }

    @Override
    public String getValueName() {
        return "Animation";
    }

    @Override
    public List<MRMofTextureAnimation> getValues() {
        this.cachedTextureAnimations.clear();
        MRStaticMof staticMof = getController().getActiveStaticMof();
        if (staticMof != null) {

            for (int i = 0; i < staticMof.getParts().size(); i++) {
                MRMofPart mofPart = staticMof.getParts().get(i);
                if (mofPart.getTextureAnimations().size() > 0)
                    this.cachedTextureAnimations.addAll(mofPart.getTextureAnimations());
            }
        }

        return this.cachedTextureAnimations;
    }

    @Override
    protected MRModelTextureAnimationPreview setupDisplay(MRMofTextureAnimation textureAnimation) {
        return new MRModelTextureAnimationPreview(this, textureAnimation);
    }

    @Override
    protected void updateEditor(MRMofTextureAnimation textureAnimation) {
        GUIEditorGrid grid = getEditorGrid();
        // TODO: Have a way to add/remove polygons. (Verify their part is okay.)

        // Setup preview.
        // Create the animation preview.
        List<Node> toDisable = new ArrayList<>();
        if (textureAnimation.getEntries().size() > 0) {
            List<MRMofTextureAnimationEntry> entries = textureAnimation.getEntries();
            MWDFile mwd = getGameInstance().getMainArchive();

            grid.addBoldLabel("Preview:");
            AtomicBoolean isAnimating = new AtomicBoolean(false);
            AtomicInteger framesWaited = new AtomicInteger(0);
            int maxValidFrame = entries.size() - 1;
            ImageView imagePreview = grid.addCenteredImage(mwd.getImageByTextureId(entries.get(0).getGlobalImageId()).toFXImage(PREVIEW_SETTINGS), 150);
            Slider frameSlider = grid.addIntegerSlider("Animation Frame", 0, newFrame ->
                    imagePreview.setImage(mwd.getImageByTextureId(entries.get(newFrame).getGlobalImageId()).toFXImage(PREVIEW_SETTINGS)), 0, maxValidFrame);

            toDisable.add(frameSlider);
            double millisInterval = (1000D / getGameInstance().getFPS());
            Timeline animationTimeline = new Timeline(new KeyFrame(Duration.millis(millisInterval), evt -> {
                if (framesWaited.getAndIncrement() == entries.get((int) frameSlider.getValue()).getDuration()) {
                    framesWaited.set(0);
                } else {
                    return;
                }

                int i = (int) frameSlider.getValue() + 1;
                if (i == maxValidFrame + 1)
                    i = 0;
                frameSlider.setValue(i);
            }));
            animationTimeline.setCycleCount(Timeline.INDEFINITE);

            imagePreview.setOnMouseClicked(evt -> {
                isAnimating.set(!isAnimating.get());
                boolean playNow = isAnimating.get();
                if (playNow) {
                    animationTimeline.play();
                } else {
                    animationTimeline.pause();
                }

                for (Node node : toDisable)
                    node.setDisable(playNow);
            });
        }

        // Setup editor.
        grid.addBoldLabel("Textures:");
        for (int i = 0; i < textureAnimation.getEntries().size(); i++) {
            final int tempIndex = i;
            MRMofTextureAnimationEntry entry = textureAnimation.getEntries().get(i);
            GameImage image = getGameInstance().getMainArchive().getImageByTextureId(entry.getGlobalImageId());
            Image scaledImage = FXUtils.toFXImage(image.toBufferedImage(VLOArchive.ICON_EXPORT), true);
            ImageView view = new ImageView(scaledImage);
            view.setFitWidth(20);
            view.setFitHeight(20);

            view.setOnMouseClicked(evt -> {
                VLOArchive vlo = getMesh().getModel().getVloFile();
                if (vlo == null) {
                    FXUtils.makePopUp("There is no VLO file associated with this model.\nSo, FrogLord doesn't know what textures it should let you pick from.", AlertType.WARNING);
                    return;
                }

                vlo.promptImageSelection(newImage -> {
                    entry.setGlobalImageId(newImage.getTextureId());
                    updateEditor();
                }, false);
            });

            HBox hbox = new HBox();
            hbox.setSpacing(5);
            hbox.getChildren().add(view);
            hbox.getChildren().add(new Label("Duration:"));
            grid.setupNode(hbox);

            HBox hbox2 = new HBox();

            TextField durationField = grid.setupSecondNode(new TextField(String.valueOf(entry.getDuration())), false);
            FXUtils.setHandleTestKeyPress(durationField, NumberUtils::isInteger, newValue -> {
                entry.setDuration(Integer.parseInt(newValue));
                updateEditor();
            });
            durationField.setMaxWidth(30);

            Button removeButton = new Button("Remove");
            removeButton.setOnAction(evt -> {
                textureAnimation.getEntries().remove(tempIndex);
                updateEditor();
            });

            hbox2.setSpacing(40);
            hbox2.getChildren().add(durationField);
            hbox2.getChildren().add(removeButton);
            grid.setupSecondNode(hbox2, false);
            grid.addRow(25);

            toDisable.add(view);
            toDisable.add(durationField);
            toDisable.add(removeButton);
        }

        toDisable.add(grid.addButton("Add Texture", () -> {
            VLOArchive vlo = getMesh().getModel().getVloFile();
            if (vlo == null) {
                FXUtils.makePopUp("There is no VLO file associated with this model.\nSo, FrogLord doesn't know what textures it should let you pick from.", AlertType.WARNING);
                return;
            }

            vlo.promptImageSelection(newImage -> {
                textureAnimation.getEntries().add(new MRMofTextureAnimationEntry(newImage.getTextureId(), 1));
                updateEditor();
            }, false);
        }));
    }

    @Override
    protected void setVisible(MRMofTextureAnimation textureAnimation, MRModelTextureAnimationPreview preview, boolean visible) {
        if (preview != null) {
            if (visible) {
                preview.createPreview();
            } else {
                preview.removePreview();
            }
        }
    }

    @Override
    protected void onSelectedValueChange(MRMofTextureAnimation oldValue, MRModelTextureAnimationPreview oldPreview, MRMofTextureAnimation newValue, MRModelTextureAnimationPreview newPreview) {
        // Do nothing.
    }

    @Override
    protected MRMofTextureAnimation createNewValue() {
        MRStaticMof staticMof = getController().getActiveStaticMof();
        MRMofPart mofPart = staticMof != null && staticMof.getParts().size() > 0 ? staticMof.getParts().get(0) : null; // TODO: allow choosing the part instead, via selection menu?
        return mofPart != null ? new MRMofTextureAnimation(mofPart) : null;
    }

    @Override
    protected boolean tryAddValue(MRMofTextureAnimation animation) {
        return animation != null && animation.getParentPart().addTextureAnimation(animation);
    }

    @Override
    protected boolean tryRemoveValue(MRMofTextureAnimation animation) {
        return animation != null && animation.getParentPart().removeTextureAnimation(animation);
    }

    @Override
    protected void onDelegateRemoved(MRMofTextureAnimation textureAnimation, MRModelTextureAnimationPreview preview) {
        preview.removePreview();
    }

    @Override
    protected String getListDisplayName(int index, MRMofTextureAnimation textureAnimation) {
        if (textureAnimation != null) {
            return super.getListDisplayName(index, textureAnimation) + ", Part " + textureAnimation.getParentPart().getPartID();
        } else {
            return super.getListDisplayName(index, null);
        }
    }

    @Override
    protected Image getListDisplayImage(int index, MRMofTextureAnimation textureAnimation) {
        if (textureAnimation == null || textureAnimation.getEntries().isEmpty())
            return null;

        MRMofTextureAnimationEntry entry = textureAnimation.getEntries().get(0);
        GameImage gameImage = getGameInstance().getMainArchive().getImageByTextureId(entry.getGlobalImageId());
        return gameImage != null ? FXUtils.toFXImage(gameImage.toBufferedImage(), true) : null;
    }

    @RequiredArgsConstructor
    public static class MRModelTextureAnimationPreview {
        @Getter private final MRModelTextureAnimationUIManager uiManager;
        @Getter private final MRMofTextureAnimation textureAnimation;
        private final List<MRMofPolygon> highlightedPolygons = new ArrayList<>();

        /**
         * Creates the preview containing the highlighted polygons.
         */
        public void createPreview() {
            MRModelMesh mesh = this.uiManager.getMesh();
            mesh.pushBatchOperations();

            removePreview();
            MRMofPart mofPart = this.textureAnimation.getParentPart();
            for (int i = 0; i < mofPart.getTextureAnimationPolygonTargets().size(); i++) {
                MRMofTextureAnimationPolygonTarget target = mofPart.getTextureAnimationPolygonTargets().get(i);
                if (target.getAnimation() == this.textureAnimation) {
                    DynamicMeshDataEntry dataEntry = mesh.getPolygonDataEntry(target.getPolygon());
                    if (dataEntry != null) {
                        mesh.getHighlightedAnimatedPolygonsNode().setOverlayTexture(dataEntry, ANIMATION_COLOR);
                        this.highlightedPolygons.add(target.getPolygon());
                    }
                }
            }

            mesh.popBatchOperations();
        }

        /**
         * Removes the active preview.
         */
        public void removePreview() {
            MRModelMesh mesh = this.uiManager.getMesh();
            mesh.pushBatchOperations();
            for (int i = 0; i < this.highlightedPolygons.size(); i++) {
                DynamicMeshDataEntry dataEntry = mesh.getPolygonDataEntry(this.highlightedPolygons.get(i));
                if (dataEntry != null)
                    mesh.getHighlightedAnimatedPolygonsNode().setOverlayTexture(dataEntry, null);
            }

            // Clear polygons.
            this.highlightedPolygons.clear();
            mesh.popBatchOperations();
        }
    }
}
