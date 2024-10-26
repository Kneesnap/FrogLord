package net.highwayfrogs.editor.games.konami.greatquest.loading;

import net.highwayfrogs.editor.games.konami.greatquest.chunks.GreatQuestChunkTextureReference;
import net.highwayfrogs.editor.games.konami.greatquest.chunks.kcCResource;
import net.highwayfrogs.editor.games.konami.greatquest.file.GreatQuestArchiveFile;
import net.highwayfrogs.editor.games.konami.greatquest.file.GreatQuestAssetBinFile;
import net.highwayfrogs.editor.games.konami.greatquest.file.GreatQuestChunkedFile;
import net.highwayfrogs.editor.games.konami.greatquest.file.GreatQuestImageFile;
import net.highwayfrogs.editor.games.konami.greatquest.model.kcMaterial;
import net.highwayfrogs.editor.utils.FileUtils;
import net.highwayfrogs.editor.utils.StringUtils;

import java.util.*;
import java.util.Map.Entry;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 * Manages resolving of material textures during the file load process.
 * Created by Kneesnap on 7/11/2023.
 */
public class kcMaterialLoadContext {
    private final GreatQuestAssetBinFile mainArchive;
    private final Map<GreatQuestChunkedFile, Map<String, GreatQuestImageFile>> chunkedFileImageCache = new HashMap<>();
    private final Map<String, List<GreatQuestImageFile>> globalCachedImages = new HashMap<>();
    private final Map<kcMaterial, GreatQuestArchiveFile> allMaterials = new HashMap<>();
    private final Map<kcMaterial, GreatQuestArchiveFile> multipleMatchMaterials = new HashMap<>();

    public kcMaterialLoadContext(GreatQuestAssetBinFile binFile) {
        this.mainArchive = binFile;
    }

    /**
     * Gets the logger
     */
    public Logger getLogger() {
        return this.mainArchive.getLogger();
    }

    /**
     * Apply texture file names.
     * @param fullPath  The full file path to apply texture names from.
     * @param materials The materials to apply texture file names from.
     */
    public void applyLevelTextureFileNames(GreatQuestArchiveFile sourceFile, String fullPath, List<kcMaterial> materials) {
        if (fullPath == null || fullPath.isEmpty() || materials == null || materials.isEmpty())
            return;

        // The path provided could be a path to a model, or a chunked level file.
        // If it's a path to a chunked level file, it will be in the \GameData\ folder, but the textures are going to be in \GameSource\ instead.
        // So, we're going to switch directories.
        if (fullPath.toLowerCase().startsWith("\\gamedata\\"))
            fullPath = "\\GameSource\\" + fullPath.substring(10);

        // Because this is a level, the texture may be either be in the shared level textures or in a model specific for the folder.
        // For example, '\GameSource\Level13Catacombs\Props\cofflid2\COFFLID2.VTX' uses a texture 'wood1.img'.
        // This texture is at '\GameSource\Level13Catacombs\Level\wood1.img', so we need to modify the path if we see it.
        String[] splitPath = fullPath.split(Pattern.quote("\\"));
        for (int i = 0; i < splitPath.length; i++) {
            String name = splitPath[i];

            if ("Props".equalsIgnoreCase(name) && splitPath.length == i + 3) {
                String levelPath = String.join("\\", Arrays.copyOfRange(splitPath, 0, i)) + "\\Level\\";
                this.applyTextureFileNames(sourceFile, levelPath, materials);
            }
        }

        // Attempt to apply the texture to the folder we're in.
        // We want to do this last since if it's found, it's correct.
        this.applyTextureFileNames(sourceFile, fullPath, materials);
    }

    /**
     * Apply texture file names.
     * @param fullPath  The full file path to apply texture names from.
     * @param materials The materials to apply texture file names from.
     */
    public void applyTextureFileNames(GreatQuestArchiveFile sourceFile, String fullPath, List<kcMaterial> materials) {
        if (fullPath == null || fullPath.isEmpty())
            return;

        for (kcMaterial material : materials) {
            if (material.getTextureFileName() == null)
                continue;

            String textureFileName = FileUtils.stripExtension(material.getTextureFileName());
            if (textureFileName.isEmpty())
                continue;

            // Search for texture local to model folder.
            int lastDirectorySeparator = fullPath.lastIndexOf('\\');
            if (lastDirectorySeparator == -1)
                continue;

            String texturePath = fullPath.substring(0, lastDirectorySeparator + 1)
                    + textureFileName + ".img";

            GreatQuestArchiveFile file = this.mainArchive.applyFileName(texturePath, false);

            if (file instanceof GreatQuestImageFile) {
                material.setTexture((GreatQuestImageFile) file);
                this.allMaterials.put(material, sourceFile);
                this.multipleMatchMaterials.remove(material); // We prefer the texture referenced in the same chunked file, so if that exists we don't consider there to be multiple files which could apply.
            }
        }
    }

    /**
     * Resolve textures for all provided materials from the chunked file.
     * The file which the materials are considered to have come from is the chunked file.
     * @param chunkedFile The file to search for images to resolve from.
     * @param materials   The materials which need textures resolved.
     */
    public void resolveMaterialTexturesInChunk(GreatQuestChunkedFile chunkedFile, List<kcMaterial> materials) {
        resolveMaterialTexturesInChunk(chunkedFile, chunkedFile, materials);
    }

    /**
     * Resolve textures for all provided materials from the chunked file.
     * @param chunkedFile The file to search for images to resolve from.
     * @param sourceFile  the file which the material is defined in.
     * @param materials   The materials which need textures resolved.
     */
    public void resolveMaterialTexturesInChunk(GreatQuestChunkedFile chunkedFile, GreatQuestArchiveFile sourceFile, List<kcMaterial> materials) {
        if (materials == null || materials.isEmpty())
            return;

        for (kcMaterial material : materials) {
            if (material.getTexture() != null)
                continue;

            GreatQuestImageFile image = findLocalImageFile(chunkedFile, material.getTextureFileName());
            this.allMaterials.put(material, sourceFile);
            if (image != null) {
                material.setTexture(image);
                this.multipleMatchMaterials.remove(material); // We prefer the texture referenced in the same chunked file, so if that exists we don't consider there to be multiple files which could apply.
            }
        }
    }

    /**
     * Resolve textures for all provided materials by searching all images.
     * Any materials which already have a texture resolved are skipped.
     * @param sourceFile The file the materials came from.
     * @param materials  The materials which need texture resolution.
     */
    public void resolveMaterialTexturesGlobally(GreatQuestArchiveFile sourceFile, List<kcMaterial> materials) {
        if (materials == null || materials.isEmpty())
            return;

        if (this.globalCachedImages.isEmpty()) {
            for (GreatQuestArchiveFile file : this.mainArchive.getFiles()) {
                if (file instanceof GreatQuestImageFile && file.getFileName() != null) {
                    String fileName = FileUtils.stripExtension(file.getFileName());
                    List<GreatQuestImageFile> images = this.globalCachedImages.computeIfAbsent(fileName, key -> new ArrayList<>());
                    images.add((GreatQuestImageFile) file);
                }
            }

            // Sort the cache.
            this.globalCachedImages.values().forEach(list -> list.sort(Comparator.comparingInt(GreatQuestArchiveFile::getArchiveIndex)));
        }

        for (kcMaterial material : materials) {
            this.allMaterials.put(material, sourceFile);
            if (material.getTexture() != null)
                continue; // Already has a resolved texture reference.

            List<GreatQuestImageFile> images = this.globalCachedImages.get(FileUtils.stripExtension(material.getTextureFileName()));
            if (images == null || images.isEmpty())
                continue;

            material.setTexture(images.get(0));
            if (images.size() > 1)
                this.multipleMatchMaterials.put(material, sourceFile);
        }
    }

    /**
     * Runs final code execution on complete.
     */
    public void onComplete() {
        // Find materials which didn't resolve.
        for (Entry<kcMaterial, GreatQuestArchiveFile> entry : this.allMaterials.entrySet()) {
            kcMaterial material = entry.getKey();
            if (!material.hasTexture())
                continue; // The material isn't supposed to have a texture.

            // A few models such as '\GameSource\Level05MushroomValley\Props\Fosfshs2\Fosfshs2.vtx' have weird materials.
            // The materials have the flag indicating a texture is included, but no name is actually there.
            // I believe they just might be bad exports from their 3D modelling tools.
            // It's probably unnecessary to warn about this.
            if (StringUtils.isNullOrEmpty(material.getTextureFileName()) && StringUtils.isNullOrEmpty(material.getMaterialName()))
                continue;

            if (material.getTexture() != null)
                continue; // A material has been set.

            getLogger().warning("No image file was identified for file '" + material.getTextureFileName() + "' from the material named '" + material.getMaterialName() + "' in " + entry.getValue().getDebugName() + ".");
        }

        // Find materials which had multiple possibilities.
        for (Entry<kcMaterial, GreatQuestArchiveFile> entry : this.multipleMatchMaterials.entrySet()) {
            kcMaterial material = entry.getKey();
            GreatQuestArchiveFile file = entry.getValue();
            List<GreatQuestImageFile> foundImages = this.globalCachedImages.get(FileUtils.stripExtension(material.getTextureFileName()));
            int foundImageCount = foundImages != null ? foundImages.size() : 0;

            // Attempt to search for images.
            if (file.hasFilePath() && !material.getTextureFileName().isEmpty()) {
                int lastDirectorySeparator = file.getFilePath().lastIndexOf('\\');
                if (lastDirectorySeparator != -1) {
                    String texturePath = file.getFilePath().substring(0, lastDirectorySeparator + 1)
                            + FileUtils.stripExtension(material.getTextureFileName()) + ".img";

                    GreatQuestArchiveFile targetImageFile = this.mainArchive.applyFileName(texturePath, false);
                    if (targetImageFile != null) {
                        if (!(targetImageFile instanceof GreatQuestImageFile))
                            throw new RuntimeException("We found a file for material ref '" + texturePath + "', but it wasn't an image file! It was a(n) " + targetImageFile.getClass().getSimpleName() + ".");

                        GreatQuestImageFile imageFile = (GreatQuestImageFile) targetImageFile;
                        material.setTexture(imageFile);
                        continue; // Skip.
                    }
                }
            }

            // Print output.
            getLogger().warning(foundImageCount + " image file(s) were identified for file '" + material.getTextureFileName() + "' from the material named '" + material.getMaterialName() + "' in " + file.getDebugName() + (foundImageCount > 0 ? ":" : "."));
            if (foundImages != null && foundImageCount > 0)
                for (GreatQuestImageFile foundImage : foundImages)
                    getLogger().warning(" - " + foundImage.getDebugName());
        }
    }

    /**
     * Find a texture local to the given chunked file.
     * @param chunkedFile   The chunked file which the image is searched from.
     * @param imageFileName The texture file name.
     * @return localImageFile or null.
     */
    private GreatQuestImageFile findLocalImageFile(GreatQuestChunkedFile chunkedFile, String imageFileName) {
        String strippedTextureFileName = FileUtils.stripExtension(imageFileName);

        // Setup image cache.
        Map<String, GreatQuestImageFile> cachedImages = this.chunkedFileImageCache.get(chunkedFile);
        if (cachedImages == null) {
            this.chunkedFileImageCache.put(chunkedFile, cachedImages = new HashMap<>());
            for (kcCResource resource : chunkedFile.getChunks()) {
                if (!(resource instanceof GreatQuestChunkTextureReference))
                    continue;

                GreatQuestChunkTextureReference texRef = (GreatQuestChunkTextureReference) resource;
                GreatQuestImageFile referencedImage = texRef.getReferencedImage();
                if (referencedImage != null)
                    cachedImages.put(strippedTextureFileName, referencedImage);
            }
        }

        // Get data from cache.
        return cachedImages.get(imageFileName);
    }
}