package com.innerfunction.uri.test;

import java.net.URISyntaxException;

import com.innerfunction.uri.CompoundURI;
import com.innerfunction.uri.Resource;

import android.test.AndroidTestCase;
import android.util.Log;

public class ResourceTest extends AndroidTestCase {

    Resource resource;
    Resource context ;
    String uri, uri2;
    Resource resolve;
    CompoundURI cmp;
    
    public void setUp() throws Exception {
        uri="http://www.google.com";
    }
    
    public void testResolveURI() throws URISyntaxException{
        
        cmp=new CompoundURI(uri);
        resolve=resource.resolveURI(cmp);
        
        
    }
    
    public void testResolveURIFromString1(){
         resolve=resource.resolveURIFromString(uri, context);
         cmp=resolve.getURI();
         uri2=cmp.toString();
         Log.w("valor", uri2);
         assertEquals(uri,uri2);
    }
    
    public void testResolveURIFromString2(){
        
    }
}
