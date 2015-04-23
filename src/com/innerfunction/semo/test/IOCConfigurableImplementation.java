package com.innerfunction.semo.test;

import android.util.Log;

import com.innerfunction.semo.Container;
import com.innerfunction.semo.IOCConfigurable;

public class IOCConfigurableImplementation implements IOCConfigurable{

    public String value;
    public boolean beforeConfigureCalled = false;
    public boolean afterConfigureCalled = false;
    
    @Override
    public void beforeConfigure(Container container) {
        beforeConfigureCalled = (value == null);
    }
    
    @Override
    public void afterConfigure(Container container) {
        afterConfigureCalled = (value != null);
    }

    public void setValue(String value) {
        this.value = value;
    }
}
