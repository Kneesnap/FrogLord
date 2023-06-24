package net.highwayfrogs.editor.games.tgq.toc;

import lombok.Getter;
import lombok.Setter;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.tgq.TGQChunkedFile;
import net.highwayfrogs.editor.games.tgq.TGQFile;
import net.highwayfrogs.editor.games.tgq.TGQImageFile;

/**
 * Represents a texture reference.
 * Created by Kneesnap on 8/25/2019.
 */
@Getter
@Setter
public class TGQChunkTextureReference extends kcCResource {
    private String path;

    private static final int PATH_SIZE = 260;
    private static final byte PATH_TERMINATOR = (byte) 0xCD;

    public TGQChunkTextureReference(TGQChunkedFile parentFile) {
        super(parentFile, KCResourceID.TEXTURE);
    }

    @Override
    public void load(DataReader reader) {
        super.load(reader);
        this.path = reader.readTerminatedStringOfLength(PATH_SIZE);
    }

    @Override
    public void afterLoad() {
        super.afterLoad();
        if (getParentFile() == null)
            return;

        getParentFile().getMainArchive().applyFileName(this.path);
        TGQFile tgqFile = getParentFile().getMainArchive().getFileByName(this.path);
        if (!(tgqFile instanceof TGQImageFile))
            return; // Not an image.

        TGQImageFile imageFile = (TGQImageFile) tgqFile;
        for (TGQFile file : getParentFile().getMainArchive().getFiles())
            if (file instanceof TGQChunkedFile)
                for (kcCResource chunk : ((TGQChunkedFile) file).getChunks())
                    if (chunk instanceof TGQChunk3DModel)
                        ((TGQChunk3DModel) chunk).load(this, imageFile);
    }

    @Override
    public void save(DataWriter writer) {
        super.save(writer);
        writer.writeTerminatedStringOfLength(this.path, PATH_SIZE, PATH_TERMINATOR);
    }
}
