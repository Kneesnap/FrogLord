package net.highwayfrogs.editor.file.mof;

import lombok.Getter;
import net.highwayfrogs.editor.Utils;
import net.highwayfrogs.editor.file.GameObject;
import net.highwayfrogs.editor.file.mof.flipbook.MOFFlipbook;
import net.highwayfrogs.editor.file.mof.hilite.MOFHilite;
import net.highwayfrogs.editor.file.mof.poly_anim.MOFPartPolyAnim;
import net.highwayfrogs.editor.file.mof.poly_anim.MOFPartPolyAnimEntryList;
import net.highwayfrogs.editor.file.mof.prims.MOFPolygon;
import net.highwayfrogs.editor.file.mof.prims.MOFPrimType;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.standard.SVector;
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
    private Map<MOFPrimType, List<MOFPolygon>> mofPolygons = new HashMap<>();
    private int flags;
    private List<MOFPartcel> partcels = new ArrayList<>();
    private List<MOFHilite> hilites = new ArrayList<>();
    private PSXMatrix matrix;
    private MOFCollprim collprim;
    private List<MOFPartPolyAnim> partPolyAnims = new ArrayList<>();
    private List<MOFPartPolyAnimEntryList> partPolyAnimLists = new ArrayList<>();
    private MOFFlipbook flipbook;
    private int verticeCount;
    private int normalCount;

    private transient MOFFile parent;
    private transient Map<Integer, MOFPartPolyAnimEntryList> loadAnimEntryListMap = new HashMap<>();
    private transient Map<List<SVector>, Integer> saveNormalMap = new HashMap<>();
    private transient Map<MOFBBox, Integer> saveBoxMap = new HashMap<>();
    private transient List<MOFPolygon> orderedByLoadPolygons = new ArrayList<>();
    private transient int tempPartcelPointer;
    private transient int tempPrimitivePointer;
    private transient int tempHilitePointer;
    private transient int tempCollPrimPointer;
    private transient int tempMatrixPointer;
    private transient int tempAnimatedTexturesPointer;
    private transient int tempFlipbookPointer;

    private static final int FLAG_ANIMATED_POLYS = 1; // Does this contain some animated texture polys?

    public MOFPart(MOFFile parent) {
        this.parent = parent;
    }

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
            int readVal = reader.readInt();
            reader.jumpReturn();
            if (readVal == 0) { // This is probably an incomplete MOF, ABORT!
                getParent().setIncompleteMOF(true);
                return;
            }

            reader.jumpTemp(partcelPointer);
            for (int i = 0; i < partcelCount; i++) {
                MOFPartcel partcel = new MOFPartcel(this, verticeCount, normalCount);
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
        orderedByLoadPolygons.clear();
        reader.jumpTemp(primitivePointer);
        while (primitiveCount > 0) {
            short primType = reader.readShort(); // MR_MPRIM_HEADER
            short primCount = reader.readShort();

            Utils.verify(MOFPrimType.values().length > primType, "Unknown prim-type: %d", primType);
            MOFPrimType mofPrimType = MOFPrimType.values()[primType];

            List<MOFPolygon> prims = mofPolygons.computeIfAbsent(mofPrimType, type -> new ArrayList<>());
            for (int i = 0; i < primCount; i++) {
                MOFPolygon newPoly = mofPrimType.getMaker().get();
                newPoly.load(reader);
                prims.add(newPoly);
                orderedByLoadPolygons.add(newPoly);
            }

            primitiveCount -= primCount;
        }
        reader.jumpReturn();

        // Read Hilites
        reader.jumpTemp(hilitePointer);
        for (int i = 0; i < hiliteCount; i++) {
            MOFHilite hilite = new MOFHilite(this);
            hilite.load(reader);
            hilites.add(hilite);
        }
        reader.jumpReturn();

        if (animatedTexturesPointer > 0) {
            loadAnimEntryListMap.clear();
            reader.jumpTemp(animatedTexturesPointer);
            int count = reader.readInt();
            for (int i = 0; i < count; i++) {
                MOFPartPolyAnim partPolyAnim = new MOFPartPolyAnim(this);
                partPolyAnim.load(reader);
                this.partPolyAnims.add(partPolyAnim);
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
            getSaveBoxMap().clear();
            getSaveNormalMap().clear();
            getPartcels().forEach(partcel -> partcel.save(writer));
            getPartcels().forEach(partcel -> partcel.saveNormalData(writer));
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
            getPartPolyAnimLists().forEach(list -> list.save(writer));
            getPartPolyAnims().forEach(mofPartPolyAnim -> mofPartPolyAnim.saveExtra(writer));
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

    /**
     * Gets a polygon id, by the loaded order. (The way it turns out Frogger does it.)
     * @param primId The id to get.
     * @return mofPolygon
     */
    public MOFPolygon getPolygon(int primId) {
        return getOrderedByLoadPolygons().get(primId);
    }

    /**
     * Gets the ID that a given MOFPolygon should be saved as.
     * @param poly The polygon to save.
     * @return primId
     */
    public int getPolygonSaveID(MOFPolygon poly) {
        int primId = 0;
        for (MOFPrimType type : MOFPrimType.values()) {
            List<MOFPolygon> polygons = getMofPolygons().get(type);
            if (polygons == null)
                continue;

            int index = polygons.indexOf(poly);
            if (index >= 0) {
                return primId + index;
            } else {
                primId += polygons.size();
            }
        }

        throw new IllegalArgumentException("This MOFPolygon is not registered, and therefore does not have an id!");
    }
}
