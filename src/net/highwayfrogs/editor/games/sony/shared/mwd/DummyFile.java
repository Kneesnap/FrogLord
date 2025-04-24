package net.highwayfrogs.editor.games.sony.shared.mwd;

import javafx.scene.image.Image;
import lombok.Getter;
import net.highwayfrogs.editor.games.sony.SCGameFile.SCSharedGameFile;
import net.highwayfrogs.editor.games.sony.SCGameInstance;
import net.highwayfrogs.editor.gui.DefaultFileUIController;
import net.highwayfrogs.editor.gui.GameUIController;
import net.highwayfrogs.editor.gui.ImageResource;
import net.highwayfrogs.editor.gui.components.PropertyListViewerComponent.PropertyList;
import net.highwayfrogs.editor.utils.DataSizeUnit;
import net.highwayfrogs.editor.utils.data.reader.DataReader;
import net.highwayfrogs.editor.utils.data.writer.DataWriter;

import java.nio.ByteBuffer;

/**
 * A dummy file that represents a file which has not been implemented yet.
 * Created by Kneesnap on 8/11/2018.
 */
@Getter
public class DummyFile extends SCSharedGameFile {
    private final int length;
    private final ByteBuffer buffer;

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
        return ImageResource.QUESTION_MARK_32.getFxImage();
    }

    @Override
    public GameUIController<?> makeEditorUI() {
        return DefaultFileUIController.loadEditor(getGameInstance(), new DefaultFileUIController<>(getGameInstance(), "Unsupported File Type", getCollectionViewIcon()), this);
    }

    @Override
    public PropertyList addToPropertyList(PropertyList propertyList) {
        propertyList = super.addToPropertyList(propertyList);
        propertyList.add("Size", DataSizeUnit.formatSize(this.length) + " (" + this.length + " bytes)");
        return propertyList;
    }
}