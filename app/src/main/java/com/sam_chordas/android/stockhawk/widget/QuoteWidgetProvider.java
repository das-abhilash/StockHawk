package com.sam_chordas.android.stockhawk.widget;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;

import com.sam_chordas.android.stockhawk.service.WidgetIntentService;
import com.sam_chordas.android.stockhawk.service.StockTaskService;


/**
 * Created by Abhilash on 3/31/2016.
 */
public class QuoteWidgetProvider extends AppWidgetProvider {

    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {

        context.startService(new Intent(context, WidgetIntentService.class));
    }

    @Override
    public void onAppWidgetOptionsChanged(Context context, AppWidgetManager appWidgetManager,
                                          int appWidgetId, Bundle newOptions) {
        context.startService(new Intent(context, WidgetIntentService.class));
    }

    @Override
    public void onReceive(@NonNull Context context, @NonNull Intent intent) {
        super.onReceive(context, intent);
        if (StockTaskService.ACTION_DATA_UPDATED.equals(intent.getAction())) {
            context.startService(new Intent(context, WidgetIntentService.class));
        }
    }
}

