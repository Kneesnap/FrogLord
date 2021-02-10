package net.highwayfrogs.editor.file.standard.psx;

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
import net.highwayfrogs.editor.file.GameObject;
import net.highwayfrogs.editor.file.map.poly.polygon.MAPPolyTexture;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.gui.GUIEditorGrid;
import net.highwayfrogs.editor.gui.InputMenu;
import net.highwayfrogs.editor.utils.Utils;

import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * Implements the PSX "CVECTOR" struct. Used for storing color data.
 * Created by Kneesnap on 8/25/2018.
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class PSXColorVector extends GameObject {
    private byte red;
    private byte green;
    private byte blue;
    private byte cd; // Might be alpha? Frogger seems to have 0xFF for all cases here, which would make sense.

    public static final int BYTE_LENGTH = 4 * Constants.BYTE_SIZE;

    @Override
    public void load(DataReader reader) {
        this.red = reader.readByte();
        this.green = reader.readByte();
        this.blue = reader.readByte();
        this.cd = reader.readByte();
    }

    @Override
    public void save(DataWriter writer) {
        writer.writeByte(this.red);
        writer.writeByte(this.green);
        writer.writeByte(this.blue);
        writer.writeByte(this.cd);
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof PSXColorVector))
            return false;

        PSXColorVector other = (PSXColorVector) obj;
        return getRed() == other.getRed() && getGreen() == other.getGreen() && getBlue() == other.getBlue() && getCd() == other.getCd();
    }

    @Override
    public int hashCode() {
        return ((0xFF & this.red) << 24) | ((0xFF & this.green) << 16) |
                ((0xFF & this.blue) << 8) | (0xFF & this.cd);
    }

    /**
     * Gets the red color value used for polygon shading.
     */
    public byte getShadingRed() {
        return (byte) ((this.red & Constants.BIT_FLAG_7) == Constants.BIT_FLAG_7 ? 0x7F : (this.red & 0x7F));
    }

    /**
     * Gets the green color value used for polygon shading.
     */
    public byte getShadingGreen() {
        return (byte) ((this.green & Constants.BIT_FLAG_7) == Constants.BIT_FLAG_7 ? 0x7F : (this.green & 0x7F));
    }

    /**
     * Gets the blue color value used for polygon shading.
     */
    public byte getShadingBlue() {
        return (byte) ((this.blue & Constants.BIT_FLAG_7) == Constants.BIT_FLAG_7 ? 0x7F : (this.blue & 0x7F));
    }

    /**
     * Gets the color used for shading.
     */
    public Color toShadeColor() {
        return new Color(getShadingRed(), getShadingGreen(), getShadingBlue(), 255);
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
        return new Color(Utils.byteToUnsignedShort(getRed()), Utils.byteToUnsignedShort(getGreen()), Utils.byteToUnsignedShort(getBlue()), (alpha & 0xFF));
    }

    /**
     * Turn this color into a shade RGB integer.
     * @return rgbValue
     */
    public int toShadeRGB() {
        return Utils.toRGB(getShadingRed(), getShadingGreen(), getShadingBlue());
    }

    /**
     * Turn this color into an RGB integer.
     * @return rgbValue
     */
    public int toRGB() {
        return Utils.toRGB(getRed(), getGreen(), getBlue());
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
     * Create a PSXColorVector from an RGB value.
     * @param rgbValue The rgb color value.
     * @return colorVector
     */
    public static PSXColorVector makeColorFromRGB(int rgbValue) {
        PSXColorVector vec = new PSXColorVector();
        vec.fromRGB(rgbValue);
        return vec;
    }

    /**
     * Adds a color vector to the editor.
     * This is not for picking a color, rather it's for the PSX gouraud shading settings.
     */
    public void setupEditor(GUIEditorGrid grid, String label, BufferedImage previewImage, Runnable onUpdate, boolean fullRange) {
        HBox box = new HBox();

        Runnable[] imageUpdate = new Runnable[1];
        Font useFont = Font.font(Font.getDefault().getSize() * .75D);
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

                    if ((colorRGB & 0b100000001000000010000000) != 0) {
                        Utils.makePopUp("Each value may not exceed $7F (127). Try Again.", AlertType.ERROR);
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

    private Label labelFont(String text, Font font) {
        javafx.scene.control.Label label = new Label(text);
        label.setFont(font);
        label.setWrapText(true);
        return label;
    }

    /**
     * Creates a copy of this object.
     */
    @SuppressWarnings("MethodDoesntCallSuperMethod")
    public PSXColorVector clone() {
        return new PSXColorVector(this.red, this.green, this.blue, this.cd);
    }
}
