package com.innerfunction.uri.test;

import com.innerfunction.uri.CompoundURI;
import com.innerfunction.uri.StandardURIResolver;

import android.test.AndroidTestCase;

public class ResourceTest extends AndroidTestCase {

    StandardURIResolver resolver;
    CompoundURI curi1;
    
    public void setUp() throws Exception {
        resolver = StandardURIResolver.getInstance( getContext() );
        curi1 = new CompoundURI("s:x-{p1}-{p2}+p1=abc+p2=def");
    }
    
    public void testStringResourceParameterReplacement() {
        assertEquals("x-abc-def", resolver.dereference( curi1 ).asString() );
    }
}
