package net.highwayfrogs.editor.file.mof;

import lombok.Getter;
import net.highwayfrogs.editor.Utils;
import net.highwayfrogs.editor.file.GameObject;
import net.highwayfrogs.editor.file.mof.flipbook.MOFFlipbook;
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
 * TODO: Volcano zone has some mofs which don't display properly.
 * TODO: MWD size is bloated by 2MB.
 * Created by Kneesnap on 8/25/2018.
 */
@Getter
public class MOFPart extends GameObject {
    private Map<MOFPrimType, List<MOFPolygon>> mofPolygons = new HashMap<>();
    private int flags;
    private List<MOFPartcel> partcels = new ArrayList<>();
    private List<MOFHilite> hilites = new ArrayList<>();
    private PSXMatrix matrix;
    private MOFCollprim collprim;
    private List<MOFPartPolyAnim> partPolyAnims = new ArrayList<>();
    private List<MOFPartPolyAnimEntry> partPolyAnimEntries = new ArrayList<>();
    private MOFFlipbook flipbook;
    private int verticeCount;
    private int normalCount;

    private transient List<MOFPrimType> loadedPrimTypeOrder = new ArrayList<>();
    private transient int tempPartcelPointer;
    private transient int tempPrimitivePointer;
    private transient int tempHilitePointer;
    private transient int tempCollPrimPointer;
    private transient int tempMatrixPointer;
    private transient int tempAnimatedTexturesPointer;
    private transient int tempFlipbookPointer;

    private static final int FLAG_ANIMATED_POLYS = 1; // Does this contain some animated texture polys?

    @Override
    public void load(DataReader reader) {
        this.flags = reader.readUnsignedShortAsInt();
        int partcelCount = reader.readUnsignedShortAsInt();
        this.verticeCount = reader.readUnsignedShortAsInt();
        this.normalCount = reader.readUnsignedShortAsInt();
        int primitiveCount = reader.readUnsignedShortAsInt();
        int hiliteCount = reader.readUnsignedShortAsInt();

        int partcelPointer = reader.readInt();
        int primitivePointer = reader.readInt();
        int hilitePointer = reader.readInt(); // May be null
        reader.readInt(); // Run-time value.

        int collprimPointer = reader.readInt(); // May be null.
        int matrixPointer = reader.readInt(); // May be null.
        int animatedTexturesPointer = reader.readInt(); // (Point to integer which is count.) Followed by: MR_PART_POLY_ANIM
        int flipbookPointer = reader.readInt(); // MR_PART_FLIPBOOK (MR_PART_FLIPBOOK_ACTION may follow?)

        // Read Partcels.
        if (partcelCount > 0) {
            reader.jumpTemp(partcelPointer);
            for (int i = 0; i < partcelCount; i++) {
                MOFPartcel partcel = new MOFPartcel(verticeCount, normalCount);
                partcel.load(reader);
                partcels.add(partcel);
            }
            reader.jumpReturn();
        }

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
        loadedPrimTypeOrder.clear();
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
            loadedPrimTypeOrder.add(mofPrimType);
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

        if (animatedTexturesPointer > 0) {
            reader.jumpTemp(animatedTexturesPointer);
            int count = reader.readInt();
            for (int i = 0; i < count; i++) {
                MOFPartPolyAnim partPolyAnim = new MOFPartPolyAnim(this);
                partPolyAnim.load(reader);
                this.partPolyAnims.add(partPolyAnim);
            }

            int entryCount = reader.readInt();
            for (int i = 0; i < entryCount; i++) {
                MOFPartPolyAnimEntry entry = new MOFPartPolyAnimEntry();
                entry.load(reader);
                this.partPolyAnimEntries.add(entry);
            }

            reader.jumpReturn();
        }

        if (flipbookPointer > 0) {
            reader.jumpTemp(flipbookPointer);
            this.flipbook = new MOFFlipbook();
            this.flipbook.load(reader);
            reader.jumpReturn();
        }
    }

    @Override
    public void save(DataWriter writer) {
        writer.writeUnsignedShort(this.flags);
        writer.writeUnsignedShort(getPartcels().size());
        writer.writeUnsignedShort(getVerticeCount());
        writer.writeUnsignedShort(getNormalCount());
        writer.writeUnsignedShort(getMofPolygons().values().stream().mapToInt(List::size).sum()); // PrimitiveCount.
        writer.writeUnsignedShort(getHilites().size());

        this.tempPartcelPointer = writer.writeNullPointer();
        this.tempPrimitivePointer = writer.writeNullPointer();
        this.tempHilitePointer = writer.writeNullPointer();
        writer.writeInt(0); // Runtime value.
        this.tempCollPrimPointer = writer.writeNullPointer();
        this.tempMatrixPointer = writer.writeNullPointer();
        this.tempAnimatedTexturesPointer = writer.writeNullPointer();
        this.tempFlipbookPointer = writer.writeNullPointer();
    }

    /**
     * Save extra data.
     * @param writer Save extra data to a writer.
     */
    public void saveExtra(DataWriter writer) {

        // Write Partcels.
        if (getPartcels().size() > 0) {
            writer.writeInt(0); //TODO: There are 4 bytes here which are used by something regarding texture animation. They need to be handled properly, this line is a placeholder.
            getPartcels().forEach(partcel -> partcel.save(writer));
            writer.writeAddressTo(getTempPartcelPointer());
            getPartcels().forEach(partcel -> partcel.savePointerData(writer));
            getPartcels().forEach(partcel -> partcel.saveBboxData(writer));
        }

        // Read Hilites
        if (getHilites().size() > 0) {
            writer.writeAddressTo(getTempHilitePointer());
            getHilites().forEach(hilite -> hilite.save(writer));
        }

        // Write collprim.
        if (getCollprim() != null) {
            writer.writeAddressTo(getTempCollPrimPointer());
            getCollprim().save(writer);
        }

        // Write Matrix.
        if (getMatrix() != null) {
            writer.writeAddressTo(getTempMatrixPointer());
            getMatrix().save(writer);
        }

        if (getPartPolyAnims().size() > 0) {
            writer.writeAddressTo(getTempAnimatedTexturesPointer());
            writer.writeInt(getPartPolyAnims().size());
            getPartPolyAnims().forEach(partPolyAnim -> partPolyAnim.save(writer));
            int pointer = writer.getIndex();
            getPartPolyAnims().forEach(mofPartPolyAnim -> mofPartPolyAnim.saveExtra(writer, pointer));

            writer.writeInt(getPartPolyAnimEntries().size());
            getPartPolyAnimEntries().forEach(entry -> entry.save(writer));
        }

        if (getFlipbook() != null) {
            writer.writeAddressTo(getTempFlipbookPointer());
            getFlipbook().save(writer);
        }

        // Write Primitives.
        writer.writeAddressTo(getTempPrimitivePointer());
        for (MOFPrimType primType : MOFPrimType.values()) { // NOTE: This erases the order which seems to not be consistent.
            List<MOFPolygon> polygons = getMofPolygons().get(primType);
            if (polygons == null)
                continue;

            writer.writeUnsignedShort(primType.ordinal()); // Write type.
            writer.writeUnsignedShort(polygons.size());
            polygons.forEach(prim -> prim.save(writer));
        }
    }
}
