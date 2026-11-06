package net.highwayfrogs.editor.games.sony.frogger.mods;

import lombok.Getter;
import lombok.NonNull;
import net.highwayfrogs.editor.games.shared.mods.ModManifest;

import java.util.List;

/**
 * A row of the demo table in a Frogger mod manifest: 'level,demo file,unlock level', or 'skip'.
 * Created by jimbolina on 11/6/2026.
 */
@Getter
public class FroggerModDemoRow {
    private final int index;
    private final String levelCode;
    private final String demoName;
    private final String unlockCode;
    private final boolean skipped;

    public static final String SKIP_VALUE = "skip";

    private FroggerModDemoRow(int index, String levelCode, String demoName, String unlockCode, boolean skipped) {
        this.index = index;
        this.levelCode = levelCode;
        this.demoName = demoName;
        this.unlockCode = unlockCode;
        this.skipped = skipped;
    }

    /**
     * Creates a row which plays a demo.
     * @param index the table row index
     * @param levelCode the level the demo plays
     * @param demoName the demo file name
     * @param unlockCode the level which must be unlocked for the demo to play
     * @return demoRow
     */
    public static FroggerModDemoRow create(int index, @NonNull String levelCode, @NonNull String demoName, @NonNull String unlockCode) {
        return new FroggerModDemoRow(index, levelCode, demoName, unlockCode, false);
    }

    /**
     * Creates a row which is skipped.
     * @param index the table row index
     * @return demoRow
     */
    public static FroggerModDemoRow createSkipped(int index) {
        return new FroggerModDemoRow(index, null, null, null, true);
    }

    /**
     * Parses a row value.
     * @param index the table row index
     * @param value the value text
     * @return demoRow, or null if the value is malformed
     */
    public static FroggerModDemoRow parse(int index, String value) {
        if (value == null)
            return null;
        if (SKIP_VALUE.equalsIgnoreCase(value.trim()))
            return createSkipped(index);

        List<String> parts = ModManifest.parseList(value);
        if (parts == null || parts.size() != 3)
            return null;

        return create(index, parts.get(0), parts.get(1), parts.get(2));
    }

    /**
     * Gets the row value as written in the manifest.
     * @return value
     */
    public String toValue() {
        return this.skipped ? SKIP_VALUE : this.levelCode + "," + this.demoName + "," + this.unlockCode;
    }

    @Override
    public String toString() {
        return this.index + "=" + toValue();
    }
}
