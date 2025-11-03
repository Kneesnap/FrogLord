package net.highwayfrogs.editor.games.konami.greatquest.entity;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.games.konami.greatquest.GreatQuestHash;
import net.highwayfrogs.editor.games.konami.greatquest.GreatQuestUtils;
import net.highwayfrogs.editor.games.konami.greatquest.chunks.*;
import net.highwayfrogs.editor.games.konami.greatquest.generic.kcCResourceGeneric;
import net.highwayfrogs.editor.games.konami.greatquest.generic.kcCResourceGeneric.kcCResourceGenericType;
import net.highwayfrogs.editor.games.konami.greatquest.generic.kcCResourceGeneric.kcCResourceGenericTypeGroup;
import net.highwayfrogs.editor.games.konami.greatquest.model.kcModelDesc;
import net.highwayfrogs.editor.games.konami.greatquest.model.kcModelWrapper;
import net.highwayfrogs.editor.games.konami.greatquest.proxy.kcProxyDesc;
import net.highwayfrogs.editor.games.konami.greatquest.script.kcScriptDisplaySettings;
import net.highwayfrogs.editor.games.konami.greatquest.ui.mesh.model.GreatQuestModelMesh;
import net.highwayfrogs.editor.games.konami.greatquest.ui.mesh.model.GreatQuestModelViewController;
import net.highwayfrogs.editor.gui.editor.MeshViewController;
import net.highwayfrogs.editor.system.Config;
import net.highwayfrogs.editor.system.Config.ConfigValueNode;
import net.highwayfrogs.editor.utils.NumberUtils;
import net.highwayfrogs.editor.utils.data.reader.DataReader;
import net.highwayfrogs.editor.utils.data.writer.DataWriter;
import net.highwayfrogs.editor.utils.logging.ILogger;

import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.List;

/**
 * Represents the 'kcActorBaseDesc' struct.
 * Created by Kneesnap on 8/21/2023.
 */
@Getter
@Setter
public class kcActorBaseDesc extends kcEntity3DDesc {
    private final GreatQuestHash<kcCResourceGeneric> parentHash;
    private final GreatQuestHash<kcCResourceGeneric> modelDescRef;
    private final GreatQuestHash<kcCResourceSkeleton> hierarchyRef; // If this value is -1, it means create a new kcCAnimCtl with CID_PRS for the animation controller. Otherwise, create a new kcCSkeleton from this. kcCSkeleton::InitHierarchy() will be called whenever the hash is not -1.
    private int channelCount = DEFAULT_ANIMATION_CHANNEL_COUNT; // Used for initializing the skeleton hierarchy in kcCActorBase::Init, this is always seen to be two.
    private final GreatQuestHash<kcCResourceAnimSet> animSetRef; // I've done an extensive search and am confident that this is completely unused.
    private final GreatQuestHash<kcCResourceGeneric> proxyDescRef; // kcCActorBase::Init() will fail if this is not either -1 or a valid hash.
    private final GreatQuestHash<kcCResourceNamedHash> animationSequencesRef; // hAnimHash, kcCActorBase::Init, kcCActorBase::ResetInt
    private static final int PADDING_VALUES = 4;
    private static final String NAME_SUFFIX = "ActorDesc";

    private static final int DEFAULT_ANIMATION_CHANNEL_COUNT = 2;

    public kcActorBaseDesc(@NonNull kcCResourceGeneric resource, kcEntityDescType descType) {
        super(resource, descType);
        this.parentHash = new GreatQuestHash<>(resource); // kcActorBaseDesc::__ct
        this.modelDescRef = new GreatQuestHash<>(); // kcCActorBase::__ct, kcCActorBase::Init()
        this.hierarchyRef = new GreatQuestHash<>(); // kcCActorBase::Init()
        this.animSetRef = new GreatQuestHash<>(); // kcCActorBase::__ct
        this.proxyDescRef = new GreatQuestHash<>(); // kcCActorBase::Init()
        this.animationSequencesRef = new GreatQuestHash<>(); // kcCActorBase::__ct
        GreatQuestUtils.applySelfNameSuffixAndToFutureNameChanges(resource, NAME_SUFFIX);
        applyNameToDescriptions();
    }

    @Override
    public void load(DataReader reader) {
        super.load(reader);
        int hThis = reader.readInt();
        int modelDescHash = reader.readInt();
        int hierarchyHash = reader.readInt();
        this.channelCount = reader.readInt();
        int animSetHash = reader.readInt();
        int proxyDescHash = reader.readInt();
        int animationHash = reader.readInt();
        reader.skipBytesRequireEmpty(PADDING_VALUES * Constants.INTEGER_SIZE);

        // Validate self hash.
        if (hThis != this.parentHash.getHashNumber())
            throw new RuntimeException("The kcActorBaseDesc reported the parent chunk as " + NumberUtils.to0PrefixedHexString(hThis) + ", but it was expected to be " + this.parentHash.getHashNumberAsString() + ".");

        // Resolve assets.
        GreatQuestUtils.resolveLevelResourceHash(kcCResourceGenericType.MODEL_DESCRIPTION, getParentFile(), this, this.modelDescRef, modelDescHash, true);
        GreatQuestUtils.resolveLevelResourceHash(kcCResourceSkeleton.class, this, this.hierarchyRef, hierarchyHash, true);
        GreatQuestUtils.resolveLevelResourceHash(kcCResourceAnimSet.class, this, this.animSetRef, animSetHash, true);
        GreatQuestUtils.resolveLevelResourceHash(kcCResourceGenericTypeGroup.PROXY_DESCRIPTION, getParentFile(), this, this.proxyDescRef, proxyDescHash, !isParentResourceNamed("Dummy", "Tree 8", "Tree 9")); // There are only 3 places this doesn't resolve, all in Rolling Rapids Creek (PC version, PS2 untested).
        if (!GreatQuestUtils.resolveLevelResourceHash(kcCResourceNamedHash.class, this, this.animationSequencesRef, animationHash, false) && animationHash != -1) // There are TONS of hashes set which correspond to sequences which don't exist.
            this.animationSequencesRef.setOriginalString(getResource().getName() + kcCResourceNamedHash.NAME_SUFFIX); // If we don't resolve the asset, we can at least apply the original string.

        applyNameToDescriptions(); // TODO: On name change, update linked names.
    }

    @Override
    public void saveData(DataWriter writer) {
        super.saveData(writer);

        // Unless the hash number is -1, it seems this is ALWAYS the resource name + "{seqs}", so ensure we save it like that.
        // But we'll only do that if there's no resource resolved, since we don't want to overwrite that.
        if (getResource() != null && getResource().getResourceName() != null && this.animationSequencesRef.getHashNumber() != -1 && this.animationSequencesRef.getResource() == null)
            this.animationSequencesRef.setHash(getResource().getName() + kcCResourceNamedHash.NAME_SUFFIX);

        writer.writeInt(this.parentHash.getHashNumber());
        writer.writeInt(this.modelDescRef.getHashNumber());
        writer.writeInt(this.hierarchyRef.getHashNumber());
        writer.writeInt(this.channelCount);
        writer.writeInt(this.animSetRef.getHashNumber());
        writer.writeInt(this.proxyDescRef.getHashNumber());
        writer.writeInt(this.animationSequencesRef.getHashNumber());
        writer.writeNull(PADDING_VALUES * Constants.INTEGER_SIZE);
    }

    @Override
    public void writeMultiLineInfo(StringBuilder builder, String padding) {
        super.writeMultiLineInfo(builder, padding);
        builder.append(padding).append("Hash: ").append(this.parentHash.getHashNumberAsString()).append(Constants.NEWLINE);
        writeAssetLine(builder, padding, "Model", this.modelDescRef);
        writeAssetLine(builder, padding, "Animation Hierarchy", this.hierarchyRef);
        if (this.channelCount != DEFAULT_ANIMATION_CHANNEL_COUNT)
            builder.append(padding).append("Channel Count: ").append(this.channelCount).append(Constants.NEWLINE);
        writeAssetLine(builder, padding, "Anim Set", this.animSetRef);
        if (this instanceof CItemDesc) {
            builder.append(padding).append("Collision Proxy: {HARDCODED ITEM PROXY}").append(Constants.NEWLINE);
        } else {
            writeAssetLine(builder, padding, "Collision Proxy", this.proxyDescRef);
        }
        writeAssetLine(builder, padding, "Animation List", this.animationSequencesRef);
    }

    @Override
    public kcCResourceGenericType getResourceType() {
        return kcCResourceGenericType.ACTOR_BASE_DESCRIPTION;
    }

    /**
     * Return the model description referenced in this description.
     * @return modelDesc
     */
    public kcModelDesc getModelDescription() {
        return this.modelDescRef.getResource() != null ? this.modelDescRef.getResource().getAsModelDescription() : null;
    }

    /**
     * Return the collision proxy description referenced in this description.
     * @return collisionProxyDescription
     */
    public kcProxyDesc getCollisionProxyDescription() {
        return this.proxyDescRef.getResource() != null ? this.proxyDescRef.getResource().getAsProxyDescription() : null;
    }

    /**
     * Gets the skeleton used for animating the actor.
     */
    public kcCResourceSkeleton getSkeleton() {
        return this.hierarchyRef.getResource();
    }

    /**
     * Gets the animation set containing the animations available to the actor.
     * MIGHT be unused, not sure yet.
     */
    public kcCResourceAnimSet getAnimationSet() {
        return this.animSetRef.getResource();
    }

    /**
     * Gets the animation sequences which the actor can use, if there are any.
     */
    public kcCResourceNamedHash getAnimationSequences() {
        return this.animationSequencesRef.getResource();
    }

    @Override
    public void handleDoubleClick() {
        super.handleDoubleClick();
        openMeshViewer();
    }

    /**
     * Opens the mesh viewer using the actor description.
     */
    public void openMeshViewer() {
        MeshViewController.setupMeshViewer(getGameInstance(), new GreatQuestModelViewController(getGameInstance()), createModelMesh());
    }

    /**
     * Creates a model mesh which can be animated.
     */
    public GreatQuestModelMesh createModelMesh() {
        kcModelDesc modelDesc = getModelDescription();
        kcCResourceAnimSet animSet = getAnimationSet();
        kcCResourceSkeleton skeleton = getSkeleton();

        kcCResourceModel model = modelDesc != null ? modelDesc.getModel() : null;
        kcModelWrapper modelWrapper = model != null ? model.getModelWrapper() : null;
        String name = model != null ? model.getFileName() : getParentHash().getOriginalString();

        List<kcCResourceTrack> animations = animSet != null ? animSet.getAnimations() : null;

        kcCResourceNamedHash namedHash = getAnimationSequences();
        return new GreatQuestModelMesh(modelWrapper, skeleton, animations, namedHash, name);
    }

    private static final String CONFIG_KEY_MODEL_DESC = "modelDesc";
    private static final String CONFIG_KEY_PROXY_DESC = "proxyDesc";
    private static final String CONFIG_KEY_HIERARCHY = "skeleton";
    private static final String CONFIG_KEY_CHANNEL_COUNT = "channelCount";
    private static final String CONFIG_KEY_ANIMATION_SET = "animationSet";
    private static final String CONFIG_KEY_ACTION_SEQUENCES = "actionSequenceTable";

    private static final DecimalFormat SPHERE_RADIUS_FORMAT = new DecimalFormat("0.####");
    static {
        SPHERE_RADIUS_FORMAT.setRoundingMode(RoundingMode.CEILING);
    }

    @Override
    public void fromConfig(ILogger logger, Config input) {
        super.fromConfig(logger, input);
        resolveResource(logger, input.getOptionalKeyValueNode(CONFIG_KEY_MODEL_DESC), kcCResourceGenericType.MODEL_DESCRIPTION, this.modelDescRef);
        resolveResource(logger, input.getOptionalKeyValueNode(CONFIG_KEY_PROXY_DESC), kcCResourceGenericTypeGroup.PROXY_DESCRIPTION, this.proxyDescRef);
        resolveResource(logger, input.getOptionalKeyValueNode(CONFIG_KEY_HIERARCHY), kcCResourceSkeleton.class, this.hierarchyRef);
        resolveResource(logger, input.getOptionalKeyValueNode(CONFIG_KEY_ANIMATION_SET), kcCResourceAnimSet.class, this.animSetRef);
        resolveResource(logger, input.getOptionalKeyValueNode(CONFIG_KEY_ACTION_SEQUENCES), kcCResourceNamedHash.class, this.animationSequencesRef);
        ConfigValueNode channelCountNode = input.getOptionalKeyValueNode(CONFIG_KEY_CHANNEL_COUNT);
        if (channelCountNode != null)
            this.channelCount = channelCountNode.getAsInteger();

        validateBoundingSphere(logger, input);
    }

    private void validateBoundingSphere(ILogger logger, Config input) {
        kcCResourceGeneric proxyDescResource = this.proxyDescRef.getResource();
        if (proxyDescResource == null)
            return;

        kcProxyDesc proxyDesc = proxyDescResource.getAsProxyDescription();
        if (proxyDesc == null)
            return;

        float minimumRadius = proxyDesc.getMinimumSphereRadius(getBoundingSphere().getPosition());
        float actualRadius = getBoundingSphere().getRadius();
        if (actualRadius >= minimumRadius)
            return;

        if (!input.hasKeyValueNode(CONFIG_KEY_BOUNDING_SPHERE_RADIUS)) { // If the radius isn't set, we automatically take care of it.
            getBoundingSphere().setRadius(minimumRadius);
        } else { // If the radius was explicitly set, and is too small, then we should let the user know.
            logger.warning("The %s must be at least %s to contain the collision proxy: '%s'. (Was: %f)",
                    CONFIG_KEY_BOUNDING_SPHERE_RADIUS, SPHERE_RADIUS_FORMAT.format(minimumRadius),
                    proxyDesc.getResourceName(), actualRadius);
        }
    }

    @Override
    public void toConfig(Config output, kcScriptDisplaySettings settings) {
        super.toConfig(output, settings);
        output.getOrCreateKeyValueNode(CONFIG_KEY_MODEL_DESC).setAsString(this.modelDescRef.getAsGqsString(settings));
        output.getOrCreateKeyValueNode(CONFIG_KEY_PROXY_DESC).setAsString(this.proxyDescRef.getAsGqsString(settings));
        output.getOrCreateKeyValueNode(CONFIG_KEY_HIERARCHY).setAsString(this.hierarchyRef.getAsGqsString(settings));
        if (this.channelCount != DEFAULT_ANIMATION_CHANNEL_COUNT)
            output.getOrCreateKeyValueNode(CONFIG_KEY_CHANNEL_COUNT).setAsInteger(this.channelCount);
        output.getOrCreateKeyValueNode(CONFIG_KEY_ANIMATION_SET).setAsString(this.animSetRef.getAsGqsString(settings));
        output.getOrCreateKeyValueNode(CONFIG_KEY_ACTION_SEQUENCES).setAsString(this.animationSequencesRef.getAsGqsString(settings));
    }

    private void applyNameToDescriptions() {
        if (getResource() == null)
            return;

        // If we resolve the model successfully, our goal is to generate the name of any corresponding collision mesh.
        String baseName = getResource().getName();
        if (baseName.endsWith(NAME_SUFFIX))
            baseName = baseName.substring(0, baseName.length() - NAME_SUFFIX.length());

        String modelDescName = baseName + kcModelDesc.NAME_SUFFIX;
        int testHash = GreatQuestUtils.hash(modelDescName);
        if (this.modelDescRef.getHashNumber() == testHash && this.modelDescRef.getResource() != null)
            this.modelDescRef.getResource().getSelfHash().setOriginalString(modelDescName);

        String proxyDescName = baseName + kcProxyDesc.NAME_SUFFIX;
        testHash = GreatQuestUtils.hash(proxyDescName);
        if (this.proxyDescRef.getHashNumber() == testHash && this.proxyDescRef.getResource() != null)
            this.proxyDescRef.getResource().getSelfHash().setOriginalString(proxyDescName);
    }
}