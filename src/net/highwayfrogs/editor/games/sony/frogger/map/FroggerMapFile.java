package net.highwayfrogs.editor.games.sony.frogger.map;

import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import lombok.Getter;
import net.highwayfrogs.editor.file.config.FroggerMapConfig;
import net.highwayfrogs.editor.file.config.exe.LevelInfo;
import net.highwayfrogs.editor.file.config.exe.ThemeBook;
import net.highwayfrogs.editor.file.standard.SVector;
import net.highwayfrogs.editor.file.vlo.GameImage;
import net.highwayfrogs.editor.file.vlo.VLOArchive;
import net.highwayfrogs.editor.games.sony.frogger.FroggerConfig;
import net.highwayfrogs.editor.games.sony.frogger.FroggerGameInstance;
import net.highwayfrogs.editor.games.sony.frogger.map.data.FroggerMapLight;
import net.highwayfrogs.editor.games.sony.frogger.map.data.grid.FroggerGridSquare;
import net.highwayfrogs.editor.games.sony.frogger.map.data.grid.FroggerGridSquareReaction;
import net.highwayfrogs.editor.games.sony.frogger.map.data.grid.FroggerGridStack;
import net.highwayfrogs.editor.games.sony.frogger.map.mesh.FroggerMapMesh;
import net.highwayfrogs.editor.games.sony.frogger.map.mesh.FroggerMapMeshController;
import net.highwayfrogs.editor.games.sony.frogger.map.mesh.FroggerMapPolygon;
import net.highwayfrogs.editor.games.sony.frogger.map.mesh.FroggerMapPolygonType;
import net.highwayfrogs.editor.games.sony.frogger.map.packets.*;
import net.highwayfrogs.editor.games.sony.frogger.map.packets.FroggerMapFilePacketGeneral.FroggerMapStartRotation;
import net.highwayfrogs.editor.games.sony.frogger.ui.FroggerMapInfoUIController;
import net.highwayfrogs.editor.games.sony.shared.LinkedTextureRemap;
import net.highwayfrogs.editor.games.sony.shared.SCChunkedFile;
import net.highwayfrogs.editor.games.sony.shared.SCChunkedFile.SCFilePacket.PacketSizeType;
import net.highwayfrogs.editor.games.sony.shared.TextureRemapArray;
import net.highwayfrogs.editor.games.sony.shared.misc.MRLightType;
import net.highwayfrogs.editor.games.sony.shared.mwd.WADFile;
import net.highwayfrogs.editor.games.sony.shared.mwd.mwi.MWIResourceEntry;
import net.highwayfrogs.editor.gui.GameUIController;
import net.highwayfrogs.editor.gui.ImageResource;
import net.highwayfrogs.editor.gui.editor.MeshViewController;
import net.highwayfrogs.editor.utils.ColorUtils;
import net.highwayfrogs.editor.utils.FXUtils;
import net.highwayfrogs.editor.utils.Utils;
import net.highwayfrogs.editor.utils.Utils.ProblemResponse;
import net.highwayfrogs.editor.utils.objects.IndexBitArray;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Random;

/**
 * Represents a Frogger map file.
 * TODO:
 *  1) Pathing Work
 *  2) Lighting Work
 *  3) Fix all to-dos in map folder.
 *  4) Validate old versions to ensure they work. Fix bugs with them.
 *  5) Come up with a plan for how to make each UI as intuitive as possible. Incorporate 3D space as best as possible.
 *  6) Start work on the new Frogger terrain editor.
 *
 * TODO: We need to allow deselecting the current entry in the BasicList mesh view thing.
 * TODO: Finish converting the rest of the old FrogLord code to the new standards.
 * TODO: Build 7/8/11/17/18/19 -> GENERIC.VH FAILURE
 * TODO: It seems like gouraud shading is used significantly more than flat shading. How possible is it that the editor was gouraud primarily, and just created flat polygons whenever a gouraud polygon would be unnecessary to render in gouraud mode? Study maps to figure that one out. (The walls in desert levels make me question this idea)
 *
 * TODO: Future Editor Notes
 *  - When Frogger started, PSX Alpha shows their collision grid only supported quads, not tris.
 *  - Nearly all of the game is quads, but there are places that non-model tri's are used.
 *  - We'll need a way to configure textures, both when placing new polygons and updating existing ones. (Paint texture sheet onto tiles, Stretch single texture, repeat single texture, etc.)
 *  - Go through vanilla VLOs to find textures that should be treated as a whole.
 *  - They had the ability to auto-generate geometric shapes. For example, in Reservoir Frogs, the pipes are clearly cylinders where they stated "I would like 5 grid-squares worth of cylinder pls", then they moved the vertices down to deform the shape.
 *   -> Another example of where I think this was used are the branches on the tree on the ground in FOR2.
 *  - It seems all the operations necessary to modify Frogger map geometry can be done with the existing format, EXCEPT lighting.
 *  - Another important tool is the angler. Take a plane, 3D model, whatever. Really just a collection of vertices. Specify which axis you'd like to angle, then how much slope you'd like to apply. The Y value of each vertex will be changed up/down until reaching the end of the vertices in the desired axis.
 *   -> This is how JUNGLE was angled, and how SKIING was angled. Probably also how the slime sliding pipes were angled.
 *   -> There may have been a spline component to this too, as the bridge in FOR2 seems like that was feasible. Either that or it was angled by hand. Also feasible for something that small.
 *  - The grid is absolute, meaning grid squares cannot be arbitrarily sized. I think this means that the maximum vertex offset in XZ from its grid square is either .25 or .5 * size of square. Do an automated scan of each level's collision grid polygons to see if this is true.
 *   -> The limit appears to be 0.5 for USABLE_SAFE grid squares, and potentially 1.25 for everything else.
 *  - Have a UI where in the grid editor to add a new plane into the world. It should be perfectly aligned to grid tile coordinates, and allow changing the Y in the 3D world with a gizmo. Example of what that would add would be the leaves near the red frog in FOR2.
 *  - Allow placing a single texture into the map as a single polygon of specified grid size. Then, have a way to optionally split such textures into grid squares. (See FOR2.MAP's floor)
 *
 * TODO: Grid Editor Plans:
 *  - The plan eventually is to have it possible to use a selection rectangle (just like blender) to select multiple polygons at once
 *   - then you could click a button "add all selected polygons to grid" and it'll automatically place them in the right places
 * Created by Kneesnap on 8/22/2018.
 */
public class FroggerMapFile extends SCChunkedFile<FroggerGameInstance> {
    @Getter private final FroggerMapFilePacketHeader headerPacket;
    @Getter private final FroggerMapFilePacketGeneral generalPacket;
    @Getter private final FroggerMapFilePacketPath pathPacket;
    @Getter private final FroggerMapFilePacketZone zonePacket;
    @Getter private final FroggerMapFilePacketForm formPacket;
    @Getter private final FroggerMapFilePacketEntity entityPacket;
    @Getter private final FroggerMapFilePacketGraphical graphicalPacket;
    @Getter private final FroggerMapFilePacketLight lightPacket;
    @Getter private final FroggerMapFilePacketGroup groupPacket;
    @Getter private final FroggerMapFilePacketPolygon polygonPacket;
    @Getter private final FroggerMapFilePacketVertex vertexPacket;
    @Getter private final FroggerMapFilePacketGrid gridPacket;
    @Getter private final FroggerMapFilePacketAnimation animationPacket;
    private transient FroggerMapConfig cachedMapConfig;
    private transient VLOArchive cachedVloFile;
    private transient LinkedTextureRemap<FroggerMapFile> cachedTextureRemap;

    public FroggerMapFile(FroggerGameInstance instance, MWIResourceEntry resourceEntry) {
        super(instance, true);
        addFilePacket(this.headerPacket = new FroggerMapFilePacketHeader(this));
        addFilePacket(this.generalPacket = new FroggerMapFilePacketGeneral(this));
        addFilePacket(this.pathPacket = new FroggerMapFilePacketPath(this));
        addFilePacket(this.zonePacket = new FroggerMapFilePacketZone(this));
        addFilePacket(this.formPacket = new FroggerMapFilePacketForm(this));
        addFilePacket(this.entityPacket = new FroggerMapFilePacketEntity(this));
        addFilePacket(this.graphicalPacket = new FroggerMapFilePacketGraphical(this));
        addFilePacket(this.lightPacket = new FroggerMapFilePacketLight(this));
        addFilePacket(this.groupPacket = new FroggerMapFilePacketGroup(this));
        addFilePacket(this.polygonPacket = new FroggerMapFilePacketPolygon(this));
        addFilePacket(this.vertexPacket = new FroggerMapFilePacketVertex(this));
        addFilePacket(this.gridPacket = new FroggerMapFilePacketGrid(this));
        addFilePacket(this.animationPacket = new FroggerMapFilePacketAnimation(this, getMapConfig(resourceEntry).isMapAnimationSupported()));
    }

    @Override
    protected PacketSizeType getPacketSizeForUnknownChunk(String identifier) {
        return PacketSizeType.NO_SIZE;
    }

    @Override
    public String getCollectionViewDisplayStyle() {
        return getMapConfig().isIslandPlaceholder() ? "-fx-text-fill: red;" : null;
    }

    @Override
    public Image getCollectionViewIcon() {
        FroggerMapLevelID level = getMapLevelID();

        // Find level image, or create it if it's not found.
        Image levelImage = getGameInstance().getLevelImageMap().get(level);
        if (levelImage == null && getGameInstance().getLevelInfoMap().size() > 0) {
            LevelInfo info = getGameInstance().getLevelInfoMap().get(level);
            if (info != null) {
                GameImage levelTextureImage = getGameInstance().getImageFromPointer(info.getLevelTexturePointer());
                if (levelTextureImage != null)
                    getGameInstance().getLevelImageMap().put(level, levelImage = FXUtils.toFXImage(Utils.resizeImage(levelTextureImage.toBufferedImage(), 32, 32), false));
            }
        }

        return levelImage != null ? levelImage : ImageResource.TREASURE_MAP_32.getFxImage();
    }

    @Override
    public GameUIController<?> makeEditorUI() {
        return loadEditor(getGameInstance(), "edit-file-map", new FroggerMapInfoUIController(getGameInstance()), this);
    }

    @Override
    public void handleWadEdit(WADFile parent) {
        MeshViewController.setupMeshViewer(getGameInstance(), new FroggerMapMeshController(getGameInstance()), new FroggerMapMesh(this));
    }

    /**
     * Gets the remap table for this map.
     * @return remapTable
     */
    @SuppressWarnings("unchecked")
    public TextureRemapArray getTextureRemap() {
        if (this.cachedTextureRemap != null)
            return this.cachedTextureRemap;

        // Try to get the remap from the MWI Entry.
        MWIResourceEntry mwiEntry = getIndexEntry();
        if (mwiEntry != null)
            return this.cachedTextureRemap = (LinkedTextureRemap<FroggerMapFile>) getGameInstance().getLinkedTextureRemap(mwiEntry);

        return null;
    }

    /**
     * Gets the VLO file which is loaded at the same time as the map.
     * @return vloFile, if any
     */
    public VLOArchive getVloFile() {
        if (this.cachedVloFile != null)
            return this.cachedVloFile;

        ThemeBook themeBook = getGameInstance().getThemeBook(this.generalPacket.getMapTheme());
        return this.cachedVloFile = (themeBook != null ? themeBook.getVLO(this) : null);
    }

    @Override
    public FroggerConfig getConfig() {
        return (FroggerConfig) super.getConfig();
    }

    /**
     * Gets the map theme
     */
    public FroggerMapTheme getMapTheme() {
        return this.generalPacket.getMapTheme();
    }

    /**
     * Gets the map config usable by this map.
     * @return mapConfig
     */
    public FroggerMapConfig getMapConfig() {
        if (this.cachedMapConfig != null)
            return this.cachedMapConfig;

        return this.cachedMapConfig = getMapConfig(getIndexEntry());
    }

    /**
     * Gets the map config usable by this map.
     * @return mapConfig
     */
    protected FroggerMapConfig getMapConfig(MWIResourceEntry resourceEntry) {
        FroggerMapConfig mapConfig = resourceEntry != null ? getConfig().getMapConfigs().get(resourceEntry.getDisplayName()) : null;
        return mapConfig != null ? mapConfig : getConfig().getDefaultMapConfig();
    }

    /**
     * Returns true iff map animation is enabled for this map file.
     * @return mapAnimation
     */
    public boolean isMapAnimationEnabled() {
        return getMapConfig().isMapAnimationSupported();
    }

    /**
     * Checks if this map is a multiplayer map.
     * Unfortunately, nothing distinguishes the map files besides where you can access them from and the names.
     * @return isMultiplayer
     */
    public boolean isMultiplayer() {
        return getFileDisplayName().startsWith(this.generalPacket.getMapTheme().getInternalName() + "M");
    }

    /**
     * Checks if this map is a low-poly map.
     * Unfortunately, nothing distinguishes the map files besides where you can access them from and the names.
     * @return isLowPolyMode
     */
    public boolean isLowPolyMode() {
        return getGameInstance().isPC() && getFileDisplayName().contains("_WIN95");
    }

    /**
     * Procedurally generate an empty map, making it easier to start from scratch on a map you'd like to make in Blender.
     */
    public void randomizeMap(int xTileCount, int zTileCount) {
        this.generalPacket.setStartRotation(FroggerMapStartRotation.NORTH);
        this.generalPacket.setStartingTimeLimit(99);
        this.generalPacket.getDefaultCameraSourceOffset().setValues(0F, -100F, -25F);
        this.generalPacket.getDefaultCameraTargetOffset().clear();

        this.pathPacket.getPaths().clear();
        this.zonePacket.getZones().clear();
        this.formPacket.getForms().clear();
        this.entityPacket.clear();
        this.lightPacket.getLights().clear();
        this.vertexPacket.getVertices().clear();
        this.animationPacket.getAnimations().clear();
        this.polygonPacket.clearPolygons();
        this.gridPacket.clear(); // The resized grid should be empty.
        this.gridPacket.resizeGrid(xTileCount, zTileCount); // Add two, so we can have a border surrounding the map.
        xTileCount = this.gridPacket.getGridXCount(); // Ensure we use what's actually been applied.
        zTileCount = this.gridPacket.getGridZCount();
        this.generalPacket.setStartGridCoordX((xTileCount / 2) + 1);
        this.generalPacket.setStartGridCoordZ(1);

        // Create stacks.
        Random random = new Random();
        int[] colorPalette = new int[zTileCount];
        int midIndex = (zTileCount / 2) + (zTileCount % 2);
        for (int i = 0; i < midIndex; i++)
            colorPalette[i] = random.nextInt(0x1000000);
        for (int i = midIndex; i < colorPalette.length; i++)
            colorPalette[i] = colorPalette[colorPalette.length - i - 1];

        short xSize = this.gridPacket.getGridXSize();
        short zSize = this.gridPacket.getGridZSize();
        short baseX = this.gridPacket.getBaseGridX();
        short baseZ = this.gridPacket.getBaseGridZ();
        for (int z = 0; z <= this.gridPacket.getGridZCount(); z++) {
            for (int x = 0; x <= this.gridPacket.getGridXCount(); x++) {
                int bottomLeftIndex = this.vertexPacket.getVertices().size();
                this.vertexPacket.getVertices().add(new SVector(baseX + x * xSize, 0, baseZ + z * zSize));
                int bottomRightIndex = bottomLeftIndex + 1;
                int topLeftIndex = bottomLeftIndex + xTileCount + 1;
                int topRightIndex = bottomRightIndex + xTileCount + 1;

                // Add polygon unless we're generating the final row of vertices.
                if (x < this.gridPacket.getGridXCount() && z < this.gridPacket.getGridZCount()) {
                    FroggerGridStack gridStack = this.gridPacket.getGridStack(x, z);
                    gridStack.getGridSquares().clear();
                    gridStack.setCliffHeight(0F);

                    FroggerMapPolygon polyF4 = new FroggerMapPolygon(this, FroggerMapPolygonType.F4);
                    polyF4.setVisible(true);

                    int colorIndex = (z + Math.abs((x - (xTileCount / 2)) / 4)) % colorPalette.length;
                    polyF4.getColors()[0].fromCRGB(0xFF000000 | colorPalette[colorIndex]);
                    polyF4.getVertices()[0] = topLeftIndex;
                    polyF4.getVertices()[1] = topRightIndex;
                    polyF4.getVertices()[2] = bottomLeftIndex;
                    polyF4.getVertices()[3] = bottomRightIndex;
                    this.polygonPacket.addPolygon(polyF4);

                    FroggerGridSquare newSquare = new FroggerGridSquare(gridStack, polyF4);
                    newSquare.setReaction(FroggerGridSquareReaction.SAFE);
                    gridStack.getGridSquares().add(newSquare);
                }
            }
        }

        // Add Lights (Makes sure models get colored in):
        FroggerMapLight light1 = new FroggerMapLight(this, MRLightType.PARALLEL);
        light1.setColor(ColorUtils.toBGR(Color.WHITE));
        light1.getDirection().setValues(-140.375F, 208.375F, 48.125F);
        this.lightPacket.getLights().add(light1);

        FroggerMapLight light2 = new FroggerMapLight(this, MRLightType.AMBIENT);
        light2.setColor(ColorUtils.toBGR(ColorUtils.fromRGB(0x494949)));
        this.lightPacket.getLights().add(light2);

        // Setup Group:
        this.groupPacket.generateMapGroups(ProblemResponse.CREATE_POPUP, true);
        getLogger().info("Cleared the map, its new size is %dx%d.", xTileCount, zTileCount);
    }

    /**
     * Calculates a bit array where the bits set indicate an unused vertex.
     * @return unusedVertexBitArray
     */
    public IndexBitArray findUnusedVertexIds() {
        IndexBitArray bitArray = new IndexBitArray();
        bitArray.setBits(0, getVertexPacket().getVertices().size(), true);

        // Each vertex seen in the polygon should remove the bit.
        List<FroggerMapPolygon> polygons = getPolygonPacket().getPolygons();
        for (int i = 0; i < polygons.size(); i++) {
            FroggerMapPolygon polygon = polygons.get(i);
            for (int j = 0; j < polygon.getVertexCount(); j++)
                bitArray.setBit(polygon.getVertices()[j], false);
        }

        return bitArray;
    }

    /**
     * Get a list of all unused vertices in the map.
     * @return unusedVerticeList
     */
    public List<SVector> findUnusedVertices() {
        // Set all used vertices to null.
        List<SVector> vertices = new ArrayList<>(getVertexPacket().getVertices());
        List<FroggerMapPolygon> polygons = getPolygonPacket().getPolygons();
        for (int i = 0; i < polygons.size(); i++) {
            FroggerMapPolygon polygon = polygons.get(i);
            for (int j = 0; j < polygon.getVertexCount(); j++)
                vertices.set(polygon.getVertices()[j], null);
        }

        vertices.removeIf(Objects::isNull); // Remove unused vertices.
        return vertices;
    }

    /**
     * Test if this is the island map, but not a placeholder map.
     */
    public boolean isIsland() {
        return "ISLAND.MAP".equals(getFileDisplayName());
    }

    /**
     * Test if this is the island map, or an island placeholder map.
     */
    public boolean isIslandOrIslandPlaceholder() {
        return isIsland() || getMapConfig().isIslandPlaceholder();
    }

    /**
     * Test if this is the ancient QB.MAP
     */
    public boolean isQB() {
        return "QB.MAP".equals(getFileDisplayName());
    }

    /**
     * Returns true iff this map appears to be an extremely early map format.
     * This format is from April/May 1997.
     */
    public boolean isExtremelyEarlyMapFormat() {
        return getConfig().isSonyPresentation() || isQB() || isIslandOrIslandPlaceholder();
    }

    /**
     * Returns true iff this map appears to be an early map format.
     * This format is from June 1997.
     */
    public boolean isEarlyMapFormat() {
        return getConfig().isAtOrBeforeBuild1() || isQB() || isIslandOrIslandPlaceholder();
    }

    /**
     * Gets the hardcoded map level ID for the level, if one exists.
     */
    public FroggerMapLevelID getMapLevelID() {
        return FroggerMapLevelID.getByName(getFileDisplayName());
    }
}