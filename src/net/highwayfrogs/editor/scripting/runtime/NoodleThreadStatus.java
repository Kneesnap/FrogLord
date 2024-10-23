package net.highwayfrogs.editor.scripting.runtime;

/**
 * Different states a noodle thread can be in.
 */
public enum NoodleThreadStatus {
    NONE,
    RUNNING,
    FINISHED,
    ERROR,
    YIELD,
    CANCELLED,
}
