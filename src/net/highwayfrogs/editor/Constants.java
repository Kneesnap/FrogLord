package net.highwayfrogs.editor;

import javafx.scene.text.Font;
import lombok.Getter;
import net.highwayfrogs.editor.games.generic.GameConfig;
import net.highwayfrogs.editor.games.generic.IGameType;
import net.highwayfrogs.editor.games.konami.ancientshadow.AncientShadowGameType;
import net.highwayfrogs.editor.games.konami.beyond.FroggerBeyondGameType;
import net.highwayfrogs.editor.games.konami.greatquest.GreatQuestGameType;
import net.highwayfrogs.editor.games.konami.rescue.FroggerRescueGameType;
import net.highwayfrogs.editor.games.renderware.game.RwGenericGameType;
import net.highwayfrogs.editor.games.sony.SCGameType;

import java.awt.*;
import java.util.List;
import java.util.*;

/**
 * Holds constant variables which may come in handy.
 * Created by Kneesnap on 8/10/2018.
 */
public class Constants {
    public static final String NEWLINE = System.lineSeparator();

    public static final int CD_SECTOR_SIZE = 0x800;

    public static final int BYTE_SIZE = 1;
    public static final int SHORT_SIZE = 2;
    public static final int INTEGER_SIZE = 4;
    public static final int FLOAT_SIZE = 4;
    public static final int POINTER_SIZE = INTEGER_SIZE;
    public static final byte NULL_BYTE = (byte) 0;

    public static final int BIT_TRUE = 1;
    public static final int BIT_FALSE = 0;

    public static final int BITS_PER_BYTE = 8;
    public static final int BITS_PER_INTEGER = BITS_PER_BYTE * INTEGER_SIZE;

    public static final String SKY_LAND_PREFIX = "SKY_LAND";

    public static final String VERSION = "v1.0.0 Omega";
    public static final int UPDATE_VERSION = 1; // Update this with every release.

    public static final int BIT_FLAG_0 = 1;
    public static final int BIT_FLAG_1 = 1 << 1;
    public static final int BIT_FLAG_2 = 1 << 2;
    public static final int BIT_FLAG_3 = 1 << 3;
    public static final int BIT_FLAG_4 = 1 << 4;
    public static final int BIT_FLAG_5 = 1 << 5;
    public static final int BIT_FLAG_6 = 1 << 6;
    public static final int BIT_FLAG_7 = 1 << 7;
    public static final int BIT_FLAG_8 = 1 << 8;
    public static final int BIT_FLAG_9 = 1 << 9;
    public static final int BIT_FLAG_10 = 1 << 10;
    public static final int BIT_FLAG_11 = 1 << 11;
    public static final int BIT_FLAG_12 = 1 << 12;
    public static final int BIT_FLAG_13 = 1 << 13;
    public static final int BIT_FLAG_14 = 1 << 14;
    public static final int BIT_FLAG_15 = 1 << 15;
    public static final int BIT_FLAG_16 = 1 << 16;
    public static final int BIT_FLAG_17 = 1 << 17;
    public static final int BIT_FLAG_18 = 1 << 18;
    public static final int BIT_FLAG_19 = 1 << 19;
    public static final int BIT_FLAG_20 = 1 << 20;
    public static final int BIT_FLAG_21 = 1 << 21;
    public static final int BIT_FLAG_22 = 1 << 22;
    public static final int BIT_FLAG_23 = 1 << 23;
    public static final int BIT_FLAG_24 = 1 << 24;
    public static final int BIT_FLAG_25 = 1 << 25;
    public static final int BIT_FLAG_26 = 1 << 26;
    public static final int BIT_FLAG_27 = 1 << 27;
    public static final int BIT_FLAG_28 = 1 << 28;
    public static final int BIT_FLAG_29 = 1 << 29;
    public static final int BIT_FLAG_30 = 1 << 30;
    public static final int BIT_FLAG_31 = 1 << 31;

    public static final Font SYSTEM_BOLD_FONT = new Font("System Bold", 12);
    public static final String DUMMY_FILE_NAME = "NULL";

    public static final boolean LOG_EXE_INFO = false;

    public static final Color COLOR_TURQUOISE = new Color(0, 128, 128);
    public static final Color COLOR_DEEP_GREEN = new Color(0, 128, 0);
    public static final Color COLOR_DARK_YELLOW = new Color(128, 128, 0);
    public static final Color COLOR_TAN = new Color(240, 240, 240);

    @Getter private static final List<IGameType> gameTypes = new ArrayList<>();
    @Getter private static final Map<IGameType, List<GameConfig>> cachedConfigsByGameType = new HashMap<>();

    public static final String HIGHWAY_FROGS_WEBSITE_URL = "https://highwayfrogs.net/";
    public static final String SOURCE_CODE_REPOSITORY_URL = "https://github.com/Kneesnap/FrogLord/";

    public static final String FX_STYLE_INVALID_TEXT = "-fx-text-inner-color: red;";
    public static final double RECOMMENDED_TREE_VIEW_FIXED_CELL_SIZE = 24; // Recommended by https://docs.oracle.com/javase/8/javafx/api/javafx/scene/control/TreeView.html for performance, which was an issue.

    /**
     * Log exe info if the option is enabled.
     * @param obj The object to log.
     */
    public static void logExeInfo(Object obj) {
        if (LOG_EXE_INFO)
            System.out.println(obj);
    }

    static {
        gameTypes.addAll(Arrays.asList(SCGameType.values()));
        gameTypes.add(GreatQuestGameType.INSTANCE);
        gameTypes.add(AncientShadowGameType.INSTANCE);
        gameTypes.add(FroggerBeyondGameType.INSTANCE);
        gameTypes.add(FroggerRescueGameType.INSTANCE);
        gameTypes.add(RwGenericGameType.INSTANCE);
        gameTypes.sort(Comparator.comparing(IGameType::getDisplayName)); // Sort alphabetically.
    }
}