package net.highwayfrogs.editor.games.sony.shared.model.primitive;

import net.highwayfrogs.editor.games.generic.data.IBinarySerializable;
import net.highwayfrogs.editor.games.generic.data.IGameObject;

/**
 * Represents a primitive.
 * Created by Kneesnap on 5/17/2024.
 */
public interface IPTPrimitive extends IGameObject, IBinarySerializable {
    /**
     * Gets the primitive type.
     */
    PTPrimitiveType getPrimitiveType();
}