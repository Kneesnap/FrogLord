package net.highwayfrogs.editor.games.sony.frogger.map.data.zone;

import lombok.AllArgsConstructor;
import net.highwayfrogs.editor.games.sony.frogger.map.FroggerMapFile;

import java.util.function.Function;

/**
 * Represents the different kinds of zones supported by Frogger.
 * There's only one though.
 * Created by Kneesnap on 8/22/2018.
 */
@AllArgsConstructor
public enum FroggerMapZoneType {
    CAMERA(FroggerMapCameraZone::new);

    private final Function<FroggerMapFile, FroggerMapZone> zoneCreator;

    /**
     * Creates a new zone object instance.
     * @param mapFile The map file to create the zone for.
     * @return newZoneInstance
     */
    public FroggerMapZone createNewZoneInstance(FroggerMapFile mapFile) {
        if (this.zoneCreator == null)
            throw new RuntimeException("There is no zone creation available for " + name() + ".");

        return this.zoneCreator.apply(mapFile);
    }
}