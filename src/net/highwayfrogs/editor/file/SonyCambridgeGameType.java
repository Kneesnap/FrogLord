package net.highwayfrogs.editor.file;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * A registry of different game types supported by FrogLord by Sony Cambridge.
 * Created by Kneesnap on 9/6/2023.
 */
@Getter
@AllArgsConstructor
public enum SonyCambridgeGameType {
    BEAST_WARS(false),
    FROGGER(false),
    MEDIEVIL(false),
    MOONWARRIOR(true),
    MEDIEVIL2(true),
    C12(true);

    private final boolean mwiHasChecksum;
}