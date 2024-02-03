package com.webviewapp.app;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Objects;

public class FirebaseTokenSender implements Runnable{
    public static final String SESSION_TOKEN = "SESSION_TOKEN";

    private final String tokenRegisterUrl;
    private final String sessionToken;
    private final String token;
    public FirebaseTokenSender(
            Context context,
            String token
    ){
        this.token = token;
        SharedPreferences sharedPref = context.getSharedPreferences(
                FirebaseTokenSender.SESSION_TOKEN, Context.MODE_PRIVATE
        );
        sessionToken = sharedPref.getString(
                FirebaseTokenSender.SESSION_TOKEN,
                null
        );
        tokenRegisterUrl = context.getString(R.string.firebase_token_register_url);
    }
    public void run(){
        if(sessionToken != null){
            try{
                JSONObject tokenRequest = new JSONObject();
                tokenRequest.put("token", token);
                URLRequestHandler requestHandler = new URLRequestHandler(
                        tokenRequest.toString(),
                        tokenRegisterUrl,
                        new HashMap<>() {{
                            put("Authorization", "Bearer " + sessionToken);
                        }}
                );
                requestHandler.getResponseMessage();
            } catch (JSONException exception){
                Log.e(this.getClass().getName(), Objects.requireNonNull(exception.getMessage()));
            }
        }
    }
}
