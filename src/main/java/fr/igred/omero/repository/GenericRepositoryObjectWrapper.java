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
import fr.igred.omero.annotations.TableWrapper;
import fr.igred.omero.annotations.TagAnnotationWrapper;
import fr.igred.omero.exception.AccessException;
import fr.igred.omero.exception.ServiceException;
import omero.gateway.exception.DSAccessException;
import omero.gateway.exception.DSOutOfServiceException;
import omero.gateway.model.*;
import omero.model.NamedValue;
import omero.model.TagAnnotationI;

import java.io.File;
import java.util.*;
import java.util.concurrent.ExecutionException;

import static fr.igred.omero.exception.ExceptionHandler.handleServiceOrAccess;


/**
 * Generic class containing a DataObject (or a subclass) object.
 *
 * @param <T> Subclass of {@link DataObject}
 */
public abstract class GenericRepositoryObjectWrapper<T extends DataObject> extends GenericObjectWrapper<T> {


    /**
     * Constructor of the class GenericRepositoryObjectWrapper.
     *
     * @param client The client handling the connection.
     * @param object The object contained in the GenericRepositoryObjectWrapper.
     */
    protected GenericRepositoryObjectWrapper(Client client, T object) {
        super(client, object);
    }


    /**
     * Adds a newly created tag to the object in OMERO, if possible.
     *
     * @param name        Tag Name.
     * @param description Tag description.
     *
     * @throws ServiceException   Cannot connect to OMERO.
     * @throws AccessException    Cannot access data.
     * @throws ExecutionException A Facility can't be retrieved or instantiated.
     */
    public void addTag(String name, String description)
    throws ServiceException, AccessException, ExecutionException {
        TagAnnotationData tagData = new TagAnnotationData(name);
        tagData.setTagDescription(description);

        addTag(tagData);
    }


    /**
     * Adds a tag to the object in OMERO, if possible.
     *
     * @param tag Tag to be added.
     *
     * @throws ServiceException   Cannot connect to OMERO.
     * @throws AccessException    Cannot access data.
     * @throws ExecutionException A Facility can't be retrieved or instantiated.
     */
    public void addTag(TagAnnotationWrapper tag)
    throws ServiceException, AccessException, ExecutionException {
        addTag(tag.asTagAnnotationData());
    }


    /**
     * Private function. Adds a tag to the object in OMERO, if possible.
     *
     * @param tagData Tag to be added.
     *
     * @throws ServiceException   Cannot connect to OMERO.
     * @throws AccessException    Cannot access data.
     * @throws ExecutionException A Facility can't be retrieved or instantiated.
     */
    protected void addTag(TagAnnotationData tagData)
    throws ServiceException, AccessException, ExecutionException {
        try {
            client.getDm().attachAnnotation(client.getCtx(), tagData, data);
        } catch (DSOutOfServiceException | DSAccessException e) {
            handleServiceOrAccess(e, "Cannot add tag " + tagData.getTagValue() + " to " + toString());
        }
    }


    /**
     * Adds multiple tags to the object in OMERO, if possible.
     *
     * @param id Id in OMERO of tag to add.
     *
     * @throws ServiceException   Cannot connect to OMERO.
     * @throws AccessException    Cannot access data.
     * @throws ExecutionException A Facility can't be retrieved or instantiated.
     */
    public void addTag(Long id)
    throws ServiceException, AccessException, ExecutionException {
        TagAnnotationI    tag     = new TagAnnotationI(id, false);
        TagAnnotationData tagData = new TagAnnotationData(tag);
        addTag(tagData);
    }


    /**
     * Adds multiple tag to the object in OMERO, if possible.
     *
     * @param tags Array of TagAnnotationWrapper to add.
     *
     * @throws ServiceException   Cannot connect to OMERO.
     * @throws AccessException    Cannot access data.
     * @throws ExecutionException A Facility can't be retrieved or instantiated.
     */
    public void addTags(TagAnnotationWrapper... tags)
    throws ServiceException, AccessException, ExecutionException {
        for (TagAnnotationWrapper tag : tags) {
            addTag(tag.asTagAnnotationData());
        }
    }


    /**
     * Adds multiple tags by ID to the object in OMERO, if possible.
     *
     * @param ids Array of tag id in OMERO to add.
     *
     * @throws ServiceException   Cannot connect to OMERO.
     * @throws AccessException    Cannot access data.
     * @throws ExecutionException A Facility can't be retrieved or instantiated.
     */
    public void addTags(Long... ids)
    throws ServiceException, AccessException, ExecutionException {
        for (Long id : ids) {
            addTag(id);
        }
    }


    /**
     * Gets all tag linked to an object in OMERO, if possible.
     *
     * @return Collection of TagAnnotationWrapper each containing a tag linked to the object.
     *
     * @throws ServiceException   Cannot connect to OMERO.
     * @throws AccessException    Cannot access data.
     * @throws ExecutionException A Facility can't be retrieved or instantiated.
     */
    public List<TagAnnotationWrapper> getTags()
    throws ServiceException, AccessException, ExecutionException {
        List<Class<? extends AnnotationData>> types = new ArrayList<>();
        types.add(TagAnnotationData.class);

        List<AnnotationData> annotations = null;
        try {
            annotations = client.getMetadata().getAnnotations(client.getCtx(), data, types, null);
        } catch (DSOutOfServiceException | DSAccessException e) {
            handleServiceOrAccess(e, "Cannot get tags for " + toString());
        }

        List<TagAnnotationWrapper> tags = new ArrayList<>();
        if (annotations != null) {
            for (AnnotationData annotation : annotations) {
                TagAnnotationData tagAnnotation = (TagAnnotationData) annotation;

                tags.add(new TagAnnotationWrapper(client, tagAnnotation));
            }
        }

        tags.sort(new SortById<>());
        return tags;
    }


    /**
     * Adds a single Key-Value pair to the object.
     *
     * @param key   Name of the key.
     * @param value Value associated to the key.
     *
     * @throws ServiceException   Cannot connect to OMERO.
     * @throws AccessException    Cannot access data.
     * @throws ExecutionException A Facility can't be retrieved or instantiated.
     */
    public void addPairKeyValue(String key, String value)
    throws ServiceException, AccessException, ExecutionException {
        List<NamedValue> result = new ArrayList<>();
        result.add(new NamedValue(key, value));

        MapAnnotationData mapData = new MapAnnotationData();
        mapData.setContent(result);
        addMapAnnotation(new MapAnnotationWrapper(client, mapData));
    }


    /**
     * Gets the List of NamedValue (Key-Value pair) associated to an object.
     *
     * @return Collection of NamedValue.
     *
     * @throws ServiceException   Cannot connect to OMERO.
     * @throws AccessException    Cannot access data.
     * @throws ExecutionException A Facility can't be retrieved or instantiated.
     */
    public Map<String, String> getKeyValuePairs()
    throws ServiceException, AccessException, ExecutionException {
        Map<String, String> keyValuePairs = new HashMap<>();

        List<Long> userIds = new ArrayList<>();
        userIds.add(client.getId());

        List<Class<? extends AnnotationData>> types = new ArrayList<>();
        types.add(MapAnnotationData.class);

        List<AnnotationData> annotations = null;
        try {
            annotations = client.getMetadata().getAnnotations(client.getCtx(), data, types, userIds);
        } catch (DSOutOfServiceException | DSAccessException e) {
            handleServiceOrAccess(e, "Cannot get k/v pairs for " + toString());
        }

        if (annotations != null) {
            for (AnnotationData annotation : annotations) {
                MapAnnotationData mapAnnotation = (MapAnnotationData) annotation;

                @SuppressWarnings("unchecked")
                List<NamedValue> list = (List<NamedValue>) mapAnnotation.getContent();

                list.forEach(kv -> keyValuePairs.put(kv.name, kv.value));
            }
        }

        return keyValuePairs;
    }


    /**
     * Gets the value from a Key-Value pair associated to the object
     *
     * @param key Key researched.
     *
     * @return Value associated to the key.
     *
     * @throws ServiceException       Cannot connect to OMERO.
     * @throws AccessException        Cannot access data.
     * @throws NoSuchElementException Key not found.
     * @throws ExecutionException     A Facility can't be retrieved or instantiated.
     */
    public String getValue(String key)
    throws ServiceException, AccessException, NoSuchElementException, ExecutionException {
        Map<String, String> keyValuePairs = getKeyValuePairs();
        String value = keyValuePairs.get(key);
        if(value != null) {
            return value;
        }
        else {
            throw new NoSuchElementException("Key value pair " + key + " not found");
        }
    }


    /**
     * Adds a List of Key-Value pair to the object
     * <p>The list is contained in the MapAnnotationWrapper
     *
     * @param mapAnnotation MapAnnotationWrapper containing a list of NamedValue.
     *
     * @throws ServiceException   Cannot connect to OMERO.
     * @throws AccessException    Cannot access data.
     * @throws ExecutionException A Facility can't be retrieved or instantiated.
     */
    public void addMapAnnotation(MapAnnotationWrapper mapAnnotation)
    throws ServiceException, AccessException, ExecutionException {
        try {
            client.getDm().attachAnnotation(client.getCtx(),
                                            mapAnnotation.asMapAnnotationData(),
                                            this.data);
        } catch (DSOutOfServiceException | DSAccessException e) {
            handleServiceOrAccess(e, "Cannot add k/v pairs to " + toString());
        }
    }


    /**
     * Adds a table to the object in OMERO
     *
     * @param table Table to add to the object.
     *
     * @throws ServiceException   Cannot connect to OMERO.
     * @throws AccessException    Cannot access data.
     * @throws ExecutionException A Facility can't be retrieved or instantiated.
     */
    public void addTable(TableWrapper table)
    throws ServiceException, AccessException, ExecutionException {
        TableData tableData = table.createTable();
        try {
            tableData = client.getTablesFacility().addTable(client.getCtx(), data, table.getName(), tableData);
        } catch (DSOutOfServiceException | DSAccessException e) {
            handleServiceOrAccess(e, "Cannot add table to " + toString());
        }
        table.setFileId(tableData.getOriginalFileId());
    }


    /**
     * Gets a certain table linked to the object in OMERO
     *
     * @param fileId FileId of the table researched.
     *
     * @return TableWrapper containing the table information.
     *
     * @throws ServiceException   Cannot connect to OMERO.
     * @throws AccessException    Cannot access data.
     * @throws ExecutionException A Facility can't be retrieved or instantiated.
     */
    public TableWrapper getTable(Long fileId)
    throws ServiceException, AccessException, ExecutionException {
        TableData table = null;
        try {
            table = client.getTablesFacility().getTable(client.getCtx(), fileId);
        } catch (DSOutOfServiceException | DSAccessException e) {
            handleServiceOrAccess(e, "Cannot get table from " + toString());
        }
        return new TableWrapper(Objects.requireNonNull(table));
    }


    /**
     * Gets all table linked to the object in OMERO.
     *
     * @return List of TableWrapper containing the tables information.
     *
     * @throws ServiceException   Cannot connect to OMERO.
     * @throws AccessException    Cannot access data.
     * @throws ExecutionException A Facility can't be retrieved or instantiated.
     */
    public List<TableWrapper> getTables()
    throws ServiceException, AccessException, ExecutionException {
        Collection<FileAnnotationData> tables = new ArrayList<>();
        try {
            tables = client.getTablesFacility().getAvailableTables(client.getCtx(), data);
        } catch (DSOutOfServiceException | DSAccessException e) {
            handleServiceOrAccess(e, "Cannot get tables from " + toString());
        }

        List<TableWrapper> tablesWrapper = new ArrayList<>(tables.size());
        for (FileAnnotationData table : tables) {
            TableWrapper tableWrapper = getTable(table.getFileID());
            tableWrapper.setId(table.getId());
            tablesWrapper.add(tableWrapper);
        }

        return tablesWrapper;
    }


    /**
     * Links a file to the object
     *
     * @param file File to add.
     *
     * @return ID of the file created in OMERO.
     *
     * @throws ExecutionException   A Facility can't be retrieved or instantiated.
     * @throws InterruptedException The thread was interrupted.
     */
    public long addFile(File file) throws ExecutionException, InterruptedException {
        return client.getDm().attachFile(client.getCtx(),
                                         file,
                                         null,
                                         "",
                                         file.getName(),
                                         data).get().getId();
    }

}
