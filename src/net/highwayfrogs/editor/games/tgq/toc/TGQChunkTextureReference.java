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
    public void afterLoad1() {
        super.afterLoad1();
        // We must wait until afterLoad1() because the file object won't exist for files found later in the file if we don't.
        // But, this must run before afterLoad2() because that's when we start doing lookups based on file paths.
        if (getParentFile() != null)
            getParentFile().getMainArchive().applyFileName(this.path);
    }

    @Override // Wait until after texture names are resolved to run.
    public void afterLoad2() {
        super.afterLoad2();
        if (getParentFile() == null)
            return;

        TGQFile tgqFile = getOptionalFileByName(this.path);
        if (!(tgqFile instanceof TGQImageFile)) {
            System.out.println("The texture reference '" + getName() + "'/'" + this.path + "' did not resolve to an image. (Got: " + tgqFile + ")");
            return; // Not an image.
        }

        // When we load, any models in this same chunked file as the texture reference would likely always prefer to use the texture reference seen in the same chunk, in the case of conflicting texture file names.
        // Haven't checked if this is what the actual game does, but I think this is a good idea.
        TGQImageFile imageFile = (TGQImageFile) tgqFile;
        for (kcCResource resource : getParentFile().getChunks()) {
            if (resource instanceof kcCResourceModel)
                ((kcCResourceModel) resource).resolveMaterialTextures(this, imageFile);
            if (resource instanceof OTTChunk)
                ((OTTChunk) resource).resolveMaterialTextures(this, imageFile);
            // TODO: TriMesh?
        }
    }

    @Override
    public void save(DataWriter writer) {
        super.save(writer);
        writer.writeTerminatedStringOfLength(this.path, PATH_SIZE, PATH_TERMINATOR);
    }
}
