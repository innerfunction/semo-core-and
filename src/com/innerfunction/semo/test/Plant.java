package com.innerfunction.semo.test;

import java.util.List;

public class Plant extends Thing {

    List<Thing> contains;
    
    public void setContains(List<Thing> contains) {
        this.contains = contains;
    }
    
    public List<Thing> getContains() {
        return contains;
    }
    
}
