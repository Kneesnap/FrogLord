package net.highwayfrogs.editor.games.konami.greatquest.map;

import javafx.scene.image.Image;
import lombok.Getter;
import lombok.Setter;
import net.highwayfrogs.editor.games.konami.greatquest.GreatQuestUtils;
import net.highwayfrogs.editor.games.konami.greatquest.chunks.GreatQuestChunkedFile;
import net.highwayfrogs.editor.games.konami.greatquest.chunks.KCResourceID;
import net.highwayfrogs.editor.games.konami.greatquest.chunks.kcCResource;
import net.highwayfrogs.editor.gui.GUIEditorGrid;
import net.highwayfrogs.editor.gui.ImageResource;
import net.highwayfrogs.editor.gui.components.propertylist.PropertyListNode;
import net.highwayfrogs.editor.gui.editor.MeshViewController;
import net.highwayfrogs.editor.utils.NumberUtils;
import net.highwayfrogs.editor.utils.data.reader.DataReader;
import net.highwayfrogs.editor.utils.data.writer.DataWriter;

/**
 * A representation of the 'kcEnvironment' struct.
 * Created by Kneesnap on 8/1/2023.
 */
@Getter
@Setter
public class kcEnvironment extends kcCResource {
    private boolean lightingEnabled;
    private int ambientLightPackedColor; // uint
    private final kcLight[] directionalLights = new kcLight[3];
    private boolean fogEnabled;
    private final kcFogParams fog = new kcFogParams();
    private final kcPerspective perspective = new kcPerspective();

    public static final String RESOURCE_NAME = "_kcEnvironment";

    public kcEnvironment(GreatQuestChunkedFile parentFile) {
        super(parentFile, KCResourceID.RAW);
    }

    @Override
    public void load(DataReader reader) {
        super.load(reader);

        this.lightingEnabled = GreatQuestUtils.readTGQBoolean(reader);
        this.ambientLightPackedColor = reader.readInt();
        for (int i = 0; i < getDirectionalLightCount(); i++)
            getDirectionalLight(i).load(reader);

        this.fogEnabled = GreatQuestUtils.readTGQBoolean(reader);
        getFog().load(reader);
        getPerspective().load(reader);
    }

    @Override
    public void save(DataWriter writer) {
        super.save(writer);

        GreatQuestUtils.writeTGQBoolean(writer, this.lightingEnabled);
        writer.writeInt(this.ambientLightPackedColor);
        for (int i = 0; i < getDirectionalLightCount(); i++)
            getDirectionalLight(i).save(writer);

        GreatQuestUtils.writeTGQBoolean(writer, this.fogEnabled);
        getFog().save(writer);
        getPerspective().save(writer);
    }

    @Override
    public Image getCollectionViewIcon() {
        return ImageResource.GHIDRA_ICON_INTERNET_16.getFxImage();
    }

    /**
     * Creates the editor for the data here.
     * @param editorGrid the editor to setup
     * @param viewController the view controller
     */
    public void setupEditor(GUIEditorGrid editorGrid, MeshViewController<?> viewController) {
        this.perspective.setupEditor(editorGrid);

        editorGrid.addCheckBox("Fog Enabled", this.fogEnabled, newValue -> this.fogEnabled = newValue);
        this.fog.setupEditor(editorGrid, this);

        editorGrid.addCheckBox("Lighting Enabled", this.lightingEnabled, newValue -> this.lightingEnabled = newValue); // TODO: Impact 3D view?
        editorGrid.addColorPickerWithAlpha("Ambient Light Color", this.ambientLightPackedColor, newValue -> this.ambientLightPackedColor = newValue);

        // Lights.
        for (int i = 0; i < this.directionalLights.length; i++) {
            if (this.directionalLights[i] == null)
                continue; // TODO: Allow creation, but not deletion.

            editorGrid.addBoldLabel("Directional Light #" + (i + 1));
            this.directionalLights[i].setupEditor(editorGrid, viewController);
        }
    }

    @Override
    public void addToPropertyList(PropertyListNode propertyList) {
        super.addToPropertyList(propertyList);
        propertyList.addProperties("Perspective", this.perspective);

        propertyList.addBoolean("Fog Enabled", this.fogEnabled, newValue -> this.fogEnabled = newValue);
        propertyList.addProperties("Fog", this.fog);

        propertyList.addBoolean("Lighting Enabled", this.lightingEnabled, newValue -> this.lightingEnabled = newValue);
        propertyList.addInteger("Ambient Light Color", this.ambientLightPackedColor)
                .setDataToStringConverter(NumberUtils::to0PrefixedHexString)
                .setDataFromStringConverter(NumberUtils::parseHexInteger);

        for (int i = 0; i < this.directionalLights.length; i++) {
            if (this.directionalLights[i] != null)
                propertyList.addProperties("Directional Light #" + (i + 1), this.directionalLights[i]);
        }
    }

    /**
     * Get the number of directional lights.
     * @return directionalLightCount
     */
    public int getDirectionalLightCount() {
        return this.directionalLights.length;
    }

    /**
     * Gets the nth directional light, creating it if it does not exist and there is room for it.
     * @param lightId The ID of the light to get.
     * @return directionalLight
     */
    public kcLight getDirectionalLight(int lightId) {
        if (lightId < 0 || lightId >= this.directionalLights.length)
            throw new IndexOutOfBoundsException("The provided light ID of " + lightId + " is invalid.");

        kcLight light = this.directionalLights[lightId];
        if (light == null)
            this.directionalLights[lightId] = light = new kcLight();

        return light;
    }
}