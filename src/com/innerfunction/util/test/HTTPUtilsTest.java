package com.innerfunction.util.test;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;
import com.innerfunction.util.HTTPUtils;
import com.innerfunction.util.HTTPUtils.FileRequestCallback;
import com.innerfunction.util.HTTPUtils.JSONRequestCallback;
import android.test.AndroidTestCase;
import android.util.Log;

public class HTTPUtilsTest extends AndroidTestCase {

    HTTPUtils http;
    String charset="UTF-8";
    URL url;
    JSONRequestCallback callbackjson = null;
    FileRequestCallback calbackfile;
    Map<String,Object> data;

    public void setUp() throws Exception {
        url = new URL("http://www.android.com/");
    }
    
    public void testgetConnectionCharset() throws IOException{
        assertTrue(charset.equals(http.getConnectionCharset((HttpURLConnection)url.openConnection())));
    }

    public void testgetJSON() throws MalformedURLException{
        http.getJSON("http://192.168.1.6/semo-server/package.json", callbackjson);
    }
    
    public void testpostJSON() throws MalformedURLException{
        http.postJSON("http://www.android.com/", data, callbackjson);
    }
    
    public void testgetFile(){
        //http.getFile(url, offset, file, calbackfile);
    }

}
