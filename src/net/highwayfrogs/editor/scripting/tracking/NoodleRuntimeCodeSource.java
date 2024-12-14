package net.highwayfrogs.editor.scripting.tracking;

import lombok.Getter;

/**
 * A NoodleCodeSource designed for code runtime.
 */
@Getter
public final class NoodleRuntimeCodeSource extends NoodleCodeSource {
    private final String sourceDisplay;
    private final int index;

    public NoodleRuntimeCodeSource(String sourceDisplay, int index) {
        this.sourceDisplay = sourceDisplay;
        this.index = index;
    }

    @Override
    public String getDisplay() {
        return this.sourceDisplay;
    }
}
