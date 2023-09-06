package net.highwayfrogs.editor.gui.extra.hash;

import java.util.List;

/**
 * Represents a generator which can generate possible strings for a given hash.
 * Created by Kneesnap on 2/25/2022.
 */
public interface IHashStringGenerator {
    /**
     * Generates strings
     * @param targetLinkerHash The hash to target the generation of.
     * @param searchQuery      The query to use to restrict what is displayed. Behavior can vary with different generators.
     * @return Generated strings.
     */
    public List<String> generateStrings(int targetLinkerHash, String searchQuery);

    /**
     * Called to setup the string generator.
     * @param controller The UI controller which is using this generator. Null should be supported.
     */
    public void onSetup(HashPlaygroundController controller);
}
