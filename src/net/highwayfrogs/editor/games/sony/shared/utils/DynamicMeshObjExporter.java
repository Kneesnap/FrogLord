package net.highwayfrogs.editor.games.sony.shared.utils;

import javafx.embed.swing.SwingFXUtils;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.games.generic.GameInstance;
import net.highwayfrogs.editor.gui.mesh.DynamicMesh;
import net.highwayfrogs.editor.utils.FileUtils;
import net.highwayfrogs.editor.utils.FileUtils.BrowserFileType;
import net.highwayfrogs.editor.utils.FileUtils.SavedFilePath;
import net.highwayfrogs.editor.utils.StringUtils;
import net.highwayfrogs.editor.utils.Utils;
import net.highwayfrogs.editor.utils.logging.ILogger;

import javax.imageio.ImageIO;
import java.io.File;
import java.io.IOException;
import java.util.Calendar;

/**
 * Contains utilities for exporting DynamicMesh objects to wavefront .obj files.
 * Created by Kneesnap on 5/2/2025.
 */
public class DynamicMeshObjExporter {
    public static final BrowserFileType OBJ_FILE_TYPE = new BrowserFileType("Wavefront Obj", "obj");
    public static final SavedFilePath OBJ_EXPORT_FILE_PATH = new SavedFilePath("objExportFilePath", "Please select the folder to save the .obj file to.", OBJ_FILE_TYPE);
    public static final SavedFilePath OBJ_EXPORT_FOLDER_PATH = new SavedFilePath("objExportFolderPath", "Please select the folder to save the .obj file to.");

    /**
     * Asks the user where they would like to export a DynamicMesh to wavefront .obj, then exports it.
     * This export process is not intended for mesh editing, and cannot be re-imported.
     * This will export the model with vertex colors baked into the texture, if possible.
     * @param logger The logger to print messages with
     * @param mesh The mesh to export
     * @param outputName The name to export the mesh as
     * @param exportTextures If true, textures will be exported
     */
    public static void askUserToMeshToObj(GameInstance instance, ILogger logger, DynamicMesh mesh, String outputName, boolean exportTextures) {
        if (StringUtils.isNullOrWhiteSpace(outputName))
            throw new NullPointerException("instance");
        if (mesh == null)
            throw new NullPointerException("mesh");
        if (StringUtils.isNullOrWhiteSpace(outputName))
            throw new NullPointerException("outputName");

        String suggestedName = FileUtils.stripExtension(outputName) + ".obj";
        File outputFile = FileUtils.askUserToSaveFile(instance, OBJ_EXPORT_FILE_PATH, suggestedName);
        if (outputFile != null)
            exportMeshToObj(logger, mesh, outputFile, FileUtils.stripExtension(outputFile.getName()), exportTextures);
    }

    /**
     * Exports a DynamicMesh to wavefront obj file.
     * This export process is not intended for mesh editing, and cannot be re-imported.
     * This will export the model with vertex colors baked into the texture, if possible.
     * @param logger The logger to print messages with
     * @param mesh The mesh to export
     * @param output The directory/file to export to
     * @param outputName The name to export the mesh as
     * @param exportTextures If true, textures will be exported
     */
    public static void exportMeshToObj(ILogger logger, DynamicMesh mesh, File output, String outputName, boolean exportTextures) {
        if (mesh == null)
            throw new NullPointerException("mesh");
        if (logger == null)
            logger = mesh.getLogger();
        if (output == null)
            throw new NullPointerException("directory");

        File outputDirectory;
        if (output.isDirectory()) {
            if (outputName == null)
                throw new NullPointerException("outputName");
            if (!FileUtils.isUntrustedInputValidFileName(outputName))
                throw new IllegalArgumentException("The provided outputName '" + outputName + "' was non alphanumeric!");

            outputDirectory = output;
            if (!output.exists())
                throw new RuntimeException("The given output directory '" + output + "' does not exist!");
        } else if (output.isFile() || !output.exists()) {
            outputName = FileUtils.stripExtension(output.getName());
            outputDirectory = output.getParentFile();
        } else {
            throw new IllegalArgumentException("Not sure what the path '" + output + "' is, wasn't either a file or a folder!");
        }

        logger.info("Exporting %s as %s.obj.", mesh.getMeshName(), outputName);

        String mtlName = outputName + ".mtl";
        File objFile = new File(outputDirectory, outputName + ".obj");
        StringBuilder objWriter = new StringBuilder();

        objWriter.append("# FrogLord Map Export").append(Constants.NEWLINE);
        objWriter.append("# Exported: ").append(Calendar.getInstance().getTime()).append(Constants.NEWLINE);
        objWriter.append("# Mesh Name: ").append(mesh.getMeshName()).append(" (").append(Utils.getSimpleName(mesh)).append(")").append(Constants.NEWLINE);
        objWriter.append(Constants.NEWLINE);

        if (exportTextures) {
            objWriter.append("mtllib ").append(mtlName).append(Constants.NEWLINE);
            objWriter.append(Constants.NEWLINE);
        }

        // Write Vertices.
        int vertexSize = mesh.getEditableVertices().getElementsPerUnit();
        if (vertexSize != 3)
            throw new RuntimeException("Strange vertexSize! Expected 3 values, but got " + vertexSize + "!");

        objWriter.append("# Vertices").append(Constants.NEWLINE);
        for (int i = 0; i < mesh.getEditableVertices().size(); i += vertexSize) {
            objWriter.append("v");
            objWriter.append(" ").append(mesh.getEditableVertices().get(i));
            objWriter.append(" ").append(mesh.getEditableVertices().get(i + 2));
            objWriter.append(" ").append(-mesh.getEditableVertices().get(i + 1));
            objWriter.append(Constants.NEWLINE);
        }

        // Register textures.
        if (exportTextures) {
            int texCoordSize = mesh.getEditableTexCoords().getElementsPerUnit();
            if (texCoordSize != 2)
                throw new RuntimeException("Strange texCoordSize! Expected 2 values, but got " + texCoordSize + "!");

            objWriter.append("# Vertex Textures").append(Constants.NEWLINE);
            for (int i = 0; i < mesh.getEditableTexCoords().size(); i += texCoordSize) {
                objWriter.append("vt");
                objWriter.append(" ").append(mesh.getEditableTexCoords().get(i));
                objWriter.append(" ").append(1F - mesh.getEditableTexCoords().get(i + 1)); // Flipping the Y seems to work JavaFX format conversion to obj files.
                objWriter.append(Constants.NEWLINE);
            }

            objWriter.append(Constants.NEWLINE);
        }

        // Write Faces.
        objWriter.append("# Faces").append(Constants.NEWLINE);
        if (exportTextures)
            objWriter.append("usemtl main_texture_sheet").append(Constants.NEWLINE);

        // Write Faces.
        int faceTotalElements = mesh.getEditableFaces().getElementsPerUnit();
        int faceElementsPerVertex = mesh.getVertexFormat().getVertexIndexSize();
        int vertexOffset = mesh.getEditableVertices().getVertexOffset();
        int texCoordOffset = mesh.getEditableTexCoords().getVertexOffset();

        objWriter.append("# Faces").append(Constants.NEWLINE);
        for (int i = 0; i < mesh.getEditableFaces().size(); i += faceTotalElements) {
            objWriter.append("f");
            for (int j = 0; j < faceTotalElements; j += faceElementsPerVertex) {
                objWriter.append(" ").append(mesh.getEditableFaces().get(i + j + vertexOffset) + 1);
                if (exportTextures && texCoordOffset >= 0)
                    objWriter.append("/").append(mesh.getEditableFaces().get(i + j + texCoordOffset) + 1);
            }
            objWriter.append(Constants.NEWLINE);
        }

        // Write MTL file and textures.
        if (exportTextures) {
            String mainTextureSheetFileName = outputName + ".png";

            try {
                ImageIO.write(SwingFXUtils.fromFXImage(mesh.getTextureAtlas().getFxImage(), null), "png", new File(outputDirectory, mainTextureSheetFileName));
            } catch (IOException ex) {
                Utils.handleError(logger, ex, true, "Failed to save texture sheet %s for .obj file.", mainTextureSheetFileName);
                return;
            }

            // Generate MTL File.
            StringBuilder mtlWriter = new StringBuilder();
            mtlWriter.append("newmtl main_texture_sheet").append(Constants.NEWLINE);
            mtlWriter.append("Kd 1 1 1").append(Constants.NEWLINE); // Diffuse color.
            mtlWriter.append("map_Kd ").append(mainTextureSheetFileName).append(Constants.NEWLINE);
            mtlWriter.append(Constants.NEWLINE);

            // Write MTL File.
            if (!FileUtils.writeStringToFile(logger, new File(outputDirectory, mtlName), mtlWriter.toString(), true))
                return;
        }

        if (FileUtils.writeStringToFile(logger, objFile, objWriter.toString(), true))
            logger.info("Export complete.");
    }

}
