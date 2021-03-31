/*
 *  Copyright (C) 2020 GReD
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

package fr.igred.omero.repository;


import fr.igred.omero.Client;
import fr.igred.omero.GenericObjectWrapper;
import fr.igred.omero.annotations.MapAnnotationWrapper;
import fr.igred.omero.annotations.TagAnnotationWrapper;
import fr.igred.omero.exception.AccessException;
import fr.igred.omero.exception.ServiceException;
import fr.igred.omero.exception.OMEROServerError;
import fr.igred.omero.roi.ROIWrapper;
import fr.igred.omero.repository.PixelsWrapper.Coordinates;
import fr.igred.omero.repository.PixelsWrapper.Bounds;
import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.measure.Calibration;
import ij.process.ImageProcessor;
import loci.common.DataTools;
import loci.formats.FormatTools;
import omero.ServerError;
import omero.api.RawFileStorePrx;
import omero.gateway.exception.DSAccessException;
import omero.gateway.exception.DSOutOfServiceException;
import omero.gateway.facility.ROIFacility;
import omero.gateway.model.AnnotationData;
import omero.gateway.model.ChannelData;
import omero.gateway.model.FolderData;
import omero.gateway.model.ImageData;
import omero.gateway.model.MapAnnotationData;
import omero.gateway.model.ROIData;
import omero.gateway.model.ROIResult;
import omero.gateway.model.TagAnnotationData;
import omero.model.*;
import omero.model.enums.ChecksumAlgorithmSHA1160;
import org.apache.commons.io.FilenameUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.sql.Timestamp;
import java.util.*;
import java.util.concurrent.ExecutionException;

import static fr.igred.omero.exception.ExceptionHandler.handleServiceOrAccess;


/**
 * Class containing an ImageData.
 * <p> Implements function using the ImageData contained
 */
public class ImageWrapper extends GenericObjectWrapper<ImageData> {


    /**
     * Constructor of the class ImageWrapper
     *
     * @param image The image contained in the ImageWrapper.
     */
    public ImageWrapper(ImageData image) {
        super(image);
    }


    /**
     * Gets the ImageData name
     *
     * @return name.
     */
    public String getName() {
        return data.getName();
    }


    /**
     * Gets the ImageData description
     *
     * @return description.
     */
    public String getDescription() {
        return data.getDescription();
    }


    /**
     * Gets the ImageData acquisition date
     *
     * @return acquisition date.
     */
    public Timestamp getAcquisitionDate() {
        return data.getAcquisitionDate();
    }


    /**
     * @return ImageData contained.
     */
    public ImageData getImage() {
        return data;
    }


    /**
     * Adds a tag to the image in OMERO. The tag is created from the name and description in parameters.
     *
     * @param client      The user.
     * @param name        Name of the tag.
     * @param description Description of the tag.
     *
     * @throws ServiceException   Cannot connect to OMERO.
     * @throws AccessException    Cannot access data.
     * @throws ExecutionException A Facility can't be retrieved or instantiated.
     */
    public void addTag(Client client, String name, String description)
    throws ServiceException, AccessException, ExecutionException {
        TagAnnotationData tagData = new TagAnnotationData(name);
        tagData.setTagDescription(description);

        addTag(client, tagData);
    }


    /**
     * Adds a tag to the image in OMERO. The tag to be added is already created.
     *
     * @param client The user.
     * @param tag    TagAnnotationWrapper containing the tag to be added.
     *
     * @throws ServiceException   Cannot connect to OMERO.
     * @throws AccessException    Cannot access data.
     * @throws ExecutionException A Facility can't be retrieved or instantiated.
     */
    public void addTag(Client client, TagAnnotationWrapper tag)
    throws ServiceException, AccessException, ExecutionException {
        addTag(client, tag.getTag());
    }


    /**
     * Private function. Adds a tag to the image in OMERO.
     *
     * @param client  The user.
     * @param tagData Tag to be added.
     *
     * @throws ServiceException   Cannot connect to OMERO.
     * @throws AccessException    Cannot access data.
     * @throws ExecutionException A Facility can't be retrieved or instantiated.
     */
    private void addTag(Client client, TagAnnotationData tagData)
    throws ServiceException, AccessException, ExecutionException {
        ImageAnnotationLink link = new ImageAnnotationLinkI();
        link.setChild(tagData.asAnnotation());
        link.setParent(data.asImage());

        client.save(link);
    }


    /**
     * Adds a tag to the image in OMERO. The tag id is used.
     *
     * @param client The user.
     * @param id     Id of the tag.
     *
     * @throws ServiceException   Cannot connect to OMERO.
     * @throws AccessException    Cannot access data.
     * @throws ExecutionException A Facility can't be retrieved or instantiated.
     */
    public void addTag(Client client, Long id)
    throws ServiceException, AccessException, ExecutionException {
        ImageAnnotationLink link = new ImageAnnotationLinkI();
        link.setChild(new TagAnnotationI(id, false));
        link.setParent(data.asImage());

        client.save(link);
    }


    /**
     * Adds multiple tag to the image in OMERO.
     *
     * @param client The user.
     * @param tags   Table of TagAnnotationWrapper to add.
     *
     * @throws ServiceException   Cannot connect to OMERO.
     * @throws AccessException    Cannot access data.
     * @throws ExecutionException A Facility can't be retrieved or instantiated.
     */
    public void addTags(Client client, TagAnnotationWrapper... tags)
    throws ServiceException, AccessException, ExecutionException {
        for (TagAnnotationWrapper tag : tags) {
            addTag(client, tag.getTag());
        }
    }


    /**
     * Adds multiple tag to the image in OMERO. The tags id is used
     *
     * @param client The user.
     * @param ids    Table of tag id to add.
     *
     * @throws ServiceException   Cannot connect to OMERO.
     * @throws AccessException    Cannot access data.
     * @throws ExecutionException A Facility can't be retrieved or instantiated.
     */
    public void addTags(Client client, Long... ids)
    throws ServiceException, AccessException, ExecutionException {
        for (Long id : ids) {
            addTag(client, id);
        }
    }


    /**
     * Gets all tag linked to an image in OMERO
     *
     * @param client The user.
     *
     * @return List of TagAnnotationWrapper each containing a tag linked to the image.
     *
     * @throws ServiceException   Cannot connect to OMERO.
     * @throws AccessException    Cannot access data.
     * @throws ExecutionException A Facility can't be retrieved or instantiated.
     */
    public List<TagAnnotationWrapper> getTags(Client client)
    throws ServiceException, AccessException, ExecutionException {
        List<TagAnnotationWrapper> tags = new ArrayList<>();

        List<Long> userIds = new ArrayList<>();
        userIds.add(client.getId());

        List<Class<? extends AnnotationData>> types = new ArrayList<>();
        types.add(TagAnnotationData.class);

        List<AnnotationData> annotations = null;
        try {
            annotations = client.getMetadata().getAnnotations(client.getCtx(), data, types, userIds);
        } catch (DSOutOfServiceException | DSAccessException e) {
            handleServiceOrAccess(e, "Cannot get tags for image ID: " + getId());
        }

        if (annotations != null) {
            for (AnnotationData annotation : annotations) {
                TagAnnotationData tagAnnotation = (TagAnnotationData) annotation;

                tags.add(new TagAnnotationWrapper(tagAnnotation));
            }
        }

        tags.sort(new SortById<>());
        return tags;
    }


    /**
     * Gets the List of NamedValue (Key-Value pair) associated to an image.
     *
     * @param client The user.
     *
     * @return Collection of NamedValue.
     *
     * @throws ServiceException   Cannot connect to OMERO.
     * @throws AccessException    Cannot access data.
     * @throws ExecutionException A Facility can't be retrieved or instantiated.
     */
    public List<NamedValue> getKeyValuePairs(Client client)
    throws ServiceException, AccessException, ExecutionException {
        List<NamedValue> keyValuePairs = new ArrayList<>();

        List<Long> userIds = new ArrayList<>();
        userIds.add(client.getId());

        List<Class<? extends AnnotationData>> types = new ArrayList<>();
        types.add(MapAnnotationData.class);

        List<AnnotationData> annotations = null;
        try {
            annotations = client.getMetadata().getAnnotations(client.getCtx(), data, types, userIds);
        } catch (DSOutOfServiceException | DSAccessException e) {
            handleServiceOrAccess(e, "Cannot get k/v pairs for image ID: " + getId());
        }

        if (annotations != null) {
            for (AnnotationData annotation : annotations) {
                MapAnnotationData mapAnnotation = (MapAnnotationData) annotation;

                @SuppressWarnings("unchecked")
                List<NamedValue> list = (List<NamedValue>) mapAnnotation.getContent();

                keyValuePairs.addAll(list);
            }
        }

        return keyValuePairs;
    }


    /**
     * Gets the value from a Key-Value pair associated to the image
     *
     * @param client The user.
     * @param key    Key researched.
     *
     * @return Value associated to the key.
     *
     * @throws ServiceException       Cannot connect to OMERO.
     * @throws AccessException        Cannot access data.
     * @throws NoSuchElementException Key not found.
     * @throws ExecutionException     A Facility can't be retrieved or instantiated.
     */
    public String getValue(Client client, String key)
    throws ServiceException, AccessException, NoSuchElementException, ExecutionException {
        Collection<NamedValue> keyValuePairs = getKeyValuePairs(client);

        for (NamedValue namedValue : keyValuePairs) {
            if (namedValue.name.equals(key)) {
                return namedValue.value;
            }
        }

        throw new NoSuchElementException("Key value pair " + key + " not found");
    }


    /**
     * Adds a List of Key-Value pair to the image The list is contained in the MapAnnotationWrapper
     *
     * @param client The user.
     * @param data   MapAnnotationWrapper containing a list of NamedValue.
     *
     * @throws ServiceException   Cannot connect to OMERO.
     * @throws AccessException    Cannot access data.
     * @throws ExecutionException A Facility can't be retrieved or instantiated.
     */
    public void addMapAnnotation(Client client, MapAnnotationWrapper data)
    throws ServiceException, AccessException, ExecutionException {
        try {
            client.getDm().attachAnnotation(client.getCtx(),
                                            data.getMapAnnotation(),
                                            new ImageData(new ImageI(this.data.getId(), false)));
        } catch (DSOutOfServiceException | DSAccessException e) {
            handleServiceOrAccess(e, "Cannot add k/v pairs to image ID: " + getId());
        }
    }


    /**
     * Adds a single Key-Value pair to the image.
     *
     * @param client The user.
     * @param key    Name of the key.
     * @param value  Value associated to the key.
     *
     * @throws ServiceException   Cannot connect to OMERO.
     * @throws AccessException    Cannot access data.
     * @throws ExecutionException A Facility can't be retrieved or instantiated.
     */
    public void addPairKeyValue(Client client, String key, String value)
    throws ServiceException, AccessException, ExecutionException {
        List<NamedValue> result = new ArrayList<>();
        result.add(new NamedValue(key, value));

        MapAnnotationData data = new MapAnnotationData();
        data.setContent(result);
        try {
            client.getDm().attachAnnotation(client.getCtx(), data, new ImageData(new ImageI(this.data.getId(), false)));
        } catch (DSOutOfServiceException | DSAccessException e) {
            handleServiceOrAccess(e, "Cannot add k/v pair to image ID: " + getId());
        }
    }


    /**
     * Links a ROI to the image in OMERO
     * <p> DO NOT USE IT IF A SHAPE WAS DELETED !!!
     *
     * @param client The user.
     * @param roi    ROI to be added.
     *
     * @throws ServiceException   Cannot connect to OMERO.
     * @throws AccessException    Cannot access data.
     * @throws ExecutionException A Facility can't be retrieved or instantiated.
     */
    public void saveROI(Client client, ROIWrapper roi)
    throws ServiceException, AccessException, ExecutionException {
        try {
            ROIData roiData = client.getRoiFacility()
                                    .saveROIs(client.getCtx(),
                                              data.getId(),
                                              Collections.singletonList(roi.getROI()))
                                    .iterator().next();
            roi.setData(roiData);
        } catch (DSOutOfServiceException | DSAccessException e) {
            handleServiceOrAccess(e, "Cannot link ROI to image ID: " + getId());
        }
    }


    /**
     * Gets all ROIs linked to the image in OMERO
     *
     * @param client The user.
     *
     * @return List of ROIs linked to the image.
     *
     * @throws ServiceException   Cannot connect to OMERO.
     * @throws AccessException    Cannot access data.
     * @throws ExecutionException A Facility can't be retrieved or instantiated.
     */
    public List<ROIWrapper> getROIs(Client client)
    throws ServiceException, AccessException, ExecutionException {
        List<ROIWrapper> roiWrappers = new ArrayList<>();
        List<ROIResult>  roiResults  = new ArrayList<>();
        try {
            roiResults = client.getRoiFacility().loadROIs(client.getCtx(), data.getId());
        } catch (DSOutOfServiceException | DSAccessException e) {
            handleServiceOrAccess(e, "Cannot get ROIs from image ID: " + getId());
        }
        ROIResult r = roiResults.iterator().next();

        Collection<ROIData> rois = r.getROIs();

        for (ROIData roi : rois) {
            ROIWrapper temp = new ROIWrapper(roi);
            roiWrappers.add(temp);
        }

        return roiWrappers;
    }


    /**
     * Gets the list of Folder linked to the image Associate the folder to the image
     *
     * @param client The user.
     *
     * @return List of FolderWrapper containing the folder.
     *
     * @throws ServiceException   Cannot connect to OMERO.
     * @throws AccessException    Cannot access data.
     * @throws ExecutionException A Facility can't be retrieved or instantiated.
     */
    public List<FolderWrapper> getFolders(Client client)
    throws ServiceException, AccessException, ExecutionException {
        List<FolderWrapper> roiFolders  = new ArrayList<>();
        ROIFacility         roiFacility = client.getRoiFacility();

        Collection<FolderData> folders = new ArrayList<>();
        try {
            folders = roiFacility.getROIFolders(client.getCtx(), this.data.getId());
        } catch (DSOutOfServiceException | DSAccessException e) {
            handleServiceOrAccess(e, "Cannot get folders for image ID: " + getId());
        }

        for (FolderData folder : folders) {
            FolderWrapper roiFolder = new FolderWrapper(folder);
            roiFolder.setImage(this.data.getId());

            roiFolders.add(roiFolder);
        }

        return roiFolders;
    }


    /**
     * Gets the folder with the specified id on OMERO.
     *
     * @param client   The user.
     * @param folderId Id of the folder.
     *
     * @return The folder if it exist.
     *
     * @throws ServiceException Cannot connect to OMERO.
     * @throws OMEROServerError Server error.
     */
    public FolderWrapper getFolder(Client client, Long folderId) throws ServiceException, OMEROServerError {
        List<IObject> os = client.findByQuery("select f " +
                                              "from Folder as f " +
                                              "where f.id = " +
                                              folderId);

        FolderWrapper folderWrapper = new FolderWrapper((Folder) os.get(0));
        folderWrapper.setImage(this.data.getId());

        return folderWrapper;
    }


    /**
     * Gets the PixelsWrapper of the image
     *
     * @return Contains the PixelsData associated with the image.
     */
    public PixelsWrapper getPixels() {
        return new PixelsWrapper(data.getDefaultPixels());
    }


    /**
     * Generates the ImagePlus from the ij library corresponding to the image from OMERO WARNING : you need to include
     * the ij library to use this function
     *
     * @param client The user.
     *
     * @return ImagePlus generated from the current image.
     *
     * @throws AccessException    If an error occurs while retrieving the plane data from the pixels source.
     * @throws ExecutionException A Facility can't be retrieved or instantiated.
     */
    public ImagePlus toImagePlus(Client client) throws AccessException, ExecutionException {
        return this.toImagePlus(client, null, null, null, null, null);
    }


    /**
     * Gets the imagePlus generated from the image from OMERO corresponding to the bound
     *
     * @param client The user.
     * @param xBound Array containing the X bound from which the pixels should be retrieved.
     * @param yBound Array containing the Y bound from which the pixels should be retrieved.
     * @param cBound Array containing the C bound from which the pixels should be retrieved.
     * @param zBound Array containing the Z bound from which the pixels should be retrieved.
     * @param tBound Array containing the T bound from which the pixels should be retrieved.
     *
     * @return an ImagePlus from the ij library.
     *
     * @throws AccessException    If an error occurs while retrieving the plane data from the pixels source.
     * @throws ExecutionException A Facility can't be retrieved or instantiated.
     */
    public ImagePlus toImagePlus(Client client, int[] xBound, int[] yBound, int[] cBound, int[] zBound, int[] tBound)
    throws AccessException, ExecutionException {
        PixelsWrapper pixels = this.getPixels();

        Bounds bounds = pixels.getBounds(xBound, yBound, cBound, zBound, tBound);

        int startX = bounds.getStart().getX();
        int startY = bounds.getStart().getY();
        int startC = bounds.getStart().getC();
        int startZ = bounds.getStart().getZ();
        int startT = bounds.getStart().getT();

        int sizeX = bounds.getSize().getX();
        int sizeY = bounds.getSize().getY();
        int sizeC = bounds.getSize().getC();
        int sizeZ = bounds.getSize().getZ();
        int sizeT = bounds.getSize().getT();

        Length spacingX = pixels.getPixelSizeX();
        Length spacingY = pixels.getPixelSizeY();
        Length spacingZ = pixels.getPixelSizeZ();

        int pixelType = FormatTools.pixelTypeFromString(pixels.getPixelType());
        int bpp       = FormatTools.getBytesPerPixel(pixelType);

        ImagePlus imp = IJ.createHyperStack(data.getName(), sizeX, sizeY, sizeC, sizeZ, sizeT, bpp * 8);

        Calibration cal = imp.getCalibration();

        if (spacingX != null) {
            cal.setXUnit(spacingX.getUnit().name());
            cal.pixelWidth = spacingX.getValue();
        }
        if (spacingY != null) {
            cal.setYUnit(spacingY.getUnit().name());
            cal.pixelHeight = spacingY.getValue();
        }
        if (spacingZ != null) {
            cal.setZUnit(spacingZ.getUnit().name());
            cal.pixelDepth = spacingZ.getValue();
        }

        imp.setCalibration(cal);

        boolean isFloat = FormatTools.isFloatingPoint(pixelType);

        ImageStack stack = imp.getImageStack();

        double min = imp.getProcessor().getMin();
        double max = 0;

        for (int t = 0; t < sizeT; t++) {
            int posT = t + startT;
            for (int z = 0; z < sizeZ; z++) {
                int posZ = z + startZ;
                for (int c = 0; c < sizeC; c++) {
                    int posC = c + startC;

                    Coordinates pos = new Coordinates(startX, startY, posC, posZ, posT);

                    byte[] tiles = pixels.getRawTile(client, pos, sizeX, sizeY, bpp);

                    int n = imp.getStackIndex(c + 1, z + 1, t + 1);
                    stack.setPixels(DataTools.makeDataArray(tiles, bpp, isFloat, false), n);
                    ImageProcessor ip = stack.getProcessor(n);
                    ip.resetMinAndMax();

                    max = Math.max(ip.getMax(), max);
                    min = Math.min(ip.getMin(), min);

                    stack.setProcessor(ip, n);
                }
            }
        }

        imp.setStack(stack);
        imp.setOpenAsHyperStack(true);
        imp.setDisplayMode(IJ.COMPOSITE);

        imp.getProcessor().setMinAndMax(min, max);

        return imp;
    }


    /**
     * Gets the name of the channel
     *
     * @param client The user.
     * @param index  Channel number.
     *
     * @return name of the channel.
     *
     * @throws ServiceException   Cannot connect to OMERO.
     * @throws AccessException    Cannot access data.
     * @throws ExecutionException A Facility can't be retrieved or instantiated.
     */
    public String getChannelName(Client client, int index)
    throws ServiceException, AccessException, ExecutionException {
        List<ChannelData> channels = new ArrayList<>();
        try {
            channels = client.getMetadata().getChannelData(client.getCtx(), this.data.getId());
        } catch (DSOutOfServiceException | DSAccessException e) {
            handleServiceOrAccess(e, "Cannot get the channel name for image ID: " + getId());
        }

        return channels.get(index).getChannelLabeling();
    }


    /**
     * Links a file to the Dataset
     *
     * @param client The user.
     * @param file   File to add.
     *
     * @return ID of the file created in OMERO.
     *
     * @throws ServiceException      Cannot connect to OMERO.
     * @throws AccessException       Cannot access data on server.
     * @throws ExecutionException    A Facility can't be retrieved or instantiated.
     * @throws OMEROServerError      Server error.
     * @throws FileNotFoundException The file could not be found.
     * @throws IOException           If an I/O error occurs.
     */
    public long addFile(Client client, File file) throws
                                                  ServiceException,
                                                  AccessException,
                                                  ExecutionException,
                                                  OMEROServerError,
                                                  IOException {
        final int INC = 262144;

        ImageAnnotationLink newLink;
        RawFileStorePrx     rawFileStore;

        String name         = file.getName();
        String absolutePath = file.getAbsolutePath();
        String path         = absolutePath.substring(0, absolutePath.length() - name.length());

        OriginalFile originalFile = new OriginalFileI();
        originalFile.setName(omero.rtypes.rstring(name));
        originalFile.setPath(omero.rtypes.rstring(path));
        originalFile.setSize(omero.rtypes.rlong(file.length()));

        final ChecksumAlgorithm checksumAlgorithm = new ChecksumAlgorithmI();
        checksumAlgorithm.setValue(omero.rtypes.rstring(ChecksumAlgorithmSHA1160.value));
        originalFile.setHasher(checksumAlgorithm);
        originalFile.setMimetype(omero.rtypes.rstring(FilenameUtils.getExtension(file.getName())));
        originalFile = (OriginalFile) client.save(originalFile);

        try {
            rawFileStore = client.getGateway().getRawFileService(client.getCtx());
        } catch (DSOutOfServiceException oos) {
            throw new ServiceException(oos, oos.getConnectionStatus());
        }

        long       pos = 0;
        int        rLen;
        byte[]     buf = new byte[INC];
        ByteBuffer bBuf;
        try (FileInputStream stream = new FileInputStream(file)) {
            rawFileStore.setFileId(originalFile.getId().getValue());
            while ((rLen = stream.read(buf)) > 0) {
                rawFileStore.write(buf, pos, rLen);
                pos += rLen;
                bBuf = ByteBuffer.wrap(buf);
                bBuf.limit(rLen);
            }
            originalFile = rawFileStore.save();
            rawFileStore.close();
        } catch (ServerError se) {
            throw new OMEROServerError(se);
        }

        FileAnnotation fa = new FileAnnotationI();
        fa.setFile(originalFile);
        fa.setDescription(omero.rtypes.rstring(""));
        fa.setNs(omero.rtypes.rstring(file.getName()));

        fa = (FileAnnotation) client.save(fa);

        ImageAnnotationLink link = new ImageAnnotationLinkI();
        link.setChild(fa);
        link.setParent(data.asImage());
        newLink = (ImageAnnotationLink) client.save(link);

        return newLink.getChild().getId().getValue();
    }

}
