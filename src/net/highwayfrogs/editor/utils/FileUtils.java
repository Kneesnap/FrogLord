package net.highwayfrogs.editor.utils;

import javafx.scene.control.Alert.AlertType;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import lombok.Cleanup;
import lombok.Getter;
import lombok.NonNull;
import lombok.SneakyThrows;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.FrogLordApplication;
import net.highwayfrogs.editor.games.generic.GameInstance;
import net.highwayfrogs.editor.system.Config;
import net.highwayfrogs.editor.system.Config.ConfigValueNode;
import net.highwayfrogs.editor.utils.data.reader.DataReader;
import net.highwayfrogs.editor.utils.data.writer.ArrayReceiver;
import net.highwayfrogs.editor.utils.data.writer.DataWriter;
import net.highwayfrogs.editor.utils.logging.ILogger;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.function.BiPredicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Contains static utilities for working with files.
 * Created by Kneesnap on 10/25/2024.
 */
public class FileUtils {
    public static final BrowserFileType IMPORT_IMAGE_FILE_TYPE = new BrowserFileType("Image File", ImageIO.getReaderFileSuffixes());
    public static final BrowserFileType EXPORT_IMAGE_FILE_TYPE = new BrowserFileType("Image File", ImageIO.getWriterFileSuffixes());
    public static final SavedFilePath IMPORT_SINGLE_IMAGE_PATH = new SavedFilePath("singleImageFileImportPath", "Please select the image file to open.", IMPORT_IMAGE_FILE_TYPE);
    public static final SavedFilePath EXPORT_SINGLE_IMAGE_PATH = new SavedFilePath("singleImageFileExportPath", "Please select the file to save the image as...", EXPORT_IMAGE_FILE_TYPE);
    private static final File[] EMPTY_FILE_ARRAY = new File[0];

    /**
     * Gets the JAR file representing FrogLord.
     */
    public static File getFrogLordJar() {
        return getFileFromURL(Utils.class.getProtectionDomain().getCodeSource().getLocation());
    }

    /**
     * Get a resource in the JAR.
     * @param resourcePath The resource path.
     * @param includeSubFolders if true, sub folders will be included.
     * @return resourceURL
     */
    @SneakyThrows
    public static List<URL> getInternalResourceFilesInDirectory(URL resourcePath, boolean includeSubFolders) {
        if (resourcePath.getProtocol() != null && resourcePath.getProtocol().equalsIgnoreCase("jar")) {
            String fullResourcePath = resourcePath.getFile();
            int exclamationPos = fullResourcePath.indexOf('!');
            if (exclamationPos < 0) {
                FXUtils.makePopUp("Couldn't find the JAR-embedded file resource path in the URL '" + resourcePath + "'.", AlertType.ERROR);
                return Collections.emptyList();
            }

            String localResourcePath = fullResourcePath.substring(exclamationPos + 1);
            File frogLordJar = getFrogLordJar();
            if (!frogLordJar.exists())
                throw new RuntimeException("Failed to find resource files at '" + localResourcePath + "', we resolved the FrogLord jar file to '" + frogLordJar + "', which did not exist. (" + resourcePath + ")");

            BiPredicate<Path, BasicFileAttributes> pathValidityCheck = (path, attributes) -> path.startsWith(localResourcePath);
            try (FileSystem fs = FileSystems.newFileSystem(frogLordJar.toPath(), Utils.class.getClassLoader())) {
                List<URL> foundResourceUrls = new ArrayList<>();

                // Test the path for resource files.
                for (Path root : fs.getRootDirectories()) {
                    try (Stream<Path> stream = Files.find(root, Integer.MAX_VALUE, pathValidityCheck)) {
                        foundResourceUrls.addAll(getUrlsFromPaths(stream, true));
                    } catch (Throwable th) {
                        Utils.handleError(null, th, false, "Failed to test the path '%s' for internal resource files.", root);
                    }
                }

                return removeFolders(foundResourceUrls);
            } catch (Throwable th) {
                Utils.handleError(null, th, false, "Failed to get the FileSystem object for the FrogLord jar: '%s'", frogLordJar);
            }

            throw new RuntimeException("Failed to enumerate resource files in '" + resourcePath + "'.");
        }

        // We should only get here when running from an IDE. (Or if there's some other version FrogLord would be run outside a jar?)
        try {
            Path path = Paths.get(resourcePath.toURI());
            try (Stream<Path> stream = Files.walk(path, includeSubFolders ? Integer.MAX_VALUE : 1)) {
                return removeFolders(getUrlsFromPaths(stream, false));
            }
        } catch (URISyntaxException | IOException ex) {
            throw new RuntimeException("Failed to get files in resource directory '" + resourcePath + "'", ex);
        }
    }

    private static List<URL> removeFolders(List<URL> list) {
        return list.stream()
                .filter(url -> url != null && !url.getPath().endsWith("/"))
                .collect(Collectors.toList());
    }

    private static List<URL> getUrlsFromPaths(Stream<Path> stream, boolean remakeResources) {
        Stream<URL> urlStream = stream.map(Path::toUri)
                .map(uri -> {
                    try {
                        return uri.toURL();
                    } catch (Throwable th) {
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .filter(url -> !url.getPath().endsWith("/")); // Remove directories.

        // This part is necessary since the urls we get from the file walker aren't actually valid for to open for some reason.
        // So, we take the file paths we get, and then feed them back into something we know we can get the paths from.
        if (remakeResources) {
            urlStream = urlStream.map(url -> {
                String fullResourcePath = url.getFile();
                int exclamationPos = fullResourcePath.indexOf('!');
                if (exclamationPos < 0)
                    throw new RuntimeException("Couldn't find the JAR-embedded file resource path in the URL '" + url + "'/'" + fullResourcePath + "'.");

                String localResourcePath = fullResourcePath.substring(exclamationPos + (fullResourcePath.charAt(exclamationPos + 1) == '/' ? 2 : 1));
                URL convertedURL = getResourceURL(localResourcePath);
                if (convertedURL == null)
                    throw new RuntimeException("Failed to convert local resource path '" + localResourcePath + "' from URL '" + url + "' into usable resource path URL.");

                return convertedURL;
            });
        }

        return urlStream.collect(Collectors.toList());
    }

    /**
     * Often times we have improperly formatted URLs in java. Unfortunately, this comes with characters like ' ' replaced with %20, which can cause headaches when working with the file system.
     * <a href="https://web.archive.org/web/20100327174235/http://weblogs.java.net/blog/kohsuke/archive/2007/04/how_to_convert.html"/>
     * This is our attempt at fixing the problem.
     * @param url the url to resolve a file for
     * @return validFile
     */
    public static File getFileFromURL(URL url) {
        if (!url.getProtocol().equalsIgnoreCase("file") && !url.getProtocol().equalsIgnoreCase("jar"))
            throw new UnsupportedOperationException("Cannot get file from URL with protocol '" + url.getProtocol() + "'. (" + url + ")");

        try {
            return new File(url.toURI());
        } catch(URISyntaxException e) {
            return new File(url.getPath());
        }
    }

    /**
     * Get a resource in the JAR.
     * @param resourceName The resource name.
     * @return resourceURL
     */
    public static URL getResourceURL(String resourceName) {
        return Utils.class.getClassLoader().getResource(resourceName);
    }

    /**
     * Get a JAR resource as a stream.
     * @param resourceName The name of the resource to load.
     * @return resourceStream
     */
    public static InputStream getResourceStream(String resourceName) {
        return Utils.class.getClassLoader().getResourceAsStream(resourceName);
    }

    /**
     * Read lines of text from an InputStream
     * @param stream The stream to read from.
     * @return lines
     */
    @SneakyThrows
    public static List<String> readLinesFromStream(InputStream stream) {
        @Cleanup InputStreamReader reader = new InputStreamReader(stream);
        @Cleanup BufferedReader bufferedReader = new BufferedReader(reader);
        return bufferedReader.lines().collect(Collectors.toList());
    }

    /**
     * Read lines of text from a file.
     * @param file The file to read from.
     * @return fileText
     */
    public static List<String> readLinesFromFile(File file) {
        try {
            return Files.readAllLines(file.toPath());
        } catch (IOException e) {
            Utils.handleError(null, e, false, "Failed to read text lines from file '%s'", file);
            return Collections.emptyList();
        }
    }

    /**
     * Read text from a file.
     * @param file The file to read from.
     * @return fileText
     */
    public static String readFileText(File file) {
        return String.join(Constants.NEWLINE, readLinesFromFile(file));
    }

    /**
     * Read bytes from an InputStream
     * Stolen from sun.nio.ch
     * @param stream The stream to read from.
     * @return lines
     */
    @SneakyThrows
    public static byte[] readBytesFromStream(InputStream stream) {
        byte[] output = new byte[0];
        int var1 = Integer.MAX_VALUE;

        int var6;
        for (int var4 = 0; var4 < var1; var4 += var6) {
            int var5;
            if (var4 >= output.length) {
                var5 = Math.min(var1 - var4, output.length + 1024);
                if (output.length < var4 + var5) {
                    output = Arrays.copyOf(output, var4 + var5);
                }
            } else {
                var5 = output.length - var4;
            }

            var6 = stream.read(output, var4, var5);
            if (var6 < 0) {
                if (output.length != var4)
                    output = Arrays.copyOf(output, var4);
                break;
            }
        }

        return output;
    }

    /**
     * Read bytes from an InputStream, and writes them to an output stream.
     * @param input  The stream to read from.
     * @param output The stream to write to.
     */
    public static void copyInputStreamData(InputStream input, OutputStream output, boolean closeInput) {
        byte[] buffer = new byte[4096];

        try {
            int bytesRead;
            while ((bytesRead = input.read(buffer)) != -1)
                output.write(buffer, 0, bytesRead);
        } catch (IOException ex) {
            Utils.handleError(null, ex, false, "Failed to copy stream data from the input stream to the output stream!");
        }

        if (closeInput) {
            try {
                input.close();
            } catch (IOException ex) {
                Utils.handleError(null, ex, false, "Failed to close the input stream.");
            }
        }
    }

    /**
     * Get the first file with this name that does not exist. Appends stuff like (1).
     * @param file The file to get.
     * @return nonexistentFile.
     */
    public static File getNonExistantFile(File file) {
        if (!file.exists())
            return file;

        int id = 0;

        File result = file;
        while (result.exists())
            result = getFile(file, ++id);

        return result;
    }

    private static File getFile(File file, int id) {
        if (id == 0)
            return file;

        String fileName = file.getName();
        String name = fileName.replaceFirst("[.][^.]+$", ""); // Remove extension.
        name += " (" + id + ")";
        if (fileName.contains("."))
            name += fileName.substring(fileName.lastIndexOf(".") + 1);

        return new File(file.getParentFile(), name);
    }

    /**
     * Strip the extension from a file name.
     * @param name The file name.
     * @return stripped
     */
    public static String stripExtension(String name) {
        return name.split("\\.")[0];
    }

    /**
     * Strip the extension from a file name.
     * @param name The file name.
     * @return stripped
     */
    public static String stripSingleExtension(String name) {
        int lastDotIndex = name.lastIndexOf('.');
        return lastDotIndex >= 0 ? name.substring(0, lastDotIndex) : name;
    }

    /**
     * Get the file name from the url.
     * @param url the url to get the file name from
     * @return fileName
     */
    public static String getFileName(URL url) {
        if (url == null)
            throw new NullPointerException("url");
        String query = url.getQuery();
        if (query != null)
            return query;

        String fullPath = url.getFile();
        int backslashPos = fullPath.lastIndexOf('/');
        return backslashPos >= 0 ? fullPath.substring(backslashPos + 1) : fullPath;
    }

    /**
     * Get the file name from the url.
     * @param url the url to get the file name from
     * @return fileName
     */
    public static String getFileNameWithoutExtension(URL url) {
        return stripSingleExtension(getFileName(url));
    }

    /**
     * Deletes a file.
     * @param file The file to delete.
     */
    @SuppressWarnings("ResultOfMethodCallIgnored")
    public static void deleteFile(File file) {
        if (file.exists())
            file.delete();
    }

    /**
     * Find the valid folder, for instance maybe the file was deleted.
     * @param folder The folder to get a valid one from.
     * @return validFolder
     */
    public static File getValidFolder(File folder) {
        if (folder != null && folder.exists() && folder.isDirectory())
            return folder;

        return folder != null ? getValidFolder(folder.getParentFile()) : new File(FrogLordApplication.getMainApplicationFolder(), "./");
    }

    /**
     * A null-safe way of reading files from a directory.
     * @param directory The directory to read files from.
     * @return readFiles
     */
    public static File[] listFiles(File directory) {
        Utils.verify(directory.isDirectory(), "This is not a directory!");
        File[] files = directory.listFiles();
        return files != null ? files : EMPTY_FILE_ARRAY;
    }

    /**
     * Create the directory.
     * @param directory The directory to create.
     */
    @SuppressWarnings("ResultOfMethodCallIgnored")
    public static void makeDirectory(File directory) {
        if (!directory.exists())
            directory.mkdirs();
    }

    /**
     * Tests if a file is located within a folder. Restricts directory escalation.
     * @param targetFile The target file to test.
     * @param holdingFolder The folder which must hold the file.
     * @return Whether the file is held.
     */
    public static boolean isFileWithinParent(File targetFile, File holdingFolder) {
        if (targetFile == null)
            throw new RuntimeException("The folder to test is null.");
        if (holdingFolder == null)
            throw new RuntimeException("The holding folder is null.");
        if (!holdingFolder.isDirectory() || holdingFolder.isFile())
            throw new RuntimeException("The holding folder '" + holdingFolder.getName() + "' is actually not a folder.");

        // Note: Make sure that if holdingFolder == targetFile, that still returns false.
        File testFile;
        try {
            testFile = targetFile.getCanonicalFile().getParentFile(); // Evaluates everything like '..\', drive letters, etc.
            holdingFolder = holdingFolder.getCanonicalFile(); // Necessary to make .equals() work.
        } catch (IOException ex) {
            // Probably not accessible due to permissions, or, it's an invalid file path which by definition can't be evaluated.
            return false;
        }

        // Search parent files.
        while (testFile != null) {
            if (testFile.equals(holdingFolder))
                return true;

            testFile = testFile.getParentFile();
        }

        // Didn't find a match.
        return false;
    }

    /**
     * Gets the file as a path local to the server root. This is mainly for displays.
     * @param rootFolder The folder to treat as the root.
     * @param file The file to get the path of.
     * @return localPathString
     */
    public static String toLocalPath(File rootFolder, File file, boolean allowOutsideRoot) {
        if (file == null)
            return "NULL FILE";
        if (rootFolder == null)
            throw new NullPointerException("rootFolder");
        if (!rootFolder.isDirectory())
            throw new RuntimeException("The provided rootFolder: '" + rootFolder + "' was not a directory!");

        if (!isFileWithinParent(file, rootFolder)) {
            if (allowOutsideRoot)
                return file.getPath();

            throw new RuntimeException("File '" + file + "' was not found within '" + rootFolder + "'.");
        }

        String rootPath;
        try {
            rootPath = rootFolder.getCanonicalPath();
        } catch (IOException ex) {
            Utils.getLogger().severe("Failure path: '" + rootFolder + "'");
            throw new RuntimeException("Failed to get canonical path of file.", ex);
        }

        String targetPath;
        try {
            targetPath = file.getCanonicalPath();
        } catch (IOException ex) {
            Utils.getLogger().severe("Failure path: '" + rootFolder + "'");
            throw new RuntimeException("Failed to get canonical path of target file.", ex);
        }

        // Strip file path.
        String resultPath = targetPath;
        if (resultPath.startsWith(rootPath))
            resultPath = resultPath.substring(rootPath.length() + 1);

        return resultPath;
    }

    /**
     * Adds a suffix to the file name, before the extension.
     * @param file the file to add a suffix to
     * @param suffix the suffix to add
     * @return the new file with the suffix applied
     */
    public static File addFileNameSuffix(File file, String suffix) {
        if (file == null)
            throw new NullPointerException("file");
        if (suffix == null || suffix.isEmpty())
            return file;

        String inputFileName = file.getName();
        String strippedInputName = stripExtension(inputFileName);
        return new File(file.getParentFile(), strippedInputName + suffix + inputFileName.substring(strippedInputName.length() + 1));
    }

    /**
     * Ensures input string is using the path separator for the system currently running FrogLord.
     * @param input the string containing potentially incorrect path separators
     * @param removeLeadingSeparator if true, any path separators at the start of the string will be removed.
     * @return processed string
     */
    public static String ensureValidPathSeparator(String input, boolean removeLeadingSeparator) {
        if (input == null || input.length() == 0)
            return input;

        String result = input;
        if (File.separatorChar != '\\')
            result = result.replace('\\', File.separatorChar);
        if (File.separatorChar != '/')
            result = result.replace('/', File.separatorChar);

        if (removeLeadingSeparator)
            while (result.length() > 0 && result.charAt(0) == File.separatorChar)
                result = result.substring(1);

        return result;
    }

    /**
     * Gets the file name extension from the file name.
     * @param fileName the file name to get the extension from
     * @return fileNameExtension, if there is one
     */
    public static String getFileNameExtension(String fileName) {
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex < 0)
            return null;

        return fileName.substring(dotIndex + 1);
    }

    /**
     * Gets the file name extension from the file name in lower-case form.
     * @param fileName the file name to get the extension from
     * @return fileNameExtension, if there is one
     */
    public static String getFileNameExtensionLower(String fileName) {
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex < 0)
            return null;

        return fileName.substring(dotIndex + 1).toLowerCase();
    }

    /**
     * Safely writes the given string to a file, replacing the file if it already exists.
     * The string will be written to the file as a UTF-8 byte-array, and will fully overwrite any existing contents of the file.
     * If an exception occurs during the writing of the file, false will be returned, and the error will be handled
     * @param logger the logger to write any error to. If null is provided, the util logger will be used.
     * @param outputFile The file to write the data to
     * @param value The string to write to the file
     * @param showPopupOnError If true and an error occurs, a popup will be displayed.
     * @return true iff the file was successfully written
     */
    public static boolean writeStringToFile(ILogger logger, File outputFile, String value, boolean showPopupOnError) {
        if (value == null)
            throw new NullPointerException("value");

        return writeBytesToFile(logger, outputFile, value.getBytes(StandardCharsets.UTF_8), showPopupOnError);
    }


    /**
     * Safely writes the given bytes to a file, replacing the file if it already exists.
     * If an exception occurs during the writing of the file, false will be returned, and the error will be handled
     * @param logger the logger to write any error to. If null is provided, the util logger will be used.
     * @param outputFile The file to write the data to
     * @param bytes The bytes to write to the file
     * @param showPopupOnError If true and an error occurs, a popup will be displayed.
     * @return true iff the file was successfully written
     */
    public static boolean writeBytesToFile(ILogger logger, File outputFile, byte[] bytes, boolean showPopupOnError) {
        if (outputFile == null)
            throw new NullPointerException("outputFile");
        if (bytes == null)
            throw new NullPointerException("bytes");

        if (outputFile.exists() && !outputFile.isFile())
            throw new IllegalArgumentException("'" + outputFile + "' is not a valid file!");

        File folder = outputFile.getParentFile();
        if (!folder.exists())
            throw new IllegalArgumentException("The path to '" + outputFile + "' did not exist, therefore the file cannot be written.");

        try {
            if (!folder.canWrite() && !folder.setWritable(true)) // We want it to properly create popups based on thread/etc., since this error is one which is likely the user's responsibility.
                throw new IOException("Can't write to the file '" + outputFile.getName() + "'." + Constants.NEWLINE + "Check that you have permission to save to this folder.");

            if (outputFile.isFile() && outputFile.exists() && !outputFile.canWrite() && !outputFile.setWritable(true)) // TODO: Consider changing this to a Yes/No popup, with the ability to accept all
                throw new IOException("Can't write to the file '" + outputFile.getName() + "'." + Constants.NEWLINE + "Check that you have permission to write to this file.");

            if (outputFile.exists() && !outputFile.setLastModified(System.currentTimeMillis()))
                throw new IOException("Failed to update the last modified date for '" + outputFile.getName() + "'.");

            Files.write(outputFile.toPath(), bytes);
            return true;
        } catch (IOException ex) {
            Utils.handleError(logger, ex, showPopupOnError, "Failed to save file '%s'.", outputFile.getName());
            return false;
        }
    }

    /**
     * Tests if a string is alphanumeric or not.
     * @param testString The string to test.
     * @return isAlphanumeric
     */
    @SuppressWarnings("ResultOfMethodCallIgnored")
    public static boolean isValidFileName(String testString) {
        File f = new File(testString);
        try {
            f.getCanonicalPath();
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    @Getter
    public static final class SavedFilePath {
        @NonNull private final String configKeyName;
        @NonNull private final String title;
        private final List<BrowserFileType> fileTypes = new ArrayList<>();

        public SavedFilePath(@NonNull String configKeyName, @NonNull String title, BrowserFileType... fileTypes) {
            this.configKeyName = configKeyName;
            this.title = title;
            if (fileTypes != null && fileTypes.length > 0)
                this.fileTypes.addAll(Arrays.asList(fileTypes));
        }

        private Config getSavedPaths(GameInstance instance) {
            if (instance == null)
                throw new NullPointerException("instance");
            return instance.getConfig().getOrCreateChildConfigByName("FilePaths");
        }

        /**
         * Gets the folder to open a prompt within.
         * @param instance the instance to get the start folder from
         * @return startFolder
         */
        public File getLastPath(GameInstance instance) {
            if (instance == null)
                return FrogLordApplication.getWorkingDirectory();

            Config savedPaths = getSavedPaths(instance);
            ConfigValueNode node = savedPaths.getOrCreateKeyValueNode(this.configKeyName);

            File prevFile;
            if (!StringUtils.isNullOrWhiteSpace(node.getAsString())) {
                prevFile = new File(node.getAsString());
            } else {
                prevFile = FrogLordApplication.getWorkingDirectory();
            }

            return prevFile;
        }

        /**
         * Sets the folder to open a prompt within for next time.
         * @param instance the instance to set the start folder for
         */
        public void setResult(GameInstance instance, File result) {
            if (instance == null)
                return;
            if (result == null)
                throw new NullPointerException("result");

            Config savedPaths = getSavedPaths(instance);
            ConfigValueNode node = savedPaths.getOrCreateKeyValueNode(this.configKeyName);

            try {
                node.setAsString(result.getCanonicalPath());
            } catch (IOException ex) {
                Utils.handleError(instance.getLogger(), ex, false, "Failed to get '%s' as a canonical path.", result);
            }
        }
    }

    @Getter
    public static final class BrowserFileType {
        private final String typeDescription;
        private final List<String> extensions = new ArrayList<>();

        public static final BrowserFileType ALL_FILES = new BrowserFileType("All Files", "*");

        public BrowserFileType(String typeDescription, String... extensions) {
            this.typeDescription = typeDescription;

            for (int i = 0; i < extensions.length; i++) {
                String tempExtension = extensions[i];
                String type = tempExtension.contains(".") ? tempExtension : "*." + tempExtension; // Unix is case-sensitive, so we add both lower-case and upper-case.
                String lowerCase = type.toLowerCase();
                String upperCase = type.toUpperCase();

                if (lowerCase.equals(upperCase) || ((lowerCase.startsWith("sl") || lowerCase.startsWith("sc")) && lowerCase.endsWith("_*.*"))) { // Ignore 'SLUS', etc.
                    this.extensions.add(type);
                } else {
                    this.extensions.add(lowerCase);
                    this.extensions.add(upperCase);
                }
            }
        }
    }

    /**
     * Prompt the user to select a file.
     * @param instance The game instance to find the file path saved within.
     * @param savedPath The information on how to obtain the saved path, as well as file extensions.
     * @return selectedFile, or null if the user cancelled the prompt
     */
    public static File askUserToOpenFile(GameInstance instance, SavedFilePath savedPath) {
        if (savedPath == null)
            throw new NullPointerException("savedPath");

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle(savedPath.getTitle());
        for (BrowserFileType fileType : savedPath.getFileTypes())
            fileChooser.getExtensionFilters().add(new ExtensionFilter(fileType.getTypeDescription(), fileType.getExtensions()));

        File lastDirectory = FileUtils.getValidFolder(savedPath.getLastPath(instance));
        fileChooser.setInitialDirectory(lastDirectory);
        File selectedFile = fileChooser.showOpenDialog(instance != null ? instance.getMainStage() : null);
        if (selectedFile != null) {
            savedPath.setResult(instance, selectedFile);
            FrogLordApplication.setWorkingDirectory(selectedFile.getParentFile());
        }

        return selectedFile;
    }

    /**
     * Asks the user to provide the image file, then it will be imported.
     * @param logger The logger to provide in-case of failure.
     * @param instance The game instance to save the image path to.
     * @return image, if successfully loaded
     */
    public static BufferedImage askUserToOpenImageFile(ILogger logger, GameInstance instance) {
        File selectedFile = askUserToOpenFile(instance, IMPORT_SINGLE_IMAGE_PATH);
        if (selectedFile == null)
            return null;

        try {
            return ImageIO.read(selectedFile);
        } catch (IOException ex) {
            Utils.handleError(logger, ex, true, "Failed to load image from file '%s'.", selectedFile.getName());
            return null;
        }
    }

    /**
     * Prompt the user to select a file.
     * @param instance The game instance to find the file path saved within.
     * @param savedPath The information on how to obtain the saved path, as well as file extensions.
     * @return selectedFile, or null if the user cancelled the prompt
     */
    public static File askUserToSaveFile(GameInstance instance, SavedFilePath savedPath, String suggestedFileName) {
        return askUserToSaveFile(instance, savedPath, suggestedFileName, false);
    }

    /**
     * Prompt the user to select a file.
     * @param instance The game instance to find the file path saved within.
     * @param savedPath The information on how to obtain the saved path, as well as file extensions.
     * @return selectedFile, or null if the user cancelled the prompt
     */
    public static File askUserToSaveFile(GameInstance instance, SavedFilePath savedPath, String suggestedFileName, boolean overrideLastFileName) {
        if (savedPath == null)
            throw new NullPointerException("savedPath");

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle(savedPath.getTitle());
        for (BrowserFileType fileType : savedPath.getFileTypes())
            fileChooser.getExtensionFilters().add(new ExtensionFilter(fileType.getTypeDescription(), fileType.getExtensions()));

        File lastFile = savedPath.getLastPath(instance);
        File lastDirectory = FileUtils.getValidFolder(savedPath.getLastPath(instance));
        fileChooser.setInitialDirectory(lastDirectory);

        if (!overrideLastFileName && lastFile != null && lastFile.isFile()) {
            fileChooser.setInitialFileName(lastFile.getName());
        } else if (savedPath.getFileTypes().size() > 0){
            String startFileName = suggestedFileName;
            String extension = getFileNameExtension(savedPath.getFileTypes().get(0).getExtensions().get(0));
            if (extension != null && !extension.equals("*") && (startFileName == null || !startFileName.contains(".")))
                startFileName += "." + extension;

            fileChooser.setInitialFileName(startFileName);
        }

        File selectedFile = fileChooser.showSaveDialog(instance != null ? instance.getMainStage() : null);
        if (selectedFile == null)
            return null;

        if (selectedFile.isFile() && selectedFile.exists() && !selectedFile.canWrite() && !selectedFile.setWritable(true)) {
            FXUtils.makePopUp("Can't write to the file '" + selectedFile.getName() + "'." + Constants.NEWLINE + "Check that you have permission to write to this file.", AlertType.ERROR);
            return askUserToSaveFile(instance, savedPath, suggestedFileName, overrideLastFileName);
        }

        if (selectedFile.exists() && !selectedFile.setLastModified(System.currentTimeMillis())) {
            FXUtils.makePopUp("Can't write to the file '" + selectedFile.getName() + "'." + Constants.NEWLINE + "Check that you have permission to write to this file.", AlertType.ERROR);
            return askUserToSaveFile(instance, savedPath, suggestedFileName, overrideLastFileName);
        }

        savedPath.setResult(instance, selectedFile);
        FrogLordApplication.setWorkingDirectory(selectedFile.getParentFile());
        return selectedFile;
    }

    /**
     * Asks the user where to save the image file, then saves it there.
     * @param logger The logger to provide in-case of failure.
     * @param instance The game instance to save the image path to.
     * @param image The image to save
     * @param suggestedFileName The suggested file name
     * @return file, if successfully saved
     */
    public static File askUserToSaveImageFile(ILogger logger, GameInstance instance, BufferedImage image, String suggestedFileName) {
        boolean hasSuggestedFileName = suggestedFileName != null && !suggestedFileName.trim().isEmpty();
        if (!hasSuggestedFileName)
            suggestedFileName = "unknown";

        // Add extension.
        String[] extensionSuffixes = ImageIO.getWriterFileSuffixes();
        String providedExtension = FileUtils.getFileNameExtension(suggestedFileName);
        if (providedExtension == null || !Utils.contains(extensionSuffixes, providedExtension.toLowerCase()))
            suggestedFileName += "." + (Utils.contains(extensionSuffixes, "png") ? "png" : extensionSuffixes[0]);

        File selectedFile = askUserToSaveFile(instance, EXPORT_SINGLE_IMAGE_PATH, suggestedFileName, hasSuggestedFileName);
        if (selectedFile == null)
            return null;

        String extension = FileUtils.getFileNameExtensionLower(selectedFile.getName());
        if (extension == null || !Utils.contains(extensionSuffixes, extension)) {
            FXUtils.makePopUp("Couldn't determine which image format to use for the file extension '." + extension + "'.", AlertType.ERROR);
            return null;
        }

        try {
            if (!ImageIO.write(image, extension, selectedFile)) {
                FXUtils.makePopUp("No image writer was available for the ." + extension + " image type.", AlertType.ERROR);
                return null;
            }
        } catch (IOException ex) {
            Utils.handleError(logger, ex, true, "Failed to save image to file '%s'.", selectedFile.getName());
            return null;
        }

        if (!selectedFile.exists() || !selectedFile.isFile()) {
            FXUtils.makePopUp("The image was expected to have been written to the file, but wasn't!!", AlertType.ERROR);
            return null;
        }

        return selectedFile;
    }

    /**
     * Prompt the user to select a directory.
     * @param instance The game instance to find the file path saved within.
     * @param savedPath The information on how to obtain the saved path, as well as file extensions.
     * @return directoryFile, or null if the user cancelled the prompt
     */
    public static File askUserToSelectFolder(GameInstance instance, SavedFilePath savedPath) {
        if (savedPath == null)
            throw new NullPointerException("savedPath");
        if (!savedPath.getFileTypes().isEmpty())
            throw new IllegalArgumentException("The provided SavedBrowser path had file types set, making it unusable for a directory picker!");

        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle(savedPath.getTitle());

        File lastDirectory = FileUtils.getValidFolder(savedPath.getLastPath(instance));
        chooser.setInitialDirectory(lastDirectory);

        File selectedFolder = chooser.showDialog(instance != null ? instance.getMainStage() : null);
        if (selectedFolder != null) {
            savedPath.setResult(instance, selectedFolder);
            FrogLordApplication.setWorkingDirectory(selectedFolder);
        }

        return selectedFolder;
    }

    /**
     * Tests if a string is a safe file name or not.
     * @param testString The string to test.
     * @return isSafeFileName
     */
    public static boolean isUntrustedInputValidFileName(String testString) {
        for (int i = 0; i < testString.length(); i++) {
            char temp = testString.charAt(i);
            if (!Character.isLetterOrDigit(temp) && temp != '_' && temp != ' ' && temp != '-' && temp != '.')
                return false;
        }

        return true;
    }

    /**
     * Test if the given file appears to be a PSX/PS2 CD ISO image.
     * @param file the file to test
     * @return true iff the iso appears to be an iso image
     */
    public static boolean isProbablySonyIso(File file) {
        if (file == null)
            throw new NullPointerException("file");
        if (!file.isFile() || !file.exists())
            throw new RuntimeException("Invalid file: " + file);

        final int sector = 16;
        final int signatureOffset = 24;

        // Check if file has a valid ISO9660 header.
        if (file.length() <= (sector * Constants.SONY_ISO_CD_SECTOR_SIZE) + signatureOffset + Constants.SONY_ISO_CD_SIGNATURE_BYTES.length)
            return false;

        try (RandomAccessFile fileReader = new RandomAccessFile(file, "r")) {
            return isProbablySonyIso(fileReader);
        } catch (Throwable th) {
            Utils.handleError(null, th, true);
            return false;
        }
    }

    /**
     * Test if the given file appears to be a PSX/PS2 CD ISO image.
     * @param file the file to test
     * @return true iff the iso appears to be an iso image
     */
    @SneakyThrows
    public static boolean isProbablySonyIso(RandomAccessFile file) {
        if (file == null)
            throw new NullPointerException("file");

        final int sector = 16;
        final int signatureOffset = 24;

        // Check if file has a valid ISO9660 header.
        if (file.length() > (sector * Constants.SONY_ISO_CD_SECTOR_SIZE) + signatureOffset + Constants.SONY_ISO_CD_SIGNATURE_BYTES.length) {
            file.seek((sector * Constants.SONY_ISO_CD_SECTOR_SIZE) + signatureOffset);
            byte[] testSignature = new byte[Constants.SONY_ISO_CD_SIGNATURE_BYTES.length];

            int bytesRead;
            if ((bytesRead = file.read(testSignature)) != testSignature.length)
                throw new RuntimeException("Failed to read enough bytes! (Expected " + testSignature.length + ", but actually read " + bytesRead + ".)");

            return Arrays.equals(testSignature, Constants.SONY_ISO_CD_SIGNATURE_BYTES);
        }

        return false;
    }

    private static final String FROGLORD_EXECUTABLE_SIGNATURE = "FROGLORD";
    private static final byte[] FROGLORD_EXECUTABLE_SIGNATURE_BYTES = FROGLORD_EXECUTABLE_SIGNATURE.getBytes(StandardCharsets.US_ASCII);
    private static final String PSX_EXECUTABLE_SIGNATURE = "PS-X EXE";
    private static final short PSX_EXE_HEADER_VERSION = 0; // Only increment this with a breaking change.
    private static final int PSX_BASIC_HEADER_SIZE = FROGLORD_EXECUTABLE_SIGNATURE_BYTES.length + 1;
    private static final String WINDOWS_PE_EXECUTABLE_SIGNATURE = "MZ";
    private static final short WINDOWS_EXE_HEADER_VERSION = 0; // Only increment this with a breaking change.

    /**
     * Reads configuration data from the executable.
     * @param executableReader the reader of executable data
     * @param executableFileName the name of the file to read from
     * @return parsedConfig
     */
    public static Config loadConfigDataFromExecutable(DataReader executableReader, String executableFileName) {
        if (executableReader == null)
            throw new NullPointerException("executableBytes");
        if (executableReader.getSize() < 16)
            throw new IllegalArgumentException("The executable is too small!");

        executableReader.setIndex(0);
        byte[] headerBytes = executableReader.readBytes(16);

        // Try to read any configuration stored in the executable.
        byte[] configBytes;
        if (DataUtils.testSignature(headerBytes, 0, WINDOWS_PE_EXECUTABLE_SIGNATURE)) { // PC
            // Test if the executable already has the identifier at the end.
            executableReader.setIndex(executableReader.getSize() - Constants.INTEGER_SIZE - FROGLORD_EXECUTABLE_SIGNATURE_BYTES.length - 1);
            byte version = executableReader.readByte();
            if ((version & 0xFF) > PSX_EXE_HEADER_VERSION)
                throw new RuntimeException("This executable was saved with a newer (incompatible) version of FrogLord, and is thus unsupported in this version.");

            int address = executableReader.readInt();
            byte[] signature = executableReader.readBytes(FROGLORD_EXECUTABLE_SIGNATURE_BYTES.length);
            if (!DataUtils.testSignature(signature, FROGLORD_EXECUTABLE_SIGNATURE))
                return null; // Nothing here.

            executableReader.setIndex(address);
            configBytes = executableReader.readBytes(executableReader.getRemaining() - Constants.INTEGER_SIZE - FROGLORD_EXECUTABLE_SIGNATURE_BYTES.length);
        } else if (DataUtils.testSignature(headerBytes, 0, PSX_EXECUTABLE_SIGNATURE)) { // PSX
            if (executableReader.getSize() < Constants.CD_SECTOR_SIZE)
                throw new RuntimeException("The executable is too small to be a PSX executable!");

            executableReader.setIndex(Constants.PSX_WRITABLE_START_OFFSET);
            byte[] signature = executableReader.readBytes(FROGLORD_EXECUTABLE_SIGNATURE_BYTES.length);
            if (!DataUtils.testSignature(signature, FROGLORD_EXECUTABLE_SIGNATURE))
                return null; // There's no data there.

            // Read after the header, but before the data begins.
            byte version = executableReader.readByte();
            if ((version & 0xFF) > PSX_EXE_HEADER_VERSION)
                throw new RuntimeException("This executable was saved with a newer (incompatible) version of FrogLord, and is thus unsupported in this version.");

            configBytes = executableReader.readBytes(Constants.PSX_WRITABLE_AREA_SIZE - PSX_BASIC_HEADER_SIZE);
        } else {
            throw new UnsupportedOperationException("loadConfigDataFromExecutable() doesn't support recognize the executable format.");
        }

        // Find the end of the string.
        int endIndex;
        for (endIndex = 0; endIndex < configBytes.length; endIndex++)
            if (configBytes[endIndex] == 0)
                break;

        // Read the build info, and use that to determine the version.
        // This only applies to versions of the game saved by FrogLord.
        String parsedConfigText = new String(configBytes, 0, endIndex, StandardCharsets.UTF_8);
        return Config.loadConfigFromString(parsedConfigText, executableFileName);
    }

    /**
     * Writes configuration data to the executable.
     * @param gameInstance the loaded game instance owning the executable
     * @param executableBytes the executable data to update
     * @param config the config data to write
     * @return updatedExecutableBytes
     */
    public static byte[] saveConfigDataToExecutable(GameInstance gameInstance, byte[] executableBytes, Config config) {
        if (gameInstance == null)
            throw new NullPointerException("gameInstance");
        if (executableBytes == null)
            throw new NullPointerException("executableBytes");

        byte[] bytes = config != null ? config.toString().getBytes(StandardCharsets.UTF_8) : new byte[0];

        if (gameInstance.isPC()) {
            // Test if the executable already has the identifier at the end.
            byte[] realExecutable;
            int frogLordSignatureStartIndex = executableBytes.length - FROGLORD_EXECUTABLE_SIGNATURE_BYTES.length;
            if (DataUtils.testSignature(executableBytes, frogLordSignatureStartIndex, FROGLORD_EXECUTABLE_SIGNATURE)) {
                // Remove previously appended executable data.
                int address = DataUtils.readIntFromBytes(executableBytes, frogLordSignatureStartIndex - Constants.INTEGER_SIZE);
                realExecutable = new byte[address];
                System.arraycopy(executableBytes, 0, realExecutable, 0, address);
            } else {
                realExecutable = executableBytes;
            }

            ArrayReceiver receiver = new ArrayReceiver();
            DataWriter writer = new DataWriter(receiver);
            writer.writeBytes(bytes);
            writer.writeByte(Constants.NULL_BYTE); // Terminate string.
            writer.writeUnsignedByte(WINDOWS_EXE_HEADER_VERSION); // Version.
            writer.writeInt(realExecutable.length);
            writer.writeStringBytes(FROGLORD_EXECUTABLE_SIGNATURE);
            writer.closeReceiver();

            // Generate a new array containing the executable and the extra data.
            byte[] appendBytes = receiver.toArray();
            byte[] newExecutableBytes = new byte[realExecutable.length + appendBytes.length];
            System.arraycopy(realExecutable, 0, newExecutableBytes, 0, realExecutable.length);
            System.arraycopy(appendBytes, 0, newExecutableBytes, realExecutable.length, appendBytes.length);
            return newExecutableBytes;
        } else if (gameInstance.isPSX()) {
            if (bytes.length + PSX_BASIC_HEADER_SIZE > Constants.PSX_WRITABLE_AREA_SIZE)
                throw new RuntimeException("Cannot save configuration to executable, it is too large to fit in the available area! (" + bytes.length + " > " + Constants.PSX_WRITABLE_AREA_SIZE + ")");

            // Write after the header, but before the data begins.
            int writeOffset = Constants.PSX_WRITABLE_START_OFFSET;
            System.arraycopy(FROGLORD_EXECUTABLE_SIGNATURE_BYTES, 0, executableBytes, writeOffset, FROGLORD_EXECUTABLE_SIGNATURE_BYTES.length);
            writeOffset += FROGLORD_EXECUTABLE_SIGNATURE_BYTES.length;;

            // Write version.
            executableBytes[writeOffset++] = (byte) PSX_EXE_HEADER_VERSION;

            // Write config.
            System.arraycopy(bytes, 0, executableBytes, writeOffset, bytes.length);
            writeOffset += bytes.length;

            // Write padding.
            Arrays.fill(executableBytes, writeOffset, Constants.CD_SECTOR_SIZE, Constants.NULL_BYTE);
            return executableBytes;
        } else {
            throw new UnsupportedOperationException("saveConfigDataToExecutable() doesn't support the " + gameInstance.getPlatform() + " platform yet.");
        }
    }
}