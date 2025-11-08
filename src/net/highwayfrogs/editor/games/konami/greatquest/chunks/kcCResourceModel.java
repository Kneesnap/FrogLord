package net.highwayfrogs.editor.games.konami.greatquest.chunks;

import lombok.Getter;
import net.highwayfrogs.editor.games.konami.greatquest.GreatQuestInstance;
import net.highwayfrogs.editor.games.konami.greatquest.GreatQuestUtils;
import net.highwayfrogs.editor.games.konami.greatquest.file.GreatQuestArchiveFile;
import net.highwayfrogs.editor.games.konami.greatquest.file.GreatQuestAssetBinFile;
import net.highwayfrogs.editor.games.konami.greatquest.loading.kcLoadContext;
import net.highwayfrogs.editor.games.konami.greatquest.model.kcModel;
import net.highwayfrogs.editor.games.konami.greatquest.model.kcModelWrapper;
import net.highwayfrogs.editor.gui.GameUIController;
import net.highwayfrogs.editor.gui.InputMenu;
import net.highwayfrogs.editor.gui.components.PropertyListViewerComponent.PropertyList;
import net.highwayfrogs.editor.utils.FileUtils;
import net.highwayfrogs.editor.utils.StringUtils;
import net.highwayfrogs.editor.utils.data.reader.DataReader;
import net.highwayfrogs.editor.utils.data.writer.DataWriter;

/**
 * A reference to a 3D model.
 * Created by Kneesnap on 6/28/2023.
 */
@Getter
public class kcCResourceModel extends kcCResource {
    private String fullPath = ""; // Full file path to the real model file.

    public static final int FULL_PATH_SIZE = 260;

    public kcCResourceModel(GreatQuestChunkedFile parentFile) {
        super(parentFile, KCResourceID.MODEL);
    }

    @Override
    public void load(DataReader reader) {
        super.load(reader);
        this.fullPath = reader.readNullTerminatedFixedSizeString(FULL_PATH_SIZE); // Don't read with the padding byte, as the padding bytes are only valid when the buffer is initially created, if the is shrunk (Such as GOOBER.VTX in 00.dat), after the null byte, the old bytes will still be there.

        // Validate file name.
        String fileName = GreatQuestUtils.getFileNameFromPath(this.fullPath);
        if (!fileName.equalsIgnoreCase(getName()) && !kcCResource.DEFAULT_RESOURCE_NAME.equalsIgnoreCase(getName()))
            getLogger().warning("The file name was expected to match the resource name ('%s'), but it determined to actually be '%s' instead.", getName(), fileName);

        // Apply original string just in-case any of these are 'unnamed'. (Happens occasionally)
        int testHash = GreatQuestUtils.hash(fileName);
        if (testHash == getHash())
            getSelfHash().setOriginalString(fileName);

        // Apply the file name found here to the corresponding global file.
        GreatQuestAssetBinFile mainArchive = getMainArchive();
        if (mainArchive != null)
            mainArchive.applyFileName(this.fullPath, false);

        applyCollisionMeshName();
    }

    @Override
    public void save(DataWriter writer) {
        super.save(writer);
        writer.writeNullTerminatedFixedSizeString(this.fullPath, FULL_PATH_SIZE, GreatQuestInstance.PADDING_BYTE_CD);
    }

    @Override
    public void afterLoad1(kcLoadContext context) {
        super.afterLoad1(context);
        // We must wait until afterLoad1() because the file object won't exist for files found later in the file if we don't.
        // But, this must run before afterLoad2() because that's when we start doing lookups based on file paths.
        kcModelWrapper wrapper = getModelWrapper();
        if (wrapper != null)
            context.getMaterialLoadContext().applyLevelTextureFileNames(getParentFile(), this.fullPath, wrapper.getModel().getMaterials());
    }

    @Override
    public void afterLoad2(kcLoadContext context) {
        super.afterLoad2(context);
        // Now we'll resolve the textures for this model using the textures found in the chunked file.
        // We don't need to print a warning if the model doesn't exist, because the applyFileName() call would have already caught it.
        kcModelWrapper wrapper = getModelWrapper();
        if (wrapper != null)
            context.getMaterialLoadContext().resolveMaterialTexturesInChunk(getParentFile(), wrapper, wrapper.getModel().getMaterials());
    }

    @Override
    public PropertyList addToPropertyList(PropertyList propertyList) {
        propertyList.add("IMPORTANT", "This is not an actual 3D model!\nInstead, this is the file path to a 3D model.");
        propertyList.add("", "");

        propertyList = super.addToPropertyList(propertyList);
        propertyList.add("File Path", this.fullPath,
                () -> InputMenu.promptInputBlocking(getGameInstance(), "Please enter the new path.", this.fullPath, newPath -> setFullPath(newPath, true)));
        return propertyList;
    }

    @Override
    public GameUIController<?> createExtraUIController() {
        kcModelWrapper modelWrapper = getModelWrapper();
        return modelWrapper != null ? modelWrapper.createEmbeddedModelViewer() : super.createExtraUIController();
    }

    /**
     * Gets the file name without the full path.
     */
    public String getFileName() {
        return GreatQuestUtils.getFileNameFromPath(this.fullPath);
    }

    /**
     * Gets the 3D this model stands in for.
     * @return model
     */
    public kcModel getModel() {
        GreatQuestArchiveFile file = getOptionalFileByName(this.fullPath);
        return (file instanceof kcModelWrapper) ? ((kcModelWrapper) file).getModel() : null;
    }

    /**
     * Gets the wrapper around the 3D model.
     * @return modelWrapper
     */
    public kcModelWrapper getModelWrapper() {
        GreatQuestArchiveFile file = getOptionalFileByName(this.fullPath);
        return (file instanceof kcModelWrapper) ? ((kcModelWrapper) file) : null;
    }

    /**
     * Sets the file path of the asset referenced here.
     * @param newPath the file path to apply
     * @param throwIfPathCannotBeResolved if the file path cannot be resolved to a valid asset and this is true, an exception will be thrown.
     */
    public void setFullPath(String newPath, boolean throwIfPathCannotBeResolved) {
        if (newPath == null)
            throw new NullPointerException("newPath");
        if (newPath.length() >= FULL_PATH_SIZE)
            throw new IllegalArgumentException("The provided path is too large! (Provided: " + newPath.length() + ", Maximum: " + (FULL_PATH_SIZE - 1) + ")");

        if (throwIfPathCannotBeResolved) {
            GreatQuestArchiveFile file = getOptionalFileByName(newPath);
            if (!(file instanceof kcModelWrapper))
                throw new IllegalArgumentException("The file path could not be resolved to a model! (" + newPath + ")");
        }

        setName(GreatQuestUtils.getFileNameFromPath(newPath));
        this.fullPath = newPath;
    }

    private void applyCollisionMeshName() {
        // If we resolve the model successfully, our goal is to generate the name of any corresponding collision mesh.
        String collisionMeshName = getAsCollisionTriMeshFileName(getFileName());
        kcCResourceTriMesh triMesh = getParentFile().getResourceByName(collisionMeshName, kcCResourceTriMesh.class);
        if (triMesh != null && StringUtils.isNullOrEmpty(triMesh.getSelfHash().getOriginalString()))
            triMesh.getSelfHash().setOriginalString(collisionMeshName);
    }

    /**
     * Takes the file name and gets it as a collision file name.
     * @param fileName the file name to generate a collision file name for
     * @return collisionFileName
     */
    public static String getAsCollisionTriMeshFileName(String fileName) {
        String baseName = FileUtils.stripExtension(fileName);
        String collisionMeshName = (baseName + kcCResourceTriMesh.EXTENSION_SUFFIX.toLowerCase());
        if (baseName.equals(baseName.toUpperCase()))
            collisionMeshName = collisionMeshName.toUpperCase();

        return collisionMeshName;
    }
}