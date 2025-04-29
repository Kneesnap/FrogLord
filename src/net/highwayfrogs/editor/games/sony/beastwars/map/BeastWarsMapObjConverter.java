package net.highwayfrogs.editor.games.sony.beastwars.map;

import javafx.scene.control.Alert.AlertType;
import net.highwayfrogs.editor.FrogLordApplication;
import net.highwayfrogs.editor.file.map.view.UnknownTextureSource;
import net.highwayfrogs.editor.games.sony.beastwars.BeastWarsTexFile;
import net.highwayfrogs.editor.games.sony.beastwars.map.data.BeastWarsMapCollprim;
import net.highwayfrogs.editor.games.sony.beastwars.map.data.MapTextureInfoEntry;
import net.highwayfrogs.editor.games.sony.shared.collprim.MRCollprim.CollprimType;
import net.highwayfrogs.editor.utils.FXUtils;
import net.highwayfrogs.editor.utils.FileUtils;
import net.highwayfrogs.editor.utils.Utils;
import net.highwayfrogs.editor.utils.logging.ILogger;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Converts a Beast Wars map to a .obj file.
 * Created by Kneesnap on 9/22/2023.
 */
public class BeastWarsMapObjConverter {
    /**
     * Exports a Beast Wars map to a Wavefront obj file.
     * @param map The map file to export to obj.
     */
    public static void exportMapToObj(BeastWarsMapFile map) {
        String strippedName = FileUtils.stripExtension(map.getFileDisplayName());
        exportMapToObj(new File(FrogLordApplication.getWorkingDirectory(), strippedName), strippedName, map);
    }

    /**
     * Exports a Beast Wars map to a Wavefront obj file.
     * @param folder     The folder to export the map into. Should not contain anything else.
     * @param exportName The name to export as.
     * @param map        The map file to export to obj.
     */
    @SuppressWarnings("unchecked")
    public static void exportMapToObj(File folder, String exportName, BeastWarsMapFile map) {
        ILogger logger = map.getLogger();

        // Find the corresponding texture file.
        BeastWarsTexFile texFile = map.getTextureFile();
        if (texFile == null)
            FXUtils.makePopUp("Couldn't find the associated .TEX file. Exporting anyways.", AlertType.WARNING);

        // Create folder.
        FileUtils.makeDirectory(folder);

        // Setup textures by vertex list.
        List<Integer>[] verticesPerTexture = new ArrayList[BeastWarsTexFile.MAXIMUM_TEXTURE_COUNT + 1];
        for (int i = 0; i < verticesPerTexture.length; i++)
            verticesPerTexture[i] = new ArrayList<>();

        StringBuilder mtlWriter = new StringBuilder();
        StringBuilder objWriter = new StringBuilder();

        // Write material.
        objWriter.append("mtllib Map.mtl\n\n");

        // Write vertices.
        int vertexId = 1;
        short worldHeightScale = map.getWorldHeightScale();
        short heightMapXLength = map.getHeightMapXLength();
        short heightMapZLength = map.getHeightMapZLength();
        for (int z = 0; z < heightMapZLength; z++) {
            for (int x = 0; x < heightMapXLength; x++) {
                short y = map.getHeightMap()[z][x];
                short texture = map.getTileMap()[z][x];
                objWriter.append("v ").append((x - heightMapXLength / ((z > 0 || x > 0) ? 2 : 1)) << 7).append(' ').append(y << (worldHeightScale & 0x1F)).append(' ').append((z - (heightMapZLength / ((z > 0 || x > 0) ? 2 : 1))) << 7).append('\n');
                vertexId++;
                // TODO: The whole "snap first vertex to 0 0 0" thing should be tossed at some point.
                // TODO: Vertex.getWorldXYZ().

                // The last vertices in each direction don't create faces otherwise they would require vertices beyond them.
                // So, we can skip them.
                if (heightMapXLength - 1 > x && heightMapZLength - 1 > z) {
                    List<Integer> vertices = verticesPerTexture[texture];
                    vertices.add((z * heightMapXLength) + x + 1); // Add 1 because obj files start counting at 1 not zero.
                }
            }
        }

        // TODO: CREATE VERTICES FOR FART HEAD
        for (int i = 0; i < map.getCollprims().size(); i++) {
            BeastWarsMapCollprim collprim = map.getCollprims().get(i);
            if (collprim.getType() != CollprimType.CUBOID)
                continue;

            float minX = collprim.getOffset().getFloatX() - collprim.getXLength();
            float maxX = collprim.getOffset().getFloatX() + collprim.getXLength();
            float minY = collprim.getOffset().getFloatY() - collprim.getYLength() * 2;
            float maxY = collprim.getOffset().getFloatY() + collprim.getYLength() * 2;
            float minZ = collprim.getOffset().getFloatZ() - collprim.getZLength();
            float maxZ = collprim.getOffset().getFloatZ() + collprim.getZLength();

            objWriter.append("v ").append(minX).append(' ').append(minY).append(' ').append(minZ).append('\n');
            objWriter.append("v ").append(minX).append(' ').append(minY).append(' ').append(maxZ).append('\n');
            objWriter.append("v ").append(maxX).append(' ').append(minY).append(' ').append(minZ).append('\n');
            objWriter.append("v ").append(maxX).append(' ').append(minY).append(' ').append(maxZ).append('\n');
            objWriter.append("v ").append(minX).append(' ').append(maxY).append(' ').append(minZ).append('\n');
            objWriter.append("v ").append(minX).append(' ').append(maxY).append(' ').append(maxZ).append('\n');
            objWriter.append("v ").append(maxX).append(' ').append(maxY).append(' ').append(minZ).append('\n');
            objWriter.append("v ").append(maxX).append(' ').append(maxY).append(' ').append(maxZ).append('\n');
        }

        // Write texture coordinates.
        objWriter.append('\n');
        objWriter.append("vt 0.0 1.0\n"); // Bottom Left
        objWriter.append("vt 0.0 0.0\n"); // Top Left
        objWriter.append("vt 1.0 1.0\n"); // Bottom Right
        objWriter.append("vt 1.0 0.0\n"); // Top Right

        // Write faces grouped together.
        for (int i = 0; i < verticesPerTexture.length; i++)
            writeNewMaterialAndFaces(map, texFile, folder, objWriter, mtlWriter, verticesPerTexture, i);

        // TODO: TOSS
        // Create collprim geometry. (Temporary)
        try {
            ImageIO.write(UnknownTextureSource.MAGENTA_INSTANCE.makeTexture(null), "png", new File(folder, "collprim.png"));
        } catch (IOException ex) {
            Utils.handleError(logger, ex, false, "Failed to export collprim image.");
        }

        // Create and use a new material.
        mtlWriter.append("\nnewmtl TEX_ENTRY_COLLPRIM\n");
        mtlWriter.append("Kd 1 1 1\n");
        mtlWriter.append("map_Kd collprim.png\n");

        // Write material switch to use the new material.
        objWriter.append('\n');
        objWriter.append("usemtl TEX_ENTRY_COLLPRIM\n");

        for (int i = 0; i < map.getCollprims().size(); i++) {
            BeastWarsMapCollprim collprim = map.getCollprims().get(i);
            if (collprim.getType() != CollprimType.CUBOID)
                continue;

            int localVertexId = vertexId + (8 * i);
            objWriter.append("f ").append(localVertexId).append("/4 ").append(localVertexId + 1).append("/3 ").append(localVertexId + 3).append("/1\n"); // Bottom Tri 1
            objWriter.append("f ").append(localVertexId).append("/4 ").append(localVertexId + 3).append("/1 ").append(localVertexId + 2).append("/2\n"); // Bottom Tri 2
            objWriter.append("f ").append(localVertexId + 4).append("/4 ").append(localVertexId + 5).append("/3 ").append(localVertexId + 7).append("/1\n"); // Top Tri 1
            objWriter.append("f ").append(localVertexId + 4).append("/4 ").append(localVertexId + 7).append("/1 ").append(localVertexId + 6).append("/2\n"); // Top Tri 2
            objWriter.append("f ").append(localVertexId).append("/4 ").append(localVertexId + 4).append("/3 ").append(localVertexId + 6).append("/1\n"); // Side1 Tri 1
            objWriter.append("f ").append(localVertexId).append("/4 ").append(localVertexId + 6).append("/1 ").append(localVertexId + 2).append("/2\n"); // Side1 Tri 2
            objWriter.append("f ").append(localVertexId + 3).append("/4 ").append(localVertexId + 7).append("/3 ").append(localVertexId + 5).append("/1\n"); // Side2 Tri 1
            objWriter.append("f ").append(localVertexId + 3).append("/4 ").append(localVertexId + 5).append("/1 ").append(localVertexId + 1).append("/2\n"); // Side2 Tri 2
        }

        // Save files.
        try {
            Files.write(new File(folder, "Map.obj").toPath(), Arrays.asList(objWriter.toString().split("\n")));
            Files.write(new File(folder, "Map.mtl").toPath(), Arrays.asList(mtlWriter.toString().split("\n")));
            logger.info("Exported '" + map.getFileDisplayName() + " to " + exportName + "/Map.obj");
        } catch (IOException ex) {
            throw new RuntimeException("Failed to export " + map.getFileDisplayName() + " to " + exportName + "/Map.obj", ex);
        }

        // TODO: TOSS (Data seen in MS1_P_01.MAP)
        for (int i = 0; i < map.getTextureInfoEntries().length; i++) {
            MapTextureInfoEntry infoEntry = map.getTextureInfoEntries()[i];
            if (infoEntry.isActive() && infoEntry.getFlags() > 0)
                logger.info("Texture Entry #" + i + ": " + infoEntry.getTextureId() + ", " + infoEntry.getFlags());
        }
    }

    private static void writeNewMaterialAndFaces(BeastWarsMapFile map, BeastWarsTexFile texFile, File folder, StringBuilder objWriter, StringBuilder mtlWriter, List<Integer>[] verticesPerTexture, int textureEntryIndex) {
        List<Integer> vertices = verticesPerTexture[textureEntryIndex];
        if (vertices == null || vertices.isEmpty())
            return;

        MapTextureInfoEntry textureEntry = map.getTextureInfoEntries()[textureEntryIndex];
        boolean hasEntry = textureEntry != null && textureEntry.isActive();

        // Export texture file.
        boolean hasImage = hasEntry && texFile != null && texFile.getImages().size() > textureEntry.getTextureId();
        BufferedImage image = hasImage ? texFile.getImages().get(textureEntry.getTextureId()).getImage() : null;
        String imageFileName = null;

        try {
            if (image != null) {
                imageFileName = String.valueOf(textureEntryIndex);
            } else if (textureEntryIndex == BeastWarsMapFile.TEXTURE_ID_NO_TEXTURE) {
                imageFileName = "disabled";
                image = UnknownTextureSource.CYAN_INSTANCE.makeTexture(null);
            } else {
                imageFileName = "missing";
                image = UnknownTextureSource.MAGENTA_INSTANCE.makeTexture(null);
            }

            if (image != null)
                ImageIO.write(image, "png", new File(folder, imageFileName + ".png"));
        } catch (IOException ex) {
            Utils.handleError(map.getLogger(), ex, false, "Failed to export image %d.", textureEntryIndex);
        }

        // Create and use a new material.
        mtlWriter.append("\nnewmtl TEX_ENTRY_").append(textureEntryIndex).append('\n');
        if (hasEntry && textureEntry.getFlags() > 0)
            mtlWriter.append("# Flags: ").append(textureEntry.getFlags()).append('\n');

        mtlWriter.append("Kd 1 1 1\n");
        mtlWriter.append("map_Kd ").append(imageFileName).append(".png\n");

        // Write material switch to use the new material.
        objWriter.append('\n');
        objWriter.append("usemtl TEX_ENTRY_").append(textureEntryIndex).append('\n');

        // Write faces.
        for (int j = 0; j < vertices.size(); j++) {
            int vtxBottomLeft = vertices.get(j);
            int vtxBottomRight = vtxBottomLeft + 1;
            int vtxTopLeft = vtxBottomLeft + map.getHeightMapXLength();
            int vtxTopRight = vtxTopLeft + 1;

            objWriter.append("f ").append(vtxBottomLeft).append("/4 ").append(vtxTopLeft).append("/3 ").append(vtxTopRight).append("/1\n");
            objWriter.append("f ").append(vtxBottomLeft).append("/4 ").append(vtxTopRight).append("/1 ").append(vtxBottomRight).append("/2\n");
        }
    }
}