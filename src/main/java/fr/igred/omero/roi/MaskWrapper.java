package fr.igred.omero.roi;


import omero.gateway.model.MaskData;


public class MaskWrapper extends ShapeWrapper<MaskData> {


    /**
     * Constructor of the MaskWrapper class using a MaskData.
     *
     * @param shape the shape
     */
    public MaskWrapper(MaskData shape) {
        super(shape);
    }


    /**
     * Constructor of the MaskWrapper class using a new empty MaskData.
     */
    public MaskWrapper() {
        this(new MaskData());
    }


    /**
     * Constructor of the MaskWrapper class using a new MaskData.
     *
     * @param x      The x-coordinate of the top-left corner of the image.
     * @param y      The y-coordinate of the top-left corner of the image.
     * @param width  The width of the image.
     * @param height The height of the image.
     * @param mask   The mask image.
     */
    public MaskWrapper(double x, double y, double width, double height, byte[] mask) {
        this(new MaskData(x, y, width, height, mask));
    }


    /**
     * Returns the x-coordinate of the top-left corner of the mask.
     *
     * @return See above.
     */
    public double getX() {
        return data.getX();
    }


    /**
     * Sets the x-coordinate top-left corner of an untransformed mask.
     *
     * @param x The value to set.
     */
    public void setX(double x) {
        data.setX(x);
    }


    /**
     * Returns the y-coordinate of the top-left corner of the mask.
     *
     * @return See above.
     */
    public double getY() {
        return data.getY();
    }


    /**
     * Sets the y-coordinate top-left corner of an untransformed mask.
     *
     * @param y See above.
     */
    public void setY(double y) {
        data.setY(y);
    }


    /**
     * Returns the width of the mask.
     *
     * @return See above.
     */
    public double getWidth() {
        return data.getWidth();
    }


    /**
     * Sets the width of an untransformed mask.
     *
     * @param width See above.
     */
    public void setWidth(double width) {
        data.setWidth(width);
    }


    /**
     * Returns the height of the mask.
     *
     * @return See above.
     */
    public double getHeight() {
        return data.getHeight();
    }


    /**
     * Sets the height of an untransformed mask.
     *
     * @param height See above.
     */
    public void setHeight(double height) {
        data.setHeight(height);
    }


    /**
     * Returns the mask image.
     *
     * @return See above.
     */
    public int[][] getMaskAsBinaryArray() {
        return data.getMaskAsBinaryArray();
    }


    /**
     * Returns the mask as a byte array.
     *
     * @return See above.
     */
    public byte[] getMask() {
        return data.getMask();
    }


    /**
     * Sets the mask image.
     *
     * @param mask See above.
     */
    public void setMask(byte[] mask) {
        data.setMask(mask);
    }


    /**
     * Sets the mask
     *
     * @param mask The binary mask (int[width][height])
     */
    public void setMask(int[][] mask) {
        data.setMask(mask);
    }


    /**
     * Sets the mask
     *
     * @param mask The binary mask (boolean[width][height])
     */
    public void setMask(boolean[][] mask) {
        data.setMask(mask);
    }


    /**
     * Sets the coordinates of the MaskData shape.
     *
     * @param x      The x-coordinate of the top-left corner.
     * @param y      The y-coordinate of the top-left corner.
     * @param width  The width of the rectangle.
     * @param height The height of the rectangle.
     */
    public void setCoordinates(double x, double y, double width, double height) {
        setX(x);
        setY(y);
        setWidth(width);
        setHeight(height);
    }


    /**
     * Gets the coordinates of the MaskData shape.
     *
     * @return Array of coordinates containing {X,Y,Width,Height}.
     */
    public double[] getCoordinates() {
        double[] coordinates = new double[4];
        coordinates[0] = getX();
        coordinates[1] = getY();
        coordinates[2] = getWidth();
        coordinates[3] = getHeight();
        return coordinates;
    }


    /**
     * Sets the coordinates of the MaskData shape.
     *
     * @param coordinates Array of coordinates containing {X,Y,Width,Height}.
     */
    public void setCoordinates(double[] coordinates) {
        if (coordinates == null) {
            throw new IllegalArgumentException("MaskData cannot set null coordinates.");
        } else if (coordinates.length == 4) {
            setCoordinates(coordinates[0], coordinates[1], coordinates[2], coordinates[3]);
        } else {
            throw new IllegalArgumentException("4 coordinates required for MaskData.");
        }
    }

}
