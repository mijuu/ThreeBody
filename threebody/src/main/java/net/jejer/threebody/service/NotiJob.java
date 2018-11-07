package net.jejer.threebody.service;

import android.support.annotation.NonNull;

import com.evernote.android.job.Job;

import net.jejer.threebody.bean.HiSettingsHelper;
import net.jejer.threebody.okhttp.OkHttpHelper;
import net.jejer.threebody.ui.HiApplication;
import net.jejer.threebody.utils.Logger;

/**
 * Created by GreenSkinMonster on 2017-07-19.
 */

public class NotiJob extends Job {

    public static final String TAG = "noti_job_tag";

    @Override
    @NonNull
    protected Result onRunJob(Params params) {
        if (!OkHttpHelper.getInstance().isLoggedIn()) {
            NotiHelper.cancelJob();
        } else {
            if (!HiApplication.isAppVisible()
                    && !HiSettingsHelper.getInstance().isInSilentMode()) {
                HiSettingsHelper.getInstance().setNotiJobLastRunTime();
                checkNotifications();
            }
        }
        return Result.SUCCESS;
    }

    private void checkNotifications() {
        try {
            NotiHelper.fetchNotification(null);
            NotiHelper.showNotification(HiApplication.getAppContext());
        } catch (Exception e) {
            Logger.e(e);
        }
    }

}
