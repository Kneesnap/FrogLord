package net.highwayfrogs.editor.file.config.data;

import lombok.Getter;
import net.highwayfrogs.editor.file.config.TargetPlatform;

/**
 * A registry of all music tracks in the game.
 * Created by Kneesnap on 1/29/2019.
 */
@Getter
public enum MusicTrack {
    CAVE1(2, 2),
    CAVE2(3, 5),
    DESERT1(4, 9),
    DESERT2(5, 13),
    FOREST1(6, 15),
    FOREST2(7, 18), // Ruins Music.
    VOLCANO1(8, 12),
    VOLCANO2(9, 1),
    JUNGLE1(10, 10),
    JUNGLE2(11, 15), // Main Menu.
    JUNGLE3(12, 8), // Honey Bee Hollow.
    LEVEL_SELECT(13, 19),
    ORIGINAL1(14, 7),
    ORIGINAL2(15, 17),
    SWAMP1(16, 4),
    SWAMP2(17, 11),
    SKY1(18, 0),
    SKY2(19, 16),
    SUBURBIA1(20, 6),
    SUBURBIA2(21, 3);

    private final byte pcTrack;
    private final byte psxTrack;

    public static final byte TERMINATOR = (byte) -1;

    MusicTrack(int pcTrack, int psxTrack) {
        this.pcTrack = (byte) pcTrack;
        this.psxTrack = (byte) psxTrack;
    }

    /**
     * Gets the track based on the platform.
     * @param platform The platform this track is used on.
     * @return trackId
     */
    public byte getTrack(TargetPlatform platform) {
        if (platform == TargetPlatform.PC) {
            return getPcTrack();
        } else if (platform == TargetPlatform.PSX) {
            return getPsxTrack();
        }

        throw new RuntimeException("Cannot get track for platform-type: " + platform + ".");
    }

    /**
     * Get a MusicTrack by its PC ID.
     * @param id The PC id to get.
     * @return musicTrack
     */
    public static MusicTrack getTrackByPCId(byte id) {
        for (MusicTrack test : values())
            if (test.getPcTrack() == id)
                return test;
        throw new RuntimeException("Unknown PC Track: " + id);
    }

    /**
     * Get a MusicTrack by its PSX ID.
     * @param id The PSX id to get.
     * @return musicTrack
     */
    public static MusicTrack getTrackByPSXId(byte id) {
        for (MusicTrack test : values())
            if (test.getPsxTrack() == id)
                return test;
        throw new RuntimeException("Unknown PSX Track: " + id);
    }

    /**
     * Gets a music track by its id.
     * @param platform The PSX id to get.
     * @param id       The id to get.
     * @return track
     */
    public static MusicTrack getTrackById(TargetPlatform platform, byte id) {
        if (platform == TargetPlatform.PC) {
            return getTrackByPCId(id);
        } else if (platform == TargetPlatform.PSX) {
            return getTrackByPSXId(id);
        }

        throw new RuntimeException("Cannot get track for platform-type: " + platform + ".");
    }
}
