package com.innerfunction.semo;

import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import android.content.Context;
import android.content.res.Resources;
import android.util.DisplayMetrics;
import android.util.Log;

import com.innerfunction.uri.CompoundURI;
import com.innerfunction.uri.Resource;
import com.innerfunction.uri.SchemeHandler;
import com.innerfunction.uri.StandardURIResolver;
import com.innerfunction.util.I18nMap;
import com.innerfunction.util.Locals;

/**
 * A container providing default app functionality.
 * @author juliangoacher
 *
 */
public class AppContainer extends Container {

    static final String Tag = AppContainer.class.getSimpleName();
    
    /** Flag indicating whether to force-reset all local settings at app startup. */
    static final boolean ForceResetDefaultSettings = false;

    /**
     * A URI resolver.
     * As well as the standard URI schemes, provides a "named" scheme for resolving
     * the app container's named objects; as well as whatever additional schemes are
     * defined in the "schemes" section of the container's configuration.
     */
    private StandardURIResolver resolver;
    /**
     * The app's Android context.
     */
    private Context androidContext;
    /**
     * A set of global values.
     * Includes information about the device platform and locale. Available when evaluating
     * the container configuration.
     */
    private Map<String,Object> globals;
    /**
     * A set of locally stored settings.
     * Prefixed with the "semo." namespace.
     */
    private Locals locals;

    /**
     * Create a new app container.
     * @param config    An object describing the container's configuration. May be a Configuration
     *                  instance; or a CompoundURI, or a String URI, referencing the configuration.
     * @param context   The Android context.
     * @throws URISyntaxException If the configuration reference is an invalid URI.
     */
    @SuppressWarnings("unchecked")
    public AppContainer(Object config, Context context) throws URISyntaxException {
        
        resolver = StandardURIResolver.getInstance( context );
        androidContext = context;
        
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
                Log.i( Tag, String.format("Setting up app container with URI %s", uri ));
                Resource resource = resolver.resolveURI( uri );
                configuration = new Configuration( resource, context );
            }
            else {
                try {
                    Log.i( Tag, "Attempting to setup app container with data...");
                    configuration = new Configuration( (Map<String,Object>)config, context );
                }
                catch(ClassCastException e) {
                    Log.e( Tag, String.format("Unable to setup app container with data type %s", config.getClass().getName() ));
                }
            }
        }
        
        if( configuration != null ) {
            configure( configuration );
        }
        else {
            Log.w( Tag, "Unable to resolve configuration");
        }
    }
    
    /**
     * Configure this container.
     */
    @SuppressWarnings("unchecked")
    @Override
    public void configure(Configuration configuration) {
        // Set object type mappings.
        setTypes( configuration.getValueAsConfiguration("types") );
        
        // Add additional schemes to the resolver/dispatcher.
        resolver.addHandler("named", new SchemeHandler() {
            @Override
            public CompoundURI resolveToAbsoluteURI(CompoundURI uri, CompoundURI context) {
                return uri;
            }
            @Override
            public Resource handle(CompoundURI uri, Map<String, Resource> params, Resource parent) {
                Object namedObj = named.get( uri.getName() );
                return namedObj == null ? null : new Resource( androidContext, namedObj, uri, parent );
            }
        });
        Configuration dispatcherConfig = configuration.getValueAsConfiguration("schemes");
        for( String schemeName : dispatcherConfig.getValueNames() ) {
            Configuration schemeConfig  = dispatcherConfig.getValueAsConfiguration( schemeName );
            Object handler = makeObject( schemeConfig, schemeName );
            if( handler instanceof SchemeHandler ) {
                resolver.addHandler( schemeName, (SchemeHandler)handler );
            }
        }

        // Default local settings.
        this.locals = new Locals("semo");
        Map<String,Object> settings = (Map<String,Object>)configuration.getValue("settings");
        if( settings != null ) {
            locals.setValues( settings, ForceResetDefaultSettings );
        }

        // Setup template context.
        globals = makeDefaultGlobalModelValues( configuration );
        configuration.setTemplateContext( globals );

        named.put("resolver", resolver );
        named.put("androidContext", androidContext );
        named.put("globals", globals );
        named.put("locals", locals );
        named.put("container", this );
        
        // Add named objects.
        Configuration namedConfig = configuration.getValueAsConfiguration("named");
        if( namedConfig == null ) {
            namedConfig = configuration.getValueAsConfiguration("names");
        }
        if( namedConfig != null ) {
            super.configure( namedConfig );
        }
    }
    
    /**
     * Make the set of global values.
     * @param configuration
     * @return
     */
    private Map<String,Object> makeDefaultGlobalModelValues(Configuration configuration) {
        
        Resources r = androidContext.getResources();
        
        Map<String,Object> values = new HashMap<String,Object>();
        DisplayMetrics dm = r.getDisplayMetrics();
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
        
        String mode = configuration.getValueAsString("mode", "LIVE");
        Log.i(Tag, String.format("Configuration mode: %s", mode ));
        values.put("mode", mode );
        
        Locale locale = r.getConfiguration().locale;
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
        values.put("i18n", new I18nMap( androidContext ) );
        
        return values;
    }
    
}