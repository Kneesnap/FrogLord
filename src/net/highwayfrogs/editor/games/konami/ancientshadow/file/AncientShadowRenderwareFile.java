package net.highwayfrogs.editor.games.konami.ancientshadow.file;

import javafx.scene.image.Image;
import lombok.Getter;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.konami.ancientshadow.AncientShadowGameFile;
import net.highwayfrogs.editor.games.konami.ancientshadow.AncientShadowInstance;
import net.highwayfrogs.editor.games.konami.hudson.IHudsonFileDefinition;
import net.highwayfrogs.editor.games.renderware.RwStreamChunk;
import net.highwayfrogs.editor.games.renderware.RwStreamFile;
import net.highwayfrogs.editor.games.renderware.chunks.RwPlatformIndependentTextureDictionaryChunk;
import net.highwayfrogs.editor.games.renderware.chunks.RwPlatformIndependentTextureDictionaryChunk.RWPlatformIndependentTextureEntry;
import net.highwayfrogs.editor.gui.ImageResource;
import net.highwayfrogs.editor.gui.components.PropertyListViewerComponent.PropertyList;
import net.highwayfrogs.editor.utils.Utils;

import javax.imageio.ImageIO;
import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Represents a Renderware file.
 * Created by Kneesnap on 8/4/2024.
 */
@Getter
public class AncientShadowRenderwareFile extends AncientShadowGameFile {
    private final RwStreamFile rwStreamFile;

    public AncientShadowRenderwareFile(IHudsonFileDefinition fileDefinition) {
        super(fileDefinition);
        this.rwStreamFile = new RwStreamFile(getGameInstance(), AncientShadowInstance.getRwStreamChunkTypeRegistry(), fileDefinition.getFileName());
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
        return ImageResource.GHIDRA_ICON_MULTIMEDIA_16.getFxImage();
    }

    @Override
    public PropertyList addToPropertyList(PropertyList propertyList) {
        propertyList = super.addToPropertyList(propertyList);
        propertyList.add("Chunks", this.rwStreamFile.getChunks().size());

        for (int i = 0; i < this.rwStreamFile.getChunks().size(); i++)
            propertyList.add("Chunk " + i, this.rwStreamFile.getChunks().get(i));

        return propertyList;
    }

    /**
     * Export all textures in the file.
     * @param outputFolder the file to export textures to
     * @param fileNameCountMap the file-name count map to use.
     */
    public void exportTextures(File outputFolder, Map<String, AtomicInteger> fileNameCountMap) {
        for (RwStreamChunk chunk : rwStreamFile.getChunks()) {
            if (!(chunk instanceof RwPlatformIndependentTextureDictionaryChunk))
                continue;

            RwPlatformIndependentTextureDictionaryChunk textureDictionaryChunk = (RwPlatformIndependentTextureDictionaryChunk) chunk;
            for (RWPlatformIndependentTextureEntry entry : textureDictionaryChunk.getEntries()) {
                for (int i = 0; i < entry.getMipLevelImages().size(); i++) {
                    String baseName = entry.makeFileName(i);
                    int num = fileNameCountMap.computeIfAbsent(baseName, key -> new AtomicInteger()).getAndIncrement();

                    Utils.makeDirectory(outputFolder);
                    File imageOutputFile = new File(outputFolder, String.format("%s_%02d.png", baseName, num));

                    try {
                        ImageIO.write(entry.getMipLevelImages().get(i).getImage(), "png", imageOutputFile);
                    } catch (IOException ex) {
                        Utils.handleError(getLogger(), ex, false, "Failed to save '%s'.", imageOutputFile.getName());
                    }
                }
            }
        }
    }
}