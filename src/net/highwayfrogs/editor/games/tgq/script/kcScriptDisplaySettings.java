package net.highwayfrogs.editor.games.tgq.script;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Map;

/**
 * The settings used to display human-readable scripts.
 * Created by Kneesnap on 6/27/2023.
 */
@Getter
@AllArgsConstructor
public class kcScriptDisplaySettings {
    private Map<Integer, String> namesByHash;
    private boolean showLabels;
    private boolean showUnusedValues;
}
