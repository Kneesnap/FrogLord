package net.highwayfrogs.editor.games.sony.frogger.utils;

import javafx.scene.control.Alert;
import net.highwayfrogs.editor.FrogLordApplication;
import net.highwayfrogs.editor.file.config.exe.ThemeBook;
import net.highwayfrogs.editor.file.vlo.ImageWorkHorse;
import net.highwayfrogs.editor.games.generic.GameInstance;
import net.highwayfrogs.editor.games.sony.SCGameFile;
import net.highwayfrogs.editor.games.sony.SCGameType;
import net.highwayfrogs.editor.games.sony.frogger.FroggerGameInstance;
import net.highwayfrogs.editor.games.sony.frogger.map.FroggerMapFile;
import net.highwayfrogs.editor.games.sony.frogger.map.FroggerMapTheme;
import net.highwayfrogs.editor.games.sony.frogger.map.mesh.FroggerMapMesh;
import net.highwayfrogs.editor.games.sony.shared.mof2.MRModel;
import net.highwayfrogs.editor.games.sony.shared.mwd.WADFile;
import net.highwayfrogs.editor.games.sony.shared.mwd.WADFile.WADEntry;
import net.highwayfrogs.editor.games.sony.shared.utils.DynamicMeshObjExporter;
import net.highwayfrogs.editor.utils.ColorUtils;
import net.highwayfrogs.editor.utils.FXUtils;
import net.highwayfrogs.editor.utils.FileUtils;
import net.highwayfrogs.editor.utils.Utils;

import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Contains static utilities specifically for Frogger.
 * Created by Kneesnap on 5/2/2025.
 */
public class FroggerUtils {
    public static final int DEFAULT_TEXT_COLOR_RGB = 0xFFE8E8A0;
    public static final Color DEFAULT_TEXT_AWT_COLOR = ColorUtils.toAwtColorARGB(DEFAULT_TEXT_COLOR_RGB);

    /**
     * Exports a map file to wavefront obj.
     * @param map The map to export.
     * @param directory The directory to export it to.
     */
    public static void exportMapToObj(FroggerMapFile map, File directory) {
        if (map == null)
            throw new NullPointerException("map");
        if (directory == null)
            throw new NullPointerException("directory");
        if (!directory.isDirectory())
            throw new IllegalArgumentException("The provided destination was not a directory!");

        FroggerMapMesh mapMesh = new FroggerMapMesh(map);
        String cleanName = FileUtils.stripExtension(map.getFileDisplayName());
        DynamicMeshObjExporter.exportMeshToObj(map.getLogger(), mapMesh, directory, cleanName, true);
    }

    /**
     * Gets the Frogger map theme which the WAD corresponds to.
     * Returns null if the game is not Frogger, or there is no map theme.
     * @return froggerMapTheme
     */
    public static FroggerMapTheme getFroggerMapTheme(WADFile wadFile) {
        if (wadFile == null)
            return null;
        if (!wadFile.getGameInstance().isFrogger())
            return null;

        ThemeBook themeBook = null;
        for (ThemeBook book : ((FroggerGameInstance) wadFile.getGameInstance()).getThemeLibrary()) {
            if (book != null && book.isEntry(wadFile)) {
                themeBook = book;
                if (themeBook.getTheme() != null)
                    break;
            }
        }

        return themeBook != null && themeBook.getTheme() != null
                ? themeBook.getTheme() : FroggerMapTheme.getTheme(wadFile.getFileDisplayName());
    }

    /**
     * Checks if this map is a low-poly file.
     * Unfortunately, nothing distinguishes the files besides where you can access them from and the names.
     * @return isLowPolyMode
     */
    public static boolean isLowPolyMode(SCGameFile<?> gameFile) {
        return gameFile != null && gameFile.getGameInstance().isFrogger() && gameFile.getGameInstance().isPC()
                && (gameFile.getFileDisplayName().contains("_WIN95") || gameFile.getFileDisplayName().equals("OPTIONSL.WAD"));
    }

    /**
     * Checks if the provided file is used for multiplayer gameplay.
     * Unfortunately, nothing distinguishes the files besides where you can access them from and the file names themselves.
     * @return isMultiplayer
     */
    public static boolean isMultiplayerFile(SCGameFile<?> gameFile, FroggerMapTheme mapTheme) {
        return gameFile != null && mapTheme != null && gameFile.getGameInstance().isFrogger()
                && gameFile.getFileDisplayName().contains(mapTheme.getInternalName() + "M");
    }

    /**
     * Gets all instances of Frogger except the provided game instance.
     * @param instance the instance to search from
     * @return froggerInstances, if there are any.
     */
    public static List<FroggerGameInstance> getAllFroggerInstancesExcept(GameInstance instance) {
        List<FroggerGameInstance> instances = Collections.emptyList();
        for (GameInstance testInstance : FrogLordApplication.getActiveGameInstances()) {
            if (!(testInstance instanceof FroggerGameInstance) || testInstance == instance)
                continue;

            if (instances.isEmpty())
                instances = new ArrayList<>();
            instances.add((FroggerGameInstance) testInstance);
        }

        return instances;
    }

    /**
     * Gets the only other active instance of Frogger which is not the provided game instance.
     * @param instance the instance to search from
     * @return froggerInstance, if there is one.
     */
    public static FroggerGameInstance getOtherFroggerInstanceOrWarnUser(GameInstance instance) {
        FroggerGameInstance foundInstance = null;
        for (GameInstance testInstance : FrogLordApplication.getActiveGameInstances()) {
            if (!(testInstance instanceof FroggerGameInstance) || testInstance == instance)
                continue;

            if (foundInstance != null) {
                FXUtils.makePopUp("There is more than one copy of Frogger open at once,\n so FrogLord is unable to choose which version to convert the map to.", Alert.AlertType.ERROR);
                return null;
            }

            foundInstance = (FroggerGameInstance) testInstance;
        }

        if (foundInstance == null) {
            FXUtils.makePopUp("Please open a copy of Frogger to target.", Alert.AlertType.ERROR);
            return null;
        }

        return foundInstance;
    }

    /**
     * Clears a .WAD file and prepares it for use with a new level.
     * @param template The template .WAD file to copy wad entries from
     * @param target the target .WAD file to set up (the per-level wad file)
     */
    @SuppressWarnings("unused") // Used by Noodle scripts.
    public static void setupPerLevelWad(WADFile template, WADFile target) {
        if (template == null)
            throw new NullPointerException("template");
        if (target == null)
            throw new NullPointerException("target");
        if (template.getGameInstance() != target.getGameInstance())
            throw new IllegalArgumentException("The provided WAD files do not belong to the same game instance!");

        // Clear the tracking for the old wad file entries.
        for (int i = 0; i < target.getFiles().size(); i++) {
            WADEntry testEntry = target.getFiles().get(i);
            testEntry.setFile(null);
        }

        // Copy/create new wad entries.
        target.getFiles().clear();
        for (int i = 0; i < template.getFiles().size(); i++) {
            WADEntry copyEntry = template.getFiles().get(i);

            SCGameFile<?> newFile;
            if (copyEntry.getFile() instanceof MRModel) {
                MRModel oldModel = (MRModel) copyEntry.getFile();
                MRModel model = new MRModel(copyEntry.getGameInstance(), null);
                model.setDummy();
                model.setRawFileData(MRModel.DUMMY_DATA);
                model.setVloFile(oldModel.getVloFile());
                newFile = model;
            } else {
                byte[] rawData = copyEntry.getFile().writeDataToByteArray();
                newFile = copyEntry.getArchive().replaceFile(copyEntry.getDisplayName(), rawData, copyEntry.getFileEntry(), copyEntry.getFile(), false);
                newFile.setRawFileData(rawData);
            }

            newFile.setFileDefinition(copyEntry.getFileEntry());

            WADEntry newEntry = new WADEntry(target, copyEntry.getResourceId(), copyEntry.isCompressed());
            newFile.setWadFileEntry(newEntry);
            newEntry.setFile(newFile);
            target.getFiles().add(newEntry);
        }
    }

    private static Font froggerFont;

    /**
     * Writes Frogger text to the image with the given settings.
     * @param image the image to write the text to
     * @param width the expected width of the image
     * @param height the expected height of the image
     * @param text the text to write
     * @param fontSize the size of the font to use when writing the text.
     * @return imageWithText
     */
    public static BufferedImage writeFroggerText(BufferedImage image, int width, int height, String text, float fontSize, Color textColor, boolean isFourBitMode) {
        if (text == null)
            throw new NullPointerException("text");
        if (textColor == null)
            textColor = DEFAULT_TEXT_AWT_COLOR;
        if (image == null || image.getWidth() != width || image.getHeight() != height)
            image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

        Font font = getFroggerFont().deriveFont(fontSize);
        Graphics2D graphics = image.createGraphics();

        if (text.contains("\r"))
            text = text.replace("\r", "");

        String[] split = text.split("\n");

        try {
            graphics.setColor(Color.BLACK);
            graphics.fillRect(0, 0, image.getWidth(), image.getHeight());

            graphics.setFont(font);
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR); // May or may not improve the output. Unclear.
            graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            graphics.setColor(textColor);

            double y = -4;
            for (int i = 0; i < split.length; i++) {
                String textLine = split[i];
                Rectangle2D bounds = font.getStringBounds(textLine, graphics.getFontRenderContext());
                y += bounds.getHeight();
                graphics.drawString(textLine, (float) ((image.getWidth() - bounds.getWidth()) * .5), (float) y);
            }

            graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
        } finally {
            graphics.dispose();
        }

        if (isFourBitMode) {
            // Create the palette to pull colors from
            final int colorCount = 16;
            int[] colorPalette = new int[colorCount];
            int baseRed = textColor.getRed();
            int baseGreen = textColor.getGreen();
            int baseBlue = textColor.getBlue();
            for (int i = 1; i < colorCount; i++) {
                double scaleAmount = (double) i / (colorCount - 1);
                byte red = (byte) (baseRed * scaleAmount);
                byte green = (byte) (baseGreen * scaleAmount);
                byte blue = (byte) (baseBlue * scaleAmount);
                colorPalette[i] = ColorUtils.toARGB(red, green, blue, (byte) 0xFE);
            }

            // Snap pixel values to the palette.
            int[] rawPixelData = ImageWorkHorse.getPixelIntegerArray(image);
            float averageMultiple = 1 / 3F;
            for (int i = 0; i < rawPixelData.length; i++) {
                int rgb = rawPixelData[i];
                float red = (float) ColorUtils.getRedInt(rgb) / textColor.getRed();
                float green = (float) ColorUtils.getGreenInt(rgb) / textColor.getGreen();
                float blue = (float) ColorUtils.getBlueInt(rgb) / textColor.getBlue();
                float averageValue = (red + green + blue) * averageMultiple;
                int index = Math.min(colorCount - 1, Math.round(averageValue * (colorCount - 1)));
                rawPixelData[i] = colorPalette[index];
            }
        } else {
            // Ensure transparency.
            int[] rawPixelData = ImageWorkHorse.getPixelIntegerArray(image);
            for (int i = 0; i < rawPixelData.length; i++) {
                if ((rawPixelData[i] & 0xFFFFFF) == 0) {
                    rawPixelData[i] &= 0x00FFFFFF;
                } else {
                    rawPixelData[i] &= ~0x80000000;
                    rawPixelData[i] |= 0x7F000000;
                }
            }
        }

        return image;
    }

    /**
     * Gets the font used to create text.
     * The font has been provided by StarmanUltra, thank you.
     */
    public static Font getFroggerFont() {
        if (froggerFont != null)
            return froggerFont;

        InputStream fontStream = SCGameType.FROGGER.getEmbeddedResourceStream("font-main-text.otf");
        try {
            froggerFont = Font.createFont(Font.TRUETYPE_FONT, fontStream);
        } catch (FontFormatException | IOException ex) {
            Utils.handleError(null, ex, true, "Failed to load Frogger font. Using system default...");
            froggerFont = GraphicsEnvironment.getLocalGraphicsEnvironment().getAllFonts()[0];
        }

        return froggerFont;
    }
}
