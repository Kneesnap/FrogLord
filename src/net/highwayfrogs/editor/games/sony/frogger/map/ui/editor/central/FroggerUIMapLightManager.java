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

import java.util.List;

/**
 * Manages map lighting for Frogger.
 * TODO: Go over FroggerMapLight values.
 * TODO: For cone preview, we could just draw lines to the cone edges instead of doing a full cone inside.
 * TODO: For changing the direction of a light, a rotation gizmo is ideal.
 * TODO: We should show the lights in 3D space too? Or at least the ones with 3D data. (3D Preview! I like making a billboard sprite light icon.)
 * Created by Kneesnap on 6/1/2024.
 */
@Getter
public class FroggerUIMapLightManager extends FroggerCentralMapListManager<FroggerMapLight, FroggerMapLightPreview> {
    private CheckBox applyLightingToEntitiesCheckBox;
    private DisplayList lightList;

    public FroggerUIMapLightManager(MeshViewController<FroggerMapMesh> controller) {
        super(controller);
    }

    @Override
    public void onSetup() {
        this.lightList = getRenderManager().createDisplayList();
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
        lightPreview.setVisible(visible);
    }

    @Override
    public boolean isValueVisibleByUI(FroggerMapLight light) {
        return super.isValueVisibleByUI(light) && this.applyLightingToEntitiesCheckBox.isSelected();
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
        if (this.applyLightingToEntitiesCheckBox.isSelected()) {
            getController().getMainLight().getScope().remove(getEntityGroup());
        } else {
            getController().getMainLight().getScope().add(getEntityGroup());
        }

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

            if (visible && this.lightingManager.getApplyLightingToEntitiesCheckBox().isSelected()) {
                this.fxLight.getScope().add(this.lightingManager.getEntityGroup());
            } else {
                this.fxLight.getScope().remove(this.lightingManager.getEntityGroup());
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