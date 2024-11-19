package net.highwayfrogs.editor.games.konami.greatquest.math;

import lombok.Getter;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.generic.data.IBinarySerializable;
import net.highwayfrogs.editor.games.konami.greatquest.IInfoWriter.IMultiLineInfoWriter;
import net.highwayfrogs.editor.system.math.Vector3f;

/**
 * This represents the '_kcBox4' struct in kcMath3D.h
 * NOTE: The 'kcBox4' type in the exported header file uses kcVector3. Not sure why, as ghidra sees it as a kcVector4.
 * Created by Kneesnap on 7/12/2023.
 */
@Getter
public class kcBox4 implements IMultiLineInfoWriter, IBinarySerializable {
    private final kcVector4 min = new kcVector4();
    private final kcVector4 max = new kcVector4();

    @Override
    public void load(DataReader reader) {
        this.min.load(reader);
        this.max.load(reader);
    }

    @Override
    public void save(DataWriter writer) {
        this.min.save(writer);
        this.max.save(writer);
    }

    @Override
    public void writeMultiLineInfo(StringBuilder builder, String padding) {
        this.min.writePrefixedInfoLine(builder, "Box Min", padding);
        this.max.writePrefixedInfoLine(builder, "Box Max", padding);
    }

    /**
     * Performs an inclusive contains check to determine if the given position is inside the box.
     * @param position the position to test
     * @return true iff the position is contained within the box
     */
    public boolean contains(Vector3f position) {
        if (position == null)
            throw new NullPointerException("position");

        return this.contains(position.getX(), position.getY(), position.getZ());
    }

    /**
     * Performs an inclusive contains check to determine if the given position is inside the box.
     * @param posX the x positional coordinate to test
     * @param posY the y positional coordinate to test
     * @param posZ the z positional coordinate to test
     * @return true iff the position is contained within the box
     */
    public boolean contains(float posX, float posY, float posZ) {
        // As implemented in sActionCallbackUniqueAtoms
        return this.max.getX() >= posX && posX >= this.min.getX()
                && this.max.getY() >= posY && posY >= this.min.getY()
                && this.max.getZ() >= posZ && posZ >= this.min.getZ();
    }
}