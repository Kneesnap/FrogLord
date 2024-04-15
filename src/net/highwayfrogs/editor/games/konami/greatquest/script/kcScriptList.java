package net.highwayfrogs.editor.games.konami.greatquest.script;

import lombok.Getter;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.konami.greatquest.GreatQuestChunkedFile;
import net.highwayfrogs.editor.games.konami.greatquest.GreatQuestUtils;
import net.highwayfrogs.editor.games.konami.greatquest.script.interim.kcInterimScriptEffect;
import net.highwayfrogs.editor.games.konami.greatquest.script.interim.kcScriptListInterim;
import net.highwayfrogs.editor.games.konami.greatquest.script.interim.kcScriptTOC;
import net.highwayfrogs.editor.games.konami.greatquest.script.kcScript.kcScriptFunction;
import net.highwayfrogs.editor.games.konami.greatquest.toc.KCResourceID;
import net.highwayfrogs.editor.games.konami.greatquest.toc.kcCResource;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a list of scripts.
 * The logic has been determined by reading kcCScriptMgr::Load() from the PS2 PAL version.
 * Created by Kneesnap on 6/26/2023.
 */
@Getter
public class kcScriptList extends kcCResource {
    private final List<kcScript> scripts = new ArrayList<>();
    private byte[] rawUnhandledData;

    public static final String GLOBAL_SCRIPT_NAME = "scriptdata";
    public static final int GLOBAL_SCRIPT_NAME_HASH = GreatQuestUtils.hash("scriptdata");

    public kcScriptList(GreatQuestChunkedFile parentFile) {
        super(parentFile, KCResourceID.RAW);
    }

    @Override
    public void load(DataReader reader) {
        super.load(reader);

        // Read interim list.
        kcScriptListInterim interim = new kcScriptListInterim();
        interim.load(reader);

        // Read unhandled data.
        this.rawUnhandledData = null;
        if (reader.hasMore()) {
            this.rawUnhandledData = reader.readBytes(reader.getRemaining());
            System.out.println("The kcScriptList '" + getName() + "' "
                    + (getParentFile() != null ? "' " + getParentFile().getFilePath() : "")
                    + " has " + this.rawUnhandledData.length + " unread/unhandled bytes.");
        }

        // Convert interim list to script list.
        this.scripts.clear();
        for (int i = 0; i < interim.getEntries().size(); i++)
            this.scripts.add(kcScript.loadScript(interim, interim.getEntries().get(i)));
    }

    @Override
    public void save(DataWriter writer) {
        super.save(writer);

        // Convert scripts to interim data.
        List<kcScriptTOC> entries = new ArrayList<>();
        List<Integer> statusData = new ArrayList<>();
        List<kcInterimScriptEffect> effects = new ArrayList<>();
        for (int i = 0; i < this.scripts.size(); i++) {
            kcScript script = this.scripts.get(i);

            // Create new entry.
            kcScriptTOC newEntry = new kcScriptTOC(script.calculateCauseTypes(), statusData.size(), script.getFunctions().size(), script.getEffectCount());
            entries.add(newEntry);

            // Add 'cause' & 'effect' data.
            for (int j = 0; j < script.getFunctions().size(); j++) {
                kcScriptFunction function = script.getFunctions().get(j);
                function.saveCauseData(statusData, effects.size() * kcInterimScriptEffect.SIZE_IN_BYTES);
                for (int k = 0; k < function.getEffects().size(); k++)
                    effects.add(function.getEffects().get(k).toInterimScriptEffect());
            }
        }

        // Write interim data.
        kcScriptListInterim interim = new kcScriptListInterim(entries, statusData, effects);
        interim.save(writer);

        // Write unhandled data.
        if (this.rawUnhandledData != null && this.rawUnhandledData.length > 0)
            writer.writeBytes(this.rawUnhandledData);
    }

    /**
     * Writes the script list to a string builder.
     * @param level    The level to lookup any extra data from.
     * @param builder  The builder to write the script to.
     * @param settings The settings used to build the output.
     */
    public void toString(GreatQuestChunkedFile level, StringBuilder builder, kcScriptDisplaySettings settings) {
        for (int i = 0; i < this.scripts.size(); i++) {
            builder.append("// Script #").append(i + 1).append(":\n");
            this.scripts.get(i).toString(level, builder, settings);
            builder.append('\n');
        }
    }
}