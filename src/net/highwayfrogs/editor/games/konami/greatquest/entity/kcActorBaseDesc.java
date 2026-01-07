package net.highwayfrogs.editor.games.konami.greatquest.entity;

import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.games.konami.greatquest.GreatQuestAssetUtils;
import net.highwayfrogs.editor.games.konami.greatquest.GreatQuestHash;
import net.highwayfrogs.editor.games.konami.greatquest.GreatQuestUtils;
import net.highwayfrogs.editor.games.konami.greatquest.chunks.*;
import net.highwayfrogs.editor.games.konami.greatquest.generic.ILateResourceResolver;
import net.highwayfrogs.editor.games.konami.greatquest.generic.kcCResourceGeneric;
import net.highwayfrogs.editor.games.konami.greatquest.generic.kcCResourceGeneric.kcCResourceGenericType;
import net.highwayfrogs.editor.games.konami.greatquest.generic.kcCResourceGeneric.kcCResourceGenericTypeGroup;
import net.highwayfrogs.editor.games.konami.greatquest.model.kcModelDesc;
import net.highwayfrogs.editor.games.konami.greatquest.model.kcModelWrapper;
import net.highwayfrogs.editor.games.konami.greatquest.proxy.kcProxyDesc;
import net.highwayfrogs.editor.games.konami.greatquest.script.kcCActionSequence;
import net.highwayfrogs.editor.games.konami.greatquest.script.kcScriptDisplaySettings;
import net.highwayfrogs.editor.games.konami.greatquest.ui.mesh.model.GreatQuestModelMesh;
import net.highwayfrogs.editor.games.konami.greatquest.ui.mesh.model.GreatQuestModelViewController;
import net.highwayfrogs.editor.gui.components.propertylist.PropertyListNode;
import net.highwayfrogs.editor.gui.editor.MeshViewController;
import net.highwayfrogs.editor.system.Config;
import net.highwayfrogs.editor.system.Config.ConfigValueNode;
import net.highwayfrogs.editor.utils.FXUtils;
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
public class kcActorBaseDesc extends kcEntity3DDesc implements ILateResourceResolver {
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
        this.proxyDescRef = new GreatQuestHash<>(); // kcCActorBase::Init() TODO: This being null CAN cause crashes, but not always? The circumstances of this crash are unclear.
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
            this.animationSequencesRef.setOriginalString(getResourceName() + kcCResourceNamedHash.NAME_SUFFIX); // If we don't resolve the asset, we can at least apply the original string.

        applyNameToDescriptions();
    }

    @Override
    public void saveData(DataWriter writer) {
        super.saveData(writer);

        // Unless the hash number is -1, it seems this is ALWAYS the resource name + "{seqs}", so ensure we save it like that.
        // But we'll only do that if there's no resource resolved, since we don't want to overwrite that.
        if (getResource() != null && getResource().getResourceName() != null && this.animationSequencesRef.getHashNumber() != -1 && this.animationSequencesRef.getResource() == null)
            this.animationSequencesRef.setHash(getResourceName() + kcCResourceNamedHash.NAME_SUFFIX);

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
    public void addToPropertyList(PropertyListNode propertyList) {
        super.addToPropertyList(propertyList);
        this.modelDescRef.addToPropertyList(propertyList, "Model Description", getParentFile(), kcCResourceGenericType.MODEL_DESCRIPTION);
        this.hierarchyRef.addToPropertyList(propertyList, "Skeleton", getParentFile(), kcCResourceSkeleton.class);
        propertyList.addInteger("Channel Count", this.channelCount, newValue -> newValue > 0, newChannelCount -> this.channelCount = newChannelCount);
        this.animSetRef.addToPropertyList(propertyList, "Animation Set", getParentFile(), kcCResourceAnimSet.class);
        if (this instanceof CItemDesc) {
            propertyList.add("Collision Proxy", "{HARDCODED ITEM PROXY}");
        } else {
            this.proxyDescRef.addToPropertyList(propertyList, "Collision Proxy", getParentFile(), kcCResourceGenericTypeGroup.PROXY_DESCRIPTION);
        }

        this.animationSequencesRef.addToPropertyList(propertyList, "Action Sequences", getParentFile(), kcCResourceNamedHash.class);
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

    @Override
    public void resolvePendingResources(ILogger logger) {
        // Warn about invisible model descriptions.
        kcModelDesc modelDesc = getModelDescription();
        kcCResourceModel resourceModel;
        if (modelDesc == null) {
            // If this model hash is null, this was probably intentionally left blank.
            if (!this.modelDescRef.isHashNull() && !resolveResource(logger, kcCResourceGenericType.MODEL_DESCRIPTION, this.modelDescRef, false))
                logger.warning("The %s was not found for entity description '%s', so any entities using it will be invisible! (%s)", CONFIG_KEY_MODEL_DESC, getResourceName(), this.modelDescRef.getAsGqsString(null));
        } else if ((resourceModel = modelDesc.getModel()) == null) {
            logger.warning("The %s named '%s' could not find the kcCResourceModel %s, so any entities using %s will be invisible!", CONFIG_KEY_MODEL_DESC, modelDesc.getResourceName(), modelDesc.getModelRef().getAsString(), getResourceName());
        } else if (resourceModel.getModel() == null) {
            logger.warning("The kcCResourceModel named '%s' could not resolve the file path '%s', so any entities using %s will be invisible!", resourceModel.getName(), resourceModel.getFullPath(), getResourceName());
        }

        if (getSkeleton() == null && !this.hierarchyRef.isHashNull() && !resolveResource(logger, kcCResourceSkeleton.class, this.hierarchyRef, false))
            logger.warning("The entity description '%s' could not resolve its skeleton! (Skeleton: %s)", getResourceName(), this.hierarchyRef.getAsGqsString(null));

        if (getAnimationSet() == null && !this.animSetRef.isHashNull() && !resolveResource(logger, kcCResourceAnimSet.class, this.animSetRef, false))
            logger.warning("The entity description '%s' could not resolve its animation set! (Animation Set: %s)", getResource(), this.animSetRef.getAsGqsString(null));

        if (getCollisionProxyDescription() == null && !this.proxyDescRef.isHashNull() && !resolveResource(logger, kcCResourceGenericTypeGroup.PROXY_DESCRIPTION, this.proxyDescRef, false))
            logger.warning("The entity description '%s' could not resolve its collision proxy description! (Collision Proxy: %s)", getResource(), this.proxyDescRef.getAsGqsString(null));

        if (getAnimationSequences() == null && !this.animationSequencesRef.isHashNull() && !resolveResource(logger, kcCResourceNamedHash.class, this.animationSequencesRef, false))
            logger.warning("The entity description '%s' could not resolve its sequence table! (Sequence Tables: %s)", getResource(), this.animationSequencesRef.getAsGqsString(null));
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

    @Override
    public void setupRightClickMenuItems(ContextMenu contextMenu) {
        super.setupRightClickMenuItems(contextMenu);

        MenuItem copyGqsItem = new MenuItem("Apply [" + GreatQuestAssetUtils.CONFIG_SECTION_COPY_RESOURCES + "] to Clipboard");
        contextMenu.getItems().add(copyGqsItem);
        copyGqsItem.setOnAction(event -> {
            Config config = toCopyResourcesConfig();
            String configText = config.toString();
            FXUtils.setClipboardText(configText);
            FXUtils.showPopup(AlertType.INFORMATION, "Applied GQS to clipboard.", configText);
        });
    }

    /**
     * Creates a gqs copy resources config containing everything the entity needs.
     */
    public Config toCopyResourcesConfig() {
        Config root = new Config("clipboard");
        Config copyResources = new Config(GreatQuestAssetUtils.CONFIG_SECTION_COPY_RESOURCES);
        root.addChildConfig(copyResources);
        Config levelNode = new Config(getParentFile().getFilePath());
        copyResources.addChildConfig(levelNode);

        levelNode.getInternalText().add(new ConfigValueNode(getResourceName()));
        if (getModelDescription() != null)
            levelNode.getInternalText().add(new ConfigValueNode(getModelDescription().getResourceName()));
        if (getSkeleton() != null)
            levelNode.getInternalText().add(new ConfigValueNode(getSkeleton().getName()));
        if (getAnimationSet() != null)
            levelNode.getInternalText().add(new ConfigValueNode(getAnimationSet().getName()));
        if (getCollisionProxyDescription() != null)
            levelNode.getInternalText().add(new ConfigValueNode(getCollisionProxyDescription().getResourceName()));
        if (getAnimationSequences() != null)
            levelNode.getInternalText().add(new ConfigValueNode(getAnimationSequences().getName()));

        // Add animations
        List<kcCResourceTrack> animations = GreatQuestModelMesh.getAnimations(this);
        if (animations.size() > 0) {
            levelNode.getInternalText().add(new ConfigValueNode(""));
            for (int i = 0; i < animations.size(); i++)
                levelNode.getInternalText().add(new ConfigValueNode(animations.get(i).getName()));
        }

        // Add sequences.
        List<kcCActionSequence> sequences = GreatQuestModelMesh.getActionSequences(this);
        if (sequences.size() > 0) {
            levelNode.getInternalText().add(new ConfigValueNode(""));
            for (int i = 0; i < sequences.size(); i++)
                levelNode.getInternalText().add(new ConfigValueNode(sequences.get(i).getName()));
        }

        return root;
    }

    private void applyNameToDescriptions() {
        if (getResource() == null)
            return;

        // If we resolve the model successfully, our goal is to generate the name of any corresponding collision mesh.
        String baseName = getResourceName();
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