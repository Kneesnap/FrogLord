package net.highwayfrogs.editor.gui.editor.map.manager;

import javafx.scene.control.ComboBox;
import javafx.scene.input.MouseEvent;
import lombok.Getter;
import net.highwayfrogs.editor.file.MWDFile;
import net.highwayfrogs.editor.file.map.animation.MAPAnimation;
import net.highwayfrogs.editor.file.map.animation.MAPUVInfo;
import net.highwayfrogs.editor.file.map.poly.polygon.MAPPolyTexture;
import net.highwayfrogs.editor.file.map.poly.polygon.MAPPolygon;
import net.highwayfrogs.editor.file.map.view.MapMesh;
import net.highwayfrogs.editor.file.vlo.GameImage;
import net.highwayfrogs.editor.gui.GUIEditorGrid;
import net.highwayfrogs.editor.gui.SelectionMenu.AttachmentListCell;
import net.highwayfrogs.editor.gui.editor.MapUIController;
import net.highwayfrogs.editor.gui.mesh.MeshData;
import net.highwayfrogs.editor.system.AbstractStringConverter;

/**
 * Manages map animations.
 * Created by Kneesnap on 8/20/2019.
 */
@Getter
public class AnimationManager extends MapManager {
    private GUIEditorGrid animationEditor;
    private MAPAnimation editAnimation;
    private MeshData animationMarker;
    private MAPAnimation selectedAnimation;

    public AnimationManager(MapUIController controller) {
        super(controller);
    }

    @Override
    public void setupEditor() {
        if (animationEditor == null)
            animationEditor = new GUIEditorGrid(getController().getAnimationGridPane());

        animationEditor.clearEditor();

        ComboBox<MAPAnimation> box = this.animationEditor.addSelectionBox("Animation:", getSelectedAnimation(), getMap().getMapAnimations(), newAnim -> {
            this.selectedAnimation = newAnim;
            setupEditor();
        });
        box.setConverter(new AbstractStringConverter<>(anim -> "Animation #" + getMap().getMapAnimations().indexOf(anim)));
        box.setCellFactory(param -> new AttachmentListCell<>(anim -> "Animation #" + getMap().getMapAnimations().indexOf(anim), anim -> {
            if (getMap().getRemapTable() == null)
                return null;

            Short textureId;
            if (anim.getTextures().size() > 0) {
                textureId = anim.getTextures().get(0);
            } else if (anim.getMapUVs().size() > 0) {
                textureId = ((MAPPolyTexture) anim.getMapUVs().get(0).getPolygon()).getTextureId();
            } else {
                // TODO: New froglord should have different textures returned. One for if there's no remap, one for if it's not in the remap, etc.
                return null;
            }

            Short realTextureId = getMap().getRemapTable().size() > textureId ? getMap().getRemapTable().get(textureId) : null;
            if (realTextureId != null) {
                GameImage gameImage = getMap().getVlo().getImageByTextureId(realTextureId);
                if (gameImage != null)
                    return gameImage.toFXImage(MWDFile.VLO_ICON_SETTING.setTrimEdges(true));
            }

            return null;
        }));

        if (this.selectedAnimation != null) {
            this.animationEditor.addBoldLabelButton("Animation #" + getMap().getMapAnimations().indexOf(this.selectedAnimation) + ":", "Remove", 25, () -> {
                getMap().getMapAnimations().remove(this.selectedAnimation);
                this.selectedAnimation = null;
                setupEditor(); // Reload this.
            });

            this.selectedAnimation.setupEditor(this, this.animationEditor);
        }

        this.animationEditor.addSeparator(25);
        this.animationEditor.addButton("Add Animation", () -> {
            getMap().getMapAnimations().add(this.selectedAnimation = new MAPAnimation(getMap()));
            setupEditor();
        });
    }

    @Override
    public boolean handleClick(MouseEvent event, MAPPolygon clickedPolygon) {
        if (isAnimationMode()) {
            boolean removed = this.editAnimation.getMapUVs().removeIf(uvInfo -> uvInfo.getPolygon().equals(clickedPolygon));
            if (!removed)
                this.editAnimation.getMapUVs().add(new MAPUVInfo(getMap(), clickedPolygon));

            updateAnimation();
            return true;
        }

        return super.handleClick(event, clickedPolygon);
    }

    /**
     * Start editing an animation.
     * @param animation The animation to edit.
     */
    public void editAnimation(MAPAnimation animation) {
        boolean match = animation.equals(this.editAnimation);
        cancelAnimationEdit();
        if (match)
            return;

        this.editAnimation = animation;
        animation.getMapUVs().forEach(uvInfo -> uvInfo.writeOver(getController(), MapMesh.ANIMATION_COLOR));
        this.animationMarker = getMesh().getManager().addMesh();
    }

    /**
     * Test if animation edit mode is active.
     * @return animationMode
     */
    public boolean isAnimationMode() {
        return this.editAnimation != null && this.animationMarker != null;
    }

    /**
     * Update animation data.
     */
    public void updateAnimation() {
        if (!isAnimationMode())
            return;

        getMesh().getManager().removeMesh(this.animationMarker);
        this.editAnimation.getMapUVs().forEach(uvInfo -> uvInfo.writeOver(getController(), MapMesh.ANIMATION_COLOR));
        this.animationMarker = getMesh().getManager().addMesh();
    }

    /**
     * Stop the current animation edit.
     */
    public void cancelAnimationEdit() {
        if (!isAnimationMode())
            return;

        getMesh().getManager().removeMesh(getAnimationMarker());
        this.animationMarker = null;
        this.editAnimation = null;
    }
}