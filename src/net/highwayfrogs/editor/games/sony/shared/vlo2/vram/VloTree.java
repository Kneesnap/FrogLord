package net.highwayfrogs.editor.games.sony.shared.vlo2.vram;

import lombok.Getter;
import net.highwayfrogs.editor.games.sony.SCGameInstance;
import net.highwayfrogs.editor.games.sony.shared.mwd.mwi.MWIResourceEntry;
import net.highwayfrogs.editor.games.sony.shared.vlo2.VloClut;
import net.highwayfrogs.editor.games.sony.shared.vlo2.VloFile;
import net.highwayfrogs.editor.games.sony.shared.vlo2.VloImage;
import net.highwayfrogs.editor.games.sony.shared.vlo2.VloUtils;
import net.highwayfrogs.editor.system.Config;
import net.highwayfrogs.editor.utils.logging.ILogger;
import net.highwayfrogs.editor.utils.objects.CountMap;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents a tree used to ensure textures .
 * Created by Kneesnap on 1/15/2026.
 */
public final class VloTree extends VloTreeNode {
    @Getter private final int transparentPages;
    final Map<MWIResourceEntry, VloFileTreeData> vloFileDataByResourceEntry = new HashMap<>(); // Do not use the VloFile directly as the key, just in-case it gets imported.
    final Map<String, VloTreeNode> generatedNodesByName = new HashMap<>();

    VloTree(SCGameInstance instance, String name, VloTreeNodeFillMethod fillMethod, int pages, int reservedPages, int extraPages, int originalPages, int clutPages, List<String> includedNodeNames, int transparentPages) {
        super(instance, null, name, fillMethod, pages, reservedPages, extraPages, originalPages, clutPages, includedNodeNames);
        this.transparentPages = transparentPages;
    }

    /**
     * Warn about vlo tree entries which don't behave in an expected manner.
     * @param logger the logger to log warnings to
     */
    public void warnAboutUnusedVloFilesAndImages(ILogger logger) {
        if (logger == null)
            logger = this.instance.getLogger();

        // Warn about unused Vlos.
        int transparentPages = 0;
        List<VloFile> allVloFiles = this.instance.getMainArchive().getAllFiles(VloFile.class);
        for (int i = 0; i < allVloFiles.size(); i++) {
            VloFile vloFile = allVloFiles.get(i);
            VloTreeNode node = getNode(vloFile);
            if (node == null)
                logger.severe("VloTree did not use the VLO file '%s', so that file may experience problems with modification.", vloFile.getFileDisplayName());

            transparentPages = updateTransparentPages(vloFile, transparentPages);
            if (node != null)
                warnForOutOfBoundsImages(logger, vloFile, node);
        }

        if (this.transparentPages == 0 && transparentPages != 0)
            logger.info("Transparent Pages: %s", getPages(transparentPages));
    }

    /**
     * Get the node (if one exists) responsible for positioning textures in the provided VLO file.
     * @param vloFile the vlo file to get the node for
     * @return vloTreeNode, or null if none exist
     */
    public VloFileTreeData getVloFileData(VloFile vloFile) {
        if (vloFile == null)
            return null;

        MWIResourceEntry entry = vloFile.getIndexEntry();
        return entry != null ? this.vloFileDataByResourceEntry.get(entry) : null;
    }

    /**
     * Get the node (if one exists) responsible for positioning textures in the provided VLO file.
     * @param vloFile the vlo file to get the node for
     * @return vloTreeNode, or null if none exist
     */
    public VloTreeNode getNode(VloFile vloFile) {
        VloFileTreeData data = getVloFileData(vloFile);
        return data != null ? data.getNode() : null;
    }

    /**
     * Get the Vram snapshot for positioning textures in the provided VLO file.
     * @param vloFile the vlo file to get the node for
     * @return vramSnapshot, or null if none exist
     */
    public VloVramSnapshot getVramSnapshot(VloFile vloFile) {
        VloFileTreeData data = getVloFileData(vloFile);
        return data != null ? data.getSnapshot() : null;
    }

    /**
     * Get the texture ID tracker responsible for managing available texture IDs in the Vlo file.
     * @param vloFile the vlo file to get the node for
     * @return textureTracker, or null if none exists
     */
    public VloTextureIdTracker getVloTextureIdTracker(VloFile vloFile) {
        VloFileTreeData data = getVloFileData(vloFile);
        return data != null ? data.getTextureIdTracker() : null;
    }

    /**
     * Resets the texture ID trackers.
     */
    private void resetTextureIdTrackers() {
        int maxTextureId = this.instance.getMaximumTextureId();
        if (maxTextureId < 0)
            throw new IllegalStateException("Cannot resetTextureIdTrackers before the maximum texture ID has been determined.");

        List<VloTreeNode> queue = new ArrayList<>();
        queue.add(this);
        while (queue.size() > 0) {
            VloTreeNode node = queue.remove(queue.size() - 1);
            queue.addAll(node.getChildren());

            // Reset the trackers.
            node.getTextureIdTracker().reset(maxTextureId + 1);
            List<VloFileTreeData> fileTreeDataEntries = node.getVloFileDataEntries();
            for (int i = 0; i < fileTreeDataEntries.size(); i++)
                fileTreeDataEntries.get(i).getTextureIdTracker().reset(maxTextureId + 1);
        }
    }

    /**
     * Calculates which texture IDs are available for different VLO files, from the current VLO files.
     */
    public void calculateFreeTextureIds() {
        resetTextureIdTrackers();

        List<VloTreeNode> queue = new ArrayList<>();
        queue.add(this);
        for (int i = 0; i < queue.size(); i++) {
            VloTreeNode node = queue.get(i);
            queue.addAll(node.getChildren());

            // Restrict all texture IDs in all Vlos.
            List<VloFileTreeData> fileTreeDataEntries = node.getVloFileDataEntries();
            for (int j = 0; j < fileTreeDataEntries.size(); j++) {
                VloFileTreeData fileTreeData = fileTreeDataEntries.get(j);
                VloFile vloFile = fileTreeData.getVloFile();
                List<VloImage> images = vloFile.getImages();
                for (int k = 0; k < images.size(); k++)
                    fileTreeData.getTextureIdTracker().restrictTextureId(images.get(k).getTextureId());
            }
        }
    }

    /**
     * Parses a config into a VloTree.
     * @param logger the logger to write any warnings/errors to
     * @param instance the game instance to resolve VLOs with
     * @param config the config to set up the VloTree from
     * @return vloTreeRootNode
     */
    public static VloTree readVloTree(ILogger logger, SCGameInstance instance, Config config) {
        if (instance == null)
            throw new NullPointerException("instance");
        if (config == null)
            throw new NullPointerException("config");

        CountMap<MWIResourceEntry> vloUsages = new CountMap<>();

        VloTree tree;
        try {
            tree = (VloTree) readVloTreeNode(logger, instance, null, null, config, vloUsages);
        } catch (Throwable th) {
            throw new RuntimeException("Failed to parse a VloTree from the given config.", th);
        }

        if (logger != null)
            config.recursivelyWarnAboutUnusedData(logger);

        return tree;
    }

    private static int updateTransparentPages(VloFile vloFile, int transparentPages) {
        if (vloFile.isPsxMode())
            return transparentPages;

        // Check images for pages used.
        List<VloImage> images = vloFile.getImages();
        for (int j = 0; j < images.size(); j++) {
            VloImage image = images.get(j);
            if (!image.testFlag(VloImage.FLAG_BLACK_IS_TRANSPARENT))
                continue;

            int startPage = image.getPage();
            int endPage = image.getEndPage();
            int startPageGridX = VloUtils.getPageGridX(false, startPage);
            int startPageGridY = VloUtils.getPageGridY(false, startPage);
            int endPageGridX = VloUtils.getPageGridX(false, endPage);
            int endPageGridY = VloUtils.getPageGridY(false, endPage);
            for (int y = startPageGridY; y <= endPageGridY; y++)
                for (int x = startPageGridX; x <= endPageGridX; x++)
                    transparentPages |= (1 << VloUtils.getPageFromGridPos(false, x, y));
        }

        return transparentPages;
    }

        private static void warnForOutOfBoundsImages(ILogger logger, VloFile vloFile, VloTreeNode node) {
        boolean psxMode = vloFile.isPsxMode();

        // Check images for pages used.
        int imagePages = 0;
        List<VloImage> images = vloFile.getImages();
        for (int j = 0; j < images.size(); j++) {
            VloImage image = images.get(j);
            int startPage = image.getPage();
            int endPage = image.getEndPage();
            int startPageGridX = VloUtils.getPageGridX(psxMode, startPage);
            int startPageGridY = VloUtils.getPageGridY(psxMode, startPage);
            int endPageGridX = VloUtils.getPageGridX(psxMode, endPage);
            int endPageGridY = VloUtils.getPageGridY(psxMode, endPage);
            for (int y = startPageGridY; y <= endPageGridY; y++)
                for (int x = startPageGridX; x <= endPageGridX; x++)
                    imagePages |= (1 << VloUtils.getPageFromGridPos(psxMode, x, y));
        }

        // Check CLUTs for pages used.
        int clutPages = 0;
        List<VloClut> cluts = vloFile.getClutList().getCluts();
        for (int j = 0; j < cluts.size(); j++) {
            VloClut clut = cluts.get(j);
            int startPage = VloUtils.getPageFromVramPos(vloFile.getGameInstance(), clut.getX(), clut.getY());
            int endPage = VloUtils.getPageFromVramPos(vloFile.getGameInstance(), clut.getX() + clut.getWidth() - 1, clut.getY() + clut.getHeight() - 1);
            int startPageGridX = VloUtils.getPageGridX(psxMode, startPage);
            int startPageGridY = VloUtils.getPageGridY(psxMode, startPage);
            int endPageGridX = VloUtils.getPageGridX(psxMode, endPage);
            int endPageGridY = VloUtils.getPageGridY(psxMode, endPage);
            for (int y = startPageGridY; y <= endPageGridY; y++)
                for (int x = startPageGridX; x <= endPageGridX; x++)
                    clutPages |= (1 << VloUtils.getPageFromGridPos(psxMode, x, y));
        }

        // Figure out which pages are allowed.
        int clutPagesFromImagePages = (node.getUsablePages() & VloVramSnapshot.PSX_VRAM_BOTTOM_PAGE_BIT_MASK);
        if (clutPagesFromImagePages == 0)
            clutPagesFromImagePages = node.getUsablePages();

        int testImagePages = node.getOriginalPages() != 0 ? node.getOriginalPages() : node.getUsablePages();
        int testClutPages = node.getClutPages() != 0 ? node.getClutPages() : clutPagesFromImagePages;
        if (vloFile.getGameInstance().isPreviouslySavedByFrogLord()) {
            testImagePages |= node.getExtraPages();
            testClutPages |= node.getExtraPages();
        }

        // Test the pages seen.
        int leftOverImagePages = imagePages & ~testImagePages;
        if (leftOverImagePages != 0)
            logger.warning("File '%s' was not expected to use image page(s) %s!", vloFile.getFileDisplayName(), getPages(leftOverImagePages));

        int leftOverClutPages = clutPages & ~testClutPages;
        if (leftOverClutPages != 0)
            logger.warning("File '%s' was not expected to use CLUT page(s) %s!", vloFile.getFileDisplayName(), getPages(leftOverClutPages));
    }

    private static String getPages(int pages) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < VloTreeNode.MAX_PAGE; i++) {
            if ((pages & (1 << i)) == 0)
                continue;

            if (builder.length() > 0)
                builder.append(", ");
            builder.append(i);
        }

        return builder.toString();
    }
}
