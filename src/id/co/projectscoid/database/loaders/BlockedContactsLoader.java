package id.co.projectscoid.database.loaders;

import android.content.Context;
import android.database.Cursor;

import id.co.projectscoid.database.DatabaseFactory;
import id.co.projectscoid.util.AbstractCursorLoader;

public class BlockedContactsLoader extends AbstractCursorLoader {

  public BlockedContactsLoader(Context context) {
    super(context);
  }

  @Override
  public Cursor getCursor() {
    return DatabaseFactory.getRecipientDatabase(getContext())
                          .getBlocked();
  }

}
