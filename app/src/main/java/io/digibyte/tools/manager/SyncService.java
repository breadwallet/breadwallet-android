package io.digibyte.tools.manager;

import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.app.job.JobService;
import android.content.ComponentName;
import android.content.Context;

import java.util.concurrent.TimeUnit;

import io.digibyte.tools.threads.BRExecutor;
import io.digibyte.wallet.BRPeerManager;
import io.digibyte.wallet.BRWalletManager;

/**
 * Created by Noah Seidman on 2/18/18.
 * The SyncService is only registered to the JobScheduler in the onResume of the BRActivity,
 * thus it's fully assumed the wallet has already been created (seeds saved, pin, etc...)
 * Initializing the wallet starts all native block syncing functions, thus all that's needed
 * for sync to occur, is for this job to init the wallet.
 */

public class SyncService extends JobService implements BRPeerManager.OnSyncSucceeded {

    private static final int SYNC_SERVICE_ID = 1234;
    private static final long SYNC_PERIOD = TimeUnit.HOURS.toMillis(24);
    private JobParameters jobParameters;

    @Override
    public boolean onStartJob(JobParameters jobParameters) {
        this.jobParameters = jobParameters;
        BRExecutor.getInstance().forBackgroundTasks().execute(new Runnable() {
            @Override
            public void run() {
                BRWalletManager.getInstance().initWallet(SyncService.this);
            }
        });
        BRPeerManager.setOnSyncFinished(this);
        return true;
    }

    /**
     * @param jobParameters
     * @return true to always re-reun this job according to the jobInfo configuration.
     */
    @Override
    public boolean onStopJob(JobParameters jobParameters){
        return true;
    }

    /**
     * This method is executed in {@link io.digibyte.presenter.activities.BreadActivity} onResume.
     * It resets the scheduled job, schedules it to recurring execution every 24 hours, requires
     * the device to be charging and wifi to be connected. Note that if the period expires before
     * charging, the next time charging occurs it will execute, this is based on the design of the
     * job scheduler.
     * TODO we should add a setting to allow background sync over Carrier Data if desired
     * @param context
     */
    public static void scheduleBackgroundSync(Context context) {
        JobScheduler jobScheduler =
                (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
        jobScheduler.cancel(SyncService.SYNC_SERVICE_ID);
        JobInfo.Builder jobInfo = new JobInfo.Builder(
                SyncService.SYNC_SERVICE_ID, new ComponentName(context, SyncService.class));
        jobInfo.setPeriodic(SYNC_PERIOD);
        jobInfo.setRequiresCharging(true);
        jobInfo.setPersisted(true);
        jobInfo.setRequiredNetworkType(JobInfo.NETWORK_TYPE_UNMETERED);
        jobScheduler.schedule(jobInfo.build());
    }

    @Override
    public void onFinished() {
        int startHeight = BRSharedPrefs.getStartHeight(this);
        double progressStatus = BRPeerManager.syncProgress(startHeight);
        jobFinished(jobParameters, progressStatus != 1);
    }
}