package net.highwayfrogs.editor.games.sony;

import net.highwayfrogs.editor.games.psx.psyq.PsyQSymbolMap;
import net.highwayfrogs.editor.utils.data.reader.DataReader;

/**
 * Represents an SCGameConfig which can be loaded from
 * Created by Kneesnap on 10/14/2026.
 */
public interface ISCGameConfigPsyQMap {

    /**
     * Generates configuration data based on the symbol map file.
     * @param executableReader The DataReader available to read the executable included with the config.
     * @param symbolMap The debug symbol map to load from
     */
    void generateConfigData(DataReader executableReader, PsyQSymbolMap symbolMap);
}
