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
    private boolean trimEdges;
    private boolean allowTransparency;
    private boolean allowFlip;

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
        return getState() == ImageState.EXPORT;
    }

    /**
     * Reset all values in this setting object.
     */
    public void resetState() {
        this.trimEdges = false;
        this.allowTransparency = false;
        this.allowFlip = false;
    }

    public enum ImageState {
        IMPORT, EXPORT
    }
}
