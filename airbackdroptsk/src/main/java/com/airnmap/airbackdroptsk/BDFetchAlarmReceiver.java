package com.airnmap.airbackdroptsk;

import android.util.Log;
import android.os.PowerManager;
import android.content.Intent;
import android.content.Context;
import android.content.BroadcastReceiver;

public class BDFetchAlarmReceiver extends BroadcastReceiver
{
    public void onReceive(final Context context, final Intent intent) {
        final PowerManager powerManager = (PowerManager)context.getSystemService("power");
        final PowerManager.WakeLock wakeLock = powerManager.newWakeLock(1, "AirgroundLog::" + intent.getAction());
        wakeLock.acquire(60000L);
        final BDFetchJobService.CompletionHandler completionHandler = new BDFetchJobService.CompletionHandler() {
            @Override
            public void finish() {
                wakeLock.release();
                Log.d("AirgroundLog", "- BDFetchAlarmReceiver finish");
            }
        };
        final BDTask task = new BDTask(intent.getAction(), completionHandler, 0);
        BackdropFetch.getInstance(context.getApplicationContext()).onFetch(task);
    }
}
