package net.highwayfrogs.editor.games.sony.shared.vlo2.vram;

import lombok.Getter;
import lombok.NonNull;
import net.highwayfrogs.editor.games.sony.SCGameFile;
import net.highwayfrogs.editor.games.sony.SCGameInstance;
import net.highwayfrogs.editor.games.sony.shared.mwd.mwi.MWIResourceEntry;
import net.highwayfrogs.editor.games.sony.shared.vlo2.VloFile;
import net.highwayfrogs.editor.system.Config;
import net.highwayfrogs.editor.system.Config.ConfigValueNode;
import net.highwayfrogs.editor.utils.StringUtils;
import net.highwayfrogs.editor.utils.Utils;
import net.highwayfrogs.editor.utils.logging.ILogger;
import net.highwayfrogs.editor.utils.objects.CountMap;
import net.highwayfrogs.editor.utils.objects.StringNode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Represents a node forming a tree, used to group which VLO files can be used concurrently, used to build VLO file texture positions.
 * Created by Kneesnap on 1/15/2026.
 */
public class VloTreeNode {
    @Getter @NonNull protected final SCGameInstance instance;
    @Getter private final VloTreeNode parent;
    @Getter private final String name;
    @Getter private final VloVramSnapshot snapshot; // This snapshot contains a hodgepodge of different VLO textures which overlap with each other.
    @NonNull @Getter private final VloTreeNodeFillMethod fillMethod;
    private final List<MWIResourceEntry> vloFileEntries = new ArrayList<>(); // Must use MWIResourceEntry so if a new VloFile is imported, the Vlo files can still be resolved.
    private final List<VloTreeNode> children = new ArrayList<>();
    private final List<VloTreeNode> immutableChildren = Collections.unmodifiableList(this.children);
    @Getter private final int extraPages;
    @Getter private final int reservedPages;
    @Getter private final int usablePages;

    public static final int MAX_PAGE = 32;
    public static final String CONFIG_KEY_PAGES = "pages";
    public static final String CONFIG_KEY_RESERVED_PAGES = "reservedPages";
    public static final String CONFIG_KEY_EXTRA_PAGES = "extraPages";
    public static final String CONFIG_KEY_INSERTION_STRATEGY = "insertionStrategy";

    protected VloTreeNode(SCGameInstance instance, VloTreeNode parent, String name, VloTreeNodeFillMethod fillMethod, int pages, int reservedPages, int extraPages) {
        this.instance = instance;
        this.parent = parent;
        this.name = name;
        this.fillMethod = fillMethod;
        this.snapshot = new VloVramSnapshot(instance, this);
        this.usablePages = pages;
        this.reservedPages = reservedPages;
        this.extraPages = extraPages;
    }

    /**
     * Gets the tree which this node is a part of
     */
    public VloTree getTree() {
        VloTreeNode temp = this;
        while (temp != null) {
            if (temp instanceof VloTree)
                return (VloTree) temp;

            temp = temp.parent;
        }

        throw new IllegalStateException("Reached the end of the parent-chain, but no tree was found! Shouldn't be possible!");
    }

    /**
     * Get the vlo files represented by this node.
     * Each of these VLO files should be possible to load in place of each other, so overlap between these .VLO files are allowed.
     * @return vloFiles
     */
    public List<VloFile> getVloFiles() {
        List<VloFile> vloFiles = new ArrayList<>();
        for (int i = 0; i < this.vloFileEntries.size(); i++) {
            MWIResourceEntry vloFileEntry = this.vloFileEntries.get(i);
            SCGameFile<?> gameFile = vloFileEntry.getGameFile();
            if (!(gameFile instanceof VloFile))
                throw new IllegalStateException("File '" + vloFileEntry.getDisplayName() + "' is not a VloFile! (Was: " + Utils.getSimpleName(gameFile) + ")");

            vloFiles.add((VloFile) gameFile);
        }

        return vloFiles;
    }

    /**
     * Get the child nodes attached to this tree node.
     * Child nodes will not overlap with ANY of the VLO files of ANY parent node(s).
     * @return childNodes
     */
    public List<VloTreeNode> getChildren() {
        return this.immutableChildren;
    }

    /**
     * Test if the provided page ID is used by the VLOs.
     * NOTE: This will NOT include if parent nodes use the page.
     * @param page the page ID to test
     * @return true iff the page is used by the vlo files.
     */
    public boolean isPageUsable(int page) {
        return isValidPageId(page) && (this.usablePages & (1 << page)) != 0;
    }

    /**
     * Test if the provided page ID is reserved.
     * NOTE: This will NOT include if parent nodes reserve the page.
     * @param page the page ID to test
     * @return true iff the page is reserved / unusable by any child nodes.
     */
    public boolean isPageReserved(int page) {
        return isValidPageId(page) && (this.reservedPages & (1 << page)) != 0;
    }

    /**
     * Rebuild the vram positions of all Vlo files marked as dirty, and recursively any which depend on the rebuilt Vlo files.
     */
    public void recursivelyBuildDirtyVloFiles() {
        List<VloFile> vloFiles = getVloFiles();
        boolean anyVlosDirty = false;
        for (int i = 0; i < vloFiles.size(); i++) {
            VloFile vloFile = vloFiles.get(i);
            if (vloFile.isVramDirty()) {
                anyVlosDirty = true;
                break;
            }
        }

        // If any vlos are dirty, this node and all its children should be recursively rebuilt.
        if (anyVlosDirty) {
            rebuildRecursive();
            return;
        }

        // Recursively run for children.
        for (int i = 0; i < this.children.size(); i++)
            this.children.get(i).recursivelyBuildDirtyVloFiles();
    }

    /**
     * Builds the node recursively from the pre-existing game data.
     */
    public void loadFromGameDataRecursive() {
        recursivelyBuildTree(true);
    }

    /**
     * Rebuilds the texture positions for vlo files in this node.
     * Recursively rebuilds any child nodes to respond to the change.
     */
    public void rebuildRecursive() {
        recursivelyBuildTree(false);
    }

    private void recursivelyBuildTree(boolean loadFromGameData) {
        // Copy the dirty snapshot from the parent to ensure we don't place textures somewhere already used.
        if (this.parent != null && this.parent.snapshot != null) {
            this.parent.snapshot.copyTo(this.snapshot);
        } else {
            this.snapshot.clear();
        }

        // Add pre-vlo data.
        this.snapshot.addNodeData();

        // Vlo files need to have snapshots created.
        List<VloFile> vloFiles = getVloFiles();
        if (!vloFiles.isEmpty()) {
            // Setup clean snapshot copy.
            VloVramSnapshot cleanSnapshot = new VloVramSnapshot(this.snapshot.getGameInstance(), null);
            this.snapshot.copyTo(cleanSnapshot);

            // Add vlo files.
            VloTree tree = getTree();
            for (int i = 0; i < vloFiles.size(); i++) {
                VloFile vloFile = vloFiles.get(i);
                VloVramSnapshot snapshot = tree.getVramSnapshot(vloFile);
                if (snapshot != null) {
                    cleanSnapshot.copyTo(snapshot);
                    snapshot.addVlo(tree, vloFile, !loadFromGameData);
                }
            }
        }

        // Rebuild child nodes.
        // This must happen after VLOs are setup, so these will have access to the dirty snapshot.
        for (int i = 0; i < this.children.size(); i++)
            this.children.get(i).recursivelyBuildTree(loadFromGameData);
    }

    /**
     * Test if the given page ID is a valid page ID.
     * @param page the page ID to test
     * @return true iff the page ID is valid
     */
    public static boolean isValidPageId(int page) {
        return page >= 0 && page < MAX_PAGE;
    }

    static VloTreeNode readVloTreeNode(ILogger logger, SCGameInstance instance, VloTree tree, VloTreeNode parent, Config config, CountMap<MWIResourceEntry> vloUsages) {
        List<MWIResourceEntry> vloEntries = getVloEntries(logger, instance, config, vloUsages);
        int pages = getPageBitFlags(vloEntries.isEmpty() ? config.getOptionalKeyValueNode(CONFIG_KEY_PAGES) : config.getKeyValueNodeOrError(CONFIG_KEY_PAGES));
        int reservedPages = getPageBitFlags(config.getOptionalKeyValueNode(CONFIG_KEY_RESERVED_PAGES));
        int extraPages = getPageBitFlags(config.getOptionalKeyValueNode(CONFIG_KEY_EXTRA_PAGES));
        VloTreeNodeFillMethod fillMethod = config.getOrDefaultKeyValueNode(CONFIG_KEY_INSERTION_STRATEGY).getAsEnum(VloTreeNodeFillMethod.FILL_PAGE);

        // Create node.
        VloTreeNode newNode;
        if (tree != null) {
            if (parent == null)
                throw new IllegalArgumentException("parent cannot be null if tree is non-null!");

            newNode = new VloTreeNode(instance, parent, config.getSectionName(), fillMethod, pages, reservedPages, extraPages);
        } else {
            if (parent != null)
                throw new IllegalArgumentException("parent cannot be non-null if tree is null!");

            newNode = tree = new VloTree(instance, config.getSectionName(), fillMethod, pages, reservedPages, extraPages);
        }

        // Read vlo files.
        for (int i = 0; i < vloEntries.size(); i++) {
            MWIResourceEntry vloFileEntry = vloEntries.get(i);
            newNode.vloFileEntries.add(vloFileEntry);
            tree.nodesByResourceEntry.put(vloFileEntry, newNode);
            tree.snapshotsByResourceEntry.put(vloFileEntry, new VloVramSnapshot(instance, newNode));
        }

        // Read child nodes.
        List<Config> childConfigs = config.getChildConfigNodes();
        for (int i = 0; i < childConfigs.size(); i++) {
            Config childCfg = childConfigs.get(i);
            newNode.children.add(readVloTreeNode(logger, instance, tree, newNode, childCfg, vloUsages));
        }

        return newNode;
    }

    private static List<MWIResourceEntry> getVloEntries(ILogger logger, SCGameInstance instance, Config config, CountMap<MWIResourceEntry> vloUsages) {
        List<MWIResourceEntry> vloEntries = new ArrayList<>();
        List<ConfigValueNode> vloFileNames = config.getTextNodes();
        for (int i = 0; i < vloFileNames.size(); i++) {
            ConfigValueNode vloFileNameNode = vloFileNames.get(i);
            String vloFileName = vloFileNameNode != null ? vloFileNameNode.getAsStringLiteral() : null;
            if (StringUtils.isNullOrWhiteSpace(vloFileName))
                continue;

            MWIResourceEntry vloFileEntry = instance.getResourceEntryByName(vloFileName);
            if (vloFileEntry == null) {
                if (logger != null)
                    logger.warning("VloTreeNode could not find the VLO file named %s{%s}.", vloFileName, vloFileNameNode.getExtraDebugErrorInfo());

                continue;
            }

            if (vloUsages.getAndAdd(vloFileEntry) > 0) {
                if (logger != null)
                    logger.warning("VloTreeNode had VLO file %s{%s}, which has already been used elsewhere in the VloTree, and is thus ignored. (VLO saving may break if this is not fixed)", vloFileName, vloFileNameNode.getExtraDebugErrorInfo());
            } else {
                vloEntries.add(vloFileEntry);
            }
        }

        return vloEntries;
    }

    // Parses a string such as '2,5-15,21-31' into a single integer, with bits set for the ranges of bits provided.
    private static int getPageBitFlags(StringNode node) {
        if (node == null)
            return 0; // None.

        String value = node.getAsString();
        if (StringUtils.isNullOrEmpty(value))
            return 0;

        int result = 0;
        int lastRangeMinimumValue = -1;
        int lastIntStart = -1;
        for (int i = 0; i <= value.length(); i++) {
            char temp = i >= value.length() ? ',' : value.charAt(i);
            if (lastIntStart < 0 && Character.isWhitespace(temp))
                continue; // Allow whitespace.

            if (temp == ',') {
                if (lastIntStart < 0) // No value is currently being read.
                    throw new IllegalArgumentException("Invalid ',' found in '" + value + "'. " + node.getExtraDebugErrorInfo());

                int currentValue = parsePageId(node, value, lastIntStart, i);
                if (lastRangeMinimumValue >= 0) {
                    if (lastRangeMinimumValue > currentValue) // Range max is less than range min
                        throw new IllegalArgumentException("Invalid range " + lastRangeMinimumValue + "-" + currentValue + " in '" + value + "'. " + node.getExtraDebugErrorInfo());

                    for (int j = lastRangeMinimumValue; j <= currentValue; j++)
                        result |= (1 << j);
                } else {
                    result |= (1 << currentValue);
                }

                lastRangeMinimumValue = -1;
                lastIntStart = -1;
            } else if (temp == '-') {
                if (lastIntStart < 0 || lastRangeMinimumValue >= 0)
                    throw new IllegalArgumentException("Invalid range syntax in '" + value + "'. " + node.getExtraDebugErrorInfo());

                lastRangeMinimumValue = parsePageId(node, value, lastIntStart, i);
                lastIntStart = -1;
            } else if (temp >= '0' && temp <= '9') {
                if (lastIntStart < 0)
                    lastIntStart = i;
            } else {
                throw new IllegalArgumentException("Found invalid character '" + temp + "'. " + node.getExtraDebugErrorInfo());
            }
        }

        return result;
    }

    private static int parsePageId(StringNode node, String value, int startIndex, int endIndex) {
        int parsedValue = Integer.parseInt(value.substring(startIndex, endIndex));
        if (!isValidPageId(parsedValue))
            throw new IllegalArgumentException("Invalid page number: " + parsedValue + ". " + node.getExtraDebugErrorInfo());

        return parsedValue;
    }
}
