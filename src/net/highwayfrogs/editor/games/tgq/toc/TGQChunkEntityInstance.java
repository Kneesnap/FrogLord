package net.highwayfrogs.editor.games.tgq.toc;

import lombok.Getter;
import lombok.Setter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.tgq.TGQChunkedFile;
import net.highwayfrogs.editor.games.tgq.entity.KCEntityInstance;

/**
 * Holds entity instances? NST probably stands for instance.
 * Created by Kneesnap on 8/25/2019.
 */
@Getter
@Setter
public class TGQChunkEntityInstance extends kcCResource {
    private KCEntityInstance entity;

    public TGQChunkEntityInstance(TGQChunkedFile parentFile) {
        super(parentFile, KCResourceID.ENTITYINST);
    }

    @Override
    public void load(DataReader reader) {
        super.load(reader);

        reader.jumpTemp(reader.getIndex() + Constants.INTEGER_SIZE); // TODO: Add a function which does this to MTF. reader.jumpCurrent();
        int hDesc = reader.readInt(); // Model Hash.
        reader.jumpReturn();

        //this.entity = classID.makeInstance();
        //this.entity.load(reader); // TODO
    }

    @Override
    public void save(DataWriter writer) {
        super.save(writer);
        //this.entity.save(writer);
    }
}
