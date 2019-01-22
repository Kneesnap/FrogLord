package net.highwayfrogs.editor.gui.mesh;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Holds mesh data.
 * Created by Kneesnap on 1/21/2019.
 */
@Getter
@AllArgsConstructor
public class MeshData {
    private final int faceCount;
    private final int texCoordCount;
}
