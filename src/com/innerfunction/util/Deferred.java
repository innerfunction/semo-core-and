package com.innerfunction.util;

import java.util.ArrayList;
import java.util.List;

/**
 * A simple deferred promise implementation.
 * @author juliangoacher
 *
 * @param <T>
 */
public class Deferred<T> {

    /** Callback for passing a deferred promise result. */
    public static interface Callback<T> {
        public T result(T result);
        
    }
    
    /** Callback for passing a deferred promise error. */
    public static interface ErrorCallback {
        public void error(Exception e);
    }
    
    private T result;
    private Exception error;
    private Callback<T> then;
    private Deferred<T> next;
    private ErrorCallback fail;
    /** Flag indicating whether the promise has been resolved. */
    private boolean resolved;
    /** Flag indicating whether the promise has been rejected. */
    private boolean rejected;
    
    public Deferred() {}
    
    public Deferred(T result) {
        resolve( result );
    }
    
    public static <R> Deferred<R> defer(R result) {
        return new Deferred<R>( result );
    }
    
    /**
     * Wait for all promises in a list to resolve or reject.
     * @param deferreds
     * @return
     */
    public static <R> Deferred<List<R>> all(final List<Deferred<R>> deferreds) {
        final Deferred<List<R>> dresult = new Deferred<List<R>>();
        final List<R> results = new ArrayList<R>();
        for(Deferred<R> deferred : deferreds) {
            deferred
            .then(new Callback<R>() {
                @Override
                public R result(R result) {
                    results.add( result );
                    if( results.size() == deferreds.size() ) {
                        dresult.resolve( results );
                    }
                    return null;
                }
            })
            .error(new ErrorCallback() {
                @Override
                public void error(Exception e) {
                    dresult.reject( e );
                }
            });
        }
        return dresult;
    }

    /**
     * Resolve the promise by passing a result.
     * @param result
     */
    public void resolve(T result) {
        if( !(resolved || rejected) ) {
            try {
                if( result instanceof Deferred ) {
                    @SuppressWarnings("unchecked")
                    Deferred<T> deferredResult = (Deferred<T>)result;
                    deferredResult
                    .then(new Callback<T>() {
                        @Override
                        public T result(T result) {
                            // There are obvious problems here if the chained deferred resolves to a value of
                            // a different and incompatible type to the parent deferred.
                            Deferred.this.resolve( result );
                            return null;
                        }
                    })
                    .error(new ErrorCallback() {
                        @Override
                        public void error(Exception e) {
                            Deferred.this.reject( e );
                        }
                    });
                }
                else {
                    resolved = true;
                    this.result = result;
                    if( next != null ) {
                        try {
                            next.resolve( then.result( result ) );
                        }
                        catch(Exception e) {
                            next.reject( e );
                        }
                    }
                }
            }
            catch(Exception e) {
                reject( e );
            }
        }
    }

    /**
     * Reject the promise by passing an error.
     * @param e
     */
    public void reject(Exception e) {
        if( !(resolved || rejected) ) {
            rejected = true;
            if( fail != null ) {
                fail.error( e );
            }
            else if( next != null ) {
                next.reject( e );
            }
        }
    }
    
    /**
     * Add a promise result callback.
     * @param cb
     * @return
     */
    public Deferred<T> then(Callback<T> cb) {
        next = new Deferred<T>();
        if( resolved ) {
            try {
                next.resolve( cb.result( result ) );
            }
            catch(Exception e) {
                next.reject( e );
            }
        }
        else if( !rejected ) {
            this.then = cb;
        }
        return next;
    }
    
    /**
     * Add a promise reject callback.
     * @param cb
     * @return
     */
    public Deferred<T> error(ErrorCallback cb ) {
        if( rejected ) {
            cb.error( error );
        }
        else if( !resolved ) {
            this.fail = cb;
        }
        return this;
    }
    
}
