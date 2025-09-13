package net.highwayfrogs.editor.games.sony.frogger.utils;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.file.standard.SVector;
import net.highwayfrogs.editor.file.vlo.GameImage;
import net.highwayfrogs.editor.file.vlo.ImageFilterSettings;
import net.highwayfrogs.editor.file.vlo.ImageFilterSettings.ImageState;
import net.highwayfrogs.editor.file.vlo.VLOArchive;
import net.highwayfrogs.editor.games.generic.GameConfig;
import net.highwayfrogs.editor.games.sony.SCGameType;
import net.highwayfrogs.editor.games.sony.frogger.FroggerConfig;
import net.highwayfrogs.editor.games.sony.frogger.map.FroggerMapFile;
import net.highwayfrogs.editor.games.sony.frogger.map.data.animation.FroggerMapAnimation;
import net.highwayfrogs.editor.games.sony.frogger.map.data.animation.FroggerMapAnimationTargetPolygon;
import net.highwayfrogs.editor.games.sony.frogger.map.data.grid.FroggerGridSquare;
import net.highwayfrogs.editor.games.sony.frogger.map.data.grid.FroggerGridStack;
import net.highwayfrogs.editor.games.sony.frogger.map.mesh.FroggerMapPolygon;
import net.highwayfrogs.editor.games.sony.frogger.map.mesh.FroggerMapPolygonType;
import net.highwayfrogs.editor.games.sony.frogger.map.packets.FroggerMapFilePacketGrid;
import net.highwayfrogs.editor.games.sony.frogger.map.packets.FroggerMapFilePacketPolygon;
import net.highwayfrogs.editor.games.sony.frogger.map.packets.FroggerMapFilePacketVertex;
import net.highwayfrogs.editor.games.sony.shared.SCByteTextureUV;
import net.highwayfrogs.editor.games.sony.shared.TextureRemapArray;
import net.highwayfrogs.editor.utils.FileUtils;
import net.highwayfrogs.editor.utils.Utils;
import net.highwayfrogs.editor.utils.Utils.ProblemResponse;

import javax.imageio.ImageIO;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import java.util.function.BiPredicate;
import java.util.logging.Level;

/**
 * FFSUtil is a utility class used for conversion to and from the FFS (Frogger File Sync) file format.
 * This format is used to allow editing Frogger maps in Blender, and then re-importing them into FrogLord.
 *
 * The FFS format is specified as a text-based format, based on commands similar to Wavefront obj.
 * The '#' character at the start of a line indicates a comment.
 * Anything else will have the full line of text interpreted as a command, with the first (whitespace separated) parameter treated as the command label.
 * All other parameters are treated as arguments to that command.
 *
 * Command List:
 *  version_ffs <id> Indicates which version of the format is used. Should be the first command in a file.
 *  version_game <version_name> Indicates which version of the game the FFS export corresponds to. Necessary for texture ID reasons.
 *  texture <image_id> Defines a new texture.
 *  vertex <x> <y> <z> Defines a new vertex at the given xyz position (in Frogger coordinate space.) The vertex is assigned an auto-incrementing ID, starting at 0.
 *  polygon <f3|f4|ft3|ft4|g3|g4|gt3|gt4> <show|hide> [textureId] [flagsNumber] <vertexIds[]...> [textureUvs[]...] <colors[]...> [gridFlags] Defines a map polygon.
 *
 * Created on 11/5/2019 by Kneesnap.
 */
public class FFSUtil {
    public static final String BLENDER_ADDON_FILE_NAME = "frogger-map-blender-plugin.py";

    private static final ImageFilterSettings FFS_EXPORT_FILTER = new ImageFilterSettings(ImageState.EXPORT)
            .setAllowTransparency(true)
            .setTrimEdges(true);

    // Increment this with any major change to the format.
    public static final int CURRENT_FORMAT_VERSION = 1;

    /**
     * Save map data to a .ffs file for editing in Blender.
     * @param map The map to export.
     * @param outputDir The directory to write to.
     * @param response The response to take to any problems seen.
     */
    public static void saveMapAsFFS(FroggerMapFile map, File outputDir, ProblemResponse response) {
        if (map == null)
            throw new NullPointerException("map");
        if (outputDir == null)
            throw new NullPointerException("outputDir");
        if (response == null)
            throw new NullPointerException("response");
        if (!outputDir.isDirectory() || !outputDir.exists())
            throw new IllegalArgumentException("The provided outputDir was not a real directory! (" + outputDir + ")");

        TextureRemapArray textureRemap = map.getTextureRemap();
        if (textureRemap == null) {
            Utils.handleProblem(response, map.getLogger(), Level.SEVERE, "There was no texture remap available for %s.", map.getFileDisplayName());
            return;
        }

        StringBuilder builder = new StringBuilder();
        builder.append("# FFS File Export -- By FrogLord ").append(Constants.VERSION).append('\n');
        builder.append("# Map: ").append(map.getFileDisplayName()).append('\n');
        builder.append("# Export Time: ").append(new Date()).append('\n');

        // Write format version.
        builder.append(FfsCommand.FFS_VERSION.getLabel()).append(' ').append(CURRENT_FORMAT_VERSION).append("\n");
        builder.append(FfsCommand.GAME_VERSION.getLabel()).append(' ').append(map.getGameInstance().getVersionConfig().getInternalName()).append("\n\n");

        // Export textures.
        VLOArchive mapVlo = map.getVloFile();
        for (int i = 0; i < textureRemap.getTextureIds().size(); i++) {
            GameImage image = textureRemap.resolveTexture(i, mapVlo);
            if (image == null) {
                Utils.handleProblem(response, map.getLogger(), Level.SEVERE, "Could not resolve texture remap index %d to a valid image!", i);
                return;
            }

            builder.append(FfsCommand.TEXTURE.getLabel())
                    .append(' ').append(image.getTextureId())
                    .append(Constants.NEWLINE);

            File imageFileOutput = new File(outputDir, image.getTextureId() + ".png");
            try {
                ImageIO.write(image.toBufferedImage(FFS_EXPORT_FILTER), "png", imageFileOutput);
            } catch (IOException ex) {
                throw new RuntimeException("Failed to save " + imageFileOutput.getName(), ex);
            }
        }

        if (textureRemap.getTextureIds().size() > 0)
            builder.append(Constants.NEWLINE);

        // Write Vertices.
        List<SVector> vertices = map.getVertexPacket().getVertices();
        for (int i = 0; i < vertices.size(); i++) {
            SVector vertex = vertices.get(i);
            builder.append(FfsCommand.VERTEX.getLabel()).append(' ')
                    .append(vertex.getFloatX()).append(' ')
                    .append(vertex.getFloatY()).append(' ')
                    .append(vertex.getFloatZ()).append(Constants.NEWLINE);
        }
        builder.append(Constants.NEWLINE);

        // Faces:
        List<FroggerMapPolygon> polygons = map.getPolygonPacket().getPolygons();

        // Track grid squares.
        FroggerMapFilePacketGrid gridPacket = map.getGridPacket();
        Map<FroggerMapPolygon, FroggerGridSquare> gridSquaresByPolygon = new HashMap<>();
        for (int z = 0; z < gridPacket.getGridZCount(); z++) {
            for (int x = 0; x < gridPacket.getGridXCount(); x++) {
                FroggerGridStack gridStack = gridPacket.getGridStack(x, z);
                for (int i = 0; i < gridStack.getGridSquares().size(); i++) {
                    FroggerGridSquare gridSquare = gridStack.getGridSquares().get(i);
                    if (gridSquare.getPolygon() != null)
                        gridSquaresByPolygon.put(gridSquare.getPolygon(), gridSquare);
                }
            }
        }

        for (FroggerMapPolygon polygon : polygons) {
            builder.append(FfsCommand.POLYGON.getLabel()).append(' ')
                    .append(polygon.getPolygonType().name().toLowerCase()).append(' ')
                    .append(polygon.isVisible() ? "show" : "hide");

            // Write Texture ID
            if (polygon.getPolygonType().isTextured())
                builder.append(' ').append(textureRemap.getRemappedTextureId(polygon.getTextureId()))
                        .append(' ').append(polygon.getFlags());

            // Write vertices.
            for (int i = 0; i < polygon.getVertexCount(); i++)
                builder.append(' ').append(polygon.getVertices()[i]);

            // Write Texture UVs.
            if (polygon.getPolygonType().isTextured()) {
                for (int i = 0; i < polygon.getVertexCount(); i++) {
                    SCByteTextureUV textureUv = polygon.getTextureUvs()[i];
                    builder.append(' ').append(textureUv.getFloatU()).append(':').append(textureUv.getFloatV());
                }
            }

            // Write colors.
            for (int i = 0; i < polygon.getColors().length; i++)
                builder.append(String.format(" %06X", polygon.getColors()[i].toRGB()));

            // Write grid square data.
            FroggerGridSquare gridSquare = gridSquaresByPolygon.get(polygon);
             if (gridSquare != null)
                 builder.append(' ').append(gridSquare.getFlags());

            builder.append(Constants.NEWLINE);
        }
        builder.append(Constants.NEWLINE);

        // Write the FFS file.
        File ffsOutputFile = new File(outputDir, FileUtils.stripExtension(map.getFileDisplayName()) + ".ffs");

        try {
            Files.write(ffsOutputFile.toPath(), builder.toString().getBytes(StandardCharsets.UTF_8));
            map.getLogger().info("Exported .ffs to %s.", outputDir.getName() + File.separator);
        } catch (IOException ex) {
            throw new RuntimeException("Failed to save " + ffsOutputFile.getName(), ex);
        }
    }

    /**
     * Read map data from a FFS file.
     * @param map       The map to export.
     * @param inputFile The file to read from.
     */
    public static void importFFSToMap(FroggerMapFile map, File inputFile, ProblemResponse response) {
        if (map == null)
            throw new NullPointerException("map");
        if (inputFile == null)
            throw new NullPointerException("inputFile");
        if (response == null)
            throw new NullPointerException("response");
        if (!inputFile.isFile() || !inputFile.exists())
            throw new IllegalArgumentException("The provided inputFile was not a real file! (" + inputFile + ")");

        TextureRemapArray textureRemap = map.getTextureRemap();
        if (textureRemap == null) {
            Utils.handleProblem(response, map.getLogger(), Level.SEVERE, "There was no texture remap available for %s.", map.getFileDisplayName());
            return;
        }

        List<String> lines;
        try {
            lines = Files.readAllLines(inputFile.toPath());
        } catch (IOException ex) {
            throw new RuntimeException("Failed to read " + inputFile.getName(), ex);
        }

        // Clear remap before running commands. (Commands must update remap.)
        List<Short> oldTextureIds = new ArrayList<>(textureRemap.getTextureIds());
        textureRemap.getTextureIds().clear();

        FfsLoadContext context = new FfsLoadContext(map, response);
        int lineNumber = 0;
        for (String line : lines) {
            line = line.trim();
            lineNumber++;
            if (line.isEmpty() || line.startsWith("#"))
                continue;

            String[] split = line.split("\\s+");
            FfsCommand command = FfsCommand.getCommandByLabel(split[0]);
            if (command == null) {
                context.handleProblem(Level.WARNING, "Skipping unrecognized command '%s' on line %d.", split[0], lineNumber);
                continue;
            }

            String[] args = split.length > 1 ? Arrays.copyOfRange(split, 1, split.length) : new String[0];
            if (!command.runCommand(context, args, lineNumber)) {
                textureRemap.getTextureIds().clear();
                textureRemap.getTextureIds().addAll(oldTextureIds);
                return; // Command failed.
            }
        }

        // Apply the loaded FFS data to the level.

        // 1) Store animation data for later, and clear existing polygons from it.
        Map<Short, FroggerMapAnimation> animationsByTexture = new HashMap<>();
        for (FroggerMapAnimation animation : map.getAnimationPacket().getAnimations()) {
            for (int i = 0; i < animation.getTargetPolygons().size(); i++) {
                FroggerMapAnimationTargetPolygon targetPolygonWrapper = animation.getTargetPolygons().get(i);
                FroggerMapPolygon targetPolygon = targetPolygonWrapper.getPolygon();
                targetPolygonWrapper.setPolygon(null); // Remove the polygon from tracking.
                if (targetPolygon != null && targetPolygon.getPolygonType().isTextured())
                    animationsByTexture.putIfAbsent(textureRemap.getRemappedTextureId(targetPolygon.getTextureId()), animation);
            }

            // Clear the animation targets.
            animation.getTargetPolygons().clear();
        }

        // 2) Validate texture remap.
        // This is necessary, as even changes done in FrogLord need to be reapplied after a restart.
        if (textureRemap.getTextureIds().size() > textureRemap.getTextureIdSlotsAvailable())
            context.handleProblem(Level.WARNING, "The texture remap for %s has room for %d textures, but %d were imported.\nThis will likely cause graphical corruption in-game!", map.getFileDisplayName(), textureRemap.getTextureIdSlotsAvailable(), textureRemap.getTextureIds().size());
        while (textureRemap.getTextureIdSlotsAvailable() > textureRemap.getTextureIds().size())
            textureRemap.getTextureIds().add((short) -1);

        // 3) Clear old vertex data, and add new polygon data.
        FroggerMapFilePacketVertex vertexPacket = map.getVertexPacket();
        vertexPacket.getVertices().clear();
        vertexPacket.getVertices().addAll(context.getNewVertices());

        // 4) Apply new polygon data, and prepare to calculate the size of the new collision grid.
        // Register new polygons, and determine the minimum grid size we need.
        FroggerMapFilePacketPolygon polygonPacket = map.getPolygonPacket();
        polygonPacket.clear();
        for (FroggerMapPolygon polygon : context.getNewPolygons()) {
            polygonPacket.addPolygon(polygon);

            // Apply animation.
            if (polygon.getPolygonType().isTextured()) {
                FroggerMapAnimation animation = animationsByTexture.get(textureRemap.getRemappedTextureId(polygon.getTextureId()));
                if (animation != null)
                    animation.getTargetPolygons().add(new FroggerMapAnimationTargetPolygon(animation, polygon));
            }
        }

        // 5) Generate the new collision grid.
        map.getGridPacket().generateGrid(context.getNewPolygonGridSquareFlags());

        // 6) Generate map groups for the new level data.
        map.getGroupPacket().generateMapGroups(response, true);
        map.getLogger().info("Imported map data from %s.", inputFile.getName());
    }

    @Getter
    @RequiredArgsConstructor
    private static class FfsLoadContext {
        private final FroggerMapFile mapFile;
        private final ProblemResponse response;
        private final List<SVector> newVertices = new ArrayList<>();
        private final List<FroggerMapPolygon> newPolygons = new ArrayList<>();
        private final Map<FroggerMapPolygon, Integer> newPolygonGridSquareFlags = new HashMap<>();
        private int ffsVersion;
        private FroggerConfig froggerConfig;

        public void handleProblem(Level severity, String template, Object... arguments) {
            Utils.handleProblem(this.response, this.mapFile.getLogger(), severity, template, arguments);
        }
    }

    private static boolean commandFfsVersion(FfsLoadContext context, String[] args) {
        int newVersion = Integer.parseInt(args[0]);
        if (newVersion <= 0 || newVersion > CURRENT_FORMAT_VERSION)
            context.handleProblem(Level.WARNING, "The FFS reports a file version (%d) not supported by this version of FrogLord! This may lead to errors!", newVersion);

        context.ffsVersion = newVersion;
        return true;
    }

    private static boolean commandGameVersion(FfsLoadContext context, String[] args) {
        String versionName = args[0];

        if (context.getFroggerConfig() != null) {
            context.handleProblem(Level.SEVERE, "Tried to set the game version more than once? (%s)", versionName);
            return false;
        }

        GameConfig gameConfig = SCGameType.FROGGER.getVersionConfigByName(versionName);
        if (!(gameConfig instanceof FroggerConfig)) {
            context.handleProblem(Level.SEVERE, "Could not resolve a Frogger game version named '%s'.", versionName);
            return false;
        }

        context.froggerConfig = (FroggerConfig) gameConfig;
        return true;
    }

    private static boolean commandVertex(FfsLoadContext context, String[] args) {
        context.getNewVertices().add(new SVector(Float.parseFloat(args[0]), Float.parseFloat(args[1]), Float.parseFloat(args[2])));
        return true;
    }

    private static boolean commandTexture(FfsLoadContext context, String[] args) {
        // The remap is updated when the command is run because polygon commands rely on the remap having been updated first.
        context.getMapFile().getTextureRemap().getTextureIds().add(Short.parseShort(args[0]));
        return true;
    }

    private static boolean commandPolygon(FfsLoadContext context, String[] args) {
        int index = 0;

        FroggerMapPolygonType polygonType;
        try {
            polygonType = FroggerMapPolygonType.valueOf(args[index++].toUpperCase());
        } catch (IllegalArgumentException iae) {
            context.handleProblem(Level.SEVERE, "Invalid FroggerMapPolygonType: %s.", args[index - 1]);
            return false;
        }

        FroggerMapPolygon newPolygon = new FroggerMapPolygon(context.getMapFile(), polygonType);

        String showStr = args[index++];
        if ("show".equalsIgnoreCase(showStr)) {
            newPolygon.setVisible(true);
        } else if ("hide".equalsIgnoreCase(showStr)) {
            newPolygon.setVisible(false);
        } else {
            context.handleProblem(Level.SEVERE, "Don't know how to interpret '%s' as either 'show' or 'hide'.", showStr);
            return false;
        }

        // Read Texture ID & Flags
        if (polygonType.isTextured()) {
            short textureId = Short.parseShort(args[index++]);
            int textureFlags = Integer.parseInt(args[index++]);

            TextureRemapArray remap = context.getMapFile().getTextureRemap();
            newPolygon.setTextureId((short) remap.getRemapIndex(textureId)); // -1 if not found.
            newPolygon.setFlags(textureFlags);
        }

        // Read vertices.
        for (int i = 0; i < newPolygon.getVertexCount(); i++)
            newPolygon.getVertices()[i] = Integer.parseInt(args[index++]);

        // Read Texture UVs.
        if (polygonType.isTextured()) {
            for (int i = 0; i < newPolygon.getVertexCount(); i++) {
                String[] texCoordSplit = args[index++].split(":");
                float u = Float.parseFloat(texCoordSplit[0]);
                float v = Float.parseFloat(texCoordSplit[1]);
                newPolygon.getTextureUvs()[i].setFloatUV(u, v);
            }
        }

        // Read colors.
        for (int i = 0; i < newPolygon.getColors().length; i++)
            newPolygon.getColors()[i].fromRGB(Integer.parseInt(args[index++], 16));

        // Read grid square data.
        if (args.length > index) {
            int gridFlags = Integer.parseInt(args[index]);
            context.getNewPolygonGridSquareFlags().put(newPolygon, gridFlags);
        }

        context.getNewPolygons().add(newPolygon);
        return true;
    }

    @RequiredArgsConstructor
    private enum FfsCommand {
        FFS_VERSION("version_ffs", 1, FFSUtil::commandFfsVersion),
        GAME_VERSION("version_game", 1, FFSUtil::commandGameVersion),
        VERTEX("vertex", 3, FFSUtil::commandVertex),
        TEXTURE("texture", 1, FFSUtil::commandTexture),
        POLYGON("polygon", 6, FFSUtil::commandPolygon);

        @Getter private final String label;
        @Getter private final int minimumArguments;
        private final BiPredicate<FfsLoadContext, String[]> loadFunction;

        /**
         * Runs the command with the given arguments.
         * @param context the context to run with
         * @param args the arguments to run the command with
         * @return true iff the command completed successfully
         */
        public boolean runCommand(FfsLoadContext context, String[] args, int lineNumber) {
            if (context == null)
                throw new NullPointerException("context");
            if (args == null)
                throw new NullPointerException("args");

            if ((context.getFfsVersion() == 0 || context.getFroggerConfig() == null) && (this != FFS_VERSION && this != GAME_VERSION)) {
                context.handleProblem(Level.SEVERE, "The command '%s' on line %d may only be used after the game/format versions are specified.", this.label, lineNumber);
                return false;
            }

            if (this.minimumArguments > args.length) {
                context.handleProblem(Level.SEVERE, "The command '%s' on line %d requires at least %d arguments. (%d were specified)", this.label, lineNumber, this.minimumArguments, args.length);
                return false;
            }

            try {
                return this.loadFunction == null || this.loadFunction.test(context, args);
            } catch (Throwable th) {
                Utils.handleError(context.getMapFile().getLogger(), th, false, "An error occurred processing the '%s' command on line %d.", getLabel(), lineNumber);
                context.handleProblem(Level.SEVERE, "An error occurred processing the '%s' command on line %d.", getLabel(), lineNumber);
                return false;
            }
        }

        /**
         * Gets the command by the label.
         * @param label the label to resolve the command by.
         * @return commandOrNull
         */
        public static FfsCommand getCommandByLabel(String label) {
            if (label == null)
                return null;

            for (int i = 0; i < values().length; i++) {
                FfsCommand command = values()[i];
                if (label.equalsIgnoreCase(command.getLabel()))
                    return command;
            }

            return null;
        }
    }
}