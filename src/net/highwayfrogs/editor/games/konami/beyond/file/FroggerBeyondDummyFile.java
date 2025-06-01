package net.highwayfrogs.editor.games.konami.beyond.file;

import javafx.scene.image.Image;
import net.highwayfrogs.editor.games.shared.basic.file.definition.IGameFileDefinition;
import net.highwayfrogs.editor.gui.ImageResource;
import net.highwayfrogs.editor.utils.data.reader.DataReader;
import net.highwayfrogs.editor.utils.data.writer.DataWriter;

/**
 * Represents a dummy file.
 * Created by Kneesnap on 8/12/2024.
 */
public class FroggerBeyondDummyFile extends FroggerBeyondFile {
    public FroggerBeyondDummyFile(IGameFileDefinition fileDefinition) {
        super(fileDefinition);
    }

    @Override
    public void load(DataReader reader) {
        reader.skipBytes(getRawData().length);
    }

    @Override
    public void save(DataWriter writer) {
        writer.writeBytes(getRawData());
    }

    @Override
    public Image getCollectionViewIcon() {
        return ImageResource.QUESTION_MARK_16.getFxImage();
    }
}