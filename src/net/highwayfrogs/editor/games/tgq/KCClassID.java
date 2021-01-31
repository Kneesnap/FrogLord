package net.highwayfrogs.editor.games.tgq;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * A registry of different kc classes.
 * TODO: These values seem to differ. The code for the EU PS2 version seems to differ from the PS2 USA Release (which is what the PC version uses.)
 * Created by Kneesnap on 1/4/2021.
 */
@Getter
@AllArgsConstructor
public enum KCClassID {
    // None / null = 0x65357b17.
    LIGHT("kcCLight", 0xD55AF1FA, 0x5, null), // TODO: MAYBE?
    SKELETON("kcCSkeleton", 0x5D3AFFCA, 0x7, null), //TODO: MAYBE?
    MODEL("kcCModel", 0xD55B9232, 0x8, null), // TODO: MAYBE?
    ANIM_SET("kcCAnimSet", 0x578FA9F1, 0xA, null), // TODO: MAYBE?
    PROXY_CAPSULE("kcCProxyCapsule", 0x6833372A, 0xB, null), // Don't need to implement.
    PROXY_TRI_MESH("kcCProxyTriMesh", 0x7E1BC6D7, 0xC, null), // Don't need to implement.
    CAMERA_BASE("kcCCameraBase", 0xB2FA1302, 0xD, null), // TODO
    CAMERA_PID("kcCCameraPid", 0x7B2EB3A1, 0xE, null), // Don't need to implement.
    CAMERA_FREE("kcCCameraFree", 0xB2FA4162, 0xF, null), // Don't need to implement.
    CAMERA_3P("kcCCamera3P", 0x57B29F15, 0x10, null), // Don't need to implement.
    WAYPOINT("kcCWaypoint", 0x16E642C0, null), // TODO
    ACTOR_BASE("kcCActorBase", 0x428E9302, 0x1D, null), // TODO
    ACTOR("kcCActor", 0xD557428C, 0x1E, null), // TODO
    GAMEPAD("kcCGamePad", 0x517B2AA1, 0x20, null), // Don't need to implement.
    EMITTER("kcCEmitter", 0x53BE3EF7, 0x21, null), // Don't need to implement.
    PARTICLE_EMITTER("kcCParticleEmitter", 0x75DB09AD, 0x22, null), // Don't need to implement.

    // CGreatQuestFactory TODO
    CHARACTER("CCharacter", 0xE6474E77, 0x8001, null),
    ITEM("CItem", 0x0035E23D, 0x8002, null),
    COIN("CCoin", 0x003559FE, 0x8003, null),
    GEM("CGem", 0x2513D, 0x8004, null),
    MAGIC_STONE("CMagicStone", 0x1F44FC32, 0x8005, null),
    HEALTH_BUG("CHealthBug", 0x37B2E969, 0x8006, null),
    OBJ_KEY("CObjKey", 0x1594CD29, 0x8007, null),
    OBJ_MAP("CObjMap", 0x1594CB60, 0x8008, null),
    HONEY_POT("CHoneyPot", 0xE982E671, 0x8009, null),
    PROP("CProp", 0x00347480, 0x800A, null),
    UNIQUE_ITEM("CUniqueItem", 0xE6333605, null);

    @Getter private final String name;
    @Getter private final int classId; // This is a hash of the class name.
    @Getter private final int alternateClassId;
    private final Supplier<Object> maker;

    private static final Map<Integer, KCClassID> CLASS_ID_MAP = new HashMap<>();

    KCClassID(String name, int classId, Supplier<Object> maker) {
        this(name, classId, -1, maker);
    }

    /**
     * Test if this has an alternate class id.
     */
    public boolean hasAlternateClassId() {
        return this.alternateClassId != -1;
    }

    /**
     * Makes an instance of the given class.
     * @return instance
     */
    @SuppressWarnings("unchecked")
    public <T> T makeInstance() {
        if (this.maker == null)
            throw new UnsupportedOperationException("Class '" + this.name + "' is not currently supported.");
        return (T) this.maker.get();
    }

    /**
     * Gets a class by its id, or null.
     * @param id The id of the class to get.
     * @return classById
     */
    public static KCClassID getClassById(int id) {
        return CLASS_ID_MAP.get(id);
    }

    static {
        for (KCClassID value : values()) {
            CLASS_ID_MAP.put(value.getClassId(), value);
            if (value.hasAlternateClassId())
                CLASS_ID_MAP.put(value.getAlternateClassId(), value);
        }
    }
}
