package net.highwayfrogs.editor.gui.extra.hash.tree;

import lombok.Getter;
import net.highwayfrogs.editor.gui.extra.hash.FroggerHashUtil;
import net.highwayfrogs.editor.utils.FlagTracker;

import java.util.HashSet;
import java.util.Set;

/**
 * A tree node which represents a particular number, created by adding the values of other nodes.
 * Created by Kneesnap on 2/25/2022.
 */
@Getter
public class HashSum {
    private final int sum;
    private final Set<HashSumPair> pairs;
    private final FlagTracker stringLengths;

    public HashSum(int sum) {
        this.sum = sum;
        this.pairs = new HashSet<>();
        this.stringLengths = new FlagTracker(FroggerHashUtil.MAX_SYMBOL_NAME_LENGTH);
    }

    @Override
    public int hashCode() {
        return this.sum;
    }

    @Override
    public boolean equals(Object object) {
        return (object instanceof HashSum) && ((HashSum) object).sum == this.sum;
    }

    /**
     * Whether or not this HashSum is a single character.
     * @return isSingleCharacter
     */
    public boolean isSingleCharacter() {
        return this.stringLengths.getMinFlag() == 1;
    }

    /**
     * Gets the character which this HashSum represents.
     * Should only be called when this hash sum is a single character.
     */
    public char getCharacter() {
        if (!this.isSingleCharacter())
            throw new RuntimeException("Cannot run getCharacter() on HashSum=" + this.sum + ", as it is not a single character!");
        return (char) (this.sum - 1);
    }
}
