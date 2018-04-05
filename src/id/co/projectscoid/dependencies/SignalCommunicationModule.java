package id.co.projectscoid.dependencies;

import android.content.Context;
import android.util.Log;

import org.greenrobot.eventbus.EventBus;
import org.whispersystems.libsignal.util.guava.Optional;
import org.whispersystems.signalservice.api.SignalServiceAccountManager;
import org.whispersystems.signalservice.api.SignalServiceMessageReceiver;
import org.whispersystems.signalservice.api.SignalServiceMessageSender;
import org.whispersystems.signalservice.api.util.CredentialsProvider;
import org.whispersystems.signalservice.api.websocket.ConnectivityListener;

import dagger.Module;
import dagger.Provides;
import id.co.projectscoid.BuildConfig;
import id.co.projectscoid.crypto.storage.SignalProtocolStoreImpl;
import id.co.projectscoid.events.ReminderUpdateEvent;
import id.co.projectscoid.jobs.AttachmentDownloadJob;
import id.co.projectscoid.jobs.AvatarDownloadJob;
import id.co.projectscoid.jobs.CleanPreKeysJob;
import id.co.projectscoid.jobs.CreateSignedPreKeyJob;
import id.co.projectscoid.jobs.GcmRefreshJob;
import id.co.projectscoid.jobs.MultiDeviceBlockedUpdateJob;
import id.co.projectscoid.jobs.MultiDeviceContactUpdateJob;
import id.co.projectscoid.jobs.MultiDeviceGroupUpdateJob;
import id.co.projectscoid.jobs.MultiDeviceProfileKeyUpdateJob;
import id.co.projectscoid.jobs.MultiDeviceReadReceiptUpdateJob;
import id.co.projectscoid.jobs.MultiDeviceReadUpdateJob;
import id.co.projectscoid.jobs.MultiDeviceVerifiedUpdateJob;
import id.co.projectscoid.jobs.PushGroupSendJob;
import id.co.projectscoid.jobs.PushGroupUpdateJob;
import id.co.projectscoid.jobs.PushMediaSendJob;
import id.co.projectscoid.jobs.PushNotificationReceiveJob;
import id.co.projectscoid.jobs.PushTextSendJob;
import id.co.projectscoid.jobs.RefreshAttributesJob;
import id.co.projectscoid.jobs.RefreshPreKeysJob;
import id.co.projectscoid.jobs.RequestGroupInfoJob;
import id.co.projectscoid.jobs.RetrieveProfileAvatarJob;
import id.co.projectscoid.jobs.RetrieveProfileJob;
import id.co.projectscoid.jobs.RotateSignedPreKeyJob;
import id.co.projectscoid.jobs.SendReadReceiptJob;
import id.co.projectscoid.preferences.AppProtectionPreferenceFragment;
import id.co.projectscoid.push.SecurityEventListener;
import id.co.projectscoid.push.SignalServiceNetworkAccess;
import id.co.projectscoid.service.MessageRetrievalService;
import id.co.projectscoid.service.WebRtcCallService;
import id.co.projectscoid.util.TextSecurePreferences;

//import id.co.projectscoid.CreateProfileActivity;
//import id.co.projectscoid.DeviceListFragment;

@Module(complete = false, injects = {CleanPreKeysJob.class,
                                     CreateSignedPreKeyJob.class,
                                     PushGroupSendJob.class,
                                     PushTextSendJob.class,
                                     PushMediaSendJob.class,
                                     AttachmentDownloadJob.class,
                                     RefreshPreKeysJob.class,
                                     MessageRetrievalService.class,
                                     PushNotificationReceiveJob.class,
                                     MultiDeviceContactUpdateJob.class,
                                     MultiDeviceGroupUpdateJob.class,
                                     MultiDeviceReadUpdateJob.class,
                                     MultiDeviceBlockedUpdateJob.class,
                                   //  DeviceListFragment.class,
                                     RefreshAttributesJob.class,
                                     GcmRefreshJob.class,
                                     RequestGroupInfoJob.class,
                                     PushGroupUpdateJob.class,
                                     AvatarDownloadJob.class,
                                     RotateSignedPreKeyJob.class,
                                     WebRtcCallService.class,
                                     RetrieveProfileJob.class,
                                     MultiDeviceVerifiedUpdateJob.class,
                                   //  CreateProfileActivity.class,
                                     RetrieveProfileAvatarJob.class,
                                     MultiDeviceProfileKeyUpdateJob.class,
                                     SendReadReceiptJob.class,
                                     MultiDeviceReadReceiptUpdateJob.class,
                                     AppProtectionPreferenceFragment.class})
public class SignalCommunicationModule {

  private static final String TAG = SignalCommunicationModule.class.getSimpleName();

  private final Context                      context;
  private final SignalServiceNetworkAccess   networkAccess;

  private SignalServiceAccountManager  accountManager;
  private SignalServiceMessageSender   messageSender;
  private SignalServiceMessageReceiver messageReceiver;

  public SignalCommunicationModule(Context context, SignalServiceNetworkAccess networkAccess) {
    this.context       = context;
    this.networkAccess = networkAccess;
  }

  @Provides
  synchronized SignalServiceAccountManager provideSignalAccountManager() {
    if (this.accountManager == null) {
      this.accountManager = new SignalServiceAccountManager(networkAccess.getConfiguration(context),
                                                            new DynamicCredentialsProvider(context),
                                                            BuildConfig.USER_AGENT);
    }

    return this.accountManager;
  }

  @Provides
  synchronized SignalServiceMessageSender provideSignalMessageSender() {
    if (this.messageSender == null) {
      this.messageSender = new SignalServiceMessageSender(networkAccess.getConfiguration(context),
                                                          new DynamicCredentialsProvider(context),
                                                          new SignalProtocolStoreImpl(context),
                                                          BuildConfig.USER_AGENT,
                                                          Optional.fromNullable(MessageRetrievalService.getPipe()),
                                                          Optional.of(new SecurityEventListener(context)));
    } else {
      this.messageSender.setMessagePipe(MessageRetrievalService.getPipe());
    }

    return this.messageSender;
  }

  @Provides
  synchronized SignalServiceMessageReceiver provideSignalMessageReceiver() {
    if (this.messageReceiver == null) {
      this.messageReceiver = new SignalServiceMessageReceiver(networkAccess.getConfiguration(context),
                                                              new DynamicCredentialsProvider(context),
                                                              BuildConfig.USER_AGENT,
                                                              new PipeConnectivityListener());
    }

    return this.messageReceiver;
  }

  private static class DynamicCredentialsProvider implements CredentialsProvider {

    private final Context context;

    private DynamicCredentialsProvider(Context context) {
      this.context = context.getApplicationContext();
    }

    @Override
    public String getUser() {
      return TextSecurePreferences.getLocalNumber(context);
    }

    @Override
    public String getPassword() {
      return TextSecurePreferences.getPushServerPassword(context);
    }

    @Override
    public String getSignalingKey() {
      return TextSecurePreferences.getSignalingKey(context);
    }
  }

  private class PipeConnectivityListener implements ConnectivityListener {

    @Override
    public void onConnected() {
      Log.w(TAG, "onConnected()");
    }

    @Override
    public void onConnecting() {
      Log.w(TAG, "onConnecting()");
    }

    @Override
    public void onDisconnected() {
      Log.w(TAG, "onDisconnected()");
    }

    @Override
    public void onAuthenticationFailure() {
      Log.w(TAG, "onAuthenticationFailure()");
      TextSecurePreferences.setUnauthorizedReceived(context, true);
      EventBus.getDefault().post(new ReminderUpdateEvent());
    }

  }

}
