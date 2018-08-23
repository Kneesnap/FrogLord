package net.highwayfrogs.editor.file;

import java.util.ArrayList;
import java.util.List;

import lombok.Getter;
import net.highwayfrogs.editor.file.VHFile.FileEntry;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;

// TODO: Write code to import/export WAV

@Getter
public class VBFile extends GameObject {
    private VHFile header;
    
    
    private List<Byte> numChannels = new ArrayList<>();
    private List<Integer> sampleRates = new ArrayList<>();
    private List<Byte> bitWidths = new ArrayList<>();
    private List<List<Integer>> audioData = new ArrayList<>();
    
    public VBFile(VHFile header) {
        this.header = header;
    }
    
    @Override
    public void load(DataReader reader) {
        for(FileEntry entry : header.getEntries()) {
            getNumChannels().add((byte)entry.getChannels());
            getSampleRates().add(entry.getSampleRate());
            byte bitWidth;
            getBitWidths().add(bitWidth = (byte)entry.getBitWidth());
            reader.jumpTemp(entry.getDataStartOffset());
            int size = entry.getDataSize();
            List<Integer> curData;
            getAudioData().add(curData = new ArrayList<Integer>());
            for(int i = 0; i < size/bitWidth; i++) {
                curData.add(bitWidth == 8 ? reader.readByte() :
                    (bitWidth == 16 ? reader.readShort() : reader.readInt() /* Doubt we'll ever have 32-bit... */));
            }
        }
    }
    
    /**
     * Run BEFORE saving the index back...
     */
    @Override
    public void save(DataWriter writer) {
        header.getEntries().clear();
        int curPos = 0;
        for(int i = 0; i < getAudioData().size(); i++) {
            FileEntry entry = new FileEntry();
            entry.setBitWidth(getBitWidths().get(i));
            entry.setChannels(getNumChannels().get(i));
            entry.setSampleRate(getSampleRates().get(i));
            entry.setDataStartOffset(curPos);
            List<Integer> data = getAudioData().get(i);
            entry.setDataSize(data.size() * entry.getBitWidth() / 8);
            for(int j = 0; j < data.size(); j++) {
                if(entry.getBitWidth() == 8)
                    writer.writeByte((byte)((int)data.get(i)));
                else if(entry.getBitWidth() == 16)
                    writer.writeShort((short)((int)data.get(i)));
                else
                    writer.writeInt(data.get(i));
            }
        }
    }
}
