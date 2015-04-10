package com.innerfunction.semo.test;

import com.innerfunction.semo.Configurable;
import com.innerfunction.semo.Configuration;

public class ConfigurableImplementation implements Configurable{
    
    public String value;
    public boolean config=false;
    
    public void setValue(String value) {
        this.value = value;
    }
    
    @Override
    public void configure(Configuration configuration) {
        value=configuration.getValueAsString("value2");
        config=true;
    }
    
}
