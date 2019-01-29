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
}
