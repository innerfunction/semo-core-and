package com.innerfunction.util.test;

import java.util.concurrent.Semaphore;

import com.innerfunction.semo.test.Animal;
import com.innerfunction.util.BackgroundTaskRunner;
import com.innerfunction.util.Deferred;
import com.innerfunction.util.Deferred.Callback;

import android.os.Handler;
import android.os.Looper;
import android.test.AndroidTestCase;
import android.util.Log;

public class AsyncTest extends AndroidTestCase {

    Animal monkey;
    Deferred<Object> deferredObject;
    Deferred<String> deferredString;
    String testString = "test";
    Semaphore semaphore;

    // Test method here
    // new Semaphore
    // deferred.then(new Callback)
    // check test result
    // semaphore.release()
    // Handler, postDelayed, run after 500
    // run() {
    // deferred.resolve( value )
    //
    // semaphore.aquire()
    
    public void testString() throws InterruptedException {
        semaphore = new Semaphore(1);
        deferredString = new Deferred<String>();
        deferredString.then(new Callback<String>() {
            @Override
            public String result(String result) {
                assertEquals(result, testString);
                semaphore.release();
                return result;
            }
        });
        BackgroundTaskRunner.run(new Runnable(){
            @Override
            public void run() {
                Looper.prepare();
                Handler h = new Handler();
                h.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        deferredString.resolve(testString);
                        Looper.myLooper().quit();
                    }
                },200);
                Looper.loop();
                
            }
        });
        semaphore.acquire();
    }
    
    public void testAnimal() throws InterruptedException {
        
        semaphore = new Semaphore(1);
        deferredObject = new Deferred<Object>();
        monkey = new Animal();
        monkey.setName("Name");
        deferredObject.then(new Callback<Object>() {
            @Override
            public Object result(Object result) {
                assertSame(monkey, (Animal) result);
                assertEquals(monkey.getName(), ((Animal) result).getName());
                semaphore.release();
                return result;
            }
        });
        BackgroundTaskRunner.run(new Runnable(){
            @Override
            public void run() {
                Looper.prepare();
                Handler h = new Handler();
                h.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        deferredObject.resolve(monkey);
                        Looper.myLooper().quit();
                    }
                },200);
                Looper.loop();
                
            }
        });
        semaphore.acquire();
    }

}
