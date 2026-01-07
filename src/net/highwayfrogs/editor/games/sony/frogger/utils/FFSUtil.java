package net.highwayfrogs.editor.games.sony.frogger.utils;

import lombok.Getter;
import lombok.NonNull;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.games.psx.math.vector.SVector;
import net.highwayfrogs.editor.games.generic.GameConfig;
import net.highwayfrogs.editor.games.sony.SCGameType;
import net.highwayfrogs.editor.games.sony.frogger.FroggerConfig;
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
import net.highwayfrogs.editor.games.sony.shared.vlo2.VloFile;
import net.highwayfrogs.editor.games.sony.shared.vlo2.VloImage;
import net.highwayfrogs.editor.utils.FileUtils;
import net.highwayfrogs.editor.utils.Utils;
import net.highwayfrogs.editor.utils.Utils.ProblemResponse;
import net.highwayfrogs.editor.utils.commandparser.*;
import net.highwayfrogs.editor.utils.commandparser.CommandListException.CommandListExecutionError;
import net.highwayfrogs.editor.utils.commandparser.CommandListException.CommandListSyntaxError;
import net.highwayfrogs.editor.utils.logging.ILogger;
import net.highwayfrogs.editor.utils.logging.MessageTrackingLogger;
import net.highwayfrogs.editor.utils.objects.OptionalArguments;

import javax.imageio.ImageIO;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
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

    private static final int FFS_EXPORT_FILTER = VloImage.DEFAULT_IMAGE_NO_PADDING_EXPORT_SETTINGS;

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
        builder.append(FfsCommandType.FFS_VERSION.getLabel()).append(' ').append(CURRENT_FORMAT_VERSION).append("\n");
        builder.append(FfsCommandType.GAME_VERSION.getLabel()).append(' ').append(map.getGameInstance().getVersionConfig().getInternalName()).append("\n\n");

        // Export textures.
        VloFile mapVlo = map.getVloFile();
        for (int i = 0; i < textureRemap.getTextureIds().size(); i++) {
            VloImage image = textureRemap.resolveTexture(i, mapVlo);
            if (image == null) {
                Utils.handleProblem(response, map.getLogger(), Level.SEVERE, "Could not resolve texture remap index %d to a valid image!", i);
                return;
            }

            builder.append(FfsCommandType.TEXTURE.getLabel())
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
            builder.append(FfsCommandType.VERTEX.getLabel()).append(' ')
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
            builder.append(FfsCommandType.POLYGON.getLabel()).append(' ')
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

        // Clear remap before running commands. (Commands must update remap.)
        List<Short> oldTextureIds = new ArrayList<>(textureRemap.getTextureIds());
        textureRemap.getTextureIds().clear();
        logger.info("Importing map data from %s.", inputFile.getName());

        // 1) Run commands from file.
        MessageTrackingLogger trackedLogger = new MessageTrackingLogger(logger);
        FfsLoadContext context = new FfsLoadContext(map, trackedLogger);

        try {
            FfsCommandType.getCommandParser().executeCommands(context, inputFile);
        } catch (Throwable th) {
            textureRemap.getTextureIds().clear();
            textureRemap.getTextureIds().addAll(oldTextureIds);
            throw new RuntimeException("An error occurred while loading '" + inputFile.getName() + "'.", th);
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
            map.getGameInstance().showWarning(trackedLogger, "Overflowed the texture remap!", "The texture remap for %s has room for %d textures, but %d were imported.\nThis will likely cause graphical corruption in-game!", map.getFileDisplayName(), textureRemap.getTextureIdSlotsAvailable(), textureRemap.getTextureIds().size());
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
        map.getGroupPacket().generateMapGroups(trackedLogger, ProblemResponse.CREATE_POPUP, true);
        trackedLogger.info("Finished importing map data.");

        if (trackedLogger.hasErrorsOrWarnings())
            trackedLogger.showImportPopup(inputFile.getName());
    }

    @Getter
    private static class FfsLoadContext extends CommandListExecutionContext {
        @NonNull private final FroggerMapFile mapFile;
        private final List<SVector> newVertices = new ArrayList<>();
        private final List<FroggerMapPolygon> newPolygons = new ArrayList<>();
        private final Map<FroggerMapPolygon, Integer> newPolygonGridSquareFlags = new HashMap<>();
        private int ffsVersion;
        private FroggerConfig froggerConfig;

        public FfsLoadContext(FroggerMapFile mapFile, ILogger logger) {
            super(logger);
            this.mapFile = mapFile;
        }
    }

    private static boolean commandFfsVersion(FfsLoadContext context, OptionalArguments args) {
        int newVersion = args.useNext().getAsInteger();
        if (newVersion <= 0 || newVersion > CURRENT_FORMAT_VERSION)
            context.getLogger().warning("The FFS reports a file version (%d) not supported by this version of FrogLord! This may lead to errors!", newVersion);

        context.ffsVersion = newVersion;
        return true;
    }

    private static void commandGameVersion(FfsLoadContext context, OptionalArguments args) throws CommandListExecutionError {
        String versionName = args.useNext().getAsString();

        if (context.getFroggerConfig() != null)
            throw new CommandListExecutionError("Tried to set the game version again, after it was already set? (%s)", versionName);

        GameConfig gameConfig = SCGameType.FROGGER.getVersionConfigByName(versionName);
        if (!(gameConfig instanceof FroggerConfig))
            throw new CommandListExecutionError("Could not resolve a Frogger game version named '%s'.", versionName);

        context.froggerConfig = (FroggerConfig) gameConfig;
    }

    private static void commandVertex(FfsLoadContext context, OptionalArguments args) {
        float x = args.useNext().getAsFloat();
        float y = args.useNext().getAsFloat();
        float z = args.useNext().getAsFloat();
        context.getNewVertices().add(new SVector(x, y, z));
    }

    private static void commandTexture(FfsLoadContext context, OptionalArguments args) {
        // The remap is updated when the command is run because polygon commands rely on the remap having been updated first.
        context.getMapFile().getTextureRemap().getTextureIds().add(args.useNext().getAsShort());
    }

    private static void commandPolygon(FfsLoadContext context, OptionalArguments args) throws CommandListException {
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
            newPolygon.setTextureId((short) remap.getRemapIndex(textureId)); // -1 if not found.
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

    @Getter
    private enum FfsCommandType {
        FFS_VERSION("version_ffs", 1, FFSUtil::commandFfsVersion),
        GAME_VERSION("version_game", 1, FFSUtil::commandGameVersion),
        VERTEX("vertex", 3, FFSUtil::commandVertex),
        TEXTURE("texture", 1, FFSUtil::commandTexture),
        POLYGON("polygon", 6, FFSUtil::commandPolygon);

        private final String label;
        private final FFSCommand command;

        @Getter private static final CommandListParser<FfsLoadContext> commandParser = new CommandListParser<>();

        static {
            for (FfsCommandType commandType : values())
                commandParser.registerCommand(commandType.getCommand());
        }

        FfsCommandType(String label, int minimumArguments, ILazyTextCommandHandler<FfsLoadContext> lazyHandler) {
            this.label = label;
            this.command = new FFSCommand(this, label, minimumArguments, lazyHandler);
        }
    }

    private static class FFSCommand extends TextCommand<FfsLoadContext> {
        private final FfsCommandType commandType;
        private final ILazyTextCommandHandler<FfsLoadContext> lazyHandler;

        public FFSCommand(FfsCommandType commandType, String name, int minimumArguments, ILazyTextCommandHandler<FfsLoadContext>lazyHandler) {
            super(name, minimumArguments);
            this.commandType = commandType;
            this.lazyHandler = lazyHandler;
        }

        @Override
        public void validateBeforeExecution(FfsLoadContext context, OptionalArguments arguments, CommandLocation location) throws CommandListException {
            super.validateBeforeExecution(context, arguments, location);

            if ((context.getFfsVersion() == 0 || context.getFroggerConfig() == null) && (this.commandType != FfsCommandType.FFS_VERSION && this.commandType != FfsCommandType.GAME_VERSION))
                throw new CommandListExecutionError(location, "The command '%s' may only be used after the game/format versions are specified.", getName());
        }

        @Override
        public void execute(FfsLoadContext context, OptionalArguments arguments) throws CommandListException {
            this.lazyHandler.handle(context, arguments);
        }
    }
}