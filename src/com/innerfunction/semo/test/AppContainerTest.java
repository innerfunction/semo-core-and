package com.innerfunction.semo.test;

import java.io.IOException;
import java.net.URISyntaxException;
import android.content.Context;
import android.test.AndroidTestCase;
import android.util.Log;
import com.innerfunction.semo.AppContainer;

public class AppContainerTest extends AndroidTestCase {

    AppContainer container;
    Context context;

    public void setUp() throws Exception {
        context = getContext();
        container = AppContainer.getAppContainer(context);
    }

    public void testPrueba() throws URISyntaxException, IOException{
        container.init("app:/configuration.json");
        Animal monkey =(Animal)container.getNamed("monkey");
        Fruit banana =(Fruit)container.getNamed("banana");
        Color yellow =(Color)container.getNamed("yellow");
        assert(monkey.getLikes()=="banana");
        assert(banana.getColor()=="yellow");
    }
}
