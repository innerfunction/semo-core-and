package com.innerfunction.semo;

import com.innerfunction.util.Locals;

/**
 * A version of Locals that is back by a Configuration instance.
 * The class first looks in local storage for named property values (prefixing property
 * names with the namespace prefix). If a value isn't found then it defaults to the
 * value of the named property in the associated configuration.
 * This allows easily handling of properties that are defined in the app configuration
 * but which can be modified by the app or user.
 * @author juliangoacher
 *
 */
public class ConfiguredLocals extends Locals {

    private Configuration configuration;
    
    public ConfiguredLocals(String namespacePrefix, Configuration config) {
        super( namespacePrefix );
        this.configuration = config;
    }
    
    @Override
    public String getString(String name, String defValue) {
        String configValue = configuration.getValueAsString( name, defValue );
        return super.getString( name, configValue );
    }
    
    @Override
    public int getInt(String name, int defValue) {
        int configValue = configuration.getValueAsNumber( name, Integer.valueOf( defValue ) ).intValue();
        return super.getInt( name, configValue );
    }
    
    @Override
    public boolean getBoolean(String name, boolean defValue) {
        boolean configValue = configuration.getValueAsBoolean( name, defValue );
        return super.getBoolean( name, configValue );
    }
}
