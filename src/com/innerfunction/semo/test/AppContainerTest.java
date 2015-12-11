package com.innerfunction.semo.test;

import java.util.List;
import java.util.Locale;
import java.util.Map;

import android.content.Context;
import android.content.res.Resources;
import android.test.AndroidTestCase;
import android.util.DisplayMetrics;
import android.util.Log;

import com.innerfunction.semo.AppContainer;

public class AppContainerTest extends AndroidTestCase{

    AppContainer container;
    Context context;

    public void setUp() throws Exception {
        context = getContext();
        container = AppContainer.getAppContainer(context);
        container.loadConfiguration("app:/configuration.json");
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
        assertEquals( 4, contains.size() );
        assertSame( container.getNamed("monkey"), contains.get( 0 ) );
        assertSame( container.getNamed("banana"), contains.get( 1 ) );
        Thing parrot = contains.get( 3 );
        assertEquals( parrot.getName(), "Parrot");
    }
    
    public void testJungle() {
        Forest jungle = (Forest)container.getNamed("jungle");
        Map<String,Thing> things = jungle.getThingsInTheForest();
        assertEquals(false, things.isEmpty());
        assertEquals(3, things.size());
        assertSame( container.getNamed("tree"), things.get("tree") );
        Thing jaguar = things.get("jaguar");
        assertEquals( jaguar.getName(), "Jaguar");
    }
    
    public void testGenerics(){
        //Tree.contains test
        Plant tree = (Plant)container.getNamed("tree");
        List<Thing> contains = tree.getContains();
        //assertSame( null, contains.get( 2 ) );
        //Forest.thingsInTheForest test
        Forest jungle = (Forest)container.getNamed("jungle");
        Map<String,Thing> things = jungle.getThingsInTheForest();
        assertEquals(2, things.size());
        assertFalse(things.containsKey("red"));
    }
    
    public void testAndroidContext(){
        Color yellow= (Color)container.getNamed("yellow");
        assertTrue(yellow.hasContext());
    }
    
    public void testIOCConfigurableInterface(){
        IOCConfigurableImplementation ioconfig = (IOCConfigurableImplementation)container.getNamed("iocconfigurable");
        assertTrue(ioconfig.beforeConfigureCalled);
        assertTrue(ioconfig.afterConfigureCalled);
    }
    
    public void testConfigurableInteface(){
        ConfigurableImplementation configurable= (ConfigurableImplementation)container.getNamed("ConfigurableImplementation");
        assertEquals(configurable.value, "two");
        assertTrue(configurable.config);
    }
    
    public void testAndroidContextMapped(){
        ContextTest androidContext=(ContextTest)container.getNamed("contextTest");
        assertSame(context, androidContext.getContext());
        Log.i("hue", getContext().toString());
        Log.i("hue", androidContext.getContext().toString());
    }
    
    public void testTemplateSunstitution(){
        Resources r = getContext().getResources();
        DisplayMetrics dm = r.getDisplayMetrics();
        Locale locale = r.getConfiguration().locale;
        String density;
        switch( dm.densityDpi ) {
            case DisplayMetrics.DENSITY_LOW:    density = "ldpi"; break;
            case DisplayMetrics.DENSITY_MEDIUM: density = "mdpi"; break;
            case DisplayMetrics.DENSITY_HIGH:   density = "hdpi"; break;
            case DisplayMetrics.DENSITY_XHIGH:  density = "xhdpi"; break;
            case DisplayMetrics.DENSITY_XXHIGH: density = "xxhdpi"; break;
            default:                            density = "hdpi";
        }
        //Platform Values
        Substitutions display=(Substitutions)container.getNamed("display");
        assertEquals(density,display.getValue());
        Substitutions defaultDisplay=(Substitutions)container.getNamed("defaultDisplay");
        assertEquals("hdpi", defaultDisplay.getValue());
        Substitutions platformName =(Substitutions)container.getNamed("platformName");
        assertEquals("and-"+density, platformName.getValue()+"-"+display.getValue());
        //Locale Values
        Substitutions id = (Substitutions)container.getNamed("id");
        assertEquals(locale.toString(), id.getValue());
        Substitutions lang = (Substitutions)container.getNamed("lang");
        assertEquals(locale.getLanguage(), lang.getValue());
        Substitutions variant = (Substitutions)container.getNamed("variant");
        assertEquals(locale.getVariant(), variant.getValue());
        
    }
}