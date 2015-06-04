package com.innerfunction.util.test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Semaphore;

import com.innerfunction.util.BackgroundTaskRunner;
import com.innerfunction.util.Deferred;
import com.innerfunction.util.BackgroundTaskRunner.Task;

import android.os.Handler;
import android.os.Looper;
import android.test.AndroidTestCase;

public class DeferredAllTestAsync extends AndroidTestCase {
    ArrayList<Boolean> expectedresult;
    Semaphore semaphore;
    
    public void setUp() throws Exception {
        expectedresult= new ArrayList<Boolean>();
        expectedresult.add(true);
        expectedresult.add(false);
        expectedresult.add(null);
    }
    
    public Deferred<Boolean> promise1() {
        return Deferred.defer( true);
    }
    public Deferred<Boolean> promise2() {
        return Deferred.defer( false);
    }
    public Deferred<Boolean> promise3() {
        return Deferred.defer( null);
    }
    public void testDeferredALL() throws InterruptedException {
        final List<Deferred<Boolean>> deferreds = new ArrayList<Deferred<Boolean>>();
        deferreds.add(promise1());
        deferreds.add(promise2());
        deferreds.add(promise3());
        semaphore = new Semaphore(1);
        final Deferred<Boolean> deferred = new Deferred<Boolean>();
        Deferred.all( deferreds )
        .then(new Deferred.Callback<List<Boolean>>() {
            @Override
            public List<Boolean> result(List<Boolean> result) {
                assertEquals(expectedresult,result);
                semaphore.release();
                return result;
            }
        });
        BackgroundTaskRunner.run(new Task(){
            @Override
            public void run() {
                Looper.prepare();
                Handler h = new Handler();
                h.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        deferred.resolve(true);
                        Looper.myLooper().quit();
                    }
                },200);
                Looper.loop();
            }
        });
        semaphore.acquire();
    }
}
