package net.highwayfrogs.editor.file;

import javafx.scene.image.Image;
import lombok.Getter;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.sony.SCGameFile.SCSharedGameFile;
import net.highwayfrogs.editor.games.sony.SCGameInstance;
import net.highwayfrogs.editor.gui.GameUIController;

import java.nio.ByteBuffer;

/**
 * A dummy file that represents a file which has not been implemented yet.
 * Created by Kneesnap on 8/11/2018.
 */
@Getter
public class DummyFile extends SCSharedGameFile {
    private final int length;
    private final ByteBuffer buffer;

    public static final Image ICON = loadIcon("unknown");

    public DummyFile(SCGameInstance instance, int length) {
        super(instance);
        this.length = length;
        this.buffer = ByteBuffer.allocate(length);
    }

    @Override
    public void load(DataReader reader) {
        this.buffer.put(reader.readBytes(length));
    }

    @Override
    public void save(DataWriter writer) {
        writer.writeBytes(getArray());
    }

    /**
     * Gets the file data as an array.
     */
    public byte[] getArray() {
        return this.buffer.array();
    }

    @Override
    public Image getCollectionViewIcon() {
        return ICON;
    }

    @Override
    public GameUIController<?> makeEditorUI() {
        return null;
    }
}