package net.highwayfrogs.editor.games.sony.shared.model.actionset;

import lombok.Getter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.sony.SCGameData.SCSharedGameData;
import net.highwayfrogs.editor.games.sony.shared.math.PTQuaternion;
import net.highwayfrogs.editor.games.sony.shared.math.PTQuaternionTranslation;
import net.highwayfrogs.editor.utils.Utils;
import net.highwayfrogs.editor.utils.logging.ILogger;
import net.highwayfrogs.editor.utils.logging.InstanceLogger.LazyInstanceLogger;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents an action set.
 * Created by Kneesnap on 5/15/2024.
 */
public class PTActionSet extends SCSharedGameData {
    @Getter private final PTActionSetFile parentFile;
    @Getter private int flags = FLAG_HIERARCHICAL;
    @Getter private final List<PTQuaternion> rotations = new ArrayList<>();
    @Getter private final List<PTQuaternionTranslation> rotationWithTranslations = new ArrayList<>();
    @Getter private final List<PTAction> actions = new ArrayList<>();
    private int actionListAddress;
    private int rotationDataPointer;
    private int rotationTranslationDataPointer;

    public static final int FLAG_HIERARCHICAL = Constants.BIT_FLAG_0;
    public static final int FLAG_VALIDATION_MASK = 0b1;

    public PTActionSet(PTActionSetFile parentFile) {
        super(parentFile != null ? parentFile.getGameInstance() : null);
        this.parentFile = parentFile;
    }

    @Override
    public void load(DataReader reader) {
        this.flags = reader.readInt();
        warnAboutInvalidBitFlags(this.flags, FLAG_VALIDATION_MASK);
        int rotationCount = reader.readUnsignedShortAsInt();
        int rotationTranslationCount = reader.readUnsignedShortAsInt();
        this.rotationDataPointer = reader.readInt();
        this.rotationTranslationDataPointer = reader.readInt();
        int actionCount = reader.readUnsignedShortAsInt();
        reader.skipBytesRequireEmpty(Constants.SHORT_SIZE); // Seems to be set to zero on load, and I can't find any time when it isn't 0 to start with. Probably padding.
        this.actionListAddress = reader.readInt();

        // Setup empty action list. (Will be read later)
        this.actions.clear();
        for (int i = 0; i < actionCount; i++)
            this.actions.add(new PTAction(this));

        // Setup empty rotation lists. (Will be read later.)
        this.rotations.clear();
        for (int i = 0; i < rotationCount; i++)
            this.rotations.add(new PTQuaternion(getGameInstance()));

        this.rotationWithTranslations.clear();
        for (int i = 0; i < rotationTranslationCount; i++)
            this.rotationWithTranslations.add(new PTQuaternionTranslation(getGameInstance()));
    }

    /**
     * Reads the action list from the current position.
     * @param reader the reader to read it from
     */
    void readActionList(DataReader reader) {
        if (this.actionListAddress <= 0)
            throw new RuntimeException("Cannot read action list, the action list pointer is invalid.");

        reader.requireIndex(getLogger(), this.actionListAddress, "Expected PTAction list");
        for (int i = 0; i < this.actions.size(); i++)
            this.actions.get(i).load(reader);

        this.actionListAddress = -1;
    }

    /**
     * Reads the list of rotations without translations from the current position.
     * @param reader the reader to read it from
     */
    void readRotations(DataReader reader) {
        if (this.rotationDataPointer <= 0)
            throw new RuntimeException("Cannot read untranslated rotation list, the pointer is invalid.");

        reader.requireIndex(getLogger(), this.rotationDataPointer, "Expected untranslated rotation list");
        for (int i = 0; i < this.rotations.size(); i++)
            this.rotations.get(i).load(reader);

        this.rotationDataPointer = -1;
    }

    /**
     * Reads the list of rotations with translations from the current position.
     * @param reader the reader to read it from
     */
    void readRotationsWithTranslations(DataReader reader) {
        if (this.rotationTranslationDataPointer <= 0)
            throw new RuntimeException("Cannot read translated rotation list, the pointer is invalid.");

        reader.requireIndex(getLogger(), this.rotationTranslationDataPointer, "Expected translated rotation list");
        for (int i = 0; i < this.rotationWithTranslations.size(); i++)
            this.rotationWithTranslations.get(i).load(reader);

        this.rotationTranslationDataPointer = -1;
    }

    /**
     * Reads the action keyframe lists from the current position.
     * @param reader the reader to read it from
     */
    void readActionKeyFrameLists(DataReader reader) {
        for (int i = 0; i < this.actions.size(); i++)
            this.actions.get(i).readKeyFrameList(reader);
    }

    /**
     * Reads the action transform index list from the current position.
     * @param reader the reader to read it from
     */
    void readActionTransformIndexLists(DataReader reader) {
        for (int i = 0; i < this.actions.size(); i++)
            this.actions.get(i).readTransformIndexList(reader);
    }

    @Override
    public void save(DataWriter writer) {
        writer.writeInt(this.flags);
        writer.writeUnsignedShort(this.rotations.size());
        writer.writeUnsignedShort(this.rotationWithTranslations.size());
        this.rotationDataPointer = writer.writeNullPointer();
        this.rotationTranslationDataPointer = writer.writeNullPointer();
        writer.writeUnsignedShort(this.actions.size());
        writer.writeNull(Constants.SHORT_SIZE); // Padding.
        this.actionListAddress = writer.writeNullPointer();
    }

    /**
     * Writes the action list to the current position.
     * @param writer the writer to write it to
     */
    void writeActionList(DataWriter writer) {
        if (this.actionListAddress <= 0)
            throw new RuntimeException("Cannot write action list, the action list pointer is invalid.");

        writer.writeAddressTo(this.actionListAddress);
        for (int i = 0; i < this.actions.size(); i++)
            this.actions.get(i).save(writer);

        this.actionListAddress = -1;
    }

    /**
     * Writes the list of rotations without translations to the current position.
     * @param writer the writer to write it to
     */
    void writeRotations(DataWriter writer) {
        if (this.rotationDataPointer <= 0)
            throw new RuntimeException("Cannot write untranslated rotation list, the pointer is invalid.");

        writer.writeAddressTo(this.rotationDataPointer);
        for (int i = 0; i < this.rotations.size(); i++)
            this.rotations.get(i).save(writer);

        this.rotationDataPointer = -1;
    }

    /**
     * Writes the list of rotations with translations to the current position.
     * @param writer the writer to write it to
     */
    void writeRotationsWithTranslations(DataWriter writer) {
        if (this.rotationTranslationDataPointer <= 0)
            throw new RuntimeException("Cannot write translated rotation list, the pointer is invalid.");

        writer.writeAddressTo(this.rotationTranslationDataPointer);
        for (int i = 0; i < this.rotationWithTranslations.size(); i++)
            this.rotationWithTranslations.get(i).save(writer);

        this.rotationTranslationDataPointer = -1;
    }

    /**
     * Writes the action key frame lists to the current position.
     * @param writer the writer to write it to
     */
    void writeActionKeyFrameLists(DataWriter writer) {
        for (int i = 0; i < this.actions.size(); i++)
            this.actions.get(i).writeKeyFrameList(writer);
    }

    /**
     * Writes the action transform index lists to the current position.
     * @param writer the writer to write it to
     */
    void writeActionTransformIndexLists(DataWriter writer) {
        for (int i = 0; i < this.actions.size(); i++)
            this.actions.get(i).writeTransformIndexList(writer);
    }

    /**
     * Gets information used to identify the logger.
     */
    public String getLoggerInfo() {
        return this.parentFile != null ? this.parentFile.getFileDisplayName() + "|ActionSet=" + this.parentFile.getActionSets().indexOf(this) : Utils.getSimpleName(this);
    }

    @Override
    public ILogger getLogger() {
        return new LazyInstanceLogger(getGameInstance(), PTActionSet::getLoggerInfo, this);
    }
}