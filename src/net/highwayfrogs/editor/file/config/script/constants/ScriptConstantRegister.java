package net.highwayfrogs.editor.file.config.script.constants;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * A registry of registers.
 * Created by Kneesnap on 8/1/2019.
 */
@Getter
@AllArgsConstructor
public enum ScriptConstantRegister {
    REGISTER_0(0),
    REGISTER_1(1),
    REGISTER_2(2),
    REGISTER_3(3),
    REGISTER_4(4),
    REGISTER_5(5);

    private final int value;
}
