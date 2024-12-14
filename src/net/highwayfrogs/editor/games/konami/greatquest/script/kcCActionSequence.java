package net.highwayfrogs.editor.games.konami.greatquest.script;

import lombok.Getter;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.konami.greatquest.GreatQuestHash;
import net.highwayfrogs.editor.games.konami.greatquest.chunks.GreatQuestChunkedFile;
import net.highwayfrogs.editor.games.konami.greatquest.chunks.KCResourceID;
import net.highwayfrogs.editor.games.konami.greatquest.chunks.kcCResource;
import net.highwayfrogs.editor.games.konami.greatquest.chunks.kcCResourceNamedHash;
import net.highwayfrogs.editor.games.konami.greatquest.chunks.kcCResourceNamedHash.HashTableEntry;
import net.highwayfrogs.editor.games.konami.greatquest.entity.kcActorBaseDesc;
import net.highwayfrogs.editor.games.konami.greatquest.entity.kcEntity3DDesc;
import net.highwayfrogs.editor.games.konami.greatquest.generic.kcCResourceGeneric;
import net.highwayfrogs.editor.games.konami.greatquest.script.action.kcAction;
import net.highwayfrogs.editor.games.konami.greatquest.script.action.kcActionID;
import net.highwayfrogs.editor.gui.components.PropertyListViewerComponent.PropertyList;
import net.highwayfrogs.editor.system.Config;
import net.highwayfrogs.editor.system.Config.ConfigValueNode;
import net.highwayfrogs.editor.system.Config.IllegalConfigSyntaxException;
import net.highwayfrogs.editor.utils.StringUtils;
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

public class kcCActionSequence extends kcCResource implements kcActionExecutor {
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
    public void loadFromConfigNode(Config config) {
        if (config == null)
            throw new NullPointerException("config");

        if (!config.getSectionName().equalsIgnoreCase(getSequenceName()))
            getLogger().warning("Provided sequence name '" + config.getSectionName() + "' did not match the real sequence resource '" + getSequenceName() + "'.");

        this.actions.clear();
        getSelfHash().setHash(config.getKeyValueNodeOrError(HASH_CONFIG_FIELD).getAsInteger());
        int lineNumber = 0;
        String fileName = config.getRootNode().getSectionName();
        for (ConfigValueNode line : config.getInternalText()) {
            lineNumber++;
            String textLine = line.getAsStringLiteral();
            if (StringUtils.isNullOrWhiteSpace(textLine))
                continue;

            OptionalArguments arguments = OptionalArguments.parse(textLine);

            String commandName = arguments.useNext().getAsString();

            // Resolve kcActionEffect.
            kcActionID actionID = kcActionID.getActionByCommandName(commandName);
            if (actionID == null)
                throw new IllegalArgumentException("Could not identify the action '" + commandName + "' from '" + line + "' when importing sequence " + getName() + ".");

            kcAction newAction = actionID.newInstance(this);
            try {
                newAction.load(arguments, lineNumber, fileName);
            } catch (Throwable th) {
                throw new IllegalConfigSyntaxException("Could not parse the action '" + line + "' when importing sequence " + getName() + ".", th);
            }

            newAction.printWarnings(getLogger());

            this.actions.add(newAction);
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
        hashNode.setComment("This value is (probably) random. It uniquely identifies this sequence, so the same number should not be used more than once.");

        kcCActionSequence sequence = entry.getSequence();
        if (sequence == null)
            return result;

        String sequenceName = sequence.getSequenceName();
        if (!sequenceName.equalsIgnoreCase(entry.getKeyName()))
            entry.getParentHashTable().getLogger().warning("The sequence " + sequenceName + " did not match the expected " + entry.getKeyName() + "!");

        // Write actions.
        StringBuilder builder = new StringBuilder();
        OptionalArguments optionalArguments = new OptionalArguments();
        for (int i = 0; i < sequence.getActions().size(); i++) {
            kcAction action = sequence.getActions().get(i);
            optionalArguments.createNext().setAsString(action.getActionID().getFrogLordName(), false);
            action.save(optionalArguments, settings);
            optionalArguments.toString(builder);
            String actionText = builder.toString();
            builder.setLength(0);
            optionalArguments.clear();

            result.getInternalText().add(new ConfigValueNode(actionText, action.getEndOfLineComment()));
        }

        return result;
    }

    @Override
    public PropertyList addToPropertyList(PropertyList propertyList) {
        propertyList = super.addToPropertyList(propertyList);
        propertyList.add("Actions", this.actions.size());
        for (int i = 0; i < this.actions.size(); i++)
            propertyList.add("Action " + i, this.actions.get(i));

        return propertyList;
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
}