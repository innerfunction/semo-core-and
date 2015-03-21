package com.innerfunction.util;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.Map;

import org.json.simple.parser.ParseException;

import com.innerfunction.util.BackgroundTaskRunner.Task;

import android.annotation.SuppressLint;
import android.util.Log;

/**
 * Utility methods for download data over HTTP.
 * @author juliangoacher
 *
 */
@SuppressLint("NewApi")
public class HTTPUtils {

    static final String Tag = HTTPUtils.class.getSimpleName();

    /**
     * Read the character encoding of a HTTP response.
     * @param connection
     * @return
     */
    public static String getConnectionCharset(HttpURLConnection connection) {
        // Attempt to read the download's character encoding.
        String charset = "UTF-8"; // Default fallback, if charset not found in response.
        String contentType = connection.getContentType();
        if( contentType != null ) {
            String[] matches = Regex.matches("charset=([^;]+)", contentType );
            if( matches != null && matches.length > 1 ) {
                charset = matches[1];
            }
        }
        return charset;
    }

    /** Callback for the getJSON method. */
    public static interface JSONRequestCallback {
        
        /** Method for passing JSON response. */
        public void receivedJSON(Map<String,Object> json);
    }
    
    /** Get JSON from the specified URL. */
    public static void getJSON(final String url, final JSONRequestCallback callback) throws MalformedURLException {
        final URL _url = new URL( url );
        // Execute the request on a background thread.
        BackgroundTaskRunner.run(new Task() {
            @SuppressWarnings("unchecked")
            @Override
            public void run() {
                Map<String,Object> json = null;
                InputStream in = null;
                HttpURLConnection connection = null;
                try {
                    connection = (HttpURLConnection)_url.openConnection();
                    connection.setRequestProperty("Accepts","application/json");
                    in = connection.getInputStream();
                    String charset = HTTPUtils.getConnectionCharset( connection );
                    json = (Map<String,Object>)FileIO.readJSON( in, url, charset );
                }
                catch(IOException e) {
                    Log.e( Tag, String.format("Downloading from %s", url ), e );
                }
                catch(ParseException e) {
                    Log.e( Tag, String.format("Parsing JSON from %s", url ), e );
                }
                finally {
                    try {
                        connection.disconnect();
                        in.close();
                    }
                    catch(Exception e) {}
                    callback.receivedJSON( json );
                }
            }
        });
    }

    /** Post data to the specified URL and receive a JSON response. */
    public static void postJSON(final String url, final Map<String,Object> data, final JSONRequestCallback callback) throws MalformedURLException {
        final URL _url = new URL( url );
        // Execute the request on a background thread.
        BackgroundTaskRunner.run(new Task() {
            @SuppressWarnings("unchecked")
            @Override
            public void run() {
                Map<String,Object> json = null;
                InputStream in = null;
                HttpURLConnection connection = null;
                try {
                    connection = (HttpURLConnection)_url.openConnection();
                    // Enable a POST request
                    connection.setDoOutput( true );
                    connection.setChunkedStreamingMode( 0 );
                    // Set request headers.
                    connection.setRequestProperty("Accepts","application/json");
                    connection.setRequestProperty("Content-Type","UTF-8");
                    // Write the post body.
                    if( data != null ) {
                        Charset charset = Charset.forName("UTF-8");
                        BufferedOutputStream bos = new BufferedOutputStream( connection.getOutputStream() );
                        String token;
                        for( String name : data.keySet() ) {
                            token = URLEncoder.encode( name, "UTF-8");
                            bos.write( token.getBytes( charset ) );
                            bos.write('=');
                            token = URLEncoder.encode( data.get( name ).toString(), "UTF-8");
                            bos.write( token.getBytes( charset ) );
                        }
                        bos.write( "\n\n".getBytes( charset ) );
                        bos.flush();
                    }
                    // Read the response.
                    in = connection.getInputStream();
                    String respCharset = HTTPUtils.getConnectionCharset( connection );
                    json = (Map<String,Object>)FileIO.readJSON( in, url, respCharset );
                }
                catch(IOException e) {
                    Log.e( Tag, String.format("Downloading from %s", url ), e );
                }
                catch(ParseException e) {
                    Log.e( Tag, String.format("Parsing JSON from %s", url ), e );
                }
                finally {
                    try {
                        connection.disconnect();
                        in.close();
                    }
                    catch(Exception e) {}
                    callback.receivedJSON( json );
                }
            }
        });
    }
    
    /** Callback for the getFile method. */
    public static interface FileRequestCallback {
        
        /** Method for passing a file response. */
        public void receivedFile(File file);
        
    }
    
    /** 
     * Get data from the specified URL and write to file.
     * @param url       The URL to GET.
     * @param offset    The offset to request data from.
     * @param file      The file to write the data response to.
     * @param callback
     * @throws MalformedURLException
     */
    public static void getFile(final String url, final long offset, final File file, final FileRequestCallback callback) throws MalformedURLException {
        final URL _url = new URL( url );
        // Execute the request on a background thread.
        BackgroundTaskRunner.run(new Task() {
            @Override
            public void run() {
                InputStream in = null;
                HttpURLConnection connection = null;
                try {
                    connection = (HttpURLConnection)_url.openConnection();
                    boolean append = false;
                    if( offset > 0 ) {
                        connection.setRequestProperty("Range", String.format("%d-", offset ));
                        append = true;
                    }
                    in = connection.getInputStream();
                    if( !FileIO.writeData( file, in, append ) ) {
                        Log.w(Tag, String.format("Failed to write data to %s", file.getAbsolutePath()));
                    }
                }
                catch(IOException e) {
                    Log.e( Tag, String.format("Downloading from %s", url ), e );
                }
                finally {
                    try {
                        connection.disconnect();
                        in.close();
                    }
                    catch(Exception e) {}
                    callback.receivedFile( file );
                }
            }
        });
    }

}
