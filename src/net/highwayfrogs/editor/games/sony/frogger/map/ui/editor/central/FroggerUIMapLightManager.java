package net.highwayfrogs.editor.games.sony.frogger.map.ui.editor.central;

import javafx.geometry.Point3D;
import javafx.scene.*;
import javafx.scene.control.CheckBox;
import javafx.scene.image.Image;
import lombok.Getter;
import net.highwayfrogs.editor.games.sony.frogger.map.data.FroggerMapLight;
import net.highwayfrogs.editor.games.sony.frogger.map.mesh.FroggerMapMesh;
import net.highwayfrogs.editor.games.sony.frogger.map.ui.editor.central.FroggerCentralUIManager.FroggerCentralMapListManager;
import net.highwayfrogs.editor.games.sony.frogger.map.ui.editor.central.FroggerUIMapLightManager.FroggerMapLightPreview;
import net.highwayfrogs.editor.games.sony.shared.misc.MRLightType;
import net.highwayfrogs.editor.gui.editor.DisplayList;
import net.highwayfrogs.editor.gui.editor.MeshViewController;
import net.highwayfrogs.editor.gui.editor.UISidePanel;
import net.highwayfrogs.editor.utils.ColorUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Manages map lighting for Frogger.
 * TODO: Go over FroggerMapLight values.
 * TODO: For cone preview, we could just draw lines to the cone edges instead of doing a full cone inside.
 * TODO: For changing the direction of a light, a rotation gizmo is ideal.
 * TODO: We should show the lights in 3D space too? Or at least the ones with 3D data. (3D Preview! I like making a billboard sprite light icon.)
 * TODO: Only the first 3 parallel + point lights can be handled by MRCalculateCustomInstanceLights (entities).
 * TODO: Allow changing the frog custom light here.
 * TODO: Only allow adding a single AMBIENT LIGHT (And added lights should be placed before the final ambient light.
 * Created by Kneesnap on 6/1/2024.
 */
@Getter
public class FroggerUIMapLightManager extends FroggerCentralMapListManager<FroggerMapLight, FroggerMapLightPreview> {
    private final AmbientLight specialFrogletLight = new AmbientLight(ColorUtils.fromRGB(0xA0A0A0));
    private final List<AmbientLight> regularAmbientLights = new ArrayList<>();
    private CheckBox applyLightingToEntitiesCheckBox;
    private DisplayList lightList;

    public FroggerUIMapLightManager(MeshViewController<FroggerMapMesh> controller) {
        super(controller);
    }

    @Override
    public void onSetup() {
        this.lightList = getRenderManager().createDisplayList();
        this.lightList.add(this.specialFrogletLight);
        super.onSetup();
        updateEntityLighting();
    }

    @Override
    protected void setupMainGridEditor(UISidePanel sidePanel) {
        super.setupMainGridEditor(sidePanel);
        this.applyLightingToEntitiesCheckBox = getMainGrid().addCheckBox("Apply Lighting to Entities", true, newValue -> updateEntityLighting());
        getValueDisplaySetting().setValue(ListDisplayType.ALL); // All lights should show by default.
    }

    @Override
    protected Image getListDisplayImage(int index, FroggerMapLight light) {
        FroggerMapLightPreview preview = getDelegatesByValue().get(light);
        return preview != null ? preview.getColorPreviewImage() : null;
    }


    @Override
    public String getTitle() {
        return "Lighting";
    }

    @Override
    public String getValueName() {
        return "Light";
    }

    @Override
    public List<FroggerMapLight> getValues() {
        return getMap().getLightPacket().getLights();
    }

    @Override
    protected FroggerMapLightPreview setupDisplay(FroggerMapLight light) {
        FroggerMapLightPreview preview = new FroggerMapLightPreview(this, light);
        preview.updateLight();
        return preview;
    }

    @Override
    protected void updateEditor(FroggerMapLight light) {
        light.makeEditor(getEditorGrid(), this, getDelegatesByValue().get(light));
    }

    @Override
    protected void setVisible(FroggerMapLight light, FroggerMapLightPreview lightPreview, boolean visible) {
        if (lightPreview != null)
            lightPreview.setVisible(visible);
    }

    @Override
    public boolean isValueVisibleByUI(FroggerMapLight light) {
        return super.isValueVisibleByUI(light) && isLightingAppliedToEntities();
    }

    @Override
    protected void onSelectedValueChange(FroggerMapLight oldLight, FroggerMapLightPreview oldLightPreview, FroggerMapLight newLight, FroggerMapLightPreview newLightPreview) {
        // TODO: Position in 3D space display?
    }

    @Override
    protected FroggerMapLight createNewValue() {
        return new FroggerMapLight(getMap(), MRLightType.AMBIENT);
    }

    @Override
    protected void onDelegateRemoved(FroggerMapLight light, FroggerMapLightPreview lightPreview) {
        lightPreview.onRemove();
    }

    private Group getEntityGroup() {
        return getController().getEntityManager().getLitEntityRenderList().getRoot();
    }

    /**
     * Updates whether lighting is applied to entities.
     */
    public void updateEntityLighting() {
        if (isLightingAppliedToEntities()) {
            getController().getMainLight().getScope().remove(getEntityGroup());
        } else {
            addToScope(getController().getMainLight(), getEntityGroup());
        }

        getController().getGeneralManager().updatePlayerCharacterLighting();
        updateValueVisibility();
    }

    /**
     * Updates the light provided (Or creates a new one) to match the data provided by the FroggerMapLight.
     * @param light The map light to apply to the 3D light preview.
     * @param fxLight The 3D light preview to update.
     * @return updatedLightObject
     */
    public LightBase updateLight(FroggerMapLight light, LightBase fxLight) {
        switch (light.getLightType()) {
            case AMBIENT:
                AmbientLight ambLight = fxLight instanceof AmbientLight ? (AmbientLight) fxLight : new AmbientLight();
                ambLight.setColor(ColorUtils.fromBGR(light.getColor()));
                fxLight = ambLight;
                break;

            case PARALLEL:
                // The way the lighting is applied looks different in-game slightly, as it applies to the polygon in-game,
                // where in FrogLord it looks more like a fragment shader I think. But, I think this is about as close to accurate as we can expect.
                DirectionalLight parallelLight = fxLight instanceof DirectionalLight ? (DirectionalLight) fxLight : new DirectionalLight();
                parallelLight.setColor(ColorUtils.fromBGR(light.getColor()));
                parallelLight.setDirection(new Point3D(light.getDirection().getFloatX(12), light.getDirection().getFloatY(12), light.getDirection().getFloatZ(12)));
                fxLight = parallelLight;
                break;

            case POINT: // Point lights are not used.
                PointLight pointLight = fxLight instanceof PointLight ? (PointLight) fxLight : new PointLight();
                pointLight.setColor(ColorUtils.fromBGR(light.getColor()));
                // Assuming direction is position? Are POINT lights ever used? [AndyEder]
                pointLight.setTranslateX(light.getDirection().getFloatX());
                pointLight.setTranslateY(light.getDirection().getFloatY());
                pointLight.setTranslateZ(light.getDirection().getFloatZ());
                // TODO: How do we set the intensity / falloff?
                // TODO: Newer versions of JAvaFX (16+) have attenuation constants which we can use to control the distance, https://openjfx.io/javadoc/17/javafx.graphics/javafx/scene/PointLight.html
                fxLight = pointLight;
                break;
        }

        return fxLight;
    }

    /**
     * Applies ambient lighting to the given node.
     * @param node The node to apply to ambient lighting to
     * @param useSpecialLighting If true, the special froglet lighting will be applied
     */
    public void setAmbientLightingMode(Node node, boolean useSpecialLighting) {
        FroggerUIMapGeneralManager generalManager = getController().getGeneralManager();
        AmbientLight specialLight = (generalManager.getPlayerCharacterView() == node) ? generalManager.getFrogLight() : this.specialFrogletLight;

        // Checkpoints have special lighting applied by ent_gen.c.
        if (isLightingAppliedToEntities() && useSpecialLighting) { // 'CHECKPOINT' is valid starting from PSX Alpha to retail.
            // Remove regular ambient lights from the frog.
            for (int i = 0; i < this.regularAmbientLights.size(); i++)
                removeFromScope(this.regularAmbientLights.get(i), node);

            // Apply special froglet ambient light.
            addToScope(specialLight, node);
        } else {
            // Remove special froglet ambient light.
            removeFromScope(specialLight, node);

            // Add regular ambient lights to the node.
            for (int i = 0; i < this.regularAmbientLights.size(); i++)
                addToScope(this.regularAmbientLights.get(i), node);
        }
    }

    private static void addToScope(LightBase light, Node node) {
        if (!light.getScope().contains(node)) {
            light.getScope().add(node);
            light.setLightOn(true); // If we always keep it visible, it'll be active when the scope is empty, which to JavaFX means "apply everywhere", but to us means "apply nowhere"
        }
    }

    private static void removeFromScope(LightBase light, Node node) {
        if (light.getScope().remove(node) && light.getScope().isEmpty())
            light.setLightOn(false); // If we always keep it visible, it'll be active when the scope is empty, which to JavaFX means "apply everywhere", but to us means "apply nowhere"
    }

    /**
     * Remove the given node from all ambient lights which might explicitly include the given node in their scope.
     * @param node the node to remove.
     */
    public void clearAmbientLighting(Node node) {
        // Remove regular ambient lights from the frog.
        for (int i = 0; i < this.regularAmbientLights.size(); i++)
            removeFromScope(this.regularAmbientLights.get(i), node);

        // Remove special froglet ambient light.
        removeFromScope(this.specialFrogletLight, node);
    }

    /**
     * Tests if lighting is applied to entities.
     */
    public boolean isLightingAppliedToEntities() {
        return this.applyLightingToEntitiesCheckBox == null || this.applyLightingToEntitiesCheckBox.isSelected();
    }

    public static class FroggerMapLightPreview {
        @Getter private final FroggerUIMapLightManager lightingManager;
        @Getter private final FroggerMapLight mapLight;
        @Getter private LightBase fxLight;
        @Getter private boolean visible;
        private Image colorPreviewImage;
        private int lastColorPreviewColorValue;

        public FroggerMapLightPreview(FroggerUIMapLightManager lightingManager, FroggerMapLight mapLight) {
            this.mapLight = mapLight;
            this.lightingManager = lightingManager;
        }

        /**
         * Gets the color image preview.
         */
        public Image getColorPreviewImage() {
            if (this.lastColorPreviewColorValue == this.mapLight.getColor() && this.colorPreviewImage != null)
                return this.colorPreviewImage;

            this.lastColorPreviewColorValue = this.mapLight.getColor();
            return this.colorPreviewImage = ColorUtils.makeColorImageNoCache(ColorUtils.fromBGR(this.mapLight.getColor()), 8, 8);
        }

        /**
         * Sets whether the fx light is visible
         * @param visible the visibility state to apply
         */
        public void setVisible(boolean visible) {
            this.visible = visible;
            if (this.fxLight == null)
                return;

            if (this.fxLight instanceof AmbientLight) {
                // Ambient lights are tracked separately because of checkpoint lighting.
                boolean didChangeOccur = false;
                AmbientLight ambientLight = (AmbientLight) this.fxLight;
                if (visible) {
                    if (!this.lightingManager.regularAmbientLights.contains(ambientLight))
                        didChangeOccur = this.lightingManager.regularAmbientLights.add(ambientLight);
                } else {
                    didChangeOccur = this.lightingManager.regularAmbientLights.remove(ambientLight);
                }

                if (didChangeOccur) {
                    this.lightingManager.getController().getEntityManager().updateAllEntityMeshes(); // This is necessary for things to be lit properly.
                    this.lightingManager.getController().getGeneralManager().updatePlayerCharacterLighting();
                }
            } else {
                // Regular lights are chucked into a group for lit entities.
                if (visible && this.lightingManager.isLightingAppliedToEntities()) {
                    addToScope(this.fxLight, this.lightingManager.getEntityGroup());
                } else {
                    removeFromScope(this.fxLight, this.lightingManager.getEntityGroup());
                }
            }

            if (visible) {
                this.lightingManager.getLightList().addIfMissing(this.fxLight);
            } else {
                this.lightingManager.getLightList().remove(this.fxLight);
            }
        }

        /**
         * Updates the light preview in 3D space.
         */
        public void updateLight() {
            LightBase newLight = this.lightingManager.updateLight(this.mapLight, this.fxLight);
            if (this.fxLight != newLight) {
                boolean wasVisible = this.visible;
                setVisible(false); // Make current light invisible.

                // Setup new light.
                this.fxLight = newLight;
                setVisible(wasVisible);
            }
        }

        /**
         * Called when the preview is removed.
         */
        private void onRemove() {
            setVisible(false);
            this.fxLight = null;
        }
    }
}