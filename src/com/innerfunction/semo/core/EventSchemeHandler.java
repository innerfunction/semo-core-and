package com.innerfunction.semo.core;

import java.util.Map;

import com.innerfunction.uri.AbstractSchemeHandler;
import com.innerfunction.uri.CompoundURI;
import com.innerfunction.uri.Resource;
import com.innerfunction.util.Configuration;

public class EventSchemeHandler extends AbstractSchemeHandler implements Component {

    @Override
    public Resource handle(CompoundURI uri, Map<String, Resource> params, Resource parent) {
        Core core = Core.getCore();
        return new EPEvent( core.getAndroidContext(), null, uri, parent, params );
    }

    @Override
    public void configure(Configuration configuration) {}

}
