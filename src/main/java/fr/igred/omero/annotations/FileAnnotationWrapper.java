package fr.igred.omero.annotations;


import fr.igred.omero.Client;
import fr.igred.omero.exception.ServerException;
import fr.igred.omero.exception.ServiceException;
import omero.ServerError;
import omero.api.RawFileStorePrx;
import omero.gateway.exception.DSOutOfServiceException;
import omero.gateway.model.FileAnnotationData;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;


public class FileAnnotationWrapper extends AnnotationWrapper<FileAnnotationData> {

    /**
     * Constructor of the AnnotationWrapper class.
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


    public File getFile(Client client, String path) throws IOException, ServiceException, ServerException {
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
        } catch (DSOutOfServiceException se) {
            throw new ServiceException("Could not create RawFileService", se, se.getConnectionStatus());
        } catch (ServerError e) {
            throw new ServerException("Could not create RawFileService", e);
        }
        try {
            store.close();
        } catch (ServerError e) {
            throw new ServerException("Could not close RawFileService", e);
        }

        return file;
    }


    public String getContentAsString() {
        return data.getContentAsString();
    }


    public boolean isMovieFile() {
        return data.isMovieFile();
    }

}
