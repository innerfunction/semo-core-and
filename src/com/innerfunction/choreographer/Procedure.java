package com.innerfunction.choreographer;

/**
 * A stepped procedure.
 * A procedure which is broken down into a sequence of idempotent steps. The procedure may take a single
 * string argument, and may return a single string result. Each procedure step should communicate through
 * its process object in order to progress to another step, call another procedure, store procedure vars,
 * indicate procedure completion or indicate a procedure error.
 * The procedure choreographer will record the procedure's progress through each of its states, and will
 * use the recorded states to resume the procedure if interrupted at any particular point. In this way,
 * once started, a procedure is guaranteed to progress through each of its steps to completion unless an
 * error occurs during step execution. Each step in the procedure will be executed at least once if called,
 * but may be executed multiple times if the host app is interrupted; for this reason, it is important that
 * procedure steps are idempotent.
 * Every procedure should have a "start" step, called when the procedure is started. The start step receives
 * whichever argument is passed to the procedure when started.
 * @author juliangoacher
 *
 */
public interface Procedure {

    /**
     * Execute a procedure step.
     * @param step      The name of the step to execute.
     * @param arg       The step argument.
     * @param process   The process executing the procedure.
     */
    public void step(String step, String arg, Process process);
    
}
