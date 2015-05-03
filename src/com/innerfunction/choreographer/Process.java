package com.innerfunction.choreographer;

import java.util.List;
import java.util.Map;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

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
     * @param args
     */
    protected void start(Object... args) {
        step("start", args );
    }
    
    /** Test whether the interrupted process is waiting for a procedure call return. */ 
    protected boolean isWaiting() {
        return locals.getString("$wait") != null;
    }
    
    /**
     * Resume the process from its wait state.
     */
    @SuppressWarnings("rawtypes")
    protected void resumeWait() {
        String waitJSON = locals.getString("$wait");
        if( waitJSON != null ) {
            Map waitData = (Map)JSONValue.parse( waitJSON );
            Number pid = (Number)waitData.get("pid");
            choreographer.setWaitingProcess( this, pid );
        }
    }
    
    /**
     * Resume the process from its last recorded step.
     */
    @SuppressWarnings("rawtypes")
    protected void resume() {
        if( !isWaiting() ) {
            String stepJSON = locals.getString("$step");
            if( stepJSON != null ) {
                Map stepData = (Map)JSONValue.parse( stepJSON );
                String step = (String)stepData.get("step");
                Object args = ((List)stepData.get("args")).toArray();
                step( step, args );
            }
            else {
                done();
            }
        }
    }
    
    /**
     * Execute a step in the procedure.
     * @param name
     * @param args
     */
    @SuppressWarnings("unchecked")
    public void step(String name, Object... args) {
        try {
            JSONObject stepData = new JSONObject();
            stepData.put("step", name );
            JSONArray stepArgs  = new JSONArray();
            if( args != null ) {
                for( Object arg : args ) {
                    stepArgs.add( arg );
                }
            }
            stepData.put("args", stepArgs );
            locals.setString("$step", stepData.toJSONString() );
            procedure.step( this, name, args );
        }
        catch(Exception e) {
            error( e );
        }
    }
    
    /**
     * Call another procedure from this process.
     * @param procedure
     * @param contStep
     * @param args
     */
    @SuppressWarnings("unchecked")
    public void call(String procedure, String contStep, Object... args) {
        try {
            Number pid = choreographer.startProcedure( this, procedure, args );
            JSONObject waitData = new JSONObject();
            waitData.put("pid", pid );
            waitData.put("cont", contStep );
            locals.setString("$wait", waitData.toJSONString() );
        }
        catch(ProcessException e) {
            error( e );
        }
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
     * Receive notification that the child process that this process is waiting for has completed.
     * @param result
     */
    @SuppressWarnings("rawtypes")
    protected void childProcessCompleted(Object result) {
        String waitJSON = locals.getString("$wait");
        if( waitJSON != null ) {
            Map waitData = (Map)JSONValue.parse( waitJSON );
            String contStep = (String)waitData.get("cont");
            step( contStep, result );
            locals.remove("$wait");
        }
    }
    
    /**
     * Get the locals for this process.
     */
    public Locals getLocals() {
        return locals;
    }

}
