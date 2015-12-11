package com.innerfunction.uri;

import android.content.Context;

import java.util.HashMap;
import java.util.Map;

import com.innerfunction.util.StringTemplate;

public class StringSchemeHandler extends AbstractSchemeHandler {

    private Context context;

    public StringSchemeHandler(Context context) {
        this.context = context;
    }

    @Override
    public Resource dereference(CompoundURI uri, Map<String, Resource> params, Resource parent) {
        String value = uri.getName();
        if( params.size() > 0 ) {
            // The URI name is treated as a string template to be populated with the parameter values.
            Map<String,String> _params = new HashMap<String,String>();
            for( String name : params.keySet() ) {
                _params.put( name, params.get( name ).asString() );
            }
            value = StringTemplate.render( value, _params );
        }
        return new Resource( this.context, value, uri, parent );
    }
}
