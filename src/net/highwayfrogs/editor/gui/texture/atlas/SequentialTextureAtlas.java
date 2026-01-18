package net.highwayfrogs.editor.gui.texture.atlas;

import net.highwayfrogs.editor.utils.objects.SortedList;

import java.util.Arrays;

/**
 * This atlas tries to fit textures in sequentially.
 * This may be slower than using SimpleTreeTextureAtlas sometimes, but it should produce more tightly packed results.
 * It's also significantly faster when updating textures in real-time.
 * However, it still doesn't scale well enough for larger texture sheets.
 * This is a backport from ModToolFramework.
 * Created by Kneesnap on 9/23/2023.
 */
public class SequentialTextureAtlas extends BasicTextureAtlas<AtlasTexture> {
    private AtlasTexture[][] _cachedTextureLocations;
    private int[] _cachedTextureStartXPositions;
    private int lastInsertedYPosition;

    public SequentialTextureAtlas(int startingWidth, int startingHeight, boolean allowAutomaticResizing) {
        super(startingWidth, startingHeight, allowAutomaticResizing, AtlasTexture::new);
        this.makeCache(startingWidth, startingHeight);
    }

    private void makeCache(int width, int height) {
        this._cachedTextureStartXPositions = new int[height];
        this._cachedTextureLocations = new AtlasTexture[height][width];
    }

    private void clearCacheAndVerifyItIsLargeEnough() {
        int oldAtlasWidth = this._cachedTextureLocations[0].length;
        int oldAtlasHeight = this._cachedTextureLocations.length;
        if (getAtlasWidth() > oldAtlasWidth || getAtlasHeight() > oldAtlasHeight) {
            this.makeCache(getAtlasWidth() * 2, getAtlasHeight() * 2);
        } else {
            Arrays.fill(this._cachedTextureStartXPositions, 0);
            for (int i = 0; i < this._cachedTextureLocations.length; i++)
                Arrays.fill(this._cachedTextureLocations[i], null);
        }
    }

    @Override
    protected boolean updatePositions(SortedList<AtlasTexture> sortedTextureList) {
        this.clearCacheAndVerifyItIsLargeEnough();
        return super.updatePositions(sortedTextureList);
    }

    @Override
    protected boolean placeTexture(AtlasTexture texture) {
        boolean foundSpot = false;
        int texturePaddedWidth = texture.getPaddedWidth();
        int texturePaddedHeight = texture.getPaddedHeight();

        boolean retryIfFail = (this.lastInsertedYPosition > 0);

        for (int y = 0; y <= getAtlasHeight() - texturePaddedHeight && !foundSpot; y++) {
            int x = this._cachedTextureStartXPositions[y];

            int lastX = x;
            boolean canUpdateMinimumX = true;
            boolean canUpdateMinimumXIfFoundThisIteration = true;
            while (getAtlasWidth() >= x + texturePaddedWidth && !foundSpot) {
                int endX = x + texturePaddedWidth;
                int endY = y + texturePaddedHeight;
                if (!canUpdateMinimumX)
                    canUpdateMinimumXIfFoundThisIteration = false;

                AtlasTexture temp = this._cachedTextureLocations[y][endX - 1];
                if (temp == null)
                    canUpdateMinimumX = false;

                // Lazy fast checks. (Check corners, and if it's a top corner, skip past the end of the image because it's the same y level.)
                if ((temp != null) || ((temp = this._cachedTextureLocations[y][x]) != null)) {
                    lastX = x;
                    x = temp.getX() + temp.getPaddedWidth();
                    continue;
                } else if ((temp = this._cachedTextureLocations[endY - 1][x]) != null || (temp = this._cachedTextureLocations[endY - 1][endX - 1]) != null) {
                    // We don't update lastX here because the y value of these corners are not on the current line.
                    x = temp.getX() + temp.getPaddedWidth();
                    continue;
                }

                // We need to check if there's room for the texture.
                // We could iterate through every pixel and check if a texture is there, but I came up with a better idea.
                // Because we add higher area textures before lower area textures, any existing texture which might overlap must have an area >= the current texture's area.
                // There is logically no way that a texture with an area >= the current texture's area can fit inside the current texture without also touching one of the current texture's borders.
                // Therefore, all we need to do is check if the borders overlap. This takes the amount of checks down from the area to the perimeter, which is a massive improvement.

                foundSpot = true;

                // Vertical Checks:
                for (int searchY = y + 1; searchY < endY - 1 && foundSpot; searchY++)
                    if (((temp = this._cachedTextureLocations[searchY][endX - 1]) != null) || ((temp = this._cachedTextureLocations[searchY][x]) != null))
                        foundSpot = false;

                // Horizontal Checks:
                for (int searchX = endX - 1; searchX > x && foundSpot; searchX--) // Search in backwards direction to find the furthest right position to place the texture at. (Most notable when near small textures)
                    if (((temp = this._cachedTextureLocations[y][searchX]) != null) || ((temp = this._cachedTextureLocations[endY - 1][searchX]) != null))
                        foundSpot = false;

                if (!foundSpot) { // We'll just skip past it.
                    lastX = x;
                    x = temp.getX() + temp.getPaddedWidth();
                }
            }

            if (foundSpot) {
                this.applyTexturePositionAndUpdateCache(texture, x, y, lastX, canUpdateMinimumXIfFoundThisIteration);
                this.lastInsertedYPosition = y;
            }
        }

        // If we couldn't find space, try again from the beginning.
        if (!foundSpot && retryIfFail) {
            this.lastInsertedYPosition = 0;
            return placeTexture(texture);
        }

        return foundSpot;
    }

    private void applyTexturePositionAndUpdateCache(AtlasTexture texture, int x, int y, int lastX, boolean canUpdateStartPos) {
        texture.setPosition(x, y);

        // Writes information to cache.
        for (int writerY = y; writerY < y + texture.getPaddedHeight(); writerY++) {
            // Update start position caching if we won't be creating a gap.
            if (canUpdateStartPos && (this._cachedTextureStartXPositions[writerY] == lastX)) {
                int furthestX = this._cachedTextureStartXPositions[writerY] = x + texture.getPaddedWidth();

                // Extend it further until it stops hitting textures.
                AtlasTexture temp;
                while (getAtlasWidth() > furthestX && (temp = this._cachedTextureLocations[writerY][furthestX]) != null)
                    furthestX = this._cachedTextureStartXPositions[writerY] = temp.getX() + temp.getPaddedWidth();
            }

            // Update caching of what textures are located where.
            Arrays.fill(this._cachedTextureLocations[writerY], x, x + texture.getPaddedWidth(), texture);
        }
    }

    @Override
    protected void freeTexture(AtlasTexture texture) {
        // Writes information to cache.
        int endX = texture.getX() + texture.getPaddedWidth();
        int endY = texture.getY() + texture.getPaddedHeight();
        for (int writerY = texture.getY(); writerY < endY; writerY++) {
            // Clear cache at the position of the removed texture.
            Arrays.fill(this._cachedTextureLocations[writerY], texture.getX(), endX, null);

            // Update start position to go all the way down to the earliest texture found.
            if (this._cachedTextureStartXPositions[writerY] == endX)
                this._cachedTextureStartXPositions[writerY] = texture.getX();
            for (; this._cachedTextureStartXPositions[writerY] > 0; this._cachedTextureStartXPositions[writerY]--)
                if (this._cachedTextureLocations[writerY][this._cachedTextureStartXPositions[writerY] - 1] != null)
                    break;
        }

        // Try inserting here next time.
        if (texture.getY() < this.lastInsertedYPosition)
            this.lastInsertedYPosition = texture.getY();
    }
}