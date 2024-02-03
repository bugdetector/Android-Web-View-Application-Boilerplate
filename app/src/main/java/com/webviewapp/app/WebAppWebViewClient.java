package com.webviewapp.app;

import android.content.Intent;
import android.net.Uri;
import android.webkit.CookieManager;
import android.webkit.WebView;
import android.webkit.WebViewClient;

class WebAppWebViewClient extends WebViewClient {
    private final MainActivity activity;
    private final String host;
    private final String cookieStorageHost;


    public  WebAppWebViewClient(MainActivity activity){
        this.activity = activity;
        this.host = activity.getApplicationContext().getString(R.string.host);
        this.cookieStorageHost = activity.getApplicationContext().getString(R.string.cookie_storage_host);
    }
    @Override
    public boolean shouldOverrideUrlLoading(WebView view, String url) {
        Uri uri = Uri.parse(url);
        if (url.startsWith("file:") || uri.getHost() != null && uri.getHost().endsWith(this.host)) {
            return false;
        }
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        view.getContext().startActivity(intent);
        return true;
    }

    @Override
    public void onPageFinished(WebView view, String url){
        String sessionToken = getSessionToken();
        if(sessionToken != null){
            activity.askNotificationPermission(sessionToken);
        }
    }

    public String getSessionToken(){
        return getCookie(this.cookieStorageHost, "session-token");
    }

    public String getCookie(String siteName,String cookieName){
        String cookieValue = null;

        CookieManager cookieManager = CookieManager.getInstance();
        String cookies = cookieManager.getCookie(siteName);
        if(cookies == null){
            return null;
        }
        String[] splitCookies = cookies.split(";");
        for (String cookieParam : splitCookies ){
            if(cookieParam.contains(cookieName)){
                String[] values = cookieParam.split("=");
                cookieValue = values[1];
                break;
            }
        }
        return cookieValue;
    }
}
