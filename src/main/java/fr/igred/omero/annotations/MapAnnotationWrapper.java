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
import omero.gateway.model.MapAnnotationData;
import omero.model.NamedValue;

import java.util.List;


/**
 * Class containing a MapAnnotationData, a MapAnnotationData contains a list of NamedValue(Key-Value pair).
 * <p> Implements function using the MapAnnotationData contained
 */
public class MapAnnotationWrapper extends GenericAnnotationWrapper<MapAnnotationData> {


    /**
     * Constructor of the MapAnnotationWrapper class.
     *
     * @param client The client handling the connection.
     * @param data   MapAnnotationData to be contained.
     */
    public MapAnnotationWrapper(Client client, MapAnnotationData data) {
        super(client, data);
    }


    /**
     * Constructor of the MapAnnotationWrapper class. Sets the content of the MapAnnotationData
     *
     * @param client The client handling the connection.
     * @param result List of NamedValue(Key-Value pair).
     */
    public MapAnnotationWrapper(Client client, List<NamedValue> result) {
        super(client, new MapAnnotationData());
        data.setContent(result);
    }


    /**
     * Constructor of the MapAnnotationWrapper class.
     *
     * @param client The client handling the connection.
     */
    public MapAnnotationWrapper(Client client) {
        super(client, new MapAnnotationData());
    }


    /**
     * Gets the List of NamedValue contained in the MapAnnotationData.
     *
     * @return MapAnnotationData content.
     */
    @SuppressWarnings("unchecked")
    public List<NamedValue> getContent() {
        return (List<NamedValue>) data.getContent();
    }


    /**
     * Sets the content of the MapAnnotationData.
     *
     * @param result List of NamedValue(Key-Value pair).
     */
    public void setContent(List<NamedValue> result) {
        data = new MapAnnotationData();
        data.setContent(result);
    }


    /**
     * Gets the MapAnnotationData contained.
     *
     * @return the {@link MapAnnotationData} contained.
     */
    public MapAnnotationData asMapAnnotationData() {
        return data;
    }

}