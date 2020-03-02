package com.airnmap.airbackdroptsk;

import android.os.Build;

import java.util.List;
import java.util.ArrayList;
import org.json.JSONException;
import org.json.JSONObject;
import android.content.SharedPreferences;

import java.util.Set;
import java.util.HashSet;
import android.content.Context;
import android.util.Log;

public class BackdropConfig
{
    private Builder config;
    private static final int MINIMUM_FETCH_INTERVAL = 1;
    private static final int DEFAULT_FETCH_INTERVAL = 15;
    public static final String FIELD_TASK_ID = "taskId";
    public static final String FIELD_MINIMUM_FETCH_INTERVAL = "minimumFetchInterval";
    public static final String FIELD_START_ON_BOOT = "startOnBoot";
    public static final String FIELD_REQUIRED_NETWORK_TYPE = "requiredNetworkType";
    public static final String FIELD_REQUIRES_BATTERY_NOT_LOW = "requiresBatteryNotLow";
    public static final String FIELD_REQUIRES_CHARGING = "requiresCharging";
    public static final String FIELD_REQUIRES_DEVICE_IDLE = "requiresDeviceIdle";
    public static final String FIELD_REQUIRES_STORAGE_NOT_LOW = "requiresStorageNotLow";
    public static final String FIELD_STOP_ON_TERMINATE = "stopOnTerminate";
    public static final String FIELD_JOB_SERVICE = "jobService";
    public static final String FIELD_FORCE_ALARM_MANAGER = "forceAlarmManager";
    public static final String FIELD_PERIODIC = "periodic";
    public static final String FIELD_DELAY = "delay";
    public static final String FIELD_IS_FETCH_TASK = "isFetchTask";
    static int FETCH_JOB_ID;

    private BackdropConfig(final Builder builder) {
        this.config = builder;
        if (this.config.jobService == null) {
            if (!this.config.stopOnTerminate) {
                Log.w("AirgroundLog", "- Configuration error:  In order to use stopOnTerminate: false, you must set enableHeadless: true");
                this.config.setStopOnTerminate(true);
            }
            if (this.config.startOnBoot) {
                Log.w("AirgroundLog", "- Configuration error:  In order to use startOnBoot: true, you must enableHeadless: true");
                this.config.setStartOnBoot(false);
            }
        }
    }

    void save(final Context context) {
        final SharedPreferences preferences = context.getSharedPreferences("AirgroundLog", 0);
        Set<String> taskIds = (Set<String>)preferences.getStringSet("tasks", (Set)new HashSet());
        if (taskIds == null) {
            taskIds = new HashSet<String>();
        }
        if (!taskIds.contains(this.config.taskId)) {
            final Set<String> newIds = new HashSet<String>(taskIds);
            newIds.add(this.config.taskId);
            final SharedPreferences.Editor editor = preferences.edit();
            editor.putStringSet("tasks", (Set)newIds);
            editor.apply();
        }
        final SharedPreferences.Editor editor2 = context.getSharedPreferences("AirgroundLog:" + this.config.taskId, 0).edit();
        editor2.putString("taskId", this.config.taskId);
        editor2.putBoolean("isFetchTask", this.config.isFetchTask);
        editor2.putInt("minimumFetchInterval", this.config.minimumFetchInterval);
        editor2.putBoolean("stopOnTerminate", this.config.stopOnTerminate);
        editor2.putBoolean("startOnBoot", this.config.startOnBoot);
        editor2.putInt("requiredNetworkType", this.config.requiredNetworkType);
        editor2.putBoolean("requiresBatteryNotLow", this.config.requiresBatteryNotLow);
        editor2.putBoolean("requiresCharging", this.config.requiresCharging);
        editor2.putBoolean("requiresDeviceIdle", this.config.requiresDeviceIdle);
        editor2.putBoolean("requiresStorageNotLow", this.config.requiresStorageNotLow);
        editor2.putString("jobService", this.config.jobService);
        editor2.putBoolean("forceAlarmManager", this.config.forceAlarmManager);
        editor2.putBoolean("periodic", this.config.periodic);
        editor2.putLong("delay", this.config.delay);
        editor2.apply();
    }

    void destroy(final Context context) {
        final SharedPreferences preferences = context.getSharedPreferences("AirgroundLog", 0);
        Set<String> taskIds = (Set<String>)preferences.getStringSet("tasks", (Set)new HashSet());
        if (taskIds == null) {
            taskIds = new HashSet<String>();
        }
        if (taskIds.contains(this.config.taskId)) {
            final Set<String> newIds = new HashSet<String>(taskIds);
            newIds.remove(this.config.taskId);
            final SharedPreferences.Editor editor = preferences.edit();
            editor.putStringSet("tasks", (Set)newIds);
            editor.apply();
        }
        if (!this.config.isFetchTask) {
            final SharedPreferences.Editor editor2 = context.getSharedPreferences("AirgroundLog:" + this.config.taskId, 0).edit();
            editor2.clear();
            editor2.apply();
        }
    }

    boolean isFetchTask() {
        return this.config.isFetchTask;
    }

    public String getTaskId() {
        return this.config.taskId;
    }

    public int getMinimumFetchInterval() {
        return this.config.minimumFetchInterval;
    }

    public int getRequiredNetworkType() {
        return this.config.requiredNetworkType;
    }

    public boolean getRequiresBatteryNotLow() {
        return this.config.requiresBatteryNotLow;
    }

    public boolean getRequiresCharging() {
        return this.config.requiresCharging;
    }

    public boolean getRequiresDeviceIdle() {
        return this.config.requiresDeviceIdle;
    }

    public boolean getRequiresStorageNotLow() {
        return this.config.requiresStorageNotLow;
    }

    public boolean getStopOnTerminate() {
        return this.config.stopOnTerminate;
    }

    public boolean getStartOnBoot() {
        return this.config.startOnBoot;
    }

    public String getJobService() {
        return this.config.jobService;
    }

    public boolean getForceAlarmManager() {
        return this.config.forceAlarmManager;
    }

    public boolean getPeriodic() {
        return this.config.periodic || this.isFetchTask();
    }

    public long getDelay() {
        return this.config.delay;
    }

    int getJobId() {
        if (this.config.forceAlarmManager) {
            return 0;
        }
        return this.isFetchTask() ? BackdropConfig.FETCH_JOB_ID : this.config.taskId.hashCode();
    }

    @Override
    public String toString() {
        final JSONObject output = new JSONObject();
        try {
            output.put("taskId", (Object)this.config.taskId);
            output.put("isFetchTask", this.config.isFetchTask);
            output.put("minimumFetchInterval", this.config.minimumFetchInterval);
            output.put("stopOnTerminate", this.config.stopOnTerminate);
            output.put("requiredNetworkType", this.config.requiredNetworkType);
            output.put("requiresBatteryNotLow", this.config.requiresBatteryNotLow);
            output.put("requiresCharging", this.config.requiresCharging);
            output.put("requiresDeviceIdle", this.config.requiresDeviceIdle);
            output.put("requiresStorageNotLow", this.config.requiresStorageNotLow);
            output.put("startOnBoot", this.config.startOnBoot);
            output.put("jobService", (Object)this.config.jobService);
            output.put("forceAlarmManager", this.config.forceAlarmManager);
            output.put("periodic", this.getPeriodic());
            output.put("delay", this.config.delay);
            return output.toString(2);
        }
        catch (JSONException e) {
            e.printStackTrace();
            return output.toString();
        }
    }

    static void load(final Context context, final OnLoadCallback callback) {
        BackdropFetch.getThreadPool().execute(new Runnable() {
            @Override
            public void run() {
                final List<BackdropConfig> result = new ArrayList<BackdropConfig>();
                final SharedPreferences preferences = context.getSharedPreferences("AirgroundLog", 0);
                final Set<String> taskIds = (Set<String>)preferences.getStringSet("tasks", (Set)new HashSet());
                if (taskIds != null) {
                    for (final String taskId : taskIds) {
                        result.add(new Builder().load(context, taskId));
                    }
                }
                BackdropFetch.getUiHandler().post((Runnable)new Runnable() {
                    @Override
                    public void run() {
                        callback.onLoad(result);
                    }
                });
            }
        });
    }

    static {
        BackdropConfig.FETCH_JOB_ID = 999;
    }

    public static class Builder
    {
        private String taskId;
        private int minimumFetchInterval;
        private long delay;
        private boolean periodic;
        private boolean forceAlarmManager;
        private boolean stopOnTerminate;
        private boolean startOnBoot;
        private int requiredNetworkType;
        private boolean requiresBatteryNotLow;
        private boolean requiresCharging;
        private boolean requiresDeviceIdle;
        private boolean requiresStorageNotLow;
        private boolean isFetchTask;
        private String jobService;

        public Builder() {
            this.minimumFetchInterval = 15;
            this.delay = -1L;
            this.periodic = false;
            this.forceAlarmManager = false;
            this.stopOnTerminate = true;
            this.startOnBoot = false;
            this.requiredNetworkType = 0;
            this.requiresBatteryNotLow = false;
            this.requiresCharging = false;
            this.requiresDeviceIdle = false;
            this.requiresStorageNotLow = false;
            this.isFetchTask = false;
            this.jobService = null;
        }

        public Builder setTaskId(final String taskId) {
            this.taskId = taskId;
            return this;
        }

        public Builder setIsFetchTask(final boolean value) {
            this.isFetchTask = value;
            return this;
        }

        public Builder setMinimumFetchInterval(final int fetchInterval) {
            if (fetchInterval >= 1) {
                this.minimumFetchInterval = fetchInterval;
            }
            return this;
        }

        public Builder setStopOnTerminate(final boolean stopOnTerminate) {
            this.stopOnTerminate = stopOnTerminate;
            return this;
        }

        public Builder setStartOnBoot(final boolean startOnBoot) {
            this.startOnBoot = startOnBoot;
            return this;
        }

        public Builder setRequiredNetworkType(int networkType) {
            if (Build.VERSION.SDK_INT >= 22) {
                if (networkType != 1 && networkType != 4 && networkType != 0 && networkType != 3 && networkType != 2) {
                    Log.e("AirgroundLog", "[ERROR] Invalid requiredNetworkType: " + networkType + "; Defaulting to NETWORK_TYPE_NONE");
                    networkType = 0;
                }
                this.requiredNetworkType = networkType;
            }
            return this;
        }

        public Builder setRequiresBatteryNotLow(final boolean value) {
            this.requiresBatteryNotLow = value;
            return this;
        }

        public Builder setRequiresCharging(final boolean value) {
            this.requiresCharging = value;
            return this;
        }

        public Builder setRequiresDeviceIdle(final boolean value) {
            this.requiresDeviceIdle = value;
            return this;
        }

        public Builder setRequiresStorageNotLow(final boolean value) {
            this.requiresStorageNotLow = value;
            return this;
        }

        public Builder setJobService(final String className) {
            this.jobService = className;
            return this;
        }

        public Builder setForceAlarmManager(final boolean value) {
            this.forceAlarmManager = value;
            return this;
        }

        public Builder setPeriodic(final boolean value) {
            this.periodic = value;
            return this;
        }

        public Builder setDelay(final long value) {
            this.delay = value;
            return this;
        }

        public BackdropConfig build() {
            return new BackdropConfig( null);
        }

        public BackdropConfig load(final Context context, final String taskId) {
            final SharedPreferences preferences = context.getSharedPreferences("AirgroundLog:" + taskId, 0);
            if (preferences.contains("taskId")) {
                this.setTaskId(preferences.getString("taskId", taskId));
            }
            if (preferences.contains("isFetchTask")) {
                this.setIsFetchTask(preferences.getBoolean("isFetchTask", this.isFetchTask));
            }
            if (preferences.contains("minimumFetchInterval")) {
                this.setMinimumFetchInterval(preferences.getInt("minimumFetchInterval", this.minimumFetchInterval));
            }
            if (preferences.contains("stopOnTerminate")) {
                this.setStopOnTerminate(preferences.getBoolean("stopOnTerminate", this.stopOnTerminate));
            }
            if (preferences.contains("requiredNetworkType")) {
                this.setRequiredNetworkType(preferences.getInt("requiredNetworkType", this.requiredNetworkType));
            }
            if (preferences.contains("requiresBatteryNotLow")) {
                this.setRequiresBatteryNotLow(preferences.getBoolean("requiresBatteryNotLow", this.requiresBatteryNotLow));
            }
            if (preferences.contains("requiresCharging")) {
                this.setRequiresCharging(preferences.getBoolean("requiresCharging", this.requiresCharging));
            }
            if (preferences.contains("requiresDeviceIdle")) {
                this.setRequiresDeviceIdle(preferences.getBoolean("requiresDeviceIdle", this.requiresDeviceIdle));
            }
            if (preferences.contains("requiresStorageNotLow")) {
                this.setRequiresStorageNotLow(preferences.getBoolean("requiresStorageNotLow", this.requiresStorageNotLow));
            }
            if (preferences.contains("startOnBoot")) {
                this.setStartOnBoot(preferences.getBoolean("startOnBoot", this.startOnBoot));
            }
            if (preferences.contains("jobService")) {
                this.setJobService(preferences.getString("jobService", (String)null));
            }
            if (preferences.contains("forceAlarmManager")) {
                this.setForceAlarmManager(preferences.getBoolean("forceAlarmManager", this.forceAlarmManager));
            }
            if (preferences.contains("periodic")) {
                this.setPeriodic(preferences.getBoolean("periodic", this.periodic));
            }
            if (preferences.contains("delay")) {
                this.setDelay(preferences.getLong("delay", this.delay));
            }
            return new BackdropConfig( null);
        }
    }

    interface OnLoadCallback
    {
        void onLoad(final List<BackdropConfig> p0);
    }
}