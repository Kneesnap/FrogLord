package net.highwayfrogs.editor.file.mof.poly_anim;

import lombok.Getter;
import lombok.Setter;
import net.highwayfrogs.editor.file.GameObject;
import net.highwayfrogs.editor.file.mof.MOFPart;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;

import java.util.ArrayList;
import java.util.List;

/**
 * Holds anim entries.
 * Created by Kneesnap on 1/9/2019.
 */
@Getter
public class MOFPartPolyAnimEntryList extends GameObject {
    private List<MOFPartPolyAnimEntry> entries = new ArrayList<>();
    private transient int tempSavePointer;
    @Setter private transient MOFPart parent;

    public MOFPartPolyAnimEntryList(MOFPart parent) {
        this.parent = parent;
    }

    @Override
    public void load(DataReader reader) {
        parent.getLoadAnimEntryListMap().put(reader.getIndex(), this);
        parent.getPartPolyAnimLists().add(this);

        int entryCount = reader.readInt();
        for (int i = 0; i < entryCount; i++) {
            MOFPartPolyAnimEntry entry = new MOFPartPolyAnimEntry();
            entry.load(reader);
            entries.add(entry);
        }
    }

    @Override
    public void save(DataWriter writer) {
        this.tempSavePointer = writer.getIndex();
        writer.writeInt(entries.size());
        entries.forEach(entry -> entry.save(writer));
    }
}
