package net.highwayfrogs.editor.games.konami.greatquest.toc;

import lombok.Getter;
import lombok.Setter;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.konami.greatquest.TGQChunkedFile;
import net.highwayfrogs.editor.games.konami.greatquest.entity.kcEntity3DInst;
import net.highwayfrogs.editor.games.konami.greatquest.entity.kcEntityInst;

/**
 * Recreation of the 'kcCResourceEntityInst' class from the PS2 version.
 * TODO: Maybe belongs in entity folder?
 * Created by Kneesnap on 8/25/2019.
 */
@Getter
@Setter
public class kcCResourceEntityInst extends kcCResource {
    private kcEntityInst entity;
    private byte[] dummyBytes;

    public kcCResourceEntityInst(TGQChunkedFile parentFile) {
        super(parentFile, KCResourceID.ENTITYINST);
    }

    @Override
    public void load(DataReader reader) {
        super.load(reader);

        reader.jumpTemp(reader.getIndex());
        int sizeInBytes = reader.readInt(); // Number of bytes used for entity data.
        reader.jumpReturn();

        int calculatedSize = reader.getRemaining(); // We've returned to before the size integer was read.
        if (sizeInBytes != calculatedSize)
            throw new RuntimeException("The expected amount of entity data (" + sizeInBytes + " bytes) different from the actual amount (" + calculatedSize + " bytes).");

        if (sizeInBytes == kcEntity3DInst.SIZE_IN_BYTES) {
            this.entity = new kcEntity3DInst(this);
            this.entity.load(reader);
        } else if (sizeInBytes == kcEntityInst.SIZE_IN_BYTES) {
            this.entity = new kcEntityInst(this);
            this.entity.load(reader);
        } else {
            // TODO: Let's reverse engineer this.
            System.err.println("Couldn't identify the entity type for '" + getName() + "' from the byte size of " + sizeInBytes + ".");
            this.dummyBytes = reader.readBytes(sizeInBytes);
        }
    }

    @Override
    public void save(DataWriter writer) {
        super.save(writer);
        if (this.entity != null) {
            this.entity.save(writer);
        } else if (this.dummyBytes != null) {
            writer.writeBytes(this.dummyBytes);
        }
    }
}