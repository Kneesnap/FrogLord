package net.highwayfrogs.editor.games.konami.greatquest.script;

import lombok.Getter;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.konami.greatquest.chunks.KCResourceID;
import net.highwayfrogs.editor.games.konami.greatquest.chunks.kcCResource;
import net.highwayfrogs.editor.games.konami.greatquest.file.GreatQuestChunkedFile;
import net.highwayfrogs.editor.games.konami.greatquest.script.action.kcAction;
import net.highwayfrogs.editor.gui.components.PropertyListViewerComponent.PropertyList;

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
@Getter
public class kcCActionSequence extends kcCResource {
    private final List<kcAction> actions = new ArrayList<>();

    public kcCActionSequence(GreatQuestChunkedFile parentFile) {
        super(parentFile, KCResourceID.ACTIONSEQUENCE);
    }

    @Override
    public void load(DataReader reader) {
        super.load(reader);
        this.actions.clear();
        while (reader.hasMore())
            this.actions.add(kcAction.readAction(reader, getParentFile()));
    }

    @Override
    public void save(DataWriter writer) {
        super.save(writer);
        for (int i = 0; i < this.actions.size(); i++)
            this.actions.get(i).save(writer);
    }

    @Override
    public PropertyList addToPropertyList(PropertyList propertyList) {
        propertyList = super.addToPropertyList(propertyList);
        propertyList.add("Actions", this.actions.size());
        for (int i = 0; i < this.actions.size(); i++)
            propertyList.add("Action " + i, this.actions.get(i));

        return propertyList;
    }
}