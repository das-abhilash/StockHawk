package com.sam_chordas.android.stockhawk.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.TextView;
import android.widget.Toast;

import com.sam_chordas.android.stockhawk.R;
import com.sam_chordas.android.stockhawk.ui.StockDetailActivity;
import com.sam_chordas.android.stockhawk.ui.StockDetailActivityFragment;

/**
 * Created by Abhilash on 4/3/2016.
 */
public class InvalidDataBroadcastReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {

        String InvalidSymbol = intent.getStringExtra("InvalidSymbol");
        Toast.makeText(context, context.getString(R.string.invalid_entry, InvalidSymbol), Toast.LENGTH_LONG).show();

    }
}
