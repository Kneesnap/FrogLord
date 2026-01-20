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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents a tree used to ensure textures .
 * Created by Kneesnap on 1/15/2026.
 */
@Getter
public final class VloTree extends VloTreeNode {
    final Map<MWIResourceEntry, VloTreeNode> nodesByResourceEntry = new HashMap<>(); // Do not use the VloFile directly as the key, just in-case it gets imported.
    final Map<MWIResourceEntry, VloVramSnapshot> snapshotsByResourceEntry = new HashMap<>(); // Do not use the VloFile directly as the key, just in-case it gets imported.

    VloTree(SCGameInstance instance, String name, VloTreeNodeFillMethod fillMethod, int pages, int reservedPages, int extraPages) {
        super(instance, null, name, fillMethod, pages, reservedPages, extraPages);
    }

    /**
     * Warn about vlo tree entries which don't behave in an expected manner.
     * @param logger the logger to log warnings to
     */
    public void warnAboutUnusedVloFilesAndImages(ILogger logger) {
        if (logger == null)
            logger = this.instance.getLogger();

        // Warn about unused Vlos.
        List<VloFile> allVloFiles = this.instance.getMainArchive().getAllFiles(VloFile.class);
        for (int i = 0; i < allVloFiles.size(); i++) {
            VloFile vloFile = allVloFiles.get(i);
            VloTreeNode node = getNode(vloFile);
            if (node == null)
                logger.severe("VloTree did not use the VLO file '%s', so that file may experience problems with modification.", vloFile.getFileDisplayName());

            if (node != null)
                warnForOutOfBoundsImages(logger, vloFile, node);
        }
    }

    /**
     * Get the node (if one exists) responsible for positioning textures in the provided VLO file.
     * @param vloFile the vlo file to get the node for
     * @return vloTreeNode, or null if none exist
     */
    public VloTreeNode getNode(VloFile vloFile) {
        if (vloFile == null)
            return null;

        MWIResourceEntry entry = vloFile.getIndexEntry();
        return entry != null ? this.nodesByResourceEntry.get(entry) : null;
    }

    /**
     * Get the node (if one exists) responsible for positioning textures in the provided VLO file.
     * @param vloFile the vlo file to get the node for
     * @return vloTreeNode, or null if none exist
     */
    public VloVramSnapshot getVramSnapshot(VloFile vloFile) {
        if (vloFile == null)
            return null;

        MWIResourceEntry entry = vloFile.getIndexEntry();
        return entry != null ? this.snapshotsByResourceEntry.get(entry) : null;
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

    private static void warnForOutOfBoundsImages(ILogger logger, VloFile vloFile, VloTreeNode node) {
        int usedPages = 0;
        boolean psxMode = vloFile.isPsxMode();

        // Check images for pages used.
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
                    usedPages |= (1 << VloUtils.getPageFromGridPos(psxMode, x, y));
        }

        // Check CLUTs for pages used.
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
                    usedPages |= (1 << VloUtils.getPageFromGridPos(psxMode, x, y));
        }

        // Figure out which pages are allowed.
        int testPages = node.getUsablePages();
        if (vloFile.getGameInstance().isPreviouslySavedByFrogLord())
            testPages |= node.getExtraPages();

        // Test the pages seen.
        int leftOverPages = usedPages & ~testPages;
        if (leftOverPages == 0)
            return;

        // Build message.
        StringBuilder pages = new StringBuilder();
        for (int i = 0; i < VloTreeNode.MAX_PAGE; i++) {
            if ((leftOverPages & (1 << i)) == 0)
                continue;

            if (pages.length() > 0)
                pages.append(", ");
            pages.append(i);
        }

        logger.warning("File '%s' was not expected to use page(s) %s!", vloFile.getFileDisplayName(), pages);
    }
}
