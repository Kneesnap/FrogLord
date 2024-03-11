package net.highwayfrogs.editor.games.sony.oldfrogger.map.ui;

import javafx.scene.AmbientLight;
import javafx.scene.Group;
import javafx.scene.LightBase;
import javafx.scene.paint.Color;
import lombok.Getter;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.mesh.OldFroggerMapMesh;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.packet.OldFroggerMapLightPacket.OldFroggerMapLight;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.ui.OldFroggerMapUIManager.OldFroggerMapListManager;
import net.highwayfrogs.editor.gui.editor.DisplayList;
import net.highwayfrogs.editor.gui.editor.MeshViewController;
import net.highwayfrogs.editor.gui.editor.UISidePanel;

import java.util.List;

/**
 * Manages old frogger map lights.
 * Created by Kneesnap on 12/23/2023.
 */
@Getter
public class OldFroggerLightManager extends OldFroggerMapListManager<OldFroggerMapLight, LightBase> {
    private final Group lightingGroup = new Group(); // This is the group of things impacted by lighting.
    private AmbientLight mainLight;
    private DisplayList lightList;

    public OldFroggerLightManager(MeshViewController<OldFroggerMapMesh> controller) {
        super(controller);
    }

    @Override
    public String getTitle() {
        return "Lights";
    }

    @Override
    public String getValueName() {
        return "Light";
    }

    @Override
    public List<OldFroggerMapLight> getValues() {
        return getMap().getLightPacket().getLights();
    }

    @Override
    public void onSetup() {
        this.lightList = getRenderManager().createDisplayList();
        super.onSetup();

        // There is no lighting on terrain, equivalent to a fully white ambient light.
        this.mainLight = new AmbientLight(Color.WHITE);
        this.mainLight.getScope().add(getController().getMeshView());
        getRenderManager().getRoot().getChildren().add(this.mainLight);

        // Add lighting group.
        getRenderManager().getRoot().getChildren().add(this.lightingGroup);

        // Setup lighting.
        toggleMainLightIfNecessary();
    }

    @Override
    protected void setupMainGridEditor(UISidePanel sidePanel) {
        super.setupMainGridEditor(sidePanel);
        if (getMap().getMapConfig().isCaveLightingEnabled()) {
            getValueDisplaySetting().setValue(ListDisplayType.NONE); // Disable lighting by default, because it's pitch black.
        } else {
            getValueDisplaySetting().setValue(ListDisplayType.ALL); // All lights should show by default.
        }
    }

    @Override
    protected LightBase setupDisplay(OldFroggerMapLight light) {
        LightBase lightBase = light.createJavaFxLight();
        if (lightBase != null) {
            this.lightList.add(lightBase);
            lightBase.getScope().add(this.lightingGroup);
        }

        return lightBase;
    }

    @Override
    protected void updateEditor(OldFroggerMapLight light) {
        light.setupEditor(this, getEditorGrid());
    }

    @Override
    protected void setValuesVisible(boolean visible) {
        super.setValuesVisible(visible);
        toggleMainLightIfNecessary(); // Ensure the main light gets added & removed.
    }

    @Override
    protected void setVisible(OldFroggerMapLight oldFroggerMapLight, LightBase light, boolean visible) {
        if (light != null)
            light.setLightOn(visible);
    }

    @Override
    protected void onSelectedValueChange(OldFroggerMapLight oldLight, LightBase oldLightBase, OldFroggerMapLight newLight, LightBase newLightBase) {
        // Don't need to do anything.
    }

    @Override
    protected OldFroggerMapLight createNewValue() {
        return new OldFroggerMapLight(getMap().getGameInstance());
    }

    @Override
    protected void onDelegateRemoved(OldFroggerMapLight oldFroggerMapLight, LightBase light) {
        if (light != null) {
            light.getScope().remove(this.lightingGroup);
            this.lightList.remove(light);
        }

        toggleMainLightIfNecessary();
    }

    /**
     * Test if the main light should be active (If everything else is disabled).
     */
    public boolean shouldMainLightBeActive() {
        ListDisplayType displayType = getValueDisplaySetting() != null ? getValueDisplaySetting().getValue() : null;
        return (displayType == null || displayType == ListDisplayType.NONE) || getValues().isEmpty();
    }

    /**
     * Update the activation state of the main light.
     */
    public boolean toggleMainLightIfNecessary() {
        if (this.mainLight == null)
            return false; // Main light isn't setup yet.

        this.mainLight.getScope().remove(this.lightingGroup);

        // Activate if necessary.
        if (shouldMainLightBeActive()) {
            this.mainLight.getScope().add(this.lightingGroup);
            return true;
        }

        return false;
    }
}
