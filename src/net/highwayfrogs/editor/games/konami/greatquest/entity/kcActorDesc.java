package net.highwayfrogs.editor.games.konami.greatquest.entity;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.games.konami.greatquest.generic.kcCResourceGeneric;
import net.highwayfrogs.editor.games.konami.greatquest.script.kcScriptDisplaySettings;
import net.highwayfrogs.editor.system.Config;
import net.highwayfrogs.editor.utils.data.reader.DataReader;
import net.highwayfrogs.editor.utils.data.writer.DataWriter;
import net.highwayfrogs.editor.utils.logging.ILogger;

/**
 * Represents the 'kcActorDesc' struct.
 * Used for setup by kcCActor::Init.
 * Created by Kneesnap on 8/21/2023.
 */
@Getter
@Setter
public class kcActorDesc extends kcActorBaseDesc {
    private final kcHealthDesc health;
    private int invincibleDurationLimitMs = DEFAULT_INVINCIBLE_DURATION_LIMIT_MILLIS; // May not be used?
    private static final int PADDING_VALUES = 3;

    private static final int DEFAULT_INVINCIBLE_DURATION_LIMIT_MILLIS = 2000;

    public kcActorDesc(@NonNull kcCResourceGeneric resource, kcEntityDescType entityDescType) {
        super(resource, entityDescType);
        this.health = new kcHealthDesc(resource.getGameInstance());
    }

    @Override
    public void load(DataReader reader) {
        super.load(reader);
        this.health.load(reader);
        this.invincibleDurationLimitMs = reader.readInt();
        reader.skipBytesRequireEmpty(PADDING_VALUES * Constants.INTEGER_SIZE);
    }

    @Override
    public void saveData(DataWriter writer) {
        super.saveData(writer);
        this.health.save(writer);
        writer.writeInt(this.invincibleDurationLimitMs);
        writer.writeNull(PADDING_VALUES * Constants.INTEGER_SIZE);
    }

    private static final String CONFIG_KEY_INVICIBILITY_DURATION = "invincibleDurationLimitMs";

    @Override
    public void fromConfig(ILogger logger, Config input) {
        super.fromConfig(logger, input);
        this.health.fromConfig(logger, input);
        this.invincibleDurationLimitMs = input.getOrDefaultKeyValueNode(CONFIG_KEY_INVICIBILITY_DURATION).getAsInteger(DEFAULT_INVINCIBLE_DURATION_LIMIT_MILLIS);
    }

    @Override
    public void toConfig(Config output, kcScriptDisplaySettings settings) {
        super.toConfig(output, settings);
        this.health.toConfig(output);
        if (this.invincibleDurationLimitMs != DEFAULT_INVINCIBLE_DURATION_LIMIT_MILLIS)
            output.getOrCreateKeyValueNode(CONFIG_KEY_INVICIBILITY_DURATION).setAsInteger(this.invincibleDurationLimitMs);
    }

    @Override
    public void writeMultiLineInfo(StringBuilder builder, String padding) {
        super.writeMultiLineInfo(builder, padding);
        this.health.writePrefixedMultiLineInfo(builder, "Health", padding);
        if (this.invincibleDurationLimitMs != DEFAULT_INVINCIBLE_DURATION_LIMIT_MILLIS)
            builder.append(padding).append("Invincible Duration Limit (Ms): ").append(this.invincibleDurationLimitMs).append(Constants.NEWLINE);
    }
}