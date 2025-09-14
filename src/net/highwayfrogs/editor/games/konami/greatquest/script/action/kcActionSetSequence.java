package net.highwayfrogs.editor.games.konami.greatquest.script.action;

import lombok.Getter;
import net.highwayfrogs.editor.games.konami.greatquest.GreatQuestHash;
import net.highwayfrogs.editor.games.konami.greatquest.GreatQuestUtils;
import net.highwayfrogs.editor.games.konami.greatquest.chunks.kcCResource;
import net.highwayfrogs.editor.games.konami.greatquest.chunks.kcCResourceNamedHash;
import net.highwayfrogs.editor.games.konami.greatquest.chunks.kcCResourceNamedHash.HashTableEntry;
import net.highwayfrogs.editor.games.konami.greatquest.entity.kcActorBaseDesc;
import net.highwayfrogs.editor.games.konami.greatquest.script.interim.kcParamReader;
import net.highwayfrogs.editor.games.konami.greatquest.script.interim.kcParamWriter;
import net.highwayfrogs.editor.games.konami.greatquest.script.*;
import net.highwayfrogs.editor.utils.NumberUtils;
import net.highwayfrogs.editor.utils.logging.ILogger;
import net.highwayfrogs.editor.utils.objects.OptionalArguments;

/**
 * Implements setting an action sequence.
 * Created by Kneesnap on 10/29/2024.
 */
public class kcActionSetSequence extends kcAction {
    @Getter private final GreatQuestHash<kcCActionSequence> sequenceRef = new GreatQuestHash<>();
    @Getter private boolean ignoreIfAlreadyActive;
    @Getter private boolean openBoneChannel;
    private HashTableEntry sequenceEntry;

    private static final kcArgument[] ARGUMENTS = kcArgument.make(kcParamType.HASH_NULL_IS_ZERO, "sequence", kcParamType.BOOLEAN, "ignoreIfAlreadyActive", kcParamType.BOOLEAN, "openBoneChannel");
    private static final String ARGUMENT_IGNORE_IF_ALREADY_ACTIVE = "IgnoreIfAlreadyActive"; // This controls whether we will reset the sequence if it is already playing.
    private static final String ARGUMENT_OPEN_BONE_CHANNEL = "OpenBoneChannel";

    public kcActionSetSequence(kcActionExecutor executor, kcActionID action) {
        super(executor, action);
        this.sequenceRef.setNullRepresentedAsZero();
    }

    @Override
    public kcArgument[] getArgumentTemplate(kcParam[] arguments) {
        return ARGUMENTS;
    }

    @Override
    public void load(kcParamReader reader) {
        setAnimationSequenceHash(reader.next().getAsInteger());
        this.ignoreIfAlreadyActive = reader.next().getAsBoolean();
        this.openBoneChannel = reader.next().getAsBoolean();
    }

    @Override
    public void save(kcParamWriter writer) {
        writer.write(this.sequenceRef.getHashNumber());
        writer.write(this.ignoreIfAlreadyActive);
        writer.write(this.openBoneChannel);
    }

    @Override
    public int getGqsArgumentCount(kcArgument[] argumentTemplates) {
        return 1;
    }

    @Override
    protected void loadArguments(OptionalArguments arguments) {
        String sequenceName = arguments.useNext().getAsString(); // We can't resolve the sequence by the hash of the string normally since these seem to use randomized hash values.
        if (NumberUtils.isHexInteger(sequenceName)) {
            setAnimationSequenceHash(NumberUtils.parseHexInteger(sequenceName));
        } else {
            kcCActionSequence foundSequence = null;

            // Use the actor description to search for the sequence, which might be in a different chunked file than this script.
            HashTableEntry sequenceEntry = null;
            kcActorBaseDesc actorDesc = getExecutor() != null ? getExecutor().getExecutingActorBaseDescription() : null;
            if (actorDesc != null) {
                kcCResourceNamedHash sequenceTable = actorDesc.getAnimationSequences();
                if (sequenceTable != null) {
                    sequenceEntry = sequenceTable.getEntryByName(sequenceName);
                    if (sequenceEntry != null)
                        foundSequence = sequenceEntry.getSequence();
                }

                // Search the chunked file where the actor definition was found, if it's not the one where this action was found.
                // Only works for fully qualified names.
                // Ie: "blab01" would not be found, but "Frogmother[blab01]" would be found.
                if (foundSequence == null && getChunkedFile() != actorDesc.getParentFile()) {
                    for (kcCResource chunk : actorDesc.getParentFile().getChunks()) {
                        if (chunk instanceof kcCActionSequence && chunk.getName().equalsIgnoreCase(sequenceName)) {
                            foundSequence = (kcCActionSequence) chunk;
                            break;
                        }
                    }
                }
            }

            // Last Resort: Search the chunked file we're in. Only works for fully qualified names.
            // Ie: "blab01" would not be found, but "Frogmother[blab01]" would be found.
            if (foundSequence == null) {
                for (kcCResource chunk : getChunkedFile().getChunks()) {
                    if (chunk instanceof kcCActionSequence && chunk.getName().equalsIgnoreCase(sequenceName)) {
                        foundSequence = (kcCActionSequence) chunk;
                        break;
                    }
                }
            }

            // Resolve the sequence hash, while also tracking the sequence name in-case of error.
            int newHash = foundSequence != null ? foundSequence.getHash() : (sequenceEntry != null ? sequenceEntry.getValueRef().getHashNumber() : 0);
            this.sequenceRef.setHash(newHash, sequenceName, false);
            setAnimationSequenceHash(newHash);
        }

        this.ignoreIfAlreadyActive = arguments.useFlag(ARGUMENT_IGNORE_IF_ALREADY_ACTIVE);
        this.openBoneChannel = arguments.useFlag(ARGUMENT_OPEN_BONE_CHANNEL);
    }

    @Override
    protected void saveArguments(OptionalArguments arguments, kcScriptDisplaySettings settings) {
        if (this.sequenceRef.getResource() != null) {
            arguments.createNext().setAsString(this.sequenceRef.getResource().getSequenceName(), true);
        } else if (this.sequenceEntry != null) {
            arguments.createNext().setAsString(this.sequenceEntry.getKeyName(), true);
        } else {
            this.sequenceRef.applyGqsString(arguments.createNext(), settings);
        }

        if (this.ignoreIfAlreadyActive)
            arguments.getOrCreate(ARGUMENT_IGNORE_IF_ALREADY_ACTIVE);
        if (this.openBoneChannel)
            arguments.getOrCreate(ARGUMENT_OPEN_BONE_CHANNEL);
    }

    @Override
    public String getEndOfLineComment() {
        if (this.sequenceRef.getResource() != null) {
            return this.sequenceRef.getResource().getName();
        } else {
            return super.getEndOfLineComment();
        }
    }

    @Override
    public void printWarnings(ILogger logger) {
        super.printWarnings(logger);
        kcActorBaseDesc actorDesc = getExecutor() != null ? getExecutor().getExecutingActorBaseDescription() : null;
        String entityName = getExecutor() != null ? getExecutor().getName() : null;
        String entityDescName = actorDesc != null && actorDesc.getResource() != null ? actorDesc.getResource().getName() : null;
        if (actorDesc == null) {
            logger.warning("The action '%s' will CRASH the game, since the entity %s cannot resolve its kcEntityDesc.", getAsGqsStatement(), entityName);
        } else if (actorDesc.getSkeleton() == null) {
            logger.warning("The action '%s' may CRASH the game, since the description %s cannot resolve its skeleton.", getAsGqsStatement(), entityDescName);
        } else if (actorDesc.getAnimationSequences() == null) {
            logger.warning("The action '%s' may CRASH the game, since the description %s cannot resolve its kcCResourceNamedHash.", getAsGqsStatement(), entityDescName);
        } else if (this.sequenceRef.getResource() == null && this.sequenceEntry == null) {
            printWarning(logger, "no sequence could be found with that name.");
        }
    }

    /**
     * Resolves a new animation sequence hash for this action.
     * @param newAnimationSequenceHash The hash of sequence we'd like to apply when running this action.
     */
    public void setAnimationSequenceHash(int newAnimationSequenceHash) {
        this.sequenceEntry = null;
        if (GreatQuestUtils.resolveLevelResourceHash(kcCActionSequence.class, getChunkedFile(), this, this.sequenceRef, newAnimationSequenceHash, false))
            return; // Successfully resolved hash.

        kcActorBaseDesc actorDesc = getExecutor() != null ? getExecutor().getExecutingActorBaseDescription() : null;
        if (actorDesc == null)
            return;

        kcCResourceNamedHash sequenceTable = actorDesc.getAnimationSequences();
        if (sequenceTable == null)
            return;

        for (int i = 0; i < sequenceTable.getEntries().size(); i++) {
            HashTableEntry entry = sequenceTable.getEntries().get(i);
            if (entry.getValueRef().getHashNumber() == newAnimationSequenceHash) {
                this.sequenceEntry = entry;
                return;
            }
        }
    }
}
