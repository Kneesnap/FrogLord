package net.highwayfrogs.editor.games.sony.shared.overlay;

import lombok.Getter;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.sony.SCGameData.SCSharedGameData;
import net.highwayfrogs.editor.games.sony.SCGameInstance;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a table of overlays.
 * Passed to MRInitialiseOverlays().
 * The way to locate this is usually to just look for a call that looks like MRInitialiseOverlays() in ghidra or to just search for the pointer 0x80010000 in the executable.
 * Created by Kneesnap on 3/11/2024.
 */
@Getter
public class SCOverlayTable extends SCSharedGameData {
    private final List<SCOverlayTableEntry> entries = new ArrayList<>();

    public SCOverlayTable(SCGameInstance instance) {
        super(instance);
    }

    @Override
    public void load(DataReader reader) {
        this.entries.clear();

        SCOverlayTableEntry lastEntry = null;
        do {
            if (lastEntry != null)
                this.entries.add(lastEntry);

            lastEntry = new SCOverlayTableEntry(getGameInstance());
            lastEntry.load(reader);
        } while (lastEntry.getFilePath() != null && lastEntry.getOverlayStartPointer() > 0);
    }

    @Override
    public void save(DataWriter writer) {
        for (int i = 0; i < this.entries.size(); i++)
            this.entries.get(i).save(writer);
    }
}