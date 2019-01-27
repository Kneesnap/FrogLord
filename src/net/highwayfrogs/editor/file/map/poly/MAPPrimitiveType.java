package net.highwayfrogs.editor.file.map.poly;

import java.util.function.Supplier;

/**
 * All drawable primitive enum registries should implement this interface.
 * Created by Kneesnap on 8/25/2018.
 */
public interface MAPPrimitiveType {

    /**
     * Gets the enum name.
     * @return enumName
     */
    public String name();

    /**
     * Get how many bytes the C equivalent's struct would take up in memory.
     * @return byteLength
     */
    public int getByteLength();

    /**
     * Get a Supplier which will return a PSXGPUPrimitive when called.
     * @return maker
     */
    public Supplier<? extends MAPPrimitive> getMaker();

    /**
     * Return a new instance of the given primitive.
     * @return newPrimitive
     */
    default MAPPrimitive newPrimitive() {
        return getMaker().get();
    }
}