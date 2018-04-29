package com.pinterest.android.pdk;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.Signature;
import android.net.Uri;
import android.os.Build;
import android.text.TextUtils;
import android.graphics.Bitmap;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;

import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;


import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class PDKClient {

    public static final String PDKCLIENT_VERSION_CODE = "1.0";

    public static final String PDKCLIENT_PERMISSION_READ_PUBLIC = "read_public";
    public static final String PDKCLIENT_PERMISSION_WRITE_PUBLIC = "write_public";
    public static final String PDKCLIENT_PERMISSION_READ_RELATIONSHIPS = "read_relationships";
    public static final String PDKCLIENT_PERMISSION_WRITE_RELATIONSHIPS = "write_relationships";

    public static final String PDK_QUERY_PARAM_FIELDS = "fields";
    public static final String PDK_QUERY_PARAM_CURSOR = "cursor";

    private static final String PDKCLIENT_EXTRA_APPID = "PDKCLIENT_EXTRA_APPID";
    private static final String PDKCLIENT_EXTRA_APPNAME = "PDKCLIENT_EXTRA_APPNAME";
    private static final String PDKCLIENT_EXTRA_PERMISSIONS = "PDKCLIENT_EXTRA_PERMISSIONS";
    private static final String PDKCLIENT_EXTRA_RESULT = "PDKCLIENT_EXTRA_RESULT";

    private static final String PDK_SHARED_PREF_FILE_KEY = "com.pinterest.android.pdk.PREF_FILE_KEY";
    private static final String PDK_SHARED_PREF_TOKEN_KEY = "PDK_SHARED_PREF_TOKEN_KEY";
    private static final String PDK_SHARED_PREF_SCOPES_KEY = "PDK_SHARED_PREF_SCOPES_KEY";
    private static final int PDKCLIENT_REQUEST_CODE = 8772;
    private static final String VOLLEY_TAG = "volley_tag";

    private static final String PROD_BASE_API_URL = "https://api.pinterest.com/v1/";
    private static final String PROD_WEB_OAUTH_URL = "https://api.pinterest.com/oauth/";
    private static final String ME = "me/";
    private static final String USER = "users/";
    private static final String PINS = "pins/";
    private static final String BOARDS = "boards/";
    private static final String LIKES = "likes/";
    private static final String FOLLOWERS = "followers/";
    private static final String FOLLOWING = "following/";
    private static final String INTERESTS = "interests/";


    private static boolean _debugMode;
    private static String _clientId;
    private static Context _context;
    private static String _accessToken;
    private static Set<String> _scopes;
    private static Set<String> _requestedScopes;
    private static PDKClient _mInstance = null;
    private PDKCallback _authCallback;
    private PDKCallback _onConnectCallback;
    private String baseUrl = PROD_BASE_API_URL;
    private String webOAuth = PROD_WEB_OAUTH_URL;
    private static RequestQueue _requestQueue;

    private static boolean _isConfigured;
    private static boolean _isAuthenticated = false;

    private static final String PINTEREST_PACKAGE = "com.pinterest";
    private static final String PINTEREST_OAUTH_ACTIVITY = "com.pinterest.sdk.PinterestOauthActivity";
    private static final String PINTEREST_SIGNATURE_HASH = "b6a74dbcb894b0f73d8c485c72eb1247a8f027ca";
    private static final int MAX_PIXELS_FOR_B64 = 3150000;

    private PDKClient() {

    }

    public static PDKClient getInstance() {
        if (_mInstance == null)
        {
            _mInstance = new PDKClient();
            _requestQueue = getRequestQueue();
        }
        return _mInstance;
    }

    public static PDKClient configureInstance(Context context, String clientId) {
        PDKClient._clientId = clientId;
        PDKClient._context = context.getApplicationContext();
        _isConfigured = true;

        _accessToken = restoreAccessToken();
        _scopes = restoreScopes();
        // _isAuthenticated = _accessToken != null; // Wrong! It may be expired
        return PDKClient.getInstance();
    }

    /**
     * Alternative method that allows to specify a custom base url (a mock server for example)
     */
    public static PDKClient configureInstance(Context context, String clientId, String baseUrl) {
        PDKClient instance = configureInstance(context, clientId);
        if(baseUrl != null) {
            instance.baseUrl = baseUrl;
        }
        return  instance;
    }

    /**
     * Alternative method that allows to specify a custom base url (a mock server for example)
     * and a custom OAuth page url
     */
    public static PDKClient configureInstance(Context context, String clientId, String baseUrl, String oauthUrl) {
        PDKClient instance = configureInstance(context, clientId);
        if(baseUrl != null) {
            instance.baseUrl = baseUrl;
        }
        if(oauthUrl != null) {
            instance.webOAuth = oauthUrl;
        }
        return  instance;
    }

    // ================================================================================
    // Getters/Setters
    // ================================================================================

    /**
     * Get state of debug mode
     *
     * @return true if enabled, false if disabled
     */
    public static boolean isDebugMode() {
        return _debugMode;
    }

    /**
     * Enable/disable debug mode which will print logs when there are issues.
     *
     * @param debugMode true to enabled, false to disable
     */
    public static void setDebugMode(boolean debugMode) {
        PDKClient._debugMode = debugMode;
    }

    /**
     * Set Oauth Access token
     */
    public void setAccessToken(String token) {
        _accessToken = token;
        saveAccessToken(_accessToken);
    }

    /**
     *  The oauth Token returned from the server upon authentication. If you store this in your
     *  app, please be sure to do so securely and be warned that it can expire
     *  at any time. If the token expires, you will need to re-authenticate and get a new token.
     */
    public String getAccessToken() {
        return _accessToken;
    }

    /**
     * Let the caller application to know if it is authenticated.
     */
    public static boolean isAuthenticated() { return _isAuthenticated; }

    // ================================================================================
    // API Interface
    // ================================================================================

    public void logout() {
        _accessToken = null;
        _scopes = null;
        cancelPendingRequests();
        saveAccessToken(null);
        saveScopes(null);
    }

    private static boolean checkAppNumber(Context context, String clientId) {
        Intent testIntent = new Intent(Intent.ACTION_VIEW);
        testIntent.setData(Uri.parse("pdk" + clientId + "://"));
        PackageManager pm = context.getPackageManager();
        List<ResolveInfo> activities = pm.queryIntentActivities(testIntent, 0);
        if (activities.size() == 0) {
            throw new IllegalStateException("No app responding to the Intent, please check the manifest to make sure the intent filter is set up properly.");
        }
        if (activities.size() != 1) return false;
        return true;
    }

    public void silentLogin (final Context context, final List<String> permissions, final PDKCallback callback) {
        login(context, true, permissions, callback);
    }

    public void login (final Context context, final List<String> permissions, final PDKCallback callback) {
        login(context, false, permissions, callback);
        /* // Old logic
        _authCallback = callback;
        if (!checkAppNumber(context, _clientId)) {
            if (callback != null) callback.onFailure(new PDKException("Unable to log in because multiple apps are responding to the login request."));
            return;
        }
        if (Utils.isEmpty(permissions)) {
            if (callback != null) callback.onFailure(new PDKException("Scopes cannot be empty"));
            return;
        }
        if (!(context instanceof Activity)) {
            if (callback != null) callback.onFailure(new PDKException("Please pass Activity context with login request"));
            return;
        }
        _requestedScopes = new HashSet<String>();
        _requestedScopes.addAll(permissions);
        if (!Utils.isEmpty(_accessToken) && !Utils.isEmpty(_scopes)) {
            getPath("oauth/inspect", null, new PDKCallback() {
                @Override
                public void onSuccess(PDKResponse response) {
                    if (verifyAccessToken(response.getData())) {
                        _isAuthenticated = true;
                        PDKClient.getInstance().getMe(_authCallback);
                    } else {
                        initiateLogin(context, permissions);
                    }
                }

                @Override
                public void onFailure(PDKException exception) {
                    initiateLogin(context, permissions);
                }
            });
        } else {
            initiateLogin(context, permissions);
        }
        */
    }

    public void onOauthResponse(int requestCode, int resultCode, Intent data) {
        if (requestCode == PDKCLIENT_REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                Utils.log("PDK: result - %s", data.getStringExtra(PDKCLIENT_EXTRA_RESULT));
                onOauthResponse(data.getStringExtra(PDKCLIENT_EXTRA_RESULT));
            } else {
                Utils.log("PDK: Authentication failed");
                _authCallback.onFailure(new PDKException("Authentication failed"));
            }
        }
    }

    public void onConnect(Context context) {
        onConnect(context,null);
    }
    public void onConnect(Context context, PDKCallback callback) {
        _onConnectCallback = callback;
        if (!(context instanceof Activity)) {
            if(_onConnectCallback != null) {
                _onConnectCallback.onExecutionAborted("Please pass Activity context with onConnect request");
            } else if (_authCallback != null) {
                _authCallback.onFailure(new PDKException("Please pass Activity context with onConnect request"));
            }
            return;
        }
        Activity activity = (Activity) context;
        boolean oAuthTriggered = false;
        if (Intent.ACTION_VIEW.equals(activity.getIntent().getAction())) {
            Uri uri = activity.getIntent().getData();
            if (uri != null && uri.toString().contains("pdk" + _clientId + "://")) {
                oAuthTriggered = true;
                onOauthResponse(uri.toString());
            }

        }
        if(_onConnectCallback != null && !oAuthTriggered) {
            _onConnectCallback.onExecutionAborted("It wasn't necessary to trigger the OAuth");
        }
    }

    public void getPath(String path, PDKCallback callback) {
        getPath(path, null, callback);
    }

    public void getPath(String path, HashMap<String, String> params, PDKCallback callback) {
        if (Utils.isEmpty(path)) {
            if (callback != null) callback.onFailure(new PDKException("Invalid path"));
            return;
        }
        String url = baseUrl + path;
        if (params == null) params = new HashMap<String, String>();
        if (callback != null) callback.setPath(path);
        if (callback != null) callback.setParams(params);
        getRequest(url, params, callback);
    }

    public void postPath(String path, HashMap<String, String> params, PDKCallback callback) {
        if (Utils.isEmpty(path)) {
            if (callback != null) callback.onFailure(new PDKException("Invalid path"));
            return;
        }
        if (callback != null) callback.setPath(path);
        String url = baseUrl + path;
        postRequest(url, params, callback);
    }

    public void deletePath(String path, PDKCallback callback) {
        if (Utils.isEmpty(path)) {
            if (callback != null) callback.onFailure(new PDKException("Invalid path"));
            return;
        }
        if (callback != null) callback.setPath(path);
        String url = baseUrl + path;
        deleteRequest(url, null, callback);
    }

    public void putPath(String path, HashMap<String, String> params, PDKCallback callback) {
        if (Utils.isEmpty(path)) {
            if (callback != null) callback.onFailure(new PDKException("Invalid path"));
            return;
        }
        if (callback != null) callback.setPath(path);
        String url = baseUrl + path;
        putRequest(url, params, callback);
    }


    //Authorized user Endpoints

    public void getMe(PDKCallback callback) {
        getPath(ME, callback);
    }

    public void getMe(String fields, PDKCallback callback) {
        getPath(ME, getMapWithFields(fields), callback);
    }

    public void getMyPins(String fields, PDKCallback callback) {
        String path =  ME + PINS;
        getPath(path, getMapWithFields(fields), callback);
    }

    public void getMyBoards(String fields, PDKCallback callback) {
        String path = ME + BOARDS;
        getPath(path, getMapWithFields(fields), callback);
    }

    public void getMyLikes(String fields, PDKCallback callback) {
        String path = ME + LIKES;
        getPath(path, getMapWithFields(fields), callback);
    }

    public void getMyFollowers(String fields, PDKCallback callback) {
        String path = ME + FOLLOWERS;
        getPath(path, getMapWithFields(fields), callback);
    }

    public void getMyFollowedUsers(String fields, PDKCallback callback) {
        String path = ME + FOLLOWING + USER;
        getPath(path, getMapWithFields(fields), callback);
    }

    public void getMyFollowedBoards(String fields, PDKCallback callback) {
        String path = ME + FOLLOWING + BOARDS;
        getPath(path, getMapWithFields(fields), callback);
    }

    public void getMyFollowedInterests(String fields, PDKCallback callback) {
        String path = ME + FOLLOWING + INTERESTS;
        getPath(path, getMapWithFields(fields), callback);
    }

    //User Endpoint

    public void getUser(String userId, String fields, PDKCallback callback) {
        if (Utils.isEmpty(userId)) {
            if (callback != null) callback.onFailure(new PDKException("Invalid user name/Id"));
            return;
        }
        String path =  USER + userId;
        getPath(path, getMapWithFields(fields), callback);
    }

    //Board Endpoints

    public void getBoard(String boardId, String fields, PDKCallback callback) {
        if (Utils.isEmpty(boardId)) {
            if (callback != null) callback.onFailure(new PDKException("Invalid board Id"));
            return;
        }
        String path =  BOARDS + boardId;
        getPath(path, getMapWithFields(fields), callback);
    }

    public void getBoardPins(String boardId, String fields, PDKCallback callback) {
        if (Utils.isEmpty(boardId)) {
            if (callback != null) callback.onFailure(new PDKException("Invalid board Id"));
            return;
        }
        String path =  BOARDS + boardId + "/" + PINS;
        getPath(path, getMapWithFields(fields), callback);
    }

    public void deleteBoard(String boardId, PDKCallback callback) {
        if (Utils.isEmpty(boardId)) {
            if (callback != null) callback.onFailure(new PDKException("Board Id cannot be empty"));
            return;
        }
        String path = BOARDS + boardId + "/";
        deletePath(path, callback);
    }

    public void createBoard(String name, String desc, PDKCallback callback) {
        if (Utils.isEmpty(name)) {
            if (callback != null) callback.onFailure(new PDKException("Board name cannot be empty"));
            return;
        }
        HashMap<String, String> params = new HashMap<String, String>();
        params.put("name", name);
        if (!Utils.isEmpty(desc)) params.put("description", desc);
        postPath(BOARDS, params, callback);
    }

    //Pin Endpoints

    public void getPin(String pinId, String fields, PDKCallback callback) {
        if (Utils.isEmpty(pinId)) {
            if (callback != null) callback.onFailure(new PDKException("Invalid pin Id"));
            return;
        }
        String path =  PINS + pinId;
        getPath(path, getMapWithFields(fields), callback);
    }

    public void createPin(String note, String boardId, String imageUrl, String link, PDKCallback callback) {
        if (Utils.isEmpty(note) || Utils.isEmpty(boardId) || Utils.isEmpty(imageUrl)) {
            if (callback != null) callback.onFailure(new PDKException("Board Id, note, Image cannot be empty"));
            return;
        }
        HashMap<String, String> params = new HashMap<String, String>();
        params.put("board", boardId);
        params.put("note", note);
        if (!Utils.isEmpty(link)) params.put("link", link);
        if (!Utils.isEmpty(imageUrl)) params.put("image_url", imageUrl);
        postPath(PINS, params, callback);
    }

    public void createPin(String note, String boardId, Bitmap image, String link, PDKCallback callback) {
        if (Utils.isEmpty(note) || Utils.isEmpty(boardId) || image == null) {
            if (callback != null) callback.onFailure(new PDKException("Board Id, note and Image cannot be empty"));
            return;
        }
        int numPixels = image.getHeight() * image.getWidth();
        if(numPixels > MAX_PIXELS_FOR_B64) {
            if (callback != null) callback.onFailure(new PDKException("Image too large to encode to Base 64."));
            return;
        }

        HashMap<String, String> params = new HashMap<String, String>();
        params.put("board", boardId);
        params.put("note", note);
        params.put("image_base64", Utils.base64String(image));
        if (!Utils.isEmpty(link)) params.put("link", link);
        postPath(PINS, params, callback);
    }

    public void deletePin(String pinId, PDKCallback callback) {
        if (Utils.isEmpty(pinId)) {
            if (callback != null) callback.onFailure(new PDKException("Pin Id cannot be empty"));
            return;
        }
        String path = PINS + pinId + "/";
        deletePath(path, callback);
    }


    // ================================================================================
    // Internal
    // ================================================================================

    /**
     * CAUTION: This should be called only after verifying the access token
     */
    private void saveTokenDetails(JSONObject token) {
        try {
            Set<String> appScopes = new HashSet<String>();
            JSONArray scopesArray = token.getJSONArray("scopes");
            int size = scopesArray.length();
            for (int i = 0; i < size; i++) {
                appScopes.add(scopesArray.get(i).toString());
            }
            _scopes = appScopes;
            saveScopes(appScopes);
        } catch (JSONException e) {
            Utils.loge("PDK: ", e.getLocalizedMessage());
        }
    }

    private void onOauthResponse(String result) {
        boolean errorFlag = false;
        if (!Utils.isEmpty(result)) {
            Uri uri = Uri.parse(result);
            if (uri.getQueryParameter("access_token") != null) {
                String token = uri.getQueryParameter("access_token");
                try {
                    token = java.net.URLDecoder.decode(token, "UTF-8");
                } catch (UnsupportedEncodingException e) {
                    errorFlag = true;
                    Utils.loge(e.getLocalizedMessage());
                }
                /* // Old logic
                _accessToken = token;
                _isAuthenticated = true;
                PDKClient.getInstance().getMe(_authCallback);
                saveAccessToken(_accessToken);
                */
                _accessToken = token; // Save the access token.
                saveAccessToken(token);
                Utils.log("Token retrieved and saved");
                // At this point we can be quite sure that the token is valid.
                // Let's check the permissions.
                getPath("oauth/inspect", null, new PDKCallback() {
                    @Override
                    public void onSuccess(PDKResponse response) {
                        if (verifyAccessToken(response.getData())) {
                            Utils.log("Token verified");
                            // Save access token and scopes
                            saveTokenDetails((JSONObject) response.getData());
                            Utils.log("Scopes saved");
                            _isAuthenticated = true;
                            if(_onConnectCallback != null) {
                                PDKClient.getInstance().getMe(_onConnectCallback);
                            } else {
                                PDKClient.getInstance().getMe(_authCallback);
                            }
                        } else {
                            if(_onConnectCallback != null) {
                                _onConnectCallback.onExecutionAborted("Token not valid");
                            } else if(_authCallback != null) {
                                _authCallback.onFailure(new PDKException("PDK: Token not valid"));
                            }
                        }
                    }

                    @Override
                    public void onFailure(PDKException exception) {
                        if(_onConnectCallback != null) {
                            _onConnectCallback.onFailure(exception);
                        } else if(_authCallback != null) {
                            _authCallback.onFailure(exception);
                        }
                    }
                });
            } else if (uri.getQueryParameter("error") != null) {
                errorFlag = true;
                String error = uri.getQueryParameter("error");
                Utils.loge("PDK: authentication error: %s", error);
            }
        }

        if (errorFlag) {
            if (_onConnectCallback != null) {
                _onConnectCallback.onFailure(new PDKException("PDK: authentication failed"));
            } else {
                _authCallback.onFailure(new PDKException("PDK: authentication failed"));
            }
        }
    }


    private void login (final Context context, final boolean silent, final List<String> permissions, final PDKCallback callback) {
        _authCallback = callback;
        if (!checkAppNumber(context, _clientId)) {
            if (callback != null) callback.onFailure(new PDKException("Unable to log in because multiple apps are responding to the login request."));
            return;
        }
        if (Utils.isEmpty(permissions)) {
            if (callback != null) callback.onFailure(new PDKException("Scopes cannot be empty"));
            return;
        }
        if (!(context instanceof Activity)) {
            if (callback != null) callback.onFailure(new PDKException("Please pass Activity context with login request"));
            return;
        }
        _requestedScopes = new HashSet<String>();
        _requestedScopes.addAll(permissions);
        Utils.log("Access token on login: "+_accessToken);
        Utils.log("Scopes on login: "+_scopes);
        if (!Utils.isEmpty(_accessToken) && !Utils.isEmpty(_scopes)) {
            getPath("oauth/inspect", null, new PDKCallback() {
                @Override
                public void onSuccess(PDKResponse response) {
                    if (verifyAccessToken(response.getData())) {
                        _isAuthenticated = true;
                        PDKClient.getInstance().getMe(_authCallback);
                    } else {
                        initiateLogin(context, permissions);
                    }
                }

                @Override
                public void onFailure(PDKException exception) {
                    initiateLogin(context, permissions);
                }
            });
        } else if(!silent) {
            // This maps the behaviour of the iOS PDK
            // See: https://github.com/pinterest/ios-pdk/blob/798ac68e083324195c05732c0dceeda8d363df0a/Pod/Classes/PDKClient.m#L193
            initiateLogin(context, permissions);
        } else if(silent && Utils.isEmpty(_accessToken)) {
            // silent was yes, but we did not have a cached token. that counts as a failure.
            // It's necessary to notify the caller application that the login was not successfull.
            callback.onFailure(new PDKException("PDK: Token not found"));
        }
    }

    private void initiateLogin(Context c, List<String> permissions) {
        if (pinterestInstalled(_context)) {
            Intent intent = createAuthIntent(_context, _clientId, permissions);
            openPinterestAppForLogin(c, intent, permissions);
        } else {
            initiateWebLogin(c, permissions);
        }
    }

    private void initiateWebLogin(Context c, List<String> permissions) {
        try {
            List paramList = new LinkedList<BasicNameValuePair>();
            paramList.add(new BasicNameValuePair("client_id", _clientId));
            paramList.add(new BasicNameValuePair("scope",  TextUtils.join(",", permissions)));
            paramList.add(new BasicNameValuePair("redirect_uri", "pdk" + _clientId + "://"));
            paramList.add(new BasicNameValuePair("response_type", "token"));

            String url =  Utils.getUrlWithQueryParams(webOAuth, paramList);
            Intent oauthIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            c.startActivity(oauthIntent);

        } catch (Exception e) {
            Utils.loge("PDK: Error initiating web oauth");
        }
    }

    private void openPinterestAppForLogin(Context c, Intent intent, List<String> permissions) {
        try {
            //Utils.log("PDK: starting Pinterest app for auth");
            ((Activity)c).startActivityForResult(intent, PDKCLIENT_REQUEST_CODE);
        } catch (ActivityNotFoundException e) {
            // Ideally this should not happen because intent is not null
            Utils.loge("PDK: failed to open Pinterest App for login");
            initiateWebLogin(c, permissions);
            return;
        }
        return;
    }

    private Intent createAuthIntent(Context context, String appId, List<String> permissions) {
        return new Intent()
            .setClassName(PINTEREST_PACKAGE, PINTEREST_OAUTH_ACTIVITY)
            .putExtra(PDKCLIENT_EXTRA_APPID, appId)
            .putExtra(PDKCLIENT_EXTRA_APPNAME, "appName")
            .putExtra(PDKCLIENT_EXTRA_PERMISSIONS, TextUtils.join(",", permissions));
    }


//    //validate Pinterest Activity and/or package integrity
//    private static Intent validateActivityIntent(Context context, Intent intent) {
//        if (intent == null) {
//            return null;
//        }
//
//        ResolveInfo resolveInfo = context.getPackageManager().resolveActivity(intent, 0);
//        if (resolveInfo == null) {
//            return null;
//        }
//
//        //validate pinterest app?
//        //        if (!appInfo.validateSignature(context, resolveInfo.activityInfo.packageName)) {
//        //            return null;
//        //        }
//
//        return intent;
//    }

    /**
     * Check if the device meets the requirements needed to pin using this library.
     *
     * @return true for supported, false otherwise
     */
    private static boolean meetsRequirements() {
        return Build.VERSION.SDK_INT >= 8;
    }

    /**
     * Check if the device has Pinterest installed that supports PinIt Button
     *
     * @param context Application or Activity context
     * @return true if requirements are met, false otherwise
     */
    private static boolean pinterestInstalled(final Context context) {
        if (!meetsRequirements())
            return false;

        boolean installed = false;
        try {
            PackageInfo info = context.getPackageManager().getPackageInfo(PINTEREST_PACKAGE, PackageManager.GET_SIGNATURES);
            if (info != null && info.versionCode >= 16) {
                //Utils.log("PDK versionCode:%s versionName:%s", info.versionCode,
                //    info.versionName);
                for (Signature signature : info.signatures) {
                    String signatureHash = Utils.sha1Hex(signature.toByteArray());
                    installed = signatureHash.equals(PINTEREST_SIGNATURE_HASH);
                }
            }
            if (!installed)
                Utils.log("PDK: Pinterest App not installed or version too low!");
        } catch (Exception e) {
            Utils.loge(e.getLocalizedMessage());
            installed = false;
        }
        return installed;
    }

    private void saveAccessToken(String accessToken) {
        SharedPreferences sharedPref = _context.getSharedPreferences(PDK_SHARED_PREF_FILE_KEY, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString(PDK_SHARED_PREF_TOKEN_KEY, accessToken);
        editor.commit();
    }

    private static String restoreAccessToken() {
        SharedPreferences sharedPref = _context.getSharedPreferences(PDK_SHARED_PREF_FILE_KEY, Context.MODE_PRIVATE);
        return sharedPref.getString(PDK_SHARED_PREF_TOKEN_KEY, null);
    }

    private void saveScopes(Set<String> perms) {
        SharedPreferences sharedPref = _context.getSharedPreferences(PDK_SHARED_PREF_FILE_KEY, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putStringSet(PDK_SHARED_PREF_SCOPES_KEY, perms);
        editor.commit();
    }

    private static Set<String> restoreScopes() {
        SharedPreferences sharedPref = _context.getSharedPreferences(PDK_SHARED_PREF_FILE_KEY, Context.MODE_PRIVATE);
        return sharedPref.getStringSet(PDK_SHARED_PREF_SCOPES_KEY, new HashSet<String>());
    }

    private static RequestQueue getRequestQueue() {
        if (_requestQueue == null) {
            _requestQueue = Volley.newRequestQueue(_context);
        }
        return _requestQueue;
    }

    private static <T> void addToRequestQueue(Request<T> req) {
        req.setTag(VOLLEY_TAG);
        getRequestQueue().add(req);
    }

    private static void cancelPendingRequests() {
        _requestQueue.cancelAll(VOLLEY_TAG);
    }

    private static boolean validateScopes(Set<String> requestedScopes) {
        return _scopes.equals(requestedScopes);
    }

    private HashMap<String, String> getMapWithFields(String fields) {
        HashMap map = new HashMap<String, String>();
        map.put(PDK_QUERY_PARAM_FIELDS, fields);
        return map;
    }

    private static Map<String, String> getHeaders() {
        Map<String, String> headers = new HashMap<String, String>();
        headers.put("User-Agent", String.format("PDK %s", PDKCLIENT_VERSION_CODE));
        return headers;
    }

    private boolean verifyAccessToken(Object obj) {
        boolean verified = false;
        String appId = "";
        Set<String> appScopes = new HashSet<String>();
        try {
            JSONObject jsonObject = (JSONObject)obj;
            if (jsonObject.has("app")) {
                JSONObject appObj = jsonObject.getJSONObject("app");
                if (appObj.has("id")) {
                    appId = appObj.getString("id");
                }
            }
            if (jsonObject.has("scopes")) {
                JSONArray scopesArray = jsonObject.getJSONArray("scopes");
                int size = scopesArray.length();
                for (int i = 0; i < size; i++) {
                    appScopes.add(scopesArray.get(i).toString());
                }
            }
        } catch (JSONException exception) {
            Utils.loge("PDK: ", exception.getLocalizedMessage());
        }
        if (!Utils.isEmpty(appScopes)) {
            saveScopes(appScopes);
        }
        if (!Utils.isEmpty(appId) && !Utils.isEmpty(appScopes)) {
            if (appId.equalsIgnoreCase(_clientId) && appScopes.equals(_requestedScopes)) {
                verified = true;
            }
        }
        return verified;
    }

    private static Request getRequest(String url, HashMap<String, String> params, PDKCallback callback) {
        Utils.log("PDK GET: %s", url);
        List paramList = new LinkedList<>();
        paramList.add(new BasicNameValuePair("access_token", _accessToken));
        if (!Utils.isEmpty(params)) {
            for (HashMap.Entry<String, String> e : params.entrySet()) {
                paramList.add(new BasicNameValuePair(e.getKey(), e.getValue()));
            }
        }
        url = Utils.getUrlWithQueryParams(url, paramList);

        if (callback == null) callback = new PDKCallback();
        PDKRequest request = new PDKRequest(Request.Method.GET, url, null, callback, getHeaders());
        addToRequestQueue(request);
        return request;
    }

    private static Request postRequest(String url, HashMap<String, String> params, PDKCallback callback) {
        Utils.log(String.format("PDK POST: %s", url));
        if (params == null) params = new HashMap<String, String>();

        List queryParams = new LinkedList<>();
        queryParams.add(new BasicNameValuePair("access_token", _accessToken));
        url = Utils.getUrlWithQueryParams(url, queryParams);

        if (callback == null) callback = new PDKCallback();
        PDKRequest request = new PDKRequest(Request.Method.POST, url, new JSONObject(params), callback, getHeaders());
        addToRequestQueue(request);
        return request;
    }

    private static Request deleteRequest(String url, HashMap<String, String> params, PDKCallback callback) {
        Utils.log(String.format("PDK DELETE: %s", url));

        List queryParams = new LinkedList<>();
        queryParams.add(new BasicNameValuePair("access_token", _accessToken));
        url = Utils.getUrlWithQueryParams(url, queryParams);

        if (callback == null) callback = new PDKCallback();

        PDKRequest request = new PDKRequest(Request.Method.DELETE, url, null, callback, getHeaders());
        request.setShouldCache(false);
        addToRequestQueue(request);
        return request;
    }

    private static Request putRequest(String url, HashMap<String, String> params, PDKCallback callback) {
        Utils.log(String.format("PDK PUT: %s", url));
        if (params == null) params = new HashMap<String, String>();

        List queryParams = new LinkedList<>();
        queryParams.add(new BasicNameValuePair("access_token", _accessToken));
        url = Utils.getUrlWithQueryParams(url, queryParams);

        if (callback == null) callback = new PDKCallback();
        PDKRequest request = new PDKRequest(Request.Method.PUT, url, new JSONObject(params), callback, getHeaders());
        addToRequestQueue(request);
        return request;
    }
}
