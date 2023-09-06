package net.highwayfrogs.editor.file.map;

import javafx.scene.control.Alert.AlertType;
import lombok.Cleanup;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.file.map.animation.MAPAnimation;
import net.highwayfrogs.editor.file.map.animation.MAPAnimationType;
import net.highwayfrogs.editor.file.map.animation.MAPUVInfo;
import net.highwayfrogs.editor.file.map.grid.GridSquare;
import net.highwayfrogs.editor.file.map.grid.GridStack;
import net.highwayfrogs.editor.file.map.light.APILightType;
import net.highwayfrogs.editor.file.map.light.Light;
import net.highwayfrogs.editor.file.map.poly.polygon.*;
import net.highwayfrogs.editor.file.standard.SVector;
import net.highwayfrogs.editor.file.standard.psx.ByteUV;
import net.highwayfrogs.editor.file.standard.psx.PSXColorVector;
import net.highwayfrogs.editor.file.vlo.GameImage;
import net.highwayfrogs.editor.file.vlo.ImageFilterSettings;
import net.highwayfrogs.editor.file.vlo.ImageFilterSettings.ImageState;
import net.highwayfrogs.editor.utils.Utils;

import javax.imageio.ImageIO;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

/**
 * FFSUtil is a utility class used for conversion to and from the FFS file format.
 * The FFS file format stands for two things:
 * - Frogger File Share
 * - For Fucks Sakes it's impossible to find a file format that has everyone Frogger needs and a Java library or is simple enough we could implement it in Java.
 * This format is used to allow editing Frogger maps in blender, and then re-importing them into FrogLord.
 * Created on 11/5/2019 by Kneesnap.
 */
public class FFSUtil {
    private static final ImageFilterSettings FFS_EXPORT_FILTER = new ImageFilterSettings(ImageState.EXPORT)
            .setAllowTransparency(true);

    /**
     * Save map data to a .ffs file for editing in Blender.
     * @param map       The map to export.
     * @param outputDir The directory to write to.
     */
    public static void saveMapAsFFS(MAPFile map, File outputDir) throws IOException {
        if (outputDir == null)
            return;

        List<Short> remapTable = map.getRemapTable();
        if (remapTable == null) {
            Utils.makePopUp("No remap could be found for this level, so unfortunately this cannot be exported to ffs.", AlertType.INFORMATION);
            return;
        }

        if (!outputDir.isDirectory()) {
            Utils.makePopUp("This is not a directory!", AlertType.ERROR);
            return;
        }

        @Cleanup PrintWriter pw = new PrintWriter(new File(outputDir, Utils.stripExtension(map.getFileEntry().getDisplayName()) + ".ffs"));

        // Export textures.
        if (map.getVlo() != null) {
            for (int i = 0; i < map.getVlo().getImages().size(); i++) {
                GameImage image = map.getVlo().getImages().get(i);
                if (remapTable.contains(image.getTextureId())) {
                    ImageIO.write(image.toBufferedImage(FFS_EXPORT_FILTER), "png", new File(outputDir, image.getTextureId() + ".png"));
                    pw.write("tex " + image.getTextureId() + " " + ((image.getFullWidth() - image.getIngameWidth()) / 2) + " " + ((image.getFullHeight() - image.getIngameHeight()) / 2) + Constants.NEWLINE);
                }
            }

            pw.write(Constants.NEWLINE);
        }

        // Grab polygons and vertices
        List<MAPPolygon> polygons = map.getAllPolygonsSafe();
        List<SVector> allVertices = map.getVertexes();

        // Write Vertices.
        for (SVector vec : allVertices) {
            final float x = vec.getFloatX();
            final float y = vec.getFloatY();
            final float z = vec.getFloatZ();
            pw.write("v " + x + " " + -y + " " + z + Constants.NEWLINE);
        }
        pw.write(Constants.NEWLINE);

        // Faces:
        for (MAPPolygon polygon : polygons) {
            pw.write(polygon.getType().name().toLowerCase() + " " + (polygon.isAllowDisplay() ? "show" : "hide") + " ");

            // [AE] Note face winding (vertex index ordering)
            for (int i = 0; i < polygon.getVerticeCount(); i++)
                pw.write(polygon.getVertices()[i] + " ");

            if (polygon instanceof MAPPolyFlat) {
                pw.write(String.valueOf(((MAPPolyFlat) polygon).getColor().toRGB()));
            } else if (polygon instanceof MAPPolyGouraud) {
                MAPPolyGouraud polyGouraud = (MAPPolyGouraud) polygon;
                for (int i = 0; i < polyGouraud.getColors().length; i++)
                    pw.write((i > 0 ? " " : "") + polyGouraud.getColors()[i].toRGB());
            } else if (polygon instanceof MAPPolyTexture) {
                MAPPolyTexture polyTex = (MAPPolyTexture) polygon;
                pw.write(polyTex.getFlags() + " ");
                pw.write(String.valueOf(remapTable.get(polyTex.getTextureId())));

                for (int i = 0; i < polyTex.getColors().length; i++) {
                    PSXColorVector color = polyTex.getColors()[i];
                    pw.write(" " + color.toFullRGB());
                }

                for (int i = 0; i < polyTex.getUvs().length; i++) {
                    ByteUV uv = polyTex.getUvs()[i];
                    float u = uv.getFloatU();
                    float v = uv.getFloatV();

                    pw.write(" " + u + ":" + v);
                }
            } else {
                throw new RuntimeException("Unknown polygon-type: " + polygon);
            }

            pw.write(Constants.NEWLINE);
        }
        pw.write(Constants.NEWLINE);

        // Flat:
        // f3 show v1 v2 v3 color
        // f4 show v1 v2 v3 v4 color

        // Gouraud:
        // g3 show v1 v2 v3 color1 color2 color3
        // g4 show v1 v2 v3 v4 color1 color2 color3 color4

        // Textured:
        // ft3 show v1 v2 v3 flags texture color uv1 uv2 uv3
        // ft4 show v1 v2 v3 v4 flags texture color uv1 uv2 uv3 uv4
        // gt3 show v1 v2 v3 flags texture color1 color2 color3 uv1 uv2 uv3
        // gt4 show v1 v2 v3 v4 flags texture color1 color2 color3 color4 uv1 uv2 uv3 uv4

        // Map Group Data.
        pw.write("group-count-x-z " + map.getGroupCount() + " " + map.getGroupXCount() + " " + map.getGroupZCount() + Constants.NEWLINE);
        pw.write("group-size-x-z " + map.getGroupXSize() + " " + map.getGroupZSize() + Constants.NEWLINE);
        pw.write(Constants.NEWLINE);

        // Map Base Data.
        pw.write("base-grid-x-z " + map.getBaseGridX() + " " + map.getBaseGridZ() + Constants.NEWLINE);
        pw.write("base-point-world-x-z " + map.getBasePointWorldX() + " " + map.getBasePointWorldZ() + Constants.NEWLINE);
        pw.write("base-tile-x-z " + map.getBaseXTile() + " " + map.getBaseZTile() + Constants.NEWLINE);
        pw.write(Constants.NEWLINE);

        // Grid Data.
        pw.write("grid-size " + map.getGridXCount() + " " + map.getGridZCount() + Constants.NEWLINE);
        for (GridStack stack : map.getGridStacks()) {
            pw.write("grid " + stack.getHeight());
            for (GridSquare square : stack.getGridSquares())
                pw.write(" " + polygons.indexOf(square.getPolygon()) + ":" + square.getFlags());
            pw.write(Constants.NEWLINE);
        }
        pw.write(Constants.NEWLINE);

        // Light Data.
        for (Light light : map.getLights()) {
            pw.write("light ");
            pw.write(light.getApiType().name());
            pw.write(" ");
            pw.write(String.valueOf(light.getPriority()));
            pw.write(" ");
            pw.write(String.valueOf(light.getParentId()));
            pw.write(" ");
            pw.write(String.valueOf(light.getColor()));
            pw.write(" ");
            pw.write(light.getPosition().getFloatX(12) + "," + light.getPosition().getFloatY(12) + "," + light.getPosition().getFloatZ(12));
            pw.write(" ");
            pw.write(light.getDirection().getFloatX(12) + "," + light.getDirection().getFloatY(12) + "," + light.getDirection().getFloatZ(12));
            pw.write(" ");
            pw.write(String.valueOf(light.getAttribute1()));
            pw.write(" ");
            pw.write(String.valueOf(light.getAttribute1()));
            pw.write(Constants.NEWLINE);
        }
        if (map.getLights().size() > 0)
            pw.write(Constants.NEWLINE);

        // Animation Data.
        for (MAPAnimation mapAnim : map.getMapAnimations()) {
            pw.write("anim " + mapAnim.getType().name() + " " + mapAnim.getUChange() + " " + mapAnim.getVChange() + " " + mapAnim.getUvFrameCount() + " " + mapAnim.getTexFrameDuration() + " ");

            // Write Textures.
            for (int i = 0; i < mapAnim.getTextures().size(); i++)
                pw.write((i > 0 ? "," : "") + remapTable.get(mapAnim.getTextures().get(i)));
            if (mapAnim.getTextures().size() == 0)
                pw.write("-");
            pw.write(" ");

            // Write the faces that this animation applies to.
            for (int i = 0; i < mapAnim.getMapUVs().size(); i++)
                pw.write((i > 0 ? "," : "") + polygons.indexOf(mapAnim.getMapUVs().get(i).getPolygon()));
            if (mapAnim.getMapUVs().size() == 0)
                pw.write("-");

            pw.write(Constants.NEWLINE);
        }
        pw.write(Constants.NEWLINE);

        // Close the file stream.
        pw.close();
        System.out.println("Exported " + map.getFileEntry().getDisplayName() + " to " + outputDir.getName() + File.separator + ".");
    }

    /**
     * Read map data from a FFS file.
     * @param map       The map to export.
     * @param inputFile The file to read from.
     */
    @SuppressWarnings("StatementWithEmptyBody")
    public static void importFFSToMap(MAPFile map, File inputFile) throws IOException {
        if (inputFile == null)
            return;

        List<Short> remapTable = map.getRemapTable();
        if (remapTable == null) {
            Utils.makePopUp("No remap could be found for this level, so unfortunately this cannot be imported from ffs.", AlertType.INFORMATION);
            return;
        }

        map.getLoadPointerPolygonMap().clear();
        map.getPolygons().values().forEach(List::clear);
        map.getGridStacks().clear();
        map.getMapAnimations().clear();
        map.getVertexes().clear();
        map.getLights().clear();

        List<String> lines = Files.readAllLines(inputFile.toPath());

        int gridId = 0;
        List<MAPPolygon> fullPolygonList = new ArrayList<>();
        for (String line : lines) {
            if (line.isEmpty() || line.equals(" "))
                continue;

            String[] args = line.split(" ");
            if (args.length <= 1)
                continue;

            String action = args[0];
            if (action.equalsIgnoreCase("v")) {
                // Read vertex.
                // v <x> <y> <z>
                map.getVertexes().add(new SVector(Float.parseFloat(args[1]), -Float.parseFloat(args[2]), Float.parseFloat(args[3])));
            } else if (action.equalsIgnoreCase("group-count-x-z")) {
                // group-count-x-z groupCount groupXCount groupZCount
                map.setGroupXCount(Short.parseShort(args[2]));
                map.setGroupZCount(Short.parseShort(args[3]));
            } else if (action.equalsIgnoreCase("group-size-x-z")) {
                // group-size-x-z <xSize> <zSize>
                map.setGroupXSize(Short.parseShort(args[1]));
                map.setGroupZSize(Short.parseShort(args[2]));
            } else if (action.equalsIgnoreCase("base-grid-x-z")) {
                // Automatically calculated.
            } else if (action.equalsIgnoreCase("base-point-world-x-z")) {
                // Automatically generated.
            } else if (action.equalsIgnoreCase("base-tile-x-z")) {
                // base-tile-x-z <x> <z>
                map.setBaseXTile(Short.parseShort(args[1]));
                map.setBaseZTile(Short.parseShort(args[2]));
            } else if (action.equalsIgnoreCase("grid-size")) {
                map.setGridXCount(Short.parseShort(args[1]));
                map.setGridZCount(Short.parseShort(args[2]));

                gridId = 0;
                map.getGridStacks().clear();
                for (int i = 0; i < map.getGridXCount() * map.getGridZCount(); i++)
                    map.getGridStacks().add(new GridStack());
            } else if (action.equalsIgnoreCase("grid")) {
                if (gridId >= map.getGridStacks().size())
                    throw new RuntimeException("There are more than " + map.getGridStacks().size() + " stacks in the .ffs file!");

                GridStack stack = map.getGridStacks().get(gridId++);
                stack.setHeight(Integer.parseInt(args[1]));
                for (int i = 2; i < args.length; i++) {
                    String[] split = args[i].split(":");
                    stack.getGridSquares().add(new GridSquare(fullPolygonList.get(Integer.parseInt(split[0])), map, Integer.parseInt(split[1])));
                }
            } else if (action.equalsIgnoreCase("tex")) {
                // Do nothing.
            } else if (action.equalsIgnoreCase("anim")) {
                MAPAnimation mapAnim = new MAPAnimation(map);
                mapAnim.setType(MAPAnimationType.valueOf(args[1].toUpperCase()));
                mapAnim.setUChange(Short.parseShort(args[2]));
                mapAnim.setVChange(Short.parseShort(args[3]));
                mapAnim.setUvFrameCount(Short.parseShort(args[4]));
                mapAnim.setTexFrameDuration(Short.parseShort(args[5]));

                if (!args[6].equalsIgnoreCase("-")) {
                    String[] texSplit = args[6].split(",");
                    for (int i = 0; i < texSplit.length; i++)
                        mapAnim.getTextures().add((short) remapTable.indexOf(Short.parseShort(texSplit[i])));
                }

                if (args.length > 7 && !args[7].equalsIgnoreCase("-")) {
                    String[] faceSplit = args[7].split(",");
                    for (int i = 0; i < faceSplit.length; i++)
                        mapAnim.getMapUVs().add(new MAPUVInfo(map, fullPolygonList.get(Integer.parseInt(faceSplit[i]))));
                }

                map.getMapAnimations().add(mapAnim);
            } else if (action.equalsIgnoreCase("light")) {
                Light mapLight = new Light();
                mapLight.setApiType(APILightType.valueOf(args[1].toUpperCase()));
                mapLight.setPriority(Short.parseShort(args[2]));
                mapLight.setParentId(Integer.parseInt(args[3]));
                mapLight.setColor(Integer.parseInt(args[4]));

                String[] splitPos = args[5].split(",");
                mapLight.getPosition().setValues(Float.parseFloat(splitPos[0]), Float.parseFloat(splitPos[1]), Float.parseFloat(splitPos[2]), 12);

                String[] splitDirection = args[6].split(",");
                mapLight.getDirection().setValues(Float.parseFloat(splitDirection[0]), Float.parseFloat(splitDirection[1]), Float.parseFloat(splitDirection[2]), 12);

                mapLight.setAttribute0(Integer.parseInt(args[7]));
                mapLight.setAttribute1(Integer.parseInt(args[8]));
                map.getLights().add(mapLight);
            } else {
                MAPPolygonType polyType;
                try {
                    polyType = MAPPolygonType.valueOf(action.toUpperCase());
                } catch (IllegalArgumentException iae) {
                    System.out.println("Unknown ffs marker '" + action + "'. Skipping.");
                    continue;
                }

                MAPPolygon newPolygon = polyType.getMaker().get();
                newPolygon.setMapFile(map);
                newPolygon.setAllowDisplay(args[1].equalsIgnoreCase("show"));

                int index = 2;

                // [AE] Note face winding (vertex index ordering)
                for (int j = 0; j < newPolygon.getVerticeCount(); j++, index++)
                    newPolygon.getVertices()[j] = Integer.parseInt(args[index]);

                if (newPolygon instanceof MAPPolyFlat) {
                    ((MAPPolyFlat) newPolygon).setColor(PSXColorVector.makeColorFromRGB(Integer.parseInt(args[index])));
                } else if (newPolygon instanceof MAPPolyGouraud) {
                    MAPPolyGouraud polyGouraud = (MAPPolyGouraud) newPolygon;
                    for (int j = 0; j < polyGouraud.getColors().length; j++, index++)
                        polyGouraud.getColors()[j] = PSXColorVector.makeColorFromRGB(Integer.parseInt(args[index]));
                } else if (newPolygon instanceof MAPPolyTexture) {
                    MAPPolyTexture polyTexture = (MAPPolyTexture) newPolygon;
                    polyTexture.setFlags(Short.parseShort(args[index++]));
                    polyTexture.setTextureId((short) remapTable.indexOf(Short.parseShort(args[index++])));

                    for (int j = 0; j < polyTexture.getColors().length; j++, index++)
                        polyTexture.getColors()[j] = PSXColorVector.makeColorFromFullRGB(Integer.parseInt(args[index]));

                    for (int j = 0; j < polyTexture.getUvs().length; j++, index++) {
                        String[] split = args[index].split(":");

                        float u = Float.parseFloat(split[0]);
                        float v = Float.parseFloat(split[1]);

                        polyTexture.getUvs()[j] = new ByteUV(u, v);
                    }
                } else {
                    throw new RuntimeException("Unknown polygon-type: " + newPolygon + ", " + polyType);
                }

                map.getPolygons().get(polyType).add(newPolygon);
                fullPolygonList.add(newPolygon);
            }
        }
        System.out.println("Imported " + inputFile.getName() + " as " + map.getFileEntry().getDisplayName() + ".");
    }
}