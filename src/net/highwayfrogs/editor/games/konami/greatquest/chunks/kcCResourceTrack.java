package net.highwayfrogs.editor.games.konami.greatquest.chunks;

import lombok.Getter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.konami.greatquest.IInfoWriter.IMultiLineInfoWriter;
import net.highwayfrogs.editor.games.konami.greatquest.animation.kcTrack;
import net.highwayfrogs.editor.games.konami.greatquest.file.GreatQuestChunkedFile;

import java.util.ArrayList;
import java.util.List;

/**
 * An animation "track" (think of timelines in video editing software).
 * Created by Kneesnap on 4/16/2024.
 */
@Getter
public class kcCResourceTrack extends kcCResource implements IMultiLineInfoWriter {
    private final List<kcTrack> tracks = new ArrayList<>();

    public kcCResourceTrack(GreatQuestChunkedFile parentFile) {
        super(parentFile, KCResourceID.TRACK);
    }

    @Override
    public void load(DataReader reader) {
        super.load(reader);
        reader.verifyString(KCResourceID.TRACK.getSignature()); // For some reason this is here again.
        reader.skipInt(); // Skip the size.
        int baseByteOffset = reader.getIndex(); // 40 (32 bytes for name, and 8 for the file ID + size.)

        this.tracks.clear();
        while (reader.hasMore()) {
            kcTrack newTrack = new kcTrack(this);
            newTrack.load(reader, baseByteOffset);
            this.tracks.add(newTrack);
        }
    }

    @Override
    public void save(DataWriter writer) {
        int dataStartAddress = writer.getIndex();
        super.save(writer);
        writer.writeStringBytes(KCResourceID.TRACK.getSignature());
        int dataSizeAddress = writer.writeNullPointer();
        int baseByteOffset = writer.getIndex(); // 40 (32 bytes for name, and 8 for the file ID + size.)

        // Write each track.
        for (int i = 0; i < this.tracks.size(); i++)
            this.tracks.get(i).save(writer, baseByteOffset);

        // Ensure we get the size right.
        writer.writeAddressAt(dataSizeAddress, writer.getIndex() - dataStartAddress);
    }

    @Override
    public void writeMultiLineInfo(StringBuilder builder, String padding) {
        builder.append(padding).append("kcCResourceTrack['").append(getName()).append("'/").append(getHashAsHexString()).append(", Tracks: ").append(this.tracks.size()).append("]:").append(Constants.NEWLINE);
        String newPadding = padding + " ";
        for (int i = 0; i < this.tracks.size(); i++) {
            this.tracks.get(i).writeMultiLineInfo(builder, newPadding);
            builder.append(Constants.NEWLINE);
        }
    }
}