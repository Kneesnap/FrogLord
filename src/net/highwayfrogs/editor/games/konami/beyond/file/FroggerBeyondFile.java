package net.highwayfrogs.editor.games.konami.beyond.file;

import javafx.scene.image.Image;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.konami.beyond.FroggerBeyondInstance;
import net.highwayfrogs.editor.games.shared.basic.file.BasicGameFile;
import net.highwayfrogs.editor.games.shared.basic.file.definition.IGameFileDefinition;

/**
 * Represents a game file in Frogger Beyond.
 * Created by Kneesnap on 8/12/2024.
 */
public abstract class FroggerBeyondFile extends BasicGameFile<FroggerBeyondInstance> {
    public FroggerBeyondFile(IGameFileDefinition fileDefinition) {
        super(fileDefinition);
    }

    @Override
    public abstract void load(DataReader reader);

    @Override
    public abstract void save(DataWriter writer);

    @Override
    public abstract Image getCollectionViewIcon();
}