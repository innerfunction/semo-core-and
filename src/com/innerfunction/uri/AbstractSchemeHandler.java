package com.innerfunction.uri;

import java.util.Map;

public abstract class AbstractSchemeHandler implements SchemeHandler {

    @Override
    public CompoundURI resolve(CompoundURI uri, CompoundURI context) {
        return uri;
    }

    @Override
    public abstract Resource dereference(CompoundURI uri, Map<String, Resource> params, Resource parent);

}
