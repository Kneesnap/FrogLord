package net.highwayfrogs.editor.games.konami.greatquest.chunks;

import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.konami.greatquest.IInfoWriter.IMultiLineInfoWriter;
import net.highwayfrogs.editor.games.konami.greatquest.animation.kcTrack;
import net.highwayfrogs.editor.games.konami.greatquest.file.GreatQuestChunkedFile;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * An animation "track" (think of timelines in video editing software).
 * Created by Kneesnap on 4/16/2024.
 */
public class kcCResourceTrack extends kcCResource implements IMultiLineInfoWriter {
    private final List<kcTrack> tracks = new ArrayList<>(); // These are usually sorted by track ID, but not always. The ordering of this list is likely undefined / not important.
    private final List<kcTrack> immutableTracks = Collections.unmodifiableList(this.tracks);
    private final List<List<kcTrack>> tracksByTag = new ArrayList<>();

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
        this.tracksByTag.clear();
        while (reader.hasMore()) {
            kcTrack newTrack = new kcTrack(this);
            newTrack.load(reader, baseByteOffset);
            this.tracks.add(newTrack);

            if (newTrack.getTag() >= 0) {
                // Ensure the tracks by tag is large enough.
                while (newTrack.getTag() >= this.tracksByTag.size())
                    this.tracksByTag.add(null);

                // Remember the new track by its tag.
                List<kcTrack> existingTracks = this.tracksByTag.get(newTrack.getTag());
                if (existingTracks == null)
                    this.tracksByTag.set(newTrack.getTag(), existingTracks = new ArrayList<>());
                existingTracks.add(newTrack);
            }
        }

        // Validate track flags.
        for (int i = 0; i < this.tracks.size(); i++) {
            kcTrack track = this.tracks.get(i);

            // Validate "FLAG_IS_FIRST"
            boolean expectedFirstTrackFlag = (i == 0);
            boolean didHaveFirstTrackFlag = ((track.getFlags() & kcTrack.FLAG_IS_FIRST) == kcTrack.FLAG_IS_FIRST);
            if (expectedFirstTrackFlag ^ didHaveFirstTrackFlag)
                getLogger().severe("Expected track " + i + " to have the flag identifying it as the first track? " + expectedFirstTrackFlag + ", but was: " + didHaveFirstTrackFlag);

            // Validate "FLAG_HAS_NEXT"
            boolean expectedNextTrackFlag = (this.tracks.size() > i + 1);
            boolean didHaveNextTrackFlag = ((track.getFlags() & kcTrack.FLAG_HAS_NEXT) == kcTrack.FLAG_HAS_NEXT);
            if (expectedNextTrackFlag ^ didHaveNextTrackFlag)
                getLogger().severe("Expected track " + i + " to have the flag identifying there being a next track? " + expectedNextTrackFlag + ", but was: " + didHaveNextTrackFlag);
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
            this.tracks.get(i).save(writer, baseByteOffset, i == 0, this.tracks.size() > i + 1);

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

    /**
     * Gets the animation tracks available for this animation.
     */
    public List<kcTrack> getTracks() {
        return this.immutableTracks;
    }

    /**
     * Get the kcTracks associated with the provided tag.
     */
    public List<kcTrack> getTracksByTag(int tag) {
        return tag >= 0 && tag < this.tracksByTag.size() ? this.tracksByTag.get(tag) : Collections.emptyList();
    }
}