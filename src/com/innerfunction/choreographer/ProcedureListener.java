package com.innerfunction.choreographer;

/**
 * Interface for monitoring a procedure's progress.
 * @author juliangoacher
 *
 */
public interface ProcedureListener {

    public void procedureCompleted(String procedure, Number pid, String result);
    
}
