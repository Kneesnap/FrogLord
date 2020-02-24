package net.highwayfrogs.editor.gui.editor.map.manager;

import javafx.scene.AmbientLight;
import javafx.scene.LightBase;
import javafx.scene.Node;
import javafx.scene.PointLight;
import net.highwayfrogs.editor.file.map.light.APILightType;
import net.highwayfrogs.editor.file.map.light.Light;
import net.highwayfrogs.editor.gui.GUIEditorGrid;
import net.highwayfrogs.editor.gui.editor.MapUIController;
import net.highwayfrogs.editor.utils.Utils;

import java.util.ArrayList;
import java.util.List;

/**
 * Manages map lights.
 * Created by Kneesnap on 8/19/2019.
 */
public class LightManager extends MapManager {
    private GUIEditorGrid lightEditor;
    private static final String LIGHT_LIST = "lightList";
    private List<LightBase> lights = new ArrayList<>();
    private LightBase mainLight;

    public LightManager(MapUIController controller) {
        super(controller);
    }

    @Override
    public void onSetup() {
        super.onSetup();
        getRenderManager().addMissingDisplayList(LIGHT_LIST);
        getController().getApplyLightsCheckBox().setOnAction(evt -> updateEntityLighting());

        this.mainLight = new AmbientLight(Utils.fromRGB(0xFFFFFF));
        this.mainLight.getScope().add(getController().getMeshView());
    }

    @Override
    public void setupEditor() {
        updateEntityLighting();
        if (lightEditor == null)
            lightEditor = new GUIEditorGrid(getController().getLightGridPane());

        lightEditor.clearEditor();
        for (int i = 0; i < getMap().getLights().size(); i++) {
            final int tempIndex = i;
            lightEditor.addBoldLabelButton("Light #" + (i + 1) + ":", "Remove", 25, () -> {
                getMap().getLights().remove(tempIndex);
                setupEditor();
            });

            lightEditor.addLabel("ApiType:", getMap().getLights().get(i).getApiType().name(), 25);
            getMap().getLights().get(i).makeEditor(lightEditor, this);

            lightEditor.addSeparator(25);
        }

        lightEditor.addButtonWithEnumSelection("Add Light", apiType -> {
            getMap().getLights().add(new Light(apiType));
            setupEditor();
        }, APILightType.values(), APILightType.AMBIENT);
    }

    /**
     * Update the lighting applied to a node.
     * @param node The node to update lighting for.
     */
    public void updateLightsForNode(Node node) {
        if (getController().getApplyLightsCheckBox().isSelected()) {
            this.mainLight.getScope().remove(node);
            for (LightBase light : this.lights)
                light.getScope().remove(node);
            this.mainLight.setVisible(!this.mainLight.getScope().isEmpty());
        } else { // We're not applying the lighting that the game will, instead we're going to apply white light to make it visible.
            this.mainLight.getScope().add(node);
            this.mainLight.setVisible(true);
        }
    }

    private void updateEntityLightingDisplay() {
        getController().getEntityManager().getEntityRenderGroup().getChildren().forEach(this::updateLightsForNode);
    }

    /**
     * Applies the current lighting setup to the level.
     */
    public void updateEntityLighting() {
        getRenderManager().clearDisplayList(LIGHT_LIST);
        this.lights.clear();

        if (!getController().getApplyLightsCheckBox().isSelected()) {
            updateEntityLightingDisplay();
            return; // Don't lights if they're disabled.
        }


        // Iterate through each light and apply the the root scene graph node
        for (Light light : getMap().getLights()) {

            LightBase lightBase = null;
            switch (light.getApiType()) {
                case AMBIENT:
                    AmbientLight ambLight = new AmbientLight();
                    ambLight.setColor(Utils.fromBGR(light.getColor()));
                    lightBase = ambLight;
                    break;

                case PARALLEL:
                    // IMPORTANT! JavaFX does NOT support parallel (directional) lights [AndyEder]
                    PointLight parallelLight = new PointLight();
                    parallelLight.setColor(Utils.fromBGR(light.getColor()));
                    // Use direction as a vector to set a position to simulate a parallel light as best as we can
                    parallelLight.setTranslateX(-light.getDirection().getFloatX(12) * 1024);
                    parallelLight.setTranslateY(-light.getDirection().getFloatY(12) * 1024);
                    parallelLight.setTranslateZ(-light.getDirection().getFloatZ(12) * 1024);
                    lightBase = parallelLight;
                    break;

                case POINT: // Point lights are not used.
                    PointLight pointLight = new PointLight();
                    pointLight.setColor(Utils.fromBGR(light.getColor()));
                    // Assuming direction is position? Are POINT lights ever used? [AndyEder]
                    pointLight.setTranslateX(light.getDirection().getFloatX());
                    pointLight.setTranslateY(light.getDirection().getFloatY());
                    pointLight.setTranslateZ(light.getDirection().getFloatZ());
                    lightBase = pointLight;
                    break;
            }

            if (lightBase != null) {
                this.lights.add(lightBase);
                getRenderManager().addNode(LIGHT_LIST, lightBase);
            }
        }

        updateEntityLightingDisplay();
    }
}
