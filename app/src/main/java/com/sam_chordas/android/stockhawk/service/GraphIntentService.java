package com.sam_chordas.android.stockhawk.service;

import android.app.IntentService;
import android.content.ContentValues;
import android.content.Intent;
import android.os.Bundle;

import com.google.android.gms.gcm.GcmNetworkManager;
import com.google.android.gms.gcm.TaskParams;
import com.sam_chordas.android.stockhawk.data.QuoteColumns;
import com.sam_chordas.android.stockhawk.data.QuoteProvider;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

/**
 * Created by Abhilash on 4/3/2016.
 */
public class GraphIntentService extends IntentService {

    private OkHttpClient client = new OkHttpClient();

    public GraphIntentService() {
        super("GraphIntentService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {

        StringBuilder urlStringBuilder = new StringBuilder();

        Calendar c = Calendar.getInstance();
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        c.add(Calendar.DATE, 1);
        String endDate = df.format(c.getTime());
        c.add(Calendar.YEAR, -1);
        String startDate = df.format(c.getTime());


        String symbol = intent.getStringExtra("symbol");

        // Base URL for the Yahoo query
        urlStringBuilder.append("https://query.yahooapis.com/v1/public/yql?q=");

        try {
            urlStringBuilder.append(URLEncoder.encode("select * from yahoo.finance.historicaldata where symbol = \"" +
                    symbol
                    + "\" and startDate =\"" + startDate + "\"and endDate =\"" + endDate + "\"", "UTF-8").replace("+", "%20"));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        urlStringBuilder.append("&format=json&diagnostics=true&env=store%3A%2F%2Fdatatables.org%2Falltableswithkeys&callback=");
        String urlString;
        String getResponse = null;
        int result = GcmNetworkManager.RESULT_FAILURE;
        if (urlStringBuilder != null) {
            urlString = urlStringBuilder.toString();
            try {
                getResponse = fetchData(urlString);
                result = GcmNetworkManager.RESULT_SUCCESS;
            } catch (IOException e) {
                e.printStackTrace();
            }


            String graphValues = GetGraphValues(getResponse);
            ContentValues contentValues = new ContentValues();
            contentValues.put(QuoteColumns.GRAPHVALUES, graphValues);
            getApplicationContext().getContentResolver().update(QuoteProvider.Quotes.withSymbol(symbol),
                    contentValues, null, null);
        }
    }


    private String GetGraphValues(String getResponse) {

        String graphValues = null;
        try {
            JSONObject jsonObject = new JSONObject(getResponse);
            JSONObject JOquery = jsonObject.getJSONObject("query");
            JSONObject JOresult = JOquery.getJSONObject("results");
            JSONArray JAquote = JOresult.getJSONArray("quote");
            graphValues = JAquote.toString();
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return graphValues;
    }

    String fetchData(String url) throws IOException {
        Request request = new Request.Builder()
                .url(url)
                .build();

        Response response = client.newCall(request).execute();
        return response.body().string();
    }

}
