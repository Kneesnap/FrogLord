package net.highwayfrogs.editor.games.konami.greatquest.proxy;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.konami.greatquest.GreatQuestHash;
import net.highwayfrogs.editor.games.konami.greatquest.GreatQuestUtils;
import net.highwayfrogs.editor.games.konami.greatquest.entity.kcBaseDesc;
import net.highwayfrogs.editor.games.konami.greatquest.generic.kcCResourceGeneric;
import net.highwayfrogs.editor.games.konami.greatquest.generic.kcIGenericResourceData;
import net.highwayfrogs.editor.utils.NumberUtils;

/**
 * Implements the 'kcProxyDesc' struct.
 * Created by Kneesnap on 8/22/2023.
 */
@Getter
@Setter
public abstract class kcProxyDesc extends kcBaseDesc implements kcIGenericResourceData {
    private final GreatQuestHash<kcCResourceGeneric> parentHash; // The hash of this object's parent.
    private int reaction;
    private int collisionGroup;
    private int collideWith; // Not a hash
    private boolean isStatic;

    public static final int CLASS_ID = GreatQuestUtils.hash("kcCProxy");

    public kcProxyDesc(@NonNull kcCResourceGeneric resource) {
        super(resource);
        this.parentHash = new GreatQuestHash<>(resource);
    }

    @Override
    protected int getTargetClassID() {
        return CLASS_ID;
    }

    @Override
    public void load(DataReader reader) {
        super.load(reader);
        int hThis = reader.readInt();
        this.reaction = reader.readInt();
        this.collisionGroup = reader.readInt();
        this.collideWith = reader.readInt();
        this.isStatic = GreatQuestUtils.readTGQBoolean(reader);

        if (hThis != this.parentHash.getHashNumber() && (getResource() == null || !getResource().doesNameMatch("TEST")))
            throw new RuntimeException("The kcProxyDesc reported the parent chunk as " + NumberUtils.to0PrefixedHexString(hThis) + ", but it was expected to be " + this.parentHash.getHashNumberAsString() + ".");
    }

    @Override
    public void saveData(DataWriter writer) {
        writer.writeInt(this.parentHash.getHashNumber());
        writer.writeInt(this.reaction);
        writer.writeInt(this.collisionGroup);
        writer.writeInt(this.collideWith);
        GreatQuestUtils.writeTGQBoolean(writer, this.isStatic);
    }

    @Override
    public void writeMultiLineInfo(StringBuilder builder, String padding) {
        // No need to display the hash, if we need to know that we can look at the resource containing this data.
        builder.append(padding).append("Reaction: ").append(this.reaction).append(Constants.NEWLINE); // TODO: Enum?
        builder.append(padding).append("Collision Group: ").append(this.collisionGroup).append(Constants.NEWLINE);
        builder.append(padding).append("Collide With: ").append(this.collideWith).append(Constants.NEWLINE);
        builder.append(padding).append("Static: ").append(this.isStatic).append(Constants.NEWLINE);
    }

    @Override
    public kcCResourceGeneric getResource() {
        return (kcCResourceGeneric) super.getResource();
    }
}