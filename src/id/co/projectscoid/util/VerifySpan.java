package id.co.projectscoid.util;

import android.content.Context;
import android.support.annotation.NonNull;
import android.text.style.ClickableSpan;
import android.view.View;

import org.whispersystems.libsignal.IdentityKey;

import id.co.projectscoid.database.Address;
import id.co.projectscoid.database.documents.IdentityKeyMismatch;

//import id.co.projectscoid.VerifyIdentityActivity;

public class VerifySpan extends ClickableSpan {

  private final Context     context;
  private final Address     address;
  private final IdentityKey identityKey;

  public VerifySpan(@NonNull Context context, @NonNull IdentityKeyMismatch mismatch) {
    this.context     = context;
    this.address     = mismatch.getAddress();
    this.identityKey = mismatch.getIdentityKey();
  }

  public VerifySpan(@NonNull Context context, @NonNull Address address, @NonNull IdentityKey identityKey) {
    this.context     = context;
    this.address     = address;
    this.identityKey = identityKey;
  }

  @Override
  public void onClick(View widget) {
  /*  Intent intent = new Intent(context, VerifyIdentityActivity.class);
    intent.putExtra(VerifyIdentityActivity.ADDRESS_EXTRA, address);
    intent.putExtra(VerifyIdentityActivity.IDENTITY_EXTRA, new IdentityKeyParcelable(identityKey));
    intent.putExtra(VerifyIdentityActivity.VERIFIED_EXTRA, false);
    context.startActivity(intent); */
  }
}
