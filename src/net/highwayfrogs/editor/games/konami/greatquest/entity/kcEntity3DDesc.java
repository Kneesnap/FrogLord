package net.highwayfrogs.editor.games.konami.greatquest.entity;

import lombok.Getter;
import lombok.Setter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.konami.greatquest.GreatQuestInstance;
import net.highwayfrogs.editor.games.konami.greatquest.GreatQuestUtils;
import net.highwayfrogs.editor.games.konami.greatquest.math.kcSphere;
import net.highwayfrogs.editor.utils.Utils;

/**
 * Represents the 'kcEntity3DDesc' struct.
 * Created by Kneesnap on 8/21/2023.
 */
@Getter
@Setter
public class kcEntity3DDesc extends kcBaseDesc {
    private int instanceFlags; // TODO: These seem to be just the mapped flags.
    private final kcSphere boundingSphere = new kcSphere(0, 0, 0, 1F); // Positioned relative to entity position.
    private static final int PADDING_VALUES = 3;
    private static final int PADDING_VALUES_3D = 4;
    // TODO: Go over all usages of cached resource too.

    // kcCHud: Seems to have independent hud flags.
    // I'm not sure the instanceFlags are even applied to most entities.
    // Wait hmm. Are these even used? I'm not convinced they are.

    // mFlags:
    // 00 00000001 (kcCActorBase) Enable kcProxyCapsule (kcCActorBase::Update)
    //  - Enable kcCHudMgr::HudMgrUpdate, kcCHud::HudDestroy
    // 01 00000002 (kcCWaypoint) (RESIDES ON WAYPOINT) -> Enable interactions with waypoint
    //  - Seems to indicate if collision is enabled.
    //  - For waypoints it seems to exist on the waypoint.
    //  - For collision proxies, it seems to be used to indicate if the collision proxy is activated or not. (See: kcCActorBase::SetCollideable)
    // 02 00000004 (kcCActorBase) Enable TerrainTrack() (kcCActorBase::Update)
    //  - static kcCDialog::mbActive = true or false (not sure which) based on this bit when kcCDialog::Update runs.
    //  - kcCActorBase::SetCollideable (and others) will deactivate collision if this bit is set. They also won't activate collision if it is set too.
    //  - Controls if the HUD is visible for kcCHud::HudVisible
    // 03 00000008 (kcCActorBase) == ??? (Cleared by kcCActorBase::EvaluateTransform)
    // 04 00000010 (kcCActorBase) == ??? (Set kcCActorBase::Update to whether an FSM exists or not AND if bit 31 is not set.)
    // 05 00000020 (CCharacter) == 1 -> TargetIsNotNegativeOne MonsterClass::Do_Guard (kcCCameraBase::ResetInt sets this bit too?) This does seem to just indicate, is mhTarget != -1.
    // 07 00000080 (kcCActorBase) == Disable kcCEntity::onEntityEnable() (What is this even?) kcCActorBase::Update
    // 11 00000800 (CCharacter) == 0 -> ScriptControl (Controlled by script? Not sure.), aisystem.cpp/MonsterClass::Set_States
    // 12 00001000 (kcCActorBase) == Must be set for the impulse to go through. (kcCActorBase::OnImpulse)
    // 15 00008000 (kcCActorBase) If the bit is set, kcCActorBase::Animate will return without doing anything.
    // 16 00010000 (kcCActorBase) Enables Entity RenderDebug(). kcCActorBase::Render, Disables kcCDialog::Render
    //  - Could also be related to collision. This bit must be true for collision to activate, and being false will often deactivate it.
    // 18 00040000 (kcCActorBase) Enables the entity taking damage from scripts. (kcCActorBase::OnCommand)
    // 26 04000000 (kcCMotionAgent) Enables gravity (kcCMotionAgent::Update)
    // 30 40000000 (kcCEntity) All entities seem to have this bit set when registered in the entity tracker. When removed, this bit is also removed. Probably means "entity is registered"
    // 31 80000000 (kcCActorBase) If the bit is set, kcCActorBase::Update will apply bit 4 based on if there is an FSM or not. If this is not set, bit 4 is always 0.
    //  - Also disables kcProxyCapsule interactions in kcCActorBase::Update
    //  - Also disables TerrainTrack interactions in kcCActorBase::Update
    //  - Also disables kcCEntity::OnEnableUpdate interactions in kcCActorBase::Update
    //  - Skips rendering an entity if debug rendering is enabled and this is NOT enabled.

    // mFlagsEntity3D:
    // ?? ???

    // Default kcCActorBase Flags: 0x242000 (kcCActorBase::__ct)
    // mFlagsActor:
    // 03 008 (CCharacter) -> == 1 Probably BeenDamaged?

    private static final int CLASS_ID = GreatQuestUtils.hash("kcCEntity3D");

    public kcEntity3DDesc(GreatQuestInstance instance) {
        super(instance);
        this.boundingSphere.setRadius(1F); // Default radius is 1.
    }

    @Override
    protected int getTargetClassID() {
        return CLASS_ID;
    }

    @Override
    public void load(DataReader reader) {
        super.load(reader);

        // Main Data
        this.instanceFlags = reader.readInt();
        reader.skipBytesRequireEmpty(PADDING_VALUES * Constants.INTEGER_SIZE);
        this.boundingSphere.load(reader);
        reader.skipBytesRequireEmpty(PADDING_VALUES_3D * Constants.INTEGER_SIZE);
    }

    @Override
    public void saveData(DataWriter writer) {
        // Main Data
        writer.writeInt(this.instanceFlags);
        writer.writeNull(PADDING_VALUES * Constants.INTEGER_SIZE);
        this.boundingSphere.save(writer);
        writer.writeNull(PADDING_VALUES_3D * Constants.INTEGER_SIZE);
    }

    @Override
    public void writeMultiLineInfo(StringBuilder builder, String padding) {
        builder.append(padding).append("Flags: ").append(Utils.toHexString(this.instanceFlags)).append(Constants.NEWLINE);
        this.boundingSphere.writePrefixedMultiLineInfo(builder, "Bounding Sphere", padding);
    }
}