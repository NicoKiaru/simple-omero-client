package fr.igred.omero.exception;


import omero.ServerError;
import omero.gateway.exception.DSAccessException;
import omero.gateway.exception.DSOutOfServiceException;

import java.util.NoSuchElementException;
import java.util.Objects;


public class ExceptionHandler<T> {

    private final Throwable exception;
    private final T         value;
    private final String    error;


    private ExceptionHandler(T value, Throwable t, String error) {
        this.value = value;
        this.exception = t;
        this.error = error;
    }


    public static <A> ExceptionHandler<A> of(A value, String errorMessage) {
        return new ExceptionHandler<>(Objects.requireNonNull(value), null, errorMessage);
    }


    @SuppressWarnings("unchecked")
    private static <E extends Throwable> void sneakyThrow(Throwable t) throws E {
        throw (E) t;
    }


    public static <T, U> U handleServiceAndAccess(T value, ThrowingFunction<? super T, ? extends U> mapper,
                                                  String error)
    throws ServiceException, AccessException {
        return of(value, error)
                .map(mapper)
                .propagate(DSOutOfServiceException.class, ServiceException::new)
                .propagate(DSAccessException.class, AccessException::new)
                .get();
    }


    public static <T, U> U handleServiceAndServer(T value, ThrowingFunction<? super T, ? extends U> mapper,
                                                  String error)
    throws ServiceException, OMEROServerError {
        return of(value, error)
                .map(mapper)
                .propagate(DSOutOfServiceException.class, ServiceException::new)
                .propagate(ServerError.class, OMEROServerError::new)
                .get();
    }


    public <U> ExceptionHandler<U> map(ThrowingFunction<? super T, ? extends U> mapper) {
        Objects.requireNonNull(mapper);
        Throwable throwable = null;

        U u = null;
        try {
            u = mapper.apply(value);
        } catch (Throwable t) {
            throwable = t;
        }
        return new ExceptionHandler<>(u, throwable, error);
    }


    public ExceptionHandler<T> apply(ThrowingConsumer<? super T> consumer) {
        Objects.requireNonNull(consumer);
        Throwable throwable = null;
        try {
            consumer.apply(value);
        } catch (Throwable t) {
            throwable = t;
        }
        return new ExceptionHandler<>(value, throwable, error);
    }


    public <E extends Throwable> ExceptionHandler<T> propagate(Class<E> excType) throws E {
        if (excType.isInstance(exception))
            throw excType.cast(exception);
        return this;
    }


    public <E extends Throwable, F extends Throwable> ExceptionHandler<T>
    propagate(Class<E> excType, ExceptionWrapper<? super E, ? extends F> translator)
    throws F {
        if (excType.isInstance(exception))
            throw translator.apply(error, excType.cast(exception));
        return this;
    }


    public T get() {
        if (exception != null) sneakyThrow(exception);
        return value;
    }


    @FunctionalInterface
    public interface ThrowingFunction<T, R> {

        R apply(T t) throws Throwable;

    }


    @FunctionalInterface
    public interface ThrowingConsumer<T> {

        void apply(T t) throws Throwable;

    }


    @FunctionalInterface
    public interface ExceptionWrapper<T, E> {

        E apply(String message, T t);

    }

}
