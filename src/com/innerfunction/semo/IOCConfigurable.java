package com.innerfunction.semo;

/**
 * Interface allowing IOC configurable objects to detect when configuration is taking place.
 * @author juliangoacher
 *
 */
public interface IOCConfigurable {

    /** Called immediately before the object is configured by calls to its properties. */
    public void beforeConfigure(Container container);
    
    /** Called immediately after the object is configured by calls to its properties. */
    public void afterConfigure(Container container);
    
}
