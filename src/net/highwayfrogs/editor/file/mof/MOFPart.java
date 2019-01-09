package net.highwayfrogs.editor.file.mof;

import lombok.Getter;
import net.highwayfrogs.editor.Utils;
import net.highwayfrogs.editor.file.GameObject;
import net.highwayfrogs.editor.file.mof.prims.MOFPolygon;
import net.highwayfrogs.editor.file.mof.prims.MOFPrimType;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.standard.psx.PSXMatrix;
import net.highwayfrogs.editor.file.writer.DataWriter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents the MR_PART struct.
 * Created by Kneesnap on 8/25/2018.
 */
@Getter
public class MOFPart extends GameObject {
    private int flags;
    private List<MOFPartcel> partcels = new ArrayList<>();
    private List<MOFHilite> hilites = new ArrayList<>();
    private PSXMatrix matrix;
    private MOFCollprim collprim;
    private Map<MOFPrimType, List<MOFPolygon>> mofPolygons = new HashMap<>();

    private static final int FLAG_ANIMATED_POLYS = 1; // Does this contain some animated texture polys?

    @Override
    public void load(DataReader reader) {
        this.flags = reader.readUnsignedShortAsInt();
        int partcelCount = reader.readUnsignedShortAsInt();
        int verticeCount = reader.readUnsignedShortAsInt();
        int normalCount = reader.readUnsignedShortAsInt();
        int primitiveCount = reader.readUnsignedShortAsInt();
        int hiliteCount = reader.readUnsignedShortAsInt();

        int partcelPointer = reader.readInt();
        int primitivePointer = reader.readInt();
        int hilitePointer = reader.readInt(); // May be null
        reader.readInt(); // Run-time value.

        int collprimPointer = reader.readInt(); // May be null.
        int matrixPointer = reader.readInt(); // May be null.
        int animatedTexturesPointer = reader.readInt(); // (Point to integer which is count.) Followed by: MR_PART_POLY_ANIM TODO: Support
        int flipbookPointer = reader.readInt(); // MR_PART_FLIPBOOK (MR_PART_FLIPBOOK_ACTION may follow?) TODO: Support

        // Read Partcels.
        reader.jumpTemp(partcelPointer);
        for (int i = 0; i < partcelCount; i++) {
            MOFPartcel partcel = new MOFPartcel(verticeCount, normalCount);
            partcel.load(reader);
            partcels.add(partcel);
        }
        reader.jumpReturn();

        // Read matrix.
        if (matrixPointer > 0) {
            reader.jumpTemp(matrixPointer);
            this.matrix = new PSXMatrix();
            this.matrix.load(reader);
            reader.jumpReturn();
        }

        // Read collprim.
        if (collprimPointer > 0) {
            reader.jumpTemp(collprimPointer);
            this.collprim = new MOFCollprim();
            this.collprim.load(reader);
            reader.jumpReturn();
        }

        // Read Primitives:
        reader.jumpTemp(primitivePointer);
        while (primitiveCount > 0) {
            short primType = reader.readShort(); // MR_MPRIM_HEADER
            short primCount = reader.readShort();

            Utils.verify(MOFPrimType.values().length > primType, "Unknown prim-type: %d", primType);
            MOFPrimType mofPrimType = MOFPrimType.values()[primType];

            List<MOFPolygon> prims = new ArrayList<>(primCount);
            for (int i = 0; i < primCount; i++) {
                MOFPolygon newPoly = mofPrimType.getMaker().get();
                newPoly.load(reader);
                prims.add(newPoly);
                primitiveCount--;
            }
            mofPolygons.put(mofPrimType, prims);
        }
        reader.jumpReturn();

        // Read Hilites
        reader.jumpTemp(hilitePointer);
        for (int i = 0; i < hiliteCount; i++) {
            MOFHilite hilite = new MOFHilite();
            hilite.load(reader);
            hilites.add(hilite);
        }
        reader.jumpReturn();
    }

    @Override
    public void save(DataWriter writer) {
        //TODO: Save logic.
    }
}
