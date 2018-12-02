package net.highwayfrogs.editor.file.vlo;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

/**
 * Contains information about what operations should and should not be applied.
 * Created by Kneesnap on 12/1/2018.
 */
@Getter
@Setter
@Accessors(chain = true)
public class ImageFilterSettings {
    private ImageState state;
    private boolean allowTransparency;
    private boolean allowFlip;
    private boolean trimEdges;

    public ImageFilterSettings(ImageState state) {
        this.state = state;
    }

    /**
     * Check if this is an imported image.
     * @return isImport
     */
    public boolean isImport() {
        return getState() == ImageState.IMPORT;
    }

    /**
     * Check if this is an exported image.
     * @return isExport
     */
    public boolean isExport() {
        return getState() == ImageState.IMPORT;
    }

    public enum ImageState {
        IMPORT, EXPORT
    }
}
