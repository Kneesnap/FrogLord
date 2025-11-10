package net.highwayfrogs.editor.games.konami.greatquest.script;

import lombok.Getter;
import net.highwayfrogs.editor.games.konami.greatquest.GreatQuestHash;
import net.highwayfrogs.editor.games.konami.greatquest.GreatQuestUtils;
import net.highwayfrogs.editor.games.konami.greatquest.chunks.*;
import net.highwayfrogs.editor.games.konami.greatquest.chunks.kcCResourceNamedHash.HashTableEntry;
import net.highwayfrogs.editor.games.konami.greatquest.entity.kcActorBaseDesc;
import net.highwayfrogs.editor.games.konami.greatquest.entity.kcEntity3DDesc;
import net.highwayfrogs.editor.games.konami.greatquest.generic.ILateResourceResolver;
import net.highwayfrogs.editor.games.konami.greatquest.generic.kcCResourceGeneric;
import net.highwayfrogs.editor.games.konami.greatquest.script.action.kcAction;
import net.highwayfrogs.editor.games.konami.greatquest.script.action.kcActionID;
import net.highwayfrogs.editor.games.konami.greatquest.script.action.kcActionSetAnimation;
import net.highwayfrogs.editor.gui.components.propertylist.PropertyListNode;
import net.highwayfrogs.editor.system.Config;
import net.highwayfrogs.editor.system.Config.ConfigValueNode;
import net.highwayfrogs.editor.system.Config.IllegalConfigSyntaxException;
import net.highwayfrogs.editor.utils.StringUtils;
import net.highwayfrogs.editor.utils.data.reader.DataReader;
import net.highwayfrogs.editor.utils.data.writer.DataWriter;
import net.highwayfrogs.editor.utils.logging.ILogger;
import net.highwayfrogs.editor.utils.objects.OptionalArguments;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents an action sequence definition.
 * Built-in actions:
 * AirIdle: CFrogCtl::OnExecuteAir
 * AirWalkRun: CFrogCtl::OnBeginJump, CFrogCtl::OnCaseClimb
 * Climb: CFrogCtl::OnExecuteClimb
 * ClimbDangle: CFrogCtl::OnExecuteClimb
 * ClimbL: CFrogCtl::OnExecuteClimb
 * ClimbR: CFrogCtl::OnExecuteClimb
 * ClimbTrnsRun: CFrogCtl::OnExecuteClimb
 * Dive: CFrogCtl::OnExecuteAir
 * FalWalk02: CFrogCtl::OnExecuteAir
 * FlyIdle01: CCharacter::Update
 * FlyWalk01: CCharacter::Update
 * HighFall: CFrogCtl::OnExecuteAir
 * HighFallEnd: CFrogCtl::OnExecuteLand
 * JumpIdle: CFrogCtl::OnBeginSwimJump, CFrogCtl::OnBeginJump
 * LandIdle: CFrogCtl::OnExecuteLand
 * LandWalkRun: CFrogCtl::OnExecuteLand
 * NrmAtk01: CFrogCtl::OnBeginMelee
 * NrmAtk03: CFrogCtl::CheckForHealthBug
 * NrmAtk04: CFrogCtl::OnBeginMelee
 * NrmAtk05: CFrogCtl::OnBeginMelee
 * NrmAtk06: CFrogCtl::OnBeginMelee
 * NrmAtk07: CFrogCtl::OnBeginMelee
 * NrmDie01: CFrogCtl::OnBeginDying, CCharacter::Update
 * NrmDodgL01: CFrogCtl::OnExecuteDodge
 * NrmDodgR01: CFrogCtl::OnExecuteDodge
 * NrmIdle01: CFrogCtl::OnExecuteDodge, CFrogCtl::OnCaseClimb, CFrogCtl::OnExecuteIdle, CFrogCtl::OnBeginIdle, CFrogCtl::OnCaseIdle, CCharacter::Update <- Also appears to be the default pose.
 * NrmIdle02: CFrogCtl::OnExecuteIdle
 * NrmIdle03: CFrogCtl::OnExecuteIdle
 * NrmIdle04: CFrogCtl::OnExecuteIdle
 * NrmIdle05: CFrogCtl::OnExecuteIdle
 * NrmIdle06: CFrogCtl::OnExecuteIdle
 * NrmIdle08: CFrogCtl::OnExecuteIdle
 * NrmPup01: CCharacter::OnPickup
 * NrmReac01: CFrogCtl::OnBeginDamage
 * NrmReac02: CFrogCtl::OnBeginDamage
 * NrmRng01: CFrogCtl::OnBeginMissile
 * NrmRng04: CFrogCtl::OnBeginMagicStone
 * NrmRun01: CFrogCtl::OnBeginRun
 * NrmWalk01: CFrogCtl::OnBeginWalk, CCharacter::Update
 * NrmWalk02: CFrogCtl::OnBeginWalk, CFrogCtl::OnCaseIdle
 * Roll: CFrogCtl::CollisionCallback, CFrogCtl::CanJump
 * Spit: CFrogCtl::OnBeginMissile, CFrogCtl::OnExecuteSwim
 * Swim: CFrogCtl::OnExecuteSwim, CFrogCtl::OnBeginSwim
 * SwimIdle: CFrogCtl::OnExecuteDodge, CFrogCtl::OnExecuteAir
 * SwimIdle01: CFrogCtl::OnExecuteSwim, CFrogCtl::OnExecuteSwimSurface, CFrogCtl::OnBeginSwimSurface, CFrogCtl::OnEndAir
 * SwmAtk01: CFrogCtl::CheckForHealthBug
 * SwmAtk02: CFrogCtl::CheckForHealthBug
 * SwmIdle01: CCharacter::Update
 * SwmReac01: CFrogCtl::OnBeginDamage
 * SwmTrnL: CFrogCtl::OnExecuteSwim, CCharacter::Update
 * SwmTrnR: CFrogCtl::OnExecuteSwim, CCharacter::Update
 * SwimWalk01: CFrogCtl::OnExecuteSwim, CCharacter::Update
 * SwimWalk03: CFrogCtl::OnExecuteSwimSurface
 * ThroatFloat: CFrogCtl::OnBeginGlide
 *
 * AISystemClass::Process also can call SetSequence for MonsterClass->TransitionSequence.
 *  -> MonsterClass::Init sets TransitionSequence to "None".
 *
 *  -> MonsterClass::TransitionTo is capable of building animation names itself. Here's the core logic:
 *  1) Write 'Nrm', 'Fly', or 'Swm' based on some bit flags.
 *  2) Take the passed actiontype, and get the next string from the array:
 *   ["None", "Idle", "Walk", "Run", "Atk", "Tnt", "Reac", "Rng", "Spel", "Idle", "Die", "Talk", "Slp", "Spc", "XXX", NULL]
 *  3) Then write format("%-02.02d", num)
 *  4) Then, if the current goal is 0x02 && actiontype <= 2, append "Agg"
 *  The resulting string is then stored into TransitionSequence or NextTransitionSequence, so that it will be the new sequence played.
 *
 * Created by Kneesnap on 3/23/2020.
 */

public class kcCActionSequence extends kcCResource implements kcActionExecutor, ILateResourceResolver {
    @Getter private final List<kcAction> actions = new ArrayList<>();
    private final GreatQuestHash<kcCResourceGeneric> cachedActorBaseDescRef = new GreatQuestHash<>();

    public static final String HASH_CONFIG_FIELD = "hash";

    public kcCActionSequence(GreatQuestChunkedFile parentFile) {
        super(parentFile, KCResourceID.ACTIONSEQUENCE);
    }

    @Override
    public void load(DataReader reader) {
        super.load(reader);
        this.actions.clear();
        while (reader.hasMore())
            this.actions.add(kcAction.readAction(reader, this));
    }

    @Override
    public void save(DataWriter writer) {
        super.save(writer);
        for (int i = 0; i < this.actions.size(); i++)
            this.actions.get(i).save(writer);
    }

    /**
     * Gets the name of the sequence without the name of the entity.
     */
    public String getEntityDescriptionName() {
        String baseName = getName();
        if (!baseName.endsWith("]"))
            throw new IllegalArgumentException("Cannot determine entityDesc name from '" + baseName + "'.");

        int openIndex = baseName.lastIndexOf('[');
        if (openIndex < 0)
            throw new IllegalArgumentException("Cannot determine entityDesc name from '" + baseName + "'.");

        return baseName.substring(0, openIndex);
    }

    /**
     * Gets the name of the sequence without the name of the entity.
     */
    public String getSequenceName() {
        String baseName = getName();
        if (!baseName.endsWith("]"))
            throw new IllegalArgumentException("Cannot determine sequence name from '" + baseName + "'.");

        int openIndex = baseName.lastIndexOf('[');
        if (openIndex < 0)
            throw new IllegalArgumentException("Cannot determine sequence name from '" + baseName + "'.");

        return baseName.substring(openIndex + 1, baseName.length() - 1);
    }

    /**
     * Loads the sequence from a Config node.
     * @param config The config to load the script from
     */
    public void loadFromConfigNode(Config config, ILogger logger) {
        if (config == null)
            throw new NullPointerException("config");

        if (!config.getSectionName().equalsIgnoreCase(getSequenceName()))
            logger.warning("Provided sequence name '%s' did not match the real sequence resource '%s'.", config.getSectionName(), getSequenceName());

        // Read actions.
        this.actions.clear();
        int lineNumber = 0;
        String fileName = config.getRootNode().getSectionName();
        for (ConfigValueNode line : config.getTextNodes()) {
            lineNumber++;
            String textLine = line.getAsStringLiteral();
            if (StringUtils.isNullOrWhiteSpace(textLine))
                continue;

            OptionalArguments arguments = OptionalArguments.parse(textLine);

            String commandName = arguments.useNext().getAsString();

            // Resolve kcActionEffect.
            kcActionID actionID = kcActionID.getActionByCommandName(commandName);
            if (actionID == null)
                throw new IllegalArgumentException("Could not identify the action '" + commandName + "' from '" + textLine + "' when importing sequence " + getName() + ".");

            kcAction newAction = actionID.newInstance(this);
            try {
                newAction.load(logger, arguments, lineNumber, fileName);
            } catch (Throwable th) {
                throw new IllegalConfigSyntaxException("Could not parse the action '" + line + "' when importing sequence " + getName() + ".", th);
            }

            this.actions.add(newAction);
            newAction.printWarnings(logger);
            arguments.warnAboutUnusedArguments(logger);
        }
    }

    /**
     * Saves the sequence to a gqs-compliant config node.
     * @param logger the logger to write messages to
     * @param settings the settings to save the sequence with
     * @return configNode
     */
    public Config saveToConfigNode(ILogger logger, kcScriptDisplaySettings settings) {
        Config result = new Config(getSequenceName()); // Use the key name instead of the sequence name, since this is what it is resolved by.
        ConfigValueNode hashNode = result.getOrCreateKeyValueNode(HASH_CONFIG_FIELD);
        hashNode.setAsString("0x" + getSelfHash().getHashNumberAsString());
        hashNode.setComment("This value is (probably) random. It uniquely identifies this sequence, so the same number should not be used for more than one sequence.");
        writeSequenceToConfig(logger, result, settings);
        return result;
    }

    private void writeSequenceToConfig(ILogger logger, Config config, kcScriptDisplaySettings settings) {
        if (config == null)
            throw new NullPointerException("config");
        if (logger == null)
            logger = getLogger();

        // Write actions.
        StringBuilder builder = new StringBuilder();
        OptionalArguments optionalArguments = new OptionalArguments();
        config.getInternalText().clear();
        for (int i = 0; i < this.actions.size(); i++) {
            kcAction action = this.actions.get(i);
            optionalArguments.createNext().setAsString(action.getActionID().getFrogLordName(), false);
            action.save(logger, optionalArguments, settings);
            optionalArguments.toString(builder);
            String actionText = builder.toString();
            builder.setLength(0);
            optionalArguments.clear();

            config.getInternalText().add(new ConfigValueNode(actionText, action.getEndOfLineComment()));
        }
    }

    /**
     * Converts a HashTableEntry pointing to a kcCActionSequence into a config node.
     * @param entry the entry to convert
     * @param settings the settings to use
     * @return configNode
     */
    public static Config toConfigNode(HashTableEntry entry, kcScriptDisplaySettings settings) {
        if (entry == null)
            throw new NullPointerException("entry");

        Config result = new Config(entry.getKeyName()); // Use the key name instead of the sequence name, since this is what it is resolved by.
        ConfigValueNode hashNode = result.getOrCreateKeyValueNode(HASH_CONFIG_FIELD);
        hashNode.setAsString("0x" + entry.getValueRef().getHashNumberAsString());
        hashNode.setComment("This value is (probably) random. It uniquely identifies this sequence, so the same number should not be used for more than one sequence.");

        ILogger logger = entry.getParentHashTable().getLogger();
        kcCActionSequence sequence = entry.getSequence();
        if (sequence == null) {
            // Mark the sequence as not being found.
            result.getInternalText().add(new ConfigValueNode("", "Could not resolve this action sequence from the hash table."));
            return result;
        }

        String sequenceName = sequence.getSequenceName();
        if (!sequenceName.equalsIgnoreCase(entry.getKeyName()))
            logger.warning("The sequence '%s' was expected to be named '%s'! (Sequence hash table broken?)", sequenceName, entry.getKeyName());

        sequence.writeSequenceToConfig(logger, result, settings);
        return result;
    }

    @Override
    public void addToPropertyList(PropertyListNode propertyList) {
        super.addToPropertyList(propertyList);
        propertyList.add("Actions", this.actions.size());
        for (int i = 0; i < this.actions.size(); i++)
            propertyList.add("Action " + i, this.actions.get(i));
    }

    @Override
    public GreatQuestChunkedFile getChunkedFile() {
        return getParentFile();
    }

    @Override
    public kcActorBaseDesc getExecutingEntityDescription() {
        if (this.cachedActorBaseDescRef.getResource() == null) {
            for (kcCResource resource : getParentFile().getChunks()) {
                if (!(resource instanceof kcCResourceGeneric))
                    continue;

                kcCResourceGeneric resGeneric = (kcCResourceGeneric) resource;
                kcEntity3DDesc entityDesc = resGeneric.getAsEntityDescription();
                if (!(entityDesc instanceof kcActorBaseDesc))
                    continue;

                kcActorBaseDesc actorBaseDesc = (kcActorBaseDesc) entityDesc;

                boolean actionSequenceLinked = false;
                kcCResourceNamedHash actionSequenceTable = actorBaseDesc.getAnimationSequences();
                if (actionSequenceTable != null) {
                    for (HashTableEntry entry : actorBaseDesc.getAnimationSequences().getEntries()) {
                        if (entry.getSequence() == this) {
                            actionSequenceLinked = true;
                            break;
                        }
                    }
                }

                if (actionSequenceLinked) {
                    this.cachedActorBaseDescRef.setResource(resGeneric, false);
                    return actorBaseDesc;
                }
            }
        }

        if (this.cachedActorBaseDescRef.getResource() != null) {
            kcEntity3DDesc entityDesc = this.cachedActorBaseDescRef.getResource().getAsEntityDescription();
            return entityDesc instanceof kcActorBaseDesc ? (kcActorBaseDesc) entityDesc : null;
        } else {
            return null;
        }
    }

    @Override
    public void resolvePendingResources(ILogger logger) {
        for (int i = 0; i < this.actions.size(); i++) {
            kcAction action = this.actions.get(i);
            if (!(action instanceof kcActionSetAnimation))
                continue;

            kcActionSetAnimation setAnimation = (kcActionSetAnimation) action;
            GreatQuestHash<kcCResourceTrack> animationRef = setAnimation.getTrackRef();
            if (animationRef.getResource() != null)
                continue;

            if (!GreatQuestUtils.resolveLevelResourceHash(logger, kcCResourceTrack.class, getParentFile(), this, animationRef, animationRef.getHashNumber(), false))
                logger.warning("Sequence '%s' contains an action '%s' uses an animation which was not made accessible to this level.", getName(), action.getAsGqsStatement());
        }
    }
}