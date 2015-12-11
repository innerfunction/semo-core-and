package com.innerfunction.uri;

import java.util.Map;

public interface SchemeHandler {

    public CompoundURI resolve(CompoundURI uri, CompoundURI context);

    public Resource dereference(CompoundURI uri, Map<String,Resource> params, Resource parent);

}
