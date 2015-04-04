package com.innerfunction.semo;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

//import com.innerfunction.semo.Configuration.ValueType;



import com.innerfunction.uri.Resource;

import android.annotation.SuppressLint;
import android.util.Log;

/**
 * A container for named objects and services.
 * Acts as an object factory and IOC container. Objects built using this class are
 * instantiated and configured using an object definition read from a JSON configuration.
 * The object's properties may be configured using other built objects, or using references
 * to named objects contained by the container.
 * @author juliangoacher
 *
 */
public class Container implements Service, Configurable {

    static final String Tag = Container.class.getSimpleName();
    
    /** The map of named objects. */
    protected Map<String,Object> named = new HashMap<String,Object>();
    /**
     * A list of services recognized by this container.
     * Any object instantiated by this container that implements the Service interface
     * will be added to this list.
     */
    private List<Service> services = new ArrayList<Service>();
    /**
     * A set of type names mapped to class names.
     */
    private Configuration types = Configuration.EmptyConfiguration;
    /** Flag indicating whether the container's services are running. */
    private boolean running;
    
    /** Get a named object. */
    public Object getNamed(String name) {
        return named.get( name );
    }
    
    /** Set the object type mappings. */
    public void setTypes(Configuration types) {
        this.types = types == null ? Configuration.EmptyConfiguration : types;
    }
    
    /**
     * Build an object from its definition.
     * Instantiates and configures the object.
     * @param definition    The object definition.
     * @param id            A string for identifying the object instance in log output.
     * @return The new object instance, or null if it can't be instantiated.
     */
    public Object makeObject(Configuration definition, String id) {
        definition = definition.normalize();
        Object instance = initObject( definition, id );
        if( instance != null ) {
            configureObject( instance, definition, id );
        }
        return instance;
    }
    
    /**
     * Instantiate an object from its definition.
     * @param definition    The object definition. Must include a "type" field.
     * @param id            A string for identifying the object instance in log output.
     * @return The new object instance, or null if it can't be instantiated.
     */
    protected Object initObject(Configuration definition, String id) {
        String type = definition.getValueAsString("type");
        Object instance = null;
        if( type != null ) {
            String className = types.getValueAsString( type );
            if( className != null ) {
                try {
                    instance = newInstanceForClass( className );
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
    
    /**
     * Return a new class instance.
     * @throws ClassNotFoundException 
     * @throws IllegalAccessException 
     * @throws InstantiationException 
     */
    protected Object newInstanceForClass(String className) throws InstantiationException, IllegalAccessException, ClassNotFoundException {
        return Class.forName( className ).newInstance();
    }
    
    /**
     * Configure an object instance using its definition.
     * If the object implements the Configurable interface then its configure() method will be called.
     * Otherwise, the method will attempt to call a setXxx method for each property named xxx in the
     * definition.
     * If the object implements the IOCConfigurable interface then the beforeConfigure() and afterConfigure()
     * method will be called immediately before and after configuration has taken place.
     * @param instance      The object to be configured.
     * @param definition    The object's definition.
     * @param id            A string for identifying the object instance in log output.
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    @SuppressLint("DefaultLocale")
    public void configureObject(Object instance, Configuration definition, String id) {
        if( instance instanceof Configurable ) {
            ((Configurable)instance).configure( definition );
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
                    Class[] argTypes = method.getParameterTypes();
                    if( argTypes.length == 1 ) {
                        String baseName = methodName.substring( 3 );
                        String propName = baseName.substring( 0, 1 ).toLowerCase()+baseName.substring( 1 );
                        methods.put( propName, method );
                    }
                }
            }
            for( String name : definition.getValueNames() ) {
                if( name.startsWith("ios:") || name.equals("type") || name.equals("extends") || name.equals("config") ) {
                    continue; // Skip names starting with ios:, or configuration reserved words 
                }
                if( name.startsWith("and:") || name.startsWith("obj:") ) {
                    // Strip and: or obj: prefix from names
                    // The obj: prefix can be used to map properties using configuration reserved words, e.g. obj:type
                    name = name.substring( 4 );
                }
                try {
                    Method method = methods.get( name );
                    if( method == null ) {
                        continue;
                    }
                    Class<?> propType = method.getParameterTypes()[0];
                    if( propType == Boolean.class ) {
                        method.invoke( instance,  definition.getValueAsBoolean( name ) );
                    }
                    else if( Number.class.isAssignableFrom( propType ) ) {
                        Number value = definition.getValueAsNumber( name );
                        if( propType == Integer.class ) {
                            method.invoke( instance, value.intValue() );
                        }
                        else if( propType == Float.class ) {
                            method.invoke( instance, value.floatValue() );
                        }
                        else if( propType == Double.class ) {
                            method.invoke( instance, value.doubleValue() );
                        }
                        else {
                            method.invoke( instance, value );
                        }
                    }
                    else if( propType.isAssignableFrom( String.class ) ) {
                        method.invoke( instance, definition.getValueAsString( name ) );
                    }
                    else if( propType.isAssignableFrom( List.class ) ) {
                        List<Configuration> configs = definition.getValueAsConfigurationList( name );
                        if( configs != null ) {
                            List instances = new ArrayList( configs.size() );
                            for(Configuration config : configs ) {
                                instances.add( makeObject( config, name ) );
                            }
                            method.invoke( instance, instances );
                        }
                    }
                    else if( propType.isAssignableFrom( Map.class ) ) {
                        Map<String,Configuration> configs = definition.getValueAsConfigurationMap( name );
                        if( configs != null ) {
                            Map<String,Object> instances = new HashMap<String,Object>( configs.size() );
                            for(String iname : configs.keySet() ) {
                                Configuration iconfig = configs.get( iname );
                                Object obj = makeObject( iconfig, iname );
                                if( obj != null ) {
                                    instances.put( iname, obj );
                                }
                            }
                            method.invoke( instance, instances );
                        }
                    }
                    else if( propType.isAssignableFrom( Resource.class ) ) {
                        Resource rsc = definition.getValueAsResource( name );
                        method.invoke( instance, rsc );
                    }
                    else {
                        // General case - map an object value to an object property.
                        Configuration config = definition.getValueAsConfiguration( name );
                        // If the object property is of type Configuration then just pass the current
                        // configuration to it.
                        if( propType.isAssignableFrom( Configuration.class ) ) {
                            method.invoke( instance, config );
                        }
                        else {
                            // Otherwise try instantiating a new object using the config...
                            Object obj = makeObject( config, name );
                            // ...and assigning it to the object property.
                            if( propType.isAssignableFrom( obj.getClass() ) ) {
                                method.invoke( instance, obj );
                            }
                        }
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
        // If the object instance is a service then add to the list of services, and start the
        // service if the container services are running.
        if( instance instanceof Service ) {
            Service service = (Service)instance;
            services.add( service );
            if( running ) {
                service.startService();
            }
        }
    }
    
    /**
     * Configure this container.
     * @param configuration The container configuration. If this has a "named" or "names"
     *                      property then the container will attempt to build an object for
     *                      each top-level property, and to add that object to its set of
     *                      named objects.
     */
    @Override
    public void configure(Configuration configuration) {
        // Add named objects.
        Configuration namedConfig = configuration.getValueAsConfiguration("named");
        if( namedConfig == null ) {
            namedConfig = configuration.getValueAsConfiguration("names");
        }
        if( namedConfig != null ) {
            List<String> names = namedConfig.getValueNames();
            Map<String,Configuration> definitions = new HashMap<String,Configuration>();
            // Initialize named objects.
            for(String name : names) {
                Configuration definition = namedConfig.getValueAsConfiguration( name );
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
    }

    /**
     * Start the container service.
     * Starts all services contained by the container.
     */
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

    /**
     * Stop the container service.
     * Stops all services contained by the container.
     */
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
