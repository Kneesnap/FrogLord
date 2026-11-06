package net.highwayfrogs.editor.file.patch;

import lombok.Getter;
import lombok.NonNull;
import net.highwayfrogs.editor.games.sony.SCGameInstance;
import net.highwayfrogs.editor.utils.NumberUtils;
import net.highwayfrogs.editor.utils.Utils.ProblemResponse;
import net.highwayfrogs.editor.utils.data.writer.DataReceiver;
import net.highwayfrogs.editor.utils.data.writer.DataWriter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * A data receiver which records the executable ranges a patch writes to without changing any bytes.
 * The receiver has no read-back: patch read commands always read the real executable bytes through the game instance's executable reader.
 * Created by jimbolina on 11/6/2026.
 */
public class PatchWriteRecorder implements DataReceiver {
    private final List<PatchWriteRange> writes = new ArrayList<>();
    @Getter private int index;

    @Override
    public void writeByte(byte value) {
        recordWrite(this.index, 1);
        this.index++;
    }

    @Override
    public void writeBytes(byte[] values) {
        writeBytes(values, 0, values.length);
    }

    @Override
    public void writeBytes(byte[] values, int offset, int amount) {
        if (amount <= 0)
            return;

        recordWrite(this.index, amount);
        this.index += amount;
    }

    @Override
    public void setIndex(int newIndex) {
        this.index = newIndex;
    }

    private void recordWrite(int address, int length) {
        if (!this.writes.isEmpty()) {
            PatchWriteRange lastRange = this.writes.get(this.writes.size() - 1);
            if (lastRange.getEndAddress() == address) {
                this.writes.set(this.writes.size() - 1, new PatchWriteRange(lastRange.getAddress(), lastRange.getLength() + length));
                return;
            }
        }

        this.writes.add(new PatchWriteRange(address, length));
    }

    /**
     * Gets the recorded write ranges, sorted by address with overlapping and adjacent ranges merged.
     * @return writtenRanges
     */
    public List<PatchWriteRange> getRanges() {
        List<PatchWriteRange> sorted = new ArrayList<>(this.writes);
        sorted.sort(Comparator.comparingInt(PatchWriteRange::getAddress));

        List<PatchWriteRange> merged = new ArrayList<>();
        for (PatchWriteRange range : sorted) {
            if (!merged.isEmpty()) {
                PatchWriteRange lastRange = merged.get(merged.size() - 1);
                if (range.getAddress() <= lastRange.getEndAddress()) {
                    int endAddress = Math.max(lastRange.getEndAddress(), range.getEndAddress());
                    merged.set(merged.size() - 1, new PatchWriteRange(lastRange.getAddress(), endAddress - lastRange.getAddress()));
                    continue;
                }
            }

            merged.add(range);
        }

        return Collections.unmodifiableList(merged);
    }

    /**
     * Runs a patch's code for the version of the game instance with default arguments, and records the executable ranges it writes.
     * The setup code reads the real executable bytes. No executable byte is changed.
     * Errors are thrown as exceptions.
     * @param instance the game instance to run the patch against
     * @param patch the patch to run
     * @return writtenRanges, sorted by address and merged
     */
    public static List<PatchWriteRange> recordPatchWrites(@NonNull SCGameInstance instance, @NonNull GamePatch patch) {
        PatchWriteRecorder recorder = new PatchWriteRecorder();
        PatchRuntime runtime = new PatchRuntime(instance, patch, new DataWriter(recorder), ProblemResponse.THROW_EXCEPTION);
        runtime.runSetup();
        runtime.run();
        return recorder.getRanges();
    }

    /**
     * A contiguous range of executable bytes written by a patch.
     */
    @Getter
    public static class PatchWriteRange {
        private final int address;
        private final int length;

        public PatchWriteRange(int address, int length) {
            this.address = address;
            this.length = length;
        }

        /**
         * Gets the address directly after the last byte of the range.
         * @return endAddress
         */
        public int getEndAddress() {
            return this.address + this.length;
        }

        @Override
        public String toString() {
            return NumberUtils.toHexString(this.address) + "+" + this.length;
        }
    }
}
