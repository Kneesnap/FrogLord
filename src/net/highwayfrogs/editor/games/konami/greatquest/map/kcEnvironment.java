package net.highwayfrogs.editor.games.konami.greatquest.map;

import lombok.Getter;
import lombok.Setter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.konami.greatquest.GreatQuestChunkedFile;
import net.highwayfrogs.editor.games.konami.greatquest.GreatQuestUtils;
import net.highwayfrogs.editor.games.konami.greatquest.IInfoWriter.IMultiLineInfoWriter;
import net.highwayfrogs.editor.games.konami.greatquest.toc.KCResourceID;
import net.highwayfrogs.editor.games.konami.greatquest.toc.kcCResource;
import net.highwayfrogs.editor.utils.Utils;

/**
 * A representation of the 'kcEnvironment' struct.
 * Created by Kneesnap on 8/1/2023.
 */
@Getter
@Setter
public class kcEnvironment extends kcCResource implements IMultiLineInfoWriter {
    private boolean lightingEnabled;
    private int ambientLightPackedColor; // uint
    private final kcLight[] directionalLights = new kcLight[3];
    private boolean fogEnabled;
    private final kcFogParams fog = new kcFogParams();
    private final kcPerspective perspective = new kcPerspective();

    public static final String ENVIRONMENT_NAME = "_kcEnvironment";

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
    public void writeMultiLineInfo(StringBuilder builder, String padding) {
        String newPadding = padding + " ";

        this.perspective.writePrefixedMultiLineInfo(builder, "Perspective", padding, newPadding);

        builder.append(padding).append("Fog Enabled: ").append(this.fogEnabled).append(Constants.NEWLINE);
        this.fog.writePrefixedMultiLineInfo(builder, "Fog", padding, newPadding);

        builder.append(padding).append("Lighting Enabled: ").append(this.lightingEnabled).append(Constants.NEWLINE);
        builder.append(padding).append("Ambient Light Color: ").append(Utils.to0PrefixedHexString(this.ambientLightPackedColor)).append(Constants.NEWLINE);

        for (int i = 0; i < this.directionalLights.length; i++) {
            if (this.directionalLights[i] == null)
                continue;

            builder.append(padding).append("Directional Light #").append(i + 1).append(':').append(Constants.NEWLINE);
            this.directionalLights[i].writeMultiLineInfo(builder, newPadding);
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