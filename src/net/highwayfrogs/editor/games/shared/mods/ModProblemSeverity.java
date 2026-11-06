package net.highwayfrogs.editor.games.shared.mods;

/**
 * How serious a problem found in a mod package is.
 * Created by jimbolina on 11/6/2026.
 */
public enum ModProblemSeverity {
    /**
     * The package cannot be used as written.
     */
    ERROR,

    /**
     * The package can be used, but something looks wrong.
     */
    WARNING
}
