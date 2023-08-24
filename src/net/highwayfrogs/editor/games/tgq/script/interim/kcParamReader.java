package net.highwayfrogs.editor.games.tgq.script.interim;

import lombok.Getter;
import lombok.Setter;
import net.highwayfrogs.editor.games.tgq.script.kcParam;

/**
 * Allows reading of kcParam arguments sequentially.
 * Created by Kneesnap on 8/23/2023.
 */
@Getter
public class kcParamReader {
    private final kcParam[] arguments;
    @Setter private int currentIndex;

    public kcParamReader(kcParam[] arguments) {
        this.arguments = arguments;
    }

    /**
     * Reads the next parameter.
     */
    public kcParam next() {
        if (this.currentIndex >= this.arguments.length)
            throw new RuntimeException("There are no more arguments left.");

        return this.arguments[this.currentIndex++];
    }

    /**
     * Returns the argument at the specified index.
     * @param index The index to lookup.
     * @return argument at the specified index.
     */
    public kcParam getArgument(int index) {
        if (index < 0 || index >= this.arguments.length)
            throw new RuntimeException("The argument at index " + index + " does not exist.");

        return this.arguments[index];
    }

    /**
     * Returns true while there are more parameters to read.
     */
    public boolean hasMore() {
        return this.arguments.length > this.currentIndex;
    }
}
