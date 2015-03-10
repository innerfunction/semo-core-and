package com.innerfunction.semo.core;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Timer;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.util.DisplayMetrics;
import android.util.Log;

import com.innerfunction.semo.core.mvc.Controller;
import com.innerfunction.semo.core.mvc.DataObserver;
import com.innerfunction.semo.core.ui.ViewFactory;
import com.innerfunction.semo.core.ui.ViewResource;
import com.innerfunction.util.Configuration;
import com.innerfunction.util.Configuration.ValueType;
import com.innerfunction.uri.CompoundURI;
import com.innerfunction.uri.IFAssetManager;
import com.innerfunction.uri.Resource;
import com.innerfunction.uri.SchemeHandler;
import com.innerfunction.uri.StandardURIResolver;
import com.innerfunction.uri.URIResolver;
import com.innerfunction.util.StringTemplate;
import com.innerfunction.util.TypeConversions;

/**
 * Eventpac core service and root resource.
 * Note on usage: Core is a singleton class, and instances are mainly resolved using getCore(). Note however
 * that the singleton must be initialized and configured by a call to Core.setupWithConfiguration(...) before first
 * use. The main reason for this is because the Android context must be passed to the instance before it can be
 * fully operational.
 * 
 * @author juliangoacher
 *
 */
public class Core extends Resource implements Service, EPEventHandler {

    static final String Tag = Core.class.getSimpleName();

    private static Core CoreInstance;
    
    private Configuration configuration;
    private String mode; // Configuration mode - normally 'test' or 'live'.
    private List<Service> services = new ArrayList<Service>();
    private Map<String,Service> servicesByName = new HashMap<String,Service>();
    private Map<String,Object> globalValues;
    private Controller mvc;
    private IFAssetManager assetManager;
    private StandardURIResolver standardResolver;
    private Configuration types;
    private TypeConversions conversions;
    private Context androidContext;
    private Resources r;
    private I18nMap i18nMap = new I18nMap( this );
    private String packageName;
    private Activity currentActivity;
    private Timer timer;

    private Core(Context androidContext) {
        super( androidContext );
        this.assetManager = new IFAssetManager( androidContext );
        this.standardResolver = new StandardURIResolver( androidContext, this, assetManager );
        this.resolver = this.standardResolver; // this.resolver is in Resource super class.
        this.conversions = TypeConversions.instanceForContext( androidContext );
        this.androidContext = androidContext;
        this.r = androidContext.getResources();
        this.packageName = androidContext.getPackageName();
        this.timer = new Timer();
        this.uri = new CompoundURI("s","EPCore");
    }
    
    public void configure(Configuration configuration) {
        setItem( configuration ); // Set the core resource's data to the configuration object.

        this.mode = configuration.getValueAsString("mode", "LIVE");
        Log.i(Tag, String.format("Configuration mode: %s", mode ));

        // Setup template context.
        globalValues = makeDefaultGlobalModelValues( configuration );
        configuration.setContext( globalValues );

        this.configuration = configuration;
        this.types = configuration.getValueAsConfiguration("types");

        setDefaultLocalSettings();
        
        // Setup the MVC controller.
        mvc = new Controller( standardResolver, globalValues );
        mvc.configure( configuration );
        services.add( mvc );
        servicesByName.put("mvc", mvc );

        // Setup services.
        if( configuration.getValueType("services") == ValueType.List ) {
            List<Configuration> servicesConfig = configuration.getValueAsConfigurationList("services");
            int i = 0;
            for( Configuration serviceConfig : servicesConfig ) {
                String serviceName = serviceConfig.getValueAsString("name");
                if( serviceName != null ) {
                    Component component = makeComponent( serviceConfig, serviceName );
                    if( component != null && component instanceof Service ) {
                        Service service = (Service)component;
                        services.add( service );
                        servicesByName.put( serviceName, service );
                    }
                }
                else {
                    Log.w(Tag,String.format("No name provided for service at position %d, skipping instantiation", i));
                }
                i++;
            }
        }
        else {
            Log.e(Tag,"'services' configuration must be a list");
        }

        // Add additional schemes to the resolver/dispatcher.
        Configuration dispatcherConfig = configuration.getValueAsConfiguration("dispatcher.schemes");
        for( String schemeName : dispatcherConfig.getValueNames() ) {
            Configuration schemeConfig  = dispatcherConfig.getValueAsConfiguration( schemeName );
            Component component = makeComponent( schemeConfig, schemeName );
            if( component instanceof SchemeHandler ) {
                standardResolver.addHandler( schemeName, (SchemeHandler)component );
            }
            if( component instanceof Service ) {
                Service service = (Service)component;
                services.add( service );
                servicesByName.put( String.format("%s:", schemeName ), service );
            }
        }

        mvc.setGlobalValue("services", servicesByName );
    }
    
    public Component makeComponent(Configuration definition, String id) {
        definition = definition.normalize();
        String type = definition.getValueAsString("type");
        Component result = null;
        if( type != null ) {
            String className = types.getValueAsString( type );
            if( className != null ) {
                try {
                    Object instance = Class.forName( className ).newInstance();
                    if( instance instanceof Component ) {
                        result = (Component)instance;
                        result.configure( definition );
                    }
                    else {
                        Log.w( Tag, String.format("EPCore - make '%s': Instance of class %s not a Component", id, className ));
                    }
                }
                catch (InstantiationException e) {
                    Log.e( Tag, String.format("EPCore - make '%s': Error instantiating class %s", id, className ), e );
                }
                catch (IllegalAccessException e) {
                    Log.e( Tag, String.format("EPCore - make '%s': Unable to instantiating class %s", id, className ), e );
                }
                catch (ClassNotFoundException e) {
                    Log.e( Tag, String.format("EPCore - make '%s': Class %s not found", id, className ), e );
                }
            }
            else {
                Log.w( Tag, String.format("EPCore - make '%s': No class name found for type (%s)", id, type ));
            }
        }
        else {
            Log.w( Tag, String.format("EPCore - make '%s': Component configuration missing 'type' property", id ));
        }
        return result;
    }
    
    public Controller getMVC() {
        return mvc;
    }
    
    public ViewResource getRootView() {
        ViewResource result = null;
        try {
            result = (ViewResource)this.configuration.getValueAsResource("rootView");
        }
        catch(ClassCastException e) {
            Log.e(Tag,"Root view does not resolve to a ViewResource");
        }
        if( result == null ) {
            Log.e(Tag,"Root view not resolved");
        }
        return result;
    }
    
    public Service getService(String name) {
        return servicesByName.get( name );
    }

    public ViewFactory getViewFactory() {
        ViewFactory viewFactory = (ViewFactory)getService("viewFactory");
        if( viewFactory == null ) {
            Log.w(Tag,"View factory service 'viewFactory' not available");
        }
        return viewFactory;
    }

    public URIResolver getResolver() {
        return standardResolver;
    }

    public Configuration getTypes() {
        return types;
    }
    
    public boolean isEPURIScheme(String schemeName) {
        return standardResolver.hasHandlerForURIScheme( schemeName );
    }
    
    public TypeConversions getTypeConversions() {
        return conversions;
    }

    public String getLocalizedString(String resourceID) {
        int rid = this.r.getIdentifier( resourceID, "string", this.packageName );
        return rid > 0 ? this.r.getString( rid ) : null;
    }

    public Drawable resolveImage(String svalue) {
        svalue = StringTemplate.render( svalue, globalValues );
        Drawable image = null;
        if( svalue.charAt( 0 ) == '@' ) {
            String uri = svalue.substring( 1 );
            Resource rsc = resolver.resolveURIFromString( uri );
            if( rsc != null ) {
                image = rsc.asImage();
            }
        }
        else {
            image = conversions.asImage( svalue );
        }
        return image;
    }

    /**
     * Add a data observer for the specified configuration.
     * The configuration should have an "observes" property.
     * @param observer
     * @param configuration
     */
    public void addDataObserverForConfiguration(DataObserver observer, Configuration configuration) {
        if( configuration.hasValue("observes") ) {
            String observes = configuration.getValueAsString("observes");
            mvc.getGlobalModel().addObserver( observes, observer );
        }
    }

    /**
     * Remove a data observer for the specified configuration.
     * @param configuration
     * @return
     */
    public void removeDataObserver(DataObserver observer) {
        mvc.getGlobalModel().removeObserver( observer );
    }

    /** Flag indicating whether to force-reset all local settings at app startup. */
    static final boolean ForceResetDefaultSettings = false;
    
    /**
     * Set the app's default local settings.
     * Values are read from the "settings" configuration property. Values are only set
     * if a setting value doesn't already exist, or if ForceResetDefaultSettings is true. 
     */
    private void setDefaultLocalSettings() {
        @SuppressWarnings("unchecked")
        Map<String,Object> settings = (Map<String,Object>)configuration.getValue("settings");
        if( settings != null ) {
            SharedPreferences preferences = getLocalStorage();
            SharedPreferences.Editor editor = preferences.edit();
            for( String key : settings.keySet() ) {
                if( !preferences.contains( key ) || ForceResetDefaultSettings ) {
                    Object value = settings.get( key );
                    if( value instanceof String ) {
                        editor.putString( key, (String)value );
                    }
                    else if( value instanceof Boolean ) {
                        editor.putBoolean( key, (Boolean)value );
                    }
                    else if( value instanceof Number ) {
                        editor.putFloat( key, ((Number)value).floatValue() );
                    }
                }
            }
            editor.commit();
        }
    }
    
    private Map<String,Object> makeDefaultGlobalModelValues(Configuration configuration) {
        Map<String,Object> values = new HashMap<String,Object>();
        DisplayMetrics dm = this.r.getDisplayMetrics();
        String density;
        switch( dm.densityDpi ) {
        case DisplayMetrics.DENSITY_LOW:    density = "ldpi"; break;
        case DisplayMetrics.DENSITY_MEDIUM: density = "mdpi"; break;
        case DisplayMetrics.DENSITY_HIGH:   density = "hdpi"; break;
        case DisplayMetrics.DENSITY_XHIGH:  density = "xhdpi"; break;
        case DisplayMetrics.DENSITY_XXHIGH: density = "xxhdpi"; break;
        default:                            density = "hdpi";
        }
        Map<String,Object> platformValues = new HashMap<String,Object>();
        platformValues.put("name", "and");
        platformValues.put("display", density );
        platformValues.put("defaultDisplay", "hdpi");
        platformValues.put("full", "and-"+density);
        values.put("platform", platformValues );
        
        values.put("mode", mode );
        
        Locale locale = this.r.getConfiguration().locale;
        // The 'assetLocales' setting can be used to declare a list of the locales that app assets are
        // available in. If the platform's default locale (above) isn't on this list then the code below
        // will attempt to find a supported locale that uses the same language; if no match is found then
        // the first locale on the list is used as the default.
        if( configuration.hasValue("assetLocales") ) {
            @SuppressWarnings("unchecked")
            List<String> assetLocales = (List<String>)configuration.getValue("assetLocales");
            if( assetLocales.size() > 0 && !assetLocales.contains( locale.toString() ) ) {
                // Attempt to find a matching locale.
                // Always assigns the first item on the list (as the default option); if a later
                // item has a matching language then that is assigned and the loop is exited.
                String lang = locale.getLanguage();
                boolean langMatch = false, assignDefault;
                for( int i = 0; i < assetLocales.size() && !langMatch; i++ ) {
                    String[] localeParts = assetLocales.get( 0 ).split("_");
                    assignDefault = (i == 0);
                    langMatch = localeParts[0].equals( lang );
                    if( assignDefault || langMatch ) {
                        switch( localeParts.length ) {
                        case 1: locale = new Locale( localeParts[0] ); break;
                        case 2: locale = new Locale( localeParts[0], localeParts[1] ); break;
                        case 3: locale = new Locale( localeParts[0], localeParts[1], localeParts[3] ); break;
                        default:
                            Log.w(Tag,String.format("Bad locale identifier: %s", assetLocales.get( 0 )));
                        }
                    }
                }
            }
        }
        
        Map<String,Object> localeValues = new HashMap<String,Object>();
        localeValues.put("id", locale.toString());
        localeValues.put("lang", locale.getLanguage());
        localeValues.put("variant", locale.getVariant());
        values.put("locale", localeValues );
        
        // Access to localized resources through a Map interface.
        values.put("i18n", i18nMap );
        
        return values;
    }

    public I18nMap getI18nMap() {
        return i18nMap;
    }

    public Context getAndroidContext() {
        return androidContext;
    }
    
    public SharedPreferences getLocalStorage(){
        ApplicationInfo ainfo = this.androidContext.getApplicationInfo();
        String prefsName = String.format("Eventpac.%s", ainfo.processName );
        Log.i( Tag, String.format("Using shared preferences name %s", prefsName ) );
        return this.androidContext.getSharedPreferences( prefsName, 0 );
    }

    public IFAssetManager getAssetManager() {
        return this.assetManager;
    }

    public Timer getTimer() {
        return this.timer;
    }

    public CompoundURI setGlobalValue(String name, Object value) {
        mvc.setGlobalValue( name, value );
        return new CompoundURI("globals", name );
    }

    /**
     * A flag indicating that the current app startup is the first start after an app upgrade.
     */
    private boolean upgradeStart = false;
    public boolean isUpgradeState() {
        return upgradeStart;
    }

    public void startService() {
        String appVersion = null;
        try {
            appVersion = context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionName;
            String previousAppVersion = getLocalStorage().getString("previousAppVersion", null );
            upgradeStart = !appVersion.equals( previousAppVersion );
            if( upgradeStart ) {
                Log.d(Tag,String.format("Detected app upgrade from %s to %s", previousAppVersion, appVersion ));
            }
        }
        catch(Exception e) {
            Log.w(Tag,"Error whilst checking current app version", e );
        }
        for( String name : servicesByName.keySet() ) {
            Service service = servicesByName.get( name );
            try {
                service.startService();
            }
            catch(Exception e) {
                Log.e(Tag,String.format("Error starting service %s", name ), e );
            }
        }
        if( upgradeStart && appVersion != null ) {
            try {
                SharedPreferences.Editor editor = getLocalStorage().edit();
                editor.putString("previousAppVersion", appVersion );
                editor.commit();
            }
            catch(Exception e) {
                Log.w(Tag,"Error whilst storing current app version", e);
            }
        }
    }
    
    public void stopService() {
        for( String name : servicesByName.keySet() ) {
            Service service = servicesByName.get( name );
            try {
                service.stopService();
            }
            catch(Exception e) {
                Log.e(Tag,String.format("Error stopping service %s", name ), e );
            }
        }
    }
    
    public Activity getCurrentActivity() {
        return currentActivity;
    }
    
    public void setCurrentActivity(Activity activity) {
        currentActivity = activity;
    }
    
    public void unsetCurrentActivity(Activity activity) {
        if( currentActivity == activity ) {
            currentActivity = null;
        }
    }

    /**
     * Handle an EP application event.
     * If the event name is in the form service\<name>.<action> then the event will be dispatched
     * to the service with the specified name, assuming it exists and is an EPEventHandler instance.
     * @param name  The event name.
     * @param args  The event's arguments.
     * @return A result as returned by the event handler.
     */
    @Override
    public Object handleEPEvent(EPEvent event) {
        Object result = EPEventHandler.EventNotHandled;
        // Attempt dispatching to a service.
        String eventName = event.getName();
        if( eventName.startsWith("service/") ) {
            // TODO: Can this event name parsing code be moved into the EPEvent class?
            // Need to work out what the general form of an event/action name is.
            // (See also FragmentEPEventHandler; ABFragmentActivity handleEPEvent methods).
            String serviceName = eventName.substring( 8 );
            Service service = getService( serviceName );
            if( service instanceof EPEventHandler ) {
                result = ((EPEventHandler)service).handleEPEvent( event );
            }
        }
        return result == EPEventHandler.EventNotHandled ? null : result;
    }

    /**
     * Dispatch an application action.
     * The action description is basically an event: URI without the scheme, and this method
     * works by building a URI using the scheme and then dispatching it through the URI resolver.
     * @param config
     * @param context
     * @return
     * @throws URISyntaxException
     */
    public Object dispatchAction(String action, EPEventHandler target) {
        action = StringTemplate.render( action, mvc.getGlobalModel() );
        Object result = null;
        String uri = action.startsWith("event:") ? action : String.format("event:%s", action );
        Resource resource = this.resolver.resolveURIFromString( uri );
        if( resource instanceof EPEvent && target != null ) {
            result = target.handleEPEvent( (EPEvent)resource );
        }
        return result;
    }

    /**
     * Dispatch a URI.
     * Currently, the only dispatchable URI scheme is event:
     * @param config
     * @param context
     * @return
     * @throws URISyntaxException
     */
    public Object dispatchURI(String uri, EPEventHandler target) {
        return uri.startsWith("event:") ? dispatchAction( uri, target ) : null;
    }

    @SuppressWarnings("unchecked")
    public static Core setupWithConfiguration(Object config, Context context) throws URISyntaxException {
        
        CoreInstance = new Core( context );

        Configuration configuration = null;
        if( config instanceof Configuration ) {
            configuration = (Configuration)config;
        }
        else {
            CompoundURI uri = null;
            if( config instanceof CompoundURI ) {
                uri = (CompoundURI)config;
            }
            else if( config instanceof String ) {
                uri = new CompoundURI( (String)config );
            }
            if( uri != null ) {
                Log.i( Tag, String.format("Setting up EP Core with URI %s", uri ));
                Resource resource = CoreInstance.getResolver().resolveURI( uri, CoreInstance );
                configuration = new Configuration( resource, context );
            }
            else {
                try {
                    Log.i( Tag, "Attempting to setup EP Core with data...");
                    configuration = new Configuration( (Map<String,Object>)config, CoreInstance, context );
                }
                catch(ClassCastException e) {
                    Log.e(Tag, String.format("Unable to setup EP Core with data type %s", config.getClass().getName() ));
                }
            }
        }
        CoreInstance.configure( configuration );
        return CoreInstance;
    }
    
    public static Core getCore() {
        return CoreInstance;
    }
}