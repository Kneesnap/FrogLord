package net.highwayfrogs.editor.games.sony.c12;

import net.highwayfrogs.editor.games.sony.shared.SCGameConfig2000;

/**
 * Represents a game config for a build of C-12 Final Resistance.
 * Created by Kneesnap on 4/19/2026.
 */
public class C12Config extends SCGameConfig2000 {
    public C12Config(String internalName) {
        super(internalName);
    }

    /**
     * Returns true iff this version of C-12 is the May E3 2000 build.
     */
    public boolean isMayE3Build() {
        return "2000-05-04-build-0.03a-e3-demo".equals(getInternalName());
    }

    /**
     * Returns true iff this version of C-12 is the December 2000 "Beta Candidate 3" build or later.
     */
    public boolean isAtLeastBetaCandidate3() {
        return !isMayE3Build();
    }
}
