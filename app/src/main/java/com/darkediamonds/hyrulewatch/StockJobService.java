package com.darkediamonds.hyrulewatch;

import android.app.job.*;
import android.content.*;

public final class StockJobService extends JobService {
    private Thread worker;
    public static void schedule(Context context, boolean enabled) {
        JobScheduler scheduler = context.getSystemService(JobScheduler.class);
        if (!enabled) { scheduler.cancel(40); return; }
        JobInfo job = new JobInfo.Builder(40, new ComponentName(context, StockJobService.class))
                .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY).setPersisted(true)
                .setPeriodic(15 * 60 * 1000L).build();
        if (scheduler.schedule(job) == JobScheduler.RESULT_FAILURE) throw new IllegalStateException("Android could not schedule background checks");
    }
    @Override public boolean onStartJob(JobParameters parameters) {
        worker = new Thread(() -> {
            try { Tracker.refresh(getApplicationContext()); }
            finally { jobFinished(parameters, false); }
        }, "stock-job");
        worker.start(); return true;
    }
    @Override public boolean onStopJob(JobParameters parameters) {
        if (worker != null) worker.interrupt();
        return true;
    }
}
