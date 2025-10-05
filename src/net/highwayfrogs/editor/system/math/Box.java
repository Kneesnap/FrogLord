package net.highwayfrogs.editor.system.math;

import lombok.Getter;
import lombok.NonNull;

import java.util.Objects;

/**
 * Represents an axis-aligned 3D Box which can be used to perform 3D operations.
 * This is a backport from ModToolFramework.
 * Created by Kneesnap on 11/22/2024.
 */
@Getter
public class Box {
    private final Vector3f minPosition = new Vector3f();
    private final Vector3f maxPosition = new Vector3f();

    /**
     * Creates a new {@code Box}.
     */
    public Box() {
        this(0, 0, 0, 0, 0, 0);
    }

    /**
     * Creates a new {@code Box} from an existing {@code Box}.
     * @param box The box to copy values from.
     */
    public Box(@NonNull Box box) {
        this(box.getMinX(), box.getMinY(), box.getMinZ(), box.getMaxX(), box.getMaxY(), box.getMaxZ());
    }

    /**
     * Creates a new {@code Box} instance from xyz values.
     * @param x1 Either the minimum or maximum X position.
     * @param y1 Either the minimum or maximum Y position.
     * @param z1 Either the minimum or maximum Z position.
     * @param x2 Either the minimum or maximum X position.
     * @param y2 Either the minimum or maximum Y position.
     * @param z2 Either the minimum or maximum Z position.
     */
    public Box(float x1, float y1, float z1, float x2, float y2, float z2) {
        this.set(x1, y1, z1, x2, y2, z2);
    }

    /**
     * Gets the X position of the minimum corner.
     */
    public float getMinX() {
        return this.minPosition.getX();
    }

    /**
     * Sets the X position of the minimum corner.
     */
    public void setMinX(float newX) {
        if (!Float.isFinite(newX))
            throw new IllegalArgumentException("Invalid coordinate: " + newX);
        if (newX > this.maxPosition.getX())
            throw new IllegalArgumentException("The provided new value for minX was higher than the maxX! (New Min: " + newX + ", Current Max: " + this.maxPosition.getX() + ")");

        this.minPosition.setX(newX);
    }

    /**
     * Gets the Y position of the minimum corner.
     */
    public float getMinY() {
        return this.minPosition.getY();
    }

    /**
     * Sets the Y position of the minimum corner.
     */
    public void setMinY(float newY) {
        if (!Float.isFinite(newY))
            throw new IllegalArgumentException("Invalid coordinate: " + newY);
        if (newY > this.maxPosition.getY())
            throw new IllegalArgumentException("The provided new value for minY was higher than the maxY! (New Min: " + newY + ", Current Max: " + this.maxPosition.getY() + ")");

        this.minPosition.setY(newY);
    }

    /**
     * Gets the Z position of the minimum corner.
     */
    public float getMinZ() {
        return this.minPosition.getZ();
    }

    /**
     * Sets the Z position of the minimum corner.
     */
    public void setMinZ(float newZ) {
        if (!Float.isFinite(newZ))
            throw new IllegalArgumentException("Invalid coordinate: " + newZ);
        if (newZ > this.maxPosition.getZ())
            throw new IllegalArgumentException("The provided new value for minZ was higher than the maxZ! (New Min: " + newZ + ", Current Max: " + this.maxPosition.getZ() + ")");

        this.minPosition.setZ(newZ);
    }

    /**
     * Gets the X position of the maximum corner.
     */
    public float getMaxX() {
        return this.maxPosition.getX();
    }

    /**
     * Sets the X position of the maximum corner.
     */
    public void setMaxX(float newX) {
        if (!Float.isFinite(newX))
            throw new IllegalArgumentException("Invalid coordinate: " + newX);
        if (newX < this.minPosition.getX())
            throw new IllegalArgumentException("The provided new value for maxX was lower than the minX! (New Max: " + newX + ", Current Min: " + this.minPosition.getX() + ")");

        this.maxPosition.setX(newX);
    }

    /**
     * Gets the Y position of the maximum corner.
     */
    public float getMaxY() {
        return this.maxPosition.getY();
    }

    /**
     * Sets the Y position of the maximum corner.
     */
    public void setMaxY(float newY) {
        if (!Float.isFinite(newY))
            throw new IllegalArgumentException("Invalid coordinate: " + newY);
        if (newY < this.minPosition.getY())
            throw new IllegalArgumentException("The provided new value for maxY was lower than the minY! (New Max: " + newY + ", Current Min: " + this.minPosition.getY() + ")");

        this.maxPosition.setY(newY);
    }

    /**
     * Gets the Z position of the maximum corner.
     */
    public float getMaxZ() {
        return this.maxPosition.getZ();
    }

    /**
     * Sets the Z position of the maximum corner.
     */
    public void setMaxZ(float newZ) {
        if (!Float.isFinite(newZ))
            throw new IllegalArgumentException("Invalid coordinate: " + newZ);
        if (newZ < this.minPosition.getZ())
            throw new IllegalArgumentException("The provided new value for maxZ was lower than the minZ! (New Max: " + newZ + ", Current Min: " + this.minPosition.getZ() + ")");

        this.maxPosition.setZ(newZ);
    }

    /**
     * Gets the center position of the box.
     * @param output the output storage of the center position
     * @return centerPosition
     */
    public Vector3f getCenterPosition(Vector3f output) {
        if (output == null)
            output = new Vector3f();

        output.setXYZ((this.minPosition.getX() + this.maxPosition.getX()) * .5F,
                (this.minPosition.getY() + this.maxPosition.getY()) * .5F,
                (this.minPosition.getZ() + this.maxPosition.getZ()) * .5F);
        return output;
    }

    /**
     * Sets the center position of the box
     * @param newCenterPosition the center position to apply
     */
    public Box setCenterPos(Vector3f newCenterPosition) {
        if (newCenterPosition == null)
            throw new NullPointerException("newCenterPosition");

        return setCenterPos(newCenterPosition.getX(), newCenterPosition.getY(), newCenterPosition.getZ());
    }

    /**
     * Sets the center position of the box.
     * @param x the new center position x coordinate
     * @param y the new center position x coordinate
     * @param z the new center position x coordinate
     */
    public Box setCenterPos(float x, float y, float z) {
        if (!Float.isFinite(x))
            throw new IllegalArgumentException("The provided X value (" + x + ") is not a valid positional value.");
        if (!Float.isFinite(y))
            throw new IllegalArgumentException("The provided Y value (" + y + ") is not a valid positional value.");
        if (!Float.isFinite(z))
            throw new IllegalArgumentException("The provided Z value (" + z + ") is not a valid positional value.");


        float xLengthHalf = getWidth() * .5F;
        float yLengthHalf = getHeight() * .5F;
        float zLengthHalf = getDepth() * .5F;
        this.minPosition.setXYZ(x - xLengthHalf, y - yLengthHalf, z - zLengthHalf);
        this.maxPosition.setXYZ(x + xLengthHalf, y + yLengthHalf, z + zLengthHalf);
        return this;
    }

    /**
     * Gets the full width (x length) of the box.
     */
    public float getWidth() {
        return Math.abs(this.maxPosition.getX() - this.minPosition.getX());
    }

    /**
     * Sets the width (x length) of the box, without changing the center position.
     * @param newWidth the new width to apply
     * @return this
     */
    public Box setWidth(float newWidth) {
        if (!Float.isFinite(newWidth) || newWidth < 0)
            throw new IllegalArgumentException("Invalid width: " + newWidth);

        float centerX = (this.minPosition.getX() + this.maxPosition.getX()) * .5F;
        this.minPosition.setX(centerX - (newWidth * .5F));
        this.maxPosition.setX(centerX + (newWidth * .5F));
        return this;
    }

    /**
     * Gets the full height (y length) of the box.
     */
    public float getHeight() {
        return Math.abs(this.maxPosition.getY() - this.minPosition.getY());
    }

    /**
     * Sets the height (y length) of the box, without changing the center position.
     * @param newHeight the new height to apply
     * @return this
     */
    public Box setHeight(float newHeight) {
        if (!Float.isFinite(newHeight) || newHeight < 0)
            throw new IllegalArgumentException("Invalid height: " + newHeight);

        float centerY = (this.minPosition.getY() + this.maxPosition.getY()) * .5F;
        this.minPosition.setY(centerY - (newHeight * .5F));
        this.maxPosition.setY(centerY + (newHeight * .5F));
        return this;
    }

    /**
     * Gets the full depth (z length) of the box.
     */
    public float getDepth() {
        return Math.abs(this.maxPosition.getZ() - this.minPosition.getZ());
    }

    /**
     * Sets the depth (z length) of the box, without changing the center position.
     * @param newDepth the new depth to apply
     * @return this
     */
    public Box setDepth(float newDepth) {
        if (!Float.isFinite(newDepth) || newDepth < 0)
            throw new IllegalArgumentException("Invalid depth: " + newDepth);

        float centerZ = (this.minPosition.getZ() + this.maxPosition.getZ()) * .5F;
        this.minPosition.setZ(centerZ - (newDepth * .5F));
        this.maxPosition.setZ(centerZ + (newDepth * .5F));
        return this;
    }

    /**
     * Gets the dimensions of the box.
     * @param output the output storage of the box dimensions
     * @return boxDimensions
     */
    public Vector3f getDimensions(Vector3f output) {
        if (output == null)
            output = new Vector3f();

        output.setXYZ(Math.abs(this.maxPosition.getX() - this.minPosition.getX()),
                Math.abs(this.maxPosition.getY() - this.minPosition.getY()),
                Math.abs(this.maxPosition.getZ() - this.minPosition.getZ()));
        return output;
    }

    /**
     * Sets the dimensions of the box, without changing the center position.
     * @param newDimensions a vector containing the new dimensions to apply
     * @return this
     */
    public Box setDimensions(Vector3f newDimensions) {
        if (newDimensions == null)
            throw new NullPointerException("newDimensions");

        return setDimensions(newDimensions.getX(), newDimensions.getY(), newDimensions.getZ());
    }

    /**
     * Sets the dimensions of the box, without changing the center position.
     * @param newWidth the new width to apply
     * @param newHeight the new height to apply
     * @param newDepth the new depth to apply
     * @return this
     */
    public Box setDimensions(float newWidth, float newHeight, float newDepth) {
        if (!Float.isFinite(newWidth) || newWidth < 0)
            throw new IllegalArgumentException("Invalid width: " + newWidth);
        if (!Float.isFinite(newHeight) || newHeight < 0)
            throw new IllegalArgumentException("Invalid height: " + newHeight);
        if (!Float.isFinite(newDepth) || newDepth < 0)
            throw new IllegalArgumentException("Invalid depth: " + newDepth);

        float centerX = (this.minPosition.getX() + this.maxPosition.getX()) * .5F;
        float centerY = (this.minPosition.getY() + this.maxPosition.getY()) * .5F;
        float centerZ = (this.minPosition.getZ() + this.maxPosition.getZ()) * .5F;
        this.minPosition.setXYZ(centerX - (newWidth * .5F), centerY - (newHeight * .5F), centerZ - (newDepth * .5F));
        this.maxPosition.setXYZ(centerX + (newWidth * .5F), centerY + (newHeight * .5F), centerZ + (newDepth * .5F));
        return this;
    }

    /**
     * Sets the center position and dimensions of the box.
     * @param centerPosition the dimensions to apply
     * @param boxDimensions the dimensions to apply from the center point
     * @return this
     */
    public Box setCenterPositionAndDimensions(Vector3f centerPosition, Vector3f boxDimensions) {
        if (centerPosition == null)
            throw new NullPointerException("centerPosition");
        if (boxDimensions == null)
            throw new NullPointerException("boxDimensions");

        return this.setCenterPositionAndDimensions(centerPosition.getX(), centerPosition.getY(), centerPosition.getZ(),
                boxDimensions.getX(), boxDimensions.getY(), boxDimensions.getZ());
    }

    /**
     * Sets the center position and dimensions of the box.
     * @param centerX the x coordinate of the new center position
     * @param centerY the y coordinate of the new center position
     * @param centerZ the z coordinate of the new center position
     * @param width the width of the box from the center position
     * @param height the height of the box from the center position
     * @param depth the depth of the box from the center position
     * @return this
     */
    public Box setCenterPositionAndDimensions(float centerX, float centerY, float centerZ, float width, float height, float depth) {
        if (!Float.isFinite(centerX))
            throw new IllegalArgumentException("Invalid centerX: " + centerX);
        if (!Float.isFinite(centerY))
            throw new IllegalArgumentException("Invalid centerY: " + centerY);
        if (!Float.isFinite(centerZ))
            throw new IllegalArgumentException("Invalid centerZ: " + centerZ);
        if (!Float.isFinite(width) || width < 0)
            throw new IllegalArgumentException("Illegal width: " + width);
        if (!Float.isFinite(height) || height < 0)
            throw new IllegalArgumentException("Illegal height: " + height);
        if (!Float.isFinite(depth) || depth < 0)
            throw new IllegalArgumentException("Illegal depth: " + depth);

        return this.set(
                (centerX - (width * .5F)),
                (centerY - (height * .5F)),
                (centerZ - (depth * .5F)),
                (centerX + (width * .5F)),
                (centerY + (height * .5F)),
                (centerZ + (depth * .5F)));
    }

    /**
     * Sets the new box position/dimensions from another box.
     * @param box The box to get dimensions from.
     */
    public Box set(Box box) {
        return this.set(box.getMinX(), box.getMinY(), box.getMinZ(), box.getMaxX(), box.getMaxY(), box.getMaxZ());
    }

    /**
     * Sets the box corner positions.
     * @param x1 Either the minimum or maximum X position.
     * @param y1 Either the minimum or maximum Y position.
     * @param z1 Either the minimum or maximum Z position.
     * @param x2 Either the minimum or maximum X position.
     * @param y2 Either the minimum or maximum Y position.
     * @param z2 Either the minimum or maximum Z position.
     */
    public Box set(float x1, float y1, float z1, float x2, float y2, float z2) {
        if (!Float.isFinite(x1))
            throw new IllegalArgumentException("The provided x1 value (" + x1 + ") is not a valid positional value.");
        if (!Float.isFinite(y1))
            throw new IllegalArgumentException("The provided y1 value (" + y1 + ") is not a valid positional value.");
        if (!Float.isFinite(z1))
            throw new IllegalArgumentException("The provided z1 value (" + z1 + ") is not a valid positional value.");
        if (!Float.isFinite(x2))
            throw new IllegalArgumentException("The provided x2 value (" + x2 + ") is not a valid positional value.");
        if (!Float.isFinite(y2))
            throw new IllegalArgumentException("The provided y2 value (" + y2 + ") is not a valid positional value.");
        if (!Float.isFinite(z2))
            throw new IllegalArgumentException("The provided z2 value (" + z2 + ") is not a valid positional value.");

        this.minPosition.setXYZ(Math.min(x1, x2), Math.min(y1, y2), Math.min(z1, z2));
        this.maxPosition.setXYZ(Math.max(x1, x2), Math.max(y1, y2), Math.max(z1, z2));
        return this;
    }

    /**
     * Checks whether a position is located within this box.
     * @param position the position to check.
     * @param boundaryMode The boundary mode used to handle box edge behavior
     * @return Whether the box contains the position.
     */
    public boolean contains(Vector3f position, BoxBoundaryMode boundaryMode) {
        if (position == null)
            throw new IllegalArgumentException("position");
        
        return this.contains(position.getX(), position.getY(), position.getZ(), boundaryMode);
    }

    /**
     * Checks whether a position is located within this box.
     * @param x the x coordinate of the position to check.
     * @param y the y coordinate of the position to check.
     * @param z the z coordinate of the position to check.
     * @param boundaryMode The boundary mode used to handle box edge behavior
     * @return Whether the box contains the position.
     */
    public boolean contains(float x, float y, float z, BoxBoundaryMode boundaryMode) {
        switch (boundaryMode) {
            case INCLUSIVE:
                return (x >= this.minPosition.getX()) && (y >= this.minPosition.getY()) && (z >= this.minPosition.getZ())
                        && (this.maxPosition.getX() >= x) && (this.maxPosition.getY() >= y) && (this.maxPosition.getZ() >= z);
            case EXCLUSIVE:
                return (x > this.minPosition.getX()) && (y > this.minPosition.getY()) && (z > this.minPosition.getZ())
                        && (this.maxPosition.getX() > x) && (this.maxPosition.getY() > y) && (this.maxPosition.getZ() > z);
            case INCLUSIVE_MIN_EXCLUSIVE_MAX:
                return (x >= this.minPosition.getX()) && (y >= this.minPosition.getY()) && (z >= this.minPosition.getZ())
                        && (this.maxPosition.getX() > x) && (this.maxPosition.getY() > y) && (this.maxPosition.getZ() > z);
            case EXCLUSIVE_MIN_INCLUSIVE_MAX:
                return (x > this.minPosition.getX()) && (y > this.minPosition.getY()) && (z > this.minPosition.getZ())
                        && (this.maxPosition.getX() >= x) && (this.maxPosition.getY() >= y) && (this.maxPosition.getZ() >= z);
            default:
                throw new IllegalArgumentException("Unknown BoxBoundaryMode: " + boundaryMode);
        }
    }

    /**
     * Test whether another box is contained within this box.
     * @param otherBox the box to test
     * @param boundaryMode The boundary mode used to handle box edge behavior
     * @return true iff the other box is fully contained by this box
     */
    public boolean contains(Box otherBox, BoxBoundaryMode boundaryMode) {
        if (otherBox == null)
            throw new NullPointerException("otherBox");

        switch (boundaryMode) {
            case INCLUSIVE:
                return (otherBox.getMinX() >= this.getMinX()) && (otherBox.getMinY() >= this.getMinY()) && (otherBox.getMinZ() >= this.getMinZ())
                        && (otherBox.getMaxX() <= this.getMaxX()) && (otherBox.getMaxY() <= this.getMaxY()) && (otherBox.getMaxZ() <= this.getMaxZ());
            case EXCLUSIVE:
                return (otherBox.getMinX() > this.getMinX()) && (otherBox.getMinY() > this.getMinY()) && (otherBox.getMinZ() > this.getMinZ())
                        && (otherBox.getMaxX() < this.getMaxX()) && (otherBox.getMaxY() < this.getMaxY()) && (otherBox.getMaxZ() < this.getMaxZ());
            case INCLUSIVE_MIN_EXCLUSIVE_MAX:
                return (otherBox.getMinX() >= this.getMinX()) && (otherBox.getMinY() >= this.getMinY()) && (otherBox.getMinZ() >= this.getMinZ())
                        && (otherBox.getMaxX() < this.getMaxX()) && (otherBox.getMaxY() < this.getMaxY()) && (otherBox.getMaxZ() < this.getMaxZ());
            case EXCLUSIVE_MIN_INCLUSIVE_MAX:
                return (otherBox.getMinX() > this.getMinX()) && (otherBox.getMinY() > this.getMinY()) && (otherBox.getMinZ() > this.getMinZ())
                        && (otherBox.getMaxX() <= this.getMaxX()) && (otherBox.getMaxY() <= this.getMaxY()) && (otherBox.getMaxZ() <= this.getMaxZ());
            default:
                throw new IllegalArgumentException("Unknown BoxBoundaryMode: " + boundaryMode);
        }
    }

    /**
     * Tests whether this box shapes any space with the provided box.
     * If either box contains each other, or they intersect, this will return true.
     * Overlap is defined as sharing either box sharing any area, including when one box fully encompasses the other.
     * @param otherBox the box to test
     * @param boundaryMode The boundary mode used to handle box edge behavior
     * @return true, iff either the box contains the other box OR intersects with the other box
     */
    public boolean overlaps(Box otherBox, BoxBoundaryMode boundaryMode) {
        if (otherBox == null)
            throw new NullPointerException("otherBox");

        switch (boundaryMode) {
            case INCLUSIVE:
                return ((otherBox.getMinX() <= this.getMaxX()) && (otherBox.getMaxX() >= this.getMinX()))
                        && ((otherBox.getMinY() <= this.getMaxY()) && (otherBox.getMaxY() >= this.getMinY()))
                        && ((otherBox.getMinZ() <= this.getMaxZ()) && (otherBox.getMaxZ() >= this.getMinZ()));
            case EXCLUSIVE:
                return ((otherBox.getMinX() < this.getMaxX()) && (otherBox.getMaxX() > this.getMinX()))
                        && ((otherBox.getMinY() < this.getMaxY()) && (otherBox.getMaxY() > this.getMinY()))
                        && ((otherBox.getMinZ() < this.getMaxZ()) && (otherBox.getMaxZ() > this.getMinZ()));
            case INCLUSIVE_MIN_EXCLUSIVE_MAX:
                return ((otherBox.getMinX() < this.getMaxX()) && (otherBox.getMaxX() >= this.getMinX()))
                        && ((otherBox.getMinY() < this.getMaxY()) && (otherBox.getMaxY() >= this.getMinY()))
                        && ((otherBox.getMinZ() < this.getMaxZ()) && (otherBox.getMaxZ() >= this.getMinZ()));
            case EXCLUSIVE_MIN_INCLUSIVE_MAX:
                return ((otherBox.getMinX() <= this.getMaxX()) && (otherBox.getMaxX() > this.getMinX()))
                        && ((otherBox.getMinY() <= this.getMaxY()) && (otherBox.getMaxY() > this.getMinY()))
                        && ((otherBox.getMinZ() <= this.getMaxZ()) && (otherBox.getMaxZ() > this.getMinZ()));
            default:
                throw new IllegalArgumentException("Unknown BoxBoundaryMode: " + boundaryMode);
        }
    }

    /**
     * Tests whether this intersects any with the provided box.
     * Intersection is defined the boxes sharing area, but neither box fully encompassing the other.
     * @param otherBox the box to test
     * @param boundaryMode The boundary mode used to handle box edge behavior
     * @return true, iff either the box intersects with the other box
     */
    public boolean intersects(Box otherBox, BoxBoundaryMode boundaryMode) {
        if (otherBox == null)
            throw new NullPointerException("otherBox");

        return overlaps(otherBox, boundaryMode)
                && !contains(otherBox, boundaryMode)
                && !otherBox.contains(this, boundaryMode);
    }

    @Override
    public boolean equals(Object other) {
        return (other instanceof Box)
                && Objects.equals(this.minPosition, ((Box) other).minPosition)
                && Objects.equals(this.maxPosition, ((Box) other).maxPosition);
    }

    @Override
    public int hashCode() {
        return this.minPosition.hashCode() ^ this.maxPosition.hashCode();
    }

    /**
     * Reads the contents of this box from a string
     * @param input the input to parse
     */
    public void parse(String input, BoxDisplayMode displayMode) {
        if (input == null)
            throw new NullPointerException("input");

        String[] split = input.split(",?\\s+");
        float[] values = new float[6];
        if (split.length != values.length)
            throw new NumberFormatException("'" + input + "' cannot be parsed as a Box because it appears to have " + split.length + " values.");

        for (int i = 0; i < split.length; i++)
            values[i] = Float.parseFloat(split[i]);

        switch (displayMode) {
            case MIN_AND_MAX:
                this.minPosition.setXYZ(values[0], values[1], values[2]);
                this.maxPosition.setXYZ(values[3], values[4], values[5]);
                break;
            case CENTER_AND_DIMENSIONS:
                setCenterPositionAndDimensions(values[0], values[1], values[2], values[3], values[4], values[5]);
                break;
            default:
                throw new IllegalArgumentException("Unknown BoxDisplayMode: " + displayMode);
        }
    }

    /**
     * Gets this box as a string which can be parsed.
     */
    public String toParseableString(BoxDisplayMode displayMode) {
        switch (displayMode) {
            case MIN_AND_MAX:
                return this.minPosition.getX() + ", " + this.minPosition.getY() + ", " + this.minPosition.getZ() + ", "
                        + this.maxPosition.getX() + ", " + this.maxPosition.getY() + ", " + this.maxPosition.getZ();
            case CENTER_AND_DIMENSIONS:
                Vector3f centerPosition = getCenterPosition(null);
                return centerPosition.getX() + ", " + centerPosition.getY() + ", " + centerPosition.getZ()
                        + ", " + getWidth() + ", " + getHeight() + ", " + getDepth();
            default:
                throw new IllegalArgumentException("Unknown BoxDisplayMode: " + displayMode);
        }
    }

    @Override
    public String toString() {
        return "Box{minX=" + this.minPosition.getX()
                + ",minY=" + this.minPosition.getY()
                + ",minZ=" + this.minPosition.getZ()
                + ",maxX=" + this.maxPosition.getX()
                + ",maxY=" + this.maxPosition.getY()
                + ",maxZ=" + this.maxPosition.getZ()
                + "}";
    }

    public enum BoxDisplayMode {
        MIN_AND_MAX, CENTER_AND_DIMENSIONS
    }
    
    public enum BoxBoundaryMode {
        INCLUSIVE, EXCLUSIVE, INCLUSIVE_MIN_EXCLUSIVE_MAX, EXCLUSIVE_MIN_INCLUSIVE_MAX;
    }
}
