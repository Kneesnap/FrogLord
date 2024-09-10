package net.highwayfrogs.editor.games.konami.greatquest.audio;

import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.image.Image;
import lombok.Getter;
import lombok.NonNull;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.reader.FileSource;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.file.writer.FileReceiver;
import net.highwayfrogs.editor.file.writer.LargeFileReceiver;
import net.highwayfrogs.editor.games.generic.GameData;
import net.highwayfrogs.editor.games.generic.GameObject;
import net.highwayfrogs.editor.games.konami.greatquest.GreatQuestGameFile;
import net.highwayfrogs.editor.games.konami.greatquest.GreatQuestInstance;
import net.highwayfrogs.editor.games.konami.greatquest.GreatQuestLooseGameFile;
import net.highwayfrogs.editor.games.konami.greatquest.audio.SoundChunkFile.SoundChunkIndexFile.SoundChunkBodyFile;
import net.highwayfrogs.editor.games.konami.greatquest.audio.SoundChunkFile.SoundChunkIndexFile.SoundChunkEntry;
import net.highwayfrogs.editor.gui.GameUIController;
import net.highwayfrogs.editor.gui.ImageResource;
import net.highwayfrogs.editor.gui.components.DefaultFileEditorUISoundListComponent;
import net.highwayfrogs.editor.gui.components.DefaultFileEditorUISoundListComponent.IBasicSound;
import net.highwayfrogs.editor.gui.components.DefaultFileEditorUISoundListComponent.IBasicSoundList;
import net.highwayfrogs.editor.gui.components.PropertyListViewerComponent.PropertyList;
import net.highwayfrogs.editor.utils.DataSizeUnit;
import net.highwayfrogs.editor.utils.Utils;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.Clip;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * This is a conceptual file which combines the sound chunk index file with the sound chunk data file.
 * There is just one of these files per game instance (SNDCHUNK.IDX/SNDCHUNK.SCK), and they contain longer audio clips which benefit from streaming instead of getting loaded fully into memory.
 * Eg: This includes voice clips, music, and sound effects which are not used frequently.
 * Created by Kneesnap on 9/9/2024.
 */
public class SoundChunkFile extends GreatQuestGameFile implements IBasicSoundList {
    @Getter private final List<SoundChunkEntry> entries = new ArrayList<>();
    private final SoundChunkIndexFile index;
    private final SoundChunkBodyFile body;

    public static final AudioFormat AUDIO_FORMAT = new AudioFormat(24000, 16, 1, true, false);

    public SoundChunkFile(GreatQuestInstance instance, File idxFile, File sckFile) {
        super(instance);
        this.index = new SoundChunkIndexFile(instance, this, idxFile);
        this.body = new SoundChunkBodyFile(instance, this, sckFile);
    }

    @Override
    public String getFileName() {
        return this.index.getFileName() + "/" + this.body.getFileName();
    }

    @Override
    public String getFilePath() {
        return Utils.stripSingleExtension(this.index.getFilePath());
    }

    @Override
    public GameUIController<?> makeEditorUI() {
        return DefaultFileEditorUISoundListComponent.loadEditor(getGameInstance(), new DefaultFileEditorUISoundListComponent<>(getGameInstance()), this);
    }

    @Override
    public Image getCollectionViewIcon() {
        return ImageResource.MUSIC_NOTE_16.getFxImage();
    }

    @Override
    public void load(DataReader reader) {
        loadFileContents();
    }

    /**
     * Loads the file contents from all the files.
     */
    public void loadFileContents() {
        this.entries.clear();

        // Read index first, so we have the data necessary to read the sounds.
        try {
            DataReader indexReader = new DataReader(new FileSource(this.index.getFile()));
            this.index.load(indexReader);
        } catch (Throwable th) {
            Utils.handleError(getLogger(), th, true, "Failed to read '" + this.index.getFileName() + "', aborting..!");
            return;
        }

        // Read sound data after we can reference the index.
        try {
            DataReader bodyReader = new DataReader(new FileSource(this.body.getFile()));
            this.body.load(bodyReader);
        } catch (Throwable th) {
            Utils.handleError(getLogger(), th, true, "Failed to read '" + this.body.getFileName() + "', aborting..!");
        }
    }

    @Override
    public void save(DataWriter writer) {
        saveFileContents();
    }

    /**
     * Saves the file contents to each of the files.
     */
    public void saveFileContents() {
        // Write Body first (to generate a valid index for the .IDX)
        try {
            DataWriter bodyWriter = new DataWriter(new LargeFileReceiver(this.body.getFile()));
            this.body.save(bodyWriter);
        } catch (Throwable th) {
            Utils.handleError(getLogger(), th, true, "Failed to write '" + this.body.getFileName() + "', aborting..!");
            return;
        }

        // Write index second, after it has been generated with correct information.
        try {
            DataWriter indexWriter = new DataWriter(new FileReceiver(this.index.getFile()));
            this.index.save(indexWriter);
        } catch (Throwable th) {
            Utils.handleError(getLogger(), th, true, "Failed to write '" + this.index.getFileName() + "', aborting..!");
        }
    }

    @Override
    public Collection<? extends IBasicSound> getSounds() {
        return this.entries;
    }

    @Override
    public boolean isAllowAddRemoveOperationUI() {
        return true;
    }

    @Getter
    public static class SoundChunkIndexFile extends GreatQuestLooseGameFile {
        @NonNull private final SoundChunkFile soundChunkFile;
        private final List<kcStreamIndexEntry> indexEntries = new ArrayList<>();

        public SoundChunkIndexFile(GreatQuestInstance instance, SoundChunkFile parentFile, File file) {
            super(instance, file);
            this.soundChunkFile = parentFile;
        }

        @Override
        public void load(DataReader reader) {
            this.indexEntries.clear();

            int entryCount = reader.readInt();
            for (int i = 0; i < entryCount; i++) {
                kcStreamIndexEntry entry = new kcStreamIndexEntry(getGameInstance());
                entry.load(reader);
                this.indexEntries.add(entry);
            }
        }

        @Override
        public void save(DataWriter writer) {
            writer.writeInt(this.indexEntries.size());
            for (int i = 0; i < this.indexEntries.size(); i++)
                this.indexEntries.get(i).save(writer);
        }

        @Override
        public GameUIController<?> makeEditorUI() {
            return this.soundChunkFile.makeEditorUI();
        }

        @Override
        public Image getCollectionViewIcon() {
            return this.soundChunkFile.getCollectionViewIcon();
        }

        @Getter
        public static class kcStreamIndexEntry extends GameData<GreatQuestInstance> {
            private int sfxId = -1;
            private int offset = -1;

            public kcStreamIndexEntry(GreatQuestInstance instance) {
                super(instance);
            }

            @Override
            public void load(DataReader reader) {
                this.sfxId = reader.readInt();
                this.offset = reader.readInt();
            }

            @Override
            public void save(DataWriter writer) {
                writer.writeInt(this.sfxId);
                writer.writeInt(this.offset);
            }
        }

        public static class SoundChunkBodyFile extends GreatQuestLooseGameFile {
            @NonNull private final SoundChunkFile soundChunkFile;

            public SoundChunkBodyFile(GreatQuestInstance instance, SoundChunkFile soundChunkFile, File file) {
                super(instance, file);
                this.soundChunkFile = soundChunkFile;
            }

            @Override
            public GameUIController<?> makeEditorUI() {
                return this.soundChunkFile.makeEditorUI();
            }

            @Override
            public Image getCollectionViewIcon() {
                return this.soundChunkFile.getCollectionViewIcon();
            }

            @Override
            public void load(DataReader reader) {
                List<kcStreamIndexEntry> indexEntries = this.soundChunkFile.index.indexEntries;
                for (int i = 0; i < indexEntries.size(); i++) {
                    kcStreamIndexEntry indexEntry = indexEntries.get(i);
                    kcStreamIndexEntry nextIndexEntry = indexEntries.size() > i + 1 ? indexEntries.get(i + 1) : null;

                    SoundChunkEntry newSoundEntry = new SoundChunkEntry(this.soundChunkFile);
                    newSoundEntry.load(reader, indexEntry, nextIndexEntry);
                    this.soundChunkFile.entries.add(newSoundEntry);
                }
            }

            @Override
            public void save(DataWriter writer) {
                List<kcStreamIndexEntry> indexEntries = this.soundChunkFile.index.indexEntries;
                List<SoundChunkEntry> soundEntries = this.soundChunkFile.entries;

                // Ensure the index entry list is the correct size.
                while (soundEntries.size() > indexEntries.size())
                    indexEntries.add(new kcStreamIndexEntry(getGameInstance()));
                while (indexEntries.size() > soundEntries.size())
                    indexEntries.remove(indexEntries.size() - 1);

                // Save the sound entries and update the corresponding index entries.
                for (int i = 0; i < soundEntries.size(); i++)
                    soundEntries.get(i).save(writer, indexEntries.get(i));
            }
        }

        @Getter
        public static class SoundChunkEntry extends GameObject<GreatQuestInstance> implements IBasicSound {
            @NonNull private final SoundChunkFile soundChunkFile;
            private int id = -1;
            private byte[] rawFileBytes; // This includes a WAV file header on PS2, but NOT on PC, it is just raw sound data, I think?

            public SoundChunkEntry(SoundChunkFile soundChunkFile) {
                super(soundChunkFile.getGameInstance());
                this.soundChunkFile = soundChunkFile;
            }

            /**
             * Loads the sound data from the reader, using the index entry as a reference.
             * @param reader the reader to read the sound data from
             * @param indexEntry the index entry representing this sound.
             * @param nextEntry the index entry representing this sound which comes after this one. If null is received, it is assumed to be the final sound.
             */
            public void load(DataReader reader, kcStreamIndexEntry indexEntry, kcStreamIndexEntry nextEntry) {
                if (indexEntry == null)
                    throw new NullPointerException("indexEntry");

                this.id = indexEntry.getSfxId();
                int soundDataEndIndex = nextEntry != null ? nextEntry.getOffset() : reader.getSize();
                reader.requireIndex(this.soundChunkFile.getLogger(), indexEntry.getOffset(), "Expected SFX Body Data");
                this.rawFileBytes = reader.readBytes(soundDataEndIndex - reader.getIndex());
            }

            /**
             * Saves the sound data to the writer, and updates the index entry to contain correct information.
             * @param writer the writer to write the sound data to
             * @param indexEntry the index entry to update
             */
            public void save(DataWriter writer, kcStreamIndexEntry indexEntry) {
                if (indexEntry == null)
                    throw new NullPointerException("indexEntry");

                // Update the index entry.
                indexEntry.sfxId = this.id;
                indexEntry.offset = writer.getIndex();

                // Write the data.
                if (this.rawFileBytes != null)
                    writer.writeBytes(this.rawFileBytes);
            }

            /**
             * Gets a playable audio clip from the raw audio data.
             */
            public Clip getClip() {
                if (getGameInstance().isPC()) {
                    // The PC version contains raw audio data.
                    return Utils.getClipFromRawAudioData(AUDIO_FORMAT, this.rawFileBytes);
                } else if (getGameInstance().isPS2()) {
                    // The PS2 version contains wav files with full headers.
                    return Utils.getClipFromWavFile(this.rawFileBytes);
                } else {
                    throw new UnsupportedOperationException("Unsupported game platform: " + getGameInstance().getPlatform());
                }
            }

            /**
             * Save the sound data as a wav file.
             * @param outputFile The file to save the sound as, or the directory to save it in.
             */
            public void saveAsWavFile(File outputFile) {
                if (outputFile == null)
                    throw new NullPointerException("outputFile");
                if (outputFile.isDirectory())
                    outputFile = new File(outputFile, Utils.padNumberString(this.id, 4) + ".wav");

                if (getGameInstance().isPC()) {
                    // The PC version contains raw audio data.
                    Utils.saveRawAudioDataToWavFile(outputFile, AUDIO_FORMAT, this.rawFileBytes);
                } else if (getGameInstance().isPS2()) {
                    // The PS2 version contains wav files with full headers.
                    try {
                        Files.write(outputFile.toPath(), this.rawFileBytes);
                    } catch (IOException ex) {
                        Utils.handleError(null, ex, false, "Failed to save audio file '%s'.", outputFile.getName());
                    }
                } else {
                    throw new UnsupportedOperationException("Unsupported game platform: " + getGameInstance().getPlatform());
                }
            }

            /**
             * Loads the audio from the supplied file into this sound entry.
             * @param file the file to load the audio from
             */
            public void loadSupportedAudioFile(File file) {
                if (file == null)
                    throw new NullPointerException("file");
                if (!file.exists() || !file.isFile())
                    throw new RuntimeException(new FileNotFoundException("The file '" + file.getName() + "' was not found!"));

                byte[] rawFileData;
                try {
                    rawFileData = Files.readAllBytes(file.toPath());
                } catch (IOException ex) {
                    throw new RuntimeException("Failed to read contents of file '" + file.getName() + "'.");
                }

                if (getGameInstance().isPC()) {
                    // The PC version contains raw audio data.
                    this.rawFileBytes = Utils.getRawAudioDataConvertedFromWavFile(AUDIO_FORMAT, rawFileData);
                } else if (getGameInstance().isPS2()) {
                    // The PS2 version contains wav files with full headers.
                    if (!Utils.testSignature(rawFileData, "RIFF"))
                        throw new RuntimeException("The provided file does not look like a .wav file!");

                    this.rawFileBytes = rawFileData;
                } else {
                    throw new UnsupportedOperationException("Unsupported game platform: " + getGameInstance().getPlatform());
                }
            }

            @Override
            public String getExportFileName() {
                return Utils.padNumberString(this.id, 4) + ".wav";
            }

            @Override
            public String getCollectionViewDisplayName() {
                return getExportFileName();
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
            public PropertyList addToPropertyList(PropertyList propertyList) {
                propertyList.add("SFX ID", this.id);
                propertyList.add("Sound File Size", this.rawFileBytes.length + " (" + DataSizeUnit.formatSize(this.rawFileBytes.length) + ")");
                return propertyList;
            }

            @Override
            public void setupRightClickMenuItems(ContextMenu contextMenu) {
                MenuItem menuItemImport = new MenuItem("Import Sound");
                contextMenu.getItems().add(menuItemImport);
                menuItemImport.setOnAction(event -> {
                    File inputFile = Utils.promptFileOpen(getGameInstance(), "Specify the sound file to import", "Audio File", "wav");
                    if (inputFile != null) {
                        try {
                            loadSupportedAudioFile(inputFile);
                        } catch (Throwable th) {
                            Utils.handleError(getLogger(), th, true, "Failed to import file '%s'.", inputFile.getName());
                        }
                    }
                });

                MenuItem menuItemExport = new MenuItem("Export Sound");
                contextMenu.getItems().add(menuItemExport);
                menuItemExport.setOnAction(event -> {
                    File outputFile = Utils.promptFileSave(getGameInstance(), "Specify the file to save the chunk data as...", getExportFileName(), "Raw RenderWare Stream", "rawrws");
                    if (outputFile != null) {
                        try {
                            saveAsWavFile(outputFile);
                        } catch (Throwable th) {
                            Utils.handleError(getLogger(), th, true, "Failed to export sound as '%s'.", outputFile.getName());
                        }
                    }
                });
            }
        }
    }
}
