package net.highwayfrogs.editor.file.map.path;

import lombok.AllArgsConstructor;
import lombok.Getter;
import net.highwayfrogs.editor.file.GameObject;

/**
 * A single part of the path. When saved, this is broken up by <type,offset> -> segment data
 * Created by Kneesnap on 8/22/2018.
 */
@Getter
@AllArgsConstructor
public abstract class PathSegment extends GameObject {
    private PathType type;
}
