package net.highwayfrogs.editor.games.konami.greatquest.entity;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.konami.greatquest.generic.kcCResourceGeneric;
import net.highwayfrogs.editor.games.konami.greatquest.script.kcScriptDisplaySettings;
import net.highwayfrogs.editor.system.Config;

/**
 * Represents the 'kcActorDesc' struct.
 * Used for setup by kcCActor::Init.
 * Created by Kneesnap on 8/21/2023.
 */
@Getter
@Setter
public class kcActorDesc extends kcActorBaseDesc {
    private final kcHealthDesc health;
    private int invincibleDurationLimitMs = 2000; // TODO: May not be used?
    private static final int PADDING_VALUES = 3;

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

    @Override
    public void fromConfig(Config input) {
        super.fromConfig(input);
        this.health.fromConfig(input);
        this.invincibleDurationLimitMs = input.getKeyValueNodeOrError("invincibleDurationLimitMs").getAsInteger();

    }

    @Override
    public void toConfig(Config output, kcScriptDisplaySettings settings) {
        super.toConfig(output, settings);
        this.health.toConfig(output);
        output.getOrCreateKeyValueNode("invincibleDurationLimitMs").setAsInteger(this.invincibleDurationLimitMs);
    }

    @Override
    public void writeMultiLineInfo(StringBuilder builder, String padding) {
        super.writeMultiLineInfo(builder, padding);
        this.health.writePrefixedMultiLineInfo(builder, "Health", padding);
        builder.append(padding).append("Invincible Duration Limit (Ms): ").append(this.invincibleDurationLimitMs).append(Constants.NEWLINE);
    }
}