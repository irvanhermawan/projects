package id.co.projectscoid.mms;


import android.content.Context;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import id.co.projectscoid.attachments.Attachment;
import id.co.projectscoid.util.StorageUtil;

public class DocumentSlide extends Slide {

  public DocumentSlide(@NonNull Context context, @NonNull Attachment attachment) {
    super(context, attachment);
  }

  public DocumentSlide(@NonNull Context context, @NonNull Uri uri,
                       @NonNull String contentType,  long size,
                       @Nullable String fileName)
  {
    super(context, constructAttachmentFromUri(context, uri, contentType, size, true, StorageUtil.getCleanFileName(fileName), false));
  }

  @Override
  public boolean hasDocument() {
    return true;
  }

}
