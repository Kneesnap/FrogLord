package net.highwayfrogs.editor.file.config.data;

import lombok.Getter;
import net.highwayfrogs.editor.file.config.FroggerEXEInfo;
import net.highwayfrogs.editor.file.config.TargetPlatform;

/**
 * A registry of all music tracks in the game.
 * Created by Kneesnap on 1/29/2019.
 */
@Getter
public enum MusicTrack {
    CAVE1(2, 2, 2),
    CAVE2(3, 5, 3),
    DESERT1(4, 9, 4),
    DESERT2(5, 13, 5),
    FOREST1(6, 15, 6),
    FOREST2(7, 18, 7), // Ruins Music.
    VOLCANO1(8, 12, 24),
    VOLCANO2(9, 1, 25),
    JUNGLE1(10, 10, 10),
    JUNGLE2(11, 15, 11), // Main Menu.
    JUNGLE3(12, 8, 12), // Honey Bee Hollow.
    LEVEL_SELECT(13, 19, 13),
    ORIGINAL1(14, 7, 14),
    ORIGINAL2(15, 17, 15),
    SWAMP1(16, 4, 22),
    SWAMP2(17, 11, 23),
    SKY1(18, 0, 18),
    SKY2(19, 16, 19),
    SUBURBIA1(20, 6, 20),
    SUBURBIA2(21, 3, 21);

    private final byte pcTrack;
    private final byte psxTrack;
    private final byte prototypeTrack;

    public static final byte TERMINATOR = (byte) -1;

    MusicTrack(int pcTrack, int psxTrack, int prototypeTrack) {
        this.pcTrack = (byte) pcTrack;
        this.psxTrack = (byte) psxTrack;
        this.prototypeTrack = (byte) prototypeTrack;
    }

    /**
     * Gets the track based on the platform.
     * @param info The config this track is used on.
     * @return trackId
     */
    public byte getTrack(FroggerEXEInfo info) {
        if (info.getPlatform() == TargetPlatform.PC) {
            if (info.isAtLeastRetailWindows()) {
                return getPcTrack();
            } else {
                return getPrototypeTrack();
            }
        } else if (info.getPlatform() == TargetPlatform.PSX) {
            return getPsxTrack();
        }

        throw new RuntimeException("Cannot get track id for platform-type: " + info.getName() + ".");
    }

    /**
     * Gets a music track by its id.
     * @param info The config to determine music ids from.
     * @param id   The id to get.
     * @return track
     */
    public static MusicTrack getTrackById(FroggerEXEInfo info, byte id) {
        for (MusicTrack test : values())
            if (test.getTrack(info) == id)
                return test;
        throw new RuntimeException("Cannot get track id " + id + " from platform-type: " + info.getName() + ".");
    }
}
