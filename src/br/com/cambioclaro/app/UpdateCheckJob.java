package br.com.cambioclaro.app;

import android.app.job.JobParameters;
import android.app.job.JobService;

/** Periodic, network-constrained update check. Android controls the exact execution time. */
public final class UpdateCheckJob extends JobService {
    @Override public boolean onStartJob(JobParameters params){
        new Thread(()->{try{GithubUpdates.checkIfDue(this);}finally{jobFinished(params,false);}} ,"github-update-check").start();
        return true;
    }
    @Override public boolean onStopJob(JobParameters params){return true;}
}
