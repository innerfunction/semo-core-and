package com.innerfunction.util.test;

import java.util.ArrayList;
import java.util.List;

import com.innerfunction.util.Deferred;

import android.test.AndroidTestCase;

public class DeferredAllTest extends AndroidTestCase {
    ArrayList<Boolean> expectedresult;
    
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
    public void testDeferredALL() {
        final List<Deferred<Boolean>> deferreds = new ArrayList<Deferred<Boolean>>();
        deferreds.add(promise1());
        deferreds.add(promise2());
        deferreds.add(promise3());
        
        final Deferred<Boolean> deferred = new Deferred<Boolean>();
        Deferred.all( deferreds )
        .then(new Deferred.Callback<List<Boolean>>() {
            @Override
            public List<Boolean> result(List<Boolean> result) {
                deferred.resolve( true );
                assertEquals(expectedresult,result);
                return null;
            }
        });
    }

}
