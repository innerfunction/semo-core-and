package com.innerfunction.util.test;

import com.innerfunction.semo.test.Animal;
import com.innerfunction.util.Deferred;
import com.innerfunction.util.Deferred.Callback;

import android.test.AndroidTestCase;

public class SynchronousTest extends AndroidTestCase {
    
    Deferred<String> deferredString;
    Deferred<Object> deferredObject;
    Deferred<Deferred<Object>> deferredPromise;
    String stringPromise;
    Animal monkey;
    //promises
    //asytask
    //no setUp(), variables on each method
    public void setUp() {
        deferredString = new Deferred<String>();
        stringPromise= "Promise String";
        deferredObject = new Deferred<Object>();
        monkey = new Animal();
        deferredPromise = new Deferred<Deferred<Object>>();
    }

    public void testReturnString() {
        deferredString.resolve(stringPromise);
        deferredString.then(new Callback<String>() {
            @Override
            public String result(String result) {
                assertEquals(stringPromise, result);
                return result;
            }
        });
    }

    public void testReturnObject() {
        monkey.setName("Name");
        deferredObject.resolve(monkey);
        deferredObject.then(new Callback<Object>() {
            @Override
            public Object result(Object result) {
                Animal resultAnimal= (Animal) result;
                assertSame(result, monkey);
                assertEquals(resultAnimal.getName(), monkey.getName());
                return result;
            }
        });
       
    }
    
    public void testReturnNull() {
        deferredObject.resolve(null);
        deferredObject.then(new Callback<Object>() {
            @Override
            public Object result(Object result) {
                assertSame(result, null);
                return result;
            }
        });
    }
    
    public void testReturnPromise() {
        deferredPromise.resolve(deferredObject);
        deferredPromise.then(new Callback<Deferred<Object>>() {
            @Override
            public Deferred<Object> result(Deferred<Object> result) {
                assertSame(result, deferredObject);
                return result;
            }
        });
    }
}
    
