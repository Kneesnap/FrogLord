package net.highwayfrogs.editor.file.sound;

import javafx.scene.Node;
import javafx.scene.image.Image;
import lombok.Getter;
import net.highwayfrogs.editor.Utils;
import net.highwayfrogs.editor.file.GameFile;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.sound.VHFile.AudioHeader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.gui.editor.VABController;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Parses VB files and allows exporting to WAV, and importing audio files.
 * Created by rdrpenguin04 on 8/22/2018.
 */
@Getter
public class VBFile extends GameFile {
    private VHFile header;
    private List<GameSound> audioEntries = new ArrayList<>();
    private DataReader cachedReader;

    @Override
    public Image getIcon() {
        return VHFile.ICON;
    }

    @Override
    public Node makeEditor() {
        return loadEditor(new VABController(), "vb", this);
    }

    /**
     * Load the VB file, with the mandatory VH file.
     * @param file The VHFile to load information from.
     */
    public void load(VHFile file) {
        Utils.verify(this.cachedReader != null, "Tried to load VB without a reader.");
        this.header = file;
        load(this.cachedReader);
        this.cachedReader = null;
    }

    @Override
    public void load(DataReader reader) {
        if (getHeader() == null) {
            this.cachedReader = reader;
            return;
        }

        AtomicInteger atomicId = getHeader().getSuppliedSoundId();
        while (header.getEntries().size() > atomicId.get()) {
            AudioHeader vhEntry = header.getEntries().get(atomicId.get());
            GameSound audioEntry = new PCSound(atomicId.get(), vhEntry);

            int byteSize = vhEntry.getDataSize();
            int readLength = byteSize / audioEntry.getByteWidth();
            reader.jumpTemp(vhEntry.getDataStartOffset());

            if (!reader.hasMore() || reader.getIndex() + byteSize > reader.getSize())
                return; // For some reason, the .VH files have way more entries than the VB has files. It looks to me like the VH has entries for all audio files, not just ones present in the VB.

            for (int i = 0; i < readLength; i++)
                audioEntry.getAudioData().add(reader.readInt(audioEntry.getByteWidth()));
            reader.jumpReturn();

            this.audioEntries.add(audioEntry);
            atomicId.incrementAndGet();
        }
    }

    @Override
    public void save(DataWriter writer) {
        for (GameSound entry : getAudioEntries())
            for (int toWrite : entry.getAudioData())
                writer.writeNumber(toWrite, entry.getByteWidth());
    }

    @Getter
    public static class PCSound extends GameSound {
        private AudioHeader header;

        public PCSound(int vanillaTrackId, AudioHeader vhEntry) {
            super(vanillaTrackId);
            this.header = vhEntry;
        }

        @Override
        public int getChannelCount() {
            return header.getChannels();
        }

        @Override
        public void setChannelCount(int channelCount) {
            header.setChannels(channelCount);
        }

        @Override
        public int getSampleRate() {
            return header.getSampleRate();
        }

        @Override
        public void setSampleRate(int newSampleRate) {
            header.setSampleRate(newSampleRate);
        }

        @Override
        public int getBitWidth() {
            return header.getBitWidth();
        }

        @Override
        public void setBitWidth(int newBitWidth) {
            header.setBitWidth(newBitWidth);
        }

        @Override
        public void setDataSize(int newSize) {
            header.setDataSize(newSize);
        }
    }
}
