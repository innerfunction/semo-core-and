package com.innerfunction.semo.test;

import java.util.List;
import java.util.Map;

import android.content.Context;
import android.test.AndroidTestCase;

import com.innerfunction.semo.AppContainer;

public class AppContainerTest extends AndroidTestCase {

    AppContainer container;
    Context context;

    public void setUp() throws Exception {
        context = getContext();
        container = AppContainer.getAppContainer(context);
        container.init("app:/configuration.json");
    }

    public void testMonkey() {
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
    
    public void testTree() {
        Plant tree = (Plant)container.getNamed("tree");
        List<Thing> contains = tree.getContains();
        assertEquals( 3, contains.size() );
        assertSame( container.getNamed("monkey"), contains.get( 0 ) );
        assertSame( container.getNamed("banana"), contains.get( 1 ) );
        Thing parrot = contains.get( 2 );
        assertEquals( parrot.getName(), "Parrot");
    }
    
    public void testJungle() {
        Forest jungle = (Forest)container.getNamed("jungle");
        Map<String,Thing> things = jungle.getThingsInTheForest();
        assertSame( container.getNamed("tree"), things.get("tree") );
        Thing jaguar = things.get("jaguar");
        assertEquals( jaguar.getName(), "Jaguar");
    }
}
