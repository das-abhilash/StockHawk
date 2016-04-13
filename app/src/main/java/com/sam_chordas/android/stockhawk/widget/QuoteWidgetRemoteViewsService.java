package com.sam_chordas.android.stockhawk.widget;

import android.annotation.TargetApi;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.widget.AdapterView;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import com.sam_chordas.android.stockhawk.R;
import com.sam_chordas.android.stockhawk.data.QuoteColumns;
import com.sam_chordas.android.stockhawk.data.QuoteProvider;

/**
 * Created by Abhilash on 4/7/2016.
 */
public class QuoteWidgetRemoteViewsService extends RemoteViewsService {
    public final String LOG_TAG = QuoteWidgetRemoteViewsService.class.getSimpleName();
    private static final String[] QUOTE_COLUMNS = {
            QuoteColumns._ID,
            QuoteColumns.SYMBOL,
            QuoteColumns.BIDPRICE,
            QuoteColumns.CHANGE,
            QuoteColumns.ISUP
    };
    // these indices must match the projection
    private static final int INDEX_ID = 0;
    private static final int INDEX_SYMBOL = 1;
    private static final int INDEX_BIDPRICE = 2;
    private static final int INDEX_CHANGE = 3;
    private static final int INDEX_ISUP = 4;


    @Override
    public RemoteViewsFactory onGetViewFactory(Intent intent) {
        return new RemoteViewsFactory() {
            private Cursor data = null;

            @Override
            public void onCreate() {
                // Nothing to do
            }

            @Override
            public void onDataSetChanged() {
                if (data != null) {
                    data.close();
                }
                // This method is called by the app hosting the widget (e.g., the launcher)
                // However, our ContentProvider is not exported so it doesn't have access to the
                // data. Therefore we need to clear (and finally restore) the calling identity so
                // that calls use our process and permission
                final long identityToken = Binder.clearCallingIdentity();

                data = getContentResolver().query(QuoteProvider.Quotes.CONTENT_URI, QUOTE_COLUMNS, null,
                        null, null);
                Binder.restoreCallingIdentity(identityToken);
            }

            @Override
            public void onDestroy() {
                if (data != null) {
                    data.close();
                    data = null;
                }
            }

            @Override
            public int getCount() {
                return data == null ? 0 : data.getCount();
            }

            @Override
            public RemoteViews getViewAt(int position) {
                if (position == AdapterView.INVALID_POSITION ||
                        data == null || !data.moveToPosition(position)) {
                    return null;
                }
                RemoteViews views = new RemoteViews(getPackageName(),
                        R.layout.widget_collection_item);



                int quoteId = data.getInt(INDEX_ID);
                String stock_symbol = data.getString(INDEX_SYMBOL);
                String bid_price = data.getString(INDEX_BIDPRICE);
                String change = data.getString(INDEX_CHANGE);
                int isUp = data.getInt(INDEX_ISUP);
                views.setTextViewText(R.id.stock_symbol, stock_symbol);
                views.setTextViewText(R.id.change, change);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1) {
                    setRemoteContentDescription(views,stock_symbol,isUp,change);
                }
                final Intent fillInIntent = new Intent();

                fillInIntent.putExtra("symbol", stock_symbol);
                int sdf = 0;
                views.setOnClickFillInIntent(R.id.widget_list_item, fillInIntent);
                return views;
            }
             
                                 @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1)
                         private void setRemoteContentDescription(RemoteViews views,String stock_symbol,int isUp,String change) {
                                     String upDescription = "down by";
                                     if (isUp ==1)upDescription = "up by" ;
                                 views.setContentDescription(R.id.widget_list_item, getString(R.string.a11y_widget,stock_symbol,change,upDescription));
                             }

            @Override
            public RemoteViews getLoadingView() {
                return new RemoteViews(getPackageName(), R.layout.widget_collection_item);
            }

            @Override
            public int getViewTypeCount() {
                return 1;
            }

            @Override
            public long getItemId(int position) {
                if (data.moveToPosition(position))
                    return data.getLong(INDEX_ID);
                return position;
            }

            @Override
            public boolean hasStableIds() {
                return true;
            }
        };
    }
}