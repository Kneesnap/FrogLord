package net.highwayfrogs.editor.games.konami.greatquest;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

/**
 * A registry of different kc classes.
 * TODO: These values seem to differ. The code for the EU PS2 version seems to differ from the PS2 USA Release (which is what the PC version uses.)
 * It seems the way these are created is by doing hash(ignoreCase: true). Example: See GreatQuestUtils.hash("CCharacter", true)
 * Created by Kneesnap on 1/4/2021.
 */
@Getter
@AllArgsConstructor
public enum kcClassID {
    // kcBaseClass = 0x65357b17.
    // From kcClassFactory::Create
    LIGHT("kcCLight", 0xD55AF1FA, 0x5),
    SKELETON("kcCSkeleton", 0x5D3AFFCA, 0x7),
    MODEL("kcCModel", 0xD55B9232, 0x8),
    ANIM_SET("kcCAnimSet", 0x578FA9F1, 0xA),
    PROXY_CAPSULE("kcCProxyCapsule", 0x6833372A, 0xB), // Don't need to implement.
    PROXY_TRI_MESH("kcCProxyTriMesh", 0x7E1BC6D7, 0xC), // Don't need to implement.
    CAMERA_BASE("kcCCameraBase", 0xB2FA1302, 0xD),
    CAMERA_PID("kcCCameraPid", 0x7B2EB3A1, 0xE), // Don't need to implement.
    CAMERA_FREE("kcCCameraFree", 0xB2FA4162, 0xF), // Don't need to implement.
    CAMERA_3P("kcCCamera3P", 0x57B29F15, 0x10), // Don't need to implement.
    WAYPOINT("kcCWaypoint", 0x16E642C0),
    ACTOR_BASE("kcCActorBase", 0x428E9302, 0x1D),
    ACTOR("kcCActor", 0xD557428C, 0x1E),
    GAMEPAD("kcCGamePad", 0x517B2AA1, 0x20), // Don't need to implement.
    EMITTER("kcCEmitter", 0x53BE3EF7, 0x21), // Don't need to implement.
    PARTICLE_EMITTER("kcCParticleEmitter", 0x75DB09AD, 0x22), // Don't need to implement.

    // From CGreatQuestFactory::Create
    CHARACTER("CCharacter", 0xE6474E77, 0x8001),
    ITEM("CItem", 0x0035E23D, 0x8002),
    COIN("CCoin", 0x003559FE, 0x8003),
    GEM("CGem", 0x2513D, 0x8004),
    MAGIC_STONE("CMagicStone", 0x1F44FC32, 0x8005),
    HEALTH_BUG("CHealthBug", 0x37B2E969, 0x8006),
    OBJ_KEY("CObjKey", 0x1594CD29, 0x8007),
    OBJ_MAP("CObjMap", 0x1594CB60, 0x8008),
    HONEY_POT("CHoneyPot", 0xE982E671, 0x8009),
    PROP("CProp", 0x00347480, 0x800A),
    UNIQUE_ITEM("CUniqueItem", 0xE6333605);

    private final String name;
    private final int classId; // This is a hash of the class name.
    private final int alternateClassId;
    private static final Map<Integer, kcClassID> CLASS_ID_MAP = new HashMap<>();

    kcClassID(String name, int classId) {
        this(name, classId, -1);
    }

    /**
     * Test if this has an alternate class id.
     */
    public boolean hasAlternateClassId() {
        return this.alternateClassId != -1;
    }

    /**
     * Gets a class by its id, or null.
     * @param id The id of the class to get.
     * @return classById
     */
    public static kcClassID getClassById(int id) {
        return CLASS_ID_MAP.get(id);
    }

    static {
        for (kcClassID value : values()) {
            CLASS_ID_MAP.put(value.getClassId(), value);
            if (value.hasAlternateClassId())
                CLASS_ID_MAP.put(value.getAlternateClassId(), value);
        }
    }
}