package net.highwayfrogs.editor.games.sony.frogger.ui.mapeditor;

import javafx.scene.AmbientLight;
import javafx.scene.Group;
import javafx.scene.LightBase;
import javafx.scene.PointLight;
import javafx.scene.paint.Color;
import net.highwayfrogs.editor.file.map.light.APILightType;
import net.highwayfrogs.editor.file.map.light.Light;
import net.highwayfrogs.editor.gui.GUIEditorGrid;
import net.highwayfrogs.editor.utils.Utils;

import java.util.ArrayList;
import java.util.List;

/**
 * Manages map lights.
 * Created by Kneesnap on 8/19/2019.
 */
public class LightManager extends MapManager {
    private GUIEditorGrid lightEditor;
    private final List<LightBase> lights = new ArrayList<>();
    private AmbientLight mainLight;
    private static final String LIGHTING_LIST = "lightingList";

    public LightManager(MapUIController controller) {
        super(controller);
    }

    @Override
    public void onSetup() {
        super.onSetup();
        getController().getApplyLightsCheckBox().setOnAction(evt -> updateEntityLighting());

        // There is no lighting on terrain.
        this.mainLight = new AmbientLight(Color.WHITE);
        this.mainLight.getScope().add(getController().getMeshView());
        getRenderManager().addNode(this.mainLight);
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

    private Group getEntityGroup() {
        return getController().getEntityManager().getEntityRenderGroup();
    }

    /**
     * Applies the current lighting setup to the level.
     */
    public void updateEntityLighting() {
        getRenderManager().addMissingDisplayList(LIGHTING_LIST);
        getRenderManager().clearDisplayList(LIGHTING_LIST);
        getEntityGroup().getChildren().removeAll(this.lights);
        this.mainLight.getScope().remove(getEntityGroup());
        this.lights.clear();

        if (!getController().getApplyLightsCheckBox().isSelected()) {
            this.mainLight.getScope().add(getEntityGroup());
            return; // Don't update lights if they're disabled.
        }

        // Iterate through each light and apply the root scene graph node
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
                lightBase.getScope().add(getEntityGroup());
                getRenderManager().addNode(LIGHTING_LIST, lightBase);
            }
        }
    }
}