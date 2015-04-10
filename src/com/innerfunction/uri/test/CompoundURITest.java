package com.innerfunction.uri.test;

import java.util.HashMap;
import java.util.Map;

import com.innerfunction.uri.CompoundURI;

import android.test.AndroidTestCase;
import android.util.Log;

public class CompoundURITest extends AndroidTestCase {

    String uri="s:name+p1@[s:p2+np@[bang:x+p@3]]+p2@s:p2";
    String uri2="s:name+p3[s:p2]";
    CompoundURI cpu;
    Map<String,CompoundURI> params=new HashMap<String,CompoundURI>();;
    
    public void setUp() throws Exception {
        cpu = new CompoundURI(uri);
    }
    
    public void testGenerics(){ 
        params.put(uri2, cpu);

        String Scheme,Name;
        Scheme=cpu.getScheme();
        Name=cpu.getName();
        assertEquals("s", Scheme);
        assertEquals("name", Name);
        assertEquals("s:p2+np@[bang:x+p@[s:3]]", cpu.getParameter("p1").toString());
        assertEquals("s:p2", cpu.getParameter("p2").toString());
        cpu.addParameters(params);
        Log.i("hue", cpu.getParameter("p1").toString());
        Log.i("hue", cpu.getParameter("p2").toString());

    }
}
