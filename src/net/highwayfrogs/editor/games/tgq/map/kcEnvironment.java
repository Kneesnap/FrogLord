package net.highwayfrogs.editor.games.tgq.map;

import lombok.Getter;
import lombok.Setter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.tgq.TGQChunkedFile;
import net.highwayfrogs.editor.games.tgq.TGQUtils;
import net.highwayfrogs.editor.games.tgq.toc.KCResourceID;
import net.highwayfrogs.editor.games.tgq.toc.kcCResource;
import net.highwayfrogs.editor.utils.Utils;

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
    private kcFogParams fog;
    private kcPerspective perspective;

    public static final String ENVIRONMENT_NAME = "_kcEnvironment";

    public kcEnvironment(TGQChunkedFile parentFile) {
        super(parentFile, KCResourceID.RAW);
    }

    @Override
    public void load(DataReader reader) {
        super.load(reader);

        this.lightingEnabled = TGQUtils.readTGQBoolean(reader);
        this.ambientLightPackedColor = reader.readInt();
        for (int i = 0; i < getDirectionalLightCount(); i++)
            getDirectionalLight(i).load(reader);

        this.fogEnabled = TGQUtils.readTGQBoolean(reader);
        getFog().load(reader);
        getPerspective().load(reader);
    }

    @Override
    public void save(DataWriter writer) {
        super.save(writer);

        TGQUtils.writeTGQBoolean(writer, this.lightingEnabled);
        writer.writeInt(this.ambientLightPackedColor);
        for (int i = 0; i < getDirectionalLightCount(); i++)
            getDirectionalLight(i).save(writer);

        TGQUtils.writeTGQBoolean(writer, this.fogEnabled);
        getFog().save(writer);
        getPerspective().save(writer);
    }

    /**
     * Writes information about this environment.
     * @param builder The builder to write the information to.
     * @param padding The padding to apply to new lines.
     */
    public void writeInfo(StringBuilder builder, String padding) {
        String newPadding = padding + " ";

        if (this.perspective != null) {
            builder.append(padding).append("Perspective:").append(Constants.NEWLINE);
            this.perspective.writeInfo(builder, newPadding);
        }

        builder.append(padding).append("Fog: ").append(this.fogEnabled).append(Constants.NEWLINE);
        builder.append(newPadding).append("Enabled: ").append(this.fogEnabled).append(Constants.NEWLINE);
        if (this.fog != null)
            this.fog.writeInfo(builder, newPadding);

        builder.append(padding).append("Lighting Enabled: ").append(this.lightingEnabled).append(Constants.NEWLINE);
        builder.append(padding).append("Ambient Light Color: ").append(Utils.to0PrefixedHexString(this.ambientLightPackedColor)).append(Constants.NEWLINE);

        for (int i = 0; i < this.directionalLights.length; i++) {
            if (this.directionalLights[i] == null)
                continue;

            builder.append(padding).append("Directional Light #").append((i + 1)).append(':').append(Constants.NEWLINE);
            this.directionalLights[i].writeInfo(builder, newPadding);
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

    /**
     * Gets the kcFogParams applied to this environment, creating it if it does not exist.
     * TODO: When we port to C#, we can remove these methods. When saving, we can do (this.fog ?? DEFAULT_FOG).save() instead.
     */
    public kcFogParams getFog() {
        if (this.fog == null)
            this.fog = new kcFogParams();
        return this.fog;
    }

    /**
     * Gets the kcPerspective applied to this environment, creating it if it does not exist.
     */
    public kcPerspective getPerspective() {
        if (this.perspective == null)
            this.perspective = new kcPerspective();
        return this.perspective;
    }
}
