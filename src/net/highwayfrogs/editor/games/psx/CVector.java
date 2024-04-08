package net.highwayfrogs.editor.games.psx;

import javafx.geometry.Pos;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.vlo.ImageWorkHorse;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.sony.shared.shading.PSXTextureShader;
import net.highwayfrogs.editor.gui.GUIEditorGrid;
import net.highwayfrogs.editor.gui.InputMenu;
import net.highwayfrogs.editor.utils.IBinarySerializable;
import net.highwayfrogs.editor.utils.Utils;

import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * Represents the 'CVECTOR' struct on PlayStation. Also by extension, MR_CVEC.
 * Created by Kneesnap on 12/9/2023.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CVector implements IBinarySerializable {
    private byte red;
    private byte green;
    private byte blue;

    // PSX documentation calls this "GPU code". Frogger seems to have 0xFF for all cases here, but this might just be the PC version. Some of the earlier PSX builds are confirmed not to.
    // Frogger in mapload.c will write values to this for all map polygons, so it seems this may be ignorable.
    // According to https://psx-spx.consoledev.net/graphicsprocessingunitgpu/, these are bit flags:
    // Bit 0:    raw texture / modulation (texture blending)
    // Bit 1:    semi-transparent / opaque
    // Bit 2:    textured / untextured
    // Bit 3:    4 vertices / 3 vertices
    // Bit 4:    gouraud / flat shading
    // Bit 5-7:  polygon render (always 001, if not 001, then this isn't a command to render a polygon. Read the GP0 command list for more info)
    // This value is ignored for all colors after the first one.
    private byte code = (byte) 0xFF; // Frogger has these as 0xFF by default, but other games might not do that.

    // GP0 Command List:
    //  0 (000)      Misc commands
    //  1 (001)      Polygon primitive
    //  2 (010)      Line primitive
    //  3 (011)      Rectangle primitive
    //  4 (100)      VRAM-to-VRAM blit
    //  5 (101)      CPU-to-VRAM blit
    //  6 (110)      VRAM-to-CPU blit
    //  7 (111)      Environment commands

    public static final int BYTE_LENGTH = 4 * Constants.BYTE_SIZE;

    public static final int FLAG_MODULATION = Constants.BIT_FLAG_0;
    public static final int FLAG_SEMI_TRANSPARENT = Constants.BIT_FLAG_1;
    public static final int FLAG_TEXTURED = Constants.BIT_FLAG_2;
    public static final int FLAG_QUAD = Constants.BIT_FLAG_3;
    public static final int FLAG_GOURAUD_SHADING = Constants.BIT_FLAG_4;
    public static final int MASK_COMMAND = 0b11100000;
    public static final int MASK_COMMAND_VALID = 0b00100000;
    public static final int GP0_COMMAND_POLYGON_PRIMITIVE = 0b00100000;

    @Override
    public void load(DataReader reader) {
        this.red = reader.readByte();
        this.green = reader.readByte();
        this.blue = reader.readByte();
        this.code = reader.readByte();
    }

    @Override
    public void save(DataWriter writer) {
        writer.writeByte(this.red);
        writer.writeByte(this.green);
        writer.writeByte(this.blue);
        writer.writeByte(this.code);
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof CVector))
            return false;

        CVector other = (CVector) obj;
        return this.red == other.red && this.green == other.green && this.blue == other.blue && this.code == other.code;
    }

    @Override
    public int hashCode() {
        return ((0xFF & this.red) << 24) | ((0xFF & this.green) << 16) |
                ((0xFF & this.blue) << 8) | (0xFF & this.code);
    }

    /**
     * Test if the code represents a valid draw command.
     */
    public boolean isCodeValid() {
        return (this.code & MASK_COMMAND) == MASK_COMMAND_VALID;
    }

    /**
     * Test if the code is valid and represents a valid draw command.
     * @param bitMask the mask to test
     * @return bitsSet
     */
    public boolean testFlag(int bitMask) {
        return isCodeValid() && (this.code & bitMask) == bitMask;
    }

    /**
     * Gets the red color value between 0 and 255 as a short (since byte is signed)
     * @return redAsShort
     */
    public short getRedShort() {
        return Utils.byteToUnsignedShort(this.red);
    }

    /**
     * Sets the red color value from a short to a byte.
     * @param newRed the new value of red between 0 and 255.
     */
    public void setRedShort(short newRed) {
        this.red = Utils.unsignedShortToByte(newRed);
    }

    /**
     * Gets the green color value between 0 and 255 as a short (since byte is signed)
     * @return greenAsShort
     */
    public short getGreenShort() {
        return Utils.byteToUnsignedShort(this.green);
    }

    /**
     * Sets the green color value from a short to a byte.
     * @param newGreen the new value of green between 0 and 255.
     */
    public void setGreenShort(short newGreen) {
        this.green = Utils.unsignedShortToByte(newGreen);
    }

    /**
     * Gets the blue color value between 0 and 255 as a short (since byte is signed)
     * @return blueShort
     */
    public short getBlueShort() {
        return Utils.byteToUnsignedShort(this.blue);
    }

    /**
     * Sets the blue color value from a short to a byte.
     * @param newBlue the new value of blue between 0 and 255.
     */
    public void setBlueShort(short newBlue) {
        this.blue = Utils.unsignedShortToByte(newBlue);
    }

    /**
     * Copy the contents of another CVector to this one.
     * @param other The other vector to copy from.
     */
    public void copyFrom(CVector other) {
        if (other == null)
            throw new NullPointerException("other");

        this.red = other.red;
        this.green = other.green;
        this.blue = other.blue;
        this.code = other.code;
    }

    /**
     * Get this color as a Java color.
     * This method assumes that modulation is disabled, since modulation makes the CVector more than just a color.
     * @return javaColor
     */
    public Color toColor() {
        return toColor((byte) 0xFF);
    }

    /**
     * Get this color as a Java color.
     * This method assumes that modulation is disabled, since modulation makes the CVector more than just a color.
     * @return javaColor
     */
    public Color toColor(byte alpha) {
        return new Color(getRedShort(), getGreenShort(), getBlueShort(), (alpha & 0xFF));
    }

    /**
     * Turn this color into an RGB integer.
     * @return rgbValue
     */
    public int toRGB() {
        return Utils.toRGB(getRed(), getGreen(), getBlue());
    }

    /**
     * Turn this color into an ARGB integer.
     * @return argbValue
     */
    public int toARGB() {
        return Utils.toARGB(getRed(), getGreen(), getBlue(), (byte) 0xFF);
    }

    /**
     * Read color data from an integer value.
     * @param rgbValue The value to read from.
     */
    public void fromRGB(int rgbValue) {
        this.red = Utils.unsignedShortToByte((short) ((rgbValue >> 16) & 0xFF));
        this.green = Utils.unsignedShortToByte((short) ((rgbValue >> 8) & 0xFF));
        this.blue = Utils.unsignedShortToByte((short) (rgbValue & 0xFF));
    }

    /**
     * Read color data from an integer value.
     * @param crgbValue The value to read from.
     */
    public void fromCRGB(int crgbValue) {
        fromRGB(crgbValue);
        this.code = Utils.unsignedShortToByte((short) ((crgbValue >> 24) & 0xFF));
    }

    @Override
    public String toString() {
        return "CVector<red=" + getRedShort() + ",green=" + getGreenShort() + ",blue=" + getBlueShort() + ",code=" + Utils.byteToUnsignedShort(this.code) + ">";
    }

    /**
     * Create a CVector from an RGB value.
     * @param rgbValue The rgb color value.
     * @return colorVector
     */
    public static CVector makeColorFromRGB(int rgbValue) {
        CVector vec = new CVector();
        vec.fromRGB(rgbValue);
        return vec;
    }

    /**
     * Create a CVector from an RGB value with a gpu code in the highest byte.
     * @param crgbValue The crgb color value.
     * @return colorVector
     */
    public static CVector makeColorFromCRGB(int crgbValue) {
        CVector vec = new CVector();
        vec.fromCRGB(crgbValue);
        return vec;
    }

    /**
     * Adds a color vector to the editor.
     * This is for picking / editing a color.
     */
    public void setupUnmodulatedEditor(GUIEditorGrid grid, String label, BufferedImage texture, Runnable onUpdate) {
        HBox box = new HBox();
        Runnable[] imageUpdate = new Runnable[1];
        javafx.scene.text.Font useFont = javafx.scene.text.Font.font(javafx.scene.text.Font.getDefault().getSize() * .75D);
        VBox previewBox = new VBox();
        VBox redBox = new VBox();
        VBox greenBox = new VBox();
        VBox blueBox = new VBox();

        Label redLabel = labelFont("Red (" + getRedShort() + ")", useFont);
        Label greenLabel = labelFont("Green (" + getGreenShort() + ")", useFont);
        Label blueLabel = labelFont("Blue (" + getBlueShort() + ")", useFont);

        // Downscale the texture, so it's a lot faster to update the color.
        final BufferedImage scaledTexture = texture != null ? ImageWorkHorse.resizeImage(texture, 45, 45, false) : null;

        Slider redSlider = new Slider(0D, 255D, getRedShort());
        redSlider.setBlockIncrement(1);
        redSlider.setMinorTickCount(1);
        redSlider.setSnapToTicks(true);
        redSlider.valueProperty().addListener(((observable, oldValue, newValue) -> {
            setRedShort((short) (double) newValue);
            redLabel.setText("Red (" + getRedShort() + ")");
            imageUpdate[0].run();
            if (onUpdate != null)
                onUpdate.run();
        }));

        Slider greenSlider = new Slider(0D, 255D, getGreenShort());
        greenSlider.setBlockIncrement(1);
        greenSlider.setMinorTickCount(1);
        greenSlider.setSnapToTicks(true);
        greenSlider.valueProperty().addListener(((observable, oldValue, newValue) -> {
            setGreenShort((short) (double) newValue);
            greenLabel.setText("Green (" + getGreenShort() + ")");
            imageUpdate[0].run();
            if (onUpdate != null)
                onUpdate.run();
        }));

        Slider blueSlider = new Slider(0D, 255D, getBlueShort());
        blueSlider.setBlockIncrement(1);
        blueSlider.setMinorTickCount(1);
        blueSlider.setSnapToTicks(true);
        blueSlider.valueProperty().addListener(((observable, oldValue, newValue) -> {
            setBlueShort((short) (double) newValue);
            redLabel.setText("Red (" + getBlueShort() + ")");
            imageUpdate[0].run();
            if (onUpdate != null)
                onUpdate.run();
        }));

        // Texture Preview
        ImageView preview = new ImageView(scaledTexture != null ? Utils.toFXImage(scaledTexture, false) : null);
        preview.setOnMouseClicked(evt ->
                InputMenu.promptInput("Please enter the color value you'd like to use.", Integer.toHexString(toRGB()).toUpperCase(), newText -> {
                    int colorRGB;
                    try {
                        colorRGB = Integer.parseInt(newText, 16);
                    } catch (NumberFormatException nfe) {
                        Utils.makePopUp("'" + newText + "' is not a valid hex number.", AlertType.ERROR);
                        return;
                    }

                    fromRGB(colorRGB);
                    redSlider.setValue(getRedShort());
                    greenSlider.setValue(getGreenShort());
                    blueSlider.setValue(getBlueShort());
                    redLabel.setText("Red (" + getRedShort() + ")");
                    greenLabel.setText("Green (" + getGreenShort() + ")");
                    blueLabel.setText("Blue (" + getBlueShort() + ")");
                    imageUpdate[0].run();
                    if (onUpdate != null)
                        onUpdate.run();
                }));

        imageUpdate[0] = () -> {
            BufferedImage newImage;
            if (scaledTexture != null) {
                newImage = PSXTextureShader.makeTexturedFlatShadedImage(scaledTexture, this);
            } else {
                newImage = PSXTextureShader.makeFlatShadedImage(45, 45, this);
            }
            preview.setImage(Utils.toFXImage(newImage, false));
        };
        imageUpdate[0].run();

        previewBox.getChildren().add(preview);
        redBox.getChildren().addAll(redLabel, redSlider);
        greenBox.getChildren().addAll(greenLabel, greenSlider);
        blueBox.getChildren().addAll(blueLabel, blueSlider);

        // Center HBox Children
        previewBox.setAlignment(Pos.CENTER);
        redBox.setAlignment(Pos.CENTER);
        greenBox.setAlignment(Pos.CENTER);
        blueBox.setAlignment(Pos.CENTER);

        // Space horizontal box.
        box.setSpacing(1);
        box.getChildren().addAll(previewBox, redBox, greenBox, blueBox);

        // Setup vbox.
        VBox vbox = new VBox();
        vbox.setSpacing(1);
        if (label != null)
            vbox.getChildren().add(labelFont(label, useFont));
        vbox.getChildren().add(box);

        // Add to editor grid.
        grid.setupSecondNode(vbox, true);
        grid.addRow(60);
    }

    /**
     * Adds a color vector to the editor.
     * This is not for picking a color, rather it's for the PSX gouraud shading settings.
     */
    public void setupModulatedEditor(GUIEditorGrid grid, String label, Runnable onUpdate) {
        HBox box = new HBox();

        javafx.scene.text.Font useFont = javafx.scene.text.Font.font(javafx.scene.text.Font.getDefault().getSize() * .75D);
        VBox redBox = new VBox();
        VBox greenBox = new VBox();
        VBox blueBox = new VBox();

        // Setup first line elements.

        // Setup color previews.
        BufferedImage redColorImage = new BufferedImage(35, 10, BufferedImage.TYPE_INT_ARGB);
        BufferedImage greenColorImage = new BufferedImage(35, 10, BufferedImage.TYPE_INT_ARGB);
        BufferedImage blueColorImage = new BufferedImage(35, 10, BufferedImage.TYPE_INT_ARGB);
        ImageView redColorView = new ImageView();
        ImageView greenColorView = new ImageView();
        ImageView blueColorView = new ImageView();

        // Setup color labels.
        Label redLabel = labelFont("Red (" + getRedShort() + ")", useFont);
        Label greenLabel = labelFont("Green (" + getGreenShort() + ")", useFont);
        Label blueLabel = labelFont("Blue (" + getBlueShort() + ")", useFont);

        Slider redSlider = new Slider(0D, 255D, getRedShort());
        redSlider.setBlockIncrement(1);
        redSlider.setMinorTickCount(1);
        redSlider.setSnapToTicks(true);
        redSlider.valueProperty().addListener(((observable, oldValue, newValue) -> {
            setRedShort((short) (double) newValue);
            redLabel.setText("Red (" + getRedShort() + ")");
            updateModulatedColorPreviewImage(redColorView, redColorImage, getRedShort(), 0, 0);
            if (onUpdate != null)
                onUpdate.run();
        }));

        Slider greenSlider = new Slider(0D, 255D, getGreenShort());
        greenSlider.setBlockIncrement(1);
        greenSlider.setMinorTickCount(1);
        greenSlider.setSnapToTicks(true);
        greenSlider.valueProperty().addListener(((observable, oldValue, newValue) -> {
            setGreenShort((short) (double) newValue);
            greenLabel.setText("Green (" + getGreenShort() + ")");
            updateModulatedColorPreviewImage(greenColorView, greenColorImage, 0, getGreenShort(), 0);
            if (onUpdate != null)
                onUpdate.run();
        }));

        Slider blueSlider = new Slider(0D, 255D, getBlueShort());
        blueSlider.setBlockIncrement(1);
        blueSlider.setMinorTickCount(1);
        blueSlider.setSnapToTicks(true);
        blueSlider.valueProperty().addListener(((observable, oldValue, newValue) -> {
            setBlueShort((short) (double) newValue);
            blueLabel.setText("Blue (" + getBlueShort() + ")");
            updateModulatedColorPreviewImage(blueColorView, blueColorImage, 0, 0, getBlueShort());
            if (onUpdate != null)
                onUpdate.run();
        }));

        updateModulatedColorPreviewImage(redColorView, redColorImage, getRedShort(), 0, 0);
        updateModulatedColorPreviewImage(greenColorView, greenColorImage, 0, getGreenShort(), 0);
        updateModulatedColorPreviewImage(blueColorView, blueColorImage, 0, 0, getBlueShort());

        redBox.getChildren().addAll(redLabel, redSlider, redColorView);
        greenBox.getChildren().addAll(greenLabel, greenSlider, greenColorView);
        blueBox.getChildren().addAll(blueLabel, blueSlider, blueColorView);

        // Center VBox Children
        redBox.setAlignment(Pos.CENTER);
        greenBox.setAlignment(Pos.CENTER);
        blueBox.setAlignment(Pos.CENTER);

        // Add color boxes.
        box.getChildren().addAll(redBox, greenBox, blueBox);
        box.setSpacing(1);

        // Create vertical box.
        VBox rootNode = new VBox();
        rootNode.setSpacing(1);
        if (label != null)
            rootNode.getChildren().add(labelFont(label, useFont));
        rootNode.getChildren().add(box);

        // Setup in grid.
        grid.setupSecondNode(rootNode, true);
        grid.addRow(50);
    }

    private static javafx.scene.control.Label labelFont(String text, Font font) {
        javafx.scene.control.Label label = new Label(text);
        label.setFont(font);
        label.setWrapText(true);
        return label;
    }

    private static void updateModulatedColorPreviewImage(ImageView view, BufferedImage image, int newRed, int newGreen, int newBlue) {
        Graphics2D graphics = image.createGraphics();

        // Clamp modulated color value. This preview's purpose is to make it clear that 127 is the brightest color value in the editor.
        int red = (int) Math.min((255D * newRed) / 127D, 255);
        int green = (int) Math.min((255D * newGreen) / 127D, 255);
        int blue = (int) Math.min((255D * newBlue) / 127D, 255);

        graphics.setColor(new Color(red, green, blue, 255));
        graphics.fillRect(0, 0, image.getWidth(), image.getHeight());
        graphics.dispose();

        view.setImage(Utils.toFXImage(image, false));
    }

    /**
     * Creates a copy of this object.
     */
    @SuppressWarnings("MethodDoesntCallSuperMethod")
    public CVector clone() {
        return new CVector(this.red, this.green, this.blue, this.code);
    }
}