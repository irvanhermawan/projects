package id.co.projectscoid.mms;

import id.co.projectscoid.attachments.Attachment;
import id.co.projectscoid.database.ThreadDatabase;
import id.co.projectscoid.recipients.Recipient;

import java.util.LinkedList;

public class OutgoingExpirationUpdateMessage extends OutgoingSecureMediaMessage {

  public OutgoingExpirationUpdateMessage(Recipient recipient, long sentTimeMillis, long expiresIn) {
    super(recipient, "", new LinkedList<Attachment>(), sentTimeMillis,
          ThreadDatabase.DistributionTypes.CONVERSATION, expiresIn);
  }

  @Override
  public boolean isExpirationUpdate() {
    return true;
  }

}
