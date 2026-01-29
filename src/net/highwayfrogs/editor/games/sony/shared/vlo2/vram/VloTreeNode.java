package net.highwayfrogs.editor.games.sony.shared.vlo2.vram;

import lombok.Getter;
import lombok.NonNull;
import net.highwayfrogs.editor.games.sony.SCGameInstance;
import net.highwayfrogs.editor.games.sony.shared.mwd.mwi.MWIResourceEntry;
import net.highwayfrogs.editor.games.sony.shared.vlo2.VloClut;
import net.highwayfrogs.editor.games.sony.shared.vlo2.VloFile;
import net.highwayfrogs.editor.games.sony.shared.vlo2.VloImage;
import net.highwayfrogs.editor.gui.components.ProgressBarComponent;
import net.highwayfrogs.editor.system.Config;
import net.highwayfrogs.editor.system.Config.ConfigValueNode;
import net.highwayfrogs.editor.utils.StringUtils;
import net.highwayfrogs.editor.utils.logging.ILogger;
import net.highwayfrogs.editor.utils.objects.CountMap;
import net.highwayfrogs.editor.utils.objects.StringNode;

import java.util.ArrayList;
import java.util.Arrays;
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
    @Getter private final VloTextureIdTracker textureIdTracker;
    private final List<VloFileTreeData> vloFiles = new ArrayList<>();
    private final List<VloFileTreeData> immutableVloFiles = Collections.unmodifiableList(this.vloFiles);
    private final List<VloTreeNode> children = new ArrayList<>();
    private final List<VloTreeNode> immutableChildren = Collections.unmodifiableList(this.children);
    private final List<VloTreeNode> includedNodes;
    private final List<VloTreeNode> dependantNodes = new ArrayList<>(); // Nodes which implicitly must be updated when this one is updated, other than child nodes. This is primarily for the "include=" feature, and is not commonly used.
    @Getter private final int extraPages;
    @Getter private final int reservedPages;
    @Getter private final int usablePages;
    @Getter private final int originalPages;
    @Getter private final int clutPages;

    public static final int MAX_PAGE = 32;
    public static final String CONFIG_KEY_PAGES = "pages";
    public static final String CONFIG_KEY_RESERVED_PAGES = "reservedPages";
    public static final String CONFIG_KEY_EXTRA_PAGES = "extraPages";
    public static final String CONFIG_KEY_ORIGINAL_PAGES = "originalPages";
    public static final String CONFIG_KEY_CLUT_PAGES = "clutPages";
    public static final String CONFIG_KEY_INCLUDE = "include";
    public static final String CONFIG_KEY_INSERTION_STRATEGY = "insertionStrategy";
    public static final String CONFIG_KEY_TRANSPARENT_PAGES = "pagesWhereBlackIsTransparent";

    protected VloTreeNode(SCGameInstance instance, VloTreeNode parent, String name, VloTreeNodeFillMethod fillMethod, int pages, int reservedPages, int extraPages, int originalPages, int clutPages, List<VloTreeNode> includedNodes) {
        this.instance = instance;
        this.parent = parent;
        this.name = name;
        this.fillMethod = fillMethod;
        this.includedNodes = includedNodes != null && !includedNodes.isEmpty() ? new ArrayList<>(includedNodes) : Collections.emptyList();
        this.snapshot = new VloVramSnapshot(instance, this);
        this.usablePages = pages;
        this.reservedPages = reservedPages;
        this.extraPages = extraPages;
        this.originalPages = originalPages;
        this.clutPages = clutPages;
        this.textureIdTracker = new VloTextureIdTracker(this, parent != null ? parent.textureIdTracker : null, null);
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
     * Gets a list of each of the VLO files usable by this node, and their corresponding data.
     * @return vloFileEntries
     */
    public List<VloFileTreeData> getVloFileDataEntries() {
        return this.immutableVloFiles;
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
     * @param progressBar an optional progressBar component to show the status of
     */
    public void recursivelyBuildDirtyVloFiles(ProgressBarComponent progressBar) {
        List<VloTreeNode> queue = new ArrayList<>();
        List<VloTreeNode> dirtyQueue = new ArrayList<>();
        queue.add(this);
        for (int i = 0; i < queue.size(); i++) {
            VloTreeNode node = queue.get(i);

            boolean anyVlosDirty = (node instanceof VloTree) && ((VloTree) node).isRebuildQueued();
            for (int j = 0; j < node.vloFiles.size(); j++) {
                VloFileTreeData vloFileData = node.vloFiles.get(j);
                if (vloFileData.getVloFile().isVramDirty()) {
                    anyVlosDirty = true;
                    break;
                }
            }

            if (anyVlosDirty) {
                node.recursivelyMarkDependantNodesDirty(); // Dependant nodes must by definition not yet be processed in the queue.
                dirtyQueue.add(node);
                addChildNodesToQueue(dirtyQueue, node);
            } else {
                queue.addAll(node.getChildren());
            }
        }

        // If any vlos are dirty, this node and all its children should be recursively rebuilt.
        recursivelyBuildTree(getTree(), dirtyQueue, progressBar, true);
    }

    /**
     * Builds the node recursively from the pre-existing game data.
     * @param progressBar the progress bar to display load status with (Optional)
     */
    public void loadFromGameDataRecursive(ProgressBarComponent progressBar) {
        recursivelyBuildTree(progressBar, true);
    }

    /**
     * Rebuilds the texture positions for vlo files in this node.
     * Recursively rebuilds any child nodes to respond to the change.
     * @param progressBar the progress bar to display rebuild status with (Optional)
     */
    public void rebuildRecursive(ProgressBarComponent progressBar) {
        recursivelyBuildTree(progressBar, false);
    }

    private void recursivelyBuildTree(ProgressBarComponent progressBar, boolean loadFromGameData) {
        // Get a queue of all the nodes.
        List<VloTreeNode> queue = new ArrayList<>();
        queue.add(this);
        addChildNodesToQueue(queue, this);
        recursivelyBuildTree(getTree(), queue, progressBar, loadFromGameData);
    }

    private void buildNode(boolean loadFromGameData) {
        // Copy the dirty snapshot from the parent to ensure we don't place textures somewhere already used.
        if (this.parent != null && this.parent.snapshot != null) {
            this.parent.snapshot.copyTo(this.snapshot);
        } else {
            this.snapshot.clear();
        }

        // Include other nodes. (This is rare)
        VloTree tree = getTree();
        if (!this.includedNodes.isEmpty()) {
            for (int i = 0; i < this.includedNodes.size(); i++) {
                VloTreeNode node = this.includedNodes.get(i);
                if (!tree.generatedNodes.contains(node))
                    throw new IllegalArgumentException("The node named '" + node.getName() + "' had not been generated yet, so it cannot be included by '" + this.name + "'.");

                // Add data from the target VLO files here.
                for (int j = 0; j < node.vloFiles.size(); j++) {
                    VloFile vloFile = node.vloFiles.get(j).getVloFile();

                    // Add images.
                    List<VloImage> images = vloFile.getImages();
                    for (int k = 0; k < images.size(); k++)
                        this.snapshot.applyEntryToCache(new VloVramEntryImage(images.get(k)), false);

                    // Add cluts.
                    if (vloFile.isPsxMode()) {
                        List<VloClut> cluts = vloFile.getClutList().getCluts();
                        for (int k = 0; k < cluts.size(); k++)
                            this.snapshot.applyEntryToCache(new VloVramEntryClut(cluts.get(k)), false);
                    }
                }
            }
        }

        // Add pre-vlo data.
        this.snapshot.addNodeData();

        // Vlo files need to have snapshots created.
        if (!this.vloFiles.isEmpty()) {
            // Setup clean snapshot copy.
            VloVramSnapshot cleanSnapshot = new VloVramSnapshot(this.snapshot.getGameInstance(), null);
            this.snapshot.copyTo(cleanSnapshot);

            // Add vlo files.
            for (int i = 0; i < this.vloFiles.size(); i++) {
                VloFileTreeData vloFileData = this.vloFiles.get(i);
                cleanSnapshot.copyTo(vloFileData.getSnapshot());
                vloFileData.getSnapshot().addVlo(tree, vloFileData.getVloFile(), !loadFromGameData);
            }
        }

        // Building child nodes must only occur AFTER Vlos are setup, so they will have access to the dirty snapshot.
        // This is expected to be managed by the calling function.
    }


    private static void addChildNodesToQueue(List<VloTreeNode> queue, VloTreeNode node) {
        List<VloTreeNode> children = node.getChildren();
        for (int i = 0; i < children.size(); i++) {
            VloTreeNode childNode = children.get(i);
            if (childNode.includedNodes.isEmpty())
                queue.add(childNode);
        }

        for (int i = 0; i < children.size(); i++) {
            VloTreeNode childNode = children.get(i);
            if (childNode.includedNodes.isEmpty())
                addChildNodesToQueue(queue, childNode);
        }

        // Ensure nodes which include others are added last.
        for (int i = 0; i < children.size(); i++) {
            VloTreeNode childNode = children.get(i);
            if (!childNode.includedNodes.isEmpty())
                queue.add(childNode);
        }

        for (int i = 0; i < children.size(); i++) {
            VloTreeNode childNode = children.get(i);
            if (!childNode.includedNodes.isEmpty())
                addChildNodesToQueue(queue, childNode);
        }
    }

    private static void recursivelyBuildTree(VloTree tree, List<VloTreeNode> queue, ProgressBarComponent progressBar, boolean loadFromGameData) {
        if (progressBar != null)
            progressBar.update(0, queue.size(), "Vlo Texture Placement");

        // Remove nodes from the map of generated nodes.
        for (int i = 0; i < queue.size(); i++) {
            VloTreeNode node = queue.get(i);
            if (tree == null)
                tree = node.getTree();

            tree.generatedNodes.remove(node);
        }

        // Builds the nodes in queue order. (NOTE: Respect the queue order/ensure parents are built before child nodes)
        for (int i = 0; i < queue.size(); i++) {
            VloTreeNode node = queue.get(i);
            node.buildNode(loadFromGameData);
            tree.generatedNodes.add(node); // Do this upon completion, so no child node can reference this by name.
            if (progressBar != null)
                progressBar.addCompletedProgress(1);
        }
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
        int pages = getDefaultPages(instance, config, vloEntries);
        int reservedPages = getPageBitFlags(config.getOptionalKeyValueNode(CONFIG_KEY_RESERVED_PAGES));
        int extraPages = getPageBitFlags(config.getOptionalKeyValueNode(CONFIG_KEY_EXTRA_PAGES));
        int originalPages = getPageBitFlags(config.getOptionalKeyValueNode(CONFIG_KEY_ORIGINAL_PAGES));
        int clutPages = getPageBitFlags(config.getOptionalKeyValueNode(CONFIG_KEY_CLUT_PAGES));
        VloTreeNodeFillMethod fillMethod = config.getOrDefaultKeyValueNode(CONFIG_KEY_INSERTION_STRATEGY).getAsEnum(VloTreeNodeFillMethod.AUTOMATIC);

        // Create node.
        VloTreeNode newNode;
        List<VloTreeNode> includedNodes = null;
        if (tree != null) {
            if (parent == null)
                throw new IllegalArgumentException("parent cannot be null if tree is non-null!");

            includedNodes = resolveNodeList(logger, tree, config.getOptionalKeyValueNode(CONFIG_KEY_INCLUDE));
            newNode = new VloTreeNode(instance, parent, config.getSectionName(), fillMethod, pages, reservedPages, extraPages, originalPages, clutPages, includedNodes);

            // Store by name.
            VloTreeNode oldNodeWithName = tree.nodesByName.putIfAbsent(newNode.getName(), newNode);
            if (oldNodeWithName != null)
                logger.warning("Multiple nodes named '%s' were configured in the VloTree! This may break certain vlo features.", newNode.getName());
        } else {
            if (parent != null)
                throw new IllegalArgumentException("parent cannot be non-null if tree is null!");

            int transparentPages = instance.isPC() ? getPageBitFlags(config.getOptionalKeyValueNode(CONFIG_KEY_TRANSPARENT_PAGES)) : 0;
            newNode = tree = new VloTree(instance, config.getSectionName(), fillMethod, pages, reservedPages, extraPages, originalPages, clutPages, transparentPages);
        }

        // Setup dependant nodes.
        if (includedNodes != null)
            for (int i = 0; i < includedNodes.size(); i++)
                includedNodes.get(i).dependantNodes.add(newNode);

        // Read vlo files.
        for (int i = 0; i < vloEntries.size(); i++) {
            MWIResourceEntry vloFileEntry = vloEntries.get(i);
            VloFileTreeData data = new VloFileTreeData(newNode, vloFileEntry);
            newNode.vloFiles.add(data);
            tree.vloFileDataByResourceEntry.put(vloFileEntry, data);
        }

        // Read child nodes.
        List<Config> childConfigs = config.getChildConfigNodes();
        for (int i = 0; i < childConfigs.size(); i++) {
            Config childCfg = childConfigs.get(i);
            newNode.children.add(readVloTreeNode(logger, instance, tree, newNode, childCfg, vloUsages));
        }

        return newNode;
    }

    private static List<VloTreeNode> resolveNodeList(ILogger logger, VloTree vloTree, StringNode node) {
        if (node == null || vloTree == null)
            return Collections.emptyList();

        // Load included node names.
        List<String> nodeNames = Arrays.asList(node.getAsString().split("(\\s*),(\\s*)"));
        if (nodeNames.isEmpty())
            return Collections.emptyList();

        List<VloTreeNode> nodes = new ArrayList<>();
        for (int i = 0; i < nodeNames.size(); i++) {
            String nodeName = nodeNames.get(i);
            VloTreeNode foundNode = vloTree.nodesByName.get(nodeName);
            if (foundNode == null) {
                logger.warning("No VloTreeNode could be found named '%s', has it been defined yet?", nodeName);
                continue;
            }

            if (!nodes.contains(foundNode))
                nodes.add(foundNode);
        }

        return nodes;
    }

    private static int getDefaultPages(SCGameInstance instance, Config config, List<MWIResourceEntry> vloEntries) {
        if (instance.isPC()) {
            // PC version seems to allow all pages in most cases, so we'll default to all pages if not specified.
            ConfigValueNode pagesNode = config.getOptionalKeyValueNode(CONFIG_KEY_PAGES);
            return pagesNode != null ? getPageBitFlags(pagesNode) : (1 << VloImage.PC_VRAM_TOTAL_PAGES) - 1;
        } else {
            return getPageBitFlags(vloEntries.isEmpty() ? config.getOptionalKeyValueNode(CONFIG_KEY_PAGES) : config.getKeyValueNodeOrError(CONFIG_KEY_PAGES));
        }
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

    /**
     * Recursively marks all dependant nodes as dirty/needing vram refresh.
     */
    private void recursivelyMarkDependantNodesDirty() {
        for (int i = 0; i < this.dependantNodes.size(); i++) {
            VloTreeNode node = this.dependantNodes.get(i);
            for (int j = 0; j < node.vloFiles.size(); j++) {
                VloFile vloFile = node.vloFiles.get(j).getVloFile();
                if (vloFile != null)
                    vloFile.markDirty();
            }

            if (!node.dependantNodes.isEmpty())
                node.recursivelyMarkDependantNodesDirty();
        }
    }
}
