package id.co.projectscoid.jobs;

import android.content.Context;
import android.support.annotation.NonNull;
import android.util.Log;

import org.greenrobot.eventbus.EventBus;
import org.whispersystems.jobqueue.JobParameters;
import org.whispersystems.jobqueue.requirements.NetworkRequirement;
import org.whispersystems.libsignal.util.guava.Optional;
import org.whispersystems.signalservice.api.messages.SignalServiceAttachment;
import org.whispersystems.signalservice.api.messages.SignalServiceAttachment.ProgressListener;
import org.whispersystems.signalservice.api.push.SignalServiceAddress;

import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedList;
import java.util.List;

import id.co.projectscoid.attachments.Attachment;
import id.co.projectscoid.crypto.MasterSecret;
import id.co.projectscoid.crypto.ProfileKeyUtil;
import id.co.projectscoid.database.Address;
import id.co.projectscoid.database.DatabaseFactory;
import id.co.projectscoid.events.PartProgressEvent;
import id.co.projectscoid.jobs.requirements.MasterSecretRequirement;
import id.co.projectscoid.mms.PartAuthority;
import id.co.projectscoid.notifications.MessageNotifier;
import id.co.projectscoid.recipients.Recipient;

public abstract class PushSendJob extends SendJob {

  private static final String TAG = PushSendJob.class.getSimpleName();

  protected PushSendJob(Context context, JobParameters parameters) {
    super(context, parameters);
  }

  protected static JobParameters constructParameters(Context context, Address destination) {
    JobParameters.Builder builder = JobParameters.newBuilder();
    builder.withPersistence();
    builder.withGroupId(destination.serialize());
    builder.withRequirement(new MasterSecretRequirement(context));
    builder.withRequirement(new NetworkRequirement(context));
    builder.withRetryCount(5);

    return builder.create();
  }

  @Override
  protected final void onSend(MasterSecret masterSecret) throws Exception {
  /*  if (TextSecurePreferences.getSignedPreKeyFailureCount(context) > 5) {
      ApplicationContext.getInstance(context)
                        .getJobManager()
                        .add(new RotateSignedPreKeyJob(context));

      throw new TextSecureExpiredException("Too many signed prekey rotation failures");
    }

    onPushSend();*/
  }

  protected Optional<byte[]> getProfileKey(@NonNull Recipient recipient) {
    if (!recipient.resolve().isSystemContact() && !recipient.resolve().isProfileSharing()) {
      return Optional.absent();
    }

    return Optional.of(ProfileKeyUtil.getProfileKey(context));
  }

  protected SignalServiceAddress getPushAddress(Address address) {
//    String relay = TextSecureDirectory.getInstance(context).getRelay(address.toPhoneString());
    String relay = null;
    return new SignalServiceAddress(address.toPhoneString(), Optional.fromNullable(relay));
  }

  protected List<SignalServiceAttachment> getAttachmentsFor(List<Attachment> parts) {
    List<SignalServiceAttachment> attachments = new LinkedList<>();

    for (final Attachment attachment : parts) {
      try {
        if (attachment.getDataUri() == null || attachment.getSize() == 0) throw new IOException("Assertion failed, outgoing attachment has no data!");
        InputStream is = PartAuthority.getAttachmentStream(context, attachment.getDataUri());
        attachments.add(SignalServiceAttachment.newStreamBuilder()
                                               .withStream(is)
                                               .withContentType(attachment.getContentType())
                                               .withLength(attachment.getSize())
                                               .withFileName(attachment.getFileName())
                                               .withVoiceNote(attachment.isVoiceNote())
                                               .withListener(new ProgressListener() {
                                                 @Override
                                                 public void onAttachmentProgress(long total, long progress) {
                                                   EventBus.getDefault().postSticky(new PartProgressEvent(attachment, total, progress));
                                                 }
                                               })
                                               .build());
      } catch (IOException ioe) {
        Log.w(TAG, "Couldn't open attachment", ioe);
      }
    }

    return attachments;
  }

  protected void notifyMediaMessageDeliveryFailed(Context context, long messageId) {
    long      threadId  = DatabaseFactory.getMmsDatabase(context).getThreadIdForMessage(messageId);
    Recipient recipient = DatabaseFactory.getThreadDatabase(context).getRecipientForThreadId(threadId);

    if (threadId != -1 && recipient != null) {
      MessageNotifier.notifyMessageDeliveryFailed(context, recipient, threadId);
    }
  }

  protected abstract void onPushSend() throws Exception;
}
