package net.highwayfrogs.editor.file.map.poly.line;

import lombok.Getter;
import net.highwayfrogs.editor.file.map.poly.MAPPrimitive;
import net.highwayfrogs.editor.file.map.poly.MAPPrimitiveType;
import net.highwayfrogs.editor.file.map.view.TextureMap;
import net.highwayfrogs.editor.file.map.view.TextureMap.TextureTreeNode;

/**
 * Represents a PSX line.
 * Created by Kneesnap on 8/25/2018.
 */
@Getter
public class MAPLine extends MAPPrimitive {
    public MAPLine(MAPPrimitiveType type, int verticeCount) {
        super(type, verticeCount);
    }

    @Override
    public TextureTreeNode getNode(TextureMap map) {
        return null;
    }
}
