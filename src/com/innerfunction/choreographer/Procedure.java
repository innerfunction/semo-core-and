package com.innerfunction.choreographer;

/**
 * A stepped procedure.
 * @author juliangoacher
 *
 */
public interface Procedure {

    public void step(String step, String arg, Process process);
    

}
