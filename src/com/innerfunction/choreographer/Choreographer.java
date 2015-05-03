package com.innerfunction.choreographer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import android.annotation.SuppressLint;
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
@SuppressLint("DefaultLocale")
public class Choreographer implements Service {

    static final String Tag = Choreographer.class.getSimpleName();
    
    /** A map of registered procedures, keyed by name. */
    private Map<String,Procedure> procedures;
    /** A map of running processes, keyed by process ID. */
    private Map<Number,Process> processes = new HashMap<Number,Process>();
    /** A map of waiting processes, keyed by the ID of the process they are waiting for. */
    private Map<Number,Process> waiting = new HashMap<Number,Process>();
    /** A map of lists of registered procedure listeners. */
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
     * Start a named procedure with the specified arguments.
     * TODO: Option to only start a procedure if no process for the same procedure + arg is running?
     * @param procedureName
     * @param args
     * @return The process ID of newly started procedure.
     */
    public Number startProcedure(String procedureName, Object... args) throws ProcessException {
        return startProcedure( procedureName, true, args );
    }
    
    /**
     * Start a named procedure with the specified parent process and arguments.
     */
    public Number startProcedure(Process parent, String procedureName, Object...args) throws ProcessException {
        Number pid = startProcedure( procedureName, false, args );
        setWaitingProcess( parent, pid );
        return pid;
    }
    
    /**
     * Start a named procedure with the specified arguments.
     * TODO: Option to only start a procedure if no process for the same procedure + arg is running?
     * @param procedureName
     * @param arg
     * @param runInBackground
     * @return The process ID of newly started procedure.
     */
    public synchronized Number startProcedure(final String procedureName, boolean runInBackground, final Object... args) throws ProcessException {
        Number pid = -1;
        if( procedures.containsKey( procedureName ) ) {
            // Find next free process number.
            while( processes.containsKey( pidCounter ) ) {
                pidCounter++;
            }
            pid = pidCounter;
            // Create and record the process.
            final Process process = new Process( pid, this, procedureName );
            processes.put( pid, process );
            saveProcessIDs();
            // Start the process.
            if( runInBackground ) {
                final Number _pid = pid;
                BackgroundTaskRunner.run(new BackgroundTaskRunner.Task() {
                    @Override
                    public void run() {
                        process.start( args );
                        Log.d(Tag,String.format("Started process %d for procedure %s in background", _pid, procedureName ) );
                    }
                });
            }
            else {
                process.start( args );
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
    public synchronized void done(Number pid, Object result) {
        Process process = processes.get( pid );
        if( process != null ) {
            // Notify the waiting parent process, if any.
            Process parent = waiting.get( pid );
            if( parent != null ) {
                parent.childProcessCompleted( result );
                waiting.remove( pid );
            }
            // Notify listeners.
            List<ProcedureListener> plist = listeners.get( process.procedureName );
            for( ProcedureListener listener : plist ) {
                listener.procedureCompleted( process.procedureName, pid, result );
            }
            // Remove the process.
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
            Process parent = waiting.get( pid );
            if( parent != null ) {
                String errMsg = String.format("Child process %d failed", pid );
                parent.error( new ProcessException( errMsg ) );
                waiting.remove( pid );
            }
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
     * Set a waiting process.
     * @param process The waiting process.
     * @param pid     The ID of the process being waited for.
     */
    public void setWaitingProcess(Process process, Number pid) {
        waiting.put( pid, process );
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
            final List<Process> retreivedProcesses = new ArrayList<Process>();
            int i;
            // Build list of retreived processes & resume their waiting status.
            for( i = 0; i < pids.length; i++ ) {
                try {
                    Number pid = Integer.valueOf( i );
                    Process process = new Process( pid, this );
                    processes.put( pid, process );
                    retreivedProcesses.add( process );
                    process.resumeWait();
                }
                catch(Exception e) {
                    Log.e(Tag,"Error resuming process wait", e );
                }
            }
            // Resume all retreived processes.
            for( i = 0; i < pids.length; i++ ) {
                try {
                    final Process process = retreivedProcesses.get( i );
                    BackgroundTaskRunner.run(new BackgroundTaskRunner.Task() {
                        @Override
                        public void run() {
                            process.resume();
                            Log.d(Tag,String.format("Resumed process %d", process.pid ));
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
