package net.highwayfrogs.editor.file.config;

import lombok.AllArgsConstructor;
import lombok.Getter;
import net.highwayfrogs.editor.file.config.exe.MapBook;
import net.highwayfrogs.editor.file.config.exe.ThemeBook;
import net.highwayfrogs.editor.file.config.exe.pc.PCMapBook;
import net.highwayfrogs.editor.file.config.exe.pc.PCThemeBook;
import net.highwayfrogs.editor.file.config.exe.psx.PSXMapBook;
import net.highwayfrogs.editor.file.config.exe.psx.PSXThemeBook;
import net.highwayfrogs.editor.games.sony.frogger.FroggerGameInstance;

import java.util.function.Function;

/**
 * The two Frogger target platforms.
 * Created by Kneesnap on 11/27/2018.
 */
@Getter
@AllArgsConstructor
public enum TargetPlatform {
    PSX(PSXMapBook::new, PSXThemeBook::new),
    PC(PCMapBook::new, PCThemeBook::new);

    private final Function<FroggerGameInstance, MapBook> mapBookMaker;
    private final Function<FroggerGameInstance, ThemeBook> themeBookMaker;

    /**
     * Create a new MapBook from a frogger instance.
     * @param instance The instance to use to determine the theme book type to create.
     * @return mapBook
     */
    public static MapBook makeNewMapBook(FroggerGameInstance instance) {
        if (instance.getVersionConfig().isAtLeastRetailWindows()) {
            return PC.getMapBookMaker().apply(instance);
        } else {
            return PSX.getMapBookMaker().apply(instance);
        }
    }

    /**
     * Create a new ThemeBook from a frogger instance.
     * @param instance The instance to use to determine the theme book type to create.
     * @return mapBook
     */
    public static ThemeBook makeNewThemeBook(FroggerGameInstance instance) {
        if (instance.getVersionConfig().isAtLeastRetailWindows()) {
            return PC.getThemeBookMaker().apply(instance);
        } else {
            return PSX.getThemeBookMaker().apply(instance);
        }
    }
}