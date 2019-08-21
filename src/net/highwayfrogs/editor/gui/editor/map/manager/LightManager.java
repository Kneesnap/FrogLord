package net.highwayfrogs.editor.gui.editor.map.manager;

import javafx.scene.AmbientLight;
import javafx.scene.PointLight;
import net.highwayfrogs.editor.file.map.light.APILightType;
import net.highwayfrogs.editor.file.map.light.Light;
import net.highwayfrogs.editor.gui.GUIEditorGrid;
import net.highwayfrogs.editor.gui.editor.MapUIController;
import net.highwayfrogs.editor.utils.Utils;

/**
 * Manages map lights.
 * Created by Kneesnap on 8/19/2019.
 */
public class LightManager extends MapManager {
    private GUIEditorGrid lightEditor;
    private static final String LIGHT_LIST = "lightList";

    public LightManager(MapUIController controller) {
        super(controller);
    }

    @Override
    public void onSetup() {
        super.onSetup();
        getController().getApplyLightsCheckBox().setOnAction(evt -> updateMapLighting());
    }

    @Override
    public void setupEditor() {
        updateMapLighting();
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
     * Applies the current lighting setup to the level.
     */
    public void updateMapLighting() {
        if (getRenderManager().displayListExists(LIGHT_LIST))
            getRenderManager().clearDisplayList(LIGHT_LIST);

        if (!getController().getApplyLightsCheckBox().isSelected())
            return; // Don't lights if they're disabled.

        getRenderManager().addMissingDisplayList(LIGHT_LIST);

        // Iterate through each light and apply the the root scene graph node
        for (Light light : getMap().getLights()) {
            switch (light.getApiType()) {
                case AMBIENT:
                    AmbientLight ambLight = new AmbientLight();
                    ambLight.setColor(Utils.fromBGR(light.getColor()));
                    getRenderManager().addNode(LIGHT_LIST, ambLight);
                    break;

                case PARALLEL:
                    // IMPORTANT! JavaFX does NOT support parallel (directional) lights [AndyEder]
                    PointLight parallelLight = new PointLight();
                    parallelLight.setColor(Utils.fromBGR(light.getColor()));
                    // Use direction as a vector to set a position to simulate a parallel light as best as we can
                    parallelLight.setTranslateX(-light.getDirection().getFloatX(12) * 1024);
                    parallelLight.setTranslateY(-light.getDirection().getFloatY(12) * 1024);
                    parallelLight.setTranslateZ(-light.getDirection().getFloatZ(12) * 1024);
                    getRenderManager().addNode(LIGHT_LIST, parallelLight);
                    break;

                case POINT:
                    PointLight pointLight = new PointLight();
                    pointLight.setColor(Utils.fromBGR(light.getColor()));
                    // Assuming direction is position? Are POINT lights ever used? [AndyEder]
                    pointLight.setTranslateX(light.getDirection().getFloatX());
                    pointLight.setTranslateY(light.getDirection().getFloatY());
                    pointLight.setTranslateZ(light.getDirection().getFloatZ());
                    getRenderManager().addNode(LIGHT_LIST, pointLight);
                    break;
            }
        }
    }
}
