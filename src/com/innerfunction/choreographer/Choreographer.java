package com.innerfunction.choreographer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.util.Log;

import com.innerfunction.semo.Service;
import com.innerfunction.util.BackgroundTaskRunner;
import com.innerfunction.util.Locals;

/**
 * A class for choreographing stepped procedures.
 * Manages calls to stepped procedures. Ensures that all process state is recorded and can be resumed
 * in case of interruption.
 * @author juliangoacher
 *
 */
public class Choreographer implements Service {

    static final String Tag = Choreographer.class.getSimpleName();
    
    /** A map of registered procedures, keyed by name. */
    private Map<String,Procedure> procedures;
    /** A map of running processes, keyed by process ID. */
    private Map<Number,Process> processes = new HashMap<Number,Process>();
    /** A list of registered procedure listeners. */
    private Map<String,List<ProcedureListener>> listeners;
    /** Local storage, used to record list of running processes. */
    private Locals locals = new Locals( Choreographer.class );
    /** Process ID counter. */
    private int pidCounter = 0;
    
    public void setProcedures(Map<String,Procedure> procedures) {
        this.procedures = procedures;
    }
    
    public Procedure getProcedure(String name) {
        return procedures.get( name );
    }
    
    /**
     * Start a named procedure with the specified argument.
     * TODO: Option to only start a procedure if no process for the same procedure + arg is running?
     * @param procedureName
     * @param arg
     * @return The process ID of newly started procedure.
     */
    public synchronized Number startProcedure(String procedureName, String arg) throws ProcessException {
        return startProcedure( procedureName, arg, true );
    }
    
    /**
     * Start a named procedure with the specified argument.
     * TODO: Option to only start a procedure if no process for the same procedure + arg is running?
     * @param procedureName
     * @param arg
     * @param runInBackground
     * @return The process ID of newly started procedure.
     */
    public synchronized Number startProcedure(final String procedureName, final String arg, boolean runInBackground) throws ProcessException {
        Number pid = -1;
        if( procedures.containsKey( procedureName ) ) {
            while( processes.containsKey( pidCounter ) ) {
                pidCounter++;
            }
            pid = pidCounter;
            final Process process = new Process( pid, this, procedureName );
            processes.put( pid, process );
            saveProcessIDs();
            if( runInBackground ) {
                final Number _pid = pid;
                BackgroundTaskRunner.run(new BackgroundTaskRunner.Task() {
                    @Override
                    public void run() {
                        process.start( arg );
                        Log.d(Tag,String.format("Started process %d for procedure %s in background", _pid, procedureName ) );
                    }
                });
            }
            else {
                process.start( arg );
                Log.d(Tag,String.format("Started process %d for procedure %s", pid, procedureName ) );
            }
        }
        else {
            throw new ProcessException("Procedure %s not found", procedureName );
        }
        return pid;
    }
    
    /**
     * Handle a process completion.
     * @param pid
     * @param result
     */
    public synchronized void done(Number pid, String result) {
        Process process = processes.get( pid );
        if( process != null ) {
            List<ProcedureListener> plist = listeners.get( process.procedureName );
            for( ProcedureListener listener : plist ) {
                listener.procedureCompleted( process.procedureName, pid, result );
            }
            processes.remove( process.pid );
            saveProcessIDs();
            Log.d(Tag,String.format("Process %d completed",  process.pid ));
        }
    }
    
    /**
     * Handle a process failure.
     * @param pid
     * @param e
     */
    public synchronized void error(Number pid, Exception e) {
        Process process = processes.get( pid );
        if( process != null ) {
            processes.remove( process.pid );
            saveProcessIDs();
        }
        Log.e(Tag,String.format("Process %d: Error", pid ), e );
    }
    
    /**
     * Write the list of currently running process IDs to local storage.
     */
    private void saveProcessIDs() {
        StringBuilder pids = new StringBuilder();
        for( Number pid : processes.keySet() ) {
            if( pids.length() > 0 ) pids.append(',');
            pids.append( pid );
        }
        locals.setString("pids", pids.toString() );
    }
    
    /**
     * Register a procedure listener.
     * @param procedure
     * @param listener
     */
    public void addProcedureListener(String procedure, ProcedureListener listener) {
        List<ProcedureListener> plist = listeners.get( procedure );
        if( plist == null ) {
            plist = new ArrayList<ProcedureListener>();
            listeners.put( procedure, plist );
        }
        plist.add( listener );
    }

    /**
     * Start the choreographer.
     * Resumes any interrupted processes.
     */
    @Override
    public void startService() {
        String spids = locals.getString("pids");
        if( spids != null ) {
            String[] pids = spids.split(",");
            for( int i = 0; i < pids.length; i++ ) {
                try {
                    final Number pid = Integer.valueOf( pids[i] );
                    final Process process = new Process( pid, this );
                    processes.put( pid, process );
                    BackgroundTaskRunner.run(new BackgroundTaskRunner.Task() {
                        @Override
                        public void run() {
                            process.resume();
                            Log.d(Tag,String.format("Resumed process %d", pid ));
                        }
                    });
                }
                catch(Exception e) {
                    Log.e(Tag,"Error resuming process", e );
                }
            }
        }
    }

    @Override
    public void stopService() {
    }
    
}
