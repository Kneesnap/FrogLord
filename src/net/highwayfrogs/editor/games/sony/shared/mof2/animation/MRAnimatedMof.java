package net.highwayfrogs.editor.games.sony.shared.mof2.animation;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.file.standard.IVector;
import net.highwayfrogs.editor.file.standard.SVector;
import net.highwayfrogs.editor.file.standard.psx.PSXMatrix;
import net.highwayfrogs.editor.games.sony.frogger.FroggerConfig;
import net.highwayfrogs.editor.games.sony.frogger.FroggerGameInstance;
import net.highwayfrogs.editor.games.sony.shared.mof2.MRBaseModelData;
import net.highwayfrogs.editor.games.sony.shared.mof2.MRModel;
import net.highwayfrogs.editor.games.sony.shared.mof2.animation.transform.MRAnimatedMofTransform;
import net.highwayfrogs.editor.games.sony.shared.mof2.animation.transform.MRAnimatedMofTransformType;
import net.highwayfrogs.editor.games.sony.shared.mof2.collision.MRMofBoundingBox;
import net.highwayfrogs.editor.games.sony.shared.mof2.mesh.MRMofPart;
import net.highwayfrogs.editor.games.sony.shared.mof2.mesh.MRMofPartCel;
import net.highwayfrogs.editor.games.sony.shared.mof2.mesh.MRStaticMof;
import net.highwayfrogs.editor.gui.components.PropertyListViewerComponent.PropertyList;
import net.highwayfrogs.editor.utils.Utils;
import net.highwayfrogs.editor.utils.data.reader.DataReader;
import net.highwayfrogs.editor.utils.data.writer.ArrayReceiver;
import net.highwayfrogs.editor.utils.data.writer.DataWriter;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents the contents of an animated MOF file.
 * We usually call these "XAR files" or "XAR mofs" because in reality, "animated mofs" implies that the static mofs can't have animations, which is untrue.
 * Created by Kneesnap on 2/18/2025.
 */
@Getter
public class MRAnimatedMof extends MRBaseModelData {
    private final List<MRAnimatedMofModelSet> modelSets = new ArrayList<>();
    private final MRAnimatedMofCommonData commonData = new MRAnimatedMofCommonData(this);
    private final List<MRStaticMof> staticMofs = new ArrayList<>();
    @Setter private boolean startAtFrameZero = true;
    @Setter @NonNull private MRAnimatedMofTransformType transformType = MRAnimatedMofTransformType.QUAT_BYTE;

    private static final byte MR_ANIM_FILE_START_FRAME_AT_ZERO = (byte) 0x31; // '1'

    public static final int FLAG_IS_ANIMATED_MOF = Constants.BIT_FLAG_3;
    private static final int FLAG_TRANSFORMS_ARE_INDEXED = Constants.BIT_FLAG_16;
    private static final int FLAG_BBOXES_ARE_INDEXED = Constants.BIT_FLAG_20;
    // These flags are always the ones seen. The code is documented to make it clear that these are the only valid combination of flags.
    // Well, except for FLAG_BBOXES_ARE_INDEXED, that flag is never checked/used. But that's likely an oversight.
    private static final int EXPECTED_FLAGS = FLAG_BBOXES_ARE_INDEXED | FLAG_TRANSFORMS_ARE_INDEXED | FLAG_IS_ANIMATED_MOF;

    public MRAnimatedMof(MRModel model) {
        super(model);
    }

    @Override
    protected void loadModelData(DataReader reader, byte[] signature) {
        boolean forceFrameZero = (getGameInstance().isFrogger() && ((FroggerGameInstance) getGameInstance()).getVersionConfig().getBuild() == 1);
        this.startAtFrameZero = forceFrameZero || (signature[0] == MR_ANIM_FILE_START_FRAME_AT_ZERO); // '1'
        this.transformType = MRAnimatedMofTransformType.getTypeFromOpcode(signature[1]);

        int modelSetCount = reader.readUnsignedShortAsInt();
        int staticMofCount = reader.readUnsignedShortAsInt();
        int modelSetPointer = reader.readInt();   // Right after header.
        int commonDataPointer = reader.readInt(); // Right after model set data.
        int staticMofTablePointer = reader.readInt(); // End of file.

        // Never occurs to my knowledge, but if we ever support a build that has this, it'd be good to know.
        if (modelSetCount <= 0)
            getLogger().warning("The animated MOF had NO modelSets!?");
        if (staticMofCount <= 0)
            getLogger().warning("The animated MOF had NO static Mofs!?");

        // Read model sets.
        this.modelSets.clear();
        requireReaderIndex(reader, modelSetPointer, "Expected modelSets");
        for (int i = 0; i < modelSetCount; i++) {
            MRAnimatedMofModelSet newModelSet = new MRAnimatedMofModelSet(this);
            this.modelSets.add(newModelSet); // Add before loading to ensure it can find its index while loading.
            newModelSet.load(reader);
        }

        // Read model set body data.
        for (int i = 0; i < this.modelSets.size(); i++)
            this.modelSets.get(i).readModelSetData(reader);

        // Read common data.
        requireReaderIndex(reader, commonDataPointer, "Expected MRAnimatedMofCommonData");
        this.commonData.load(reader);

        // Read mof pointer table.
        int[] mofPointers = new int[staticMofCount];
        reader.jumpTemp(staticMofTablePointer);
        for (int i = 0; i < mofPointers.length; i++)
            mofPointers[i] = reader.readInt();

        int staticMofTableEndIndex = reader.getIndex();
        reader.jumpReturn();

        // Read mofs.
        this.staticMofs.clear();
        for (int i = 0; i < staticMofCount; i++) {
            requireReaderIndex(reader, mofPointers[i], "Expected MRStaticMof[" + i + "]");
            int mofEndIndex = mofPointers.length > i + 1 ? mofPointers[i + 1] : staticMofTablePointer;
            DataReader mofReader = reader.readBytesAndCreateNewReader(mofEndIndex - mofPointers[i]);
            MRStaticMof newMof = new MRStaticMof(getModel());
            this.staticMofs.add(newMof);

            try {
                newMof.load(mofReader);
                reader.setIndex(mofPointers[i] + mofReader.getIndex()); // Ensure the reader is placed where the previous one ended. (This ensures any warning is seen.)
            } catch (Throwable th) {
                Utils.handleError(getLogger(), th, false, "Failed to load staticMof[%d].", i);
                reader.setIndex(mofEndIndex); // Don't warn about improper position.
            }
        }

        // Validate XAR animations. (Must be done after static mofs are loaded.)
        for (int i = 0; i < this.modelSets.size(); i++) {
            MRAnimatedMofModelSet modelSet = this.modelSets.get(i);
            for (int j = 0; j < modelSet.getModels().size(); j++)
                modelSet.getModels().get(j).validatePartCount();

            modelSet.getCelSet().validateAnimations();
        }

        // Validate end of MOF data is start of MOF table, then place beyond for end-position validation.
        requireReaderIndex(reader, staticMofTablePointer, "Expected the MOF Pointer Table");
        reader.setIndex(staticMofTableEndIndex);
    }

    @Override
    protected void saveModelData(DataWriter writer) {
        writer.writeUnsignedShort(this.modelSets.size());
        writer.writeUnsignedShort(this.staticMofs.size());

        int modelSetPointer = writer.writeNullPointer(); // Right after header.
        int commonDataPointer = writer.writeNullPointer(); // Right after model set data.
        int staticFilePointer = writer.writeNullPointer(); // After common data.

        // Write model sets.
        writer.writeAddressTo(modelSetPointer);
        for (int i = 0; i < this.modelSets.size(); i++)
            this.modelSets.get(i).save(writer);

        // Write model set body data.
        for (int i = 0; i < this.modelSets.size(); i++)
            this.modelSets.get(i).writeModelSetData(writer);

        // Write common data.
        writer.writeAddressTo(commonDataPointer);
        this.commonData.save(writer);

        // Write static MOFs.
        int[] mofPointers = new int[this.staticMofs.size()];
        for (int i = 0; i < this.staticMofs.size(); i++) {
            mofPointers[i] = writer.getIndex();

            ArrayReceiver receiver = new ArrayReceiver();
            DataWriter mofWriter = new DataWriter(receiver);
            this.staticMofs.get(i).save(mofWriter);
            mofWriter.closeReceiver();
            writer.writeBytes(receiver.toArray());
        }

        // Write MOF pointer table.
        writer.writeAddressTo(staticFilePointer);
        for (int i = 0; i < mofPointers.length; i++)
            writer.writeInt(mofPointers[i]);
    }

    /**
     * Gets a model by its staticModelID/static mof index (getStaticModelID()), if it can be found.
     * @param modelId the model id to resolve
     * @return mofModel, or null
     */
    public MRAnimatedMofModel getModelByID(int modelId) {
        // Resolve that mofIndex to an animated model.
        int tempModelId = 0;
        MRAnimatedMofModel mofModel = null;
        for (int i = 0; i < this.modelSets.size(); i++) {
            MRAnimatedMofModelSet modelSet = this.modelSets.get(i);
            int newTempIndex = tempModelId + modelSet.getModels().size();
            if (newTempIndex > modelId) {
                mofModel = modelSet.getModels().get(modelId - tempModelId);
                break;
            } else {
                tempModelId = newTempIndex;
            }
        }

        return mofModel;
    }

    /**
     * Get an animation transform matrix.
     * @param part The MOFPart to apply to.
     * @param xarAnimation The animation to get the transform for.
     * @param frame The frame id to get the transform for.
     * @return transform
     */
    public PSXMatrix getTransformMatrix(MRMofPart part, MRAnimatedMofXarAnimation xarAnimation, int frame) {
        if (part == null)
            throw new NullPointerException("part");
        if (xarAnimation == null)
            throw new NullPointerException("xarAnimation");

        if (!xarAnimation.isInterpolationEnabled())
            return this.commonData.getTransforms().get(xarAnimation.getTransformID(frame, part)).createMatrix();

        int baseCelIndex = xarAnimation.getCelNumberBaseIndex(frame);
        MRAnimatedMofTransform prevTransform = this.commonData.getTransforms().get(xarAnimation.getTransformIDFromCelNumberIndex(baseCelIndex, part));
        MRAnimatedMofTransform nextTransform = this.commonData.getTransforms().get(xarAnimation.getTransformIDFromCelNumberIndex(baseCelIndex + 1, part));
        int t = xarAnimation.getCelNumbers().get(baseCelIndex + 2);
        return this.transformType.interpolate(prevTransform, nextTransform, t);
    }

    /**
     * Generate a bounding box for this model.
     * This is slightly inaccurate, but only by a little, there was likely some information lost when the original models were converted to MOF.
     * TODO: Document better.
     * @return boundingBox
     */
    public MRMofBoundingBox makeBoundingBox() {
        float minX = Float.POSITIVE_INFINITY;
        float minY = Float.POSITIVE_INFINITY;
        float minZ = Float.POSITIVE_INFINITY;
        float maxX = Float.NEGATIVE_INFINITY;
        float maxY = Float.NEGATIVE_INFINITY;
        float maxZ = Float.NEGATIVE_INFINITY;

        float testX = Float.POSITIVE_INFINITY;
        float testY = Float.POSITIVE_INFINITY;
        float testZ = Float.POSITIVE_INFINITY;

        // TODO: Revisit this method.
        //  - TODO: Go over bounding boxes across games to see how accurate our generation of them is. Actually, compare with MR_VOID	MRCalculateMOFVertexExtremes() in MR_MOF.C
        for (MRStaticMof staticMof : this.staticMofs) {
            for (MRMofPart part : staticMof.getParts()) {
                for (int modelSetIdx = 0; modelSetIdx < this.modelSets.size(); modelSetIdx++) {
                    MRAnimatedMofModelSet modelSet = this.modelSets.get(modelSetIdx);
                    for (int action = 0; action < modelSet.getCelSet().getAnimations().size(); action++) {
                        MRAnimatedMofXarAnimation animation =  modelSet.getCelSet().getAnimations().get(action);
                        for (int frame = 0; frame < animation.getFrameCount(); frame++) {
                            MRMofPartCel partcel = part.getPartCel(action, frame);
                            PSXMatrix transformMatrix = getTransformMatrix(part, animation, frame);

                            for (SVector sVec : partcel.getVertices()) {
                                IVector vertex = PSXMatrix.MRApplyMatrix(transformMatrix, sVec, new IVector());

                                minX = Math.min(minX, vertex.getFloatX());
                                minY = Math.min(minY, vertex.getFloatY());
                                minZ = Math.min(minZ, vertex.getFloatZ());
                                maxX = Math.max(maxX, vertex.getFloatX());
                                maxY = Math.max(maxY, vertex.getFloatY());
                                maxZ = Math.max(maxZ, vertex.getFloatZ());
                                if (Math.abs(vertex.getFloatX()) < Math.abs(testX) || !Float.isFinite(testX))
                                    testX = vertex.getFloatX();
                                if (Math.abs(vertex.getFloatY()) < Math.abs(testY) || !Float.isFinite(testY))
                                    testY = vertex.getFloatY();
                                if (Math.abs(vertex.getFloatZ()) < Math.abs(testZ) || !Float.isFinite(testZ))
                                    testZ = vertex.getFloatZ();
                            }
                        }
                    }
                }
            }
        }

        MRMofBoundingBox box = new MRMofBoundingBox();
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
    public int generateBitFlags() {
        return EXPECTED_FLAGS; // The code makes it clear this is the only valid combination of flags.
    }

    @Override
    public String generateSignature() {
        String firstChar;
        // Current behavior works with MediEvil ECTS. May want to reorganize if it doesn't work with Beast Wars or other.
        if (getGameInstance().isFrogger() && ((FroggerConfig) getConfig()).isAtOrBeforeBuild20()) { // "start at frame zero" didn't exist before API version 1.32.
            firstChar = "\0";
        } else if (isStartAtFrameZero()) {
            firstChar = "1";
        } else {
            // MediEvil & Beast Wars need '\0'.
            // Old Frogger doesn't use any XAR models (only static MOFs), so it tells us nothing about pre-Frogger games.
            firstChar = getGameInstance().isFrogger() ? "0" : "\0";
        }

        return firstChar + (char) this.transformType.getOpcode() + "ax";
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

    @Override
    public PropertyList addToPropertyList(PropertyList propertyList) {
        int modelCount = 0;
        int xarAnimationCount = 0;
        int xarAnimationsWithInterpolationCount = 0;
        for (int i = 0; i < this.modelSets.size(); i++) {
            MRAnimatedMofModelSet modelSet = this.modelSets.get(i);
            modelCount += modelSet.getModels().size();
            xarAnimationCount += modelSet.getCelSet().getAnimations().size();
            for (int j = 0; j < modelSet.getCelSet().getAnimations().size(); j++)
                if (modelSet.getCelSet().getAnimations().get(j).isInterpolationEnabled())
                    xarAnimationsWithInterpolationCount++;
        }

        propertyList.add("Translation Type", this.transformType);
        propertyList.add("Start at Frame Zero?", this.startAtFrameZero);
        propertyList.add("Static MOF Count", this.staticMofs.size());
        propertyList.add("Model Sets", this.modelSets.size() + " (Models: " + modelCount + ")");
        propertyList.add("Xar Animations", xarAnimationCount + " (" + xarAnimationsWithInterpolationCount + " Interpolated)");

        if (this.staticMofs.size() == 1) {
            propertyList.add("Static MOF", "");
            propertyList = this.staticMofs.get(0).addToPropertyList(propertyList);
        }

        // TODO: Recursively go through and add properties.
        return propertyList;
    }
}
