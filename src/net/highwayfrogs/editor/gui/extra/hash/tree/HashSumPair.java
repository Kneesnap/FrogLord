package net.highwayfrogs.editor.gui.extra.hash.tree;

import lombok.Getter;

/**
 * Represents a pair of HashSums which are intended to be added together.
 * Created by Kneesnap on 2/25/2022.
 */
@Getter
public class HashSumPair {
    private final HashSum first;
    private final HashSum second;

    public HashSumPair(HashSum first, HashSum second) {
        this.first = first;
        this.second = second;
    }

    @Override
    public int hashCode() {
        return (this.first.getSum() - this.second.getSum()) * this.first.getSum() * this.second.getSum();
    }

    @Override
    public boolean equals(Object object) {
        return (object instanceof HashSumPair)
                && ((HashSumPair) object).first.equals(this.first)
                && ((HashSumPair) object).second.equals(this.second);
    }
}
