package net.highwayfrogs.editor.file.map.path;

import lombok.AllArgsConstructor;
import lombok.Getter;
import net.highwayfrogs.editor.file.standard.IVector;
import net.highwayfrogs.editor.file.standard.SVector;

/**
 * Represents a path calculation.
 * Created by Kneesnap on 3/26/2019.
 */
@Getter
@AllArgsConstructor
public class PathResult {
    private SVector position;
    private IVector rotation;
}
