package com.airnmap.airbackdroptsk;

import android.util.Log;
import android.os.Build;
import android.content.Intent;
import android.content.Context;
import android.content.BroadcastReceiver;

public class BDBootReceiver extends BroadcastReceiver
{
    public void onReceive(final Context context, final Intent intent) {
        final String action = intent.getAction();
        if (Build.VERSION.SDK_INT < 22) {
            Log.d("AirgroundLog", "BDBootReceiver: " + action);
            BackdropFetch.getThreadPool().execute(new Runnable() {
                @Override
                public void run() {
                    BackdropFetch.getInstance(context.getApplicationContext()).onBoot();
                }
            });
        }
    }
}
