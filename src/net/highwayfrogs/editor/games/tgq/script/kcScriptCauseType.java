package net.highwayfrogs.editor.games.tgq.script;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Represents the script cause type.
 * Created by Kneesnap on 6/26/2023.
 */
@Getter
@AllArgsConstructor
public enum kcScriptCauseType {

    UNKNOWN(0x2000);

    private final int typeId;
}
