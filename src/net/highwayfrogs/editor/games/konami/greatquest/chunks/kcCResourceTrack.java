package net.highwayfrogs.editor.games.konami.greatquest.chunks;

import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.games.konami.greatquest.IInfoWriter.IMultiLineInfoWriter;
import net.highwayfrogs.editor.games.konami.greatquest.animation.kcTrack;
import net.highwayfrogs.editor.games.konami.greatquest.animation.key.kcTrackKey;
import net.highwayfrogs.editor.gui.components.PropertyListViewerComponent.PropertyList;
import net.highwayfrogs.editor.utils.data.reader.DataReader;
import net.highwayfrogs.editor.utils.data.writer.DataWriter;

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
                getLogger().severe("Expected track %d to have the flag identifying it as the first track? %b, but was: %b", i, expectedFirstTrackFlag, didHaveFirstTrackFlag);

            // Validate "FLAG_HAS_NEXT"
            boolean expectedNextTrackFlag = (this.tracks.size() > i + 1);
            boolean didHaveNextTrackFlag = ((track.getFlags() & kcTrack.FLAG_HAS_NEXT) == kcTrack.FLAG_HAS_NEXT);
            if (expectedNextTrackFlag ^ didHaveNextTrackFlag)
                getLogger().severe("Expected track %d to have the flag identifying there being a next track? %b, but was: %b", i, expectedNextTrackFlag, didHaveNextTrackFlag);
        }
    }

    @Override
    public void save(DataWriter writer) {
        super.save(writer);
        int dataStartAddress = writer.getIndex();
        writer.writeStringBytes(KCResourceID.TRACK.getSignature());
        int dataSizeAddress = writer.writeNullPointer();
        int baseByteOffset = writer.getIndex(); // 40 (32 bytes for name, and 8 for the file ID + size.)

        // Write each track.
        for (int i = 0; i < this.tracks.size(); i++)
            this.tracks.get(i).save(writer, baseByteOffset, i == 0, this.tracks.size() > i + 1);

        // Ensure we get the size right.
        writer.writeIntAtPos(dataSizeAddress, writer.getIndex() - dataStartAddress);
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

    @Override
    public PropertyList addToPropertyList(PropertyList propertyList) {
        propertyList = super.addToPropertyList(propertyList);
        propertyList.add("Tracks", this.tracks.size());
        // TODO: Nest the track information for each track.
        return propertyList;
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

    /**
     * Determines the maxTick used in this animation.
     */
    public int getMaxTick() {
        int maxTick = 0;
        for (int i = 0; i < this.tracks.size(); i++) {
            kcTrack track = this.tracks.get(i);
            if (track.getKeyList().isEmpty())
                continue;

            kcTrackKey<?> trackKey = track.getKeyList().get(track.getKeyList().size() - 1);
            if (trackKey.getTick() > maxTick)
                maxTick = trackKey.getTick();
        }

        return maxTick;
    }

    @Override
    protected void onRemovedFromChunkFile() {
        for (kcCResource resource : getParentFile().getChunks())
            if (resource instanceof kcCResourceAnimSet)
                ((kcCResourceAnimSet) resource).removeAnimation(this, false);

        super.onRemovedFromChunkFile();
    }
}