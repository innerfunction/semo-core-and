package com.innerfunction.uri.test;

import java.net.URISyntaxException;

import com.innerfunction.uri.CompoundURI;

import android.test.AndroidTestCase;

public class CompoundURITest extends AndroidTestCase {

    String uri="s:name#hue+p1@[s:p2+np@[bang:x+p@3]]+p2@s:p2";
    String urifail="hueheuheuheuheu";
    CompoundURI cpu;
    String Scheme, Name, Fragment;

    public void setUp() throws Exception {
        cpu = new CompoundURI(uri);
    }
    
    public void testGetScheme(){ 
        Scheme=cpu.getScheme();
        assertEquals("s", Scheme);
    }
    
    public void testGetName(){ 
        Name=cpu.getName();
        assertEquals("name", Name);
    }
    
    public void testGetFragment(){ 
        Fragment=cpu.getFragment();
        assertEquals("hue", Fragment);
    }
    
    public void testCopyOf(){
        assertEquals( cpu, cpu.copyOf());
        assertEquals( "s:name#hue.hue+p1@[s:p2+np@[bang:x+p@[s:3]]]+p2@[s:p2]" , cpu.copyOfWithFragment("hue").toString());
    }
    
    public void testGetParameters(){
        assertEquals("s:p2+np@[bang:x+p@[s:3]]", cpu.getParameter("p1").toString());
        assertEquals("s:p2", cpu.getParameter("p2").toString());
        assertEquals(2,cpu.getParameters().size());
    }
    
    public void testCanonicalForm(){
        assertEquals("s:name#hue+p1@[s:p2+np@[bang:x+p@[s:3]]]+p2@[s:p2]",cpu.canonicalForm());
    }
    
    public void testToString(){
        assertEquals("s:name#hue+p1@[s:p2+np@[bang:x+p@[s:3]]]+p2@[s:p2]",cpu.toString());
    }
    
    public void testHashCode(){
        assertNotNull( cpu.hashCode());
    }
    
    public void testException(){
        urifail="lakdfadufhnuiaiuansdu";
        try {
            cpu = new CompoundURI(urifail);
            } catch (URISyntaxException e) {
            assertTrue(true);
        }
    }
}
