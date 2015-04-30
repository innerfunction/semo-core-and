package com.innerfunction.choreographer;

public class ProcessException extends Exception {

    private static final long serialVersionUID = 1L;

    public ProcessException(String message, Object... args) {
        super( String.format( message, args ) );
    }
    
}
