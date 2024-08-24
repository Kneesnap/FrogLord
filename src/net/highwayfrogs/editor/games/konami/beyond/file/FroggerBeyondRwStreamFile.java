package net.highwayfrogs.editor.games.konami.beyond.file;

import javafx.scene.image.Image;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.generic.GameInstance;
import net.highwayfrogs.editor.games.renderware.RwStreamChunkTypeRegistry;
import net.highwayfrogs.editor.games.renderware.RwStreamFile;
import net.highwayfrogs.editor.games.renderware.ui.RenderWareStreamEditorUIController;
import net.highwayfrogs.editor.games.shared.basic.file.definition.IGameFileDefinition;
import net.highwayfrogs.editor.gui.GameUIController;
import net.highwayfrogs.editor.gui.components.PropertyListViewerComponent.PropertyList;

import java.io.File;
import java.util.HashMap;

/**
 * Represents a Frogger Beyond RenderWare stream file.
 * TODO: If the next bytes are not a valid section, there's a handful of integers, then it will eventually reach another section.
 *  -> It seems Frogger Beyond IDs are 0x026AA6XX.
 *   -> 0x026AA718 -> CStage::LoadStage() -> CStageWorld::LoadDataFromFile()
 * Created by Kneesnap on 8/12/2024.
 */
public class FroggerBeyondRwStreamFile extends FroggerBeyondFile {
    private final RwStreamFile rwStreamFile;

    public FroggerBeyondRwStreamFile(IGameFileDefinition fileDefinition) {
        super(fileDefinition);
        this.rwStreamFile = new FroggerBeyondSpecialRwStreamFile(getGameInstance(), getGameInstance().getRwStreamChunkTypeRegistry(), fileDefinition.getFullFilePath());
    }

    @Override
    public void load(DataReader reader) {
        this.rwStreamFile.load(reader);
    }

    @Override
    public void save(DataWriter writer) {
        this.rwStreamFile.save(writer);
    }

    @Override
    public Image getCollectionViewIcon() {
        return this.rwStreamFile.getBestChunkIcon();
    }

    @Override
    public PropertyList addToPropertyList(PropertyList propertyList) {
        propertyList = super.addToPropertyList(propertyList);
        propertyList.add("Chunks", this.rwStreamFile.getChunks().size());

        for (int i = 0; i < this.rwStreamFile.getChunks().size(); i++)
            propertyList.add("Chunk " + i, this.rwStreamFile.getChunks().get(i).getChunkDescriptor());

        return propertyList;
    }

    @Override
    public GameUIController<?> makeEditorUI() {
        return RenderWareStreamEditorUIController.loadController(getGameInstance(), this.rwStreamFile);
    }

    @Override
    public void export(File exportFolder) {
        File imagesExportDir = new File(exportFolder, "Images [" + getFileDefinition().getFile().getParentFile().getName() + "_" + getDisplayName() + "]");
        this.rwStreamFile.exportTextures(imagesExportDir, new HashMap<>());
    }

    public static class FroggerBeyondSpecialRwStreamFile extends RwStreamFile { // TODO: Implement this better later.
        public FroggerBeyondSpecialRwStreamFile(GameInstance instance, RwStreamChunkTypeRegistry chunkTypeRegistry, String fullFilePath) {
            super(instance, chunkTypeRegistry, fullFilePath);
        }

        @Override
        public void load(DataReader reader) {
            getChunks().clear();
            while (reader.hasMore()) {
                if (RwStreamFile.isRwStreamHeader(reader)) {
                    getChunks().add(getChunkTypeRegistry().readChunk(reader, this));
                } else {
                    reader.skipInt(); // TODO: What is this?
                }
            }
        }
    }
}