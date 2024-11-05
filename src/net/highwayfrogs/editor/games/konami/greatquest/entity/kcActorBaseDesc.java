package net.highwayfrogs.editor.games.konami.greatquest.entity;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.konami.greatquest.GreatQuestHash;
import net.highwayfrogs.editor.games.konami.greatquest.GreatQuestUtils;
import net.highwayfrogs.editor.games.konami.greatquest.chunks.*;
import net.highwayfrogs.editor.games.konami.greatquest.chunks.kcCResourceNamedHash.HashTableEntry;
import net.highwayfrogs.editor.games.konami.greatquest.generic.kcCResourceGeneric;
import net.highwayfrogs.editor.games.konami.greatquest.generic.kcCResourceGeneric.kcCResourceGenericType;
import net.highwayfrogs.editor.games.konami.greatquest.kcClassID;
import net.highwayfrogs.editor.games.konami.greatquest.model.kcModelDesc;
import net.highwayfrogs.editor.games.konami.greatquest.model.kcModelWrapper;
import net.highwayfrogs.editor.games.konami.greatquest.proxy.kcProxyDesc;
import net.highwayfrogs.editor.games.konami.greatquest.ui.mesh.model.GreatQuestModelMesh;
import net.highwayfrogs.editor.games.konami.greatquest.ui.mesh.model.GreatQuestModelViewController;
import net.highwayfrogs.editor.gui.editor.MeshViewController;
import net.highwayfrogs.editor.utils.NumberUtils;

import java.util.ArrayList;
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
    private int channelCount = 2; // Used for initializing the skeleton hierarchy in kcCActorBase::Init, this is always seen to be two.
    private final GreatQuestHash<kcCResourceAnimSet> animSetRef; // I've done an extensive search and am confident that this is completely unused.
    private final GreatQuestHash<kcCResourceGeneric> proxyDescRef; // kcCActorBase::Init() will fail if this is not either -1 or a valid hash.
    private final GreatQuestHash<kcCResourceNamedHash> animationSequencesRef; // hAnimHash, kcCActorBase::Init, kcCActorBase::ResetInt TODO: The sequence 'NrmIdle01' is applied by kcCActorBase::ResetInt, if the sequence is found. Does this occur on level load? If so, we might want to put that in the editor. It appears repeating the sequence is enabled, so consider that in the editor too. We should probably have a way to preview action sequences actually.
    private static final int PADDING_VALUES = 4;
    private static final String NAME_SUFFIX = "ActorDesc";

    public kcActorBaseDesc(@NonNull kcCResourceGeneric resource) {
        super(resource);
        this.parentHash = new GreatQuestHash<>(resource); // kcActorBaseDesc::__ct
        this.modelDescRef = new GreatQuestHash<>(); // kcCActorBase::__ct, kcCActorBase::Init()
        this.hierarchyRef = new GreatQuestHash<>(); // kcCActorBase::Init()
        this.animSetRef = new GreatQuestHash<>(); // kcCActorBase::__ct
        this.proxyDescRef = new GreatQuestHash<>(); // kcCActorBase::Init()
        this.animationSequencesRef = new GreatQuestHash<>(); // kcCActorBase::__ct
        GreatQuestUtils.applySelfNameSuffixAndToFutureNameChanges(resource, NAME_SUFFIX);
    }

    @Override
    protected int getTargetClassID() {
        return kcClassID.ACTOR_BASE.getClassId();
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
        GreatQuestUtils.resolveResourceHash(kcCResourceGeneric.class, this, this.modelDescRef, modelDescHash, true);
        GreatQuestUtils.resolveResourceHash(kcCResourceSkeleton.class, this, this.hierarchyRef, hierarchyHash, true);
        GreatQuestUtils.resolveResourceHash(kcCResourceAnimSet.class, this, this.animSetRef, animSetHash, true);
        GreatQuestUtils.resolveResourceHash(kcCResourceGeneric.class, this, this.proxyDescRef, proxyDescHash, !isParentResourceNamed("Dummy", "Tree 8", "Tree 9")); // There are only 3 places this doesn't resolve, all in Rolling Rapids Creek (PC version, PS2 untested).
        if (!GreatQuestUtils.resolveResourceHash(kcCResourceNamedHash.class, this, this.animationSequencesRef, animationHash, false) && animationHash != -1) // There are TONS of hashes set which correspond to sequences which don't exist. TODO: There are enough where I'm almost wondering if we should be automatically naming/resolving this
            this.animationSequencesRef.setOriginalString(getResource().getName() + kcCResourceNamedHash.NAME_SUFFIX); // If we don't resolve the asset, we can at least apply the original string.
    }

    @Override
    public void saveData(DataWriter writer) {
        super.saveData(writer);

        // Unless the hash number is -1, it seems this is ALWAYS the resource name + "{seqs}", so ensure we save it like that.
        if (getResource() != null && getResource().getResourceName() != null && this.animationSequencesRef.getHashNumber() != -1)
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
        builder.append(padding).append("Channel Count: ").append(this.channelCount).append(Constants.NEWLINE);
        writeAssetLine(builder, padding, "Anim Set", this.animSetRef);
        writeAssetLine(builder, padding, "Collision Proxy", this.proxyDescRef);
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
        MeshViewController.setupMeshViewer(getGameInstance(), new GreatQuestModelViewController(), createModelMesh());
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

        List<kcCResourceTrack> animations = animSet != null ? animSet.getAnimations() : new ArrayList<>();
        kcCResourceNamedHash namedHash = getAnimationSequences();
        if (namedHash != null) {
            for (int i = 0; i < namedHash.getEntries().size(); i++) {
                HashTableEntry entry = namedHash.getEntries().get(i);
                //kcCResourceTrack animation = entry.getValueRef()
            }
            // TODO: ADD ANIMATIONS.
        }


        return new GreatQuestModelMesh(modelWrapper, skeleton, animations, null, name, false);
    }
}