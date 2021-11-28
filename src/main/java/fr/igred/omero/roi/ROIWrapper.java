/*
 *  Copyright (C) 2020-2021 GReD
 *
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.

 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.

 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 51 Franklin
 * Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */

package fr.igred.omero.roi;


import fr.igred.omero.Client;
import fr.igred.omero.GenericObjectWrapper;
import fr.igred.omero.exception.OMEROServerError;
import fr.igred.omero.exception.ServiceException;
import fr.igred.omero.repository.ImageWrapper;
import fr.igred.omero.repository.PixelsWrapper;
import ij.gui.Arrow;
import ij.gui.Line;
import ij.gui.OvalRoi;
import ij.gui.PointRoi;
import ij.gui.PolygonRoi;
import ij.gui.ShapeRoi;
import ij.gui.TextRoi;
import omero.ServerError;
import omero.gateway.exception.DSOutOfServiceException;
import omero.gateway.model.ROIData;
import omero.gateway.model.ShapeData;
import omero.model.Roi;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static fr.igred.omero.exception.ExceptionHandler.handleServiceOrServer;


/**
 * Class containing a ROIData
 * <p> Implements function using the ROIData contained
 */
public class ROIWrapper extends GenericObjectWrapper<ROIData> {

    /**
     * Default IJ property to store ROI local IDs / indices.
     */
    public static final String IJ_PROPERTY = "ROI";


    /**
     * Constructor of the ROIWrapper class.
     */
    public ROIWrapper() {
        super(new ROIData());
    }


    /**
     * Constructor of the ROIWrapper class.
     *
     * @param shapes List of shapes to add to the ROIData.
     */
    public ROIWrapper(Iterable<? extends GenericShapeWrapper<?>> shapes) {
        super(new ROIData());

        for (GenericShapeWrapper<?> shape : shapes)
            addShape(shape);
    }


    /**
     * Constructor of the ROIWrapper class.
     *
     * @param roi ROIData to be contained.
     */
    public ROIWrapper(ROIData roi) {
        super(roi);
    }


    /**
     * Checks the provided property.
     *
     * @param property The property where 4D ROI local ID is stored.
     *
     * @return The property, or the default value {@link #IJ_PROPERTY} (= {@value #IJ_PROPERTY}) if it is null or empty.
     */
    public static String checkProperty(String property) {
        if (property == null || property.trim().isEmpty()) return IJ_PROPERTY;
        else return property;
    }


    /**
     * Returns ID property corresponding to input local ID property (appends "_ID" to said property).
     *
     * @param property The property where 4D ROI local ID is stored, defaults to {@value #IJ_PROPERTY} if null or
     *                 empty.
     *
     * @return See above.
     */
    public static String ijIDProperty(String property) {
        property = checkProperty(property);
        return property + "_ID";
    }


    /**
     * Converts an ImageJ list of ROIs to a list of OMERO ROIs
     *
     * @param ijRois A list of ImageJ ROIs.
     *
     * @return The converted list of OMERO ROIs.
     */
    public static List<ROIWrapper> fromImageJ(List<ij.gui.Roi> ijRois) {
        return fromImageJ(ijRois, IJ_PROPERTY);
    }


    /**
     * Converts an ImageJ list of ROIs to a list of OMERO ROIs
     *
     * @param ijRois   A list of ImageJ ROIs.
     * @param property The property where 4D ROI local ID is stored. Defaults to {@value #IJ_PROPERTY} if null or
     *                 empty.
     *
     * @return The converted list of OMERO ROIs.
     */
    public static List<ROIWrapper> fromImageJ(List<? extends ij.gui.Roi> ijRois, String property) {
        property = checkProperty(property);
        final Map<Long, ROIWrapper> rois4D = new TreeMap<>();

        final Map<Integer, ROIWrapper> shape2roi = new TreeMap<>();

        for (int i = 0; i < ijRois.size(); i++) {
            final String value = ijRois.get(i).getProperty(property);
            if (value != null && value.matches("-?\\d+")) {
                final long id = Long.parseLong(value);
                rois4D.computeIfAbsent(id, val -> new ROIWrapper());
                shape2roi.put(i, rois4D.get(id));
            } else {
                shape2roi.put(i, new ROIWrapper());
            }
        }

        for (Map.Entry<Integer, ROIWrapper> entry : shape2roi.entrySet()) {
            final ij.gui.Roi ijRoi = ijRois.get(entry.getKey());
            final ROIWrapper roi   = entry.getValue();
            roi.addShape(ijRoi);
        }
        return shape2roi.values().stream().distinct().collect(Collectors.toList());
    }


    /**
     * Converts an OMERO list of ROIs to a list of ImageJ ROIs
     *
     * @param rois A list of OMERO ROIs.
     *
     * @return The converted list of ImageJ ROIs.
     */
    public static List<ij.gui.Roi> toImageJ(Collection<ROIWrapper> rois) {
        return toImageJ(rois, IJ_PROPERTY);
    }


    /**
     * Converts an OMERO list of ROIs to a list of ImageJ ROIs
     *
     * @param rois     A list of OMERO ROIs.
     * @param property The property where 4D ROI local ID will be stored. Defaults to {@value #IJ_PROPERTY} if null or
     *                 empty.
     *
     * @return The converted list of ImageJ ROIs.
     */
    public static List<ij.gui.Roi> toImageJ(Collection<ROIWrapper> rois, String property) {
        property = checkProperty(property);
        final int maxGroups = 255;
        final int nShapes   = rois.stream().map(ROIWrapper::getShapes).mapToInt(ShapeList::size).sum();

        final List<ij.gui.Roi> ijRois = new ArrayList<>(nShapes);

        int index = 1;
        for (ROIWrapper roi : rois) {
            final List<ij.gui.Roi> shapes = roi.toImageJ(property);
            for (ij.gui.Roi r : shapes) {
                r.setProperty(property, String.valueOf(index));
                if (rois.size() < maxGroups) {
                    r.setGroup(index);
                }
            }
            ijRois.addAll(shapes);
            index++;
        }
        return ijRois;
    }


    /**
     * Changes the wrapped data.
     *
     * @param data The ROI data.
     */
    public void setData(ROIData data) {
        this.data = data;
    }


    /**
     * Adds ShapeData objects from a list of GenericShapeWrapper to the ROIData
     *
     * @param shapes List of GenericShapeWrapper.
     */
    public void addShapes(Iterable<? extends GenericShapeWrapper<?>> shapes) {
        for (GenericShapeWrapper<?> shape : shapes)
            addShape(shape);
    }


    /**
     * Adds a ShapeData from a GenericShapeWrapper to the ROIData
     *
     * @param shape GenericShapeWrapper to add.
     */
    public void addShape(GenericShapeWrapper<?> shape) {
        data.addShapeData(shape.asShapeData());
    }


    /**
     * Returns the list of shapes contained in the ROIData
     *
     * @return list of shape contained in the ROIData.
     */
    public ShapeList getShapes() {
        final ShapeList shapes = new ShapeList();
        for (ShapeData shape : data.getShapes()) {
            shapes.add(shape);
        }
        return shapes;
    }


    /**
     * Sets the image linked to the ROI.
     *
     * @param image Image linked to the ROIData.
     */
    public void setImage(ImageWrapper image) {
        data.setImage(image.asImageData().asImage());
    }


    /**
     * Returns the ROIData contained.
     *
     * @return the {@link ROIData} contained.
     */
    public ROIData asROIData() {
        return data;
    }


    /**
     * Deletes a ShapeData from the ROIData.
     *
     * @param shape ShapeData to delete.
     */
    public void deleteShape(ShapeData shape) {
        data.removeShapeData(shape);
    }


    /**
     * Deletes a ShapeData from the ROIData.
     *
     * @param pos Position of the ShapeData in the ShapeData list from the ROIData.
     *
     * @throws IndexOutOfBoundsException If pos is out of the ShapeData list bounds.
     */
    public void deleteShape(int pos) {
        data.removeShapeData(data.getShapes().get(pos));
    }


    /**
     * Saves the ROI.
     *
     * @param client The client handling the connection.
     *
     * @throws ServiceException Cannot connect to OMERO.
     * @throws OMEROServerError Server error.
     */
    public void saveROI(Client client) throws OMEROServerError, ServiceException {
        try {
            final Roi roi =
                    (Roi) client.getGateway().getUpdateService(client.getCtx()).saveAndReturnObject(data.asIObject());
            data = new ROIData(roi);
        } catch (DSOutOfServiceException | ServerError e) {
            handleServiceOrServer(e, "Cannot save ROI");
        }
    }


    /**
     * Returns the 5D bounds containing the ROI.
     *
     * @return The 5D bounds.
     */
    public PixelsWrapper.Bounds getBounds() {
        final int[] x = {Integer.MAX_VALUE, Integer.MIN_VALUE};
        final int[] y = {Integer.MAX_VALUE, Integer.MIN_VALUE};
        final int[] c = {Integer.MAX_VALUE, Integer.MIN_VALUE};
        final int[] z = {Integer.MAX_VALUE, Integer.MIN_VALUE};
        final int[] t = {Integer.MAX_VALUE, Integer.MIN_VALUE};
        for (GenericShapeWrapper<?> shape : getShapes()) {
            final RectangleWrapper box = shape.getBoundingBox();
            x[0] = Math.min(x[0], (int) box.getX());
            y[0] = Math.min(y[0], (int) box.getY());
            c[0] = Math.min(c[0], box.getC());
            z[0] = Math.min(z[0], box.getZ());
            t[0] = Math.min(t[0], box.getT());
            x[1] = Math.max(x[1], (int) (box.getX() + box.getWidth() - 1));
            y[1] = Math.max(y[1], (int) (box.getY() + box.getHeight() - 1));
            c[1] = Math.max(c[1], box.getC());
            z[1] = Math.max(z[1], box.getZ());
            t[1] = Math.max(t[1], box.getT());
        }
        final PixelsWrapper.Coordinates start = new PixelsWrapper.Coordinates(x[0], y[0], c[0], z[0], t[0]);
        final PixelsWrapper.Coordinates end   = new PixelsWrapper.Coordinates(x[1], y[1], c[1], z[1], t[1]);
        return new PixelsWrapper.Bounds(start, end);
    }


    /**
     * Convert ROI to ImageJ list of ROIs.
     *
     * @return A list of ROIs.
     */
    public List<ij.gui.Roi> toImageJ() {
        return toImageJ(IJ_PROPERTY);
    }


    /**
     * Convert ROI to ImageJ list of ROIs.
     *
     * @param property The property where 4D ROI local ID will be stored.
     *
     * @return A list of ROIs.
     */
    public List<ij.gui.Roi> toImageJ(String property) {
        property = checkProperty(property);

        final ShapeList        shapes = getShapes();
        final List<ij.gui.Roi> rois   = new ArrayList<>(shapes.size());
        for (GenericShapeWrapper<?> shape : shapes) {
            final ij.gui.Roi roi = shape.toImageJ();
            if (shape.getText().isEmpty()) {
                roi.setName(String.format("%d-%d", getId(), shape.getId()));
            } else {
                roi.setName(shape.getText());
            }
            roi.setProperty(ijIDProperty(property), String.valueOf(getId()));
            rois.add(roi);
        }
        return rois;
    }


    /**
     * Adds an ImageJ ROI to an OMERO ROI.
     *
     * @param ijRoi The ImageJ ROI.
     */
    private void addShape(ij.gui.Roi ijRoi) {
        final String arrow = "Arrow";

        final int c = Math.max(0, ijRoi.getCPosition() - 1);
        final int z = Math.max(0, ijRoi.getZPosition() - 1);
        final int t = Math.max(0, ijRoi.getTPosition() - 1);

        if (ijRoi instanceof TextRoi) {
            final String text = ((TextRoi) ijRoi).getText();

            final double x = ijRoi.getBounds().getX();
            final double y = ijRoi.getBounds().getY();

            final TextWrapper shape = new TextWrapper(text, x, y);
            shape.setCZT(c, z, t);
            addShape(shape);
        } else if (ijRoi instanceof OvalRoi) {
            final double x = ijRoi.getBounds().getX();
            final double y = ijRoi.getBounds().getY();
            final double w = ijRoi.getBounds().getWidth();
            final double h = ijRoi.getBounds().getHeight();

            final EllipseWrapper shape = new EllipseWrapper(x + w / 2, y + h / 2, w / 2, h / 2);
            shape.setText(ijRoi.getName());
            shape.setCZT(c, z, t);
            addShape(shape);
        } else if (ijRoi instanceof Arrow) {
            final double x1 = ((Line) ijRoi).x1d;
            final double x2 = ((Line) ijRoi).x2d;
            final double y1 = ((Line) ijRoi).y1d;
            final double y2 = ((Line) ijRoi).y2d;

            final LineWrapper shape = new LineWrapper(x1, y1, x2, y2);
            shape.asShapeData().getShapeSettings().setMarkerEnd(arrow);
            if (((Arrow) ijRoi).getDoubleHeaded()) {
                shape.asShapeData().getShapeSettings().setMarkerStart(arrow);
            }
            shape.setText(ijRoi.getName());
            shape.setCZT(c, z, t);
            addShape(shape);
        } else if (ijRoi instanceof Line) {
            final double x1 = ((Line) ijRoi).x1d;
            final double x2 = ((Line) ijRoi).x2d;
            final double y1 = ((Line) ijRoi).y1d;
            final double y2 = ((Line) ijRoi).y2d;

            final LineWrapper shape = new LineWrapper(x1, y1, x2, y2);
            shape.setText(ijRoi.getName());
            shape.setCZT(c, z, t);
            addShape(shape);
        } else if (ijRoi instanceof PointRoi) {
            final int[] x = ijRoi.getPolygon().xpoints;
            final int[] y = ijRoi.getPolygon().ypoints;

            final List<PointWrapper> points = new LinkedList<>();
            IntStream.range(0, x.length)
                     .forEach(i -> points.add(new PointWrapper(x[i], y[i])));
            points.forEach(p -> p.setText(ijRoi.getName()));
            points.forEach(p -> p.setCZT(c, z, t));
            points.forEach(this::addShape);
        } else if (ijRoi instanceof PolygonRoi) {
            final String type = ijRoi.getTypeAsString();

            final int[] x = ijRoi.getPolygon().xpoints;
            final int[] y = ijRoi.getPolygon().ypoints;

            final List<Point2D.Double> points = new LinkedList<>();
            IntStream.range(0, x.length).forEach(i -> points.add(new Point2D.Double(x[i], y[i])));

            final GenericShapeWrapper<?> shape;
            if ("Polyline".equals(type) || "Freeline".equals(type) || "Angle".equals(type)) {
                shape = new PolylineWrapper(points);
            } else {
                shape = new PolygonWrapper(points);
            }
            shape.setText(ijRoi.getName());
            shape.setCZT(c, z, t);
            addShape(shape);
        } else if (ijRoi instanceof ShapeRoi) {
            final ij.gui.Roi[] rois = ((ShapeRoi) ijRoi).getRois();
            IntStream.range(0, rois.length).forEach(i -> rois[i].setName(ijRoi.getName()));
            IntStream.range(0, rois.length).forEach(i -> rois[i].setPosition(ijRoi.getCPosition(),
                                                                             ijRoi.getZPosition(),
                                                                             ijRoi.getTPosition()));
            IntStream.range(0, rois.length).forEach(i -> addShape(rois[i]));
        } else if (ijRoi.getType() == ij.gui.Roi.RECTANGLE) {
            final double x = ijRoi.getBounds().getX();
            final double y = ijRoi.getBounds().getY();
            final double w = ijRoi.getBounds().getWidth();
            final double h = ijRoi.getBounds().getHeight();

            final RectangleWrapper shape = new RectangleWrapper(x, y, w, h);

            shape.setText(ijRoi.getName());
            shape.setCZT(c, z, t);
            addShape(shape);
        }
    }

}