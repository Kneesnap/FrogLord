package net.highwayfrogs.editor.games.konami.greatquest.proxy;

import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.games.konami.IConfigData;
import net.highwayfrogs.editor.games.konami.greatquest.GreatQuestHash;
import net.highwayfrogs.editor.games.konami.greatquest.GreatQuestUtils;
import net.highwayfrogs.editor.games.konami.greatquest.entity.kcBaseDesc;
import net.highwayfrogs.editor.games.konami.greatquest.generic.kcCResourceGeneric;
import net.highwayfrogs.editor.games.konami.greatquest.generic.kcIGenericResourceData;
import net.highwayfrogs.editor.system.Config;
import net.highwayfrogs.editor.utils.NumberUtils;
import net.highwayfrogs.editor.utils.data.reader.DataReader;
import net.highwayfrogs.editor.utils.data.writer.DataWriter;
import net.highwayfrogs.editor.utils.objects.OptionalArguments;

/**
 * Implements the 'kcProxyDesc' struct, used to describe a kcCProxy.
 * kcCTriMesh (or kcCProxyCapsule) will check collision with the entity's sphere (separate from the proxy) before checking the proxy collision.
 * This is an optimization technique to avoid expensive collision checks, but it requires the user to update the entity sphere to contain all parts of the proxy which the user wishes to enable collision for.
 * Example: kcCProxyTriMesh::Intersect
 * Created by Kneesnap on 8/22/2023.
 */
@Getter
@Setter
public abstract class kcProxyDesc extends kcBaseDesc implements kcIGenericResourceData, IConfigData {
    private final GreatQuestHash<kcCResourceGeneric> parentHash; // The hash of this object's parent.
    private final kcProxyDescType descriptionType; // The hash of this object's parent.
    @NonNull private ProxyReact reaction = ProxyReact.SLIDE; // There is only one occurrence of any ProxyReact assigned to a kcProxyDesc which is not SLIDE. The other options are implemented (or at least I know NOTIFY/PENETRATE is), and probably would work. Default is set in CItem::Init
    private int collisionGroup; // This value is applied to the entity's kcCProxy. If either ((this->collideWith & other.collisionGroup) || (other.collideWith & this.collisionGroup)) have bits (unless it's an octree search), it will perform collision checks.
    private int collideWith; // This value is applied to the entity's kcCProxy.

    // NOTE: collisionGroup and collideWith are the same fundamental data type I believe.
    // Eg: collideWith is for colliding with other objects, and collisionGroup is for other objects colliding with this.
    // kcCProxyCapsule::ResolveCollisionInt & kcCProxyCapsule::IsColliding seem to confirm that theory.
    // sLinkProxy (OctTree handler for ResolveCollision/ResolveCollisionIterate) ensures that the entity testing collision's collideWith has at least one masked bit with the collisionGroup of the oct tree entity.

    public static final int CLASS_ID = GreatQuestUtils.hash("kcCProxy");
    public static final String NAME_SUFFIX = "ProxyDesc"; // This is applied to all kcProxyTriMeshDescs.

    public kcProxyDesc(kcCResourceGeneric resource, kcProxyDescType descriptionType) {
        super(resource);
        this.parentHash = new GreatQuestHash<>(resource);
        this.descriptionType = descriptionType;
        this.collisionGroup = (this instanceof kcProxyCapsuleDesc) ? kcCollisionGroup.PLAYER.getBitMask() : 0; // Seen in CItem::Init()
        this.collideWith = (this instanceof kcProxyCapsuleDesc) ? kcCollisionGroup.TRI_MESH.getBitMask() : 0; // Seen in CItem::Init()
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
        boolean isStatic = GreatQuestUtils.readTGQBoolean(reader);
        if (isStatic != isStatic()) // Allow this value in-case it ever happens.
            getLogger().warning("The proxy description expected to " + (isStatic() ? "be" : "not be") + " static, however this assumption was incorrect!");

        if (hThis != this.parentHash.getHashNumber() && (getResource() == null || !getResource().doesNameMatch("TEST")))
            throw new RuntimeException("The kcProxyDesc reported the parent chunk as " + NumberUtils.to0PrefixedHexString(hThis) + ", but it was expected to be " + this.parentHash.getHashNumberAsString() + ".");
    }

    @Override
    public void saveData(DataWriter writer) {
        writer.writeInt(this.parentHash.getHashNumber());
        writer.writeInt(this.reaction.ordinal());
        writer.writeInt(this.collisionGroup);
        writer.writeInt(this.collideWith);
        GreatQuestUtils.writeTGQBoolean(writer, isStatic());
    }

    @Override
    public void writeMultiLineInfo(StringBuilder builder, String padding) {
        // No need to display the hash, if we need to know that we can look at the resource containing this data.
        builder.append(padding).append("Reaction: ").append(this.reaction).append(Constants.NEWLINE);
        builder.append(padding).append("Collision Group: ").append(kcCollisionGroup.getAsString(this.collisionGroup)).append(Constants.NEWLINE);
        builder.append(padding).append("Collide With: ").append(kcCollisionGroup.getAsString(this.collideWith)).append(Constants.NEWLINE);
    }

    @Override
    public kcCResourceGeneric getResource() {
        return (kcCResourceGeneric) super.getResource();
    }

    public static final String CONFIG_KEY_DESC_TYPE = "type";
    private static final String CONFIG_KEY_REACTION = "reaction";
    private static final String CONFIG_KEY_COLLISION_GROUP = "collisionGroups";
    private static final String CONFIG_KEY_COLLIDE_WITH = "collideWith";

    @Override
    public void fromConfig(Config input) {
        kcProxyDescType descType = input.getKeyValueNodeOrError(CONFIG_KEY_DESC_TYPE).getAsEnumOrError(kcProxyDescType.class);
        if (descType != getDescriptionType())
            throw new RuntimeException("The proxy description reported itself as " + descType + ", which is incompatible with " + getDescriptionType() + ".");

        this.reaction = input.getKeyValueNodeOrError(CONFIG_KEY_REACTION).getAsEnumOrError(ProxyReact.class);
        if (this.reaction == ProxyReact.HALT)
            getLogger().warning("ProxyReact.HALT is not enabled for proxy descriptions, and will be changed to SLIDE.");

        String collisionGroupStr = input.getOrDefaultKeyValueNode(CONFIG_KEY_COLLISION_GROUP).getAsString(kcCollisionGroup.NO_COLLISION_GROUP);
        OptionalArguments collisionGroupArgs = OptionalArguments.parseCommaSeparatedNamedArguments(collisionGroupStr);
        this.collisionGroup = kcCollisionGroup.getValueFromArguments(collisionGroupArgs);
        collisionGroupArgs.warnAboutUnusedArguments(getLogger());

        String collideWithStr = input.getOrDefaultKeyValueNode(CONFIG_KEY_COLLIDE_WITH).getAsString(kcCollisionGroup.NO_COLLISION_GROUP);
        OptionalArguments collideWithArgs = OptionalArguments.parseCommaSeparatedNamedArguments(collideWithStr);
        this.collideWith = kcCollisionGroup.getValueFromArguments(collideWithArgs);
        collideWithArgs.warnAboutUnusedArguments(getLogger());
    }

    @Override
    public void toConfig(Config output) {
        output.getOrCreateKeyValueNode(CONFIG_KEY_DESC_TYPE).setAsEnum(getDescriptionType());
        output.getOrCreateKeyValueNode(CONFIG_KEY_REACTION).setAsEnum(this.reaction);
        output.getOrCreateKeyValueNode(CONFIG_KEY_COLLISION_GROUP).setAsString(kcCollisionGroup.getAsString(this.collisionGroup));
        output.getOrCreateKeyValueNode(CONFIG_KEY_COLLIDE_WITH).setAsString(kcCollisionGroup.getAsString(this.collideWith));
    }

    /**
     * There's another boolean which exists in the data, but isn't kept as a field here named "static".
     * It appears to be automatically generated. If true, this->mFlags (kcCProxyTriMesh::Init) will have 0x10 set.
     * That flag is checked by kcCProxyTriMesh::Intersect, so if the flag is present, the sphere calculation will be skipped.
     * I'm not sure in what scenario this would ever be desirable, so it will not be implemented as user-editable.
     */
    public abstract boolean isStatic();

    @Getter
    @RequiredArgsConstructor
    public enum kcCollisionGroup {
        TRI_MESH("TriangleMeshes", Constants.BIT_FLAG_0), // 0x01, Items like to use this, although it's only in kcCOctTreeAtom::sLinkProxy which it's shown this will cause LinkTemporaryMeshes() to be called. They will be immediately de-activated.
        PLAYER("Player", Constants.BIT_FLAG_1), // 0x02, This is a guess, but a good one since it's the most common flag. Also, CFrogCtl::Init uses this flag. Also Used by kcCProxyCapsule::ResolveCollisionIterate()
        NON_HOSTILE("NonHostileEntities", Constants.BIT_FLAG_2), // 0x04, CFrogCtl::Init() uses this flag. Seen on FFM, Bugs, Blair, etc.
        HOSTILE("HostileEntities", Constants.BIT_FLAG_3), // 0x08, CFrogCtl::Init() uses this flag. Seen on Hemo, fish, etc.
        PLAYER_KICK("PlayerKicks", Constants.BIT_FLAG_4), // 0x10, Never seen in game data, but CFrogCtl::Init() uses this flag.
        PLAYER_PUNCH("PlayerPunches", Constants.BIT_FLAG_5), // 0x20, Never seen in game data, but CFrogCtl::Init() uses this flag.
        FLYER("Flyers", Constants.BIT_FLAG_11), // 0x800, CCharacter::ResetInt
        SWIMMER("Swimmers", Constants.BIT_FLAG_12), // 0x1000, CCharacter::ResetInt
        SENSOR("Sensors", Constants.BIT_FLAG_14), // 0x4000, Not used by the vanilla game, but still supported. CCharacter::OnAttachSensor -> OnAttachSensor
        ITEM("Items", Constants.BIT_FLAG_15), // 0x8000, CFrogCtl::Init() uses this flag and CItem::Init() sets this flag.
        TERRAIN("Terrain", Constants.BIT_FLAG_16), // 0x10000, Unknown, but CFrogCtl::Init() uses this flag.
        CLIMBABLE("Climbable", Constants.BIT_FLAG_31); // 0x80000000, Seems to be set on climbable models such as ladders and vines.

        private final String displayName;
        private final int bitMask;

        public static final String NO_COLLISION_GROUP = "None";
        private static final String UNNAMED_GROUP_PREFIX = "UnnamedGroup";

        /**
         * Get the flags value as a parse-able string.
         * @param value The value to determine which flags to apply from
         */
        public static String getAsString(int value) {
            return value != 0 ? getAsOptionalArguments(value).getNamedArgumentsAsCommaSeparatedString() : NO_COLLISION_GROUP;
        }

        /**
         * Add flags to an optional arguments object.
         * @param value The value to determine which flags to apply from
         */
        public static OptionalArguments getAsOptionalArguments(int value) {
            OptionalArguments arguments = new OptionalArguments();
            addFlags(value, arguments);
            return arguments;
        }

        /**
         * Add flags to the arguments for the corresponding collision groups.
         * @param value The value to determine which flags to apply from
         * @param arguments The arguments to add the flags to
         */
        public static void addFlags(int value, OptionalArguments arguments) {
            // Write flags.
            for (int i = 0; i < values().length; i++) {
                kcCollisionGroup group = values()[i];
                if ((value & group.getBitMask()) == group.getBitMask()) {
                    arguments.getOrCreate(group.getDisplayName());
                    value &= ~group.getBitMask();
                }
            }

            // Add unknown arguments.
            for (int i = 0; i < Constants.BITS_PER_INTEGER && value != 0; i++) {
                int bitMask = (1 << i);
                if ((value & bitMask) == bitMask)
                    arguments.getOrCreate(String.format("%s%02d", UNNAMED_GROUP_PREFIX, i));
            }
        }

        /**
         * Consume optional flag arguments to build a value containing the same flags as specified by the arguments.
         * @param arguments The arguments to create the value from.
         * @return flagArguments
         */
        public static int getValueFromArguments(OptionalArguments arguments) {
            int value = 0;
            for (int i = 0; i < values().length; i++) {
                kcCollisionGroup group = values()[i];
                if (arguments.useFlag(group.getDisplayName()))
                    value |= group.getBitMask();
            }

            for (int i = 0; i < Constants.BITS_PER_INTEGER && value != 0; i++) {
                String testFlag = String.format("%s%02d", UNNAMED_GROUP_PREFIX, i);
                if (arguments.useFlag(testFlag))
                    value |= (1 << i);
            }

            // Avoid warning about unknown flags.
            if (value == 0)
                arguments.useFlag(NO_COLLISION_GROUP);

            return value;
        }
    }
}