package net.highwayfrogs.editor.games.renderware.game.file;

import javafx.scene.image.Image;
import net.highwayfrogs.editor.utils.data.reader.DataReader;
import net.highwayfrogs.editor.utils.data.writer.DataWriter;
import net.highwayfrogs.editor.games.shared.basic.file.definition.IGameFileDefinition;
import net.highwayfrogs.editor.gui.ImageResource;

/**
 * Represents a dummy RenderWare file for a generic RenderWare game.
 * Created by Kneesnap on 8/18/2024.
 */
public class RwGenericDummyFile extends RwGenericFile {
    public RwGenericDummyFile(IGameFileDefinition fileDefinition) {
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