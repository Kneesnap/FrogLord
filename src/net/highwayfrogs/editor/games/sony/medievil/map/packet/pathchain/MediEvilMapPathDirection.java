package net.highwayfrogs.editor.games.sony.medievil.map.packet.pathchain;

/**
 * Represents which direction a path may be going from the perspective of a start/end-point.
 * Created by Kneesnap on 2/18/2026.
 */
public enum MediEvilMapPathDirection {
    PATH_STARTS, // Represented as bit false. The connected path has its start pointt at the position PATH_STARTS is found.
    PATH_ENDS; // Represented as bit true. The connected path has its end point at the position PATH_ENDS is found.
}
