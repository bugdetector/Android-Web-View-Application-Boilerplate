package com.webviewapp.app;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;
import java.util.Scanner;

/**
 * Handles HttpRequests.
 */
public class URLRequestHandler {
    private String response;
    private final String URL;

    private final Map<String, String> headers;
    private final byte[] byteData;
    public URLRequestHandler(
            String data,
            String url,
            Map<String, String> headers
    ){
        byteData = data.getBytes();
        this.URL = url;
        this.headers = headers;
    }
    public boolean getResponseMessage(){
        try {
            URL url = new URL(URL);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("charset", "utf-8");
            connection.setRequestProperty("Content-Length", Integer.toString(byteData.length));
            if(headers != null){
                headers.forEach(connection::setRequestProperty);
            }
            DataOutputStream outputStream = new DataOutputStream(connection.getOutputStream());
            outputStream.write(byteData);
            try {
                if (connection.getResponseCode() != 200) {
                    return false;
                }
            } catch (IOException e) {
                return false;
            }
            Scanner scanner = new Scanner(connection.getInputStream());
            StringBuilder builder = new StringBuilder();
            while (scanner.hasNext()) {
                builder.append(scanner.nextLine());
            }
            this.response = builder.toString();
            connection.disconnect();
            return true;
        }catch (Exception e){
            e.printStackTrace();
            return false;
        }
    }
    public String getResponse() {
        return response;
    }
}
