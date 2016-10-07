/*
       Licensed to the Apache Software Foundation (ASF) under one
       or more contributor license agreements.  See the NOTICE file
       distributed with this work for additional information
       regarding copyright ownership.  The ASF licenses this file
       to you under the Apache License, Version 2.0 (the
       "License"); you may not use this file except in compliance
       with the License.  You may obtain a copy of the License at

         http://www.apache.org/licenses/LICENSE-2.0

       Unless required by applicable law or agreed to in writing,
       software distributed under the License is distributed on an
       "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
       KIND, either express or implied.  See the License for the
       specific language governing permissions and limitations
       under the License.
*/
package com.initialxy.cordova.themeablebrowser;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.StateListDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Browser;
import android.text.InputType;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.webkit.CookieManager;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.ProgressBar;
import android.widget.PopupWindow;
import android.widget.GridView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.AbsListView;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaArgs;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaWebView;
import org.apache.cordova.PluginManager;
import org.apache.cordova.PluginResult;
import org.apache.cordova.Whitelist;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

@SuppressLint("SetJavaScriptEnabled")
public class ThemeableBrowser extends CordovaPlugin {

    private static final String NULL = "null";
    protected static final String LOG_TAG = "ThemeableBrowser";
    private static final String SELF = "_self";
    private static final String SYSTEM = "_system";
    // private static final String BLANK = "_blank";
    private static final String EXIT_EVENT = "exit";
    private static final String LOAD_START_EVENT = "loadstart";
    private static final String LOAD_STOP_EVENT = "loadstop";
    private static final String LOAD_ERROR_EVENT = "loaderror";

    private static final String ALIGN_LEFT = "left";
    private static final String ALIGN_RIGHT = "right";

    private static final int TOOLBAR_DEF_HEIGHT = 44;
    private static final int DISABLED_ALPHA = 127;  // 50% AKA 127/255.

    private static final String EVT_ERR = "ThemeableBrowserError";
    private static final String EVT_WRN = "ThemeableBrowserWarning";
    private static final String ERR_CRITICAL = "critical";
    private static final String ERR_LOADFAIL = "loadfail";
    private static final String WRN_UNEXPECTED = "unexpected";
    private static final String WRN_UNDEFINED = "undefined";

    private ThemeableBrowserDialog dialog;
    private WebView inAppWebView;
    private EditText edittext;
    private CallbackContext callbackContext;
    private ProgressBar progressBar = null;
    private PopupWindow menuPopup = null;

    /**
     * Executes the request and returns PluginResult.
     *
     * @param action          The action to execute.
     * @param args            The exec() arguments, wrapped with some Cordova helpers.
     * @param callbackContext The callback context used when calling back into JavaScript.
     * @return
     * @throws JSONException
     */
    public boolean execute(String action, CordovaArgs args, final CallbackContext callbackContext) throws JSONException {
        if (action.equals("open")) {
            this.callbackContext = callbackContext;
            final String url = args.getString(0);
            String t = args.optString(1);
            if (t == null || t.equals("") || t.equals(NULL)) {
                t = SELF;
            }
            final String target = t;
            final Options features = parseFeature(args.optString(2));

            this.cordova.getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    String result = "";
                    // SELF
                    if (SELF.equals(target)) {
                        /* This code exists for compatibility between 3.x and 4.x versions of Cordova.
                         * Previously the Config class had a static method, isUrlWhitelisted(). That
                         * responsibility has been moved to the plugins, with an aggregating method in
                         * PluginManager.
                         */
                        Boolean shouldAllowNavigation = null;
                        if (url.startsWith("javascript:")) {
                            shouldAllowNavigation = true;
                        }
                        if (shouldAllowNavigation == null) {
                            shouldAllowNavigation = new Whitelist().isUrlWhiteListed(url);
                        }
                        if (shouldAllowNavigation == null) {
                            try {
                                Method gpm = webView.getClass().getMethod("getPluginManager");
                                PluginManager pm = (PluginManager)gpm.invoke(webView);
                                Method san = pm.getClass().getMethod("shouldAllowNavigation", String.class);
                                shouldAllowNavigation = (Boolean)san.invoke(pm, url);
                            } catch (NoSuchMethodException e) {
                            } catch (IllegalAccessException e) {
                            } catch (InvocationTargetException e) {
                            }
                        }
                        // load in webview
                        if (Boolean.TRUE.equals(shouldAllowNavigation)) {
                            webView.loadUrl(url);
                        }
                        //Load the dialer
                        else if (url.startsWith(WebView.SCHEME_TEL))
                        {
                            try {
                                Intent intent = new Intent(Intent.ACTION_DIAL);
                                intent.setData(Uri.parse(url));
                                cordova.getActivity().startActivity(intent);
                            } catch (android.content.ActivityNotFoundException e) {
                                emitError(ERR_CRITICAL,
                                        String.format("Error dialing %s: %s", url, e.toString()));
                            }
                        }
                        // load in ThemeableBrowser
                        else {
                            result = showWebPage(url, features);
                        }
                    }
                    // SYSTEM
                    else if (SYSTEM.equals(target)) {
                        result = openExternal(url);
                    }
                    // BLANK - or anything else
                    else {
                        result = showWebPage(url, features);
                    }

                    PluginResult pluginResult = new PluginResult(PluginResult.Status.OK, result);
                    pluginResult.setKeepCallback(true);
                    callbackContext.sendPluginResult(pluginResult);
                }
            });
        }
        else if (action.equals("close")) {
            closeDialog(true);
        }
        else if (action.equals("injectScriptCode")) {
            String jsWrapper = null;
            if (args.getBoolean(1)) {
                jsWrapper = String.format("prompt(JSON.stringify([eval(%%s)]), 'gap-iab://%s')", callbackContext.getCallbackId());
            }
            injectDeferredObject(args.getString(0), jsWrapper);
        }
        else if (action.equals("injectScriptFile")) {
            String jsWrapper;
            if (args.getBoolean(1)) {
                jsWrapper = String.format("(function(d) { var c = d.createElement('script'); c.src = %%s; c.onload = function() { prompt('', 'gap-iab://%s'); }; d.body.appendChild(c); })(document)", callbackContext.getCallbackId());
            } else {
                jsWrapper = "(function(d) { var c = d.createElement('script'); c.src = %s; d.body.appendChild(c); })(document)";
            }
            injectDeferredObject(args.getString(0), jsWrapper);
        }
        else if (action.equals("injectStyleCode")) {
            String jsWrapper;
            if (args.getBoolean(1)) {
                jsWrapper = String.format("(function(d) { var c = d.createElement('style'); c.innerHTML = %%s; d.body.appendChild(c); prompt('', 'gap-iab://%s');})(document)", callbackContext.getCallbackId());
            } else {
                jsWrapper = "(function(d) { var c = d.createElement('style'); c.innerHTML = %s; d.body.appendChild(c); })(document)";
            }
            injectDeferredObject(args.getString(0), jsWrapper);
        }
        else if (action.equals("injectStyleFile")) {
            String jsWrapper;
            if (args.getBoolean(1)) {
                jsWrapper = String.format("(function(d) { var c = d.createElement('link'); c.rel='stylesheet'; c.type='text/css'; c.href = %%s; d.head.appendChild(c); prompt('', 'gap-iab://%s');})(document)", callbackContext.getCallbackId());
            } else {
                jsWrapper = "(function(d) { var c = d.createElement('link'); c.rel='stylesheet'; c.type='text/css'; c.href = %s; d.head.appendChild(c); })(document)";
            }
            injectDeferredObject(args.getString(0), jsWrapper);
        }
        else if (action.equals("show")) {
            this.cordova.getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    dialog.show();
                }
            });
            PluginResult pluginResult = new PluginResult(PluginResult.Status.OK);
            pluginResult.setKeepCallback(true);
            this.callbackContext.sendPluginResult(pluginResult);
        }
        else if (action.equals("reload")) {
            if (inAppWebView != null) {
                this.cordova.getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        inAppWebView.reload();
                    }
                });
            }
        }
        else {
            return false;
        }
        return true;
    }

    /**
     * Called when the view navigates.
     */
    @Override
    public void onReset() {
        closeDialog(true);
    }

    /**
     * Called by AccelBroker when listener is to be shut down.
     * Stop listener.
     */
    public void onDestroy() {
        closeDialog(true);
    }

    /**
     * Inject an object (script or style) into the ThemeableBrowser WebView.
     *
     * This is a helper method for the inject{Script|Style}{Code|File} API calls, which
     * provides a consistent method for injecting JavaScript code into the document.
     *
     * If a wrapper string is supplied, then the source string will be JSON-encoded (adding
     * quotes) and wrapped using string formatting. (The wrapper string should have a single
     * '%s' marker)
     *
     * @param source      The source object (filename or script/style text) to inject into
     *                    the document.
     * @param jsWrapper   A JavaScript string to wrap the source string in, so that the object
     *                    is properly injected, or null if the source string is JavaScript text
     *                    which should be executed directly.
     */
    private void injectDeferredObject(String source, String jsWrapper) {
        String scriptToInject;
        if (jsWrapper != null) {
            org.json.JSONArray jsonEsc = new org.json.JSONArray();
            jsonEsc.put(source);
            String jsonRepr = jsonEsc.toString();
            String jsonSourceString = jsonRepr.substring(1, jsonRepr.length()-1);
            scriptToInject = String.format(jsWrapper, jsonSourceString);
        } else {
            scriptToInject = source;
        }
        final String finalScriptToInject = scriptToInject;
        this.cordova.getActivity().runOnUiThread(new Runnable() {
            @SuppressLint("NewApi")
            @Override
            public void run() {
                if (inAppWebView != null) {
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
                        // This action will have the side-effect of blurring the currently focused
                        // element
                        inAppWebView.loadUrl("javascript:" + finalScriptToInject);
                    } else {
                        inAppWebView.evaluateJavascript(finalScriptToInject, null);
                    }
                }
            }
        });
    }

    /**
     * Put the list of features into a hash map
     *
     * @param optString
     * @return
     */
    private Options parseFeature(String optString) {
        Options result = null;
        if (optString != null && !optString.isEmpty()) {
            try {
                result = ThemeableBrowserUnmarshaller.JSONToObj(
                        optString, Options.class);
            } catch (Exception e) {
                emitError(ERR_CRITICAL,
                        String.format("Invalid JSON @s", e.toString()));
            }
        } else {
            emitWarning(WRN_UNDEFINED,
                    "No config was given, defaults will be used, "
                    + "which is quite boring.");
        }

        if (result == null) {
            result = new Options();
        }

        // Always show location, this property is overwritten.
        result.location = true;

        return result;
    }

    /**
     * Display a new browser with the specified URL.
     *
     * @param url
     * @return
     */
    public String openExternal(String url) {
        try {
            Intent intent = null;
            intent = new Intent(Intent.ACTION_VIEW);
            // Omitting the MIME type for file: URLs causes "No Activity found to handle Intent".
            // Adding the MIME type to http: URLs causes them to not be handled by the downloader.
            Uri uri = Uri.parse(url);
            if ("file".equals(uri.getScheme())) {
                intent.setDataAndType(uri, webView.getResourceApi().getMimeType(uri));
            } else {
                intent.setData(uri);
            }
            intent.putExtra(Browser.EXTRA_APPLICATION_ID, cordova.getActivity().getPackageName());
            this.cordova.getActivity().startActivity(intent);
            return "";
        } catch (android.content.ActivityNotFoundException e) {
            Log.d(LOG_TAG, "ThemeableBrowser: Error loading url "+url+":"+ e.toString());
            return e.toString();
        }
    }

    /**
     * Closes the dialog
     */
    public void closeDialog(final boolean force) {
        this.cordova.getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                // The JS protects against multiple calls, so this should happen only when
                // closeDialog() is called by other native code.
                if (inAppWebView == null) {
                    emitWarning(WRN_UNEXPECTED, "Close called but already closed.");
                    return;
                }

                if (!force && menuPopup != null) {
                  menuPopup.dismiss();
                  menuPopup = null;
                  return;
                }

                if (menuPopup != null) {
                  menuPopup.dismiss();
                  menuPopup = null;
                }

                inAppWebView.setWebViewClient(new WebViewClient() {
                    // NB: wait for about:blank before dismissing
                    public void onPageFinished(WebView view, String url) {
                        if (dialog != null) {
                            dialog.dismiss();
                        }

                        // Clean up.
                        dialog = null;
                        inAppWebView = null;
                        edittext = null;
                        callbackContext = null;
                    }
                });

                // NB: From SDK 19: "If you call methods on WebView from any
                // thread other than your app's UI thread, it can cause
                // unexpected results."
                // http://developer.android.com/guide/webapps/migrating.html#Threads
                inAppWebView.loadUrl("about:blank");

                try {
                    JSONObject obj = new JSONObject();
                    obj.put("type", EXIT_EVENT);
                    sendUpdate(obj, false);
                } catch (JSONException ex) {
                }
            }
        });
    }

    private void emitButtonEvent(Event event, String url) {
        emitButtonEvent(event, url, null);
    }

    private void emitButtonEvent(Event event, String url, Integer index) {
        if (event != null && event.event != null) {
            try {
                JSONObject obj = new JSONObject();
                obj.put("type", event.event);
                obj.put("url", url);
                if (index != null) {
                    obj.put("index", index.intValue());
                }
                sendUpdate(obj, true);
            } catch (JSONException e) {
                // Ignore, should never happen.
            }
        } else {
            emitWarning(WRN_UNDEFINED,
                    "Button clicked, but event property undefined. "
                    + "No event will be raised.");
        }
    }

    private void emitError(String code, String message) {
        emitLog(EVT_ERR, code, message);
    }

    private void emitWarning(String code, String message) {
        emitLog(EVT_WRN, code, message);
    }

    private void emitLog(String type, String code, String message) {
        if (type != null) {
            try {
                JSONObject obj = new JSONObject();
                obj.put("type", type);
                obj.put("code", code);
                obj.put("message", message);
                sendUpdate(obj, true);
            } catch (JSONException e) {
                // Ignore, should never happen.
            }
        }
    }

    /**
     * Checks to see if it is possible to go back one page in history, then does so.
     */
    public void goBack() {
        if (this.inAppWebView != null && this.inAppWebView.canGoBack()) {
            this.inAppWebView.goBack();
        }
    }

    /**
     * Can the web browser go back?
     * @return boolean
     */
    public boolean canGoBack() {
        return this.inAppWebView != null && this.inAppWebView.canGoBack();
    }

    /**
     * Checks to see if it is possible to go forward one page in history, then does so.
     */
    private void goForward() {
        if (this.inAppWebView != null && this.inAppWebView.canGoForward()) {
            this.inAppWebView.goForward();
        }
    }

    /**
     * Navigate to the new page
     *
     * @param url to load
     */
    private void navigate(String url) {
        InputMethodManager imm = (InputMethodManager)this.cordova.getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(edittext.getWindowToken(), 0);

        if (!url.startsWith("http") && !url.startsWith("file:")) {
            this.inAppWebView.loadUrl("http://" + url);
        } else {
            this.inAppWebView.loadUrl(url);
        }
        this.inAppWebView.requestFocus();
    }

    private ThemeableBrowser getThemeableBrowser() {
        return this;
    }

    /**
     * Display a new browser with the specified URL.
     *
     * @param url
     * @param features
     * @return
     */
    public String showWebPage(final String url, final Options features) {
        final CordovaWebView thatWebView = this.webView;

        // Create dialog in new thread
        Runnable runnable = new Runnable() {
            @SuppressLint("NewApi")
            public void run() {
                // Let's create the main dialog
                dialog = new ThemeableBrowserDialog(cordova.getActivity(),
                        android.R.style.Theme_Black_NoTitleBar,
                        features.hardwareback);
                if (!features.disableAnimation) {
                    dialog.getWindow().getAttributes().windowAnimations
                            = android.R.style.Animation_Dialog;
                }
                dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
                dialog.setCancelable(true);
                dialog.setThemeableBrowser(getThemeableBrowser());

                RelativeLayout rlRoot = new RelativeLayout(cordova.getActivity());
                RelativeLayout.LayoutParams rlp = new RelativeLayout.LayoutParams(
                  RelativeLayout.LayoutParams.FILL_PARENT, RelativeLayout.LayoutParams.FILL_PARENT);

                // Main container layout
                ViewGroup main = null;

                if (features.fullscreen) {
                    main = new FrameLayout(cordova.getActivity());
                } else {
                    main = new LinearLayout(cordova.getActivity());
                    ((LinearLayout) main).setOrientation(LinearLayout.VERTICAL);
                }

                // Toolbar layout
                Toolbar toolbarDef = features.toolbar;
                FrameLayout toolbar = new FrameLayout(cordova.getActivity());
                toolbar.setBackgroundColor(hexStringToColor(
                        toolbarDef != null && toolbarDef.color != null
                                ? toolbarDef.color : "#ffffffff"));
                toolbar.setLayoutParams(new ViewGroup.LayoutParams(
                        LayoutParams.MATCH_PARENT,
                        dpToPixels(toolbarDef != null
                                ? toolbarDef.height : TOOLBAR_DEF_HEIGHT)));

                if (toolbarDef != null
                        && (toolbarDef.image != null || toolbarDef.wwwImage != null)) {
                    try {
                        Drawable background = getImage(toolbarDef.image
                                , toolbarDef.wwwImage, toolbarDef.wwwImageDensity);
                        setBackground(toolbar, background);
                    } catch (Resources.NotFoundException e) {
                        emitError(ERR_LOADFAIL,
                                String.format("Image for toolbar, %s, failed to load",
                                        toolbarDef.image));
                    } catch (IOException ioe) {
                        emitError(ERR_LOADFAIL,
                                String.format("Image for toolbar, %s, failed to load",
                                        toolbarDef.wwwImage));
                    }
                }

                // Left Button Container layout
                LinearLayout leftButtonContainer = new LinearLayout(cordova.getActivity());
                FrameLayout.LayoutParams leftButtonContainerParams = new FrameLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
                leftButtonContainerParams.gravity = Gravity.LEFT | Gravity.CENTER_VERTICAL;
                leftButtonContainer.setLayoutParams(leftButtonContainerParams);
                leftButtonContainer.setVerticalGravity(Gravity.CENTER_VERTICAL);

                // Right Button Container layout
                LinearLayout rightButtonContainer = new LinearLayout(cordova.getActivity());
                FrameLayout.LayoutParams rightButtonContainerParams = new FrameLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
                rightButtonContainerParams.gravity = Gravity.RIGHT | Gravity.CENTER_VERTICAL;
                rightButtonContainer.setLayoutParams(rightButtonContainerParams);
                rightButtonContainer.setVerticalGravity(Gravity.CENTER_VERTICAL);

                // Edit Text Box
                edittext = new EditText(cordova.getActivity());
                RelativeLayout.LayoutParams textLayoutParams = new RelativeLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
                textLayoutParams.addRule(RelativeLayout.RIGHT_OF, 1);
                textLayoutParams.addRule(RelativeLayout.LEFT_OF, 5);
                edittext.setLayoutParams(textLayoutParams);
                edittext.setSingleLine(true);
                edittext.setText(url);
                edittext.setInputType(InputType.TYPE_TEXT_VARIATION_URI);
                edittext.setImeOptions(EditorInfo.IME_ACTION_GO);
                edittext.setInputType(InputType.TYPE_NULL); // Will not except input... Makes the text NON-EDITABLE
                edittext.setOnKeyListener(new View.OnKeyListener() {
                    public boolean onKey(View v, int keyCode, KeyEvent event) {
                        // If the event is a key-down event on the "enter" button
                        if ((event.getAction() == KeyEvent.ACTION_DOWN) && (keyCode == KeyEvent.KEYCODE_ENTER)) {
                            navigate(edittext.getText().toString());
                            return true;
                        }
                        return false;
                    }
                });

                // Back button
                final Button back = createButton(
                    features.backButton,
                    "back button",
                    new View.OnClickListener() {
                        public void onClick(View v) {
                            emitButtonEvent(
                                    features.backButton,
                                    inAppWebView.getUrl());

                            if (features.backButtonCanClose && !canGoBack()) {
                                closeDialog(false);
                            } else {
                                goBack();
                            }
                        }
                    }
                );

                if (back != null) {
                    back.setEnabled(features.backButtonCanClose);
                }

                // Forward button
                final Button forward = createButton(
                    features.forwardButton,
                    "forward button",
                    new View.OnClickListener() {
                        public void onClick(View v) {
                            emitButtonEvent(
                                    features.forwardButton,
                                    inAppWebView.getUrl());

                            goForward();
                        }
                    }
                );

                if (back != null) {
                    back.setEnabled(false);
                }


                // Close/Done button
                Button close = createButton(
                    features.closeButton,
                    "close button",
                    new View.OnClickListener() {
                        public void onClick(View v) {
                            emitButtonEvent(
                                    features.closeButton,
                                    inAppWebView.getUrl());
                            closeDialog(false);
                        }
                    }
                );

                final Button menu = features.menu != null
                        ? new Button(cordova.getActivity()) : null;
                if (menu != null) {
                  menu.setLayoutParams(new LinearLayout.LayoutParams(
                          LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
                  setButtonImages(menu, features.menu, DISABLED_ALPHA);
                  menu.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                      Log.i("##RDBG", "menu button clicked");
                      LinearLayout llRoot = new LinearLayout(cordova.getActivity());
                      llRoot.setOrientation(LinearLayout.VERTICAL);
                      llRoot.setPadding(5, 20, 5, 20);
                      //LinearLayout.LayoutParams llParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT,LinearLayout.LayoutParams.WRAP_CONTENT);
                      //llRoot.setLayoutParams(llParams);

                      final PopupWindow pw = new PopupWindow(llRoot, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);

                      GridView gvMenus = new GridView(cordova.getActivity());
                      LinearLayout.LayoutParams llMenus = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT,LinearLayout.LayoutParams.WRAP_CONTENT);
                      llMenus.gravity = Gravity.CENTER_HORIZONTAL;
                      gvMenus.setLayoutParams(llMenus);
                      gvMenus.setNumColumns(3);
                      gvMenus.setVerticalSpacing(5);
                      gvMenus.setHorizontalSpacing(5);
                      gvMenus.setAdapter(new ShareMenuAdapter(cordova.getActivity(), features));
                      llRoot.addView(gvMenus);
                      gvMenus.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                        @Override
                        public void onItemClick(AdapterView<?> parent, View view,
                          int position, long id) {
                            Log.i("##RDBG", "onItemClick: position: " + position);
                            if (inAppWebView != null
                                    && position < features.menu.items.length) {
                                Log.i("##RDBG", "emitButtonEvent: position: " + position);
                                emitButtonEvent(
                                        features.menu.items[position],
                                        inAppWebView.getUrl(), position);
                            }
                            pw.dismiss();
                        }
                      });

                      /*Button btnCancel = new Button(cordova.getActivity());
                      btnCancel.setText("Cancel");
                      LinearLayout.LayoutParams llCancel = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, 70);
                      llCancel.gravity = Gravity.CENTER_HORIZONTAL;
                      llCancel.setMargins(0, 10, 0, 0);
                      btnCancel.setLayoutParams(llCancel);
                      btnCancel.setOnClickListener(new View.OnClickListener() {
                        public void onClick(View v) {
                          pw.dismiss();
                        }
                      });
                      llRoot.addView(btnCancel);*/

                      llRoot.setBackgroundColor(Color.parseColor("#ff888888"));
                      pw.setOutsideTouchable(true);
                      pw.setFocusable(false);
                      pw.setBackgroundDrawable(new BitmapDrawable());
                      pw.showAtLocation(inAppWebView, Gravity.CENTER, 0, 0);
                      menuPopup = pw;
                    }
                  });
                }

                // Menu button
                /*Spinner menu = features.menu != null
                        ? new MenuSpinner(cordova.getActivity()) : null;
                if (menu != null) {
                    menu.setLayoutParams(new LinearLayout.LayoutParams(
                            LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
                    menu.setContentDescription("menu button");
                    setButtonImages(menu, features.menu, DISABLED_ALPHA);

                    // We are not allowed to use onClickListener for Spinner, so we will use
                    // onTouchListener as a fallback.
                    menu.setOnTouchListener(new View.OnTouchListener() {
                        @Override
                        public boolean onTouch(View v, MotionEvent event) {
                            if (event.getAction() == MotionEvent.ACTION_UP) {
                                emitButtonEvent(
                                        features.menu,
                                        inAppWebView.getUrl());
                            }
                            return false;
                        }
                    });

                    if (features.menu.items != null) {
                        HideSelectedAdapter<EventLabel> adapter
                                = new HideSelectedAdapter<EventLabel>(
                                cordova.getActivity(),
                                android.R.layout.simple_spinner_item,
                                features.menu.items);
                        adapter.setDropDownViewResource(
                                android.R.layout.simple_spinner_dropdown_item);
                        menu.setAdapter(adapter);
                        menu.setOnItemSelectedListener(
                                new AdapterView.OnItemSelectedListener() {
                                    @Override
                                    public void onItemSelected(
                                            AdapterView<?> adapterView,
                                            View view, int i, long l) {
                                        if (inAppWebView != null
                                                && i < features.menu.items.length) {
                                            emitButtonEvent(
                                                    features.menu.items[i],
                                                    inAppWebView.getUrl(), i);
                                        }
                                    }

                                    @Override
                                    public void onNothingSelected(
                                            AdapterView<?> adapterView) {
                                    }
                                }
                        );
                    }
                }*/

                // Title
                final TextView title = features.title != null
                        ? new TextView(cordova.getActivity()) : null;
                if (title != null) {
                    FrameLayout.LayoutParams titleParams
                            = new FrameLayout.LayoutParams(
                            LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
                    titleParams.gravity = Gravity.CENTER;
                    title.setLayoutParams(titleParams);
                    title.setSingleLine();
                    title.setEllipsize(TextUtils.TruncateAt.END);
                    title.setGravity(Gravity.CENTER);
                    title.setTextColor(hexStringToColor(
                            features.title.color != null
                                    ? features.title.color : "#000000ff"));
                    if (features.title.staticText != null) {
                        title.setText(features.title.staticText);
                    }
                }

                // WebView
                inAppWebView = new WebView(cordova.getActivity());
                final ViewGroup.LayoutParams inAppWebViewParams = features.fullscreen
                        ? new FrameLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
                        : new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, 0);
                if (!features.fullscreen) {
                    ((LinearLayout.LayoutParams) inAppWebViewParams).weight = 1;
                }
                inAppWebView.setLayoutParams(inAppWebViewParams);
                inAppWebView.setWebChromeClient(new InAppChromeClient(thatWebView));
                WebViewClient client = new ThemeableBrowserClient(thatWebView, new PageLoadListener() {
                    @Override
                    public void onPageFinished(String url, boolean canGoBack, boolean canGoForward) {
                        if (inAppWebView != null
                                && title != null && features.title != null
                                && features.title.staticText == null
                                && features.title.showPageTitle) {
                            title.setText(inAppWebView.getTitle());
                        }

                        if (back != null) {
                            back.setEnabled(canGoBack || features.backButtonCanClose);
                        }

                        if (forward != null) {
                            forward.setEnabled(canGoForward);
                        }

                        if (progressBar != null)
                          progressBar.setVisibility(View.GONE);
                    }
                });
                inAppWebView.setWebViewClient(client);
                WebSettings settings = inAppWebView.getSettings();
                settings.setJavaScriptEnabled(true);
                settings.setJavaScriptCanOpenWindowsAutomatically(true);
                settings.setBuiltInZoomControls(features.zoom);
                settings.setDisplayZoomControls(false);
                settings.setPluginState(android.webkit.WebSettings.PluginState.ON);

                //Toggle whether this is enabled or not!
                Bundle appSettings = cordova.getActivity().getIntent().getExtras();
                boolean enableDatabase = appSettings == null || appSettings.getBoolean("ThemeableBrowserStorageEnabled", true);
                if (enableDatabase) {
                    String databasePath = cordova.getActivity().getApplicationContext().getDir("themeableBrowserDB", Context.MODE_PRIVATE).getPath();
                    settings.setDatabasePath(databasePath);
                    settings.setDatabaseEnabled(true);
                }
                settings.setDomStorageEnabled(true);

                if (features.clearcache) {
                    CookieManager.getInstance().removeAllCookie();
                } else if (features.clearsessioncache) {
                    CookieManager.getInstance().removeSessionCookie();
                }

                inAppWebView.loadUrl(url);
                inAppWebView.getSettings().setLoadWithOverviewMode(true);
                inAppWebView.getSettings().setUseWideViewPort(true);
                inAppWebView.requestFocus();
                inAppWebView.requestFocusFromTouch();

                // Add buttons to either leftButtonsContainer or
                // rightButtonsContainer according to user's alignment
                // configuration.
                int leftContainerWidth = 0;
                int rightContainerWidth = 0;

                if (features.customButtons != null) {
                    for (int i = 0; i < features.customButtons.length; i++) {
                        final BrowserButton buttonProps = features.customButtons[i];
                        final int index = i;
                        Button button = createButton(
                            buttonProps,
                            String.format("custom button at %d", i),
                            new View.OnClickListener() {
                                @Override
                                public void onClick(View view) {
                                    if (inAppWebView != null) {
                                        emitButtonEvent(buttonProps,
                                                inAppWebView.getUrl(), index);
                                    }
                                }
                            }
                        );

                        if (ALIGN_RIGHT.equals(buttonProps.align)) {
                            rightButtonContainer.addView(button);
                            rightContainerWidth
                                    += button.getLayoutParams().width;
                        } else {
                            leftButtonContainer.addView(button, 0);
                            leftContainerWidth
                                    += button.getLayoutParams().width;
                        }
                    }
                }

                // Back and forward buttons must be added with special ordering logic such
                // that back button is always on the left of forward button if both buttons
                // are on the same side.
                if (forward != null && features.forwardButton != null
                        && !ALIGN_RIGHT.equals(features.forwardButton.align)) {
                    leftButtonContainer.addView(forward, 0);
                    leftContainerWidth
                            += forward.getLayoutParams().width;
                }

                if (back != null && features.backButton != null
                        && ALIGN_RIGHT.equals(features.backButton.align)) {
                    rightButtonContainer.addView(back);
                    rightContainerWidth
                            += back.getLayoutParams().width;
                }

                if (back != null && features.backButton != null
                        && !ALIGN_RIGHT.equals(features.backButton.align)) {
                    leftButtonContainer.addView(back, 0);
                    leftContainerWidth
                            += back.getLayoutParams().width;
                }

                if (forward != null && features.forwardButton != null
                        && ALIGN_RIGHT.equals(features.forwardButton.align)) {
                    rightButtonContainer.addView(forward);
                    rightContainerWidth
                            += forward.getLayoutParams().width;
                }

                if (menu != null) {
                    if (features.menu != null
                            && ALIGN_RIGHT.equals(features.menu.align)) {
                        rightButtonContainer.addView(menu);
                        rightContainerWidth
                                += menu.getLayoutParams().width;
                    } else {
                        leftButtonContainer.addView(menu, 0);
                        leftContainerWidth
                                += menu.getLayoutParams().width;
                    }
                }

                if (close != null) {
                    if (features.closeButton != null
                            && ALIGN_RIGHT.equals(features.closeButton.align)) {
                        rightButtonContainer.addView(close);
                        rightContainerWidth
                                += close.getLayoutParams().width;
                    } else {
                        leftButtonContainer.addView(close, 0);
                        leftContainerWidth
                                += close.getLayoutParams().width;
                    }
                }

                // Add the views to our toolbar
                toolbar.addView(leftButtonContainer);
                // Don't show address bar.
                // toolbar.addView(edittext);
                toolbar.addView(rightButtonContainer);

                if (title != null) {
                    int titleMargin = Math.max(
                            leftContainerWidth, rightContainerWidth);

                    FrameLayout.LayoutParams titleParams
                            = (FrameLayout.LayoutParams) title.getLayoutParams();
                    titleParams.setMargins(titleMargin, 0, titleMargin, 0);
                    toolbar.addView(title);
                }

                if (features.fullscreen) {
                    // If full screen mode, we have to add inAppWebView before adding toolbar.
                    main.addView(inAppWebView);
                }

                // Don't add the toolbar if its been disabled
                if (features.location) {
                    // Add our toolbar to our main view/layout
                    main.addView(toolbar);
                }

                if (!features.fullscreen) {
                    // If not full screen, we add inAppWebView after adding toolbar.
                    main.addView(inAppWebView);
                }

                WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
                lp.copyFrom(dialog.getWindow().getAttributes());
                lp.width = WindowManager.LayoutParams.MATCH_PARENT;
                lp.height = WindowManager.LayoutParams.MATCH_PARENT;

                progressBar = new ProgressBar(cordova.getActivity(), null,android.R.attr.progressBarStyle);
                progressBar.setIndeterminate(true);
                progressBar.setVisibility(View.VISIBLE);

                rlRoot.addView(main, rlp);

                RelativeLayout.LayoutParams progresslp = new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
                progresslp.addRule(RelativeLayout.CENTER_IN_PARENT, RelativeLayout.TRUE);
                rlRoot.addView(progressBar, progresslp);

                dialog.setContentView(rlRoot, rlp);
                dialog.show();
                dialog.getWindow().setAttributes(lp);
                // the goal of openhidden is to load the url and not display it
                // Show() needs to be called to cause the URL to be loaded
                if(features.hidden) {
                    dialog.hide();
                }
            }
        };
        this.cordova.getActivity().runOnUiThread(runnable);
        return "";
    }

    /**
     * Convert our DIP units to Pixels
     *
     * @return int
     */
    private int dpToPixels(int dipValue) {
        int value = (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                (float) dipValue,
                cordova.getActivity().getResources().getDisplayMetrics()
        );

        return value;
    }

    private int hexStringToColor(String hex) {
        int result = 0;

        if (hex != null && !hex.isEmpty()) {
            if (hex.charAt(0) == '#') {
                hex = hex.substring(1);
            }

            // No alpha, that's fine, we will just attach ff.
            if (hex.length() < 8) {
                hex += "ff";
            }

            result = (int) Long.parseLong(hex, 16);

            // Almost done, but Android color code is in form of ARGB instead of
            // RGBA, so we gotta shift it a bit.
            int alpha = (result & 0xff) << 24;
            result = result >> 8 & 0xffffff | alpha;
        }

        return result;
    }

    /**
    * This is a rather unintuitive helper method to load images. The reason why this method exists
    * is because due to some service limitations, one may not be able to add images to native
    * resource bundle. So this method offers a way to load image from www contents instead.
    * However loading from native resource bundle is already preferred over loading from www. So
    * if name is given, then it simply loads from resource bundle and the other two parameters are
    * ignored. If name is not given, then altPath is assumed to be a file path _under_ www and
    * altDensity is the desired density of the given image file, because without native resource
    * bundle, we can't tell what density the image is supposed to be so it needs to be given
    * explicitly.
    */
    private Drawable getImage(String name, String altPath, double altDensity) throws IOException {
        Drawable result = null;
        Resources activityRes = cordova.getActivity().getResources();

        if (name != null) {
            int id = activityRes.getIdentifier(name, "drawable",
                    cordova.getActivity().getPackageName());
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
                result = activityRes.getDrawable(id);
            } else {
                result = activityRes.getDrawable(id, cordova.getActivity().getTheme());
            }
        } else if (altPath != null) {
            File file = new File("www", altPath);
            InputStream is = null;
            try {
                is = cordova.getActivity().getAssets().open(file.getPath());
                Bitmap bitmap = BitmapFactory.decodeStream(is);
                bitmap.setDensity((int) (DisplayMetrics.DENSITY_MEDIUM * altDensity));
                result = new BitmapDrawable(activityRes, bitmap);
            } finally {
                // Make sure we close this input stream to prevent resource leak.
                try {
                    is.close();
                } catch (Exception e) {}
            }
        }
        return result;
    }

    private void setButtonImages(View view, BrowserButton buttonProps, int disabledAlpha) {
        Drawable normalDrawable = null;
        Drawable disabledDrawable = null;
        Drawable pressedDrawable = null;

        CharSequence description = view.getContentDescription();

        if (buttonProps.image != null || buttonProps.wwwImage != null) {
            try {
                normalDrawable = getImage(buttonProps.image, buttonProps.wwwImage,
                        buttonProps.wwwImageDensity);
                ViewGroup.LayoutParams params = view.getLayoutParams();
                params.width = normalDrawable.getIntrinsicWidth();
                params.height = normalDrawable.getIntrinsicHeight();
            } catch (Resources.NotFoundException e) {
                emitError(ERR_LOADFAIL,
                        String.format("Image for %s, %s, failed to load",
                                description, buttonProps.image));
            } catch (IOException ioe) {
                emitError(ERR_LOADFAIL,
                        String.format("Image for %s, %s, failed to load",
                                description, buttonProps.wwwImage));
            }
        } else {
            emitWarning(WRN_UNDEFINED,
                    String.format("Image for %s is not defined. Button will not be shown",
                            description));
        }

        if (buttonProps.imagePressed != null || buttonProps.wwwImagePressed != null) {
            try {
                pressedDrawable = getImage(buttonProps.imagePressed, buttonProps.wwwImagePressed,
                        buttonProps.wwwImageDensity);
            } catch (Resources.NotFoundException e) {
                emitError(ERR_LOADFAIL,
                        String.format("Pressed image for %s, %s, failed to load",
                                description, buttonProps.imagePressed));
            } catch (IOException e) {
                emitError(ERR_LOADFAIL,
                        String.format("Pressed image for %s, %s, failed to load",
                                description, buttonProps.wwwImagePressed));
            }
        } else {
            emitWarning(WRN_UNDEFINED,
                    String.format("Pressed image for %s is not defined.",
                            description));
        }

        if (normalDrawable != null) {
            // Create the disabled state drawable by fading the normal state
            // drawable. Drawable.setAlpha() stopped working above Android 4.4
            // so we gotta bring out some bitmap magic. Credit goes to:
            // http://stackoverflow.com/a/7477572
            Bitmap enabledBitmap = ((BitmapDrawable) normalDrawable).getBitmap();
            Bitmap disabledBitmap = Bitmap.createBitmap(
                    normalDrawable.getIntrinsicWidth(),
                    normalDrawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(disabledBitmap);

            Paint paint = new Paint();
            paint.setAlpha(disabledAlpha);
            canvas.drawBitmap(enabledBitmap, 0, 0, paint);

            Resources activityRes = cordova.getActivity().getResources();
            disabledDrawable = new BitmapDrawable(activityRes, disabledBitmap);
        }

        StateListDrawable states = new StateListDrawable();
        if (pressedDrawable != null) {
            states.addState(
                new int[] {
                    android.R.attr.state_pressed
                },
                pressedDrawable
            );
        }
        if (normalDrawable != null) {
            states.addState(
                new int[] {
                    android.R.attr.state_enabled
                },
                normalDrawable
            );
        }
        if (disabledDrawable != null) {
            states.addState(
                new int[] {},
                disabledDrawable
            );
        }

        setBackground(view, states);
    }

    private void setBackground(View view, Drawable drawable) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
            view.setBackgroundDrawable(drawable);
        } else {
            view.setBackground(drawable);
        }
    }

    private Button createButton(BrowserButton buttonProps, String description,
            View.OnClickListener listener) {
        Button result = null;
        if (buttonProps != null) {
            result = new Button(cordova.getActivity());
            result.setContentDescription(description);
            result.setLayoutParams(new LinearLayout.LayoutParams(
                    LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
            setButtonImages(result, buttonProps, DISABLED_ALPHA);
            if (listener != null) {
                result.setOnClickListener(listener);
            }
        } else {
            emitWarning(WRN_UNDEFINED,
                    String.format("%s is not defined. Button will not be shown.",
                            description));
        }
        return result;
    }

    /**
     * Create a new plugin success result and send it back to JavaScript
     *
     * @param obj a JSONObject contain event payload information
     */
    private void sendUpdate(JSONObject obj, boolean keepCallback) {
        sendUpdate(obj, keepCallback, PluginResult.Status.OK);
    }

    /**
     * Create a new plugin result and send it back to JavaScript
     *
     * @param obj a JSONObject contain event payload information
     * @param status the status code to return to the JavaScript environment
     */
    private void sendUpdate(JSONObject obj, boolean keepCallback, PluginResult.Status status) {
        if (callbackContext != null) {
            PluginResult result = new PluginResult(status, obj);
            result.setKeepCallback(keepCallback);
            callbackContext.sendPluginResult(result);
            if (!keepCallback) {
                callbackContext = null;
            }
        }
    }

    public static interface PageLoadListener {
        public void onPageFinished(String url, boolean canGoBack,
                boolean canGoForward);
    }

    /**
     * The webview client receives notifications about appView
     */
    public class ThemeableBrowserClient extends WebViewClient {
        private static final String postUrlPrefix = "http://cdn.tiegushi.com/posts/";
        PageLoadListener callback;
        CordovaWebView webView;

        /**
         * Constructor.
         *
         * @param webView
         * @param callback
         */
        public ThemeableBrowserClient(CordovaWebView webView,
                PageLoadListener callback) {
            this.webView = webView;
            this.callback = callback;
        }

        /**
         * Override the URL that should be loaded
         *
         * This handles a small subset of all the URIs that would be encountered.
         *
         * @param webView
         * @param url
         */
        @Override
        public boolean shouldOverrideUrlLoading(WebView webView, String url) {
            if (url.startsWith(WebView.SCHEME_TEL)) {
                try {
                    Intent intent = new Intent(Intent.ACTION_DIAL);
                    intent.setData(Uri.parse(url));
                    cordova.getActivity().startActivity(intent);
                    return true;
                } catch (android.content.ActivityNotFoundException e) {
                    Log.e(LOG_TAG, "Error dialing " + url + ": " + e.toString());
                }
            } else if (url.startsWith("geo:") || url.startsWith(WebView.SCHEME_MAILTO) || url.startsWith("market:")) {
                try {
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.setData(Uri.parse(url));
                    cordova.getActivity().startActivity(intent);
                    return true;
                } catch (android.content.ActivityNotFoundException e) {
                    Log.e(LOG_TAG, "Error with " + url + ": " + e.toString());
                }
            }
            // If sms:5551212?body=This is the message
            else if (url.startsWith("sms:")) {
                try {
                    Intent intent = new Intent(Intent.ACTION_VIEW);

                    // Get address
                    String address = null;
                    int parmIndex = url.indexOf('?');
                    if (parmIndex == -1) {
                        address = url.substring(4);
                    } else {
                        address = url.substring(4, parmIndex);

                        // If body, then set sms body
                        Uri uri = Uri.parse(url);
                        String query = uri.getQuery();
                        if (query != null) {
                            if (query.startsWith("body=")) {
                                intent.putExtra("sms_body", query.substring(5));
                            }
                        }
                    }
                    intent.setData(Uri.parse("sms:" + address));
                    intent.putExtra("address", address);
                    intent.setType("vnd.android-dir/mms-sms");
                    cordova.getActivity().startActivity(intent);
                    return true;
                } catch (android.content.ActivityNotFoundException e) {
                    Log.e(LOG_TAG, "Error sending sms " + url + ":" + e.toString());
                }
            }
            else if (url.startsWith(postUrlPrefix)) {
              int idxStart = postUrlPrefix.length();
              String postId = url.substring(idxStart);
              Log.i(LOG_TAG, "access posts: " + postId);

              try {
                  JSONObject obj = new JSONObject();
                  obj.put("type", "toPost");
                  obj.put("postId", postId);
                  sendUpdate(obj, true);
              } catch (JSONException ex) {
                  Log.e(LOG_TAG, "postId passed in has caused a JSON error.");
              }
              return true;
            }
            return false;
        }


        /*
         * onPageStarted fires the LOAD_START_EVENT
         *
         * @param view
         * @param url
         * @param favicon
         */
        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            super.onPageStarted(view, url, favicon);
            String newloc = "";
            if (url.startsWith("http:") || url.startsWith("https:") || url.startsWith("file:")) {
                newloc = url;
            }
            else
            {
                // Assume that everything is HTTP at this point, because if we don't specify,
                // it really should be.  Complain loudly about this!!!
                Log.e(LOG_TAG, "Possible Uncaught/Unknown URI");
                newloc = "http://" + url;
            }

            // Update the UI if we haven't already
            if (!newloc.equals(edittext.getText().toString())) {
                edittext.setText(newloc);
            }

            try {
                JSONObject obj = new JSONObject();
                obj.put("type", LOAD_START_EVENT);
                obj.put("url", newloc);
                sendUpdate(obj, true);
            } catch (JSONException ex) {
                Log.e(LOG_TAG, "URI passed in has caused a JSON error.");
            }
        }

        public void onPageFinished(WebView view, String url) {
            super.onPageFinished(view, url);

            try {
                JSONObject obj = new JSONObject();
                obj.put("type", LOAD_STOP_EVENT);
                obj.put("url", url);

                sendUpdate(obj, true);

                if (this.callback != null) {
                    this.callback.onPageFinished(url, view.canGoBack(),
                            view.canGoForward());
                }
            } catch (JSONException ex) {
            }
        }

        public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
            super.onReceivedError(view, errorCode, description, failingUrl);

            try {
                JSONObject obj = new JSONObject();
                obj.put("type", LOAD_ERROR_EVENT);
                obj.put("url", failingUrl);
                obj.put("code", errorCode);
                obj.put("message", description);

                sendUpdate(obj, true, PluginResult.Status.ERROR);
            } catch (JSONException ex) {
            }
        }
    }

    /**
     * Like Spinner but will always trigger onItemSelected even if a selected
     * item is selected, and always ignore default selection.
     */
    public class MenuSpinner extends Spinner {
        private OnItemSelectedListener listener;

        public MenuSpinner(Context context) {
            super(context);
        }

        @Override
        public void setSelection(int position) {
            super.setSelection(position);

            if (listener != null) {
                listener.onItemSelected(null, this, position, 0);
            }
        }

        @Override
        public void setOnItemSelectedListener(OnItemSelectedListener listener) {
            this.listener = listener;
        }
    }

    /**
     * Extension of ArrayAdapter. The only difference is that it hides the
     * selected text that's shown inside spinner.
     * @param <T>
     */
    private static class HideSelectedAdapter<T> extends ArrayAdapter {

        public HideSelectedAdapter(Context context, int resource, T[] objects) {
            super(context, resource, objects);
        }

        public View getView (int position, View convertView, ViewGroup parent) {
            View v = super.getView(position, convertView, parent);
            v.setVisibility(View.GONE);
            return v;
        }
    }


    /**
     * A class to hold parsed option properties.
     */
    private static class Options {
        public boolean location = true;
        public boolean hidden = false;
        public boolean clearcache = false;
        public boolean clearsessioncache = false;
        public boolean zoom = true;
        public boolean hardwareback = true;

        public Toolbar toolbar;
        public Title title;
        public BrowserButton backButton;
        public BrowserButton forwardButton;
        public BrowserButton closeButton;
        public BrowserMenu menu;
        public BrowserButton[] customButtons;
        public boolean backButtonCanClose;
        public boolean disableAnimation;
        public boolean fullscreen;
    }

    private static class Event {
        public String event;
    }

    private static class EventLabel extends Event {
        public String label;
        public String image;

        public String toString() {
            return label;
        }
    }

    private static class BrowserButton extends Event {
        public String image;
        public String wwwImage;
        public String imagePressed;
        public String wwwImagePressed;
        public double wwwImageDensity = 1;
        public String align = ALIGN_LEFT;
    }

    private static class BrowserMenu extends BrowserButton {
        public EventLabel[] items;
    }

    private static class Toolbar {
        public int height = TOOLBAR_DEF_HEIGHT;
        public String color;
        public String image;
        public String wwwImage;
        public double wwwImageDensity = 1;
    }

    private static class Title {
        public String color;
        public String staticText;
        public boolean showPageTitle;
    }

    public static class ShareMenuAdapter extends BaseAdapter {

      private Context mContext;

      private Options features;

      public ShareMenuAdapter(Context c, Options os) {
          mContext = c;
          features = os;
      }

      public int getCount() {
          return features.menu.items.length;
      }

      public Object getItem(int position) {
          return position;
      }

      public long getItemId(int position) {
          return 0;
      }

      public View getView(int position, View convertView, ViewGroup parent) {
          LinearLayout llRoot;
          if (convertView == null) {
            llRoot = new LinearLayout(mContext);
            llRoot.setOrientation(LinearLayout.VERTICAL);
            GridView.LayoutParams llParams = new GridView.LayoutParams(200, 150);
            llRoot.setLayoutParams(llParams);

            ImageView ivIcon = new ImageView(mContext);
            LinearLayout.LayoutParams llIcon = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT,LinearLayout.LayoutParams.WRAP_CONTENT);
            llIcon.gravity = Gravity.CENTER_HORIZONTAL;
            ivIcon.setLayoutParams(llIcon);
            Drawable result = null;
            Resources activityRes = mContext.getResources();
            String name = features.menu.items[position].image;
            if (name != null) {
                int id = activityRes.getIdentifier(name, "drawable",
                        mContext.getPackageName());
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
                    result = activityRes.getDrawable(id);
                } else {
                    result = activityRes.getDrawable(id, mContext.getTheme());
                }
            }
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
                ivIcon.setBackgroundDrawable(result);
            } else {
                ivIcon.setBackground(result);
            }
            llRoot.addView(ivIcon);

            TextView tvLabel = new TextView(mContext);
            LinearLayout.LayoutParams llLabel = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            llLabel.gravity = Gravity.CENTER_HORIZONTAL;
            tvLabel.setLayoutParams(llLabel);
            tvLabel.setText(features.menu.items[position].label);
            llRoot.addView(tvLabel);
          } else {
              llRoot = (LinearLayout) convertView;
          }
          return llRoot;
      }
  }
}
