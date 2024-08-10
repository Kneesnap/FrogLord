package net.highwayfrogs.editor.gui;

import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.highwayfrogs.editor.utils.Utils;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;
import java.util.logging.Logger;

/**
 * A registry of all static images included in a FrogLord build.
 * Created by Kneesnap on 4/27/2024.
 */
@Getter
@RequiredArgsConstructor
public enum ImageResource {
    SQUARE_LETTER_E_128("icons/entity.png"),
    GAME_CONTROLLER_32("icons/demo.png"),
    PHOTO_ALBUM_32("icons/image.png"),
    TREASURE_MAP_32("icons/map.png"),
    GEOMETRIC_SHAPES_32("icons/model.png"),
    ZIPPED_FOLDER_32("icons/packed.png"),
    PAINTERS_PALETTE_32("icons/palette.png"),
    SKELETON_JOINTS_32("icons/skeleton-joints.png"),
    MUSIC_NOTE_32("icons/sound.png"),
    SWAMPY_32("icons/swampy.png"),
    QUESTION_MARK_32("icons/unknown.png"),
    ICON_MULTIMEDIA_32("icons/applications-multimedia32.png"),

    // Logos
    FROGLORD_LOGO_ALTERNATE_LARGE("graphics/alternate-logo-large.png"),
    FROGLORD_LOGO_ALTERNATE_SQUARE_ICON("graphics/alternate-logo-small.png"),
    FROGLORD_LOGO_MAIN_LARGE("graphics/logo-large.png"),
    FROGLORD_LOGO_MAIN_SQUARE_ICON("graphics/logo-small.png"),
    FROGLORD_LOGO_SQUARE_ICON("graphics/icon.png"),

    // Icons taken from Ghidra. See '/resources/icons/ghidra/LICENSE.MD' for licensing information.
    GHIDRA_ICON_MULTIMEDIA_16("icons/ghidra/applications-multimedia16.png"),
    GHIDRA_ICON_CHECKMARK_GREEN_16("icons/ghidra/checkmark_green.gif"),
    GHIDRA_ICON_CLOSED_SMALL_FOLDER_16("icons/ghidra/closedSmallFolder.png"),
    GHIDRA_ICON_COLLAPSE_ALL_16("icons/ghidra/collapse_all.png"),
    GHIDRA_ICON_BOMB_16("icons/ghidra/core.png"),
    GHIDRA_ICON_BOMB_24("icons/ghidra/core24.png"),
    GHIDRA_ICON_RED_SLASH_CIRCLE_16("icons/ghidra/dialog-cancel.png"),
    GHIDRA_ICON_SAVE_DISK_16("icons/ghidra/disk.png"),
    GHIDRA_ICON_SAVE_DISK_AS_16("icons/ghidra/disk_save_as.png"),
    GHIDRA_ICON_ARROW_DOWN_16("icons/ghidra/down.png"),
    GHIDRA_ICON_DRAGON_16("icons/ghidra/dragon16.gif"),
    GHIDRA_ICON_SCISSORS_16("icons/ghidra/edit-cut.png"),
    GHIDRA_ICON_RED_X_SMALL_16("icons/ghidra/edit-delete.png"),
    GHIDRA_ICON_RED_EXCLAMATION_CIRCLE_16("icons/ghidra/emblem-important.png"),
    GHIDRA_ICON_EMPTY_20("icons/ghidra/EmptyIcon.gif"),
    GHIDRA_ICON_EMPTY_16("icons/ghidra/EmptyIcon16.gif"),
    GHIDRA_ICON_ERASER_16("icons/ghidra/erase16.png"),
    GHIDRA_ICON_RED_X_16("icons/ghidra/error.png"),
    GHIDRA_ICON_GEAR_16("icons/ghidra/exec.png"),
    GHIDRA_ICON_EXPAND_ALL_16("icons/ghidra/expand_all.png"),
    GHIDRA_ICON_MONKEY_16("icons/ghidra/face-monkey16.png"),
    GHIDRA_ICON_FLAG_GREEN_16("icons/ghidra/flag.png"),
    GHIDRA_ICON_MAIN_16("icons/ghidra/GhidraIcon16.png"),
    GHIDRA_ICON_MAIN_24("icons/ghidra/GhidraIcon24.png"),
    GHIDRA_ICON_MAIN_32("icons/ghidra/GhidraIcon32.png"),
    GHIDRA_ICON_HOME_16("icons/ghidra/go-home.png"),
    GHIDRA_ICON_GREEN_DRAGON_16("icons/ghidra/greenDragon16.png"),
    GHIDRA_ICON_GREEN_DRAGON_24("icons/ghidra/greenDragon24.png"),
    GHIDRA_ICON_QUESTION_MARK_16("icons/ghidra/help-browser.png"),
    GHIDRA_ICON_INFORMATION_16("icons/ghidra/information.png"),
    GHIDRA_ICON_INTERNET_16("icons/ghidra/internet-web-browser16.png"),
    GHIDRA_ICON_PADLOCK_16("icons/ghidra/kgpg.png"),
    GHIDRA_ICON_ARROW_LEFT_YELLOW_16("icons/ghidra/left.alternate.png"),
    GHIDRA_ICON_ARROW_LEFT_BLUE_16("icons/ghidra/left.png"),
    GHIDRA_ICON_SUBTRACTION_SIGN_16("icons/ghidra/list-remove.png"),
    GHIDRA_ICON_LOCATION_IN_16("icons/ghidra/locationIn.gif"),
    GHIDRA_ICON_LOCATION_OUT_16("icons/ghidra/locationOut.gif"),
    GHIDRA_ICON_ROAD_MERGE_16("icons/ghidra/mergemgr16.gif"),
    GHIDRA_ICON_NETWORK_16("icons/ghidra/network-receive16.png"),
    GHIDRA_ICON_OPEN_FOLDER_16("icons/ghidra/openFolder.png"),
    GHIDRA_ICON_OPEN_SMALL_FOLDER_16("icons/ghidra/openSmallFolder.png"),
    GHIDRA_ICON_CLIPBOARD_PASTE_16("icons/ghidra/page_paste.png"),
    GHIDRA_ICON_CLIPBOARD_COPY_16("icons/ghidra/page_white_copy.png"),
    GHIDRA_ICON_ADDITION_16("icons/ghidra/Plus2.png"),
    GHIDRA_ICON_STOP_SIGN_X_16("icons/ghidra/process-stop.png"),
    GHIDRA_ICON_PAPER_WITH_TEXT_16("icons/ghidra/program_obj.png"),
    GHIDRA_ICON_RED_DRAGON_16("icons/ghidra/redDragon16.png"),
    GHIDRA_ICON_RED_DRAGON_24("icons/ghidra/redDragon24.png"),
    GHIDRA_ICON_RED_DRAGON_32("icons/ghidra/redDragon32.png"),
    GHIDRA_ICON_REFRESH_16("icons/ghidra/reload3.png"),
    GHIDRA_ICON_ARROW_RIGHT_YELLOW_16("icons/ghidra/right.alternate.png"),
    GHIDRA_ICON_ARROW_RIGHT_BLUE_16("icons/ghidra/right.png"),
    GHIDRA_ICON_TRIANGLE_WARNING_16("icons/ghidra/software-update-urgent.png"),
    GHIDRA_ICON_SORT_ASCENDING_16("icons/ghidra/sortascending.png"),
    GHIDRA_ICON_SORT_DESCENDING_16("icons/ghidra/sortdescending.png"),
    GHIDRA_ICON_HAMBURGER_16("icons/ghidra/stack.png"),
    GHIDRA_ICON_TEXT_ALIGN_JUSTIFY_16("icons/ghidra/text_align_justify.png"),
    GHIDRA_ICON_ARROW_UP_16("icons/ghidra/up.png"),
    GHIDRA_ICON_VIDEO_REEL_16("icons/ghidra/video-x-generic16.png"),
    GHIDRA_ICON_ARROW_DIAGONAL_UP_LEFT_16("icons/ghidra/viewmagfit.png"),
    GHIDRA_ICON_WARNING_TRIANGLE_YELLOW_16("icons/ghidra/warning.png"),

    // Resized images:
    PHOTO_ALBUM_15(ImageResource.PHOTO_ALBUM_32, 15),
    TREASURE_MAP_15(ImageResource.TREASURE_MAP_32, 15),
    GEOMETRIC_SHAPES_15(ImageResource.GEOMETRIC_SHAPES_32, 15),
    ZIPPED_FOLDER_15(ImageResource.ZIPPED_FOLDER_32, 15),
    QUESTION_MARK_15(ImageResource.QUESTION_MARK_32, 15),
    MUSIC_NOTE_16(ImageResource.MUSIC_NOTE_32, 16);

    private final String resourcePath;
    private final ImageResource parentImageResource;
    private final int resizeDimension;
    private BufferedImage awtImage;
    private Image fxImage;

    ImageResource(ImageResource parentImageResource, int resizeDimension) {
        this.resourcePath = null;
        this.parentImageResource = parentImageResource;
        this.resizeDimension = resizeDimension;
    }

    ImageResource(String resourcePath) {
        this.resourcePath = resourcePath;
        this.parentImageResource = null;
        this.resizeDimension = -1;
    }

    /**
     * Loads the image from the resource path.
     */
    public BufferedImage getAwtImage() {
        if (this.awtImage != null)
            return this.awtImage;

        if (this.resourcePath != null) {
            // Read the image.
            URL imageUrl = Utils.getResourceURL(this.resourcePath);
            if (imageUrl == null)
                throw new IllegalStateException("Cannot load image from resource '" + this.resourcePath + "'.");

            try {
                this.awtImage = ImageIO.read(imageUrl);
            } catch (IOException ex) {
                Utils.handleError(getLogger(), ex, false, "Failed to load image from resource '%s'.", this.resourcePath);
                if (this != QUESTION_MARK_32)
                    return QUESTION_MARK_32.getAwtImage();
            }
        } else if (this.parentImageResource != null) {
            // Create this image by resizing another image resource.
            BufferedImage originalImage = this.parentImageResource.getAwtImage();
            if (originalImage != null && this.resizeDimension > 0 && (this.resizeDimension != originalImage.getWidth() || this.resizeDimension != originalImage.getHeight())) {
                return this.awtImage = Utils.resizeImage(originalImage, this.resizeDimension, this.resizeDimension);
            } else {
                return originalImage;
            }
        } else {
            throw new IllegalStateException("Reached a code path where we don't know how to resolve the image '" + name() + "'.");
        }

        return this.awtImage;
    }

    /**
     * Gets the image in a form usable by JavaFX.
     */
    public Image getFxImage() {
        if (this.fxImage != null)
            return this.fxImage;

        // If we should resize, and the desired resize amount is different from the original image size...
        BufferedImage awtImage;
        if (this.resourcePath != null) {
            awtImage = getAwtImage();
        } else if (this.parentImageResource != null) {
            // Resize parent image?
            BufferedImage originalImage = this.parentImageResource.getAwtImage();
            if (originalImage != null && this.resizeDimension > 0 && (originalImage.getWidth() != this.resizeDimension || originalImage.getHeight() != this.resizeDimension)) {
                return this.fxImage = SwingFXUtils.toFXImage(getAwtImage(), null);
            } else {
                return this.parentImageResource.getFxImage();
            }
        } else {
            throw new IllegalStateException("Reached a code path where we don't know how to resolve the image '" + name() + "'.");
        }

        return this.fxImage = SwingFXUtils.toFXImage(awtImage, null);
    }

    private static Logger getLogger() {
        return Logger.getLogger(ImageResource.class.getSimpleName());
    }
}