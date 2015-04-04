package com.innerfunction.semo.test;

import java.io.IOException;
import java.net.URISyntaxException;
import android.content.Context;
import android.test.AndroidTestCase;
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
        Animal monkey = (Animal)container.getNamed("monkey");
        Fruit banana = (Fruit)container.getNamed("banana");
        Color yellow = (Color)container.getNamed("yellow");
        assertNotNull( monkey );
        assertNotNull( banana );
        assertNotNull( yellow );
        assertSame( monkey.getLikes(), banana );
        assertSame( banana.getColor(), yellow );
        assertEquals( yellow.getValue(), "#00FFFF");
    }
}
