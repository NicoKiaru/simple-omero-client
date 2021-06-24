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
import fr.igred.omero.annotations.TagAnnotationWrapper;
import fr.igred.omero.exception.AccessException;
import fr.igred.omero.exception.OMEROServerError;
import fr.igred.omero.exception.ServiceException;
import omero.gateway.exception.DSAccessException;
import omero.gateway.exception.DSOutOfServiceException;
import omero.gateway.model.DatasetData;
import omero.gateway.model.ProjectData;
import omero.gateway.util.PojoMapper;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import static fr.igred.omero.exception.ExceptionHandler.handleServiceOrAccess;


/**
 * Class containing a ProjectData
 * <p> Implements function using the Project contained
 */
public class ProjectWrapper extends GenericRepositoryObjectWrapper<ProjectData> {

    /**
     * Constructor of the ProjectWrapper class.
     *
     * @param client  The client handling the connection.
     * @param project ProjectData to be contained.
     */
    public ProjectWrapper(Client client, ProjectData project) {
        super(client, project);
    }


    /**
     * Constructor of the ProjectWrapper class. Creates a new project and save it to OMERO.
     *
     * @param client      The client handling the connection.
     * @param name        Project name.
     * @param description Project description.
     *
     * @throws ServiceException   Cannot connect to OMERO.
     * @throws AccessException    Cannot access data.
     * @throws ExecutionException A Facility can't be retrieved or instantiated.
     */
    public ProjectWrapper(Client client, String name, String description)
    throws ServiceException, AccessException, ExecutionException {
        super(client, new ProjectData());
        data.setName(name);
        data.setDescription(description);
        saveAndUpdate();
    }


    /**
     * Gets the ProjectData name
     *
     * @return ProjectData name.
     */
    public String getName() {
        return data.getName();
    }


    /**
     * Sets the name of the project.
     *
     * @param name The name of the project. Mustn't be <code>null</code>.
     *
     * @throws IllegalArgumentException If the name is <code>null</code>.
     */
    public void setName(String name) {
        data.setName(name);
    }


    /**
     * Gets the project description
     *
     * @return The project description.
     */
    public String getDescription() {
        return data.getDescription();
    }


    /**
     * Sets the description of the project.
     *
     * @param description The description of the project.
     */
    public void setDescription(String description) {
        data.setDescription(description);
    }


    /**
     * @return the ProjectData contained.
     */
    public ProjectData asProjectData() {
        return data;
    }


    /**
     * Gets all the datasets in the project available from OMERO.
     *
     * @return Collection of DatasetWrapper.
     */
    public List<DatasetWrapper> getDatasets() {
        Set<DatasetData> datasets = data.getDatasets();

        List<DatasetWrapper> wrappers = new ArrayList<>(datasets.size());
        for (DatasetData dataset : datasets) {
            wrappers.add(new DatasetWrapper(client, dataset));
        }
        wrappers.sort(new SortById<>());

        return wrappers;
    }


    /**
     * Gets the dataset with the specified name from OMERO
     *
     * @param name Name of the dataset searched.
     *
     * @return List of dataset with the given name.
     */
    public List<DatasetWrapper> getDatasets(String name) {
        List<DatasetWrapper> datasets = getDatasets();
        datasets.removeIf(dataset -> !dataset.getName().equals(name));
        return datasets;
    }


    /**
     * Add a dataset to the project in OMERO. Create the dataset.
     *
     * @param name        Dataset name.
     * @param description Dataset description.
     *
     * @return The object saved in OMERO.
     *
     * @throws ServiceException   Cannot connect to OMERO.
     * @throws AccessException    Cannot access data.
     * @throws ExecutionException A Facility can't be retrieved or instantiated.
     */
    public DatasetWrapper addDataset(String name, String description)
    throws ServiceException, AccessException, ExecutionException {
        DatasetData datasetData = new DatasetData();
        datasetData.setName(name);
        datasetData.setDescription(description);
        return addDataset(datasetData);
    }


    /**
     * Add a dataset to the project in OMERO.
     *
     * @param dataset Dataset to be added.
     *
     * @return The object saved in OMERO.
     *
     * @throws ServiceException   Cannot connect to OMERO.
     * @throws AccessException    Cannot access data.
     * @throws ExecutionException A Facility can't be retrieved or instantiated.
     */
    public DatasetWrapper addDataset(DatasetWrapper dataset)
    throws ServiceException, AccessException, ExecutionException {
        return addDataset(dataset.asDatasetData());
    }


    /**
     * Private function. Add a dataset to the project.
     *
     * @param datasetData Dataset to be added.
     *
     * @return The object saved in OMERO.
     *
     * @throws ServiceException   Cannot connect to OMERO.
     * @throws AccessException    Cannot access data.
     * @throws ExecutionException A Facility can't be retrieved or instantiated.
     */
    private DatasetWrapper addDataset(DatasetData datasetData)
    throws ServiceException, AccessException, ExecutionException {
        DatasetWrapper newDataset;
        datasetData.setProjects(Collections.singleton(data));
        DatasetData dataset = (DatasetData) PojoMapper.asDataObject(client.save(datasetData.asIObject()));
        refresh();
        newDataset = new DatasetWrapper(client, dataset);
        return newDataset;
    }


    /**
     * Gets all images in the dataset available from OMERO.
     *
     * @return ImageWrapper list.
     */
    private List<ImageWrapper> purge(List<ImageWrapper> images) {
        List<ImageWrapper> purged = new ArrayList<>();

        for (ImageWrapper image : images) {
            if (purged.isEmpty() || purged.get(purged.size() - 1).getId() != image.getId()) {
                purged.add(image);
            }
        }

        return purged;
    }


    /**
     * Gets all images in the project available from OMERO.
     *
     * @return ImageWrapper list.
     *
     * @throws ServiceException Cannot connect to OMERO.
     * @throws AccessException  Cannot access data.
     */
    public List<ImageWrapper> getImages() throws ServiceException, AccessException {
        Collection<DatasetWrapper> datasets = getDatasets();

        List<ImageWrapper> images = new ArrayList<>();
        for (DatasetWrapper dataset : datasets) {
            images.addAll(dataset.getImages());
        }
        images.sort(new SortById<>());

        return purge(images);
    }


    /**
     * Gets all images in the project with a certain from OMERO.
     *
     * @param name   Name searched.
     *
     * @return ImageWrapper list.
     *
     * @throws ServiceException Cannot connect to OMERO.
     * @throws AccessException  Cannot access data.
     */
    public List<ImageWrapper> getImages(String name)
    throws ServiceException, AccessException {
        Collection<DatasetWrapper> datasets = getDatasets();

        List<ImageWrapper> imageWrappers = new ArrayList<>();
        for (DatasetWrapper dataset : datasets) {
            imageWrappers.addAll(dataset.getImages(name));
        }
        imageWrappers.sort(new SortById<>());

        return purge(imageWrappers);
    }


    /**
     * Gets all images in the project with a certain motif in their name from OMERO.
     *
     * @param motif Motif searched in an image name.
     *
     * @return ImageWrapper list.
     *
     * @throws ServiceException Cannot connect to OMERO.
     * @throws AccessException  Cannot access data.
     */
    public List<ImageWrapper> getImagesLike(String motif)
    throws ServiceException, AccessException {
        List<ImageWrapper> images = new ArrayList<>();
        for (DatasetWrapper dataset : getDatasets()) {
            images.addAll(dataset.getImagesLike(motif));
        }
        images.sort(new SortById<>());

        return purge(images);
    }


    /**
     * Gets all images in the project tagged with a specified tag from OMERO.
     *
     * @param tag TagAnnotationWrapper containing the tag researched.
     *
     * @return ImageWrapper list.
     *
     * @throws ServiceException Cannot connect to OMERO.
     * @throws AccessException  Cannot access data.
     * @throws OMEROServerError Server error.
     */
    public List<ImageWrapper> getImagesTagged(TagAnnotationWrapper tag)
    throws ServiceException, AccessException, OMEROServerError {
        List<ImageWrapper> images = new ArrayList<>();
        for (DatasetWrapper dataset : getDatasets()) {
            images.addAll(dataset.getImagesTagged(tag));
        }
        images.sort(new SortById<>());

        return purge(images);
    }


    /**
     * Gets all images in the project tagged with a specified tag from OMERO.
     *
     * @param tagId Id of the tag researched.
     *
     * @return ImageWrapper list.
     *
     * @throws ServiceException Cannot connect to OMERO.
     * @throws AccessException  Cannot access data.
     * @throws OMEROServerError Server error.
     */
    public List<ImageWrapper> getImagesTagged(Long tagId)
    throws ServiceException, AccessException, OMEROServerError {
        List<ImageWrapper> images = new ArrayList<>();
        for (DatasetWrapper dataset : getDatasets()) {
            images.addAll(dataset.getImagesTagged(tagId));
        }
        images.sort(new SortById<>());

        return purge(images);
    }


    /**
     * Gets all images in the project with a certain key
     *
     * @param key Name of the key researched.
     *
     * @return ImageWrapper list.
     *
     * @throws ServiceException   Cannot connect to OMERO.
     * @throws AccessException    Cannot access data.
     * @throws ExecutionException A Facility can't be retrieved or instantiated.
     */
    public List<ImageWrapper> getImagesKey(String key)
    throws ServiceException, AccessException, ExecutionException {
        List<ImageWrapper> images = new ArrayList<>();
        for (DatasetWrapper dataset : getDatasets()) {
            images.addAll(dataset.getImagesKey(key));
        }
        images.sort(new SortById<>());

        return purge(images);
    }


    /**
     * Gets all images in the project with a certain key value pair from OMERO.
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
        List<ImageWrapper> images = new ArrayList<>();
        for (DatasetWrapper dataset : getDatasets()) {
            images.addAll(dataset.getImagesPairKeyValue(key, value));
        }
        images.sort(new SortById<>());

        return purge(images);
    }


    /**
     * Refreshes the wrapped project.
     *
     * @throws ServiceException Cannot connect to OMERO.
     * @throws AccessException  Cannot access data.
     */
    public void refresh() throws ServiceException, AccessException {
        try {
            data = client.getBrowseFacility()
                         .getProjects(client.getCtx(), Collections.singletonList(this.getId()))
                         .iterator().next();
        } catch (DSOutOfServiceException | DSAccessException e) {
            handleServiceOrAccess(e, "Cannot refresh " + toString());
        }
    }

}