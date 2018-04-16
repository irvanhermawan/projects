package id.co.projectscoid.attachments;


import android.net.Uri;
import android.support.annotation.Nullable;

import id.co.projectscoid.database.AttachmentDatabase;
import id.co.projectscoid.database.MmsDatabase;

public class MmsNotificationAttachment extends Attachment {

  public MmsNotificationAttachment(int status, long size) {
    super("application/mms", getTransferStateFromStatus(status), size, null, null, null, null, null, null, false, 0, 0);
  }

  @Nullable
  @Override
  public Uri getDataUri() {
    return null;
  }

  @Nullable
  @Override
  public Uri getThumbnailUri() {
    return null;
  }

  private static int getTransferStateFromStatus(int status) {
    if (status == MmsDatabase.Status.DOWNLOAD_INITIALIZED ||
        status == MmsDatabase.Status.DOWNLOAD_NO_CONNECTIVITY)
    {
      return AttachmentDatabase.TRANSFER_PROGRESS_PENDING;
    } else if (status == MmsDatabase.Status.DOWNLOAD_CONNECTING) {
      return AttachmentDatabase.TRANSFER_PROGRESS_STARTED;
    } else {
      return AttachmentDatabase.TRANSFER_PROGRESS_FAILED;
    }
  }
}
