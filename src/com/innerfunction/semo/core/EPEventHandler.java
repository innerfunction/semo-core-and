package com.innerfunction.semo.core;

/**
 * A UI event handler.
 * @author juliangoacher
 *
 */
public interface EPEventHandler {

    /** Token object to be returned when an event is not handled by an event handler. */
    public static final Object EventNotHandled = new Object();

    /**
     * Handle an event.
     * Return EventNotHandled if the event isn't recognized; otherwise return a value representing the event result.
     */
    public Object handleEPEvent(EPEvent event);

}
