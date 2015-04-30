package com.innerfunction.choreographer;

import com.innerfunction.util.Locals;

/**
 * A process running a procedure.
 * @author juliangoacher
 *
 */
public class Process {

    /** The process number. */
    protected Number pid;
    /** The name of the procedure being run. */
    protected String procedureName;
    /** The procedure being run. */
    private Procedure procedure;
    /** The parent choreographer. */
    private Choreographer choreographer;
    /** Locals used to record process state. */
    private Locals locals;
    
    /**
     * Create a new process.
     * @param pid
     * @param choreographer
     * @param procedureName
     */
    public Process(Number pid, Choreographer choreographer, String procedureName) {
        this.pid = pid;
        this.choreographer = choreographer;
        this.procedureName = procedureName;
        procedure = choreographer.getProcedure( procedureName );
        locals = new Locals( String.format("%s.%s", Process.class.getName(), pid ) );
    }

    /**
     * Re-create a new process. The procedure name is expected to be found in the process' local storage.
     * @param pid
     * @param choreographer
     */
    public Process(Number pid, Choreographer choreographer) throws ProcessException {
        this.pid = pid;
        this.choreographer = choreographer;
        locals = new Locals( String.format("%s.%s", Process.class.getName(), pid ) );
        procedureName = locals.getString("procedureName");
        if( procedureName == null ) {
            throw new ProcessException("No procedure name found for resumed process");
        }
        procedure = choreographer.getProcedure( procedureName );
        if( procedure == null ) {
            throw new ProcessException("Procedure %s not found", procedureName );
        }
    }
    
    public Number getPID() {
        return pid;
    }
    
    /**
     * Start the process.
     * Calls the procedure's 'start' step.
     * @param arg
     */
    protected void start(String arg) {
        step("start", arg );
    }
    
    /**
     * Resume the process from its last recorded step.
     */
    protected void resume() {
        String step = locals.getString("$step");
        if( step != null ) {
            String arg = locals.getString("$arg");
            step( step, arg );
        }
        else {
            done( null );
        }
    }
    
    /**
     * Execute a step in the procedure.
     * @param name
     * @param arg
     */
    public void step(String name, String arg) {
        try {
            locals.setString("$step", name );
            locals.setString("$arg", arg );
            procedure.step( name, arg, this );
        }
        catch(Exception e) {
            error( e );
        }
    }
    
    public void step(String name) {
        step( name, null );
    }
    
    /**
     * Call another procedure from this process.
     * @param procedure
     * @param arg
     */
    public void call(String procedure, String arg) {
        try {
            choreographer.startProcedure( procedure, arg, false );
        }
        catch(ProcessException e) {
            error( e );
        }
    }
    
    public void call(String procedure) {
        call( procedure, null );
    }
    
    /**
     * Signal process completion.
     * @param result
     */
    public void done(String result) {
        choreographer.done( pid, result );
        locals.removeAll();
    }
    
    /**
     * Signal process completion with no result.
     */
    public void done() {
        done( null );
    }
    
    /**
     * Signal process failure.
     * @param e
     */
    public void error(Exception e) {
        choreographer.error( pid, e );
        locals.removeAll();
    }
    
    /**
     * Signal process failure.
     * @param message
     */
    public void error(String message) {
        choreographer.error(pid,  new Exception( message ) );
        locals.removeAll();
    }
    
    /**
     * Get the locals for this process.
     */
    public Locals getLocals() {
        return locals;
    }

}
