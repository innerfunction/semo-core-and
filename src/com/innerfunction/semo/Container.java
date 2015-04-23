package com.innerfunction.semo;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

//import com.innerfunction.semo.Configuration.ValueType;








import com.innerfunction.uri.Resource;

import android.annotation.SuppressLint;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
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
     * @param configuration The object configuration.
     * @param id            A string for identifying the object instance in log output.
     * @return The new object instance, or null if it can't be instantiated.
     */
    public Object buildObject(Configuration configuration, String id) {
        configuration = configuration.normalize();
        Object object = instantiateObject( configuration, id );
        if( object != null ) {
            configureObject( object, configuration, id );
        }
        return object;
    }
    
    /**
     * Instantiate an object from its definition.
     * @param configuration The object configuration. Must include a "semo:type" field.
     * @param id            A string for identifying the object instance in log output.
     * @return The new object instance, or null if it can't be instantiated.
     */
    protected Object instantiateObject(Configuration configuration, String id) {
        Object object = null;
        String type = configuration.getValueAsString("semo:type");
        if( type != null ) {
            String className = types.getValueAsString( type );
            if( className != null ) {
                try {
                    object = newInstanceForClass( className );
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
            Log.w( Tag, String.format("Make %s: Component configuration missing 'semo:type' property", id ));
        }
        return object;
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
     * @param object        The object to be configured.
     * @param configuration The object's configuration.
     * @param id            A string for identifying the object instance in log output.
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    @SuppressLint("DefaultLocale")
    public void configureObject(Object object, Configuration configuration, String id) {
        if( object instanceof Configurable ) {
            ((Configurable)object).configure( configuration );
        }
        else {
            if( object instanceof IOCConfigurable ) {
                ((IOCConfigurable)object).beforeConfigure( this );
            }
            Class<?> cl = object.getClass();
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
            for( String name : configuration.getValueNames() ) {
                if( name.startsWith("ios:") || name.startsWith("semo:") ) {
                    continue; // Skip names starting with ios: or semo: 
                }
                if( name.startsWith("and:") ) {
                    // Strip and: prefix from names
                    name = name.substring( 4 );
                }
                try {
                    Method method = methods.get( name );
                    if( method == null ) {
                        continue;
                    }
                    Class<?> propType = method.getParameterTypes()[0];
                    if( propType == Boolean.class ) {
                        method.invoke( object,  configuration.getValueAsBoolean( name ) );
                    }
                    else if( Number.class.isAssignableFrom( propType ) ) {
                        Number value = configuration.getValueAsNumber( name );
                        if( propType == Integer.class ) {
                            method.invoke( object, value.intValue() );
                        }
                        else if( propType == Float.class ) {
                            method.invoke( object, value.floatValue() );
                        }
                        else if( propType == Double.class ) {
                            method.invoke( object, value.doubleValue() );
                        }
                        else {
                            method.invoke( object, value );
                        }
                    }
                    else if( propType.isAssignableFrom( String.class ) ) {
                        method.invoke( object, configuration.getValueAsString( name ) );
                    }
                    else if( propType.isAssignableFrom( Date.class ) ) {
                        method.invoke( object, configuration.getValueAsDate( name ) );
                    }
                    else if( propType.isAssignableFrom( Drawable.class ) ) {
                        method.invoke( object, configuration.getValueAsImage( name ) );
                    }
                    else if( propType.isAssignableFrom( Color.class ) ) {
                        method.invoke( object, configuration.getValueAsColor( name ) );
                    }
                    else if( propType.isAssignableFrom( Resource.class ) ) {
                        Resource rsc = configuration.getValueAsResource( name );
                        method.invoke( object, rsc );
                    }
                    else if( propType.isAssignableFrom( Configuration.class ) ) {
                        Configuration config = configuration.getValueAsConfiguration( name );
                        method.invoke( object, config );
                    }
                    else if( propType.isAssignableFrom( List.class ) ) {
                        Object value = configuration.getValue( name );
                        if( value instanceof List ) {
                            // Resolve the list size, and make a new list to hold the property values.
                            int length = ((List<?>)value).size();
                            List propValues = new ArrayList( length );
                            // See if the method uses a generic argument type, and if so then use to
                            // discover the type of the list items.
                            Class itemType = Object.class;
                            Type genericArgType = method.getGenericParameterTypes()[0];
                            if( genericArgType instanceof ParameterizedType ) {
                                Type[] actualTypes = ((ParameterizedType)genericArgType).getActualTypeArguments();
                                if( actualTypes.length > 0 ) {
                                    // e.g. arg is declared as List<String>, so first type parameter is 'String'
                                    itemType = (Class)actualTypes[0];
                                }
                            }
                            for( int i = 0; i < length; i++ ) {
                                propValues.add( resolveObjectProperty( itemType, configuration, name+"."+i ) );
                            }
                            method.invoke( object, propValues );
                        }
                    }
                    else if( propType.isAssignableFrom( Map.class ) ) {
                        Configuration propConfigs = configuration.getValueAsConfiguration( name );
                        if( propConfigs != null ) {
                            // See if the method uses a generic argument type, and if so then use to
                            // discover the type of the map items.
                            Class itemType = Object.class;
                            Type genericArgType = method.getGenericParameterTypes()[0];
                            if( genericArgType instanceof ParameterizedType ) {
                                Type[] actualTypes = ((ParameterizedType)genericArgType).getActualTypeArguments();
                                // Check that we have two type parameters, and that the first is assignable from String.
                                if( actualTypes.length > 1 && ((Class)actualTypes[0]).isAssignableFrom( String.class ) ) {
                                    // e.g. arg is declared as Map<String,Number> so second type param is 'Number'
                                    itemType = (Class)actualTypes[1];
                                }
                            }
                            Map propValues = new HashMap();
                            for( String valueName : propConfigs.getValueNames() ) {
                                Object propValue = resolveObjectProperty( itemType, propConfigs, valueName );
                                if( propValue != null ) {
                                    propValues.put( valueName, propValue );
                                }
                            }
                            method.invoke( object, propValues );
                        }
                    }
                    else {
                        method.invoke( object, resolveObjectProperty( propType, configuration, name ) );
                    }
                }
                catch(Exception e) {
                    Log.e(Tag,String.format("Configuring %s", name ) );
                }
            }
            if( object instanceof IOCConfigurable ) {
                ((IOCConfigurable)object).afterConfigure( this );
            }
        }
        // If the object instance is a service then add to the list of services, and start the
        // service if the container services are running.
        if( object instanceof Service ) {
            Service service = (Service)object;
            services.add( service );
            if( running ) {
                service.startService();
            }
        }
    }
    
    private Object resolveObjectProperty(Class<?> propType, Configuration configuration, String name) {
        // TODO: Should this method also handle primitive types?
        Object object = configuration.getValue( name );
        if( propType.isAssignableFrom( object.getClass() ) ) {
            return object;
        }
        if( configuration.hasValue( name+".semo:type" ) ) {
            Configuration propConfig = configuration.getValueAsConfiguration( name );
            return buildObject( propConfig, name );
        }
        // TODO: In this case, can attempt to instantiate an object of the required property type
        // (i.e. infer the type) and the configure it.
        return null;
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
            Map<String,Configuration> objConfigs = new HashMap<String,Configuration>();
            // Initialize named objects.
            for(String name : names) {
                Configuration objConfig = namedConfig.getValueAsConfiguration( name );
                Object object = instantiateObject( objConfig, name );
                if( object != null ) {
                    named.put( name, object );
                    objConfigs.put( name, objConfig );
                }
            }
            // Configure named objects.
            for(String name : names) {
                Object object = named.get( name );
                if( object != null ) {
                    Configuration objConfig = objConfigs.get( name );
                    configureObject( object, objConfig, name );
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
