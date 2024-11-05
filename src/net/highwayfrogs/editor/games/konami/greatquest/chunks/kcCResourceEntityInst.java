package net.highwayfrogs.editor.games.konami.greatquest.chunks;

import lombok.Getter;
import lombok.Setter;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.konami.greatquest.entity.kcEntity3DInst;
import net.highwayfrogs.editor.games.konami.greatquest.entity.kcEntityInst;
import net.highwayfrogs.editor.gui.components.PropertyListViewerComponent.PropertyList;

/**
 * Recreation of the 'kcCResourceEntityInst' class from the PS2 version.
 * TODO: Maybe belongs in entity folder?
 * Created by Kneesnap on 8/25/2019.
 */
@Getter
@Setter
public class kcCResourceEntityInst extends kcCResource {
    private kcEntityInst instance;
    private byte[] dummyBytes;

    public kcCResourceEntityInst(GreatQuestChunkedFile parentFile) {
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

        this.instance = null;
        this.dummyBytes = null;
        if (sizeInBytes == kcEntity3DInst.SIZE_IN_BYTES) {
            this.instance = new kcEntity3DInst(this);
            this.instance.load(reader);
        } else if (sizeInBytes == kcEntityInst.SIZE_IN_BYTES) {
            this.instance = new kcEntityInst(this);
            this.instance.load(reader);
        } else {
            // TODO: Let's reverse engineer this.
            getLogger().severe("Couldn't identify the entity type for '" + getName() + "' from the byte size of " + sizeInBytes + ".");
            this.dummyBytes = reader.readBytes(sizeInBytes);
        }
    }

    @Override
    public void save(DataWriter writer) {
        super.save(writer);
        if (this.instance != null) {
            this.instance.save(writer);
        } else if (this.dummyBytes != null) {
            writer.writeBytes(this.dummyBytes);
        }
    }

    @Override
    public PropertyList addToPropertyList(PropertyList propertyList) {
        propertyList = super.addToPropertyList(propertyList);
        // TODO: ADD ENTITY DATA INTO PROPERTY LIST!
        return propertyList;
    }
}