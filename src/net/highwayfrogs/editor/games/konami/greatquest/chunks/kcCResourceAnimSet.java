package net.highwayfrogs.editor.games.konami.greatquest.chunks;

import lombok.Getter;
import net.highwayfrogs.editor.games.konami.greatquest.entity.kcAnimSetDesc;
import net.highwayfrogs.editor.gui.components.propertylist.PropertyListNode;
import net.highwayfrogs.editor.utils.data.reader.DataReader;
import net.highwayfrogs.editor.utils.data.writer.DataWriter;

import java.util.List;

/**
 * Represents either kcCAnimSet or kcAnimSetDesc. One of those.
 * Created by Kneesnap on 4/16/2024.
 */
@Getter
public class kcCResourceAnimSet extends kcCResource {
    private final kcAnimSetDesc animSetDesc;

    public static final String NAME_SUFFIX = "-1AnimSet";

    public kcCResourceAnimSet(GreatQuestChunkedFile parentFile) {
        super(parentFile, KCResourceID.ANIMSET);
        this.animSetDesc = new kcAnimSetDesc(this);
    }

    @Override
    public void load(DataReader reader) {
        super.load(reader);
        this.animSetDesc.load(reader);
    }

    @Override
    public void save(DataWriter writer) {
        super.save(writer);
        this.animSetDesc.save(writer);
    }

    @Override
    public void addToPropertyList(PropertyListNode propertyList) {
        super.addToPropertyList(propertyList);
        this.animSetDesc.addToPropertyList(propertyList);
    }

    /**
     * Get a list containing all resolved animations.
     */
    public List<kcCResourceTrack> getAnimations() {
        return this.animSetDesc.getAnimations();
    }

    /**
     * Test if the animation set contains the given animation
     * @param animation the animation to find
     * @return true if the animation is found
     */
    public boolean contains(kcCResourceTrack animation) {
        return this.animSetDesc.contains(animation);
    }

    /**
     * Adds an animation to the set
     * @param animation the animation to add
     * @return true if it was added
     */
    public boolean addAnimation(kcCResourceTrack animation) {
        return this.animSetDesc.addAnimation(animation);
    }

    /**
     * Removes an animation from the set
     * @param animation the animation to remove
     * @return true if it was removed
     */
    public boolean removeAnimation(kcCResourceTrack animation, boolean deleteEntry) {
        return this.animSetDesc.removeAnimation(animation, deleteEntry);
    }
}