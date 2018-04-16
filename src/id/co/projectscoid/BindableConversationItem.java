package id.co.projectscoid;

import android.support.annotation.NonNull;

import id.co.projectscoid.crypto.MasterSecret;
import id.co.projectscoid.database.model.MessageRecord;
import id.co.projectscoid.mms.GlideRequests;
import id.co.projectscoid.recipients.Recipient;

import java.util.Locale;
import java.util.Set;

public interface BindableConversationItem extends Unbindable {
  void bind(@NonNull MessageRecord messageRecord,
            @NonNull GlideRequests glideRequests,
            @NonNull Locale locale,
            @NonNull Set<MessageRecord> batchSelected,
            @NonNull Recipient recipients);

  MessageRecord getMessageRecord();
}
