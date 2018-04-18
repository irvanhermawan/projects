package id.co.projectscoid.push;


import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import com.google.android.gms.security.ProviderInstaller;

import id.co.projectscoid.BuildConfig;
import id.co.projectscoid.util.TextSecurePreferences;
//import id.co.projectscoid.service.ProjectsServiceAccountManager;
import id.co.projectscoid.service.ProjectsServiceAccountManager;

public class AccountProjectsManagerFactory {

    private static final String TAG = AccountManagerFactory.class.getName();

    public static ProjectsServiceAccountManager createManager(Context context) {
        return new ProjectsServiceAccountManager(new SignalServiceNetworkAccess(context).getConfiguration(context),
                TextSecurePreferences.getLocalNumber(context),
                TextSecurePreferences.getPushServerPassword(context),TextSecurePreferences.getUserName(context),
                TextSecurePreferences.getPassword(context),
                TextSecurePreferences.getUserId(context),
                BuildConfig.USER_AGENT);
    }

    public static ProjectsServiceAccountManager createManager(final Context context, String number, String password, String username, String userpassword, String userid) {
        if (new SignalServiceNetworkAccess(context).isCensored(number)) {
            new AsyncTask<Void, Void, Void>() {
                @Override
                protected Void doInBackground(Void... params) {
                    try {
                        ProviderInstaller.installIfNeeded(context);
                    } catch (Throwable t) {
                        Log.w(TAG, t);
                    }
                    return null;
                }
            }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        }

        return new ProjectsServiceAccountManager(new SignalServiceNetworkAccess(context).getConfiguration(number),
                number, password, username, userpassword, userid, BuildConfig.USER_AGENT);
    }

}
