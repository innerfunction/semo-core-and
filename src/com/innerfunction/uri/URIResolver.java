package com.innerfunction.uri;

public interface URIResolver {

    public Resource dereference(String uri);
    
    public Resource dereference(String uri, Resource context);
    
    public Resource dereference(CompoundURI uri);
    
    public Resource dereference(CompoundURI uri, Resource context);

}
