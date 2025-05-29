package net.highwayfrogs.editor.gui.extra.hash;

import java.util.List;

/**
 * Represents a generator which can generate possible strings for a given hash.
 * Created by Kneesnap on 2/25/2022.
 */
public interface IHashStringGenerator {
    /**
     * Generates strings
     * @param controller the controller to generate strings for
     * @return Generated strings.
     */
    List<String> generateStrings(HashPlaygroundController controller);

    /**
     * Called to set up the string generator.
     * @param controller The UI controller which is using this generator. Null should be supported.
     */
    void onSetup(HashPlaygroundController controller);
}
