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
package fr.igred.omero.annotations;


import fr.igred.omero.Client;
import fr.igred.omero.exception.OMEROServerError;
import fr.igred.omero.exception.ServiceException;
import omero.ServerError;
import omero.api.RawFileStorePrx;
import omero.gateway.exception.DSOutOfServiceException;
import omero.gateway.model.FileAnnotationData;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import static fr.igred.omero.exception.ExceptionHandler.handleServiceOrServer;


public class FileAnnotationWrapper extends GenericAnnotationWrapper<FileAnnotationData> {

    /**
     * Constructor of the GenericAnnotationWrapper class.
     *
     * @param annotation Annotation to be contained.
     */
    public FileAnnotationWrapper(FileAnnotationData annotation) {
        super(annotation);
    }


    public String getOriginalMimetype() {
        return data.getOriginalMimetype();
    }


    public String getServerFileMimetype() {
        return data.getServerFileMimetype();
    }


    public String getFileFormat() {
        return data.getFileFormat();
    }


    public String getFileKind() {
        return data.getFileKind();
    }


    public File getAttachedFile() {
        return data.getAttachedFile();
    }


    public String getFileName() {
        return data.getFileName();
    }


    public String getFilePath() {
        return data.getFilePath();
    }


    public long getFileSize() {
        return data.getFileSize();
    }


    public long getFileID() {
        return data.getFileID();
    }


    public File getFile(Client client, String path) throws IOException, ServiceException, OMEROServerError {
        final int inc = 262144;

        File file = new File(path);

        RawFileStorePrx store = null;
        try (FileOutputStream stream = new FileOutputStream(file)) {
            store = client.getGateway().getRawFileService(client.getCtx());
            store.setFileId(this.getFileID());

            long size = getFileSize();
            long offset;
            for (offset = 0; offset + inc < size; offset += inc) {
                stream.write(store.read(offset, inc));
            }
            stream.write(store.read(offset, (int) (size - offset)));
        } catch (DSOutOfServiceException | ServerError e) {
            handleServiceOrServer(e, "Could not create RawFileService");
        }

        if (store != null) {
            try {
                store.close();
            } catch (ServerError e) {
                throw new OMEROServerError("Could not close RawFileService", e);
            }
        }

        return file;
    }


    public String getContentAsString() {
        return data.getContentAsString();
    }


    public boolean isMovieFile() {
        return data.isMovieFile();
    }


    /**
     * Gets the FileAnnotationData contained.
     *
     * @return the {@link FileAnnotationData} contained.
     */
    public FileAnnotationData asFileAnnotationData() {
        return data;
    }

}
