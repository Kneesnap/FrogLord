package net.highwayfrogs.editor.games.sony.frogger.map.data.path;

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
public class FroggerPathResult {
    private final SVector position;
    private final IVector rotation;
}