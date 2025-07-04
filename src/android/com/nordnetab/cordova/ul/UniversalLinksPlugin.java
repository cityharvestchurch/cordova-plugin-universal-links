package com.nordnetab.cordova.ul;

import android.content.Intent;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;
import java.util.Arrays;

import com.nordnetab.cordova.ul.js.JSAction;
import com.nordnetab.cordova.ul.model.JSMessage;
import com.nordnetab.cordova.ul.model.ULHost;
import com.nordnetab.cordova.ul.parser.ULConfigXmlParser;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaArgs;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaWebView;
import org.apache.cordova.PluginResult;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by Nikolay Demyankov on 09.09.15.
 * <p/>
 * Plugin main class.
 * Communicates with the JS side, handles launch intents and so on.
 */
public class UniversalLinksPlugin extends CordovaPlugin {

    // list of hosts, defined in config.xml
    private List<ULHost> supportedHosts;

    // list of subscribers
    private Map<String, CallbackContext> subscribers;

    // stored message, that is captured on application launch
    private JSMessage storedMessage;

     private final String pluginName = "UniversalLinksPlugin";

    // region Public API

    @Override
    public void initialize(CordovaInterface cordova, CordovaWebView webView) {
        super.initialize(cordova, webView);

        supportedHosts = new ULConfigXmlParser(cordova.getActivity()).parse();Log.d("com.outsystemsenterprise.cityharvestchurchdev.TheCHCApp", "supportedHosts: " + supportedHosts.size());

        if (subscribers == null) {
            subscribers = new HashMap<String, CallbackContext>();
        }

        handleIntent(cordova.getActivity().getIntent());
    }

    @Override
    public boolean execute(String action, CordovaArgs args, CallbackContext callbackContext) throws JSONException {Log.d("com.outsystemsenterprise.cityharvestchurchdev.TheCHCApp", "Execute Action: " + action);
        Log.d("com.outsystemsenterprise.cityharvestchurchdev.TheCHCApp", "Execute Action: " + (action == "getCordovaIntent"));
        boolean isHandled = true;
        if (JSAction.SUBSCRIBE.equals(action)) {
            subscribeForEvent(args, callbackContext);
        } else if (JSAction.UNSUBSCRIBE.equals(action)) {
            unsubscribeFromEvent(args);
        } else if(action.equals("getCordovaIntent")){Log.d("com.outsystemsenterprise.cityharvestchurchdev.TheCHCApp", "Execute getCordovaIntent");
            getCordovaIntent(callbackContext);
        } else {
            isHandled = false;
        }

        return isHandled;
    }

    @Override
    public void onNewIntent(Intent intent) {Log.d("com.outsystemsenterprise.cityharvestchurchdev.TheCHCApp", "onNewIntent");
        super.onNewIntent(intent);
        handleIntent(intent);
    }

    // endregion

    // region JavaScript methods

    /**
     * Add subscriber for the event.
     *
     * @param arguments       arguments, passed from JS side
     * @param callbackContext callback to use when event is captured
     */
    private void subscribeForEvent(final CordovaArgs arguments, final CallbackContext callbackContext) {
        final String eventName = getEventNameFromArguments(arguments);Log.d("com.outsystemsenterprise.cityharvestchurchdev.TheCHCApp", "SubscribeForEvent: " + eventName);
        if (TextUtils.isEmpty(eventName)) {
            return;
        }

        subscribers.put(eventName, callbackContext);
        tryToConsumeEvent();
    }

    /**
     * Remove subscriber from the event.
     *
     * @param arguments arguments, passed from JS side
     */
    private void unsubscribeFromEvent(final CordovaArgs arguments) {
        if (subscribers.size() == 0) {
            return;
        }

        final String eventName = getEventNameFromArguments(arguments);
        if (TextUtils.isEmpty(eventName)) {
            return;
        }

        subscribers.remove(eventName);
    }

    /**
     * Get event name from the cordova arguments.
     *
     * @param arguments received arguments
     * @return event name; <code>null</code> if non is found
     */
    private String getEventNameFromArguments(final CordovaArgs arguments) {
        String eventName = null;
        try {
            eventName = arguments.getString(0);
        } catch (JSONException e) {
            Log.d("UniversalLinks", "Failed to get event name from the JS arguments", e);
        }

        return eventName;
    }

    /**
     * Try to send event to the subscribers.
     */
    private void tryToConsumeEvent() {
        if (subscribers.size() == 0 || storedMessage == null) {
            return;
        }

        final String storedEventName = storedMessage.getEventName();
        final Set<Map.Entry<String, CallbackContext>> subscribersSet = subscribers.entrySet();
        for (Map.Entry<String, CallbackContext> subscriber : subscribersSet) {
            final String eventName = subscriber.getKey();
            if (eventName.equals(storedEventName)) {
                sendMessageToJs(storedMessage, subscriber.getValue());
                storedMessage = null;
                break;
            }
        }
    }

    /**
     * Send message to JS side.
     *
     * @param message  message to send
     * @param callback to what callback we are sending the message
     */
    private void sendMessageToJs(JSMessage message, CallbackContext callback) {
        final PluginResult result = new PluginResult(PluginResult.Status.OK, message);
        result.setKeepCallback(true);
        callback.sendPluginResult(result);
    }

    // endregion

    // region Intent handling

    /**
     * Handle launch intent.
     * If it is an UL intent - then event will be dispatched to the JS side.
     *
     * @param intent launch intent
     */
    private void handleIntent(Intent intent) {
        if (intent == null || supportedHosts == null || supportedHosts.size() == 0) {
            return;
        }

        // read intent
        String action = intent.getAction();Log.d("com.outsystemsenterprise.cityharvestchurchdev.TheCHCApp", "Handle Intent Action: " + action);
        Uri launchUri = intent.getData();
        
        if(launchUri != null){
            Log.d("com.outsystemsenterprise.cityharvestchurchdev.TheCHCApp", launchUri.toString());
        }

        // if app was not launched by the url - ignore
        if (!Intent.ACTION_VIEW.equals(action) || launchUri == null) {
            return;
        }

        // try to find host in the hosts list from the config.xml
        ULHost host = findHostByUrl(launchUri);
        if (host == null) {
            Log.d("UniversalLinks", "Host " + launchUri.getHost() + " is not supported");
            return;
        }

        // store message and try to consume it
        storedMessage = new JSMessage(host, launchUri);
        tryToConsumeEvent();
    }

    /**
     * Find host entry that matches the launch url.
     *
     * @param url launch url
     * @return host entry; <code>null</code> - if none were found
     */
    private ULHost findHostByUrl(Uri url) {
        ULHost host = null;
        final String launchHost = url.getHost().toLowerCase();
        for (ULHost supportedHost : supportedHosts) {
            if (supportedHost.getName().equals(launchHost) ||
                    supportedHost.getName().startsWith("*.") && launchHost.endsWith(supportedHost.getName().substring(1))) {
                host = supportedHost;
                break;
            }
        }

        return host;
    }

    /**
     * Send a JSON representation of the cordova intent back to the caller
     *
     * @param data
     * @param context
     */
    public void getCordovaIntent (final CallbackContext context) {
        // if(data.length() != 0) {
        //     context.sendPluginResult(new PluginResult(PluginResult.Status.INVALID_ACTION));
        //     return false;
        // }

        Log.d("com.outsystemsenterprise.cityharvestchurchdev.TheCHCApp", "Inside getCordovaIntent");

        Intent intent = cordova.getActivity().getIntent();

        String action = intent.getAction();Log.d("com.outsystemsenterprise.cityharvestchurchdev.TheCHCApp", "Get Intent Action: " + action);
        Log.d("com.outsystemsenterprise.cityharvestchurchdev.TheCHCApp", "Before uri");
        Log.d("com.outsystemsenterprise.cityharvestchurchdev.TheCHCApp", "Action View: " + Intent.ACTION_VIEW);

        if (!Intent.ACTION_VIEW.equals(action)) {Log.d("com.outsystemsenterprise.cityharvestchurchdev.TheCHCApp", "is action view");
            context.sendPluginResult(new PluginResult(PluginResult.Status.OK));
            return;
        }

        Uri launchUri = intent.getData();Log.d("com.outsystemsenterprise.cityharvestchurchdev.TheCHCApp", launchUri.getPath());
        Log.d("com.outsystemsenterprise.cityharvestchurchdev.TheCHCApp", "After uri");

        JSONObject intentJSON = new JSONObject();

        try{
            intentJSON.put("action", action);
            intentJSON.put("uri", launchUri.toString());
        }

        catch(JSONException e) {
            Log.d(pluginName, pluginName + " Error thrown during intent > JSON conversion");
            Log.d(pluginName, e.getMessage());
            Log.d(pluginName, Arrays.toString(e.getStackTrace()));

            context.sendPluginResult(new PluginResult(PluginResult.Status.INVALID_ACTION, e.getMessage()));
            return;
        }

        context.sendPluginResult(new PluginResult(PluginResult.Status.OK, intentJSON));
    }

    // endregion
}
