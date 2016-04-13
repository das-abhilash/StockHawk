package com.sam_chordas.android.stockhawk.rest;

import android.content.ContentProviderOperation;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.preference.PreferenceManager;
import android.util.Log;

import com.google.android.gms.gcm.GcmNetworkManager;
import com.sam_chordas.android.stockhawk.R;
import com.sam_chordas.android.stockhawk.data.QuoteColumns;
import com.sam_chordas.android.stockhawk.data.QuoteProvider;
import com.sam_chordas.android.stockhawk.service.GraphIntentService;
import com.sam_chordas.android.stockhawk.service.StockIntentService;
import com.sam_chordas.android.stockhawk.service.StockTaskService;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;


public class Utils {

    public Utils() {
    }

    private static String LOG_TAG = Utils.class.getSimpleName();

    public static boolean showPercent = true;
    private static Boolean isInsert = true;

    public static ArrayList quoteJsonToContentVals(String JSON, Context context) {
        ArrayList<ContentProviderOperation> batchOperations = new ArrayList<>(); //ContentProviderOperation ??
        JSONObject jsonObject = null;
        JSONArray resultsArray = null;
        Log.i(LOG_TAG, "GET FB: " + JSON);

        try {
            jsonObject = new JSONObject(JSON);
            if (jsonObject != null && jsonObject.length() != 0) {
                jsonObject = jsonObject.getJSONObject("query");
                int count = Integer.parseInt(jsonObject.getString("count"));
                if (count == 1) {
                    jsonObject = jsonObject.getJSONObject("results")
                            .getJSONObject("quote");
                    String str = jsonObject.getString("Bid");
                    String symbol = jsonObject.getString("symbol");
                    Cursor c = context.getContentResolver().query((QuoteProvider.Quotes.withSymbol(symbol)),
                            null, null, null, null);
                    if (c != null) {
                        if (c.getCount() != 0) isInsert = false;
                    }
                    // added by Abhilash
                    if ((str != "null" && !str.isEmpty())) {   // added by Abhilash to
                        batchOperations.add(buildBatchOperation(jsonObject, isInsert));
                        isInsert = true;
                        Intent graphIntentService = new Intent(context, GraphIntentService.class);
                        graphIntentService.putExtra("symbol", symbol);
                        context.startService(graphIntentService);

                    } else {
                        StockIntentService.invalidData = true;
                        StockIntentService.invalidSymbol = symbol;

                    }

                } else {
                    resultsArray = jsonObject.getJSONObject("results").getJSONArray("quote");

                    if (resultsArray != null && resultsArray.length() != 0) {
                        for (int i = 0; i < resultsArray.length(); i++) {
                            jsonObject = resultsArray.getJSONObject(i);
                            String str = jsonObject.getString("Bid");
                            String symbol = jsonObject.getString("symbol");//added by Abhilash

                            if ((str != "null" && !str.isEmpty())) {
                                Cursor c = context.getContentResolver().query((QuoteProvider.Quotes.withSymbol(symbol)),
                                        null, null, null, null);
                                if (c != null) {
                                    if (c.getCount() != 0) isInsert = false;
                                }
                                batchOperations.add(buildBatchOperation(jsonObject, isInsert));
                                isInsert = true;
                                Intent graphIntentService = new Intent(context, GraphIntentService.class);
                                graphIntentService.putExtra("symbol", symbol);
                                context.startService(graphIntentService);

                            } else {
                                StockIntentService.invalidData = true;
                                StockIntentService.invalidSymbol = StockIntentService.invalidSymbol + "," + symbol;
                            }

                        }
                    }
                }
            }
        } catch (JSONException e) {
            Log.e(LOG_TAG, "String to JSON failed: " + e);
        }
        return batchOperations;
    }


    public static String truncateBidPrice(String bidPrice) {
        if (!bidPrice.equals("null"))
            bidPrice = String.format("%.2f", Float.parseFloat(bidPrice)); // invalid float
        return bidPrice;
    }

    public static String truncateChange(String change, boolean isPercentChange) {
        String weight = change.substring(0, 1);
        String ampersand = "";
        if (isPercentChange) {
            ampersand = change.substring(change.length() - 1, change.length());
            change = change.substring(0, change.length() - 1);
        }
        change = change.substring(1, change.length());
        double round = (double) Math.round(Double.parseDouble(change) * 100) / 100;
        change = String.format("%.2f", round);
        StringBuffer changeBuffer = new StringBuffer(change);
        changeBuffer.insert(0, weight);
        changeBuffer.append(ampersand);
        change = changeBuffer.toString();
        return change;
    }

    public static ContentProviderOperation buildBatchOperation(JSONObject jsonObject, Boolean isInsert) {
        ContentProviderOperation.Builder builderInsert = ContentProviderOperation.newInsert(
                QuoteProvider.Quotes.CONTENT_URI);
        ContentProviderOperation.Builder builderUpdate = ContentProviderOperation.newUpdate(
                QuoteProvider.Quotes.CONTENT_URI);

        try {
            if (isInsert) {
                String change = jsonObject.getString("Change");
                builderInsert.withValue(QuoteColumns.SYMBOL, jsonObject.getString("symbol"));
                builderInsert.withValue(QuoteColumns.BIDPRICE, truncateBidPrice(jsonObject.getString("Bid")));
                builderInsert.withValue(QuoteColumns.PERCENT_CHANGE, truncateChange(
                        jsonObject.getString("ChangeinPercent"), true));
                builderInsert.withValue(QuoteColumns.CHANGE, truncateChange(change, false));
                builderInsert.withValue(QuoteColumns.ISCURRENT, 1);
                String graphValues = getGraphData(jsonObject.getString("symbol"));
                builderInsert.withValue(QuoteColumns.GRAPHVALUES, graphValues);
                if (change.charAt(0) == '-') {
                    builderInsert.withValue(QuoteColumns.ISUP, 0);
                } else {
                    builderInsert.withValue(QuoteColumns.ISUP, 1);
                }
                return builderInsert.build();
            } else {
                String change = jsonObject.getString("Change");
                String symbol = jsonObject.getString("symbol");
                builderUpdate.withValue(QuoteColumns.BIDPRICE, truncateBidPrice(jsonObject.getString("Bid")))
                        .withSelection(QuoteColumns.SYMBOL + "=?", new String[]{symbol});
                builderUpdate.withValue(QuoteColumns.PERCENT_CHANGE, truncateChange(
                        jsonObject.getString("ChangeinPercent"), true))
                        .withSelection(QuoteColumns.SYMBOL + "=?", new String[]{symbol});
                builderUpdate.withValue(QuoteColumns.CHANGE, truncateChange(change, false))
                        .withSelection(QuoteColumns.SYMBOL + "=?", new String[]{symbol});
                builderUpdate.withValue(QuoteColumns.ISCURRENT, 1)
                        .withSelection(QuoteColumns.SYMBOL + "=?", new String[]{symbol});

                if (change.charAt(0) == '-') {
                    builderUpdate.withValue(QuoteColumns.ISUP, 0)
                            .withSelection(QuoteColumns.SYMBOL + "=?", new String[]{symbol});
                } else {
                    builderUpdate.withValue(QuoteColumns.ISUP, 1)
                            .withSelection(QuoteColumns.SYMBOL + "=?", new String[]{symbol});
                }
                return builderUpdate.build();
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }

    static public boolean isNetworkAvailable(Context c) {
        ConnectivityManager cm =
                (ConnectivityManager) c.getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        return activeNetwork != null &&
                activeNetwork.isConnectedOrConnecting();
    }


    @SuppressWarnings("ResourceType")
    static public
    @StockTaskService.QuoteStatus
    int getLocationStatus(Context c) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(c);
        return sp.getInt(c.getString(R.string.pref_quote_status_key), StockTaskService.QUOTE_STATUS_UNKNOWN);
    }

    private static String getGraphData(String symbol) {
        StringBuilder urlStringBuilder = new StringBuilder();

        Calendar c = Calendar.getInstance();
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        c.add(Calendar.DATE, 1);
        String endDate = df.format(c.getTime());
        c.add(Calendar.YEAR, -1);
        String startDate = df.format(c.getTime());

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
        }


        String graphValues = GetGraphValues(getResponse);
        return graphValues;

    }

    private static String GetGraphValues(String getResponse) {

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

    static String fetchData(String url) throws IOException {
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url(url)
                .build();

        Response response = client.newCall(request).execute();
        return response.body().string();
    }


}
