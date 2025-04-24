package net.highwayfrogs.editor.games.renderware.game.file;

import javafx.scene.image.Image;
import net.highwayfrogs.editor.utils.data.reader.DataReader;
import net.highwayfrogs.editor.utils.data.writer.DataWriter;
import net.highwayfrogs.editor.games.renderware.game.RwGenericGameInstance;
import net.highwayfrogs.editor.games.shared.basic.file.BasicGameFile;
import net.highwayfrogs.editor.games.shared.basic.file.definition.IGameFileDefinition;

/**
 * Represents a game file in a generic RenderWare game.
 * Created by Kneesnap on 8/18/2024.
 */
public abstract class RwGenericFile extends BasicGameFile<RwGenericGameInstance> {
    public RwGenericFile(IGameFileDefinition fileDefinition) {
        super(fileDefinition);
    }

    @Override
    public abstract void load(DataReader reader);

    @Override
    public abstract void save(DataWriter writer);

    @Override
    public abstract Image getCollectionViewIcon();
}