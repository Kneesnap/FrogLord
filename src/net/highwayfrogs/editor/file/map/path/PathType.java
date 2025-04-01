package net.highwayfrogs.editor.file.map.path;

import lombok.AllArgsConstructor;
import net.highwayfrogs.editor.file.map.path.data.ArcSegment;
import net.highwayfrogs.editor.file.map.path.data.LineSegment;
import net.highwayfrogs.editor.file.map.path.data.SplineSegment;

import java.util.function.Function;

/**
 * Holds the different possible path types.
 * Created by Kneesnap on 8/22/2018.
 */
@AllArgsConstructor
public enum PathType {
    SPLINE(SplineSegment::new),
    ARC(ArcSegment::new),
    LINE(LineSegment::new);

    private Function<Path, PathSegment> maker;

    /**
     * Create a new segment.
     * @param parentPath The path which owns the segment.
     */
    public PathSegment makeNew(Path parentPath) {
        return this.maker.apply(parentPath);
    }
}
