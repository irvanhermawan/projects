package id.co.projectscoid.contacts.avatars;

import android.support.annotation.NonNull;

import id.co.projectscoid.color.MaterialColor;
import id.co.projectscoid.color.MaterialColors;

public class ContactColors {

  public static final MaterialColor UNKNOWN_COLOR = MaterialColor.GREY;

  public static MaterialColor generateFor(@NonNull String name) {
    return MaterialColors.CONVERSATION_PALETTE.get(Math.abs(name.hashCode()) % MaterialColors.CONVERSATION_PALETTE.size());
  }

}
