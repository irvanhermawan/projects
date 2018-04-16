package id.co.projectscoid;

import android.support.annotation.NonNull;

import id.co.projectscoid.crypto.MasterSecret;
import id.co.projectscoid.database.model.ThreadRecord;
import id.co.projectscoid.mms.GlideRequests;

import java.util.Locale;
import java.util.Set;

public interface BindableConversationListItem extends Unbindable {

  public void bind(@NonNull ThreadRecord thread,
                   @NonNull GlideRequests glideRequests, @NonNull Locale locale,
                   @NonNull Set<Long> selectedThreads, boolean batchMode);
}
