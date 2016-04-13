package com.sam_chordas.android.stockhawk.service;

import android.annotation.TargetApi;
import android.app.IntentService;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.widget.RemoteViews;

import com.sam_chordas.android.stockhawk.R;
import com.sam_chordas.android.stockhawk.data.QuoteColumns;
import com.sam_chordas.android.stockhawk.data.QuoteProvider;
import com.sam_chordas.android.stockhawk.ui.MyStocksActivity;
import com.sam_chordas.android.stockhawk.widget.QuoteWidgetProvider;

/**
 * Created by Abhilash on 4/1/2016.
 */
public class WidgetIntentService extends IntentService {

    private static final String[] QUOTE_COLUMNS = {
            QuoteColumns.SYMBOL,
            QuoteColumns.BIDPRICE,
            QuoteColumns.CHANGE,
            QuoteColumns._ID,
            QuoteColumns.ISUP
    };
    // these indices must match the projection
    private static final int INDEX_SYMBOL = 0;
    private static final int INDEX_BIDPRICE = 1;
    private static final int INDEX_CHANGE = 2;
    private static final int INDEX_ID = 3;
    private static final int INDEX_ISUP = 4;

    public WidgetIntentService() {
        super("WidgetIntentService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(this);
        int[] appWidgetIds = appWidgetManager.getAppWidgetIds(new ComponentName(this,
                QuoteWidgetProvider.class));

        // Get recent data from the ContentProvider
        Cursor data = getContentResolver().query(QuoteProvider.Quotes.CONTENT_URI, QUOTE_COLUMNS, null,
                null, null);
        if (data == null) {
            return;
        }
        if (!data.moveToFirst()) {
            data.close();
            return;
        }

        int quoteId = data.getInt(INDEX_ID);
        int isUp = data.getInt(INDEX_ISUP);
        String stock_symbol = data.getString(INDEX_SYMBOL);
        String bid_price = data.getString(INDEX_BIDPRICE);
        String change = data.getString(INDEX_CHANGE);
        data.close();

        // Perform this loop procedure for each stock widget
        for (int appWidgetId : appWidgetIds) {
            int layoutId = R.layout.widget_large;
            RemoteViews views = new RemoteViews(getPackageName(), layoutId);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1) {
                                setRemoteContentDescription(views, stock_symbol,isUp,change);
                            }


            // Add the data to the RemoteViews
            views.setTextViewText(R.id.stock_symbol, stock_symbol);
            views.setTextViewText(R.id.bid_price, bid_price);
            views.setTextViewText(R.id.change, change);

            // Create an Intent to launch MainActivity
            Intent launchIntent = new Intent(this, MyStocksActivity.class);
            PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, launchIntent, 0);
            views.setOnClickPendingIntent(R.id.widgetView, pendingIntent);


            // Tell the AppWidgetManager to perform an update on the current app widget
            appWidgetManager.updateAppWidget(appWidgetId, views);
        }
    }
     
                 @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1)
         private void setRemoteContentDescription(RemoteViews views,String stock_symbol,int isUp,String change) {
                     String upDescription = "down by";
                     if (isUp ==1)upDescription = "up by" ;
                     views.setContentDescription(R.id.widget_list_item, getString(R.string.a11y_widget,stock_symbol,change,upDescription));
             }


}