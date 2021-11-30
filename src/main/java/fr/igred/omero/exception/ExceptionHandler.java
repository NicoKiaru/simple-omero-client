package fr.igred.omero.exception;


import omero.ServerError;
import omero.gateway.exception.DSAccessException;
import omero.gateway.exception.DSOutOfServiceException;

import java.util.Objects;


public class ExceptionHandler {


    private ExceptionHandler() {
    }


    public static <T, U> U handleAll(T value, AllThrower<? super T, ? extends U> mapper, String error)
    throws ServiceException, AccessException, OMEROServerError, InterruptedException {
        Objects.requireNonNull(mapper);
        Objects.requireNonNull(value);
        final U u;
        try {
            u = mapper.apply(value);
        } catch (InterruptedException ie) {
            throw ie;
        } catch (DSOutOfServiceException se) {
            throw new ServiceException(error, se, se.getConnectionStatus());
        } catch (DSAccessException ae) {
            throw new AccessException(error, ae);
        } catch (ServerError se) {
            throw new OMEROServerError(error, se);
        }
        return u;
    }


    public static <T, U> U handle(T value, MainThrower<? super T, ? extends U> mapper, String error)
    throws ServiceException, AccessException, OMEROServerError {
        Objects.requireNonNull(mapper);
        Objects.requireNonNull(value);
        final U u;
        try {
            u = mapper.apply(value);
        } catch (DSOutOfServiceException se) {
            throw new ServiceException(error, se, se.getConnectionStatus());
        } catch (DSAccessException ae) {
            throw new AccessException(error, ae);
        } catch (ServerError se) {
            throw new OMEROServerError(error, se);
        }
        return u;
    }


    public static <T, U> U handleServiceAndAccess(T value, ServiceOrAccessThrower<? super T, ? extends U> mapper,
                                                  String error)
    throws ServiceException, AccessException {
        Objects.requireNonNull(mapper);
        Objects.requireNonNull(value);
        final U u;
        try {
            u = mapper.apply(value);
        } catch (DSOutOfServiceException se) {
            throw new ServiceException(error, se, se.getConnectionStatus());
        } catch (DSAccessException ae) {
            throw new AccessException(error, ae);
        }
        return u;
    }


    public static <T, U> U handleServiceAndServer(T value, ServiceOrServerThrower<? super T, ? extends U> mapper,
                                                  String error)
    throws ServiceException, OMEROServerError {
        Objects.requireNonNull(mapper);
        Objects.requireNonNull(value);
        final U u;
        try {
            u = mapper.apply(value);
        } catch (DSOutOfServiceException se) {
            throw new ServiceException(error, se, se.getConnectionStatus());
        } catch (ServerError se) {
            throw new OMEROServerError(error, se);
        }
        return u;
    }


    @FunctionalInterface
    public interface AllThrower<T, R> {

        R apply(T t) throws DSOutOfServiceException, DSAccessException, ServerError, InterruptedException;

    }


    @FunctionalInterface
    public interface MainThrower<T, R> {

        R apply(T t) throws DSOutOfServiceException, DSAccessException, ServerError;

    }


    @FunctionalInterface
    public interface ServiceOrAccessThrower<T, R> {

        R apply(T t) throws DSOutOfServiceException, DSAccessException;

    }


    @FunctionalInterface
    public interface ServiceOrServerThrower<T, R> {

        R apply(T t) throws DSOutOfServiceException, ServerError;

    }


    @FunctionalInterface
    public interface ServerOrAccessThrower<T, R> {

        R apply(T t) throws DSAccessException, ServerError;

    }

}
