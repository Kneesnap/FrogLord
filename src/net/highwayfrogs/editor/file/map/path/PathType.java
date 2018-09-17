package net.highwayfrogs.editor.file.map.path;

import lombok.AllArgsConstructor;
import lombok.Getter;
import net.highwayfrogs.editor.file.map.path.data.ArcSegment;
import net.highwayfrogs.editor.file.map.path.data.LineSegment;
import net.highwayfrogs.editor.file.map.path.data.SplineSegment;

import java.util.function.Supplier;

/**
 * Holds the different possible path types.
 * Created by Kneesnap on 8/22/2018.
 */
@Getter
@AllArgsConstructor
public enum PathType {
    SPLINE(SplineSegment::new),
    ARC(ArcSegment::new),
    LINE(LineSegment::new);

    private Supplier<PathSegment> maker;
}
