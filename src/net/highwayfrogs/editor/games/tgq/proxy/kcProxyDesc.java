package net.highwayfrogs.editor.games.tgq.proxy;

import lombok.Getter;
import lombok.Setter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.tgq.TGQUtils;
import net.highwayfrogs.editor.games.tgq.entity.kcBaseDesc;

/**
 * Implements the 'kcProxyDesc' struct.
 * Created by Kneesnap on 8/22/2023.
 */
@Getter
@Setter
public class kcProxyDesc extends kcBaseDesc {
    private int hash; // Hash of the chunk holding this description.
    private int reaction;
    private int collisionGroup;
    private int collideWith;
    private boolean isStatic;

    public static final int CLASS_ID = TGQUtils.hash("kcCProxy");

    @Override
    protected int getTargetClassID() {
        return CLASS_ID;
    }

    @Override
    public void load(DataReader reader) {
        super.load(reader);
        this.hash = reader.readInt();
        this.reaction = reader.readInt();
        this.collisionGroup = reader.readInt();
        this.collideWith = reader.readInt();
        this.isStatic = TGQUtils.readTGQBoolean(reader);
    }

    @Override
    public void saveData(DataWriter writer) {
        writer.writeInt(this.hash);
        writer.writeInt(this.reaction);
        writer.writeInt(this.collisionGroup);
        writer.writeInt(this.collideWith);
        TGQUtils.writeTGQBoolean(writer, this.isStatic);
    }

    @Override
    public void writeMultiLineInfo(StringBuilder builder, String padding) {
        // No need to display the hash, if we need to know that we can look at the resource containing this data.
        builder.append(padding).append("Reaction: ").append(this.reaction).append(Constants.NEWLINE); // TODO: Enum?
        builder.append(padding).append("Collision Group: ").append(this.collisionGroup).append(Constants.NEWLINE);
        builder.append(padding).append("Collide With: ").append(this.collideWith).append(Constants.NEWLINE);
        builder.append(padding).append("Static: ").append(this.isStatic).append(Constants.NEWLINE);
    }
}
