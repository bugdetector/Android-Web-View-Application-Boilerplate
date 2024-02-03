package com.webviewapp.app;

import android.Manifest;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.DownloadManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.GeolocationPermissions;
import android.webkit.URLUtil;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.firebase.messaging.FirebaseMessaging;

public class MainActivity extends AppCompatActivity {
    private WebView mWebView;
    private ValueCallback<Uri[]> afterLollipop;

    private ActivityResultLauncher<String> notificationRequestPermissionLauncher;
    private ActivityResultLauncher<String> locationRequestPermissionLauncher;

    String geolocationOrigin;
    GeolocationPermissions.Callback geolocationCallback;
    @Override
    @SuppressLint("SetJavaScriptEnabled")
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mWebView = findViewById(R.id.activity_main_webview);

        mWebView.setScrollBarStyle(View.SCROLLBARS_INSIDE_OVERLAY);
        mWebView.getSettings().setCacheMode(WebSettings.LOAD_CACHE_ELSE_NETWORK);
        mWebView.getSettings().setDomStorageEnabled(true);
        mWebView.getSettings().setJavaScriptEnabled(true);
        mWebView.getSettings().setGeolocationEnabled(true);
        mWebView.getSettings().setAllowFileAccess(true);
        mWebView.getSettings().setAllowContentAccess(true);
        mWebView.getSettings().setSaveFormData(true);
        mWebView.getSettings().setCacheMode(WebSettings.LOAD_NO_CACHE);
        mWebView.getSettings().setUserAgentString("android-web-view");

        mWebView.setWebViewClient(new WebAppWebViewClient(
                this
        ));
        mWebView.setWebChromeClient(new WebChromeClient(){
            // For Android > 5.0
            @TargetApi(Build.VERSION_CODES.S)
            public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> filePathCallback, WebChromeClient.FileChooserParams fileChooserParams) {

                afterLollipop = filePathCallback;
                startActivityForResult(fileChooserParams.createIntent(), 101);

                return true;
            }
            public void onGeolocationPermissionsShowPrompt(String origin, GeolocationPermissions.Callback callback) {
                askLocationPermission(origin, callback);
            }
        });

        String host = getString(R.string.host);
        String loadUrl = host.startsWith("http") ? host : "https://" + host;
        Bundle extras = getIntent().getExtras();
        if(extras != null){
            loadUrl = extras.getString("url", loadUrl);
        }
        mWebView.loadUrl(loadUrl);

        mWebView.setDownloadListener((url, userAgent, contentDisposition, mimeType, contentLength) -> {

            DownloadManager.Request request = new DownloadManager.Request(
                    Uri.parse(url));
            request.setMimeType(mimeType);
            String cookies = CookieManager.getInstance().getCookie(url);
            request.addRequestHeader("cookie", cookies);
            request.addRequestHeader("User-Agent", userAgent);
            request.setDescription(getString(R.string.downloading));
            request.setTitle(URLUtil.guessFileName(url, contentDisposition,
                    mimeType));
            request.allowScanningByMediaScanner();
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            request.setDestinationInExternalPublicDir(
                    Environment.DIRECTORY_DOWNLOADS, URLUtil.guessFileName(
                            url, contentDisposition, mimeType));
            DownloadManager dm = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
            dm.enqueue(request);
            Toast.makeText(getApplicationContext(), getString(R.string.downloading),
                    Toast.LENGTH_LONG).show();
        });

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Create channel to show notifications.
            String channelId  = getString(R.string.default_notification_channel_id);
            String channelName = getString(R.string._default);
            NotificationManager notificationManager =
                    getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(new NotificationChannel(channelId,
                    channelName, NotificationManager.IMPORTANCE_DEFAULT));
        }

        notificationRequestPermissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
            if (isGranted) {
                subscribeFCMToken(
                        ((WebAppWebViewClient) mWebView.getWebViewClient()).getSessionToken()
                );
            }
        });

        locationRequestPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> geolocationCallback.invoke(geolocationOrigin, isGranted, false)
        );
    }

    @Override
    public void onBackPressed() {
        if(mWebView.canGoBack()) {
            mWebView.goBack();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
        if (requestCode == 101 && resultCode == RESULT_OK) {
            afterLollipop.onReceiveValue(WebChromeClient.FileChooserParams.parseResult(resultCode, intent));
            afterLollipop = null;
        }

    }

    public void askNotificationPermission(String sessionToken) {
        // This is only necessary for API Level > 33 (TIRAMISU)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) ==
                    PackageManager.PERMISSION_GRANTED) {
                subscribeFCMToken(sessionToken);
            } else {
                // Directly ask for the permission
                notificationRequestPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS);
            }
        }
    }

    public void askLocationPermission(String origin, GeolocationPermissions.Callback callback) {
        // This is only necessary for API Level > 33 (TIRAMISU)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) ==
                    PackageManager.PERMISSION_GRANTED) {
                callback.invoke(origin, true, false);
            } else {
                geolocationOrigin = origin;
                this.geolocationCallback = callback;
                locationRequestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION);
            }
        }
    }

    private void subscribeFCMToken(String sessionToken){
        FirebaseMessaging.getInstance().getToken()
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        Log.w(FirebaseMessagingService.TAG, "Fetching FCM registration token failed", task.getException());
                        return;
                    }

                    // Get new FCM registration token
                    String token = task.getResult();
                    if(sessionToken != null){
                        SharedPreferences sharedPref = getApplicationContext().getSharedPreferences(
                                FirebaseTokenSender.SESSION_TOKEN, Context.MODE_PRIVATE
                        );
                        SharedPreferences.Editor editor = sharedPref.edit();
                        editor.putString(FirebaseTokenSender.SESSION_TOKEN, sessionToken);
                        editor.apply();
                        new Thread(new FirebaseTokenSender(
                                getApplicationContext(),
                                token
                        )).start();
                    }

                    Log.d(FirebaseMessagingService.TAG, "Firebase token: " + token);
                });
    }
}
