package com.innerfunction.semo.core;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import android.app.Activity;
import android.content.Context;
/*
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
*/
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.util.DisplayMetrics;
import android.util.Log;

import com.innerfunction.semo.core.Configuration.ValueType;
import com.innerfunction.uri.CompoundURI;
import com.innerfunction.uri.IFAssetManager;
import com.innerfunction.uri.Resource;
import com.innerfunction.uri.SchemeHandler;
import com.innerfunction.uri.StandardURIResolver;
import com.innerfunction.uri.URIResolver;
import com.innerfunction.util.I18nMap;
import com.innerfunction.util.Locals;
import com.innerfunction.util.StringTemplate;
import com.innerfunction.util.TypeConversions;

/**
 * Semo core service and root resource.
 * Note on usage: Core is a singleton class, and instances are mainly resolved using getCore(). Note however
 * that the singleton must be initialized and configured by a call to Core.setupWithConfiguration(...) before first
 * use. The main reason for this is because the Android context must be passed to the instance before it can be
 * fully operational.
 * 
 * @author juliangoacher
 *
 */
public class Core extends Resource implements Service, ComponentFactory {

    static final String Tag = Core.class.getSimpleName();

    private static Core CoreInstance;
    
    private Configuration configuration;
    private String mode; // Configuration mode - normally 'test' or 'live'.
    private List<Service> services = new ArrayList<Service>();
    private Map<String,Service> servicesByName = new HashMap<String,Service>();
    private Map<String,Object> globalValues;
    private IFAssetManager assetManager;
    private StandardURIResolver standardResolver;
    private Configuration types;
    private TypeConversions conversions;
    private Context androidContext;
    private Resources r;
    private I18nMap i18nMap;
    private Activity currentActivity;
    private Locals locals;
    
    private Core(Context androidContext) {
        super( androidContext );
        this.assetManager = new IFAssetManager( androidContext );
        this.standardResolver = new StandardURIResolver( androidContext, this, assetManager );
        this.resolver = this.standardResolver; // this.resolver is in Resource super class.
        this.conversions = TypeConversions.instanceForContext( androidContext );
        this.androidContext = androidContext;
        this.r = androidContext.getResources();
        this.i18nMap = new I18nMap( androidContext );
        this.uri = new CompoundURI("s","SemoCore");
        this.locals = new Locals("semo");
    }
    
    public void configure(Configuration configuration) {
        setItem( configuration ); // Set the core resource's data to the configuration object.
        
        this.mode = configuration.getValueAsString("mode", "LIVE");
        Log.i(Tag, String.format("Configuration mode: %s", mode ));
        
        // Setup template context.
        globalValues = makeDefaultGlobalModelValues( configuration );
        configuration.setTemplateContext( globalValues );
        
        this.configuration = configuration;
        this.types = configuration.getValueAsConfiguration("types");
        
        setDefaultLocalSettings();
        
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
                        Log.w( Tag, String.format("Make %s: Class %s doesn't implement Component", id, className ));
                    }
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
        return result;
    }
    
    public Service getService(String name) {
        return servicesByName.get( name );
    }
    
    public URIResolver getResolver() {
        return standardResolver;
    }
    
    public Configuration getTypes() {
        return types;
    }
    
    public boolean isURIScheme(String schemeName) {
        return standardResolver.hasHandlerForURIScheme( schemeName );
    }
    
    public TypeConversions getTypeConversions() {
        return conversions;
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
            locals.setValues( settings, ForceResetDefaultSettings );
        }
    }
    
    public Locals getLocalSettings() {
        return locals;
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
    /*
    public SharedPreferences getLocalStorage(){
        ApplicationInfo ainfo = this.androidContext.getApplicationInfo();
        String prefsName = String.format("Eventpac.%s", ainfo.processName );
        Log.i( Tag, String.format("Using shared preferences name %s", prefsName ) );
        return this.androidContext.getSharedPreferences( prefsName, 0 );
    }
    */
    public IFAssetManager getAssetManager() {
        return this.assetManager;
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
            appVersion = context.getPackageManager().getPackageInfo( context.getPackageName(), 0 ).versionName;
            String currentAppVersion = locals.getString("appVersion", null );
            upgradeStart = !appVersion.equals( currentAppVersion );
            if( upgradeStart ) {
                Log.d(Tag,String.format("Detected app upgrade from %s to %s", currentAppVersion, appVersion ));
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
                locals.setString("appVersion", appVersion );
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