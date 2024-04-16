package net.highwayfrogs.editor.games.konami.greatquest.entity;

import lombok.Getter;
import lombok.Setter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.konami.greatquest.GreatQuestInstance;
import net.highwayfrogs.editor.games.konami.greatquest.kcClassID;

/**
 * Represents the 'kcActorDesc' struct.
 * Created by Kneesnap on 8/21/2023.
 */
@Getter
@Setter
public class kcActorDesc extends kcActorBaseDesc {
    private final kcHealthDesc health;
    private int invincibleDurationLimitMs;
    private final int[] padActor = new int[3];

    public kcActorDesc(GreatQuestInstance instance) {
        super(instance);
        this.health = new kcHealthDesc(instance);
    }

    @Override
    protected int getTargetClassID() {
        return kcClassID.ACTOR.getClassId();
    }

    @Override
    public void load(DataReader reader) {
        super.load(reader);
        this.health.load(reader);
        this.invincibleDurationLimitMs = reader.readInt();
        for (int i = 0; i < this.padActor.length; i++)
            this.padActor[i] = reader.readInt();
    }

    @Override
    public void saveData(DataWriter writer) {
        super.saveData(writer);
        this.health.save(writer);
        writer.writeInt(this.invincibleDurationLimitMs);
        for (int i = 0; i < this.padActor.length; i++)
            writer.writeInt(this.padActor[i]);
    }

    @Override
    public void writeMultiLineInfo(StringBuilder builder, String padding) {
        super.writeMultiLineInfo(builder, padding);
        this.health.writePrefixedMultiLineInfo(builder, "Health", padding);
        builder.append(padding).append("Invincible Duration Limit (Ms): ").append(this.invincibleDurationLimitMs).append(Constants.NEWLINE);
    }
}