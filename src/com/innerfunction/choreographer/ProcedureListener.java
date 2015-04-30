package com.innerfunction.choreographer;

/**
 * Interface for monitoring a procedure's progress.
 * @author juliangoacher
 *
 */
public interface ProcedureListener {

    /**
     * Receive notification of a procedure's completion.
     * @param procedureName The name of the completed procedure.
     * @param pid           The process ID of the completed procedure.
     * @param result        The procedure result.
     */
    public void procedureCompleted(String procedureName, Number pid, String result);
    
}
