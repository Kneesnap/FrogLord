package net.highwayfrogs.editor.games.konami.greatquest.model;

import javafx.scene.image.Image;
import lombok.Getter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.games.konami.greatquest.GreatQuestInstance;
import net.highwayfrogs.editor.games.konami.greatquest.IFileExport;
import net.highwayfrogs.editor.games.konami.greatquest.file.GreatQuestArchiveFile;
import net.highwayfrogs.editor.games.konami.greatquest.loading.kcLoadContext;
import net.highwayfrogs.editor.games.konami.greatquest.ui.mesh.model.GreatQuestModelInfoController;
import net.highwayfrogs.editor.games.konami.greatquest.ui.mesh.model.GreatQuestModelMesh;
import net.highwayfrogs.editor.games.konami.greatquest.ui.mesh.model.GreatQuestModelViewController;
import net.highwayfrogs.editor.gui.GameUIController;
import net.highwayfrogs.editor.gui.ImageResource;
import net.highwayfrogs.editor.gui.components.PropertyListViewerComponent.IPropertyListCreator;
import net.highwayfrogs.editor.gui.components.PropertyListViewerComponent.PropertyList;
import net.highwayfrogs.editor.gui.editor.MeshViewController;
import net.highwayfrogs.editor.utils.data.reader.DataReader;
import net.highwayfrogs.editor.utils.data.writer.DataWriter;

import java.io.File;
import java.io.IOException;

/**
 * Represents a file containing a kcModel.
 * Created by Kneesnap on 6/28/2023.
 */
@Getter
public class kcModelWrapper extends GreatQuestArchiveFile implements IFileExport, IPropertyListCreator {
    private final kcModel model;

    public static final String SIGNATURE_STR = "6YTV";

    public kcModelWrapper(GreatQuestInstance instance) {
        this(instance, new kcModel(instance));
    }

    public kcModelWrapper(GreatQuestInstance instance, kcModel model) {
        super(instance);
        this.model = model;
    }

    @Override
    public void load(DataReader reader) {
        reader.verifyString(SIGNATURE_STR);
        int size = reader.readInt();

        if (size != reader.getRemaining()) // TODO: This being 4 too low indicates it's using the old (broken) format.
            getLogger().warning("The model '%s' was supposed to have %d bytes, but actually has %d byte(s).", getDebugName(), size, reader.getRemaining());

        this.model.load(reader);
        if (reader.hasMore())
            getLogger().warning("The model '%s' has %d unread byte(s).", getDebugName(), reader.getRemaining());
    }

    @Override
    public void save(DataWriter writer) {
        writer.writeStringBytes(SIGNATURE_STR);
        int sizePos = writer.writeNullPointer();
        this.model.save(writer);
        writer.writeIntAtPos(sizePos, writer.getIndex() - sizePos - Constants.INTEGER_SIZE);
    }

    @Override
    public Image getCollectionViewIcon() {
        return ImageResource.GEOMETRIC_SHAPES_16.getFxImage();
    }

    @Override
    public GameUIController<?> makeEditorUI() {
        return loadEditor(getGameInstance(), new GreatQuestModelInfoController(getGameInstance()), this);
    }

    @Override
    public void handleDoubleClick() {
        openMeshViewer();
    }

    @Override
    public String getExtension() {
        return "vtx";
    }

    @Override
    public void afterLoad2(kcLoadContext context) {
        super.afterLoad2(context);

        // Generate collision mesh names. This is done in afterLoad2() to wait for our own file path to be set.
        context.getMaterialLoadContext().applyFileNameAsCollisionMesh(getFileName());

        // Generate texture file names. This is done in afterLoad2() to wait for our own file path to be set.
        context.getMaterialLoadContext().applyLevelTextureFileNames(this, getFilePath(), this.model.getMaterials());

        // Apply file names to all materials.
        // We need to do this both when a texture reference loads and when the model loads, so regardless of if this particular model loads before or after the texture it will still get the texture names.
        context.getMaterialLoadContext().resolveMaterialTexturesGlobally(this, this.model.getMaterials());
    }

    @Override
    public String getDefaultFolderName() {
        return "Models";
    }

    @Override
    public void exportToFolder(File folder) throws IOException {
        if (this.model == null)
            return;

        this.model.saveToFile(folder, getExportName());
    }

    @Override
    public PropertyList addToPropertyList(PropertyList propertyList) {
        propertyList = super.addToPropertyList(propertyList);
        if (this.model != null)
            this.model.addToPropertyList(propertyList);

        return propertyList;
    }

    /**
     * Opens the mesh viewer for the wrapped model.
     */
    public void openMeshViewer() {
        MeshViewController.setupMeshViewer(getGameInstance(), new GreatQuestModelViewController(), new GreatQuestModelMesh(this));
    }
}