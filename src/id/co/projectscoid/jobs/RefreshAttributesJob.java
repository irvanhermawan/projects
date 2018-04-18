package id.co.projectscoid.jobs;

import android.content.Context;
import android.util.Log;

import id.co.projectscoid.dependencies.InjectableType;
import id.co.projectscoid.util.TextSecurePreferences;
import org.whispersystems.jobqueue.JobParameters;
import org.whispersystems.jobqueue.requirements.NetworkRequirement;
import id.co.projectscoid.service.ProjectsServiceAccountManager;
import org.whispersystems.signalservice.api.push.exceptions.NetworkFailureException;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

public class RefreshAttributesJob extends ContextJob implements InjectableType {

  public static final long serialVersionUID = 1L;

  private static final String TAG = RefreshAttributesJob.class.getSimpleName();

  @Inject transient ProjectsServiceAccountManager signalAccountManager;

  public RefreshAttributesJob(Context context) {
    super(context, JobParameters.newBuilder()
                                .withPersistence()
                                .withRequirement(new NetworkRequirement(context))
                                .withWakeLock(true, 30, TimeUnit.SECONDS)
                                .withGroupId(RefreshAttributesJob.class.getName())
                                .create());
  }

  @Override
  public void onAdded() {}

  @Override
  public void onRun() throws IOException {
    String  signalingKey    = TextSecurePreferences.getSignalingKey(context);
    int     registrationId  = TextSecurePreferences.getLocalRegistrationId(context);
    boolean fetchesMessages = TextSecurePreferences.isGcmDisabled(context);
    String  pin             = TextSecurePreferences.getRegistrationLockPin(context);

    signalAccountManager.setAccountAttributes(signalingKey, registrationId, fetchesMessages, pin);
  }

  @Override
  public boolean onShouldRetry(Exception e) {
    return e instanceof NetworkFailureException;
  }

  @Override
  public void onCanceled() {
    Log.w(TAG, "Failed to update account attributes!");
  }
}
