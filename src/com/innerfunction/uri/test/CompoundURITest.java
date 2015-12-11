package com.innerfunction.uri.test;

import java.util.HashMap;
import java.util.Map;

import com.innerfunction.uri.CompoundURI;

import android.test.AndroidTestCase;
import android.util.Log;

public class CompoundURITest extends AndroidTestCase {

    String uri="s:name+p1@[s:p2+np@[bang:x+p@3]]+p2@s:p2";
    String uri2="s:name+p3[s:p2]";
    CompoundURI cp1;
    CompoundURI cp2;
    
    Map<String,CompoundURI> params=new HashMap<String,CompoundURI>();;
    
    public void setUp() throws Exception {
        cp1 = new CompoundURI(uri);
        cp2 = new CompoundURI("s:x+p1=abc+p2=def");
    }
    
    public void testGenerics(){ 
        params.put(uri2, cp1);
        String Scheme,Name;
        Scheme=cp1.getScheme();
        Name=cp1.getName();
        assertEquals("s", Scheme);
        assertEquals("name", Name);
        assertEquals("s:p2+np@[bang:x+p@[s:3]]", cp1.getParameter("p1").toString());
        assertEquals("s:p2", cp1.getParameter("p2").toString());
        cp1.addParameters(params);
        Log.i("hue", cp1.getParameter("p1").toString());
        Log.i("hue", cp1.getParameter("p2").toString());
    }
    
    public void testParameterAssignmentOps() {
        assertEquals("s:abc", cp2.getParameter("p1").toString() );
        assertEquals("s:def", cp2.getParameter("p2").toString() );
        Log.i("hue", cp2.getParameter("p1").toString() );
    }
}
