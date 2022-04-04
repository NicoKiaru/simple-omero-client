/*
 * Copyright (C) 2020-2022 GReD
 * Copyright (C) 2015, Marko Topolnik. All Rights Reserved.
 *
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 3 of the License, or (at your option) any later
 * version.

 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.

 * You should have received a copy of the GNU General Public License along with
 * this program; if not, see <https://www.gnu.org/licenses/>.
 *
 * This file incorporates code covered by the following terms:
 *
 *      Licensed under the Apache License, Version 2.0 (the "License");
 *      you may not use this file except in compliance with the License.
 *      You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *      Unless required by applicable law or agreed to in writing, software
 *      distributed under the License is distributed on an "AS IS" BASIS,
 *      WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *      See the License for the specific language governing permissions and
 *      limitations under the License.
 */

package fr.igred.omero.exception;


import omero.ServerError;
import omero.gateway.exception.DSAccessException;
import omero.gateway.exception.DSOutOfServiceException;

import java.util.Objects;


/**
 * Class with methods to handle OMERO exceptions
 */
public final class ExceptionHandler<T> {

    private final Exception exception;
    private final T         value;
    private final String    error;


    /**
     * Private class constructor.
     *
     * @param value     Object to process.
     * @param exception Caught exception.
     * @param error     Error message.
     */
    private ExceptionHandler(T value, Exception exception, String error) {
        this.value = value;
        this.exception = exception;
        this.error = error;
    }


    /**
     * Create ExceptionHandler from object.
     *
     * @param value        Object to process.
     * @param errorMessage Error message.
     * @param <A>          Object type.
     *
     * @return ExceptionHandler wrapping the object to process.
     */
    public static <A> ExceptionHandler<A> of(A value, String errorMessage) {
        return new ExceptionHandler<>(Objects.requireNonNull(value), null, errorMessage);
    }


    /**
     * Sneakily throws an exception.
     *
     * @param t   The exception to throw.
     * @param <E> Type of Exception thrown
     *
     * @throws E Exception thrown.
     */
    @SuppressWarnings("unchecked")
    private static <E extends Throwable> void sneakyThrow(Exception t) throws E {
        throw (E) t;
    }


    /**
     * Apply a function to the specified object and return the result or throw {@link ServiceException} or {@link
     * AccessException}.
     *
     * @param value  Object to process.
     * @param mapper Lambda to apply on object.
     * @param error  Error message if an exception is thrown.
     * @param <T>    Object type.
     * @param <U>    Lambda result type.
     *
     * @return Whatever the lambda returns.
     *
     * @throws ServiceException Cannot connect to OMERO.
     * @throws AccessException  Cannot access data.
     */
    public static <T, U> U handleServiceAndAccess(T value,
                                                  ThrowingFunction<? super T, ? extends U, ? extends Exception> mapper,
                                                  String error)
    throws ServiceException, AccessException {
        return of(value, error)
                .map(mapper)
                .propagate(DSOutOfServiceException.class, ServiceException::new)
                .propagate(DSAccessException.class, AccessException::new)
                .get();
    }


    /**
     * Apply a function to the specified object and return the result or throw {@link ServiceException} or {@link
     * OMEROServerError}.
     *
     * @param value  Object to process.
     * @param mapper Lambda to apply on object.
     * @param error  Error message if an exception is thrown.
     * @param <T>    Object type.
     * @param <U>    Lambda result type.
     *
     * @return Whatever the lambda returns.
     *
     * @throws ServiceException Cannot connect to OMERO.
     * @throws OMEROServerError If the thread was interrupted.
     */
    public static <T, U> U handleServiceAndServer(T value,
                                                  ThrowingFunction<? super T, ? extends U, ? extends Exception> mapper,
                                                  String error)
    throws ServiceException, OMEROServerError {
        return of(value, error)
                .map(mapper)
                .propagate(DSOutOfServiceException.class, ServiceException::new)
                .propagate(ServerError.class, OMEROServerError::new)
                .get();
    }


    /**
     * Convert the contained value.
     *
     * @param mapper Lambda to apply on the contained object.
     * @param <U>    Lambda result type.
     *
     * @return An ExceptionHandler wrapping the result or an exception.
     */
    public <U> ExceptionHandler<U> map(ThrowingFunction<? super T, ? extends U, ? extends Exception> mapper) {
        Objects.requireNonNull(mapper);
        Exception ex = null;

        U u = null;
        try {
            u = mapper.apply(value);
        } catch (Exception t) {
            ex = t;
        }
        return new ExceptionHandler<>(u, ex, error);
    }


    /**
     * Apply a function to the contained value.
     *
     * @param consumer Lambda to apply on the contained object.
     *
     * @return An ExceptionHandler containing the original value and possibly an exception.
     */
    public ExceptionHandler<T> apply(ThrowingConsumer<? super T, ? extends Exception> consumer) {
        Objects.requireNonNull(consumer);
        Exception ex = null;
        try {
            consumer.apply(value);
        } catch (Exception t) {
            ex = t;
        }
        return new ExceptionHandler<>(value, ex, error);
    }


    /**
     * Throw an exception from the specified type, if one was caught.
     *
     * @param type The exception class.
     * @param <E>  The type of the exception.
     *
     * @return The same ExceptionHandler.
     *
     * @throws E An exception from the specified type.
     */
    public <E extends Throwable> ExceptionHandler<T> propagate(Class<E> type) throws E {
        if (type.isInstance(exception))
            throw type.cast(exception);
        return this;
    }


    /**
     * Throw an exception converted from the specified type, if one was caught.
     *
     * @param type   The exception class.
     * @param mapper Lambda to convert the caught exception.
     * @param <E>    The type of the exception.
     * @param <F>    The type of the exception thrown.
     *
     * @return The same ExceptionHandler.
     *
     * @throws F A converted Exception.
     */
    public <E extends Throwable, F extends Throwable> ExceptionHandler<T>
    propagate(Class<E> type, ExceptionWrapper<? super E, ? extends F> mapper)
    throws F {
        if (type.isInstance(exception))
            throw mapper.apply(error, type.cast(exception));
        return this;
    }


    /**
     * Returns the contained object.
     *
     * @return See above.
     */
    public T get() {
        if (exception != null) sneakyThrow(exception);
        return value;
    }


    /**
     * @param <T> The input type.
     * @param <R> The output type.
     * @param <E> The exception type.
     */
    @FunctionalInterface
    public interface ThrowingFunction<T, R, E extends Throwable> {

        R apply(T t) throws E;

    }


    /**
     * @param <T> The input type.
     * @param <E> The exception type.
     */
    @FunctionalInterface
    public interface ThrowingConsumer<T, E extends Throwable> {

        void apply(T t) throws E;

    }


    /**
     * @param <T> The input type.
     * @param <E> The exception type.
     */
    @FunctionalInterface
    public interface ExceptionWrapper<T, E extends Throwable> {

        E apply(String message, T t);

    }

}
