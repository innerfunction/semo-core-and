package com.innerfunction.semo;

/**
 * A container object to be started and/or stopped before use.
 * @author juliangoacher
 *
 */
public interface Service {

    public void startService();
    
    public void stopService();
    
}
