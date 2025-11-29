package net.highwayfrogs.editor.games.konami.greatquest.chunks;

import net.highwayfrogs.editor.gui.components.propertylist.PropertyListNode;
import net.highwayfrogs.editor.system.IntList;
import net.highwayfrogs.editor.utils.data.reader.DataReader;
import net.highwayfrogs.editor.utils.data.writer.DataWriter;

import java.util.*;

/**
 * A group of resource chunks. In most versions, there is one table of contents per chunked file, containing all resources.
 * However, in multi-language versions (such as PS2 PAL), these are also used to split certain translated resources by language. (eg: Italian vs French)
 * Created by Kneesnap on 4/1/2020.
 */
public class kcCResourceTableOfContents extends kcCResource {
    private final IntList hashes = new IntList();
    private int hashReadIndex = -1;
    final List<kcCResource> resourceChunks = new ArrayList<>();

    private final List<kcCResource> immutableResourceChunks = Collections.unmodifiableList(this.resourceChunks);

    private static final String SECTION_NAME = "TOC"; // The hash is 4293, which means the section name is 'TOC'. PS2 PAL is the only build seen to contain multiple TOC chunks per file, but they also use the same name/hash too.

    public kcCResourceTableOfContents(GreatQuestChunkedFile chunkedFile) {
        super(chunkedFile, KCResourceID.TOC);
        getSelfHash().setHash(0);
        setName(SECTION_NAME, false); // Ensure the name of the section is correct, even though the hash should be zero.
    }

    /**
     * Returns true iff the resources found within this group should be sorted.
     */
    public boolean shouldResourcesBeSorted() {
        return getParentFile().getTableOfContents().get(0) == this;
    }

    /**
     * Gets the chunks tracked by this resource group.
     */
    public List<kcCResource> getResourceChunks() {
        return this.immutableResourceChunks;
    }

    /**
     * Test if the following resource instance is contained by this group/table of contents.
     * @param resource the resource to test
     * @return true iff the resource is tracked by this resource group/table of contents.
     */
    public boolean contains(kcCResource resource) {
        if (resource == null)
            throw new NullPointerException("resource");
        if (resource.getParentFile() != getParentFile())
            return false;

        return indexOf(resource) >= 0;
    }

    /**
     * Gets the index of a resource within getResourceChunks(), if present.
     * Uses binary search when possible.
     * @param resource the resource to find the index of.
     * @return index, or -1 if not found
     */
    public int indexOf(kcCResource resource) {
        if (resource == null)
            throw new NullPointerException("resource");
        if (!shouldResourcesBeSorted())
            return this.resourceChunks.indexOf(resource);

        int searchIndex = Collections.binarySearch(this.resourceChunks, resource, GreatQuestChunkedFile.RESOURCE_ORDERING);
        if (searchIndex < 0)
            return -1;

        // Search for the research to the left.
        int resourceIndex = searchIndex;
        kcCResource temp = null;
        while (resourceIndex >= 0 && (temp = this.resourceChunks.get(resourceIndex)) != resource && GreatQuestChunkedFile.RESOURCE_ORDERING.compare(resource, temp) == 0)
            resourceIndex--;

        // Search for the resource to the right.
        if (resourceIndex < 0 || temp != resource) {
            resourceIndex = searchIndex + 1;
            while (this.resourceChunks.size() > resourceIndex && (temp = this.resourceChunks.get(resourceIndex)) != resource && GreatQuestChunkedFile.RESOURCE_ORDERING.compare(resource, temp) == 0)
                resourceIndex++;

            // Couldn't find the index to remove from.
            if (resourceIndex >= this.resourceChunks.size())
                return -1;
        }

        return (temp == resource) ? resourceIndex : -1;
    }

    @Override
    public void load(DataReader reader) {
        super.load(reader);
        this.hashes.clear();
        while (reader.hasMore())
            this.hashes.add(reader.readInt());
        this.hashReadIndex = 0;
    }

    @Override
    public void save(DataWriter writer) {
        super.save(writer);
        updateHashList();
        for (int i = 0; i < this.hashes.size(); i++)
            writer.writeInt(this.hashes.get(i));
    }

    @Override
    public void addToPropertyList(PropertyListNode propertyList) {
        super.addToPropertyList(propertyList);
        propertyList.add("Hashes", this.hashes.size());
    }

    /**
     * Gets the next available hash from the read hash list.
     * Used exclusively for loading resources in a chunked file.
     * Do not call this in most situations.
     */
    int getNextHash() {
        if (this.hashReadIndex < 0)
            throw new IllegalStateException("Hashes cannot be iterated yet.");
        if (this.hashReadIndex >= this.hashes.size())
            throw new IllegalStateException("A new resource chunk was found after all kcCResourceTableOfContents entries were used.");

        return this.hashes.get(this.hashReadIndex++);
    }

    /**
     * Validate reading resources finished with the expected number of resources.
     */
    void validateReadOkay() {
        if (this.hashes.size() != this.resourceChunks.size())
            throw new IllegalStateException("A new kcCResourceTableOfContents was found despite the last one not having been completed! ("
                    + this.hashes.size() + " chunks expected, but " + this.resourceChunks.size() + " were found.)");
    }

    /**
     * Gets the insertion index for a particular resource chunk
     * @param resource the resource chunk to try to insert
     * @return the index to insert the resource at
     */
    int getResourceInsertionIndex(kcCResource resource) {
        if (!shouldResourcesBeSorted())
            return this.resourceChunks.size();

        int insertionIndex;
        int searchResult = Collections.binarySearch(this.resourceChunks, resource, GreatQuestChunkedFile.RESOURCE_ORDERING);
        if (searchResult >= 0) { // This happens when there are name collisions (such as trimesh files which all call themselves "unnamed" and do not appear to have any sort order.)
            insertionIndex = searchResult + 1;
            while (this.resourceChunks.size() > insertionIndex && GreatQuestChunkedFile.RESOURCE_ORDERING.compare(resource, this.resourceChunks.get(insertionIndex)) == 0)
                insertionIndex++;
        } else { // Found a spot to insert it at.
            insertionIndex = -(searchResult + 1);
        }

        return insertionIndex;
    }

    /**
     * Updates the hash list, in preparation for saving.
     */
    private void updateHashList() {
        Map<Integer, kcCResource> collisionMap = new HashMap<>();

        this.hashReadIndex = -1;
        this.hashes.clear();
        for (int i = 0; i < this.resourceChunks.size(); i++) {
            kcCResource chunk = this.resourceChunks.get(i);

            kcCResource collidingChunk = collisionMap.put(chunk.getHash(), chunk);
            if (collidingChunk != null)
                throw new IllegalStateException("Found multiple resources (" + chunk.getHashAsHexString() + "/" + chunk.getCollectionViewDisplayName()
                        + ") and (" + collidingChunk.getHashAsHexString() + "/" + collidingChunk.getCollectionViewDisplayName()
                        + ") which share the same hash.");

            this.hashes.add(chunk.getHash());
        }
    }
}