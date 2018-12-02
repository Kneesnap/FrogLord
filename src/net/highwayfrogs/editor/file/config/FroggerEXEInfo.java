package net.highwayfrogs.editor.file.config;

import javafx.util.Pair;
import lombok.Getter;
import net.highwayfrogs.editor.Utils;
import net.highwayfrogs.editor.file.MWIFile;
import net.highwayfrogs.editor.file.reader.ArraySource;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.ArrayReceiver;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.file.writer.FixedArrayReceiver;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

/**
 * Information about a specific frogger.exe file.
 * Created by Kneesnap on 8/18/2018.
 */
@Getter
public class FroggerEXEInfo extends Config {
    private byte[] exeBytes;
    private File inputFile;
    private File outputFile;

    private static final String NO_REMAP_DATA = "PLACEHOLDER";

    public FroggerEXEInfo(File inputExe, File outputExe, InputStream inputStream) throws IOException {
        super(inputStream);
        this.inputFile = inputExe;
        this.outputFile = outputExe;
    }

    /**
     * Gets the byte-location of this executable's MWI.
     * @return mwiOffset
     */
    public int getMWIOffset() {
        return getInt("mwiOffset");
    }

    /**
     * Get the byte-length of this executable's MWI.
     * @return mwiLength
     */
    public int getMWILength() {
        return getInt("mwiLength");
    }

    /**
     * Get the platform this was released on.
     * @return platform
     */
    public TargetPlatform getPlatform() {
        return getEnum("platform", TargetPlatform.class);
    }

    /**
     * Gets if this is a major demo release.
     * @return isDemo
     */
    public boolean isDemo() {
        return getBoolean("demo");
    }

    /**
     * Get remap info for a specific level.
     * @param levelName The level to get remap info for.
     * @return remapInfo
     */
    public Pair<Integer, Integer> getRemapInfo(String levelName) {
        String remapData = getChild("Remaps").getString(levelName);
        Utils.verify(remapData != null && !remapData.equalsIgnoreCase(NO_REMAP_DATA), "There is no remap data for %s.", levelName);
        String[] split = remapData.split("\\|");
        int remapAddress = Integer.decode(split[0]);
        int remapCount = split.length > 1 ? Integer.parseInt(split[1]) : 200; // Amount of texture remaps. (Bytes / SHORT_SIZE).
        return new Pair<>(remapAddress, remapCount);
    }

    /**
     * Loads the remap table from the Frogger EXE.
     * @param levelName The name of the level.
     * @return remapTable
     */
    public List<Short> getRemapTable(String levelName) {
        List<Short> remapTable = new ArrayList<>();
        Pair<Integer, Integer> data = getRemapInfo(levelName);
        int remapAddress = data.getKey();
        int remapCount = data.getValue();

        DataReader reader = getReader();
        reader.setIndex(remapAddress);
        for (int i = 0; i < remapCount; i++)
            remapTable.add(reader.readShort());

        return remapTable;
    }

    /**
     * Read the MWI file from the executable.
     * @return mwiFile
     */
    public MWIFile readMWI() {
        DataReader reader = getReader();

        reader.setIndex(getMWIOffset());
        byte[] mwiBytes = reader.readBytes(getMWILength());

        DataReader arrayReader = new DataReader(new ArraySource(mwiBytes));
        MWIFile mwiFile = new MWIFile();
        mwiFile.load(arrayReader);
        return mwiFile;
    }

    /**
     * Patch Frogger.exe to use a modded MWI.
     * @param mwiFile    The MWI file to save.
     */
    public void patchEXE(MWIFile mwiFile) {
        ArrayReceiver receiver = new ArrayReceiver();
        DataWriter mwiWriter = new DataWriter(receiver);
        mwiFile.save(mwiWriter);
        mwiWriter.closeReceiver();

        int mwiOffset = getMWIOffset();
        DataWriter exeWriter = getWriter();
        exeWriter.setIndex(mwiOffset);
        exeWriter.writeBytes(receiver.toArray());
        exeWriter.closeReceiver();

        int bytesWritten = exeWriter.getIndex() - mwiOffset;
        Utils.verify(bytesWritten == getMWILength(), "MWI Patching Failed. The size of the written MWI does not match the correct MWI size! [%d/%d]", bytesWritten, getMWILength());
    }

    /**
     * Override remap data in the exe.
     * @param levelName   The name of the level to patch a remap for.
     * @param remapImages The new image remap array.
     */
    public void patchRemapInExe(String levelName, List<Integer> remapImages) {
        DataWriter writer = getWriter();
        Pair<Integer, Integer> pair = getRemapInfo(levelName);
        int remapAddress = pair.getKey();
        int remapCount = pair.getValue();
        Utils.verify(remapCount >= remapImages.size(), "New remap table is larger than the old remap table.");

        writer.jumpTemp(remapAddress);
        remapImages.forEach(writer::writeUnsignedShort);
        writer.jumpReturn();
        writer.closeReceiver();
    }

    /**
     * Gets a writer which rights to the executable.
     * @return writer
     */
    public DataWriter getWriter() {
        loadExeData();
        return new DataWriter(new FixedArrayReceiver(this.exeBytes));
    }

    /**
     * Get a reader which will read the input file.
     * @return dataReader
     */
    public DataReader getReader() {
        loadExeData();
        return new DataReader(new ArraySource(exeBytes));
    }

    private void loadExeData() {
        if (exeBytes != null)
            return;
        try {
            this.exeBytes = Files.readAllBytes(inputFile.toPath());
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
     * Save the Frogger executable.
     */
    public void saveExecutable() {
        try {
            Utils.deleteFile(this.outputFile);
            Files.write(this.outputFile.toPath(), this.exeBytes);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }
}
