package net.highwayfrogs.editor.file.config.script.constants;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Boolean constants.
 * Created by Kneesnap on 8/1/2019.
 */
@Getter
@AllArgsConstructor
public enum ScriptConstantBoolean {
    TRUE(-1),
    FALSE(0);

    private final int value;
}
