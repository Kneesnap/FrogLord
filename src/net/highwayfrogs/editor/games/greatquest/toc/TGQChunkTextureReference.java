package net.highwayfrogs.editor.games.greatquest.toc;

import lombok.Getter;
import lombok.Setter;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.greatquest.TGQBinFile;
import net.highwayfrogs.editor.games.greatquest.TGQChunkedFile;
import net.highwayfrogs.editor.games.greatquest.loading.kcLoadContext;

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
    public void afterLoad1(kcLoadContext context) {
        super.afterLoad1(context);
        // We must wait until afterLoad1() because the file object won't exist for files found later in the file if we don't.
        // But, this must run before afterLoad2() because that's when we start doing lookups based on file paths.
        TGQBinFile mainArchive = getMainArchive();
        if (mainArchive != null)
            mainArchive.applyFileName(this.path, true);
    }

    @Override
    public void save(DataWriter writer) {
        super.save(writer);
        writer.writeTerminatedStringOfLength(this.path, PATH_SIZE);
    }
}