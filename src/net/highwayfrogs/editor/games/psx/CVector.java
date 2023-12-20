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
import net.highwayfrogs.editor.file.map.poly.polygon.MAPPolyTexture;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
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
    private byte code;

    public static final int BYTE_LENGTH = 4 * Constants.BYTE_SIZE;

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
     * @return javaColor
     */
    public Color toColor() {
        return toColor((byte) 0xFF);
    }

    /**
     * Get this color as a Java color.
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
     * Adds a color vector to the editor.
     * TODO: Let's revisit this and see if we can improve it.
     * This is not for picking a color, rather it's for the PSX gouraud shading settings.
     */
    public void setupEditor(GUIEditorGrid grid, String label, BufferedImage previewImage, Runnable onUpdate, boolean fullRange) {
        HBox box = new HBox();

        Runnable[] imageUpdate = new Runnable[1];
        javafx.scene.text.Font useFont = javafx.scene.text.Font.font(javafx.scene.text.Font.getDefault().getSize() * .75D);
        VBox previewBox = new VBox();
        VBox redBox = new VBox();
        VBox greenBox = new VBox();
        VBox blueBox = new VBox();

        Slider redSlider = new Slider(0D, fullRange ? 255D : 127D, Utils.byteToUnsignedShort(getRed()));
        redSlider.setBlockIncrement(1);
        redSlider.setMinorTickCount(1);
        redSlider.setSnapToTicks(true);
        redSlider.valueProperty().addListener(((observable, oldValue, newValue) -> {
            if (fullRange) {
                setRed(Utils.unsignedShortToByte((short) (double) newValue));
            } else {
                setRed((byte) (int) (double) newValue);
            }
            imageUpdate[0].run();
            if (onUpdate != null)
                onUpdate.run();
        }));

        Slider greenSlider = new Slider(0D, fullRange ? 255D : 127D, Utils.byteToUnsignedShort(getGreen()));
        greenSlider.setBlockIncrement(1);
        greenSlider.setMinorTickCount(1);
        greenSlider.setSnapToTicks(true);
        greenSlider.valueProperty().addListener(((observable, oldValue, newValue) -> {
            if (fullRange) {
                setGreen(Utils.unsignedShortToByte((short) (double) newValue));
            } else {
                setGreen((byte) (int) (double) newValue);
            }
            imageUpdate[0].run();
            if (onUpdate != null)
                onUpdate.run();
        }));

        Slider blueSlider = new Slider(0D, fullRange ? 255D : 127D, Utils.byteToUnsignedShort(getBlue()));
        blueSlider.setBlockIncrement(1);
        blueSlider.setMinorTickCount(1);
        blueSlider.setSnapToTicks(true);
        blueSlider.valueProperty().addListener(((observable, oldValue, newValue) -> {
            if (fullRange) {
                setBlue(Utils.unsignedShortToByte((short) (double) newValue));
            } else {
                setBlue((byte) (int) (double) newValue);
            }

            imageUpdate[0].run();
            if (onUpdate != null)
                onUpdate.run();
        }));

        // Create the base preview image.
        BufferedImage applyImage;
        if (previewImage != null) {
            applyImage = Utils.resizeImage(previewImage, 45, 45);
        } else {
            applyImage = new BufferedImage(45, 45, BufferedImage.TYPE_INT_ARGB);
            Graphics graphics = applyImage.createGraphics();
            graphics.setColor(Color.WHITE);
            graphics.fillRect(0, 0, applyImage.getWidth(), applyImage.getHeight());
            graphics.dispose();
        }

        ImageView preview = new ImageView(Utils.toFXImage(applyImage, false));
        preview.setOnMouseClicked(evt ->
                InputMenu.promptInput("Please enter the color value you'd like to use.", Integer.toHexString(toRGB()), newText -> {
                    int colorRGB;
                    try {
                        colorRGB = Integer.parseInt(newText, 16);
                    } catch (NumberFormatException nfe) {
                        Utils.makePopUp("'" + newText + "' is not a valid hex number.", AlertType.ERROR);
                        return;
                    }

                    fromRGB(colorRGB);
                    redSlider.setValue(Utils.byteToUnsignedShort(getRed()));
                    greenSlider.setValue(Utils.byteToUnsignedShort(getGreen()));
                    blueSlider.setValue(Utils.byteToUnsignedShort(getBlue()));
                    imageUpdate[0].run();
                    if (onUpdate != null)
                        onUpdate.run();
                }));

        imageUpdate[0] = () ->
                preview.setImage(Utils.toFXImage(MAPPolyTexture.makeFlatShadedTexture(applyImage, Utils.fromRGB(toRGB()), fullRange), false));
        imageUpdate[0].run();

        previewBox.getChildren().addAll(labelFont(label, useFont), preview);
        redBox.getChildren().addAll(labelFont("Red", useFont), redSlider);
        greenBox.getChildren().addAll(labelFont("Green", useFont), greenSlider);
        blueBox.getChildren().addAll(labelFont("Blue", useFont), blueSlider);

        // Center VBox Children
        previewBox.setAlignment(Pos.CENTER);
        redBox.setAlignment(Pos.CENTER);
        greenBox.setAlignment(Pos.CENTER);
        blueBox.setAlignment(Pos.CENTER);

        box.setSpacing(1);
        box.getChildren().addAll(previewBox, redBox, greenBox, blueBox);
        grid.setupSecondNode(box, true);
        grid.addRow(60);
    }

    private static javafx.scene.control.Label labelFont(String text, Font font) {
        javafx.scene.control.Label label = new Label(text);
        label.setFont(font);
        label.setWrapText(true);
        return label;
    }

    /**
     * Creates a copy of this object.
     */
    @SuppressWarnings("MethodDoesntCallSuperMethod")
    public CVector clone() {
        return new CVector(this.red, this.green, this.blue, this.code);
    }
}