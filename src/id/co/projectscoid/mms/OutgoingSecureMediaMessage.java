package id.co.projectscoid.mms;

import id.co.projectscoid.attachments.Attachment;
import id.co.projectscoid.recipients.Recipient;

import java.util.List;

public class OutgoingSecureMediaMessage extends OutgoingMediaMessage {

  public OutgoingSecureMediaMessage(Recipient recipient, String body,
                                    List<Attachment> attachments,
                                    long sentTimeMillis,
                                    int distributionType,
                                    long expiresIn)
  {
    super(recipient, body, attachments, sentTimeMillis, -1, expiresIn, distributionType);
  }

  public OutgoingSecureMediaMessage(OutgoingMediaMessage base) {
    super(base);
  }

  @Override
  public boolean isSecure() {
    return true;
  }
}
