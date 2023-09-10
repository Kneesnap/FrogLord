package net.highwayfrogs.editor.file.mof.animation;

import lombok.Getter;
import lombok.Setter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.file.mof.*;
import net.highwayfrogs.editor.file.mof.animation.transform.TransformObject;
import net.highwayfrogs.editor.file.mof.animation.transform.TransformType;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.standard.IVector;
import net.highwayfrogs.editor.file.standard.SVector;
import net.highwayfrogs.editor.file.standard.psx.PSXMatrix;
import net.highwayfrogs.editor.file.writer.ArrayReceiver;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.sony.SCGameInstance;
import net.highwayfrogs.editor.games.sony.frogger.FroggerConfig;
import net.highwayfrogs.editor.games.sony.frogger.FroggerGameInstance;

/**
 * Represents the MR_ANIM_HEADER struct.
 * Must be encapsulated under MOFFile.
 * Created by Kneesnap on 8/25/2018.
 */
@Getter
public class MOFAnimation extends MOFBase {
    private final MOFAnimationModelSet modelSet;
    private final MOFFile staticMOF;
    private final MOFAnimCommonData commonData;
    @Setter private boolean startAtFrameZero = true;
    @Setter private TransformType transformType = TransformType.QUAT_BYTE;
    private int mofCount;
    private int modelSetCount;

    private static final int STATIC_MOF_COUNT = 1;
    private static final byte MR_ANIM_FILE_START_FRAME_AT_ZERO = (byte) 0x31; // '1'

    public MOFAnimation(SCGameInstance instance, MOFHolder holder) {
        this(instance, holder, new MOFFile(instance, holder));
    }

    public MOFAnimation(SCGameInstance instance, MOFHolder holder, MOFFile staticMOF) {
        super(instance, holder);
        this.modelSet = new MOFAnimationModelSet(instance, this);
        this.commonData = new MOFAnimCommonData(this);
        this.staticMOF = staticMOF;
    }

    @Override
    public void onLoad(DataReader reader, byte[] signature) {
        boolean forceFrameZero = (getGameInstance().isFrogger() && ((FroggerGameInstance) getGameInstance()).getConfig().getBuild() == 1);
        this.startAtFrameZero = forceFrameZero || (signature[0] == MR_ANIM_FILE_START_FRAME_AT_ZERO); // '1'
        this.transformType = TransformType.getType(signature[1]);

        int modelSetCount = reader.readUnsignedShortAsInt();
        int staticFileCount = reader.readUnsignedShortAsInt();
        int modelSetPointer = reader.readInt();   // Right after header.
        int commonDataPointer = reader.readInt(); // Right after model set data.
        int staticFilePointer = reader.readInt(); // After common data pointer.

        this.modelSetCount = modelSetCount;
        //Utils.verify(modelSetCount == 1, "Multiple model sets are not supported by FrogLord. (%d)", modelSetCount); // TODO: Medievil.
        if (modelSetCount != 1 && !getFileEntry().getDisplayName().contains("-MODELSETS"))
            getFileEntry().setFilePath(getFileEntry().getDisplayName() + "-MODELSETS");

        this.mofCount = staticFileCount;
        //Utils.verify(staticFileCount == 1, "FrogLord only supports one MOF per animation. (%d)", staticFileCount); // TODO: Medievil.
        if (staticFileCount != 1 && !getFileEntry().getDisplayName().contains("-MULTIPLEMOF"))
            getFileEntry().setFilePath(getFileEntry().getDisplayName() + "-MULTIPLEMOF");

        // Read model sets.
        reader.jumpTemp(modelSetPointer);
        this.modelSet.load(reader);
        reader.jumpReturn();

        // Read common data.
        reader.jumpTemp(commonDataPointer);
        this.commonData.load(reader);
        reader.jumpReturn();

        reader.jumpTemp(staticFilePointer);
        int mofPointer = reader.readInt();
        reader.jumpReturn();

        DataReader mofReader = reader.newReader(mofPointer, staticFilePointer - mofPointer);
        this.staticMOF.load(mofReader);
    }

    @Override
    public void onSave(DataWriter writer) {
        writer.writeUnsignedShort(1); // Model Set Count.
        writer.writeUnsignedShort(STATIC_MOF_COUNT);

        int modelSetPointer = writer.writeNullPointer(); // Right after header.
        int commonDataPointer = writer.writeNullPointer(); // Right after model set data.
        int staticFilePointer = writer.writeNullPointer(); // After common data.

        // Write model sets.
        writer.writeAddressTo(modelSetPointer);
        this.modelSet.save(writer);

        // Write common data.
        writer.writeAddressTo(commonDataPointer);
        this.commonData.save(writer);

        // Write static MOF.
        int mofPointer = writer.getIndex();
        ArrayReceiver receiver = new ArrayReceiver();
        DataWriter mofWriter = new DataWriter(receiver);
        getStaticMOF().save(mofWriter);
        mofWriter.closeReceiver();
        writer.writeBytes(receiver.toArray());

        // Write pointers.
        writer.writeAddressTo(staticFilePointer);
        writer.writeInt(mofPointer);
    }

    /**
     * Get an animation transform.
     * @param part     The MOFPart to apply to.
     * @param actionId The animation to get the transform for.
     * @param frame    The frame id to get the transform for.
     * @return transform
     */
    public TransformObject getTransform(MOFPart part, int actionId, int frame) {
        return getCommonData().getTransforms().get(getAnimationById(actionId).getTransformID(frame, part));
    }

    /**
     * Gets the MOFAnimation cel by its action id.
     * @param actionId The given action.
     * @return cel
     */
    public MOFAnimationCels getAnimationById(int actionId) {
        return getModelSet().getCelSet().getCels().get(actionId);
    }

    /**
     * Gets the amount of actions this MOFAnimation has.
     * @return actionCount
     */
    public int getAnimationCount() {
        return getModelSet().getCelSet().getCels().size();
    }

    /**
     * Generate a bounding box for this model.
     * This is slightly inaccurate, but only by a little, there was likely some information lost when the original models were converted to MOF.
     * @return boundingBox
     */
    public MOFBBox makeBoundingBox() {
        float minX = Float.MAX_VALUE;
        float minY = Float.MAX_VALUE;
        float minZ = Float.MAX_VALUE;
        float maxX = Float.MIN_VALUE;
        float maxY = Float.MIN_VALUE;
        float maxZ = Float.MIN_VALUE;

        float testX = Float.MAX_VALUE;
        float testY = Float.MAX_VALUE;
        float testZ = Float.MAX_VALUE;

        for (MOFPart part : getStaticMOF().getParts()) {
            for (int action = 0; action < getHolder().getAnimationCount(); action++) {
                for (int frame = 0; frame < getHolder().getFrameCount(action); frame++) {
                    MOFPartcel partcel = part.getCel(action, frame);
                    TransformObject transform = getTransform(part, action, frame);

                    for (SVector sVec : partcel.getVertices()) {
                        IVector vertex = PSXMatrix.MRApplyMatrix(transform.calculatePartTransform(getAnimationById(action).isInterpolationEnabled()), sVec, new IVector());

                        minX = Math.min(minX, vertex.getFloatX());
                        minY = Math.min(minY, vertex.getFloatY());
                        minZ = Math.min(minZ, vertex.getFloatZ());
                        maxX = Math.max(maxX, vertex.getFloatX());
                        maxY = Math.max(maxY, vertex.getFloatY());
                        maxZ = Math.max(maxZ, vertex.getFloatZ());
                        if (Math.abs(vertex.getFloatX()) < Math.abs(testX))
                            testX = vertex.getFloatX();
                        if (Math.abs(vertex.getFloatY()) < Math.abs(testY))
                            testY = vertex.getFloatY();
                        if (Math.abs(vertex.getFloatZ()) < Math.abs(testZ))
                            testZ = vertex.getFloatZ();
                    }
                }
            }
        }

        MOFBBox box = new MOFBBox();
        box.getVertices()[0].setValues(minX, minY, minZ, 4);
        box.getVertices()[1].setValues(minX, minY, maxZ, 4);
        box.getVertices()[2].setValues(minX, maxY, minZ, 4);
        box.getVertices()[3].setValues(minX, maxY, maxZ, 4);
        box.getVertices()[4].setValues(maxX, minY, minZ, 4);
        box.getVertices()[5].setValues(maxX, minY, maxZ, 4);
        box.getVertices()[6].setValues(maxX, maxY, minZ, 4);
        box.getVertices()[7].setValues(maxX, maxY, maxZ, 4);
        return box;
    }

    @Override
    public int buildFlags() {
        return Constants.BIT_FLAG_3 | Constants.BIT_FLAG_16 | Constants.BIT_FLAG_20; // Seems to be constant. 3 - XAR Animation 16 - Transforms Indexed, 20 - Bboxes Indexed
    }

    @Override
    public String makeSignature() {
        String firstChar;
        if (getGameInstance().isFrogger() && ((FroggerConfig) getConfig()).isAtOrBeforeBuild20()) {
            firstChar = "\0";
        } else if (isStartAtFrameZero()) {
            firstChar = "1";
        } else {
            firstChar = "0";
        }

        // TODO: medievil may want \0 instead of "0" anyways..?
        return firstChar + (char) getTransformType().getByteValue() + "ax";
    }

    /**
     * Test if a signature is a valid MOF animation.
     * @param data The file data to check.
     * @return isSignatureValid
     */
    public static boolean testSignature(byte[] data) {
        if (data == null || data.length < 4)
            return false;

        return (data[0] == '\0' || data[0] == '0' || data[0] == '1') && (data[1] >= '0' && data[1] <= '9')
                && data[2] == 'a' && data[3] == 'x';
    }
}