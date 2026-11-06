package net.highwayfrogs.editor.games.shared.mods;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The outcome of installing a mod package onto an open game: the operations applied in order, the warnings and the errors.
 * Created by jimbolina on 11/6/2026.
 */
@Getter
public class ModInstallResult {
    private final String packageName;
    @Setter private String targetName;
    @Setter private boolean gameModified;
    private final List<String> appliedOperations = new ArrayList<>();
    private final List<ModProblem> problems = new ArrayList<>();

    public ModInstallResult(String packageName) {
        this.packageName = packageName;
    }

    /**
     * Records an operation which was applied to the game.
     * @param operation the description of the operation
     */
    public void addAppliedOperation(@NonNull String operation) {
        this.appliedOperations.add(operation);
    }

    /**
     * Records a problem.
     * @param problem the problem to record
     */
    public void addProblem(@NonNull ModProblem problem) {
        this.problems.add(problem);
    }

    /**
     * Gets the operations applied to the game, in order.
     * @return appliedOperations
     */
    public List<String> getAppliedOperations() {
        return Collections.unmodifiableList(this.appliedOperations);
    }

    /**
     * Gets every problem recorded, in order.
     * @return problems
     */
    public List<ModProblem> getProblems() {
        return Collections.unmodifiableList(this.problems);
    }

    /**
     * Gets the warnings recorded, in order.
     * @return warnings
     */
    public List<ModProblem> getWarnings() {
        List<ModProblem> warnings = new ArrayList<>();
        for (ModProblem problem : this.problems)
            if (!problem.isError())
                warnings.add(problem);

        return warnings;
    }

    /**
     * Gets the errors recorded, in order.
     * @return errors
     */
    public List<ModProblem> getErrors() {
        List<ModProblem> errors = new ArrayList<>();
        for (ModProblem problem : this.problems)
            if (problem.isError())
                errors.add(problem);

        return errors;
    }

    /**
     * Returns true if the install finished without any error.
     */
    public boolean isSuccess() {
        return !ModProblem.hasErrors(this.problems);
    }

    /**
     * Describes the result as text: a summary line, the applied operations, the warnings, the errors and what to do next.
     * @return description
     */
    public String describe() {
        StringBuilder builder = new StringBuilder();
        List<ModProblem> warnings = getWarnings();
        List<ModProblem> errors = getErrors();
        builder.append(isSuccess() ? "Installed" : "Failed to install").append(" '")
                .append(this.packageName != null ? this.packageName : "mod package").append('\'');
        if (this.targetName != null)
            builder.append(" for ").append(this.targetName);
        builder.append(": ").append(this.appliedOperations.size()).append(" operation(s) applied, ")
                .append(warnings.size()).append(" warning(s), ").append(errors.size()).append(" error(s).").append(System.lineSeparator());

        appendSection(builder, "Applied", this.appliedOperations);
        appendProblems(builder, "Warnings", warnings);
        appendProblems(builder, "Errors", errors);

        if (isSuccess()) {
            builder.append("Nothing has been saved yet. Save to finish: FrogLord writes the modded_ pair.");
        } else if (this.gameModified) {
            builder.append("The open game was partly changed before the install stopped. Reload it before saving.");
        } else {
            builder.append("The open game was not changed.");
        }

        return builder.append(System.lineSeparator()).toString();
    }

    private static void appendSection(StringBuilder builder, String title, List<String> lines) {
        if (lines.isEmpty())
            return;

        builder.append(title).append(':').append(System.lineSeparator());
        for (String line : lines)
            builder.append(" - ").append(line).append(System.lineSeparator());
    }

    private static void appendProblems(StringBuilder builder, String title, List<ModProblem> problems) {
        if (problems.isEmpty())
            return;

        builder.append(title).append(':').append(System.lineSeparator());
        for (ModProblem problem : problems)
            builder.append(" - ").append(problem).append(System.lineSeparator());
    }
}
