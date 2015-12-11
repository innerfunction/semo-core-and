package com.innerfunction.uri.test;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;

import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.test.AndroidTestCase;

import com.innerfunction.uri.SchemeHandler;
import com.innerfunction.uri.StandardURIResolver;
import com.innerfunction.util.FileIO;

public class StandardURIResolverTest extends AndroidTestCase {

    StandardURIResolver sur;
    String uri="s:name#hue+p1@[s:p2+np@[bang:x+p@3]]+p2@s:p2";
    
    public void setUp() throws Exception {
        sur=StandardURIResolver.getInstance(getContext());
    }
    
    public void testAddHandler(){
        SchemeHandler handler = null;
        sur.addHandler("test", handler);
        assertEquals("[app, local, cache, s, test]",sur.getURISchemeNames().toString());
        assertTrue(sur.getURISchemeNames().size()==5);
    }
    
    public void testHasHandlerForURIScheme(){
        assertTrue(sur.hasHandlerForURIScheme("s"));
        assertTrue(sur.hasHandlerForURIScheme("app"));
        assertTrue(sur.hasHandlerForURIScheme("local"));
        assertTrue(sur.hasHandlerForURIScheme("cache"));
        assertTrue(sur.hasHandlerForURIScheme("test"));
    }
    
    public void testLocal() throws URISyntaxException{
        ApplicationInfo ainfo = getContext().getApplicationInfo();
        String prefsName = String.format("Eventpac.%s", ainfo.processName );
        SharedPreferences prefs= getContext().getSharedPreferences(prefsName, 0);
        SharedPreferences.Editor edit = prefs.edit();
        edit.putString("email", "info@innerfunction.com");
        edit.putString("name", "InnerFunction");
        edit.commit();
        assertEquals(prefs.getString("email", ""),sur.resolveURIFromString("local:email").toString());
        assertEquals(prefs.getString("name", ""),sur.resolveURIFromString("local:name").toString());
    }
    
    public void testCache() throws IOException, URISyntaxException{
        String outputDir=FileIO.getCacheDir(getContext()).toString()+"/temp.tmp";
        File outputFile = new File(outputDir);
        outputFile.createNewFile();
        FileIO.writeString( outputFile , "This is the temporary file content" );
        assertEquals("cache:%2Ftemp.tmp",sur.resolveURIFromString("cache:/temp.tmp").getURI().toString());
        assertEquals("cache", sur.resolveURIFromString("cache:/temp.tmp").getURI().getScheme());
        assertEquals("This is the temporary file content", sur.resolveURIFromString("cache:/temp.tmp").toString());
        assertEquals("/storage/emulated/0/Android/data/com.innerfunction.semo/files/Download/temp.tmp", sur.resolveURIFromString("cache:/temp.tmp").asDefault().toString());
        outputFile.delete();
    }
    
    public void testApp() throws IOException{
        assertNotNull(sur.resolveURIFromString("app:/configuration.json"));
        assertEquals("configuration.json", sur.resolveURIFromString("app:/configuration.json").asDefault().toString());
        assertEquals(FileIO.readString(getContext().getAssets().open("configuration.json"),"json" ,"UTF-8"),sur.resolveURIFromString("app:/configuration.json").toString());
    }
    
    public void tests(){
        assertEquals("name",sur.resolveURIFromString(uri).toString());
        assertEquals("s",sur.resolveURIFromString(uri).getURI().getScheme());
    }
}
