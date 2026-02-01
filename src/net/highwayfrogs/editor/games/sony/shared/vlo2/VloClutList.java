package net.highwayfrogs.editor.games.sony.shared.vlo2;

import net.highwayfrogs.editor.games.psx.PSXClutColor;
import net.highwayfrogs.editor.games.sony.SCGameInstance;
import net.highwayfrogs.editor.games.sony.SCGameObject.SCSharedGameObject;
import net.highwayfrogs.editor.games.sony.shared.vlo2.vram.VloTree;
import net.highwayfrogs.editor.games.sony.shared.vlo2.vram.VloVramSnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Represents a list of cluts.
 * Created by Kneesnap on 01/01/2026.
 */
public class VloClutList extends SCSharedGameObject {
    private final List<VloClut> cluts = new ArrayList<>();
    private final List<VloClut> immutableCluts = Collections.unmodifiableList(this.cluts);

    public VloClutList(SCGameInstance instance) {
        super(instance);
    }

    /**
     * Clear the contents of the clut list.
     */
    public void clear() {
        for (int i = 0; i < this.cluts.size(); i++)
            this.cluts.get(i).registered = false;
        this.cluts.clear();
    }

    /**
     * Returns an immutable list of cluts.
     */
    public List<VloClut> getCluts() {
        return this.immutableCluts;
    }

    /**
     * Adds a clut to the list.
     * @param clut the clut to add
     */
    boolean addClut(VloClut clut) {
        if (clut == null)
            throw new NullPointerException("clut");
        if (clut.isRegistered())
            return false;

        clut.registered = true;

        // Try to generate the clut position if it is currently invalid.
        boolean allowInvalidPosition = false;
        if (clut.getX() < 0 && clut.getY() < 0) { // If this happens, we should add it to update later.
            VloTree tree = getGameInstance().getVloTree();
            VloVramSnapshot snapshot = tree != null ? tree.getVramSnapshot(clut.getVloFile()) : null;
            if (snapshot != null) {
                allowInvalidPosition = true; // Allow invalid positions because they'll be cleaned up later.
                clut.getVloFile().markDirty();
                if (!snapshot.tryAddClut(clut))
                    tree.markForRebuild();
            }
        }

        if (!allowInvalidPosition) {
            // Ensure no overlap.
            for (int i = 0; i < this.cluts.size(); i++) {
                VloClut otherClut = this.cluts.get(i);
                if (otherClut.overlaps(clut))
                    throw new IllegalArgumentException(otherClut + " overlaps with the argument " + clut + ".");
            }
        }

        this.cluts.add(clut);
        return true;
    }

    /**
     * Removes a clut from the list
     * @param clut the clut to remove
     * @return true iff the clut was removed successfully
     */
    boolean removeClut(VloClut clut) {
        if (clut == null)
            throw new NullPointerException("clut");
        if (!clut.isRegistered())
            return false;

        // Identity check, since .remove() may possibly remove the wrong clut object.
        for (int i = 0; i < this.cluts.size(); i++) {
            if (this.cluts.get(i) == clut) {
                this.cluts.remove(i);
                clut.registered = false;
                return true;
            }
        }

        return false; // Registered to some other clut list?
    }

    /**
     * Gets an existing clut entry starting at the given x/y coordinates.
     * @param clutId the ID of the clut to resolve
     * @param errorIfNotFound if true, and no clut is found, an exception will be thrown.
     * @return foundClut
     */
    public VloClut getClutFromId(short clutId, boolean errorIfNotFound) {
        if (clutId == 0) {
            if (errorIfNotFound)
                throw new RuntimeException("The clut ID was zero. (No CLUT)");

            return null;
        }

        int clutX = VloClut.getClutX(clutId);
        int clutY = VloClut.getClutY(clutId);
        return getClutAtPos(clutX, clutY, errorIfNotFound);
    }

    /**
     * Gets an existing clut entry starting at the given x/y coordinates.
     * @param x the clut start x position
     * @param y the clut start y position
     * @param errorIfNotFound if true, and no clut is found, an exception will be thrown.
     * @return foundClut
     */
    public VloClut getClutAtPos(int x, int y, boolean errorIfNotFound) {
        for (int i = 0; i < this.cluts.size(); i++) {
            VloClut clut = this.cluts.get(i);
            if (clut.getX() == x && clut.getY() == y)
                return clut;
        }

        if (errorIfNotFound)
            throw new RuntimeException("Failed to find clut starting at coordinates [" + x + ", " + y + "].");

        return null;
    }

    /**
     * Finds a clut which perfectly matches the provided list.
     * @param clutColors the clut colors to resolve the clut for
     * @return clut
     */
    public VloClut getClut(List<PSXClutColor> clutColors) {
        if (clutColors == null)
            throw new NullPointerException("clutColors");

        for (int i = 0; i < this.cluts.size(); i++) {
            VloClut clut = this.cluts.get(i);
            if (clut.getColorCount() != clutColors.size())
                continue; // Wrong color count, so skip.

            // Ensure that all colors match.
            boolean allColorsMatch = true;
            for (int j = 0; j < clutColors.size(); j++) {
                PSXClutColor testColor = clutColors.get(j);
                PSXClutColor clutTestColor = clut.getColor(j);
                if (!testColor.equals(clutTestColor)) {
                    allColorsMatch = false;
                    break;
                }
            }

            if (allColorsMatch)
                return clut;
        }

        return null;
    }
}