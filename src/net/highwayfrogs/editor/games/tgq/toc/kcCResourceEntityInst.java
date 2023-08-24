package net.highwayfrogs.editor.games.tgq.toc;

import lombok.Getter;
import lombok.Setter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.tgq.TGQChunkedFile;
import net.highwayfrogs.editor.games.tgq.entity.KCEntityInstance;

/**
 * Recreation of the 'kcCResourceEntityInst' class from the PS2 version.
 * TODO: Maybe belongs in entity folder?
 * TODO: kcClassFactory::CreateDescription & CGreatQuestFactory::CreateDescription
 * TODO: It appears the data here is the 'kcEntityInst' struct.
 * Created by Kneesnap on 8/25/2019.
 */
@Getter
@Setter
public class kcCResourceEntityInst extends kcCResource {
    private KCEntityInstance entity;

    public kcCResourceEntityInst(TGQChunkedFile parentFile) {
        super(parentFile, KCResourceID.ENTITYINST);
    }

    @Override
    public void load(DataReader reader) {
        super.load(reader);

        int size = reader.readInt(); // Remaining bytecount from this point.
        int calculatedSize = reader.getRemaining() + Constants.INTEGER_SIZE;
        if (size != calculatedSize)
            throw new RuntimeException("The expected amount of entity data (" + size + " bytes) different from the actual amount (" + calculatedSize + " bytes).");

        int hDesc = reader.readInt(); // Model Hash...? TODO: This hash seems to be for a resource.
        reader.skipInt(); // This is data is for the class created from 'hDesc' at runtime. It seems to contain uninitialized data otherwise.


        // TODO: kcBaseDesc pointer? (Pointer to the generic resource described by hDesc I think)


        //this.entity = classID.makeInstance();
        //this.entity.load(reader); // TODO

        reader.skipBytes(reader.getRemaining()); // TODO: Finish implementing.
    }

    @Override
    public void save(DataWriter writer) {
        super.save(writer);
        //this.entity.save(writer);
    }
}
