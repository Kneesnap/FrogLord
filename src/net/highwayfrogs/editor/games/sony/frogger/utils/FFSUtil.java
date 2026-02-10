package net.highwayfrogs.editor.games.sony.frogger.utils;

import lombok.Getter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.games.psx.math.vector.SVector;
import net.highwayfrogs.editor.games.sony.frogger.FroggerGameInstance;
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
import net.highwayfrogs.editor.games.sony.shared.map.filesync.CommandFormatVersion;
import net.highwayfrogs.editor.games.sony.shared.map.filesync.LazyPostInitializationCommand;
import net.highwayfrogs.editor.games.sony.shared.map.filesync.MapFileSyncLoadContext;
import net.highwayfrogs.editor.games.sony.shared.map.filesync.SCMapFileSyncUtils;
import net.highwayfrogs.editor.games.sony.shared.vlo2.VloFile;
import net.highwayfrogs.editor.utils.FileUtils.BrowserFileType;
import net.highwayfrogs.editor.utils.FileUtils.SavedFilePath;
import net.highwayfrogs.editor.utils.Utils;
import net.highwayfrogs.editor.utils.Utils.ProblemResponse;
import net.highwayfrogs.editor.utils.commandparser.CommandListException;
import net.highwayfrogs.editor.utils.commandparser.CommandListException.CommandListSyntaxError;
import net.highwayfrogs.editor.utils.commandparser.CommandListExecutionContext;
import net.highwayfrogs.editor.utils.commandparser.CommandListParser;
import net.highwayfrogs.editor.utils.commandparser.TextCommand;
import net.highwayfrogs.editor.utils.logging.ILogger;
import net.highwayfrogs.editor.utils.objects.OptionalArguments;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
 *  texture <image_id> [texture_name] Defines a new texture.
 *  vertex <x> <y> <z> Defines a new vertex at the given xyz position (in Frogger coordinate space.) The vertex is assigned an auto-incrementing ID, starting at 0.
 *  polygon <f3|f4|ft3|ft4|g3|g4|gt3|gt4> <show|hide> [<texture_id> <texture_flags>] <vertexIds[]...> [texture_uvs[]...] <colors[]...> [grid_flags] Defines a map polygon.
 *
 * Created on 11/5/2019 by Kneesnap.
 */
public class FFSUtil {
    public static final String BLENDER_ADDON_FILE_NAME = "frogger-map-blender-plugin.py";

    // Increment this with any major change to the format.
    public static final int CURRENT_FORMAT_VERSION = 2;

    public static final String FILE_EXTENSION = "ffs";
    public static final BrowserFileType FILE_TYPE = new BrowserFileType("Frogger File Sync", FILE_EXTENSION);
    public static final SavedFilePath IMPORT_PATH = new SavedFilePath(FILE_EXTENSION + "ImportPath", "Please select the map "+ FILE_EXTENSION + " file to import.", FILE_TYPE);
    public static final SavedFilePath EXPORT_FOLDER = new SavedFilePath(FILE_EXTENSION + "ExportPath", "Please select the folder to export the ." + FILE_EXTENSION + " map into.");

    private static final String COMMAND_VERSION_FFS_NAME = "version_ffs"; // Included for compatibility.
    private static final String COMMAND_VERTEX_NAME = "vertex";
    private static final String COMMAND_POLYGON_NAME = "polygon";
    @SuppressWarnings("unchecked") @Getter private static final CommandListParser<FroggerMapLoadContext> commandParser = new CommandListParser<FroggerMapLoadContext>() {
        {
            SCMapFileSyncUtils.registerDefaultCommands(this);
            registerCommand(new LazyPostInitializationCommand<>(COMMAND_VERTEX_NAME, 3, FFSUtil::commandVertex));
            registerCommand(new LazyPostInitializationCommand<>(COMMAND_POLYGON_NAME, 6, FFSUtil::commandPolygon));

            // Compatibility:
            registerCommand(new LazyPostInitializationCommand<>(COMMAND_VERSION_FFS_NAME, CommandFormatVersion.INSTANCE.getMinimumArguments(),
                    (context, args) -> ((TextCommand<CommandListExecutionContext>) CommandFormatVersion.INSTANCE).execute(context, args)));
        }
    };


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
        SCMapFileSyncUtils.writeHeader(builder, FILE_EXTENSION, CURRENT_FORMAT_VERSION, map);

        // Export textures.
        SCMapFileSyncUtils.writeTextureRemap(map.getLogger(), builder, outputDir, textureRemap, map.getVloFile(), response);

        // Write Vertices.
        List<SVector> vertices = map.getVertexPacket().getVertices();
        for (int i = 0; i < vertices.size(); i++) {
            SVector vertex = vertices.get(i);
            builder.append(COMMAND_VERTEX_NAME).append(' ')
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
            builder.append(COMMAND_POLYGON_NAME).append(' ')
                    .append(polygon.getPolygonType().name().toLowerCase()).append(' ')
                    .append(polygon.isVisible() ? "show" : "hide");

            // Write Texture ID
            if (polygon.getPolygonType().isTextured())
                builder.append(' ').append(polygon.getTextureId()) // Use the non-remapped texture ID.
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

        // Write the files.
        SCMapFileSyncUtils.writeMapFile(map, outputDir, FILE_EXTENSION, builder);
        SCMapFileSyncUtils.writeBlenderAddon(map.getGameInstance(), outputDir, BLENDER_ADDON_FILE_NAME);
    }

    /**
     * Read map data from a FFS file.
     * @param map       The map to export.
     * @param inputFile The file to read from.
     */
    public static void importFFSToMap(ILogger logger, FroggerMapFile map, File inputFile) {
        if (map == null)
            throw new NullPointerException("map");
        if (inputFile == null)
            throw new NullPointerException("inputFile");
        if (!inputFile.isFile() || !inputFile.exists())
            throw new IllegalArgumentException("The provided inputFile was not a real file! (" + inputFile + ")");
        if (logger == null)
            logger = map.getLogger();

        FroggerGameInstance instance = map.getGameInstance();
        TextureRemapArray textureRemap = map.getTextureRemap();
        if (textureRemap == null) {
            instance.showWarning(logger, "No texture remap found!",  "There was no texture remap available for %s.", map.getFileDisplayName());
            return;
        }

        // Store animation data for later, and clear existing polygons from it.
        Map<Short, FroggerMapAnimation> animationsByTexture = new HashMap<>();
        for (FroggerMapAnimation animation : map.getAnimationPacket().getAnimations()) {
            for (int i = 0; i < animation.getTargetPolygons().size(); i++) {
                FroggerMapAnimationTargetPolygon targetPolygonWrapper = animation.getTargetPolygons().get(i);
                FroggerMapPolygon targetPolygon = targetPolygonWrapper.getPolygon();
                targetPolygonWrapper.setPolygon(null); // Remove the polygon from tracking. (Restored on error.)
                if (targetPolygon != null && targetPolygon.getPolygonType().isTextured())
                    animationsByTexture.putIfAbsent(textureRemap.getRemappedTextureId(targetPolygon.getTextureId()), animation);
            }

            // Clear the animation targets.
            animation.getTargetPolygons().clear();
        }

        // Load map file.
        FroggerMapLoadContext context = new FroggerMapLoadContext(map, logger, inputFile.getName());
        try {
            context.executeCommands(commandParser, inputFile);
        } catch (Throwable th) {
            // Undo animation polygon stuff.
            List<FroggerMapPolygon> polygons = map.getPolygonPacket().getPolygons();
            for (int i = 0; i < polygons.size(); i++)
                restoreAnimations(animationsByTexture, polygons.get(i), textureRemap);

            // If any error occurs, an exception will be thrown, and changes undone.
            throw th;
        }

        // Apply the results to the map file.
        // 1) Apply new vertices.
        FroggerMapFilePacketVertex vertexPacket = map.getVertexPacket();
        vertexPacket.getVertices().clear();
        vertexPacket.getVertices().addAll(context.getNewVertices());

        // 2) Apply new polygons.
        FroggerMapFilePacketPolygon polygonPacket = map.getPolygonPacket();
        polygonPacket.clear();
        for (FroggerMapPolygon polygon : context.getNewPolygons()) {
            polygonPacket.addPolygon(polygon);
            restoreAnimations(animationsByTexture, polygon, textureRemap); // Apply animation.
        }

        // 3) Generate the new collision grid.
        map.getGridPacket().generateGrid(context.getNewPolygonGridSquareFlags());
        map.getGridPacket().warnAboutLargeGridStacks(context.getLogger());

        // 4) Generate map groups for the new level data.
        map.getGroupPacket().generateMapGroups(context.getLogger(), ProblemResponse.CREATE_POPUP, true);

        // 5) Finish.
        context.finish();
    }

    private static void restoreAnimations(Map<Short, FroggerMapAnimation> animationsByTexture, FroggerMapPolygon polygon, TextureRemapArray textureRemap) {
        if (!polygon.getPolygonType().isTextured())
            return;

        Short remappedTextureId = textureRemap.getRemappedTextureId(polygon.getTextureId());
        if (remappedTextureId == null)
            return;

        FroggerMapAnimation animation = animationsByTexture.get(remappedTextureId);
        if (animation != null)
            animation.getTargetPolygons().add(new FroggerMapAnimationTargetPolygon(animation, polygon));
    }

    @Getter
    private static class FroggerMapLoadContext extends MapFileSyncLoadContext<FroggerMapFile> {
        private final List<SVector> newVertices = new ArrayList<>();
        private final List<FroggerMapPolygon> newPolygons = new ArrayList<>();
        private final Map<FroggerMapPolygon, Integer> newPolygonGridSquareFlags = new HashMap<>();

        public FroggerMapLoadContext(FroggerMapFile mapFile, ILogger logger, String importedMapFileName) {
            super(mapFile, logger, importedMapFileName, CURRENT_FORMAT_VERSION);
        }

        @Override
        protected TextureRemapArray resolveTextureRemap() {
            return getMapFile().getTextureRemap();
        }

        @Override
        protected VloFile resolveVloFile() {
            return getMapFile().getVloFile();
        }
    }

    private static void commandVertex(FroggerMapLoadContext context, OptionalArguments args) {
        float x = args.useNext().getAsFloat();
        float y = args.useNext().getAsFloat();
        float z = args.useNext().getAsFloat();
        context.getNewVertices().add(new SVector(x, y, z));
    }

    private static void commandPolygon(FroggerMapLoadContext context, OptionalArguments args) throws CommandListException {
        FroggerMapPolygonType polygonType = args.useNext().getAsEnumOrError(FroggerMapPolygonType.class);
        FroggerMapPolygon newPolygon = new FroggerMapPolygon(context.getMapFile(), polygonType);

        String showStr = args.useNext().getAsString();
        if ("show".equalsIgnoreCase(showStr)) {
            newPolygon.setVisible(true);
        } else if ("hide".equalsIgnoreCase(showStr)) {
            newPolygon.setVisible(false);
        } else {
            throw new CommandListSyntaxError("The argument '%s' was expected to either be 'show' or 'hide'.", showStr);
        }

        // Read Texture ID & Flags
        if (polygonType.isTextured()) {
            short textureId = args.useNext().getAsShort();
            int textureFlags = args.useNext().getAsInteger();

            TextureRemapArray remap = context.getMapFile().getTextureRemap();
            if (context.getFileFormatVersion() > 1) { // Version 2 uses the index into the remap.
                newPolygon.setTextureId(textureId);
            } else {
                newPolygon.setTextureId((short) remap.getRemapIndex(textureId)); // -1 if not found.
            }
            newPolygon.setFlags(textureFlags);
        }

        // Read vertices.
        for (int i = 0; i < newPolygon.getVertexCount(); i++)
            newPolygon.getVertices()[i] = args.useNext().getAsInteger();

        // Read Texture UVs.
        if (polygonType.isTextured()) {
            for (int i = 0; i < newPolygon.getVertexCount(); i++) {
                String[] texCoordSplit = args.useNext().getAsString().split(":");
                float u = Float.parseFloat(texCoordSplit[0]);
                float v = Float.parseFloat(texCoordSplit[1]);
                newPolygon.getTextureUvs()[i].setFloatUV(u, v);
            }
        }

        // Read colors.
        for (int i = 0; i < newPolygon.getColors().length; i++)
            newPolygon.getColors()[i].fromRGB(Integer.parseInt(args.useNext().getAsString(), 16));

        // Read grid square data.
        if (args.hasNext()) {
            int gridFlags = args.useNext().getAsInteger();
            context.getNewPolygonGridSquareFlags().put(newPolygon, gridFlags);
        }

        context.getNewPolygons().add(newPolygon);
    }
}