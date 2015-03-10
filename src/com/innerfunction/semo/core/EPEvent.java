package com.innerfunction.semo.core;

import java.util.Map;

import android.content.Context;

import com.innerfunction.uri.CompoundURI;
import com.innerfunction.uri.Resource;

public class EPEvent extends Resource {

    private Resource parent;
    private String name;
    private String action;
    private Map<String, Resource> arguments;
    
    public EPEvent(Context context, Object item, CompoundURI uri, Resource parent, Map<String, Resource> args) {
        super( context, item, uri, parent );
        this.parent = parent;
        this.name = uri.getName();
        this.action = uri.getFragment();
        this.arguments = args;
    }

    public String getName() {
        return name;
    }
    
    public String getAction() {
        return action;
    }

    public Map<String,Resource> getArguments() {
        return this.arguments;
    }
    
    public String getArgumentAsString(String name) {
        Resource r = this.arguments.get( name );
        return r != null ? r.asString() : null;
    }

    // NOTE: This method may be obsolete now that the event:name#action format is being used.
    public EPEvent copyWithName(String name) {
        EPEvent event = new EPEvent( context, null, uri, parent, arguments );
        event.name = name;
        return event;
    }
    
    @Override
    public String toString() {
        String s = super.toString();
        return s != null ? s : uri.toString();
    }
}
