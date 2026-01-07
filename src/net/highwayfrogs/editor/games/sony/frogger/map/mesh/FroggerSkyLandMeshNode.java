package net.highwayfrogs.editor.games.sony.frogger.map.mesh;

import net.highwayfrogs.editor.games.psx.math.vector.SVector;
import net.highwayfrogs.editor.games.psx.math.vector.CVector;
import net.highwayfrogs.editor.games.psx.shading.PSXTextureShader;
import net.highwayfrogs.editor.games.sony.frogger.file.FroggerSkyLand;
import net.highwayfrogs.editor.games.sony.frogger.file.FroggerSkyLand.SkyLandTile;
import net.highwayfrogs.editor.games.sony.frogger.map.FroggerMapFile;
import net.highwayfrogs.editor.games.sony.frogger.map.FroggerMapLevelID;
import net.highwayfrogs.editor.games.sony.shared.mesh.SCPolygonAdapterNode;
import net.highwayfrogs.editor.system.math.Vector2f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Displays the sky land mesh under the main map mesh.
 * Created by Kneesnap on 4/24/2025.
 */
public class FroggerSkyLandMeshNode extends SCPolygonAdapterNode<FroggerMapPolygon> {
    private final Map<SkyLandTile, FroggerMapPolygon> polygonsByTile;
    private static final float SKY_LAND_GRID_LENGTH = 64F; // 0x400
    private static final float SKY_LAND_HEIGHT = 384F; // 0x1800

    // In build 31 and older, all colors were made with this color.
    // Build 32/33 seem to use a different method color source, but I'm not sure yet what precisely, and it's not very interesting.
    private static final CVector SKY_LAND_DEFAULT_COLOR_PRE_BUILD_34 = PSXTextureShader.UNSHADED_COLOR;

    // Starting with Build 34, these colors are used.
    private static final CVector[] SKY_LAND_BASE_COLORS = {
            CVector.makeColorFromRGB(0x606060), // SKY1 Looney Balloons
            CVector.makeColorFromRGB(0x102030), // SKY2 Airshow Antics
            CVector.makeColorFromRGB(0x706040), // SKY3 Loonier Balloons
            CVector.makeColorFromRGB(0x704030), // SKY4 Time Flies
    };

    public FroggerSkyLandMeshNode(FroggerMapMesh mapMesh, Map<SkyLandTile, FroggerMapPolygon> polygonsByTile) {
        super(mapMesh);
        this.polygonsByTile = polygonsByTile;
    }

    @Override
    public FroggerMapMesh getMesh() {
        return (FroggerMapMesh) super.getMesh();
    }

    @Override
    protected void onAddedToMesh() {
        super.onAddedToMesh();
        FroggerSkyLand skyLand = getMesh().getMap().getGameInstance().getSkyLand();

        // Setup polygons.
        // First, setup the non-transparent polygons.
        for (int y = 0; y < skyLand.getYLength(); y++)
            for (int x = 0; x < skyLand.getXLength(); x++)
                this.add(this.polygonsByTile.get(skyLand.getTile(x, y)));
    }

    @Override
    protected boolean getPolygonTextureCoordinate(FroggerMapPolygon polygon, int index, Vector2f result) {
        if (!polygon.getPolygonType().isTextured())
            return false;

        polygon.getTextureUvs()[index].toVector(result);
        return true;
    }

    @Override
    public List<SVector> getAllVertices() {
        FroggerSkyLand skyLand = getMesh().getMap().getGameInstance().getSkyLand();
        List<SVector> vertices = new ArrayList<>();
        float baseX = skyLand.getXLength() * SKY_LAND_GRID_LENGTH / 2;
        float baseY = skyLand.getYLength() * SKY_LAND_GRID_LENGTH / 2;
        for (int y = 0; y <= skyLand.getYLength(); y++)
            for (int x = 0; x <= skyLand.getXLength(); x++)
                vertices.add(new SVector((x * SKY_LAND_GRID_LENGTH) - baseX, SKY_LAND_HEIGHT, (y * SKY_LAND_GRID_LENGTH) - baseY));

        return vertices;
    }

    @Override
    protected int[] getVertices(FroggerMapPolygon polygon) {
        return polygon.getVertices();
    }

    @Override
    protected int getVertexCount(FroggerMapPolygon polygon) {
        return polygon.getVertexCount();
    }

    /**
     * Returns true iff sky land should be displayed for a particular level
     * @param mapFile the map to potentially display sky land for
     * @return if sky land should be displayed
     */
    public static boolean shouldDisplaySkyLandFor(FroggerMapFile mapFile) {
        return getSkyLandID(mapFile) >= 0 && mapFile.getGameInstance().getSkyLand() != null;
    }

    /**
     * Gets the index to use for sky land display.
     * @param mapFile the map file to get the sky land ID for
     * @return skyLandID, -1 means no sky land
     */
    private static int getSkyLandID(FroggerMapFile mapFile) {
        if (mapFile == null)
            return -1;

        FroggerMapLevelID level = mapFile.getMapLevelID();
        if (level == null)
            return -1;

        switch (level) {
            case SKY1:
                return 0;
            case SKY2:
                return 1;
            case SKY3:
                return 2;
            case SKY4:
                return 3;
            default:
                return -1;
        }
    }

    private static FroggerMapPolygon createPolygon(FroggerMapFile mapFile, FroggerSkyLand skyLand, SkyLandTile skyLandTile, int skyLandLevelIndex) {
        FroggerMapPolygon newPolygon = new FroggerMapPolygon(mapFile, FroggerMapPolygonType.FT4);
        newPolygon.setVisible(true);
        newPolygon.setSkyLand(true);

        // Alternatively, debug the tiles by applying a color based on the rotation.  [SKY_LAND_DEFAULT_COLOR_PRE_BUILD_34, 0x301010, 0x103010, 0x102030]
        if (mapFile.getConfig().isAtOrBeforeBuild33()) {
            newPolygon.getColors()[0].copyFrom(SKY_LAND_DEFAULT_COLOR_PRE_BUILD_34);
        } else {
            newPolygon.getColors()[0].copyFrom(SKY_LAND_BASE_COLORS[skyLandLevelIndex]);
        }

        newPolygon.setTextureId(skyLandTile.getLocalTextureId());

        // Setup vertices.
        int bottomLeftVertex = (skyLandTile.getY() * (skyLand.getXLength() + 1)) + skyLandTile.getX();
        int bottomRightVertex = bottomLeftVertex + 1;
        int topLeftVertex = bottomLeftVertex + skyLand.getXLength() + 1;
        int topRightVertex = topLeftVertex + 1;
        newPolygon.getVertices()[0] = topLeftVertex;
        newPolygon.getVertices()[1] = topRightVertex;
        newPolygon.getVertices()[2] = bottomLeftVertex;
        newPolygon.getVertices()[3] = bottomRightVertex;

        // Setup texture UVs.
        switch (skyLandTile.getRotation()) {
            case MODE_1:
                newPolygon.getTextureUvs()[0].setFloatUV(0, 0);
                newPolygon.getTextureUvs()[1].setFloatUV(1, 0);
                newPolygon.getTextureUvs()[2].setFloatUV(0, 1);
                newPolygon.getTextureUvs()[3].setFloatUV(1, 1);
                break;
            case MODE_2:
                newPolygon.getTextureUvs()[2].setFloatUV(0, 0);
                newPolygon.getTextureUvs()[0].setFloatUV(1, 0);
                newPolygon.getTextureUvs()[3].setFloatUV(0, 1);
                newPolygon.getTextureUvs()[1].setFloatUV(1, 1);
                break;
            case MODE_3:
                newPolygon.getTextureUvs()[3].setFloatUV(0, 0);
                newPolygon.getTextureUvs()[2].setFloatUV(1, 0);
                newPolygon.getTextureUvs()[1].setFloatUV(0, 1);
                newPolygon.getTextureUvs()[0].setFloatUV(1, 1);
                break;
            case MODE_4:
                newPolygon.getTextureUvs()[1].setFloatUV(0, 0);
                newPolygon.getTextureUvs()[3].setFloatUV(1, 0);
                newPolygon.getTextureUvs()[0].setFloatUV(0, 1);
                newPolygon.getTextureUvs()[2].setFloatUV(1, 1);
                break;
            default:
                throw new UnsupportedOperationException("Unsupported Rotation: " + skyLandTile.getRotation());
        }

        return newPolygon;
    }

    /**
     * Adds the sky land shaded polygons to the shaded texture manager for a given map mesh.
     * @param mapMesh the map mesh to set up the polygon shading for
     */
    static Map<SkyLandTile, FroggerMapPolygon> addShadedPolygons(FroggerMapMesh mapMesh) {
        if (mapMesh == null)
            throw new NullPointerException("mapMesh");

        FroggerMapFile mapFile = mapMesh.getMap();
        if (!shouldDisplaySkyLandFor(mapFile))
            return null;

        FroggerSkyLand skyLand = mapFile.getGameInstance().getSkyLand();
        int skyLandLevelIndex = getSkyLandID(mapFile);

        // Setup polygons.
        // The polygons added here must be the same polygon objects used in the node (once created).
        Map<SkyLandTile, FroggerMapPolygon> results = new HashMap<>();
        for (int y = 0; y < skyLand.getYLength(); y++) {
            for (int x = 0; x < skyLand.getXLength(); x++) {
                SkyLandTile skyLandTile = skyLand.getTile(x, y);
                FroggerMapPolygon newPolygon = createPolygon(mapFile, skyLand, skyLandTile, skyLandLevelIndex);
                mapMesh.getShadedTextureManager().addPolygon(newPolygon);
                results.put(skyLandTile, newPolygon);
            }
        }

        return results;
    }
}
