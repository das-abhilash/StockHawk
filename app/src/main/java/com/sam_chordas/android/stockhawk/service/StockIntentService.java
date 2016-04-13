package com.sam_chordas.android.stockhawk.service;

import android.app.IntentService;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.gcm.TaskParams;

/**
 * Created by sam_chordas on 10/1/15.
 */
public class StockIntentService extends IntentService {

  public StockIntentService(){
    super(StockIntentService.class.getName());
  }

  public StockIntentService(String name) {
    super(name);
  }

public static Boolean invalidData = false;
    public static String invalidSymbol = null;
  @Override protected void onHandleIntent(Intent intent) {

    Log.d(StockIntentService.class.getSimpleName(), "Stock Intent Service");
    StockTaskService stockTaskService = new StockTaskService(this);
    Bundle args = new Bundle();
    if (intent.getStringExtra("tag").equals("add")){
      args.putString("symbol", intent.getStringExtra("symbol"));
    }

    stockTaskService.onRunTask(new TaskParams(intent.getStringExtra("tag"), args));

      if(invalidData){
          invalidData = false;
          Intent broadcastIntent = new Intent(this, InvalidDataBroadcastReceiver.class);
          broadcastIntent.setAction("com.sam_chordas.android.stockhawk.InvalidData");
          broadcastIntent.addCategory(Intent.CATEGORY_DEFAULT);
          broadcastIntent.putExtra("InvalidSymbol", invalidSymbol);
          sendBroadcast(broadcastIntent);
          invalidSymbol = null;
      }

  }
}
