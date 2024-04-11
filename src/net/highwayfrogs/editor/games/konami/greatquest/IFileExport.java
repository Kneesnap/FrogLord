package net.highwayfrogs.editor.games.konami.greatquest;

import java.io.File;
import java.io.IOException;

/**
 * Represents an object which can be exported to a directory.
 * Created by Kneesnap on 8/16/2023.
 */
public interface IFileExport {
    /**
     * Saves the contents of the object to the folder.
     * The definition of what is saved can differ between objects, but the general intent is to export files
     * in a form more accessible than their normal form. Eg: Saving a TGQ image as .png instead of .img.
     * @param folder The folder to save to.
     */
    void exportToFolder(File folder) throws IOException;
}