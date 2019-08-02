package net.highwayfrogs.editor.file.config.script.constants;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Register toggles.
 * Created by Kneesnap on 8/1/2019.
 */
@Getter
@AllArgsConstructor
public enum ScriptConstantRegisterToggle {
    REGISTERS(1),
    NO_REGISTERS(0);

    private final int value;
}
