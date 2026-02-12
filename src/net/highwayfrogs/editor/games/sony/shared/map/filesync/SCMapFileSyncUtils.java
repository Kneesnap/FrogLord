package net.highwayfrogs.editor.games.sony.shared.map.filesync;

import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.games.sony.SCGameFile;
import net.highwayfrogs.editor.games.sony.SCGameInstance;
import net.highwayfrogs.editor.games.sony.shared.TextureRemapArray;
import net.highwayfrogs.editor.games.sony.shared.vlo2.VloFile;
import net.highwayfrogs.editor.games.sony.shared.vlo2.VloImage;
import net.highwayfrogs.editor.utils.FileUtils;
import net.highwayfrogs.editor.utils.StringUtils;
import net.highwayfrogs.editor.utils.Utils;
import net.highwayfrogs.editor.utils.Utils.ProblemResponse;
import net.highwayfrogs.editor.utils.commandparser.CommandListParser;
import net.highwayfrogs.editor.utils.commandparser.TextCommand;
import net.highwayfrogs.editor.utils.logging.ILogger;

import javax.imageio.ImageIO;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Date;
import java.util.logging.Level;

/**
 * Contains utilities shared between various map file sync formats (.ffs, .mfs, etc).
 * Created by Kneesnap on 2/9/2026.
 */
public class SCMapFileSyncUtils {
    private static final int IMAGE_EXPORT_FILTER = VloImage.DEFAULT_IMAGE_NO_PADDING_EXPORT_SETTINGS;

    /**
     * Writes a file-sync header.
     * @param builder the builder to write the file to
     * @param fileExtension the file extension
     * @param currentFormatVersion the current format version for this file type
     * @param mapFile the map file the header represents
     */
    public static void writeHeader(StringBuilder builder, String fileExtension, int currentFormatVersion, SCGameFile<?> mapFile) {
        if (builder == null)
            throw new NullPointerException("builder");
        if (mapFile == null)
            throw new NullPointerException("mapFile");

        SCGameInstance instance = mapFile.getGameInstance();
        builder.append("# ").append(instance.getGameType().getDisplayName()).append(" File Export (.").append(fileExtension).append(") -- By FrogLord ").append(Constants.VERSION).append('\n');
        builder.append("# Map: ").append(mapFile.getFileDisplayName()).append('\n');
        builder.append("# Export Time: ").append(new Date()).append('\n');

        // Write format version.
        builder.append(CommandFormatVersion.LABEL).append(' ').append(currentFormatVersion).append("\n");
        builder.append(CommandGameVersion.LABEL).append(' ').append(instance.getVersionConfig().getInternalName()).append("\n\n");
    }

    /**
     * Writes the map file as built from the StringBuilder
     * @param map the map file to write
     * @param outputDir the output folder to write the file to
     * @param fileExtension the file extension for the file
     * @param builder the builder containing the file data
     */
    public static void writeMapFile(SCGameFile<?> map, File outputDir, String fileExtension, StringBuilder builder) {
        if (builder == null)
            throw new NullPointerException("builder");
        if (outputDir == null)
            throw new NullPointerException("outputDir");
        if (map == null)
            throw new NullPointerException("map");

        // Write the file.
        String outputFileName = FileUtils.stripExtension(map.getFileDisplayName()) + "." + fileExtension;
        File outputFile = new File(outputDir, outputFileName);

        try {
            Files.write(outputFile.toPath(), builder.toString().getBytes(StandardCharsets.UTF_8));
            map.getLogger().info("Exported %s as %s.", map.getFileDisplayName(), outputDir.getName() + File.separator + outputFileName);
        } catch (IOException ex) {
            throw new RuntimeException("Failed to save " + outputFileName, ex);
        }
    }


    /**
     * Writes the python blender addon file to the output directory
     * @param instance the instance to write from
     * @param outputDir the output directory
     * @param blenderAddonFileName the name of the python script to write (copied from internal game resource folder)
     */
    public static void writeBlenderAddon(SCGameInstance instance, File outputDir, String blenderAddonFileName) {
        if (instance == null)
            throw new NullPointerException("instance");
        if (outputDir == null)
            throw new NullPointerException("outputDir");
        if (StringUtils.isNullOrWhiteSpace(blenderAddonFileName))
            throw new NullPointerException("blenderAddonFileName");

        InputStream blenderScriptStream = instance.getGameType().getEmbeddedResourceStream(blenderAddonFileName);
        if (blenderScriptStream == null)
            throw new IllegalArgumentException("Unable to resolve the file '" + blenderAddonFileName + "' for extraction.");

        try {
            Files.write(new File(outputDir, blenderAddonFileName).toPath(), FileUtils.readBytesFromStream(blenderScriptStream));
        } catch (IOException ex) {
            throw new RuntimeException("Failed to save " + blenderAddonFileName, ex);
        }
    }

    /**
     * Writes the texture remap to the file builder, and saves the corresponding textures to the output folder.
     * @param logger the logger to write to in case of failure.
     * @param builder the builder to write the texture remap to
     * @param outputDir the output directory to save textures to
     * @param textureRemap the texture remap to write
     * @param vloFile the vlo file which contains the remapped textures (optional, but recommended)
     * @param response how to respond to problems such as textures not being found
     */
    public static void writeTextureRemap(ILogger logger, StringBuilder builder, File outputDir, TextureRemapArray textureRemap, VloFile vloFile, ProblemResponse response) {
        if (logger == null)
            throw new NullPointerException("logger");
        if (builder == null)
            throw new NullPointerException("builder");
        if (outputDir == null)
            throw new NullPointerException("outputDir");
        if (textureRemap == null)
            throw new NullPointerException("textureRemap");
        // vloFile null is allowed

        // Export textures.
        for (int i = 0; i < textureRemap.getTextureIds().size(); i++) {
            VloImage image = textureRemap.resolveTexture(i, vloFile);
            if (image == null) {
                if (textureRemap.getRemappedTextureId(i) == -1)
                    continue;

                Utils.handleProblem(response, logger, Level.SEVERE, "Could not resolve texture remap index %d to a valid image!", i);
                return;
            }

            // Write texture command.
            builder.append(CommandTexture.LABEL)
                    .append(' ').append(image.getTextureId());

            // Write texture name.
            String fileName = image.getName();
            if (fileName == null) {
                fileName = String.valueOf(image.getTextureId());
            } else {
                // Write the image file name to the command.
                builder.append(" ").append(fileName);
            }

            builder.append(Constants.NEWLINE);

            // Write texture to output directory.
            File imageFileOutput = new File(outputDir, fileName + ".png");
            try {
                ImageIO.write(image.toBufferedImage(IMAGE_EXPORT_FILTER), "png", imageFileOutput);
            } catch (IOException ex) {
                throw new RuntimeException("Failed to save " + imageFileOutput.getName(), ex);
            }
        }

        if (textureRemap.getTextureIds().size() > 0)
            builder.append(Constants.NEWLINE);
    }

    /**
     * Registers the default commands available to all map file sync formats.
     * @param commandParser the command parser to register the commands to
     * @param <TContext> the context provided to the command parser
     */
    @SuppressWarnings("unchecked")
    public static <TContext extends MapFileSyncLoadContext<?>> void registerDefaultCommands(CommandListParser<TContext> commandParser) {
        commandParser.registerCommand((TextCommand<TContext>) CommandFormatVersion.INSTANCE);
        commandParser.registerCommand((TextCommand<TContext>) CommandGameVersion.INSTANCE);
        commandParser.registerCommand((TextCommand<TContext>) CommandTexture.INSTANCE);
    }
}
