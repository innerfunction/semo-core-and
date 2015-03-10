package com.innerfunction.semo.core;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

/**
 * A Map that returns localized strings for resource ID keys.
 * Intended for use with StringTemplate, to provide localized values within templates.
 * @author juliangoacher
 *
 */
public class I18nMap implements Map<String, String> {

    private Core core;
    
    public I18nMap(Core core) {
        this.core = core;
    }
    
    @Override
    public void clear() {
        // Noop.
    }

    @Override
    public boolean containsKey(Object key) {
        return core.getLocalizedString( (String)key ) != null;
    }

    @Override
    public boolean containsValue(Object value) {
        return false;
    }

    @Override
    public Set<java.util.Map.Entry<String, String>> entrySet() {
        return null;
    }

    @Override
    public String get(Object key) {
        String s = core.getLocalizedString( (String)key );
        return s == null ? key.toString() : s;
    }

    @Override
    public boolean isEmpty() {
        return false;
    }

    @Override
    public Set<String> keySet() {
        return null;
    }

    @Override
    public String put(String key, String value) {
        return null;
    }

    @Override
    public void putAll(Map<? extends String, ? extends String> arg0) {
        // Noop
    }

    @Override
    public String remove(Object key) {
        return null;
    }

    @Override
    public int size() {
        return 0;
    }

    @Override
    public Collection<String> values() {
        return null;
    }

}
