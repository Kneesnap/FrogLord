package net.highwayfrogs.editor.games.tgq.loading;

import net.highwayfrogs.editor.games.tgq.TGQBinFile;
import net.highwayfrogs.editor.games.tgq.TGQChunkedFile;
import net.highwayfrogs.editor.games.tgq.TGQFile;
import net.highwayfrogs.editor.games.tgq.TGQImageFile;
import net.highwayfrogs.editor.games.tgq.model.kcMaterial;
import net.highwayfrogs.editor.games.tgq.toc.TGQChunkTextureReference;
import net.highwayfrogs.editor.games.tgq.toc.kcCResource;
import net.highwayfrogs.editor.utils.Utils;

import java.util.*;
import java.util.Map.Entry;

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

        Map<String, TGQImageFile> cachedImages = this.chunkedFileImageCache.get(chunkedFile);
        if (cachedImages == null) {
            this.chunkedFileImageCache.put(chunkedFile, cachedImages = new HashMap<>());
            for (kcCResource resource : chunkedFile.getChunks()) {
                if (!(resource instanceof TGQChunkTextureReference))
                    continue;

                TGQChunkTextureReference texRef = (TGQChunkTextureReference) resource;
                TGQFile texRefFile = chunkedFile.getMainArchive().getFileByName(chunkedFile, texRef.getPath());
                if (texRefFile instanceof TGQImageFile)
                    cachedImages.put(Utils.stripExtension(texRefFile.getFileName()), (TGQImageFile) texRefFile);
            }
        }

        for (kcMaterial material : materials) {
            TGQImageFile image = cachedImages.get(Utils.stripExtension(material.getTextureFileName()));
            this.allMaterials.put(material, sourceFile);
            if (image != null) {
                material.setTexture(image);
                this.multipleMatchMaterials.remove(material); // We prefer the one in the same chunked file, so if that exists we don't consider there to be multiple files which could apply.
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

            // Sort the
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
            if (material.getTexture() == null && material.hasTexture())
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

                    TGQFile targetImageFile = this.mainArchive.applyFileName(texturePath);
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
}