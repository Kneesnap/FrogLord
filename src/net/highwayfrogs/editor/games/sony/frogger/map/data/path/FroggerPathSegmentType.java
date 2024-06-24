package net.highwayfrogs.editor.games.sony.frogger.map.data.path;

import lombok.AllArgsConstructor;
import net.highwayfrogs.editor.games.sony.frogger.map.data.path.segments.FroggerPathSegment;
import net.highwayfrogs.editor.games.sony.frogger.map.data.path.segments.FroggerPathSegmentArc;
import net.highwayfrogs.editor.games.sony.frogger.map.data.path.segments.FroggerPathSegmentLine;
import net.highwayfrogs.editor.games.sony.frogger.map.data.path.segments.FroggerPathSegmentSpline;

import java.util.function.Function;

/**
 * Holds the different possible path segment types.
 * Created by Kneesnap on 8/22/2018.
 */
@AllArgsConstructor
public enum FroggerPathSegmentType {
    SPLINE(FroggerPathSegmentSpline::new),
    ARC(FroggerPathSegmentArc::new),
    LINE(FroggerPathSegmentLine::new);

    private final Function<FroggerPath, FroggerPathSegment> maker;

    /**
     * Create a new segment.
     * @param parentPath The path which owns the segment.
     */
    public FroggerPathSegment makeNew(FroggerPath parentPath) {
        return this.maker.apply(parentPath);
    }
}