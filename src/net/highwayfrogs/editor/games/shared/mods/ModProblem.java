package net.highwayfrogs.editor.games.shared.mods;

import lombok.Getter;
import lombok.NonNull;

import java.util.List;

/**
 * A single problem found while reading, validating, exporting or installing a mod package.
 * Created by jimbolina on 11/6/2026.
 */
@Getter
public class ModProblem {
    @NonNull private final ModProblemSeverity severity;
    private final String section;
    private final String key;
    @NonNull private final String message;

    public ModProblem(@NonNull ModProblemSeverity severity, String section, String key, @NonNull String message) {
        this.severity = severity;
        this.section = section;
        this.key = key;
        this.message = message;
    }

    /**
     * Creates an error problem.
     * @param section the manifest section the problem belongs to, or null
     * @param key the key the problem belongs to, or null
     * @param message the description of the problem
     * @return newProblem
     */
    public static ModProblem error(String section, String key, String message) {
        return new ModProblem(ModProblemSeverity.ERROR, section, key, message);
    }

    /**
     * Creates a warning problem.
     * @param section the manifest section the problem belongs to, or null
     * @param key the key the problem belongs to, or null
     * @param message the description of the problem
     * @return newProblem
     */
    public static ModProblem warning(String section, String key, String message) {
        return new ModProblem(ModProblemSeverity.WARNING, section, key, message);
    }

    /**
     * Returns true if this problem is an error.
     */
    public boolean isError() {
        return this.severity == ModProblemSeverity.ERROR;
    }

    /**
     * Tests if any of the given problems is an error.
     * @param problems the problems to test
     * @return true iff at least one problem is an error
     */
    public static boolean hasErrors(List<ModProblem> problems) {
        if (problems == null)
            return false;

        for (int i = 0; i < problems.size(); i++)
            if (problems.get(i).isError())
                return true;

        return false;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder(this.severity.name());
        if (this.section != null || this.key != null) {
            builder.append(" [");
            if (this.section != null)
                builder.append(this.section);
            if (this.key != null)
                builder.append(this.section != null ? "/" : "").append(this.key);
            builder.append(']');
        }

        return builder.append(": ").append(this.message).toString();
    }
}
