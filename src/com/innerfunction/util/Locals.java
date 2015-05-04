// TODO: Move to com.innerfunction.util
package com.innerfunction.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.json.simple.JSONValue;

import android.content.SharedPreferences;

/**
 * Maintain a set of values within a defined namespace in local storage.
 * @author juliangoacher
 *
 */
public class Locals {

    private String namespacePrefix;
    private SharedPreferences preferences;

    public Locals(String namespacePrefix) {
        this.namespacePrefix = namespacePrefix+".";
    }
    
    public Locals() {
        this.namespacePrefix = "";
    }
    
    public Locals(@SuppressWarnings("rawtypes") Class c) {
        this( c.getName() );
    }
    
    public void setValues(Map<String,Object> values, boolean forceReset) {
        SharedPreferences.Editor editor = preferences.edit();
        for( String name : values.keySet() ) {
            String key = getKey( name );
            if( !preferences.contains( key ) || forceReset ) {
                Object value = values.get( key );
                if( value instanceof String ) {
                    editor.putString( key, (String)value );
                }
                else if( value instanceof Boolean ) {
                    editor.putBoolean( key, (Boolean)value );
                }
                else if( value instanceof Number ) {
                    editor.putFloat( key, ((Number)value).floatValue() );
                }
            }
        }
        editor.commit();
    }
    
    public Object getJSON(String name) {
        String json = getString( name );
        return json != null ? JSONValue.parse( json ) : null;
    }
    
    public void setJSON(String name, Object data) {
        String json = data != null ? JSONValue.toJSONString( data ) : null;
        setString( name, json );
    }
    
    public String getString(String name) {
        return getString( name, null );
    }
    
    public String getString(String name, String defValue) {
        return preferences.getString( getKey( name ), defValue );
    }
    
    public String setString(String name, String value) {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString( getKey( name ), value );
        editor.commit();
        return value;
    }
    
    public int getInt(String name) {
        return getInt( name, -1 );
    }
    
    public int getInt(String name, int defValue) {
        return preferences.getInt( getKey( name ), defValue );
    }
    
    public int setInt(String name, int value) {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putInt( getKey( name ), value );
        editor.commit();
        return value;
    }
    
    public boolean getBoolean(String name) {
        return getBoolean( name, false );
    }
    
    public boolean getBoolean(String name, boolean defValue ) {
        return preferences.getBoolean( name, defValue );
    }
    
    public boolean setBoolean(String name, boolean value) {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean( getKey( name ), value );
        editor.commit();
        return value;
    }
    
    public void remove(String... names) {
        SharedPreferences.Editor editor = preferences.edit();
        for( String name : names ) {
            editor.remove( name );
        }
        editor.commit();
    }
    
    public void removeAll() {
        List<String> names = new ArrayList<String>();
        for( String name : preferences.getAll().keySet() ) {
            if( name.startsWith( namespacePrefix ) ) {
                names.add( name );
            }
        }
        if( names.size() > 0 ) {
            remove( names.toArray( new String[names.size()] ) );
        }
    }
    
    private String getKey(String name) {
        return namespacePrefix+name;
    }
}
