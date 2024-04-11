package net.highwayfrogs.editor.games.greatquest.loading;

import net.highwayfrogs.editor.games.greatquest.TGQBinFile;
import net.highwayfrogs.editor.games.greatquest.TGQChunkedFile;
import net.highwayfrogs.editor.games.greatquest.TGQFile;
import net.highwayfrogs.editor.games.greatquest.TGQImageFile;
import net.highwayfrogs.editor.games.greatquest.model.kcMaterial;
import net.highwayfrogs.editor.games.greatquest.toc.TGQChunkTextureReference;
import net.highwayfrogs.editor.games.greatquest.toc.kcCResource;
import net.highwayfrogs.editor.utils.Utils;

import java.util.*;
import java.util.Map.Entry;
import java.util.regex.Pattern;

/**
 * Manages resolving of material textures during the file load process.
 * Created by Kneesnap on 7/11/2023.
 */
public class kcMaterialLoadContext {
    private final TGQBinFile mainArchive;
    private final Map<TGQChunkedFile, Map<String, TGQImageFile>> chunkedFileImageCache = new HashMap<>();
    private final Map<String, List<TGQImageFile>> globalCachedImages = new HashMap<>();
    private final Map<kcMaterial, TGQFile> allMaterials = new HashMap<>();
    private final Map<kcMaterial, TGQFile> multipleMatchMaterials = new HashMap<>();

    public kcMaterialLoadContext(TGQBinFile binFile) {
        this.mainArchive = binFile;
    }

    /**
     * Apply texture file names.
     * @param fullPath  The full file path to apply texture names from.
     * @param materials The materials to apply texture file names from.
     */
    public void applyLevelTextureFileNames(TGQFile sourceFile, String fullPath, List<kcMaterial> materials) {
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
    public void applyTextureFileNames(TGQFile sourceFile, String fullPath, List<kcMaterial> materials) {
        if (fullPath == null || fullPath.isEmpty())
            return;

        for (kcMaterial material : materials) {
            if (material.getTextureFileName() == null)
                continue;

            String textureFileName = Utils.stripExtension(material.getTextureFileName());
            if (textureFileName.isEmpty())
                continue;

            // Search for texture local to model folder.
            int lastDirectorySeparator = fullPath.lastIndexOf('\\');
            if (lastDirectorySeparator == -1)
                continue;

            String texturePath = fullPath.substring(0, lastDirectorySeparator + 1)
                    + textureFileName + ".img";

            TGQFile file = this.mainArchive.applyFileName(texturePath, false);

            if (file instanceof TGQImageFile) {
                material.setTexture((TGQImageFile) file);
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
    public void resolveMaterialTexturesInChunk(TGQChunkedFile chunkedFile, List<kcMaterial> materials) {
        resolveMaterialTexturesInChunk(chunkedFile, chunkedFile, materials);
    }

    /**
     * Resolve textures for all provided materials from the chunked file.
     * @param chunkedFile The file to search for images to resolve from.
     * @param sourceFile  the file which the material is defined in.
     * @param materials   The materials which need textures resolved.
     */
    public void resolveMaterialTexturesInChunk(TGQChunkedFile chunkedFile, TGQFile sourceFile, List<kcMaterial> materials) {
        if (materials == null || materials.isEmpty())
            return;

        for (kcMaterial material : materials) {
            if (material.getTexture() != null)
                continue;

            TGQImageFile image = findLocalImageFile(chunkedFile, material.getTextureFileName());
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
    public void resolveMaterialTexturesGlobally(TGQFile sourceFile, List<kcMaterial> materials) {
        if (materials == null || materials.isEmpty())
            return;

        if (this.globalCachedImages.isEmpty()) {
            for (TGQFile file : this.mainArchive.getFiles()) {
                if (file instanceof TGQImageFile && file.getFileName() != null) {
                    String fileName = Utils.stripExtension(file.getFileName());
                    List<TGQImageFile> images = this.globalCachedImages.computeIfAbsent(fileName, key -> new ArrayList<>());
                    images.add((TGQImageFile) file);
                }
            }

            // Sort the cache.
            this.globalCachedImages.values().forEach(list -> list.sort(Comparator.comparingInt(TGQFile::getArchiveIndex)));
        }

        for (kcMaterial material : materials) {
            this.allMaterials.put(material, sourceFile);
            if (material.getTexture() != null)
                continue; // Already has a resolved texture reference.

            List<TGQImageFile> images = this.globalCachedImages.get(Utils.stripExtension(material.getTextureFileName()));
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
        for (Entry<kcMaterial, TGQFile> entry : this.allMaterials.entrySet()) {
            kcMaterial material = entry.getKey();
            if (!material.hasTexture())
                continue; // The material isn't supposed to have a texture.

            // A few models such as '\GameSource\Level05MushroomValley\Props\Fosfshs2\Fosfshs2.vtx' have weird materials.
            // The materials have the flag indicating a texture is included, but no name is actually there.
            // I believe they just might be bad exports from their 3D modelling tools.
            // It's probably unnecessary to warn about this.
            if (Utils.isNullOrEmpty(material.getTextureFileName()) && Utils.isNullOrEmpty(material.getMaterialName()))
                continue;

            if (material.getTexture() != null)
                continue; // A material has been set.

            System.out.println("No image file was identified for file '" + material.getTextureFileName() + "' from the material named '" + material.getMaterialName() + "' in " + entry.getValue().getDebugName() + ".");
        }

        // Find materials which had multiple possibilities.
        for (Entry<kcMaterial, TGQFile> entry : this.multipleMatchMaterials.entrySet()) {
            kcMaterial material = entry.getKey();
            TGQFile file = entry.getValue();
            List<TGQImageFile> foundImages = this.globalCachedImages.get(Utils.stripExtension(material.getTextureFileName()));
            int foundImageCount = foundImages != null ? foundImages.size() : 0;

            // Attempt to search for images.
            if (file.hasFilePath() && !material.getTextureFileName().isEmpty()) {
                int lastDirectorySeparator = file.getFilePath().lastIndexOf('\\');
                if (lastDirectorySeparator != -1) {
                    String texturePath = file.getFilePath().substring(0, lastDirectorySeparator + 1)
                            + Utils.stripExtension(material.getTextureFileName()) + ".img";

                    TGQFile targetImageFile = this.mainArchive.applyFileName(texturePath, false);
                    if (targetImageFile != null) {
                        if (!(targetImageFile instanceof TGQImageFile))
                            throw new RuntimeException("We found a file for material ref '" + texturePath + "', but it wasn't an image file! It was a(n) " + targetImageFile.getClass().getSimpleName() + ".");

                        TGQImageFile imageFile = (TGQImageFile) targetImageFile;
                        material.setTexture(imageFile);
                        continue; // Skip.
                    }
                }
            }

            // Print output.
            System.out.println(foundImageCount + " image file(s) were identified for file '" + material.getTextureFileName() + "' from the material named '" + material.getMaterialName() + "' in " + file.getDebugName() + (foundImageCount > 0 ? ":" : "."));
            if (foundImages != null && foundImageCount > 0)
                for (TGQImageFile foundImage : foundImages)
                    System.out.println(" - " + foundImage.getDebugName());
        }
    }

    /**
     * Find a texture local to the given chunked file.
     * @param chunkedFile   The chunked file which the image is searched from.
     * @param imageFileName The texture file name.
     * @return localImageFile or null.
     */
    private TGQImageFile findLocalImageFile(TGQChunkedFile chunkedFile, String imageFileName) {
        String strippedTextureFileName = Utils.stripExtension(imageFileName);

        // Setup image cache.
        Map<String, TGQImageFile> cachedImages = this.chunkedFileImageCache.get(chunkedFile);
        if (cachedImages == null) {
            this.chunkedFileImageCache.put(chunkedFile, cachedImages = new HashMap<>());
            for (kcCResource resource : chunkedFile.getChunks()) {
                if (!(resource instanceof TGQChunkTextureReference))
                    continue;

                TGQChunkTextureReference texRef = (TGQChunkTextureReference) resource;
                TGQFile texRefFile = chunkedFile.getMainArchive().getFileByName(chunkedFile, texRef.getPath());
                if (texRefFile instanceof TGQImageFile)
                    cachedImages.put(strippedTextureFileName, (TGQImageFile) texRefFile);
            }
        }

        // Get data from cache.
        return cachedImages.get(imageFileName);
    }
}