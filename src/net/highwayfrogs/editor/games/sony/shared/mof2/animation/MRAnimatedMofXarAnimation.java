package net.highwayfrogs.editor.games.sony.shared.mof2.animation;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.games.sony.SCGameData.SCSharedGameData;
import net.highwayfrogs.editor.games.sony.SCGameType;
import net.highwayfrogs.editor.games.sony.frogger.FroggerConfig;
import net.highwayfrogs.editor.games.sony.shared.mof2.MRModel;
import net.highwayfrogs.editor.games.sony.shared.mof2.mesh.MRMofPart;
import net.highwayfrogs.editor.games.sony.shared.mof2.mesh.MRStaticMof;
import net.highwayfrogs.editor.utils.Utils;
import net.highwayfrogs.editor.utils.data.reader.DataReader;
import net.highwayfrogs.editor.utils.data.writer.DataWriter;
import net.highwayfrogs.editor.utils.logging.ILogger;
import net.highwayfrogs.editor.utils.logging.InstanceLogger.AppendInfoLoggerWrapper;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents the "MR_ANIM_CELS" struct.
 * Every instance of this class is a unique action/animation.
 * Unfortunately, there are many different types of animations on MOF models.
 * We unofficially call these "XAR animations", because they're only seen in animated mof (.XAR) files.
 * XAR animations transform (moves around & rotate) groups of polygons/their vertices called "parts or rather {@code MRMofPart}.
 * Contrast this against flipbook animations which include a full positional value for every vertex for every frame (which is extremely fast but uses lots of memory),
 *  XAR animations save memory at the cost of requiring extra CPU work.
 * Created by Kneesnap on 8/25/2018.
 */
public class MRAnimatedMofXarAnimation extends SCSharedGameData {
    @Getter @NonNull private final MRAnimatedMofCelSet parentCelSet;
    // The following 'celNumbers' lists contains indices into the transformId list. (When multiplied against partCount)
    // Entry for each frame. Starts at 0, counts up for each frame, unless there is a duplicate frame, where it won't count.
    // The size of this list is the same thing as the number of frames (including repeated ones) present in the animation.
    @Getter private final List<Integer> celNumbers = new ArrayList<>();
    // The following here contain a list of Part transform IDs for each unique frame (cel).
    // Each frame has indices for each part, seemingly in order from start to end of animation.
    // However, this is not accessed directly. In order to allow for the same frame to be used more than once, the celNumbers array contains an index into this list.
    // There should always be (staticMofPartCount * uniqueCels) entries.
    @Getter private final List<Short> transformIds = new ArrayList<>();
    @Getter @Setter private boolean interpolationEnabled;
    @Getter @Setter private int staticMofPartCount;

    private transient int tempCelNumberPointer = -1;
    private transient int tempTransformIdListPointer = -1;

    public static final int CEL_NUMBERS_PER_FRAME_WHEN_INTERPOLATING = 3;
    public static final int FLAG_VIRTUAL_STANDARD = Constants.BIT_FLAG_0;
    public static final int FLAG_VIRTUAL_INTERPOLATION = Constants.BIT_FLAG_1;
    // 0x04 is seen as a flag in MediEvil & Beast Wars. (Even in the ECTS Pre-Alpha).
    // My theory is that it is garbage data, because the ECTS Pre-Alpha is using the same MR API as Frogger.

    public MRAnimatedMofXarAnimation(@NonNull MRAnimatedMofCelSet parentCelSet) {
        super(parentCelSet.getGameInstance());
        this.parentCelSet = parentCelSet;
    }

    @Override
    public ILogger getLogger() {
        int animationId = Utils.getLoadingIndex(this.parentCelSet.getAnimations(), this);
        return new AppendInfoLoggerWrapper(this.parentCelSet.getLogger(), "xarAnimationId=" + animationId, AppendInfoLoggerWrapper.TEMPLATE_COMMA_SEPARATOR);
    }

    @Override
    public void load(DataReader reader) {
        int celCount = reader.readUnsignedShortAsInt(); // The number of unique animation frames.
        this.staticMofPartCount = reader.readUnsignedShortAsInt(); // The number of mof parts in the static mof.
        int virtualCelCount = reader.readUnsignedShortAsInt(); // The number of frames in the animation.

        // Parse flags.
        int flags = reader.readUnsignedShortAsInt();
        switch (flags) {
            case FLAG_VIRTUAL_STANDARD:
                this.interpolationEnabled = false;
                break;
            case FLAG_VIRTUAL_INTERPOLATION: // TODO: https://github.com/HighwayFrogs/frogger-psx/blob/main/source/API.SRC/MR_ANIM.C#L2799
                this.interpolationEnabled = true;
                break;
            default:
                this.interpolationEnabled = false; // Flags are nonsense, and it doesn't indicate interpolation yes/no, so we'll just assume...
                // MediEvil ECTS relies on this NOT being used, because if we actually check the flag here for if it's supported, some models will not read properly.
                // So, if we ever need to change this, make sure to get the behavior right between versions.
                if (doesVersionFormatZeroFlags())
                    getLogger().warning("The MRAnimatedMofXarAnimation flags were not understood! (%X)", flags);
        }

        this.tempCelNumberPointer = reader.readInt();
        this.tempTransformIdListPointer = reader.readInt();

        // Populate lists for replacement.
        this.celNumbers.clear();
        int totalCelNumberCount = virtualCelCount * (this.interpolationEnabled ? CEL_NUMBERS_PER_FRAME_WHEN_INTERPOLATING : 1);
        for (int i = 0; i < totalCelNumberCount; i++)
            this.celNumbers.add(null);

        this.transformIds.clear();
        int totalIndexCount = celCount * this.staticMofPartCount;
        for (int i = 0; i < totalIndexCount; i++)
            this.transformIds.add(null);
    }

    @Override
    public void save(DataWriter writer) {
        int partCount = this.staticMofPartCount;
        if (partCount <= 0)
            throw new RuntimeException("Cannot save a MRAnimatedMofXarAnimation with " + partCount + " static mof part(s).");
        if ((this.transformIds.size() % partCount) != 0)
            throw new RuntimeException("The number of transform IDs (" + this.transformIds.size() + ") was not divisible by the number of mof parts! (" + partCount + ")");

        int celNumberMultiple = (this.interpolationEnabled ? CEL_NUMBERS_PER_FRAME_WHEN_INTERPOLATING : 1);
        if ((this.celNumbers.size() % celNumberMultiple) != 0)
            throw new RuntimeException("The number of celNumbers (" + this.celNumbers.size() + ") was not divisible by the expected number of values! (" + celNumberMultiple + ")");

        writer.writeUnsignedShort(this.transformIds.size() / partCount);
        writer.writeUnsignedShort(partCount);
        writer.writeUnsignedShort(this.celNumbers.size() / celNumberMultiple);
        writer.writeUnsignedShort(this.interpolationEnabled ? FLAG_VIRTUAL_INTERPOLATION : FLAG_VIRTUAL_STANDARD);
        this.tempCelNumberPointer = writer.writeNullPointer();
        this.tempTransformIdListPointer = writer.writeNullPointer();
    }

    /**
     * Reads the animation data from the reader.
     * @param reader The reader to read animation data from
     */
    @SuppressWarnings("Java8ListReplaceAll")
    void readAnimationData(DataReader reader) {
        if (this.tempCelNumberPointer < 0 || this.tempTransformIdListPointer < 0)
            throw new RuntimeException("MRAnimatedMofXarAnimation did not have load(DataReader) run first, so reading animation data cannot occur.");

        // Read celNumbers.
        requireReaderIndex(reader, this.tempCelNumberPointer, "Expected celNumbers");
        this.tempCelNumberPointer = -1;
        for (int i = 0; i < this.celNumbers.size(); i++)
            this.celNumbers.set(i, reader.readUnsignedShortAsInt());
        reader.align(Constants.INTEGER_SIZE); // There's garbage data here by the looks of it. [psx-usa-retail, MediEvil ECTS]

        requireReaderIndex(reader, this.tempTransformIdListPointer, "Expected transform indices");
        this.tempTransformIdListPointer = -1;
        for (int i = 0; i < this.transformIds.size(); i++)
            this.transformIds.set(i, reader.readShort());

        if (doesVersionFormatHaveGarbageAlignmentData()) {
            reader.align(Constants.INTEGER_SIZE); // At/before Frogger PSX Build 28 there's garbage here.
        } else {
            reader.alignRequireEmpty(Constants.INTEGER_SIZE); // There's garbage data here in Frogger prototypes. Not sure when this stops.
        }
    }

    /**
     * Writes the animation data to the writer.
     * @param writer The writer to write animation data to
     */
    void writeAnimationData(DataWriter writer) {
        if (this.tempCelNumberPointer < 0 || this.tempTransformIdListPointer < 0)
            throw new RuntimeException("MRAnimatedMofXarAnimation did not have save(DataWriter) run first, so writing animation data cannot occur.");

        // Read celNumbers.
        writer.writeAddressTo(this.tempCelNumberPointer);
        this.tempCelNumberPointer = -1;
        for (int i = 0; i < this.celNumbers.size(); i++)
            writer.writeUnsignedShort(this.celNumbers.get(i));
        writer.align(Constants.INTEGER_SIZE);

        // Read transform indices.
        writer.writeAddressTo(this.tempTransformIdListPointer);
        this.tempTransformIdListPointer = -1;
        for (int i = 0; i < this.transformIds.size(); i++)
            writer.writeShort(this.transformIds.get(i));

        writer.align(Constants.INTEGER_SIZE);
    }

    /**
     * Validates the read part count as being correct.
     * Must only be run after static mofs have been loaded.
     */
    void validatePartCount() {
        if (this.staticMofPartCount < 0)
            throw new RuntimeException("MRAnimatedMofXarAnimation has not had load(DataReader) called, and is not capable of validating the MOF part count right now.");


        int realPartCount = getMinimumValidStaticMofPartCount();
        if (this.staticMofPartCount < realPartCount)
            throw new RuntimeException("The MRAnimatedMofXarAnimation thought there were a minimum of " + this.staticMofPartCount + " static MOF Part(s), but there were actually " + realPartCount + ".");
    }

    /**
     * Gets the transform ID for an animation stage.
     * @param frame The frame of this animation.
     * @param part  The mof part to get the transform for.
     * @return transformId
     */
    public int getTransformID(int frame, @NonNull MRMofPart part) {
        MRModel model = part.getParentMof().getModel();
        boolean frameStartAtZero = model.isAnimatedMof() && model.getAnimatedMof().isStartAtFrameZero();
        // This shouldn't be negative, but is for GEN_CHECKPOINT_X.XAR in Frogger build 4.
        int celNumberIndex = (frame % this.celNumbers.size());
        if (celNumberIndex < 0)
            celNumberIndex += this.celNumbers.size();

        int actualCel = Math.max(0, this.celNumbers.get(celNumberIndex) - (frameStartAtZero ? 0 : 1));
        int partCount = this.staticMofPartCount;
        return this.transformIds.get((actualCel * partCount) + part.getPartID());
    }

    private int getMinimumValidStaticMofPartCount() {
        MRAnimatedMof parentMof = this.parentCelSet.getParentModelSet().getParentMof();
        List<MRStaticMof> staticMofs = parentMof.getStaticMofs();
        if (staticMofs.isEmpty())
            throw new RuntimeException("There are no static mofs available, so the partCount cannot be obtained!");

        int minPartCount = Integer.MAX_VALUE;
        for (int i = 0; i < staticMofs.size(); i++) {
            int realPartCount = staticMofs.get(i).getParts().size();
            if (realPartCount < minPartCount)
                minPartCount = realPartCount;
        }

        return minPartCount;
    }

    /**
     * Get the total number of frames in this cel.
     * @return frameCount
     */
    public int getFrameCount() {
        int celNumberCount = this.celNumbers.size();
        if (this.interpolationEnabled)
            celNumberCount /= CEL_NUMBERS_PER_FRAME_WHEN_INTERPOLATING;

        return celNumberCount;
    }

    /**
     * Returns true iff the active version format has garbage alignment data.
     * Garbage alignment data has been seen in Beast Wars, MediEvil, and even normal Frogger (up until Build 28).
     */
    public boolean doesVersionFormatHaveGarbageAlignmentData() {
        if (getGameInstance().isFrogger()) {
            // Observed in Frogger before Build 28.
            return ((FroggerConfig) getConfig()).isAtOrBeforeBuild28();
        } else {
            // Seen in MediEvil & Beast Wars.
            // NOT SEEN in Pre-Recode Frogger.
            return getGameInstance().getGameType().isAfter(SCGameType.FROGGER);
        }
    }

    /**
     * Returns true iff the active format version supports zeroing all the unused bit flags.
     * This has only been observed in Frogger beyond Build 4, and pre-recode Frogger.
     * Beast Wars and MediEvil (even September 1997 MediEvil) do not do this.
     */
    public boolean doesVersionFormatZeroFlags() {
        if (getGameInstance().isFrogger()) {
            FroggerConfig config = (FroggerConfig) getConfig();
            return !config.isAtOrBeforeBuild4(); // Not linked to the MR API version.
        } else {
            // Flags do appear to be zero'd in pre-recode Frogger.
            return getGameInstance().getGameType().isBefore(SCGameType.FROGGER);
        }
    }
}