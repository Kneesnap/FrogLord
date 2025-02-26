package net.highwayfrogs.editor.games.sony.shared.mof2.hilite;

/**
 * Represents the different hilite types.
 * Created by Kneesnap on 1/8/2019.
 */
public enum FroggerHiliteType {
    COLLISION, // Special. Only present on models of playable frogs. This isn't for show, but instead it seems to be used in order to test collision with other model collprims, which is why frogger has it.
    SPRITE3D_SPLASH, // 3D Sprite - Water texture animation. Completely unused, but seemingly functional.
    SPRITE3D_WAKE, // 3D Sprite - Water texture animation. Completely unused, but seemingly functional.
    PARTICLE_EXHAUST, // Smoke exhaust particles used as exhaustion behind cars.
    PARTICLE_CLOUD, // Duplicates exhaust. Seemingly unused normally.
    PARTICLE_SMOKE, // The particles that show under the desert bison while they run.
    PARTICLE_FIRE, // A flame trail used on the molten balls in Lava Crush. Also used if frogger hops in lava.
}