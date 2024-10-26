package net.highwayfrogs.editor.games.konami.greatquest.audio;

import javafx.collections.FXCollections;
import javafx.scene.Node;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.reader.FileSource;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.generic.GameData;
import net.highwayfrogs.editor.games.generic.GameObject;
import net.highwayfrogs.editor.games.konami.greatquest.GreatQuestInstance;
import net.highwayfrogs.editor.games.konami.greatquest.audio.SoundChunkFile.SoundChunkEntry;
import net.highwayfrogs.editor.games.konami.greatquest.file.GreatQuestLooseGameFile;
import net.highwayfrogs.editor.games.psx.sound.VAGUtil;
import net.highwayfrogs.editor.gui.GameUIController;
import net.highwayfrogs.editor.gui.ImageResource;
import net.highwayfrogs.editor.gui.InputMenu;
import net.highwayfrogs.editor.gui.components.DefaultFileEditorUISoundListComponent;
import net.highwayfrogs.editor.gui.components.DefaultFileEditorUISoundListComponent.BasicSoundListViewComponent;
import net.highwayfrogs.editor.gui.components.DefaultFileEditorUISoundListComponent.IBasicSound;
import net.highwayfrogs.editor.gui.components.DefaultFileEditorUISoundListComponent.IBasicSoundList;
import net.highwayfrogs.editor.gui.components.PropertyListViewerComponent.IPropertyListCreator;
import net.highwayfrogs.editor.gui.components.PropertyListViewerComponent.PropertyList;
import net.highwayfrogs.editor.system.AbstractAttachmentCell;
import net.highwayfrogs.editor.system.AbstractStringConverter;
import net.highwayfrogs.editor.utils.DataSizeUnit;
import net.highwayfrogs.editor.utils.Utils;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.Clip;
import javax.sound.sampled.LineListener;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.CountDownLatch;

/**
 * Represents .SBR sound bank files.
 * Created by Kneesnap on 8/17/2023.
 */
@Getter
public class SBRFile extends GreatQuestLooseGameFile implements IBasicSoundList {
    private int sfxBankId; // This value appears to always be 0 on PS2 NTSC, so I don't think we need to modify it.
    private final List<SfxEntry> soundEffects = new ArrayList<>();
    private final List<SfxWave> waves = new ArrayList<>();

    private static final int SIGNATURE = 0x42584653; // 'SFXB'
    private static final int SUPPORTED_VERSION = 0x100;

    public SBRFile(GreatQuestInstance instance, File file) {
        super(instance, file);
    }

    @Override
    public void load(DataReader reader) {
        int signature = reader.readInt();
        if (signature != SIGNATURE)
            throw new RuntimeException("Expected signature '" + Utils.to0PrefixedHexString(SIGNATURE) + "' for SFX Bank but got '" + Utils.to0PrefixedHexString(signature) + "' instead.");

        int version = reader.readInt();
        if (version != SUPPORTED_VERSION)
            throw new RuntimeException("Expected SFX Bank Version " + SUPPORTED_VERSION + ", but got version " + version + " instead.");

        this.sfxBankId = reader.readInt();
        int sfxCount = reader.readInt();
        int sfxAttrOffset = reader.readInt();
        @SuppressWarnings("unused") int sfxAttrSize = reader.readInt();
        int numWaves = reader.readInt();
        int waveAttrOffset = reader.readInt();
        int waveAttrSize = reader.readInt();
        int waveDataOffset = reader.readInt();
        @SuppressWarnings("unused") int waveDataSize = reader.readInt();

        // Read sfx attributes.
        this.soundEffects.clear();
        for (int i = 0; i < sfxCount; i++) {
            int sfxId = reader.readInt();
            if (sfxId == GreatQuestInstance.PADDING_CD_INT)
                break; // Uninitialized memory.... This seems like the file has invalid data to be honest... Perhaps since the data isn't actually parsed until it gets used, since this bad data is never used it never gets parsed by the game, and thus never has a chance to break stuff.

            int currentSfxAttrOffset = reader.readInt();

            reader.jumpTemp(sfxAttrOffset + currentSfxAttrOffset);
            SfxAttributes attributes = SfxAttributes.readAttributes(this, reader);
            reader.jumpReturn();

            this.soundEffects.add(new SfxEntry(this, sfxId, attributes));
        }

        // Read wav attributes.
        this.waves.clear();
        reader.setIndex(waveAttrOffset);
        for (int i = 0; i < numWaves; i++) {
            SfxWave wave = createNewWave();
            wave.load(reader, waveDataOffset);
            this.waves.add(wave);
        }

        int actualWaveAttrSize = reader.getIndex() - waveAttrOffset;
        if (actualWaveAttrSize != waveAttrSize)
            throw new RuntimeException("Read " + actualWaveAttrSize + " bytes of wave attribute data, but the file said there were supposed to be " + waveAttrSize + " bytes.");
    }

    @Override
    public void save(DataWriter writer) {
        writer.writeInt(SIGNATURE);
        writer.writeInt(SUPPORTED_VERSION);
        writer.writeInt(this.sfxBankId);
        writer.writeInt(this.soundEffects.size());
        int sfxAttrOffset = writer.writeNullPointer();
        int sfxAttrSize = writer.writeNullPointer();
        writer.writeInt(this.waves.size());
        int waveAttrOffset = writer.writeNullPointer();
        int waveAttrSize = writer.writeNullPointer();
        int waveDataOffset = writer.writeNullPointer();
        int waveDataSize = writer.writeNullPointer();

        // Write sfx attributes.
        int[] sfxAttrOffsets = new int[this.soundEffects.size()];
        for (int i = 0; i < this.soundEffects.size(); i++) {
            writer.writeInt(this.soundEffects.get(i).getSfxId());
            sfxAttrOffsets[i] = writer.writeNullPointer();
        }

        int sfxAttributeDataStart = writer.getIndex();
        writer.writeAddressTo(sfxAttrOffset);
        for (int i = 0; i < this.soundEffects.size(); i++) {
            writer.writeAddressAt(sfxAttrOffsets[i], writer.getIndex() - sfxAttributeDataStart);
            this.soundEffects.get(i).getAttributes().save(writer);
        }
        writer.writeAddressAt(sfxAttrSize, writer.getIndex() - sfxAttributeDataStart);

        // Write wav.
        int writtenWaveDataBytes = 0;
        writer.writeAddressTo(waveAttrOffset);
        for (int i = 0; i < this.waves.size(); i++) {
            SfxWave wave = this.waves.get(i);
            wave.save(writer, writtenWaveDataBytes);
            writtenWaveDataBytes += wave.getWaveSize();
        }

        writer.writeAddressAt(waveAttrSize, writtenWaveDataBytes);

        // Write wav data.
        int waveDataStartsAt = writer.getIndex();
        writer.writeAddressTo(waveDataOffset);
        for (int i = 0; i < this.waves.size(); i++)
            this.waves.get(i).saveADPCM(writer);

        writer.writeAddressAt(waveDataSize, writer.getIndex() - waveDataStartsAt);
    }

    @Override
    public GameUIController<?> makeEditorUI() {
        return DefaultFileEditorUISoundListComponent.loadEditor(getGameInstance(), new SBRFileEditorUISoundListComponent(getGameInstance()), this);
    }

    @Override
    public Image getCollectionViewIcon() {
        return ImageResource.MUSIC_NOTE_16.getFxImage();
    }

    /**
     * Creates a new sfx wave object.
     */
    public SfxWave createNewWave() {
        switch (getGameInstance().getPlatform()) {
            case WINDOWS:
                return new SfxWavePC(this);
            case PLAYSTATION_2:
                return new SfxWavePS2(this);
            default:
                throw new RuntimeException("Cannot create new wave attributes for the " + getGameInstance().getPlatform() + " platform.");
        }
    }

    /**
     * Ensure all waves have correct IDs.
     * @return true iff the UI should be refreshed.
     */
    public boolean synchronizeWaveIds() {
        // Track entries by wave.
        Map<Integer, List<SfxEntrySimpleAttributes>> entriesByWaveIndex = new HashMap<>();
        for (int i = 0; i < this.soundEffects.size(); i++) {
            SfxEntry entry = this.soundEffects.get(i);
            if (!(entry.getAttributes() instanceof SfxEntrySimpleAttributes))
                continue;

            SfxEntrySimpleAttributes attributes = (SfxEntrySimpleAttributes) entry.getAttributes();
            entriesByWaveIndex.computeIfAbsent(attributes.getWaveIndex(), key -> new ArrayList<>()).add(attributes);
        }

        // Go through each wave and update its ID.
        // If the ID changes, update all usages of the wave.
        for (int i = 0; i < this.waves.size(); i++) {
            SfxWave wave = this.waves.get(i);
            List<SfxEntrySimpleAttributes> entriesUsingWave = entriesByWaveIndex.remove(wave.getWaveID());
            if (wave.getWaveID() == i)
                continue; // Wave ID is already correct, skip!

            // Change the wave as well as all references to it.
            wave.setWaveID(i);
            for (int j = 0; j < entriesUsingWave.size(); j++)
                entriesUsingWave.get(j).waveIndex = i;
        }

        // Set the sounds without valid waves to use wave -1.
        boolean refreshDisplay = false;
        if (!entriesByWaveIndex.isEmpty()) {
            for (List<SfxEntrySimpleAttributes> attributeList : entriesByWaveIndex.values())
                attributeList.forEach(attribute -> attribute.waveIndex = -1);

            refreshDisplay = true;
        }

        return refreshDisplay;
    }

    /**
     * Plays the audio SFX blocking. Primary purpose is for debugging.
     * @param wave The sfx to play.
     * @throws InterruptedException Thrown if waiting for the audio clip to finish is interrupted.
     */
    @SuppressWarnings("unused")
    public static void playSfxBlocking(SfxWave wave) throws InterruptedException {
        Clip clip = wave.getClip();
        clip.setMicrosecondPosition(0);
        CountDownLatch latch = new CountDownLatch(1);
        LineListener listener = event -> {
            if (event.getType() == javax.sound.sampled.LineEvent.Type.STOP)
                latch.countDown();
        };

        clip.addLineListener(listener);
        clip.start();
        latch.await();
        clip.removeLineListener(listener);
    }

    @Override
    public Collection<? extends IBasicSound> getSounds() {
        return this.soundEffects;
    }

    @Override
    public PropertyList addToPropertyList(PropertyList propertyList) {
        propertyList = super.addToPropertyList(propertyList);
        propertyList.add("SFX Bank ID", this.sfxBankId);
        propertyList.add("SFX Entry Count", this.soundEffects.size());
        propertyList.add("SFX Wave Count", this.waves.size());
        return propertyList;
    }

    public interface SfxWave extends IBasicSound {
        /**
         * Loads data from the reader.
         * @param reader The reader to read data from.
         */
        void load(DataReader reader, int ADPCMStartOffset);

        /**
         * Saves data to the writer.
         * @param writer        The writer to save data to.
         * @param wavDataOffset The offset which audio data is written at.
         */
        void save(DataWriter writer, int wavDataOffset);

        /**
         * Writes the ADPCM data to the writer.
         * @param writer The writer to write the data to.
         */
        void saveADPCM(DataWriter writer);

        /**
         * Save as a .wav file.
         * @param file The file to save to.
         * @throws IOException If it was impossible to write the file.
         */
        void exportToWav(File file) throws IOException;

        /**
         * Loads from a .wav file.
         * @param file The file to save to.
         * @throws IOException If it was impossible to write the file.
         */
        void importFromWav(File file) throws IOException;

        /**
         * Returns the local wave ID.
         */
        int getWaveID();

        /**
         * Sets the new wave ID. Should always be the index of the wave within the wave list.
         * @param waveId the ID of the wave.
         */
        void setWaveID(int waveId);

        /**
         * The amount of bytes the wave data takes up when saved to the file.
         * This is not always the same as the ADPCM data size.
         */
        int getWaveSize();

        /**
         * Prompts the user to import a wave file over the contents of this wave.
         * @return Returns true iff a sound is successfully imported.
         */
        boolean promptUserImportWavFile();
    }

    @Getter
    private abstract static class BaseSfxWave extends GameObject<GreatQuestInstance> implements SfxWave {
        @NonNull private final SBRFile parentFile;
        @Getter @Setter protected int waveID; // This is not allowed to be changed by the user, for the sake of editing simplicity.

        public BaseSfxWave(@NonNull SBRFile parentFile) {
            super(parentFile.getGameInstance());
            this.parentFile = parentFile;
        }

        @Override
        public String getCollectionViewDisplayName() {
            return String.valueOf(getWaveID());
        }

        @Override
        public String getCollectionViewDisplayStyle() {
            return null;
        }

        @Override
        public Image getCollectionViewIcon() {
            return ImageResource.MUSIC_NOTE_16.getFxImage();
        }

        @Override
        public String getExportFileName() {
            return getWaveID() + ".wav";
        }

        @Override
        public PropertyList addToPropertyList(PropertyList propertyList) {
            propertyList.add("Local Wave ID", getWaveID());
            propertyList.add("Wave Size", getWaveSize() + " (" + DataSizeUnit.formatSize(getWaveSize()) + ")");
            return propertyList;
        }

        @Override
        public void setupRightClickMenuItems(ContextMenu contextMenu) {
            MenuItem menuItemImport = new MenuItem("Import Sound");
            contextMenu.getItems().add(menuItemImport);
            menuItemImport.setOnAction(event -> promptUserImportWavFile());

            MenuItem menuItemExport = new MenuItem("Export Sound");
            contextMenu.getItems().add(menuItemExport);
            menuItemExport.setOnAction(event -> promptUserExportWavFile());
        }

        /**
         * Prompts the user to import a wave file over the contents of this wave.
         * @return Returns true iff a sound is successfully imported.
         */
        public boolean promptUserImportWavFile() {
            File inputFile = Utils.promptFileOpen(getGameInstance(), "Specify the sound file to import", "Audio File", "wav");
            if (inputFile == null)
                return false;

            try {
                importFromWav(inputFile);
                return true;
            } catch (Throwable th) {
                Utils.handleError(getLogger(), th, true, "Failed to load/apply sound file '%s'.", inputFile.getName());
                return false;
            }
        }

        /**
         * Prompts the user to export this sound wave as a wave file.
         * @return Returns true iff a sound is successfully exported.
         */
        public boolean promptUserExportWavFile() {
            File outputFile = Utils.promptFileSave(getGameInstance(), "Specify the file to save the sound as...", getExportFileName(), "Audio File", "wav");
            if (outputFile == null)
                return false;

            try {
                exportToWav(outputFile);
                return true;
            } catch (Throwable th) {
                Utils.handleError(getLogger(), th, true, "Failed to export sound as '%s'.", outputFile.getName());
                return false;
            }
        }
    }

    public static class SfxWavePS2 extends BaseSfxWave implements SfxWave {
        @Getter private int flags;
        @Getter private int sampleRate = 22050; // 22050 for most SFX, 11025 for some SFX, 24000 for voice clips.
        private byte[] ADPCMData;
        private Clip cachedClip;

        public static final int FLAG_VALIDATION_MASK = 0b1;
        public static final int FLAG_REPEAT_UNTIL_STOPPED = Constants.BIT_FLAG_0;

        public SfxWavePS2(@NonNull SBRFile parentFile) {
            super(parentFile);
        }

        @Override
        public void load(DataReader reader, int wavDataOffset) {
            this.waveID = reader.readInt();
            this.flags = reader.readInt();
            this.sampleRate = reader.readInt();
            int waveDataOffset = reader.readInt();
            int waveDataSize = reader.readInt();

            // The data here follows the WAVEFORMATEX struct as defined at
            // https://learn.microsoft.com/en-us/previous-versions/ms713497(v=vs.85)

            reader.jumpTemp(wavDataOffset + waveDataOffset);
            this.ADPCMData = reader.readBytes(waveDataSize);
            reader.jumpReturn();

            warnAboutInvalidBitFlags(this.flags, FLAG_VALIDATION_MASK);
        }

        @Override
        public void save(DataWriter writer, int wavDataOffset) {
            writer.writeInt(this.waveID);
            writer.writeInt(this.flags);
            writer.writeInt(this.sampleRate);
            writer.writeInt(wavDataOffset);
            writer.writeInt(getWaveSize());
        }

        @Override
        public void saveADPCM(DataWriter writer) {
            if (this.ADPCMData != null)
                writer.writeBytes(this.ADPCMData);
        }

        @Override
        public int getWaveSize() {
            return this.ADPCMData != null ? this.ADPCMData.length : 0;
        }

        @Override
        public Clip getClip() {
            if (this.cachedClip != null)
                return this.cachedClip;

            AudioFormat format = new AudioFormat(getSampleRate(), 16, 1, true, false);
            byte[] convertedAudioData = VAGUtil.rawVagToWav(this.ADPCMData);
            return this.cachedClip = Utils.getClipFromRawAudioData(format, convertedAudioData);
        }

        private void clearCachedClip() {
            if (this.cachedClip != null) {
                this.cachedClip.stop();
                this.cachedClip = null;
            }
        }

        @Override
        public boolean isRepeatEnabled() {
            return (this.flags & FLAG_REPEAT_UNTIL_STOPPED) == FLAG_REPEAT_UNTIL_STOPPED;
        }

        @Override
        public void exportToWav(File file) throws IOException {
            Files.write(file.toPath(), VAGUtil.rawVagToWav(this.ADPCMData, getSampleRate()));
        }

        @Override
        public void importFromWav(File file) throws IOException {
            byte[] rawFileBytes = Files.readAllBytes(file.toPath());
            this.ADPCMData = VAGUtil.wavToVag(Utils.getRawAudioDataFromWavFile(rawFileBytes));

            AudioFormat newFormat = Utils.getAudioFormatFromWavFile(rawFileBytes);
            if (newFormat != null)
                setSampleRate((int) newFormat.getSampleRate());

            clearCachedClip();
        }

        @Override
        public PropertyList addToPropertyList(PropertyList propertyList) {
            propertyList = super.addToPropertyList(propertyList);
            // This is the only sound flag here. I do wonder if the other flags (VOICE_CLIP / MUSIC) are valid here.
            // But I see no reason to support them, as they don't have any obvious benefit here and that never occurs in the shipped game.
            propertyList.add("Repeat", isRepeatEnabled(), () -> {
                this.flags ^= FLAG_REPEAT_UNTIL_STOPPED;
                return isRepeatEnabled();
            });
            propertyList.add("Sample Rate", this.sampleRate, () ->
                    InputMenu.promptInputInt(getGameInstance(), "Please specify the new sample rate.", this.sampleRate, this::setSampleRate));
            return propertyList;
        }

        /**
         * Sets the sample rate of the audio clip
         * @param newSampleRate the new sample rate to apply
         */
        public void setSampleRate(int newSampleRate) {
            if (newSampleRate <= 0 || newSampleRate > 1000000)
                throw new IllegalArgumentException("The provided sample rate of " + newSampleRate + " was invalid!");

            if (this.sampleRate == newSampleRate)
                return;

            this.sampleRate = newSampleRate;
            clearCachedClip();
        }

        /**
         * Set the state of the given bit flags
         * @param flagMask the bit flags to set
         * @param bitsSet the state of the flags
         */
        public void setFlagState(int flagMask, boolean bitsSet) {
            if ((flagMask & FLAG_VALIDATION_MASK) != flagMask)
                throw new IllegalArgumentException("flagMask (" + Utils.toHexString(flagMask) + ") had unsupported bits set!");

            if (bitsSet) {
                this.flags |= flagMask;
            } else {
                this.flags &= ~flagMask;
            }
        }
    }

    public static class SfxWavePC extends BaseSfxWave implements SfxWave {
        @Getter private int unknownValue; // What is this? We should figure this out. I suspect this was probably the size of the original uncompressed wav file before it was converted to compressed wave data, but I am not sure.
        private byte[] waveFormatEx; // Modelled by the 'WAVEFORMATEX' struct. https://learn.microsoft.com/en-us/windows/win32/api/mmreg/ns-mmreg-waveformatex
        private byte[] ADPCMData;
        private Clip cachedClip;

        private static final String RIFF_SIGNATURE = "RIFF";
        private static final String WAV_SIGNATURE = "WAVE";
        private static final String DATA_CHUNK_SIGNATURE = "data";
        private static boolean hasPcWarningBeenShown;

        public SfxWavePC(@NonNull SBRFile parentFile) {
            super(parentFile);
        }

        @Override
        public void load(DataReader reader, int wavDataOffset) {
            this.waveID = reader.readInt();
            reader.skipBytesRequireEmpty(Constants.INTEGER_SIZE); // Flags are empty on PC.
            this.unknownValue = reader.readInt();
            int waveDataStartOffset = reader.readInt();
            int waveDataSize = reader.readInt();

            reader.jumpTemp(wavDataOffset + waveDataStartOffset);

            // Read WAVEFORMATEX.
            int waveFormatExSize = 18; // Default Size of 'WAVEFORMATEX'.
            reader.jumpTemp(reader.getIndex() + waveFormatExSize - Constants.SHORT_SIZE);
            waveFormatExSize += reader.readShort();
            reader.jumpReturn();

            this.waveFormatEx = reader.readBytes(waveFormatExSize);

            // Read ADPCM data.
            this.ADPCMData = reader.readBytes(waveDataSize - waveFormatExSize);
            reader.jumpReturn();
        }

        @Override
        public void save(DataWriter writer, int wavDataOffset) {
            writer.writeInt(this.waveID);
            writer.writeInt(0); // Flags are empty on PC.
            writer.writeInt(this.unknownValue);
            writer.writeInt(wavDataOffset);
            writer.writeInt(getWaveSize());
        }

        @Override
        public void saveADPCM(DataWriter writer) {
            if (this.waveFormatEx != null)
                writer.writeBytes(this.waveFormatEx);
            if (this.ADPCMData != null)
                writer.writeBytes(this.ADPCMData);
        }

        @Override
        public Clip getClip() {
            if (this.cachedClip != null)
                return this.cachedClip;

            // This fails with the game sounds because Java's Clip support (Even AudioClip, I tried!!) does not support the format used here, even if the data is technically valid.
            // However, it WILL work if the user imports new sounds which are properly formatted, since the issue is specifically the audio format itself being 2 / MICROSOFT ADPCM instead of just 1 / PCM.
            // I've not tested at the time of writing, but I suspect the PC Port will accept any valid wave format, as it's just calling the Win32 API with the header data + audio data.
            // So in theory if the files were converted to one that Java did support, it would play just fine, but the problem is they compressed the sounds to take up less space in memory.

            // The file data here has info about the AudioFormat, and the easiest way to deal with the lack of the AudioFormat is to just complete the wav file and read it directly.
            this.cachedClip = Utils.getClipFromWavFile(toWavFileBytes(), false);
            if (this.cachedClip != null)
                return this.cachedClip;

            if (!hasPcWarningBeenShown) {
                hasPcWarningBeenShown = true;
                Utils.makePopUp("FrogLord is unable to play these sound effects due to Java not supporting this audio format.\nHowever, FrogLord will still let you import/export it, as other programs will be able to play it.", AlertType.WARNING);
            }

            return null;
        }

        private void clearCachedClip() {
            if (this.cachedClip != null) {
                this.cachedClip.stop();
                this.cachedClip = null;
            }
        }

        /**
         * Gets the sound as a .wav file.
         */
        public byte[] toWavFileBytes() {
            return Utils.createWavFile(this.waveFormatEx, this.ADPCMData);
        }

        @Override
        public void exportToWav(File file) throws IOException {
            Files.write(file.toPath(), toWavFileBytes());
        }

        @Override
        public void importFromWav(File file) throws IOException {
            DataReader reader = new DataReader(new FileSource(file));

            try {
                reader.verifyString(RIFF_SIGNATURE);
                reader.skipInt();
                reader.verifyString(WAV_SIGNATURE);
                reader.verifyString("fmt ");
                int waveFormatExLength = reader.readInt();
                this.waveFormatEx = reader.readBytes(waveFormatExLength);
                reader.verifyString(DATA_CHUNK_SIGNATURE);
                int adpcmDataLength = reader.readInt();
                this.ADPCMData = reader.readBytes(adpcmDataLength);
                clearCachedClip();
            } catch (Throwable th) {
                throw new RuntimeException("Invalid wav file '" + file.getName() + "'.", th);
            }
        }

        @Override
        public int getWaveSize() {
            return (this.waveFormatEx != null ? this.waveFormatEx.length : 0)
                    + (this.ADPCMData != null ? this.ADPCMData.length : 0);
        }

        @Override
        public PropertyList addToPropertyList(PropertyList propertyList) {
            propertyList = super.addToPropertyList(propertyList);
            propertyList.add("Unknown Value", this.unknownValue,
                    () -> InputMenu.promptInputInt(getGameInstance(), "Please enter the new unknown value.", this.unknownValue, null));
            return propertyList;
        }

        @Override
        public boolean isRepeatEnabled() {
            return false;
        }
    }

    @Getter
    public static abstract class SfxAttributes extends GameData<GreatQuestInstance> implements IPropertyListCreator {
        @NonNull private final SBRFile parentFile;
        private final byte type;
        private short flags;
        private short priority = 100; // 100 seems to be default, 50 for vase break/barrel breaks, 50 for unimportant, 200 for music

        private static final int FLAG_VALIDATION_MASK = 0b11000001;
        public static final int FLAG_REPEAT = Constants.BIT_FLAG_0;
        public static final int FLAG_VOICE_CLIP = Constants.BIT_FLAG_6;
        public static final int FLAG_MUSIC = Constants.BIT_FLAG_7;

        protected SfxAttributes(@NonNull SBRFile parentFile, byte typeOpcode) {
            super(parentFile.getGameInstance());
            this.parentFile = parentFile;
            this.type = typeOpcode;
        }

        @Override
        public void load(DataReader reader) {
            this.flags = reader.readUnsignedByteAsShort();
            this.priority = reader.readUnsignedByteAsShort();
            warnAboutInvalidBitFlags(this.flags, FLAG_VALIDATION_MASK);
        }

        @Override
        public void save(DataWriter writer) {
            writer.writeByte(this.type);
            writer.writeUnsignedByte(this.flags);
            writer.writeUnsignedByte(this.priority);
        }

        @Override
        public PropertyList addToPropertyList(PropertyList propertyList) {
            propertyList.add("Type", this.type + " (" + Utils.getSimpleName(this) + ")");
            String flagString = getFlagsAsString();
            propertyList.add("Flags", Utils.toHexString(this.flags) + (flagString != null ? " (" + flagString + ")" : ""));
            addFlagToggle(propertyList, "Repeat", FLAG_REPEAT);
            addFlagToggle(propertyList, "Voice Clip", FLAG_VOICE_CLIP);
            addFlagToggle(propertyList, "Music", FLAG_MUSIC);
            propertyList.add("Priority", this.priority,
                    () -> InputMenu.promptInputInt(getGameInstance(), "What should the new priority value be set to? (Default = 100)", this.priority, this::setPriority));

            return propertyList;
        }

        /**
         * Set the state of the given bit flags
         * @param flagMask the bit flags to set
         * @param bitsSet the state of the flags
         */
        public void setFlagState(int flagMask, boolean bitsSet) {
            if ((flagMask & FLAG_VALIDATION_MASK) != flagMask)
                throw new IllegalArgumentException("flagMask (" + Utils.toHexString(flagMask) + ") had unsupported bits set!");

            if (bitsSet) {
                this.flags |= (short) flagMask;
            } else {
                this.flags &= (short) ~flagMask;
            }
        }

        @SuppressWarnings("lossy-conversions")
        private void addFlagToggle(PropertyList propertyList, String name, int flagMask) {
            propertyList.add(name + " Flag", (this.flags & flagMask) == flagMask, () -> {
                this.flags ^= flagMask;
                return (this.flags & flagMask) == flagMask;
            });
        }

        /**
         * Gets the flags as a string.
         */
        public String getFlagsAsString() {
            if (this.flags == 0)
                return null;

            StringBuilder builder = new StringBuilder();
            if ((this.flags & FLAG_REPEAT) == FLAG_REPEAT)
                builder.append("REPEAT");

            if ((this.flags & FLAG_VOICE_CLIP) == FLAG_VOICE_CLIP) {
                if (builder.length() > 0)
                    builder.append(", ");
                builder.append("VOICE_CLIP");
            }

            if ((this.flags & FLAG_MUSIC) == FLAG_MUSIC) {
                if (builder.length() > 0)
                    builder.append(", ");
                builder.append("MUSIC");
            }

            if ((this.flags & ~FLAG_VALIDATION_MASK) != 0) {
                if (builder.length() > 0)
                    builder.append(", ");
                builder.append("UNKNOWN");
            }

            return builder.length() > 0 ? builder.toString() : null;
        }

        /**
         * Gets the icon to use for this SFX.
         */
        public abstract Image getIcon();

        /**
         * Gets the playable audio clip for the provided SfxEntry.
         * @param entry the entry to resolve the clip for
         * @return audioClip, if one was found
         */
        public abstract Clip getClip(SfxEntry entry);

        /**
         * Ask the user for a new SFX ID for the sound.
         * @param entry the entry to ask for
         * @return newSfxId, or null, if the user didn't give a valid one.
         */
        public abstract Integer askUserForNewSfxId(SfxEntry entry);

        /**
         * Loads the sound data from the wav file.
         * @param inputFile the file to load data from
         */
        public abstract void loadFromWavFile(SfxEntry entry, File inputFile) throws IOException;

        /**
         * Save the sound to a wav file.
         * @param outputFile the file to save the sound as.
         */
        public abstract void saveToWavFile(SfxEntry entry, File outputFile) throws IOException;

        /**
         * Read sound effect attributes from the reader.
         * @param parentFile the sound registry file to create the attributes for
         * @param reader     The reader to read the data from.
         * @return attributes
         */
        public static SfxAttributes readAttributes(@NonNull SBRFile parentFile, DataReader reader) {
            byte type = reader.readByte();
            SfxAttributes attributes;
            if (type == SfxEntrySimpleAttributes.TYPE_OPCODE) {
                attributes = new SfxEntrySimpleAttributes(parentFile);
            } else if (type == SfxEntryStreamAttributes.TYPE_OPCODE) {
                attributes = new SfxEntryStreamAttributes(parentFile);
            } else {
                throw new RuntimeException("Don't know what SfxAttributes type " + type + " is.");
            }

            attributes.load(reader);
            return attributes;
        }

        /**
         * Sets the priority value.
         * @param newPriority The new priority
         */
        public void setPriority(int newPriority) {
            if (newPriority < 0 || newPriority > 255)
                throw new IllegalArgumentException("The value was outside of the range [0, 255]. (Value: " + newPriority + ")");

            this.priority = (short) newPriority;
        }
    }

    /**
     * Represents attributes for a simple sound effect.
     */
    @Getter
    public static class SfxEntrySimpleAttributes extends SfxAttributes {
        private short instanceLimit; // 0 seems to be default
        private short volume = 127; // 127 seems to be full volume, but this value is not always seen as 127.
        private short pan = 64; // 64 seems to be default
        private int pitch; // 0 seems to be default
        private int waveIndex = -1;

        private static final byte TYPE_OPCODE = 0;

        protected SfxEntrySimpleAttributes(@NonNull SBRFile parentFile) {
            super(parentFile, TYPE_OPCODE);
        }

        @Override
        public void load(DataReader reader) {
            super.load(reader);
            this.instanceLimit = reader.readUnsignedByteAsShort();
            this.volume = reader.readUnsignedByteAsShort();
            this.pan = reader.readUnsignedByteAsShort();
            this.pitch = reader.readUnsignedShortAsInt();
            this.waveIndex = reader.readInt();
        }

        @Override
        public void save(DataWriter writer) {
            super.save(writer);
            if (this.waveIndex < 0) // Instead of allowing us to save invalid data, or silently hiding it, fail loudly so the user has to address it.
                throw new IllegalStateException("Cannot save " + getClass().getSimpleName() + " with a waveIndex of " + this.waveIndex + ".");

            writer.writeUnsignedByte(this.instanceLimit);
            writer.writeUnsignedByte(this.volume);
            writer.writeUnsignedByte(this.pan);
            writer.writeUnsignedShort(this.pitch);
            writer.writeInt(this.waveIndex);
        }

        @Override
        public PropertyList addToPropertyList(PropertyList propertyList) {
            propertyList = super.addToPropertyList(propertyList);
            propertyList.add("Instance Limit", this.instanceLimit, () -> InputMenu.promptInputInt(getGameInstance(), "What is the new instance limit? (Default: 127)", this.instanceLimit, this::setInstanceLimit));
            propertyList.add("Volume", this.volume, () -> InputMenu.promptInputInt(getGameInstance(), "What is the new volume? (Default: 127)", this.volume, this::setVolume));
            propertyList.add("Pan", this.pan, () -> InputMenu.promptInputInt(getGameInstance(), "What is the new pan? (Default: 64)", this.pan, this::setPan));
            propertyList.add("Pitch", this.pitch, () -> InputMenu.promptInputInt(getGameInstance(), "What is the new pitch? (Default: 0)", this.pitch, this::setPitch));
            propertyList.add("Wave ID", this.waveIndex, () -> InputMenu.promptInputInt(getGameInstance(), "What is the local ID of the wave to apply?", this.waveIndex, this::setWaveIndex));

            // NOTE: I don't actually know that the limits are 127. It's possible values higher might be supported, but I don't see much of a reason to care.
            return propertyList;
        }

        @Override
        public Image getIcon() {
            if (this.waveIndex >= 0) {
                return ImageResource.MUSIC_NOTE_16.getFxImage();
            } else {
                return ImageResource.GHIDRA_ICON_RED_X_16.getFxImage();
            }
        }

        private SfxWave getWave() {
            List<SfxWave> waves = getParentFile().getWaves();
            return waves.size() > this.waveIndex && this.waveIndex >= 0 ? waves.get(this.waveIndex) : null;
        }

        @Override
        public Clip getClip(SfxEntry entry) {
            SfxWave wave = getWave();
            return wave != null ? wave.getClip() : null;
        }

        @Override
        public Integer askUserForNewSfxId(SfxEntry entry) {
            return entry.askForUnusedEmbeddedSfxId();
        }

        @Override
        public void loadFromWavFile(SfxEntry entry, File inputFile) throws IOException {
            SfxWave wave = getWave();
            if (wave == null)
                throw new IllegalStateException("Could not resolve SfxWave.");

            wave.importFromWav(inputFile);
        }

        @Override
        public void saveToWavFile(SfxEntry entry, File outputFile) throws IOException {
            SfxWave wave = getWave();
            if (wave == null)
                throw new IllegalStateException("Could not resolve SfxWave.");

            wave.exportToWav(outputFile);
        }

        /**
         * Sets the instance limit value.
         * @param newInstanceLimit The new instance limit
         */
        public void setInstanceLimit(int newInstanceLimit) {
            if (newInstanceLimit < 0 || newInstanceLimit > 127)
                throw new IllegalArgumentException("The provided value is outside of the allowed range of [0, 127]. (Value: " + newInstanceLimit + ")");

            this.instanceLimit = (short) newInstanceLimit;
        }

        /**
         * Sets the volume value.
         * @param newVolume The new volume
         */
        public void setVolume(int newVolume) {
            if (newVolume < 0 || newVolume > 127)
                throw new IllegalArgumentException("The provided value is outside of the allowed range of [0, 127]. (Value: " + newVolume + ")");

            this.volume = (short) newVolume;
        }

        /**
         * Sets the pan value.
         * @param newPan The new pan
         */
        public void setPan(int newPan) {
            if (newPan < 0 || newPan > 127)
                throw new IllegalArgumentException("The provided value is outside of the allowed range of [0, 127]. (Value: " + newPan + ")");

            this.pan = (short) newPan;
        }

        /**
         * Sets the pitch value.
         * @param newPitch The new pitch
         */
        public void setPitch(int newPitch) {
            if (newPitch < 0 || newPitch > 127)
                throw new IllegalArgumentException("The provided value is outside of the allowed range of [0, 127]. (Value: " + newPitch + ")");

            this.pitch = (short) newPitch;
        }

        /**
         * Sets the wave index.
         * @param newWaveIndex The new wave index
         */
        public void setWaveIndex(int newWaveIndex) {
            if (newWaveIndex < 0 || newWaveIndex >= getParentFile().getWaves().size())
                throw new IllegalArgumentException("That is not a valid Wave ID! (" + newWaveIndex + "). Perhaps this is an SFX ID instead?");

            this.waveIndex = (short) newWaveIndex;
        }
    }

    /**
     * Represents attributes for a stream sound effect.
     */
    @Getter
    public static class SfxEntryStreamAttributes extends SfxAttributes {
        private short volume = 127; // 127 seems to be the default (Full volume)

        private static final byte TYPE_OPCODE = 1;

        protected SfxEntryStreamAttributes(@NonNull SBRFile parentFile) {
            super(parentFile, TYPE_OPCODE);
        }

        @Override
        public void load(DataReader reader) {
            super.load(reader);
            this.volume = reader.readUnsignedByteAsShort();
        }

        @Override
        public void save(DataWriter writer) {
            super.save(writer);
            writer.writeUnsignedByte(this.volume);
        }

        @Override
        public PropertyList addToPropertyList(PropertyList propertyList) {
            propertyList = super.addToPropertyList(propertyList);
            propertyList.add("Volume", this.volume, () -> InputMenu.promptInputInt(getGameInstance(), "What is the new volume? (Default: 127)", this.volume, this::setVolume));
            return propertyList;
        }

        @Override
        public Image getIcon() {
            // Supposed to indicate the sound is imported.
            return ImageResource.GHIDRA_ICON_LOCATION_IN_16.getFxImage();
        }

        private SoundChunkEntry getSoundChunkEntry(SfxEntry entry) {
            if (getGameInstance() != null && getGameInstance().getSoundChunkFile() != null)
                return getGameInstance().getSoundChunkFile().getSoundById(entry.getSfxId());

            return null;
        }

        @Override
        public Clip getClip(SfxEntry entry) {
            SoundChunkEntry soundChunkEntry = getSoundChunkEntry(entry);
            return soundChunkEntry != null ? soundChunkEntry.getClip() : null;
        }

        @Override
        public Integer askUserForNewSfxId(SfxEntry entry) {
            return entry.askForUnusedStreamSfxId();
        }

        @Override
        public void loadFromWavFile(SfxEntry entry, File inputFile) {
            SoundChunkEntry soundChunkEntry = getSoundChunkEntry(entry);
            if (soundChunkEntry == null)
                throw new IllegalStateException("Could not resolve SoundChunkEntry.");

            soundChunkEntry.loadSupportedAudioFile(inputFile);
        }

        @Override
        public void saveToWavFile(SfxEntry entry, File outputFile) {
            SoundChunkEntry soundChunkEntry = getSoundChunkEntry(entry);
            if (soundChunkEntry == null)
                throw new IllegalStateException("Could not resolve SoundChunkEntry.");

            soundChunkEntry.saveAsWavFile(outputFile);
        }

        /**
         * Sets the volume value.
         * @param newVolume The new volume
         */
        public void setVolume(int newVolume) {
            if (newVolume < 0 || newVolume > 127)
                throw new IllegalArgumentException("The provided value is outside of the allowed range of [0, 127]. (Value: " + newVolume + ")");

            this.volume = (short) newVolume;
        }
    }

    @Getter
    public static class SfxEntry extends GameObject<GreatQuestInstance> implements IBasicSound {
        @NonNull private final SBRFile parentFile;
        private int sfxId;
        private final SfxAttributes attributes;

        public SfxEntry(@NonNull SBRFile parentFile, int sfxId, SfxAttributes attributes) {
            super(parentFile.getGameInstance());
            this.parentFile = parentFile;
            this.sfxId = sfxId;
            this.attributes = attributes;
        }

        @Override
        public String getCollectionViewDisplayName() {
            return getGameInstance().getShortenedSoundPath(this.sfxId, true);
        }

        @Override
        public String getCollectionViewDisplayStyle() {
            return null;
        }

        @Override
        public Image getCollectionViewIcon() {
            return this.attributes != null ? this.attributes.getIcon() : ImageResource.QUESTION_MARK_16.getFxImage();
        }

        @Override
        public Clip getClip() {
            return this.attributes != null ? this.attributes.getClip(this) : null;
        }

        @Override
        public String getExportFileName() {
            return getGameInstance().getSoundFileName(this.sfxId, false) + ".wav";
        }

        @Override
        public boolean isRepeatEnabled() {
            return this.attributes != null && (this.attributes.getFlags() & SfxAttributes.FLAG_REPEAT) == SfxAttributes.FLAG_REPEAT;
        }

        @Override
        public PropertyList addToPropertyList(PropertyList propertyList) {
            propertyList.add("SFX ID", this.sfxId, () -> {
                Integer newSfxId = this.attributes != null ? this.attributes.askUserForNewSfxId(this) : null;
                if (newSfxId != null)
                    this.sfxId = newSfxId;
                return newSfxId;
            });
            return this.attributes != null ? this.attributes.addToPropertyList(propertyList) : propertyList;
        }

        /**
         * Ask the user for a stream SFX ID which is not currently used by the SBRFile.
         */
        public Integer askForUnusedStreamSfxId() {
            return askForUnusedStreamSfxId(getParentFile(), this.sfxId);
        }

        /**
         * Ask the user for a stream SFX ID which is not currently used by the SBRFile OR the sound streaming file.
         */
        public Integer askForUnusedEmbeddedSfxId() {
            return askForUnusedEmbeddedSfxId(getParentFile(), this.sfxId);
        }

        /**
         * Ask the user for a stream SFX ID which is not currently used by the SBRFile.
         */
        public static Integer askForUnusedStreamSfxId(SBRFile sbrFile, int oldSfxId) {
            GreatQuestInstance instance = sbrFile.getGameInstance();

            return InputMenu.promptInputInt(instance, "Please enter the ID of a sound effect which exists, but is not currently used here.", oldSfxId, newId -> {
                if (newId < 0)
                    throw new IllegalArgumentException(newId + " is not a valid SFX ID! IDs must be greater than or equal to zero!");
                if (newId == oldSfxId)
                    throw new IllegalArgumentException("The provided SFX ID (" + newId + ") was the same as before.");

                SoundChunkEntry foundEntry = instance.getSoundChunkFile().getSoundById(newId);
                if (foundEntry == null)
                    throw new IllegalArgumentException("The provided SFX ID (" + newId + ") had no sound data in the stream chunk file.");

                for (int i = 0; i < sbrFile.getSoundEffects().size(); i++) {
                    SfxEntry tempEntry = sbrFile.getSoundEffects().get(i);
                    if (tempEntry.getSfxId() == newId)
                        throw new IllegalArgumentException("The provided SFX ID (" + newId + ") is already claimed in " + sbrFile.getFileName() + " by entry " + i + ".");
                }
            });
        }

        /**
         * Ask the user for a stream SFX ID which is not currently used by the SBRFile OR the sound streaming file.
         */
        public static Integer askForUnusedEmbeddedSfxId(SBRFile sbrFile, int oldSfxId) {
            GreatQuestInstance instance = sbrFile.getGameInstance();

            return InputMenu.promptInputInt(instance, "Please enter the ID of a sound effect which exists, but is not currently used by this file.", oldSfxId, newId -> {
                if (newId < 0)
                    throw new IllegalArgumentException(newId + " is not a valid SFX ID! IDs must be greater than or equal to zero!");
                if (newId >= instance.getSoundChunkFile().getNextFreeSoundId())
                    throw new IllegalArgumentException(newId + " is not a valid SFX ID as it is reserved for newly added sounds!");
                if (newId == oldSfxId)
                    throw new IllegalArgumentException("The provided SFX ID (" + newId + ") was the same as before.");

                SoundChunkEntry foundEntry = instance.getSoundChunkFile().getSoundById(newId);
                if (foundEntry != null)
                    throw new IllegalArgumentException("The provided SFX ID (" + newId + ") corresponds to '" + instance.getFullSoundPath(newId) + "', which has its sound data in the streamed sound chunk file, and thus cannot be used for an embedded sound effect.");

                for (int i = 0; i < sbrFile.getSoundEffects().size(); i++) {
                    SfxEntry tempEntry = sbrFile.getSoundEffects().get(i);
                    if (tempEntry.getSfxId() == newId)
                        throw new IllegalArgumentException("The provided SFX ID (" + newId + ") is already claimed in " + sbrFile.getFileName() + " by entry " + i + ".");
                }
            });
        }

        @Override
        public void setupRightClickMenuItems(ContextMenu contextMenu) {
            MenuItem menuItemImport = new MenuItem("Import Sound");
            contextMenu.getItems().add(menuItemImport);
            menuItemImport.setOnAction(event -> promptImportWavFile());

            MenuItem menuItemExport = new MenuItem("Export Sound");
            contextMenu.getItems().add(menuItemExport);
            menuItemExport.setOnAction(event -> promptExportWavFile());
        }

        /**
         * Prompts the user to replace the sound data with another wav file.
         */
        public void promptImportWavFile() {
            File inputFile = Utils.promptFileOpen(getGameInstance(), "Specify the sound file to import", "Audio File", "wav");
            if (inputFile != null) {
                try {
                    this.attributes.loadFromWavFile(this, inputFile);
                } catch (Throwable th) {
                    Utils.handleError(getLogger(), th, true, "Failed to import file '%s'.", inputFile.getName());
                }
            }
        }

        /**
         * Prompts the user to save the sound to a wav file.
         */
        public void promptExportWavFile() {
            File outputFile = Utils.promptFileSave(getGameInstance(), "Specify the file to save the sound as...", getExportFileName(), "Audio File", "wav");
            if (outputFile != null) {
                try {
                    this.attributes.saveToWavFile(this, outputFile);
                } catch (Throwable th) {
                    Utils.handleError(getLogger(), th, true, "Failed to export sound as '%s'.", outputFile.getName());
                }
            }
        }
    }

    /**
     * A variant of the default sound list implementation which can display the contents of SBR files.
     */
    public static class SBRFileEditorUISoundListComponent extends net.highwayfrogs.editor.gui.components.DefaultFileEditorUISoundListComponent<GreatQuestInstance, SBRFile> {
        public SBRFileEditorUISoundListComponent(GreatQuestInstance instance) {
            super(instance, "SBR File", ImageResource.MUSIC_NOTE_16.getFxImage());
        }

        @Override
        protected void onControllerLoad(Node rootNode) {
            super.onControllerLoad(rootNode);
            getSoundListComponent().extendParentUI();

            getCollectionEditorComponent().setRemoveButtonLogic(sound -> {
                getSoundListComponent().removeViewEntry(sound);

                // If we're removing a wave, sync the indices to keep them up to date.
                if (sound instanceof SfxWave && getFile().synchronizeWaveIds())
                    getSoundListComponent().refreshDisplay();
            });

            getCollectionEditorComponent().setMoveButtonLogic((sound, direction) -> {
                getSoundListComponent().moveViewEntry(sound, direction.getOffset());

                // If we're moving a wave, ensure the wave IDs are updated.
                if (sound instanceof SfxWave && getFile().synchronizeWaveIds())
                    getSoundListComponent().refreshDisplay();
            });
        }

        @Override
        public SBRFileListViewComponent getSoundListComponent() {
            return (SBRFileListViewComponent) super.getSoundListComponent();
        }

        @Override
        protected SBRFileListViewComponent createListViewComponent() {
            return new SBRFileListViewComponent(this);
        }
    }

    /**
     * Allows supplying a list which differs based on the actively selected UI.
     */
    public static class SBRFileListViewComponent extends BasicSoundListViewComponent<GreatQuestInstance, SBRFile, IBasicSound> {
        private final ComboBox<SBRFileUIViewType> viewCategoryComboBox;
        private final CustomMenuItem addNewWaveItem = new CustomMenuItem(new Label("Add Embedded Sound Wave"));
        private final CustomMenuItem addNewWaveEntryItem = new CustomMenuItem(new Label("Add Embedded Sound Reference"));
        private final CustomMenuItem addNewStreamEntryItem = new CustomMenuItem(new Label("Add Stream Sound Reference"));

        private SBRFileListViewComponent(SBRFileEditorUISoundListComponent listComponent) {
            super(listComponent);
            this.viewCategoryComboBox = createComboBox();
        }

        @Override
        public SBRFileEditorUISoundListComponent getListComponent() {
            return (SBRFileEditorUISoundListComponent) super.getListComponent();
        }

        @Override
        public List<? extends IBasicSound> getViewEntries() {
            SBRFileUIViewType viewType = this.viewCategoryComboBox != null ? this.viewCategoryComboBox.getValue() : null;

            if (viewType == SBRFileUIViewType.WAVES) {
                return getListComponent().getFile().getWaves();
            } else if (viewType == null || viewType == SBRFileUIViewType.ENTRIES) {
                return super.getViewEntries();
            } else {
                throw new IllegalArgumentException("Don't know how to setup UI for '" + viewType + "'.");
            }
        }

        private void updateAddMenuEntries(SBRFileUIViewType viewType) {
            this.addNewWaveItem.setDisable(viewType == SBRFileUIViewType.ENTRIES);
            this.addNewWaveEntryItem.setDisable(viewType == SBRFileUIViewType.WAVES);
            this.addNewStreamEntryItem.setDisable(viewType == SBRFileUIViewType.WAVES);
        }

        /**
         * Extends the parent UI.
         * This must be run after the parent UI has been loaded, since otherwise it can't work.
         */
        private void extendParentUI() {
            Region emptyRegion = new Region();
            HBox.setHgrow(emptyRegion, Priority.ALWAYS); // Ensures the combo box is aligned to the right.
            getListComponent().getLeftSidePanelTopBox().getChildren().addAll(emptyRegion, this.viewCategoryComboBox);
            updateAddMenuEntries(this.viewCategoryComboBox.getValue());

            this.viewCategoryComboBox.valueProperty().addListener((observable, oldValue, newValue) -> {
                SBRFileEditorUISoundListComponent listComponent = getListComponent();
                if (oldValue != newValue && newValue != null) {
                    listComponent.stopActiveAudioClip();
                    listComponent.getSoundListComponent().refreshDisplay();
                    listComponent.getCollectionEditorComponent().updateEditorControls();
                    updateAddMenuEntries(newValue);
                }
            });

            this.addNewWaveItem.setOnAction(event -> {
                SBRFile sbrFile = getListComponent().getFile();

                SfxWave newWave = sbrFile.createNewWave();
                if (!newWave.promptUserImportWavFile())
                    return; // User didn't supply a usable sound file.

                addViewEntry(newWave, 1);
                if (getListComponent().getFile().synchronizeWaveIds())
                    refreshDisplay();
            });

            this.addNewWaveEntryItem.setOnAction(event -> {
                SBRFile sbrFile = getListComponent().getFile();
                if (sbrFile.getWaves().isEmpty()) {
                    Utils.makePopUp("There are no sound waves in this file currently, so it is not possible to add a reference.", AlertType.WARNING);
                    return;
                }

                Integer sfxId = SfxEntry.askForUnusedEmbeddedSfxId(sbrFile, 0);
                if (sfxId == null)
                    return;

                // The user will be able to change the local wave ID themselves.
                SfxAttributes newAttributes = new SfxEntrySimpleAttributes(sbrFile);
                SfxEntry newEntry = new SfxEntry(sbrFile, sfxId, newAttributes);
                addViewEntry(newEntry, 1);
            });

            this.addNewStreamEntryItem.setOnAction(event -> {
                SBRFile sbrFile = getListComponent().getFile();
                Integer sfxId = SfxEntry.askForUnusedStreamSfxId(sbrFile, 0);
                if (sfxId == null)
                    return;

                SfxAttributes newAttributes = new SfxEntryStreamAttributes(sbrFile);
                SfxEntry newEntry = new SfxEntry(sbrFile, sfxId, newAttributes);
                addViewEntry(newEntry, 1);
            });

            getListComponent().getCollectionEditorComponent().addMenuItemToAddButtonLogic(this.addNewWaveItem);
            getListComponent().getCollectionEditorComponent().addMenuItemToAddButtonLogic(this.addNewWaveEntryItem);
            getListComponent().getCollectionEditorComponent().addMenuItemToAddButtonLogic(this.addNewStreamEntryItem);
        }

        private static ComboBox<SBRFileUIViewType> createComboBox() {
            ComboBox<SBRFileUIViewType> comboBox = new ComboBox<>(FXCollections.observableArrayList(SBRFileUIViewType.values()));
            comboBox.setValue(SBRFileUIViewType.ENTRIES);
            comboBox.setConverter(new AbstractStringConverter<>(SBRFileUIViewType::getDisplayName));
            comboBox.setCellFactory(listView -> new AbstractAttachmentCell<>((viewType, index) -> viewType != null ? viewType.getDisplayName() : "NULL"));
            return comboBox;
        }
    }

    @Getter
    @AllArgsConstructor
    private enum SBRFileUIViewType {
        ENTRIES("Entries"),
        WAVES("Waves");

        private final String displayName;
    }
}