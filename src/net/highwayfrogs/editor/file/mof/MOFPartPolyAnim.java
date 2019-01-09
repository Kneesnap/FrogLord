package net.highwayfrogs.editor.file.mof;

import lombok.Getter;
import net.highwayfrogs.editor.Utils;
import net.highwayfrogs.editor.file.GameObject;
import net.highwayfrogs.editor.file.mof.prims.MOFPolygon;
import net.highwayfrogs.editor.file.mof.prims.MOFPrimType;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;

import java.util.List;

/**
 * Represents "MR_PART_POLY_ANIM".
 * Created by Kneesnap on 1/8/2019.
 */
@Getter
public class MOFPartPolyAnim extends GameObject {
    private MOFPrimType primType;
    private MOFPolygon mofPolygon;

    private transient MOFPart parentPart;
    private transient int tempAnimAddress;

    public MOFPartPolyAnim(MOFPart part) {
        this.parentPart = part;
    }

    @Override
    public void load(DataReader reader) {
        this.primType = MOFPrimType.values()[reader.readInt()];
        int primId = reader.readInt();
        int startPrimId = primId;
        reader.readInt(); // Runtime.
        reader.readInt(); // Animation pointer.

        for (MOFPrimType type : getParentPart().getLoadedPrimTypeOrder()) {
            List<MOFPolygon> polygons = getParentPart().getMofPolygons().get(type);

            if (polygons.size() > primId) {
                this.mofPolygon = polygons.get(primId);
                Utils.verify(type == getPrimType(), "The PrimType is %s, but we expected %s.", type, getPrimType());
                break;
            } else {
                primId -= polygons.size();
            }
        }

        Utils.verify(getMofPolygon() != null, "Failed to load MOF Polygon. (%s, %d -> %d)", getPrimType(), startPrimId, primId);
    }

    @Override
    public void save(DataWriter writer) {
        writer.writeInt(this.primType.ordinal());

        int primId = 0;
        for (MOFPrimType type : MOFPrimType.values()) {
            List<MOFPolygon> polygons = getParentPart().getMofPolygons().get(type);
            if (polygons == null) {
                if (type == getPrimType())
                    throw new RuntimeException("Failed to find the MOFPolygon, is it registered? (" + getMofPolygon() + ")");
                continue;
            }

            if (type == getPrimType()) {
                int index = polygons.indexOf(getMofPolygon());
                Utils.verify(index >= 0, "Failed to find the MOFPolygon, is it still registered? (%s)", getMofPolygon());
                primId += index;
                break;
            } else {
                primId += polygons.size();
            }
        }

        writer.writeInt(primId);
        writer.writeInt(0); // Runtime.
        this.tempAnimAddress = writer.writeNullPointer();
    }

    /**
     * Write extra data.
     * @param writer  The writer to write data to.
     * @param address The address to write.
     */
    public void saveExtra(DataWriter writer, int address) {
        Utils.verify(this.tempAnimAddress > 0, "save() was not called first.");
        writer.jumpTemp(this.tempAnimAddress);
        writer.writeInt(address);
        writer.jumpReturn();
        this.tempAnimAddress = 0;
    }
}
