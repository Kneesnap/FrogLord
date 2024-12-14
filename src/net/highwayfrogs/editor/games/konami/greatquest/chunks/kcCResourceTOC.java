package net.highwayfrogs.editor.games.konami.greatquest.chunks;

import lombok.Getter;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.gui.components.PropertyListViewerComponent.PropertyList;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents the "table of contents" of a chunk.
 * Created by Kneesnap on 4/1/2020.
 */
@Getter
public class kcCResourceTOC extends kcCResource {
    private final List<Integer> hashes = new ArrayList<>();

    private static final String SECTION_NAME = "TOC"; // The hash is 4293, which means the section name is 'TOC'. PS2 PAL is the only build seen to contain multiple TOC chunks per file, but they also use the same name/hash too.

    public kcCResourceTOC(GreatQuestChunkedFile chunkedFile) {
        super(chunkedFile, KCResourceID.TOC);
        getSelfHash().setHash(0);
        setName(SECTION_NAME, false); // Ensure the name of the section is correct, even though the hash should be zero.
    }

    @Override
    public void load(DataReader reader) {
        super.load(reader);
        while (reader.hasMore())
            this.hashes.add(reader.readInt());
    }

    @Override
    public void save(DataWriter writer) {
        super.save(writer);
        for (int hash : getHashes())
            writer.writeInt(hash);
    }

    @Override
    public PropertyList addToPropertyList(PropertyList propertyList) {
        propertyList = super.addToPropertyList(propertyList);
        propertyList.add("Hashes", this.hashes.size());
        return propertyList;
    }

    /**
     * Generates updated contents, for saving.
     * @param resources the resources to create the hash list from
     */
    public void update(List<kcCResource> resources) {
        if (resources == null)
            throw new NullPointerException("resources");

        Map<Integer, kcCResource> collisionMap = new HashMap<>();

        this.hashes.clear();
        for (int i = 0; i < resources.size(); i++) {
            kcCResource chunk = resources.get(i);

            kcCResource collidingChunk = collisionMap.put(chunk.getHash(), chunk);
            if (collidingChunk != null)
                throw new IllegalStateException("Cannot save chunk " + chunk + " (" + chunk.getHashAsHexString() + "/" + chunk.getName() + ") as it shares the same ID as " + collidingChunk + " (" + collidingChunk.getName() + ")");

            this.hashes.add(chunk.getHash());
        }
    }
}