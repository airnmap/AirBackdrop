package com.airnmap.airbackdroptsk;

import java.util.ArrayList;
import android.content.Intent;
import java.lang.reflect.Method;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import android.app.PendingIntent;
import android.app.AlarmManager;
import android.os.PersistableBundle;
import java.util.concurrent.TimeUnit;
import android.app.job.JobInfo;
import android.content.ComponentName;
import android.app.job.JobScheduler;
import android.os.Build;
import android.util.Log;
import android.content.Context;

import java.util.List;

public class BDTask
{
    private static final List<BDTask> mTasks;
    private BDFetchJobService.CompletionHandler mCompletionHandler;
    private String mTaskId;
    private int mJobId;

    static BDTask getTask(final String taskId) {
        synchronized (BDTask.mTasks) {
            for (final BDTask task : BDTask.mTasks) {
                if (task.hasTaskId(taskId)) {
                    return task;
                }
            }
        }
        return null;
    }

    static void addTask(final BDTask task) {
        synchronized (BDTask.mTasks) {
            BDTask.mTasks.add(task);
        }
    }

    static void removeTask(final String taskId) {
        synchronized (BDTask.mTasks) {
            BDTask found = null;
            for (final BDTask task : BDTask.mTasks) {
                if (task.hasTaskId(taskId)) {
                    found = task;
                    break;
                }
            }
            if (found != null) {
                BDTask.mTasks.remove(found);
            }
        }
    }

    static void clear() {
        synchronized (BDTask.mTasks) {
            BDTask.mTasks.clear();
        }
    }

    BDTask(final String taskId, final BDFetchJobService.CompletionHandler handler, final int jobId) {
        this.mTaskId = taskId;
        this.mCompletionHandler = handler;
        this.mJobId = jobId;
    }

    String getTaskId() {
        return this.mTaskId;
    }

    int getJobId() {
        return this.mJobId;
    }

    boolean hasTaskId(final String taskId) {
        return this.mTaskId != null && this.mTaskId.equalsIgnoreCase(taskId);
    }

    void setCompletionHandler(final BDFetchJobService.CompletionHandler handler) {
        this.mCompletionHandler = handler;
    }

    void finish() {
        if (this.mCompletionHandler != null) {
            this.mCompletionHandler.finish();
        }
        this.mCompletionHandler = null;
        removeTask(this.mTaskId);
    }

    static void schedule(final Context context, final BackdropConfig config) {
        Log.d("AirgroundLog", config.toString());
        final long interval = config.isFetchTask() ? (config.getMinimumFetchInterval() * 60L * 1000L) : config.getDelay();
        if (Build.VERSION.SDK_INT >= 21 && !config.getForceAlarmManager()) {
            final JobScheduler jobScheduler = (JobScheduler)context.getSystemService("jobscheduler");
            final JobInfo.Builder builder = new JobInfo.Builder(config.getJobId(), new ComponentName(context, (Class) BDFetchJobService.class)).setRequiredNetworkType(config.getRequiredNetworkType()).setRequiresDeviceIdle(config.getRequiresDeviceIdle()).setRequiresCharging(config.getRequiresCharging()).setPersisted(config.getStartOnBoot() && !config.getStopOnTerminate());
            if (config.getPeriodic()) {
                if (Build.VERSION.SDK_INT >= 24) {
                    builder.setPeriodic(interval, TimeUnit.MINUTES.toMillis(5L));
                }
                else {
                    builder.setPeriodic(interval);
                }
            }
            else {
                builder.setMinimumLatency(interval);
            }
            final PersistableBundle extras = new PersistableBundle();
            extras.putString("taskId", config.getTaskId());
            builder.setExtras(extras);
            if (Build.VERSION.SDK_INT >= 26) {
                builder.setRequiresStorageNotLow(config.getRequiresStorageNotLow());
                builder.setRequiresBatteryNotLow(config.getRequiresBatteryNotLow());
            }
            if (jobScheduler != null) {
                jobScheduler.schedule(builder.build());
            }
        }
        else {
            final AlarmManager alarmManager = (AlarmManager)context.getSystemService("alarm");
            if (alarmManager != null) {
                final PendingIntent pi = getAlarmPI(context, config.getTaskId());
                final long delay = System.currentTimeMillis() + interval;
                if (config.getPeriodic()) {
                    alarmManager.setRepeating(0, delay, interval, pi);
                }
                else if (Build.VERSION.SDK_INT >= 23) {
                    alarmManager.setExactAndAllowWhileIdle(0, delay, pi);
                }
                else if (Build.VERSION.SDK_INT >= 19) {
                    alarmManager.setExact(0, delay, pi);
                }
                else {
                    alarmManager.set(0, delay, pi);
                }
            }
        }
    }

    void fireHeadlessEvent(final Context context, final BackdropConfig config) throws Error {
        try {
            final Class<?> HeadlessClass = Class.forName(config.getJobService());
            final Class[] types = { Context.class, String.class };
            final Object[] params = { context, config.getTaskId() };
            try {
                final Constructor<?> constructor = HeadlessClass.getDeclaredConstructor((Class<?>[])types);
                constructor.newInstance(params);
            }
            catch (NoSuchMethodException e6) {
                final Constructor<?> constructor2 = HeadlessClass.getConstructor((Class<?>[])new Class[0]);
                final Object instance = constructor2.newInstance(new Object[0]);
                final Method onFetch = instance.getClass().getDeclaredMethod("onFetch", (Class<?>[])types);
                onFetch.invoke(instance, params);
            }
        }
        catch (ClassNotFoundException e) {
            Log.i("AirgroundLog", e.toString());
            throw new Error(e.getMessage());
        }
        catch (NoSuchMethodException e2) {
            Log.i("AirgroundLog", e2.toString());
            throw new Error(e2.getMessage());
        }
        catch (IllegalAccessException e3) {
            Log.i("AirgroundLog", e3.toString());
            throw new Error(e3.getMessage());
        }
        catch (InstantiationException e4) {
            Log.i("AirgroundLog", e4.toString());
            throw new Error(e4.getMessage());
        }
        catch (InvocationTargetException e5) {
            Log.i("AirgroundLog", e5.toString());
            throw new Error(e5.getMessage());
        }
    }

    static void cancel(final Context context, final String taskId, final int jobId) {
        Log.i("AirgroundLog", "- cancel taskId=" + taskId + ", jobId=" + jobId);
        if (Build.VERSION.SDK_INT >= 21 && jobId != 0) {
            final JobScheduler jobScheduler = (JobScheduler)context.getSystemService("jobscheduler");
            if (jobScheduler != null) {
                jobScheduler.cancel(jobId);
            }
        }
        else {
            final AlarmManager alarmManager = (AlarmManager)context.getSystemService("alarm");
            if (alarmManager != null) {
                alarmManager.cancel(getAlarmPI(context, taskId));
            }
        }
    }

    static PendingIntent getAlarmPI(final Context context, final String taskId) {
        final Intent intent = new Intent(context, (Class) BDFetchAlarmReceiver.class);
        intent.setAction(taskId);
        return PendingIntent.getBroadcast(context, 0, intent, 134217728);
    }

    @Override
    public String toString() {
        return "[BDTask taskId=" + this.mTaskId + "]";
    }

    static {
        mTasks = new ArrayList<BDTask>();
    }

    static class Error extends RuntimeException
    {
        public Error(final String msg) {
            super(msg);
        }
    }
}
