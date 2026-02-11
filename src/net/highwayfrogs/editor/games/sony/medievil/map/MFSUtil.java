package net.highwayfrogs.editor.games.sony.medievil.map;


import lombok.Getter;
import lombok.NonNull;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.games.psx.math.vector.SVector;
import net.highwayfrogs.editor.games.psx.polygon.PSXPolygonType;
import net.highwayfrogs.editor.games.sony.medievil.MediEvilGameInstance;
import net.highwayfrogs.editor.games.sony.medievil.MediEvilLevelTableEntry;
import net.highwayfrogs.editor.games.sony.medievil.map.mesh.MediEvilMapPolygon;
import net.highwayfrogs.editor.games.sony.shared.SCByteTextureUV;
import net.highwayfrogs.editor.games.sony.shared.TextureRemapArray;
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
import net.highwayfrogs.editor.utils.commandparser.CommandListParser;
import net.highwayfrogs.editor.utils.logging.ILogger;
import net.highwayfrogs.editor.utils.objects.OptionalArguments;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

/**
 * MFSUtil is a utility class used for conversion to and from the .mfs (MediEvil File Sync) file format.
 * This format is used to allow editing MediEvil maps in Blender, and then re-importing them into FrogLord.
 *
 * The mfs format is specified as a text-based format, based on commands similar to Wavefront obj.
 * The '#' character at the start of a line indicates a comment.
 * Anything else will have the full line of text interpreted as a command, with the first (whitespace separated) parameter treated as the command label.
 * All other parameters are treated as arguments to that command.
 *
 * Command List:
 *  version_format <id> Indicates which version of the format is used. Should be the first command in a file.
 *  version_game <version_name> Indicates which version of the game the mfs file corresponds to. Necessary for texture ID reasons.
 *  texture <image_id> [texture_name] Defines a new texture.
 *  vertex <x> <y> <z> <color> Defines a new vertex at the given xyz position (in Frogger coordinate space.) The vertex is assigned an auto-incrementing ID, starting at 0.
 *  polygon <g3|g4|gt3|gt4> <flags> <vertex_ids[]...> [texture_id <texture_uvs[]...>] Defines a map polygon.
 *
 * Created by Kneesnap on 2/9/2026.
 */
public class MFSUtil {
    public static final String BLENDER_ADDON_FILE_NAME = "medievil-map-blender-plugin.py";


    // Increment this with any major change to the format.
    public static final int CURRENT_FORMAT_VERSION = 1;

    public static final String FILE_EXTENSION = "mfs";
    public static final BrowserFileType FILE_TYPE = new BrowserFileType("MediEvil File Sync", FILE_EXTENSION);
    public static final SavedFilePath IMPORT_PATH = new SavedFilePath(FILE_EXTENSION + "ImportPath", "Please select the map "+ FILE_EXTENSION + " file to import.", FILE_TYPE);
    public static final SavedFilePath EXPORT_FOLDER = new SavedFilePath(FILE_EXTENSION + "ExportPath", "Please select the folder to export the ." + FILE_EXTENSION + " map into.");

    private static final String COMMAND_VERTEX_NAME = "vertex";
    private static final String COMMAND_POLYGON_NAME = "polygon";
    @Getter private static final CommandListParser<MediEvilMapLoadContext> commandParser = new CommandListParser<MediEvilMapLoadContext>() {
        {
            SCMapFileSyncUtils.registerDefaultCommands(this);
            registerCommand(new LazyPostInitializationCommand<>(COMMAND_VERTEX_NAME, 4, MFSUtil::commandVertex));
            registerCommand(new LazyPostInitializationCommand<>(COMMAND_POLYGON_NAME, 5, MFSUtil::commandPolygon));
        }
    };

    /**
     * Save map data to a .medmap file for editing in Blender.
     * @param map The map to export.
     * @param outputDir The directory to write to.
     * @param response The response to take to any problems seen.
     */
    public static void saveMap(MediEvilMapFile map, File outputDir, ProblemResponse response) {
        if (map == null)
            throw new NullPointerException("map");
        if (outputDir == null)
            throw new NullPointerException("outputDir");
        if (response == null)
            throw new NullPointerException("response");
        if (!outputDir.isDirectory() || !outputDir.exists())
            throw new IllegalArgumentException("The provided outputDir was not a real directory! (" + outputDir + ")");

        MediEvilLevelTableEntry levelTableEntry = map.getLevelTableEntry();
        if (levelTableEntry == null) {
            Utils.handleProblem(response, map.getLogger(), Level.SEVERE, "There was no level table entry available for %s.", map.getFileDisplayName());
            return;
        }

        TextureRemapArray textureRemap = levelTableEntry.getRemap();
        VloFile vloFile = levelTableEntry.getVloFile(); // Null is allowed.
        if (textureRemap == null) {
            Utils.handleProblem(response, map.getLogger(), Level.SEVERE, "There was no texture remap available for %s.", map.getFileDisplayName());
            return;
        }

        StringBuilder builder = new StringBuilder();
        SCMapFileSyncUtils.writeHeader(builder, FILE_EXTENSION, CURRENT_FORMAT_VERSION, map);

        // Export textures.
        SCMapFileSyncUtils.writeTextureRemap(map.getLogger(), builder, outputDir, textureRemap, vloFile, response);

        // Write Vertices.
        List<SVector> vertices = map.getGraphicsPacket().getVertices();
        for (int i = 0; i < vertices.size(); i++) {
            SVector vertex = vertices.get(i);
            int rgbColor = MediEvilMapPolygon.getRGBFromPackedShort(vertex.getPadding());
            builder.append(COMMAND_VERTEX_NAME).append(' ')
                    .append(vertex.getFloatX()).append(' ')
                    .append(vertex.getFloatY()).append(' ')
                    .append(vertex.getFloatZ()).append(' ')
                    .append(String.format("%06X", rgbColor)).append(Constants.NEWLINE);
        }
        builder.append(Constants.NEWLINE);

        // Faces:
        List<MediEvilMapPolygon> polygons = map.getGraphicsPacket().getPolygons();
        for (MediEvilMapPolygon polygon : polygons) {
            String polygonTypeName = polygon.getPolygonType().name().toLowerCase();
            int underscoreIndex = polygonTypeName.lastIndexOf('_');
            if (underscoreIndex >= 0)
                polygonTypeName = polygonTypeName.substring(underscoreIndex + 1);

            builder.append(COMMAND_POLYGON_NAME).append(' ')
                    .append(polygonTypeName).append(' ')
                    .append(polygon.getFlags());

            // Write vertices.
            for (int i = 0; i < polygon.getVertexCount(); i++)
                builder.append(' ').append(polygon.getVertices()[i]);

            // Write Texture data.
            if (polygon.getPolygonType().isTextured()) {
                builder.append(' ').append(polygon.getTextureId()); // Use the non-remapped texture ID.

                // Write Texture UVs.
                for (int i = 0; i < polygon.getVertexCount(); i++) {
                    SCByteTextureUV textureUv = polygon.getTextureUvs()[i];
                    builder.append(' ').append(textureUv.getFloatU()).append(':').append(textureUv.getFloatV());
                }
            }

            builder.append(Constants.NEWLINE);
        }
        builder.append(Constants.NEWLINE);

        SCMapFileSyncUtils.writeMapFile(map, outputDir, FILE_EXTENSION, builder);
        SCMapFileSyncUtils.writeBlenderAddon(map.getGameInstance(), outputDir, BLENDER_ADDON_FILE_NAME);
    }

    /**
     * Read map data from a mfs file.
     * @param map       The map to export.
     * @param inputFile The file to read from.
     */
    public static void importMfsFile(ILogger logger, MediEvilMapFile map, File inputFile) {
        if (map == null)
            throw new NullPointerException("map");
        if (inputFile == null)
            throw new NullPointerException("inputFile");
        if (!inputFile.isFile() || !inputFile.exists())
            throw new IllegalArgumentException("The provided inputFile was not a real file! (" + inputFile + ")");
        if (logger == null)
            logger = map.getLogger();

        String inputFileName = inputFile.getName();
        MediEvilGameInstance instance = map.getGameInstance();

        MediEvilLevelTableEntry levelTableEntry = map.getLevelTableEntry();
        if (levelTableEntry == null) {
            instance.showWarning(logger, "No level table entry found!",  "There was no level table entry available for %s.", map.getFileDisplayName());
            return;
        }

        // Load map file.
        MediEvilMapLoadContext context = new MediEvilMapLoadContext(map, logger, inputFileName);
        context.executeCommands(commandParser, inputFile); // If any error occurs, an exception will be thrown, and changes undone.

        // Apply the results to the map file.
        // 1) Apply new vertices.
        map.getGraphicsPacket().setVertices(context.getNewVertices());

        // 2) Apply new polygons.
        map.getGraphicsPacket().getPolygons().clear();
        map.getGraphicsPacket().getPolygons().addAll(context.getNewPolygons());

        // 3) Generate collision data based on the new polygons.
        map.regeneratePolygonData();

        // 4) Finish.
        context.finish();
    }

    @Getter
    private static class MediEvilMapLoadContext extends MapFileSyncLoadContext<MediEvilMapFile> {
        private final List<SVector> newVertices = new ArrayList<>();
        private final List<MediEvilMapPolygon> newPolygons = new ArrayList<>();

        public MediEvilMapLoadContext(MediEvilMapFile mapFile, ILogger logger, String importedFileName) {
            super(mapFile, logger, importedFileName, CURRENT_FORMAT_VERSION);
        }

        private @NonNull MediEvilLevelTableEntry getLevelTableEntry() {
            MediEvilLevelTableEntry levelTableEntry = getMapFile().getLevelTableEntry();
            if (levelTableEntry == null)
                throw new IllegalStateException("Could not resolve levelTableEntry for '" + getMapFile().getFileDisplayName() + "'.");

            return levelTableEntry;
        }

        @Override
        protected TextureRemapArray resolveTextureRemap() {
            return getLevelTableEntry().getRemap();
        }

        @Override
        protected VloFile resolveVloFile() {
            return getLevelTableEntry().getVloFile();
        }
    }

    private static void commandVertex(MediEvilMapLoadContext context, OptionalArguments args) {
        float x = args.useNext().getAsFloat();
        float y = args.useNext().getAsFloat();
        float z = args.useNext().getAsFloat();
        int colorRgb = Integer.parseInt(args.useNext().getAsString(), 16);

        SVector newVertex = new SVector(x, y, z);
        newVertex.setPadding(MediEvilMapPolygon.toPackedShort(colorRgb));
        context.getNewVertices().add(newVertex);
    }

    private static void commandPolygon(MediEvilMapLoadContext context, OptionalArguments args) throws CommandListException {
        MediEvilMapPolygon newPolygon = new MediEvilMapPolygon(context.getMapFile());

        PSXPolygonType polygonType;
        String polygonTypeName = args.useNext().getAsString();
        switch (polygonTypeName) {
            case "g3":
            case "G3":
                polygonType = PSXPolygonType.POLY_G3;
                break;
            case "g4":
            case "G4":
                polygonType = PSXPolygonType.POLY_G4;
                break;
            case "gt3":
            case "GT3":
                polygonType = PSXPolygonType.POLY_GT3;
                break;
            case "gt4":
            case "GT4":
                polygonType = PSXPolygonType.POLY_GT4;
                break;
            default:
                throw new CommandListSyntaxError("Invalid polygon type name: '" + polygonTypeName + "', was expected to be : 'g3', 'g4', 'gt3', 'gt4'.");
        }

        int flags = args.useNext().getAsInteger();
        newPolygon.setFlagMask(flags, true); // Apply flags before setting polygon type, since polygon type is tracked as flags.
        newPolygon.setPolygonType(polygonType);

        // Read vertices.
        for (int i = 0; i < newPolygon.getVertexCount(); i++)
            newPolygon.getVertices()[i] = args.useNext().getAsInteger();

        // Read Texture ID & Flags
        if (polygonType.isTextured()) {
            short textureId = args.useNext().getAsShort();
            newPolygon.setTextureId(textureId);

            // Read Texture UVs.
            for (int i = 0; i < newPolygon.getVertexCount(); i++) {
                String[] texCoordSplit = args.useNext().getAsString().split(":");
                float u = Float.parseFloat(texCoordSplit[0]);
                float v = Float.parseFloat(texCoordSplit[1]);
                newPolygon.getTextureUvs()[i].setFloatUV(u, v);
            }
        }

        context.getNewPolygons().add(newPolygon);
    }
}