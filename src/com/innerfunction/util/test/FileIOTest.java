package com.innerfunction.util.test;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import junit.framework.AssertionFailedError;

import com.innerfunction.util.FileIO;

import android.content.Context;
import android.content.res.AssetManager;
import android.test.AndroidTestCase;
import android.util.Log;

public class FileIOTest extends AndroidTestCase {
    
    Context context;
    File file;
    String test,encoding,name;
    InputStream is,is2;

    public void testFileIO() throws FileNotFoundException{
        context = getContext();
        name="";
        file = new File(context.getFilesDir(),name+"testfile.txt");
        test= "Hello World";
        //encoding="edrg";
        encoding="UTF-8";
        
        //Create a File if no exist.
        if (!file.exists()){
            Log.i("SCT","File exist: No");
            try{
                if (file.createNewFile()){
                    assertTrue(true);
                    Log.i("SCT","File create: Yes");
                } else {
                    Log.i("SCT","File create: No");
                    fail();
                }
            } catch (IOException ioe) {
                  ioe.printStackTrace();
                
                }
        }else{
            Log.i("SCT","File exist: Yes");
        }
        is = new FileInputStream(file);
        is2= new FileInputStream(file);
        
        
        
        Log.i("SCT ","Start TestFileIO");
        
        //TestFileNotFoundException
        Log.i("SCT ","Test fileNotFoundException: IN");
        try
        {
            File exception = new File ("ksdnfj");
            
            byte[] test1=FileIO.readData( exception );
            fail();
            Log.e("SCT","Expected FileNotFoundException to be thrown: NO");
            }
        catch(FileNotFoundException e)
        {

            assertTrue(true);
            Log.i("SCT ","Expected FileNotFoundException to be thrown: YES");

        }
        Log.i("SCT ","Test fileNotFoundException: OUT");
        
        //TestWriteString
        Log.i("SCT ","Test writeString: IN");
        assertEquals(true, FileIO.writeString( file , test ));
        Log.i("SCT ","Test writeString: OUT");
        
        //TestReadData with File
        Log.i("SCT ","Test readData: IN");
        byte[] testbyte=FileIO.readData(file);
        assertEquals(11, testbyte.length);
        Log.i("SCT ","Test readData: OUT");
        
        //TestReadData byte[] with InputStream
        Log.i("SCT ","Test readData with InputStream: IN");
        testbyte=FileIO.readData(is, name);
        assertEquals(11, testbyte.length);
        Log.i("SCT ","Test readData with InputStream: OUT");
        
        //TestReadString with FILE
        Log.i("SCT ","Test readString with File: IN");
        String teststring=FileIO.readString(file, encoding);
        assertEquals(test, teststring);
        Log.i("SCT ","Test readString with File: OUT");
        
        //TestReadString with InputStream
        Log.i("SCT ","Test readString with InputStream: IN");
        teststring=FileIO.readString(is2, name, encoding);
        assertEquals(test, teststring);
        
        try {
            is.close();
            is2.close();
            Log.i("SCT ","InputStreams closed");

        } catch (IOException e) {
            e.printStackTrace();
            Log.e("SCT","Error InputStreams close ");
        }
        file.delete();
        Log.i("SCT ","remove file");
        
        //END
        Log.i("SCT ","End TestFileIO");
        
    }


}
