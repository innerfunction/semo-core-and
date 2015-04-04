package com.innerfunction.util.test;

import android.test.AndroidTestCase;
import com.innerfunction.util.Paths;

public class PathsTesting extends AndroidTestCase {
	private Paths path;
    private String rute;
    
    public void setUp() throws Exception {
        rute="home/test/example.txt";
    }
    
    public void testParts() {
        String a[]=path.parts(rute);
        //System.out.print(a[1]);
        assertTrue("home".equals(a[0]));
        assertTrue("test".equals(a[1]));
        assertTrue("example.txt".equals(a[2]));
    }
    
    public void testDirname(){
        String a=path.dirname(rute);
        //System.out.print(a);
        assertTrue("home/test".equals(a));
    }
    
    public void testBaseName(){
        String a=path.basename(rute);
        assertTrue("example.txt".equals(a));
    }
    
    public void testExtName(){
        String a=path.extname(rute);
        //System.out.print(a);
        assertTrue(".txt".equals(a));
        }
    
    public void testStripExt(){
        String a=path.stripext(rute);
        //System.out.print(a);
        assertTrue("home/test/example".equals(a));
        }
    
    public void testJoin(){
        String rutes1="/home/test";
        String rutes2="example2.txt";
        String a=path.join(rutes1, rutes2);
        assertTrue("/home/test/example2.txt".equals(a));
    }
}
