package com.innerfunction.semo;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.innerfunction.semo.Configuration.ValueType;

import android.annotation.SuppressLint;
import android.util.Log;

// Basic operation:
// * Register named: uri scheme mapped to named map
// * When configured, all top level properties under "names" are configs for named objects
// * So instantiate all named objects, adding to named map
// * Then configure all named objects (as Configurable, IOCConfigurables)
// * All objects are constructed through this factory
// * All Service instances constructed through this factory are added to the services list
// * So when startService is called, all services are started
public class Container implements Service {

    static final String Tag = Container.class.getSimpleName();
    
    protected Map<String,Object> named = new HashMap<String,Object>();
    private List<Service> services = new ArrayList<Service>();
    private Configuration types;
    private boolean running;
    
    public Object getNamed(String name) {
        return named.get( name );
    }
    
    public void setTypes(Configuration types) {
        this.types = types;
    }
    
    public Object makeObject(Configuration definition, String id) {
        definition = definition.normalize();
        Object instance = initObject( definition, id );
        if( instance != null ) {
            configureObject( instance, definition, id );
        }
        return instance;
    }
    
    public Object initObject(Configuration definition, String id) {
        String type = definition.getValueAsString("type");
        Object instance = null;
        if( type != null ) {
            String className = types.getValueAsString( type );
            if( className != null ) {
                try {
                    instance = Class.forName( className ).newInstance();
                }
                catch(InstantiationException e) {
                    Log.e( Tag, String.format("Make %s: Error instantiating class %s", id, className ), e );
                }
                catch(IllegalAccessException e) {
                    Log.e( Tag, String.format("Make %s: Unable to instantiating class %s", id, className ), e );
                }
                catch(ClassNotFoundException e) {
                    Log.e( Tag, String.format("Make %s: Class %s not found", id, className ), e );
                }
            }
            else {
                Log.w( Tag, String.format("Make %s: No class name found for type %s", id, type ));
            }
        }
        else {
            Log.w( Tag, String.format("Make %s: Component configuration missing 'type' property", id ));
        }
        return instance;
    }
    
    @SuppressLint("DefaultLocale")
    public void configureObject(Object instance, Configuration config, String id) {
        if( instance instanceof Configurable ) {
            ((Configurable)instance).configure( config );
        }
        else {
            if( instance instanceof IOCConfigurable ) {
                ((IOCConfigurable)instance).beforeConfigure();
            }
            Class<?> cl = instance.getClass();
            Map<String,Method> methods = new HashMap<String,Method>();
            // Generate a map of set methods keyed by name (but setXXX -> xXX)
            for( Method method : cl.getMethods() ) {
                String methodName = method.getName(); 
                if( methodName.startsWith("set") ) {
                    @SuppressWarnings("rawtypes")
                    Class[] argTypes = method.getParameterTypes();
                    if( argTypes.length == 1 ) {
                        String propName = methodName.substring( 0, 1 ).toLowerCase()+methodName.substring( 1 );
                        methods.put( propName, method );
                    }
                }
            }
            for( String name : config.getValueNames() ) {
                try {
                    ValueType type = config.getValueType( name );
                    Method method = methods.get( name );
                    Class<?> propType = method.getParameterTypes()[0];
                    switch( type ) {
                    case Number:
                        if( propType.isAssignableFrom( Number.class ) ) {
                            method.invoke( instance, config.getValueAsNumber( name ) );
                        }
                        break;
                    case String:
                        if( propType.isAssignableFrom( String.class ) ) {
                            method.invoke( instance, config.getValueAsString( name ) );
                        }
                        break;
                    case Object:
                        Configuration valueConfig = config.getValueAsConfiguration( name );
                        if( propType.isAssignableFrom( Configuration.class ) ) {
                            method.invoke( instance, valueConfig );
                            break;
                        }
                        Object value = makeObject( valueConfig, name );
                        if( propType.isAssignableFrom( value.getClass() ) ) {
                            method.invoke( instance, value );
                            break;
                        }
                        break;
                    case Undefined:
                    default:
                        break;
                    }
                }
                catch(Exception e) {
                    Log.e(Tag,String.format("Configuring %s", name ) );
                }
            }
            if( instance instanceof IOCConfigurable ) {
                ((IOCConfigurable)instance).afterConfigure();
            }
        }
        if( instance instanceof Service ) {
            Service service = (Service)instance;
            services.add( service );
            if( running ) {
                service.startService();
            }
        }
    }
    
    @Override
    public void configure(Configuration configuration) {
        List<String> names = configuration.getValueNames();
        Map<String,Configuration> definitions = new HashMap<String,Configuration>();
        // Initialize named objects.
        for(String name : names) {
            Configuration definition = configuration.getValueAsConfiguration( name );
            Object instance = initObject( definition, name );
            if( instance != null ) {
                named.put( name, instance );
                definitions.put( name, definition );
            }
        }
        // Configure named objects.
        for(String name : names) {
            Object instance = named.get( name );
            if( instance != null ) {
                Configuration definition = definitions.get( name );
                configureObject( instance, definition, name );
            }
        }
    }

    @Override
    public void startService() {
        running = true;
        for( Service service : services ) {
            try {
                service.startService();
            }
            catch(Exception e) {
                Log.e(Tag, "Starting services", e );
            }
        }
    }

    @Override
    public void stopService() {
        for( Service service : services ) {
            try {
                service.stopService();
            }
            catch(Exception e) {
                Log.e(Tag, "Stopping services", e );
            }
        }
        running = false;
    }

}
