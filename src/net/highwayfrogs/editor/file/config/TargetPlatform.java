package net.highwayfrogs.editor.file.config;

import lombok.AllArgsConstructor;
import lombok.Getter;
import net.highwayfrogs.editor.file.config.exe.MapBook;
import net.highwayfrogs.editor.file.config.exe.ThemeBook;
import net.highwayfrogs.editor.file.config.exe.pc.PCMapBook;
import net.highwayfrogs.editor.file.config.exe.pc.PCThemeBook;
import net.highwayfrogs.editor.file.config.exe.psx.PSXMapBook;
import net.highwayfrogs.editor.file.config.exe.psx.PSXThemeBook;

import java.util.function.Supplier;

/**
 * The two Frogger target platforms.
 * Created by Kneesnap on 11/27/2018.
 */
@Getter
@AllArgsConstructor
public enum TargetPlatform {
    PSX(PSXMapBook::new, PSXThemeBook::new),
    PC(PCMapBook::new, PCThemeBook::new);

    private final Supplier<MapBook> mapBookMaker;
    private final Supplier<ThemeBook> themeBookMaker;

    /**
     * Create a new MapBook from a FroggerExeInfo.
     * @param info The info to make the MapBook from.
     * @return mapBook
     */
    public static MapBook makeNewMapBook(FroggerEXEInfo info) {
        return info.isPrototype() ? PSX.getMapBookMaker().get() : info.getPlatform().getMapBookMaker().get();
    }

    /**
     * Create a new ThemeBook from a FroggerExeInfo.
     * @param info The info to make the ThemeBook from.
     * @return mapBook
     */
    public static ThemeBook makeNewThemeBook(FroggerEXEInfo info) {
        return info.isPrototype() ? PSX.getThemeBookMaker().get() : info.getPlatform().getThemeBookMaker().get();
    }
}
