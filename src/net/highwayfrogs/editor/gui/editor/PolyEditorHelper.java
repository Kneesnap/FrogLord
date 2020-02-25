package net.highwayfrogs.editor.gui.editor;

import javafx.scene.control.Alert;
import javafx.scene.image.ImageView;
import net.highwayfrogs.editor.file.map.poly.MAPPrimitive;
import net.highwayfrogs.editor.file.map.poly.polygon.*;
import net.highwayfrogs.editor.file.map.view.TextureMap;
import net.highwayfrogs.editor.file.standard.psx.ByteUV;
import net.highwayfrogs.editor.file.standard.psx.PSXColorVector;
import net.highwayfrogs.editor.file.vlo.GameImage;
import net.highwayfrogs.editor.file.vlo.ImageFilterSettings;
import net.highwayfrogs.editor.file.vlo.VLOArchive;
import net.highwayfrogs.editor.gui.GUIEditorGrid;
import net.highwayfrogs.editor.gui.editor.MapUIController;
import net.highwayfrogs.editor.utils.Utils;

public class PolyEditorHelper {
	public static final String[] SINGLE_COLOR_NAME = {"Color"};
	public static final String[] TRI_COLOR_NAMES = {"Corner 1", "Corner 2", "Corner 3"};
	public static final String[] QUAD_COLOR_NAMES = {"Top Left", "Top Right", "Bottom Right", "Bottom Left"};
	public static final String[][] COLOR_BANK = {SINGLE_COLOR_NAME, null, TRI_COLOR_NAMES, QUAD_COLOR_NAMES};
	public static final ImageFilterSettings SHOW_SETTINGS = new ImageFilterSettings(ImageFilterSettings.ImageState.EXPORT).setTrimEdges(true);

	public static void addColorEditor(GUIEditorGrid editor, MapUIController controller, MAPPolyGouraud poly) {
		// Color Editor.
		if (poly.getColors() != null) {
			editor.addBoldLabel("Colors:");
			String[] nameArray = COLOR_BANK[poly.getColors().length - 1];
			for (int i = 0; i < poly.getColors().length; i++)
				editor.addColorPicker(nameArray[i], poly.getColors()[i].toRGB(), poly.getColors()[i]::fromRGB);
			//TODO: Update map display when color is updated. (Update texture map.)
		}
	}

	public static void addColorEditor(GUIEditorGrid editor, MapUIController controller, MAPPolyTexture poly) {
		// Color Editor.
		if (poly.getColors() != null) {
			editor.addBoldLabel("Colors:");
			String[] nameArray = COLOR_BANK[poly.getColors().length - 1];
			for (int i = 0; i < poly.getColors().length; i++)
				editor.addColorPicker(nameArray[i], poly.getColors()[i].toRGB(), poly.getColors()[i]::fromRGB);
			//TODO: Update map display when color is updated. (Update texture map.)
		}
	}

	public static void addColorEditor(GUIEditorGrid editor, MapUIController controller, MAPPolyFlat poly) {
		// Color Editor.
		if (poly.getColor() != null) {
			editor.addBoldLabel("Color:");
			String nameArray = SINGLE_COLOR_NAME[0];
			editor.addColorPicker(nameArray, poly.getColor().toRGB(), poly.getColor()::fromRGB);
			//TODO: Update map display when color is updated. (Update texture map.)
		}
	}

	public static void addQuadEditor(GUIEditorGrid editor, MapUIController controller, MAPPrimitive poly) {
		editor.addCheckBox("Quad", poly.isQuadFace(), newValue -> {
			int newSize = newValue ? 4 : 3;
			int copySize = Math.min(newSize, poly.getVertices().length);

			// Copy vertices.
			int[] newVertices = new int[newSize];
			System.arraycopy(poly.getVertices(), 0, newVertices, 0, copySize);
			poly.setVertices(newVertices);
		});
	}

	public static void addQuadEditor(GUIEditorGrid editor, MapUIController controller, MAPPolyTexture poly) {
		editor.addCheckBox("Quad", poly.isQuadFace(), newValue -> {
			int newSize = newValue ? 4 : 3;
			int copySize = Math.min(newSize, poly.getVertices().length);

			// Copy vertices.
			int[] newVertices = new int[newSize];
			System.arraycopy(poly.getVertices(), 0, newVertices, 0, copySize);
			poly.setVertices(newVertices);

			// Copy uvs.
			ByteUV[] newUvs = new ByteUV[newSize];
			if (poly.getUvs() != null)
				System.arraycopy(poly.getUvs(), 0, newUvs, 0, copySize);
			for (int i = 0; i < newUvs.length; i++)
				if (newUvs[i] == null)
					newUvs[i] = new ByteUV();
			poly.setUvs(newUvs);
		});
	}

	public static void addUvEditor(GUIEditorGrid editor, MapUIController controller, MAPPolyTexture poly) {
		// UVs. (TODO: Better editor? Maybe have sliders + a live preview?)
		if (poly.getUvs() != null) {
			for (int i = 0; i < poly.getUvs().length; i++)
				poly.getUvs()[i].setupEditor("UV #" + i, editor);
		}
	}

	public static void addTextureEditor(GUIEditorGrid editor, MapUIController controller, MAPPolyTexture poly) {
		TextureMap texMap = controller.getMapMesh().getTextureMap();
		VLOArchive suppliedVLO = controller.getController().getFile().getVlo();
		GameImage image = suppliedVLO.getImageByTextureId(texMap.getRemap(poly.getTextureId()));

		// Texture Preview. (Click -> change.)
		ImageView view = editor.addCenteredImage(image.toFXImage(SHOW_SETTINGS), 150);
		view.setOnMouseClicked(evt -> suppliedVLO.promptImageSelection(newImage -> {
			short newValue = newImage.getTextureId();
			if (texMap.getRemapList() != null)
				newValue = (short) texMap.getRemapList().indexOf(newValue);

			if (newValue == (short) -1) {
				Utils.makePopUp("This image is not part of the remap! It can't be used!",
						Alert.AlertType.INFORMATION); // Show this as a popup maybe.
				return;
			}

			poly.setTextureId(newValue);
			view.setImage(newImage.toFXImage(SHOW_SETTINGS));
			controller.getGeometryManager().refreshView();
		}, false));

		// Flags.
		for (MAPPolyTexture.PolyTextureFlag flag : MAPPolyTexture.PolyTextureFlag.values())
			editor.addCheckBox(Utils.capitalize(flag.name()), testFlag(flag, poly), newState -> setFlag(flag, newState, poly));

	}

	/**
	 * Test if a texture flag is present.
	 * @param flag The flag in question.
	 * @param poly Polygon to test.
	 * @return flagPresent
	 */
	private static boolean testFlag(MAPPolyTexture.PolyTextureFlag flag, MAPPolyTexture poly) {
		return (poly.getFlags() & flag.getFlag()) == flag.getFlag();
	}

	/**
	 * Set a texture flag state.
	 * @param flag     The flag to set.
	 * @param poly     Polygon to update.
	 * @param newState The new flag state.
	 */
	private static void setFlag(MAPPolyTexture.PolyTextureFlag flag, boolean newState, MAPPolyTexture poly) {
		boolean currentState = testFlag(flag, poly);
		if (currentState == newState)
			return; // Prevents the ^ operation from breaking the value.

		if (newState) {
			poly.setFlags((short) (poly.getFlags() | flag.getFlag()));
		} else {
			poly.setFlags((short) (poly.getFlags() ^ flag.getFlag()));
		}
	}

	private static void updateColors(MapUIController controller, MAPPolyGouraud poly) {
		int newSize = poly.isQuadFace() ? 4 : 3;
		if (poly.getColors().length == newSize)
			return;

		PSXColorVector[] newColors = new PSXColorVector[newSize];
		System.arraycopy(poly.getColors(), 0, newColors, 0, Math.min(poly.getColors().length, newSize));
		for (int i = 0; i < newColors.length; i++)
			if (newColors[i] == null)
				newColors[i] = new PSXColorVector();

		poly.setColors(newColors);
		controller.getGeometryManager().setupEditor(); // Update the editor.
	}
}
