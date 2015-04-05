package com.innerfunction.semo.test;

import java.util.Map;

public class Forest extends Thing {

    Map<String,Thing> thingsInTheForest;
    
    public void setThingsInTheForest(Map<String,Thing> things) {
        this.thingsInTheForest = things;
    }
    
    public Map<String,Thing> getThingsInTheForest() {
        return thingsInTheForest;
    }
}
