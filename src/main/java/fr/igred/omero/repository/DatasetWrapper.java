/*
 *  Copyright (C) 2020-2022 GReD
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
import fr.igred.omero.annotations.TagAnnotationWrapper;
import fr.igred.omero.exception.AccessException;
import fr.igred.omero.exception.OMEROServerError;
import fr.igred.omero.exception.ServiceException;
import fr.igred.omero.roi.ROIWrapper;
import loci.formats.in.DefaultMetadataOptions;
import loci.formats.in.MetadataLevel;
import ome.formats.OMEROMetadataStoreClient;
import ome.formats.importer.ImportCandidates;
import ome.formats.importer.ImportConfig;
import ome.formats.importer.ImportContainer;
import ome.formats.importer.ImportLibrary;
import ome.formats.importer.OMEROWrapper;
import ome.formats.importer.cli.ErrorHandler;
import ome.formats.importer.cli.LoggingImportMonitor;
import omero.RLong;
import omero.ServerError;
import omero.gateway.exception.DSAccessException;
import omero.gateway.exception.DSOutOfServiceException;
import omero.gateway.model.DatasetData;
import omero.gateway.model.ImageData;
import omero.model.DatasetI;
import omero.model.DatasetImageLink;
import omero.model.DatasetImageLinkI;
import omero.model.IObject;
import omero.model.Pixels;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import static fr.igred.omero.exception.ExceptionHandler.handleServiceOrAccess;


/**
 * Class containing a DatasetData.
 * <p> Implements function using the DatasetData contained
 */
public class DatasetWrapper extends GenericRepositoryObjectWrapper<DatasetData> {

    public static final String ANNOTATION_LINK = "DatasetAnnotationLink";

    private static final Long[] LONGS = new Long[0];


    /**
     * Constructor of the DatasetWrapper class
     *
     * @param name        Name of the dataset.
     * @param description Description of the dataset.
     */
    public DatasetWrapper(String name, String description) {
        super(new DatasetData());
        this.data.setName(name);
        this.data.setDescription(description);
    }


    /**
     * Constructor of the DatasetWrapper class
     *
     * @param dataset Dataset to be contained.
     */
    public DatasetWrapper(DatasetData dataset) {
        super(dataset);
    }


    /**
     * Gets the DatasetData name
     *
     * @return DatasetData name.
     */
    @Override
    public String getName() {
        return data.getName();
    }


    /**
     * Sets the name of the dataset.
     *
     * @param name The name of the dataset. Mustn't be {@code null}.
     *
     * @throws IllegalArgumentException If the name is {@code null}.
     */
    public void setName(String name) {
        data.setName(name);
    }


    /**
     * @return the DatasetData contained.
     */
    public DatasetData asDatasetData() {
        return data;
    }


    /**
     * Gets the DatasetData description
     *
     * @return DatasetData description.
     */
    @Override
    public String getDescription() {
        return data.getDescription();
    }


    /**
     * Sets the description of the dataset.
     *
     * @param description The description of the dataset.
     */
    public void setDescription(String description) {
        data.setDescription(description);
    }


    /**
     * Returns the type of annotation link for this object
     *
     * @return See above.
     */
    @Override
    protected String annotationLinkType() {
        return ANNOTATION_LINK;
    }


    /**
     * Gets all images in the dataset available from OMERO.
     *
     * @param client The client handling the connection.
     *
     * @return ImageWrapper list.
     *
     * @throws ServiceException   Cannot connect to OMERO.
     * @throws AccessException    Cannot access data.
     * @throws ExecutionException A Facility can't be retrieved or instantiated.
     */
    public List<ImageWrapper> getImages(Client client) throws ServiceException, AccessException, ExecutionException {
        Collection<ImageData> images = new ArrayList<>(0);
        try {
            images = client.getBrowseFacility()
                           .getImagesForDatasets(client.getCtx(),
                                                 Collections.singletonList(data.getId()));
        } catch (DSOutOfServiceException | DSAccessException e) {
            handleServiceOrAccess(e, "Cannot get images from " + this);
        }
        return wrap(images, ImageWrapper::new);
    }


    /**
     * Gets all images in the dataset with a certain from OMERO.
     *
     * @param client The client handling the connection.
     * @param name   Name searched.
     *
     * @return ImageWrapper list.
     *
     * @throws ServiceException   Cannot connect to OMERO.
     * @throws AccessException    Cannot access data.
     * @throws ExecutionException A Facility can't be retrieved or instantiated.
     */
    public List<ImageWrapper> getImages(Client client, String name)
    throws ServiceException, AccessException, ExecutionException {
        List<ImageWrapper> images = getImages(client);
        images.removeIf(image -> !image.getName().equals(name));
        return images;
    }


    /**
     * Gets all images in the dataset with a certain motif in their name from OMERO.
     *
     * @param client The client handling the connection.
     * @param motif  Motif searched in an image name.
     *
     * @return ImageWrapper list.
     *
     * @throws ServiceException   Cannot connect to OMERO.
     * @throws AccessException    Cannot access data.
     * @throws ExecutionException A Facility can't be retrieved or instantiated.
     */
    public List<ImageWrapper> getImagesLike(Client client, String motif)
    throws ServiceException, AccessException, ExecutionException {
        List<ImageWrapper> images = getImages(client);
        final String       regexp = ".*" + motif + ".*";
        images.removeIf(image -> !image.getName().matches(regexp));
        return images;
    }


    /**
     * Gets all images in the dataset tagged with a specified tag from OMERO.
     *
     * @param client The client handling the connection.
     * @param tag    TagAnnotationWrapper containing the tag researched.
     *
     * @return ImageWrapper list.
     *
     * @throws ServiceException   Cannot connect to OMERO.
     * @throws AccessException    Cannot access data.
     * @throws OMEROServerError   Server error.
     * @throws ExecutionException A Facility can't be retrieved or instantiated.
     */
    public List<ImageWrapper> getImagesTagged(Client client, TagAnnotationWrapper tag)
    throws ServiceException, AccessException, OMEROServerError, ExecutionException {
        return getImagesTagged(client, tag.getId());
    }


    /**
     * Gets all images in the dataset tagged with a specified tag from OMERO.
     *
     * @param client The client handling the connection.
     * @param tagId  Id of the tag researched.
     *
     * @return ImageWrapper list.
     *
     * @throws ServiceException   Cannot connect to OMERO.
     * @throws AccessException    Cannot access data.
     * @throws OMEROServerError   Server error.
     * @throws ExecutionException A Facility can't be retrieved or instantiated.
     */
    public List<ImageWrapper> getImagesTagged(Client client, Long tagId)
    throws ServiceException, AccessException, OMEROServerError, ExecutionException {
        Long[] ids = client.findByQuery("select link.parent " +
                                        "from ImageAnnotationLink link " +
                                        "where link.child = " +
                                        tagId +
                                        " and link.parent in " +
                                        "(select link2.child " +
                                        "from DatasetImageLink link2 " +
                                        "where link2.parent = " +
                                        data.getId() + ")")
                           .stream()
                           .map(IObject::getId)
                           .map(RLong::getValue)
                           .toArray(Long[]::new);
        return client.getImages(ids);
    }


    /**
     * Gets all images in the dataset with a certain key
     *
     * @param client The client handling the connection.
     * @param key    Name of the key researched.
     *
     * @return ImageWrapper list.
     *
     * @throws ServiceException   Cannot connect to OMERO.
     * @throws AccessException    Cannot access data.
     * @throws ExecutionException A Facility can't be retrieved or instantiated.
     */
    public List<ImageWrapper> getImagesKey(Client client, String key)
    throws ServiceException, AccessException, ExecutionException {
        Collection<ImageData> images = new ArrayList<>(0);
        try {
            images = client.getBrowseFacility()
                           .getImagesForDatasets(client.getCtx(),
                                                 Collections.singletonList(data.getId()));
        } catch (DSOutOfServiceException | DSAccessException e) {
            handleServiceOrAccess(e, "Cannot get images with key \"" + key + "\" from " + this);
        }

        List<ImageWrapper> selected = new ArrayList<>();
        for (ImageData image : images) {
            ImageWrapper imageWrapper = new ImageWrapper(image);

            Map<String, String> pairsKeyValue = imageWrapper.getKeyValuePairs(client);
            if (pairsKeyValue.get(key) != null) {
                selected.add(imageWrapper);
            }
        }
        selected.sort(Comparator.comparing(GenericObjectWrapper::getId));

        return selected;
    }


    /**
     * Gets all images in the dataset with a certain key value pair from OMERO
     *
     * @param client The client handling the connection.
     * @param key    Name of the key researched.
     * @param value  Value associated with the key.
     *
     * @return ImageWrapper list.
     *
     * @throws ServiceException   Cannot connect to OMERO.
     * @throws AccessException    Cannot access data.
     * @throws ExecutionException A Facility can't be retrieved or instantiated.
     */
    public List<ImageWrapper> getImagesPairKeyValue(Client client, String key, String value)
    throws ServiceException, AccessException, ExecutionException {
        Collection<ImageData> images = new ArrayList<>(0);
        try {
            images = client.getBrowseFacility()
                           .getImagesForDatasets(client.getCtx(),
                                                 Collections.singletonList(data.getId()));
        } catch (DSOutOfServiceException | DSAccessException e) {
            handleServiceOrAccess(e, "Cannot get images with key-value pair from " + this);
        }

        List<ImageWrapper> selected = new ArrayList<>();
        for (ImageData image : images) {
            ImageWrapper imageWrapper = new ImageWrapper(image);

            Map<String, String> pairsKeyValue = imageWrapper.getKeyValuePairs(client);
            if (pairsKeyValue.get(key) != null && pairsKeyValue.get(key).equals(value)) {
                selected.add(imageWrapper);
            }
        }
        selected.sort(Comparator.comparing(GenericObjectWrapper::getId));

        return selected;
    }


    /**
     * Adds a list of image to the dataset in OMERO.
     *
     * @param client The client handling the connection.
     * @param images Image to add to the dataset.
     *
     * @throws ServiceException   Cannot connect to OMERO.
     * @throws AccessException    Cannot access data.
     * @throws ExecutionException A Facility can't be retrieved or instantiated.
     */
    public void addImages(Client client, List<? extends ImageWrapper> images)
    throws ServiceException, AccessException, ExecutionException {
        for (ImageWrapper image : images) {
            addImage(client, image);
        }
    }


    /**
     * Adds a single image to the dataset in OMERO
     *
     * @param client The client handling the connection.
     * @param image  Image to add.
     *
     * @throws ServiceException   Cannot connect to OMERO.
     * @throws AccessException    Cannot access data.
     * @throws ExecutionException A Facility can't be retrieved or instantiated.
     */
    public void addImage(Client client, ImageWrapper image)
    throws ServiceException, AccessException, ExecutionException {
        DatasetImageLink link = new DatasetImageLinkI();
        link.setChild(image.asImageData().asImage());
        link.setParent(new DatasetI(data.getId(), false));

        client.save(link);
        refresh(client);
    }


    /**
     * Removes an image from the dataset in OMERO.
     *
     * @param client The client handling the connection.
     * @param image  Image to remove.
     *
     * @throws ServiceException     Cannot connect to OMERO.
     * @throws AccessException      Cannot access data.
     * @throws ExecutionException   A Facility can't be retrieved or instantiated.
     * @throws OMEROServerError     If the thread was interrupted.
     * @throws InterruptedException If block(long) does not return.
     */
    public void removeImage(Client client, ImageWrapper image)
    throws ServiceException, AccessException, ExecutionException, OMEROServerError, InterruptedException {
        removeLink(client, "DatasetImageLink", image.getId());
    }


    /**
     * Imports all images candidates in the paths to the dataset in OMERO.
     *
     * @param client The client handling the connection.
     * @param paths  Paths to the image files on the computer.
     *
     * @return If the import did not exit because of an error.
     *
     * @throws ServiceException   Cannot connect to OMERO.
     * @throws AccessException    Cannot access data.
     * @throws OMEROServerError   Server error.
     * @throws IOException        Cannot read file.
     * @throws ExecutionException A Facility can't be retrieved or instantiated.
     */
    public boolean importImages(Client client, String... paths)
    throws ServiceException, OMEROServerError, AccessException, IOException, ExecutionException {
        boolean success;

        ImportConfig config = new ImportConfig();
        config.target.set("Dataset:" + data.getId());
        config.username.set(client.getUser().getUserName());
        config.email.set(client.getUser().getEmail());

        OMEROMetadataStoreClient store = client.getImportStore();
        try (OMEROWrapper reader = new OMEROWrapper(config)) {
            store.logVersionInfo(config.getIniVersionNumber());
            reader.setMetadataOptions(new DefaultMetadataOptions(MetadataLevel.ALL));

            ImportLibrary library = new ImportLibrary(store, reader);
            library.addObserver(new LoggingImportMonitor());

            ErrorHandler handler = new ErrorHandler(config);

            ImportCandidates candidates = new ImportCandidates(reader, paths, handler);
            success = library.importCandidates(config, candidates);
        } catch (ServerError se) {
            throw new OMEROServerError(se);
        } finally {
            store.logout();
        }

        refresh(client);
        return success;
    }


    /**
     * Imports one image file to the dataset in OMERO.
     *
     * @param client The client handling the connection.
     * @param path   Path to the image file on the computer.
     *
     * @return The list of IDs of the newly imported images.
     *
     * @throws ServiceException   Cannot connect to OMERO.
     * @throws AccessException    Cannot access data.
     * @throws OMEROServerError   Server error.
     * @throws ExecutionException A Facility can't be retrieved or instantiated.
     */
    public List<Long> importImage(Client client, String path)
    throws ServiceException, AccessException, OMEROServerError, ExecutionException {
        ImportConfig config = new ImportConfig();
        config.target.set("Dataset:" + data.getId());
        config.username.set(client.getUser().getUserName());
        config.email.set(client.getUser().getEmail());

        Collection<Pixels> pixels = new ArrayList<>(1);

        OMEROMetadataStoreClient store = client.getImportStore();
        try (OMEROWrapper reader = new OMEROWrapper(config)) {
            store.logVersionInfo(config.getIniVersionNumber());
            reader.setMetadataOptions(new DefaultMetadataOptions(MetadataLevel.ALL));

            ImportLibrary library = new ImportLibrary(store, reader);
            library.addObserver(new LoggingImportMonitor());

            ErrorHandler handler = new ErrorHandler(config);

            ImportCandidates candidates = new ImportCandidates(reader, new String[]{path}, handler);

            ExecutorService uploadThreadPool = Executors.newFixedThreadPool(config.parallelUpload.get());

            List<ImportContainer> containers = candidates.getContainers();
            if (containers != null) {
                for (int i = 0; i < containers.size(); i++) {
                    ImportContainer container = containers.get(i);
                    container.setTarget(data.asDataset());
                    List<Pixels> imported = library.importImage(container, uploadThreadPool, i);
                    pixels.addAll(imported);
                }
            }
            uploadThreadPool.shutdown();
        } catch (Throwable e) {
            throw new OMEROServerError(e);
        } finally {
            store.logout();
        }
        refresh(client);

        List<Long> ids = new ArrayList<>(pixels.size());
        pixels.forEach(pix -> ids.add(pix.getImage().getId().getValue()));
        return ids.stream().distinct().collect(Collectors.toList());
    }


    /**
     * Imports one image file to the dataset in OMERO and replace older images sharing the same name after copying their
     * annotations and ROIs, and concatenating the descriptions (on new lines).
     *
     * @param client The client handling the connection.
     * @param path   Path to the image on the computer.
     *
     * @return The list of IDs of the newly imported images.
     *
     * @throws ServiceException     Cannot connect to OMERO.
     * @throws AccessException      Cannot access data.
     * @throws OMEROServerError     Server error.
     * @throws ExecutionException   A Facility can't be retrieved or instantiated.
     * @throws InterruptedException If block(long) does not return.
     */
    public List<Long> importAndReplaceImages(Client client, String path)
    throws ServiceException, AccessException, OMEROServerError, ExecutionException, InterruptedException {
        List<Long> ids    = importImage(client, path);
        Long[]     newIds = ids.toArray(LONGS);

        List<ImageWrapper> newImages = client.getImages(newIds);
        for (ImageWrapper image : newImages) {
            List<ImageWrapper> oldImages = getImages(client, image.getName());
            oldImages.removeIf(i -> ids.contains(i.getId()));
            ArrayList<String> descriptions = new ArrayList<>(oldImages.size() + 1);
            descriptions.add(image.getDescription());
            for (ImageWrapper oldImage : oldImages) {
                descriptions.add(oldImage.getDescription());
                image.copyAnnotationLinks(client, oldImage);
                List<ROIWrapper> rois = oldImage.getROIs(client);
                for (ROIWrapper roi : rois) {
                    roi.setImage(image);
                    image.saveROI(client, roi);
                }
                client.delete(oldImage);
            }
            descriptions.removeIf(s -> s == null || s.trim().isEmpty());
            image.setDescription(String.join("\n", descriptions));
            image.saveAndUpdate(client);
        }
        return ids;
    }


    /**
     * Refreshes the wrapped dataset.
     *
     * @param client The client handling the connection.
     *
     * @throws ServiceException   Cannot connect to OMERO.
     * @throws AccessException    Cannot access data.
     * @throws ExecutionException A Facility can't be retrieved or instantiated.
     */
    public void refresh(Client client) throws ServiceException, AccessException, ExecutionException {
        try {
            data = client.getBrowseFacility()
                         .getDatasets(client.getCtx(), Collections.singletonList(this.getId()))
                         .iterator().next();
        } catch (DSOutOfServiceException | DSAccessException e) {
            handleServiceOrAccess(e, "Cannot refresh " + this);
        }
    }

}