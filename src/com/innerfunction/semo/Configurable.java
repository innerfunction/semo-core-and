package com.innerfunction.semo;

/**
 * A configurable object.
 * Allows an object to configure itself from its configuration, rather than using IOC.
 * @author juliangoacher
 *
 */
public interface Configurable {

    public void configure(Configuration configuration);
    
}
