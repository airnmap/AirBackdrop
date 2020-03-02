// 
// Decompiled by Procyon v0.5.36
// 

package com.airnmap.airbackdroptsk;

import android.app.ActivityManager;
import android.os.Build;
import android.support.annotation.Nullable;
import android.annotation.TargetApi;

import java.util.List;
import android.util.Log;
import java.util.HashMap;
import java.util.concurrent.Executors;
import android.os.Looper;
import java.util.Map;
import android.content.Context;
import android.os.Handler;
import java.util.concurrent.ExecutorService;

public class BackdropFetch
{
    public static final String TAG = "AirgroundLog";
    public static final String ACTION_CONFIGURE = "configure";
    public static final String ACTION_START = "start";
    public static final String ACTION_STOP = "stop";
    public static final String ACTION_FINISH = "finish";
    public static final String ACTION_STATUS = "status";
    public static final String ACTION_FORCE_RELOAD = "AirgroundLog-forceReload";
    public static final String EVENT_FETCH = ".event.BACKGROUND_FETCH";
    public static final int STATUS_AVAILABLE = 2;

    private static BackdropFetch mInstance;
    private static ExecutorService sThreadPool;
    private static Handler uiHandler;
    private Context mContext;
    private Callback mCallback;
    private final Map<String, BackdropConfig> mConfig;

    public static Handler getUiHandler() {
        if (BackdropFetch.uiHandler == null) {
            BackdropFetch.uiHandler = new Handler(Looper.getMainLooper());
        }
        return BackdropFetch.uiHandler;
    }

    public static ExecutorService getThreadPool() {
        if (BackdropFetch.sThreadPool == null) {
            BackdropFetch.sThreadPool = Executors.newCachedThreadPool();
        }
        return BackdropFetch.sThreadPool;
    }

    public static BackdropFetch getInstance(final Context context) {
        if (BackdropFetch.mInstance == null) {
            BackdropFetch.mInstance = getInstanceSynchronized(context.getApplicationContext());
        }
        return BackdropFetch.mInstance;
    }

    private static synchronized BackdropFetch getInstanceSynchronized(final Context context) {
        if (BackdropFetch.mInstance == null) {
            BackdropFetch.mInstance = new BackdropFetch(context.getApplicationContext());
        }
        return BackdropFetch.mInstance;
    }

    private BackdropFetch(final Context context) {
        this.mConfig = new HashMap<String, BackdropConfig>();
        this.mContext = context;
    }

    public void configure(final BackdropConfig config, final Callback callback) {
        Log.d("AirgroundLog", "- configure");
        this.mCallback = callback;
        synchronized (this.mConfig) {
            this.mConfig.put(config.getTaskId(), config);
        }
        this.start(config.getTaskId());
    }

    void onBoot() {
        BackdropConfig.load(this.mContext, new BackdropConfig.OnLoadCallback() {
            @Override
            public void onLoad(final List<BackdropConfig> result) {
                for (final BackdropConfig config : result) {
                    if (!config.getStartOnBoot() || config.getStopOnTerminate()) {
                        config.destroy(BackdropFetch.this.mContext);
                    }
                    else {
                        synchronized (BackdropFetch.this.mConfig) {
                            BackdropFetch.this.mConfig.put(config.getTaskId(), config);
                        }
                        if (config.isFetchTask()) {
                            BackdropFetch.this.start(config.getTaskId());
                        }
                        else {
                            BackdropFetch.this.scheduleTask(config);
                        }
                    }
                }
            }
        });
    }

    @TargetApi(21)
    public void start(final String fetchTaskId) {
        Log.d("AirgroundLog", "- start");
        final BDTask task = BDTask.getTask(fetchTaskId);
        if (task != null) {
            Log.e("AirgroundLog", "[AirgroundLog start] Task " + fetchTaskId + " already registered");
            return;
        }
        this.registerTask(fetchTaskId);
    }

    public void stop(@Nullable final String taskId) {
        String msg = "- stop";
        if (taskId != null) {
            msg = msg + ": " + taskId;
        }
        Log.d("AirgroundLog", msg);
        if (taskId == null) {
            synchronized (this.mConfig) {
                for (final BackdropConfig config : this.mConfig.values()) {
                    final BDTask task = BDTask.getTask(config.getTaskId());
                    if (task != null) {
                        task.finish();
                        BDTask.removeTask(config.getTaskId());
                    }
                    BDTask.cancel(this.mContext, config.getTaskId(), config.getJobId());
                    config.destroy(this.mContext);
                }
                BDTask.clear();
            }
        }
        else {
            final BDTask task2 = BDTask.getTask(taskId);
            if (task2 != null) {
                task2.finish();
                BDTask.removeTask(task2.getTaskId());
            }
            final BackdropConfig config2 = this.getConfig(taskId);
            if (config2 != null) {
                config2.destroy(this.mContext);
                BDTask.cancel(this.mContext, config2.getTaskId(), config2.getJobId());
            }
        }
    }

    public void scheduleTask(final BackdropConfig config) {
        synchronized (this.mConfig) {
            if (this.mConfig.containsKey(config.getTaskId())) {}
            config.save(this.mContext);
            this.mConfig.put(config.getTaskId(), config);
        }
        final String taskId = config.getTaskId();
        this.registerTask(taskId);
    }

    public void finish(final String taskId) {
        Log.d("AirgroundLog", "- finish: " + taskId);
        final BDTask task = BDTask.getTask(taskId);
        if (task != null) {
            task.finish();
        }
    }

    public int status() {
        return 2;
    }

    void onFetch(final BDTask task) {
        BDTask.addTask(task);
        Log.d("AirgroundLog", "- Background Fetch event received");
        synchronized (this.mConfig) {
            if (this.mConfig.isEmpty()) {
                BackdropConfig.load(this.mContext, new BackdropConfig.OnLoadCallback() {
                    @Override
                    public void onLoad(final List<BackdropConfig> result) {
                        synchronized (BackdropFetch.this.mConfig) {
                            for (final BackdropConfig config : result) {
                                BackdropFetch.this.mConfig.put(config.getTaskId(), config);
                            }
                        }
                        BackdropFetch.this.doFetch(task);
                    }
                });
                return;
            }
        }
        this.doFetch(task);
    }

    private void registerTask(final String taskId) {
        Log.d("AirgroundLog", "- registerTask: " + taskId);
        final BackdropConfig config = this.getConfig(taskId);
        if (config == null) {
            Log.e("AirgroundLog", "- registerTask failed to find BackdropConfig for taskId " + taskId);
            return;
        }
        config.save(this.mContext);
        BDTask.schedule(this.mContext, config);
    }

    private void doFetch(final BDTask task) {
        final BackdropConfig config = this.getConfig(task.getTaskId());
        if (config == null) {
            BDTask.cancel(this.mContext, task.getTaskId(), task.getJobId());
            return;
        }
        if (this.isMainActivityActive()) {
            if (this.mCallback != null) {
                this.mCallback.onFetch(task.getTaskId());
            }
        }
        else if (config.getStopOnTerminate()) {
            Log.d("AirgroundLog", "- Stopping on terminate");
            this.stop(task.getTaskId());
        }
        else if (config.getJobService() != null) {
            try {
                task.fireHeadlessEvent(this.mContext, config);
            }
            catch (BDTask.Error e) {
                Log.e("AirgroundLog", "Headless task error: " + e.getMessage());
                e.printStackTrace();
            }
        }
        else {
            Log.w("AirgroundLog", "- BackdropFetch event has occurred while app is terminated but there's no jobService configured to handle the event.  BackdropFetch will terminate.");
            this.finish(task.getTaskId());
            this.stop(task.getTaskId());
        }
        if (!config.getPeriodic()) {
            config.destroy(this.mContext);
            synchronized (this.mConfig) {
                this.mConfig.remove(task.getTaskId());
            }
        }
    }

    public Boolean isMainActivityActive() {
        Boolean isActive = false;
        if (this.mContext == null || this.mCallback == null) {
            return false;
        }
        final ActivityManager activityManager = (ActivityManager)this.mContext.getSystemService("activity");
        try {
            final List<ActivityManager.RunningTaskInfo> tasks = (List<ActivityManager.RunningTaskInfo>)activityManager.getRunningTasks(Integer.MAX_VALUE);
            for (final ActivityManager.RunningTaskInfo task : tasks) {
                if (Build.VERSION.SDK_INT >= 29) {
                    if (this.mContext.getPackageName().equalsIgnoreCase(task.baseActivity.getPackageName())) {
                        isActive = true;
                        break;
                    }
                }
            }
        }
        catch (SecurityException e) {
            Log.w("AirgroundLog", "AirgroundLog attempted to determine if MainActivity is active but was stopped due to a missing permission.  Please add the permission 'android.permission.GET_TASKS' to your AndroidManifest.  See Installation steps for more information");
            throw e;
        }
        return isActive;
    }

    private BackdropConfig getConfig(final String taskId) {
        synchronized (this.mConfig) {
            return this.mConfig.containsKey(taskId) ? this.mConfig.get(taskId) : null;
        }
    }

    static {
        BackdropFetch.mInstance = null;
    }

    public interface Callback
    {
        void onFetch(final String p0);
    }
}
