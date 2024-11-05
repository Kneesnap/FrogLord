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
    @NonNull private ProxyReact reaction = ProxyReact.SLIDE; // There is only one occurrence of any ProxyReact assigned to a kcProxy Desc which is not SLIDE. The other options are implemented (or at least I know NOTIFY/PENETRATE is), and probably would work.
    private int collisionGroup = 65536;
    private int collideWith = 14; // TODO: Is this always zero for characters? Perhaps it's just props that do this.
    private boolean isStatic; // If true, this->mFlags (kcCProxyTriMesh::Init) will have 0x10 set. Seems to only apply to kcCProxyCapsule by kcCActorBase::CreateCollisionProxy().

    // NOTE: collisionGroup and collideWith are the same fundamental data type I believe.
    // Eg: collideWith is for colliding with other objects, and collisionGroup is for other objects colliding with this.
    // kcCProxyCapsule::ResolveCollisionInt & kcCProxyCapsule::IsColliding seem to confirm that theory.
    // TODO: FINISH THIS?
    public static final int COLLISION_FLAG_ITEMS = Constants.BIT_FLAG_0; // 0x01, Items like to use this, but it's a guess really.
    public static final int COLLISION_FLAG_PLAYER = Constants.BIT_FLAG_1; // 0x02, This is a guess, but a good one since it's the most common flag. Also, CFrogCtl::Init uses this flag.
    public static final int COLLISION_FLAG_UNKNOWN_02 = Constants.BIT_FLAG_2; // 0x04, Unknown, but CFrogCtl::Init() uses this flag.
    public static final int COLLISION_FLAG_UNKNOWN_03 = Constants.BIT_FLAG_3; // 0x08, Unknown, but CFrogCtl::Init() uses this flag.
    public static final int COLLISION_FLAG_UNKNOWN_04 = Constants.BIT_FLAG_4; // 0x10, Unknown, but CFrogCtl::Init() uses this flag.
    public static final int COLLISION_FLAG_UNKNOWN_05 = Constants.BIT_FLAG_5; // 0x20, Unknown, but CFrogCtl::Init() uses this flag.
    public static final int COLLISION_FLAG_RUNTIME_FLYER = Constants.BIT_FLAG_11; // 0x800, CCharacter::ResetInt
    public static final int COLLISION_FLAG_RUNTIME_SWIMMER = Constants.BIT_FLAG_12; // 0x1000, CCharacter::ResetInt
    public static final int COLLISION_FLAG_RUNTIME_SENSOR = Constants.BIT_FLAG_14; // 0x4000, CCharacter::OnAttachSensor -> OnAttachSensor
    public static final int COLLISION_FLAG_UNKNOWN_15 = Constants.BIT_FLAG_15; // 0x8000, Unknown, but CFrogCtl::Init() uses this flag. TODO: Pickup?
    public static final int COLLISION_FLAG_UNKNOWN_16 = Constants.BIT_FLAG_16; // 0x10000, Unknown, but CFrogCtl::Init() uses this flag.


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
    @SuppressWarnings("DataFlowIssue")
    public void load(DataReader reader) {
        super.load(reader);
        int hThis = reader.readInt();
        this.reaction = ProxyReact.getReaction(reader.readInt(), false);
        this.collisionGroup = reader.readInt();
        this.collideWith = reader.readInt();
        this.isStatic = GreatQuestUtils.readTGQBoolean(reader);

        if (hThis != this.parentHash.getHashNumber() && (getResource() == null || !getResource().doesNameMatch("TEST")))
            throw new RuntimeException("The kcProxyDesc reported the parent chunk as " + NumberUtils.to0PrefixedHexString(hThis) + ", but it was expected to be " + this.parentHash.getHashNumberAsString() + ".");
    }

    @Override
    public void saveData(DataWriter writer) {
        writer.writeInt(this.parentHash.getHashNumber());
        writer.writeInt(this.reaction.ordinal());
        writer.writeInt(this.collisionGroup);
        writer.writeInt(this.collideWith);
        GreatQuestUtils.writeTGQBoolean(writer, this.isStatic);
    }

    @Override
    public void writeMultiLineInfo(StringBuilder builder, String padding) {
        // No need to display the hash, if we need to know that we can look at the resource containing this data.
        builder.append(padding).append("Reaction: ").append(this.reaction).append(Constants.NEWLINE);
        builder.append(padding).append("Collision Group: ").append(this.collisionGroup).append(Constants.NEWLINE);
        builder.append(padding).append("Collide With: ").append(this.collideWith).append(Constants.NEWLINE);
        builder.append(padding).append("Static: ").append(this.isStatic).append(Constants.NEWLINE);
    }

    @Override
    public kcCResourceGeneric getResource() {
        return (kcCResourceGeneric) super.getResource();
    }
}