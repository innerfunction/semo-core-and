package com.innerfunction.semo.test;

import android.util.Log;

import com.innerfunction.semo.IOCConfigurable;

public class IOCConfigurableImplementation implements IOCConfigurable{

    public String value;
    public boolean beforeConfigureCalled = false;
    public boolean afterConfigureCalled = false;
    
    @Override
    public void beforeConfigure() {
        beforeConfigureCalled = (value == null);
    }
    
    @Override
    public void afterConfigure() {
        afterConfigureCalled = (value != null);
    }

    public void setValue(String value) {
        this.value = value;
    }
}
