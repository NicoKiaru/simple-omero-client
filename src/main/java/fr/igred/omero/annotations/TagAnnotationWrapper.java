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

package fr.igred.omero.annotations;


import fr.igred.omero.Client;
import fr.igred.omero.exception.AccessException;
import fr.igred.omero.exception.OMEROServerError;
import fr.igred.omero.exception.ServiceException;
import fr.igred.omero.repository.DatasetWrapper;
import fr.igred.omero.repository.ImageWrapper;
import fr.igred.omero.repository.ProjectWrapper;
import omero.gateway.model.TagAnnotationData;
import omero.gateway.util.PojoMapper;
import omero.model.DatasetAnnotationLink;
import omero.model.IObject;
import omero.model.ImageAnnotationLink;
import omero.model.ProjectAnnotationLink;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;


/**
 * Class containing a TagAnnotationData
 * <p> Implements function using the TagAnnotationData contained.
 */
public class TagAnnotationWrapper extends GenericAnnotationWrapper<TagAnnotationData> {

    /**
     * Constructor of the TagAnnotationWrapper class.
     *
     * @param client The client handling the connection.
     * @param tag    Tag to be contained.
     */
    public TagAnnotationWrapper(Client client, TagAnnotationData tag) {
        super(client, tag);
        setNameSpace(tag.getContentAsString());
    }


    /**
     * Constructor of the TagAnnotationWrapper class. Creates the tag and save it in OMERO.
     *
     * @param client      The client handling the connection.
     * @param name        Annotation name.
     * @param description Tag description.
     *
     * @throws ServiceException   Cannot connect to OMERO.
     * @throws AccessException    Cannot access data.
     * @throws ExecutionException A Facility can't be retrieved or instantiated.
     */
    public TagAnnotationWrapper(Client client, String name, String description)
    throws ServiceException, AccessException, ExecutionException {
        super(client, (TagAnnotationData) PojoMapper
                .asDataObject(client.save(new TagAnnotationData(name, description).asIObject())));
    }


    /**
     * Sets the name of the TagData.
     *
     * @param name The name of the TagData. Mustn't be <code>null</code>.
     *
     * @throws IllegalArgumentException If the name is <code>null</code>.
     */
    public void setName(String name) {
        data.setTagValue(name);
    }


    /**
     * Gets the name of the TagData.
     *
     * @return TagData name.
     */
    public String getName() {
        return data.getTagValue();
    }


    /**
     * Gets the TagAnnotationData contained.
     *
     * @return the {@link TagAnnotationData} contained.
     */
    public TagAnnotationData asTagAnnotationData() {
        return data;
    }


    /**
     * Gets all projects with this tag from OMERO.
     *
     * @return ProjectWrapper list.
     *
     * @throws ServiceException Cannot connect to OMERO.
     * @throws AccessException  Cannot access data.
     * @throws OMEROServerError Server error.
     */
    public List<ProjectWrapper> getProjects()
    throws ServiceException, AccessException, OMEROServerError {
        List<IObject> os = getLinks(client, ProjectAnnotationLink.class.getSimpleName());

        List<ProjectWrapper> selected = new ArrayList<>(os.size());
        for (IObject o : os) {
            selected.add(client.getProject(o.getId().getValue()));
        }

        return selected;
    }


    /**
     * Gets all datasets with this tag from OMERO.
     *
     * @return DatasetWrapper list.
     *
     * @throws ServiceException Cannot connect to OMERO.
     * @throws AccessException  Cannot access data.
     * @throws OMEROServerError Server error.
     */
    public List<DatasetWrapper> getDatasets()
    throws ServiceException, AccessException, OMEROServerError {
        List<IObject> os = getLinks(client, DatasetAnnotationLink.class.getSimpleName());

        List<DatasetWrapper> selected = new ArrayList<>(os.size());
        for (IObject o : os) {
            selected.add(client.getDataset(o.getId().getValue()));
        }

        return selected;
    }


    /**
     * Gets all images with this tag from OMERO.
     *
     * @return ImageWrapper list.
     *
     * @throws ServiceException Cannot connect to OMERO.
     * @throws AccessException  Cannot access data.
     * @throws OMEROServerError Server error.
     */
    public List<ImageWrapper> getImages()
    throws ServiceException, AccessException, OMEROServerError {
        List<IObject> os = getLinks(client, ImageAnnotationLink.class.getSimpleName());

        List<ImageWrapper> selected = new ArrayList<>(os.size());
        for (IObject o : os) {
            selected.add(client.getImage(o.getId().getValue()));
        }

        return selected;
    }


    /**
     * Retrieves all links of the given type.
     *
     * @param client   The client handling the connection.
     * @param linkType The link type.
     *
     * @return The list of linked objects.
     *
     * @throws ServiceException Cannot connect to OMERO.
     * @throws OMEROServerError Server error.
     */
    private List<IObject> getLinks(Client client, String linkType)
    throws ServiceException, OMEROServerError {
        return client.findByQuery("select link.parent from " + linkType +
                                  " link where link.child = " + getId());
    }

}