package com.airnmap.airbackdroptsk;

import android.os.PersistableBundle;
import android.util.Log;
import android.app.job.JobParameters;
import android.annotation.TargetApi;
import android.app.job.JobService;

@TargetApi(21)
public class BDFetchJobService extends JobService
{
    public boolean onStartJob(final JobParameters params) {
        final PersistableBundle extras = params.getExtras();
        final String taskId = extras.getString("taskId");
        final CompletionHandler completionHandler = new CompletionHandler() {
            @Override
            public void finish() {
                Log.d("AirgroundLog", "- jobFinished");
                BDFetchJobService.this.jobFinished(params, false);
            }
        };
        final BDTask task = new BDTask(taskId, completionHandler, params.getJobId());
        BackdropFetch.getInstance(this.getApplicationContext()).onFetch(task);
        return true;
    }

    public boolean onStopJob(final JobParameters params) {
        Log.d("AirgroundLog", "- onStopJob");
        this.jobFinished(params, false);
        return true;
    }

    public interface CompletionHandler
    {
        void finish();
    }
}
