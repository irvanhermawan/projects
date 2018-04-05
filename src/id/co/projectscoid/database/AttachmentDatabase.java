/*
 * Copyright (C) 2011 Whisper Systems
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package id.co.projectscoid.database;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;
import android.text.TextUtils;
import android.util.Log;
import android.util.Pair;

import net.sqlcipher.database.SQLiteDatabase;

import id.co.projectscoid.attachments.Attachment;
import id.co.projectscoid.attachments.AttachmentId;
import id.co.projectscoid.attachments.DatabaseAttachment;
import id.co.projectscoid.crypto.AttachmentSecret;
import id.co.projectscoid.crypto.ClassicDecryptingPartInputStream;
import id.co.projectscoid.crypto.ModernDecryptingPartInputStream;
import id.co.projectscoid.crypto.ModernEncryptingPartOutputStream;
import id.co.projectscoid.database.helpers.SQLCipherOpenHelper;
import id.co.projectscoid.mms.MediaStream;
import id.co.projectscoid.mms.MmsException;
import id.co.projectscoid.mms.PartAuthority;
import id.co.projectscoid.util.MediaUtil;
import id.co.projectscoid.util.MediaUtil.ThumbnailData;
import id.co.projectscoid.util.StorageUtil;
import id.co.projectscoid.util.Util;
import id.co.projectscoid.video.EncryptedMediaDataSource;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;

public class AttachmentDatabase extends Database {
  
  private static final String TAG = AttachmentDatabase.class.getSimpleName();

  public  static final String TABLE_NAME             = "part";
  public  static final String ROW_ID                 = "_id";
  public  static final String ATTACHMENT_ID_ALIAS    = "attachment_id";
          static final String MMS_ID                 = "mid";
          static final String CONTENT_TYPE           = "ct";
          static final String NAME                   = "name";
          static final String CONTENT_DISPOSITION    = "cd";
          static final String CONTENT_LOCATION       = "cl";
  public  static final String DATA                   = "_data";
          static final String TRANSFER_STATE         = "pending_push";
  public  static final String SIZE                   = "data_size";
          static final String FILE_NAME              = "file_name";
  public  static final String THUMBNAIL              = "thumbnail";
          static final String THUMBNAIL_ASPECT_RATIO = "aspect_ratio";
  public  static final String UNIQUE_ID              = "unique_id";
          static final String DIGEST                 = "digest";
          static final String VOICE_NOTE             = "voice_note";
  public  static final String FAST_PREFLIGHT_ID      = "fast_preflight_id";
  public  static final String DATA_RANDOM            = "data_random";
  private static final String THUMBNAIL_RANDOM       = "thumbnail_random";

  public  static final String DIRECTORY              = "parts";

  public static final int TRANSFER_PROGRESS_DONE    = 0;
  public static final int TRANSFER_PROGRESS_STARTED = 1;
  public static final int TRANSFER_PROGRESS_PENDING = 2;
  public static final int TRANSFER_PROGRESS_FAILED  = 3;

  private static final String PART_ID_WHERE = ROW_ID + " = ? AND " + UNIQUE_ID + " = ?";

  private static final String[] PROJECTION = new String[] {ROW_ID + " AS " + ATTACHMENT_ID_ALIAS,
                                                           MMS_ID, CONTENT_TYPE, NAME, CONTENT_DISPOSITION,
                                                           CONTENT_LOCATION, DATA, THUMBNAIL, TRANSFER_STATE,
                                                           SIZE, FILE_NAME, THUMBNAIL, THUMBNAIL_ASPECT_RATIO,
                                                           UNIQUE_ID, DIGEST, FAST_PREFLIGHT_ID, VOICE_NOTE,
                                                           DATA_RANDOM, THUMBNAIL_RANDOM};

  public static final String CREATE_TABLE = "CREATE TABLE " + TABLE_NAME + " (" + ROW_ID + " INTEGER PRIMARY KEY, " +
    MMS_ID + " INTEGER, " + "seq" + " INTEGER DEFAULT 0, "                        +
    CONTENT_TYPE + " TEXT, " + NAME + " TEXT, " + "chset" + " INTEGER, "             +
    CONTENT_DISPOSITION + " TEXT, " + "fn" + " TEXT, " + "cid" + " TEXT, "  +
    CONTENT_LOCATION + " TEXT, " + "ctt_s" + " INTEGER, "                 +
    "ctt_t" + " TEXT, " + "encrypted" + " INTEGER, "                         +
    TRANSFER_STATE + " INTEGER, "+ DATA + " TEXT, " + SIZE + " INTEGER, "   +
    FILE_NAME + " TEXT, " + THUMBNAIL + " TEXT, " + THUMBNAIL_ASPECT_RATIO + " REAL, " +
    UNIQUE_ID + " INTEGER NOT NULL, " + DIGEST + " BLOB, " + FAST_PREFLIGHT_ID + " TEXT, " +
    VOICE_NOTE + " INTEGER DEFAULT 0, " + DATA_RANDOM + " BLOB, " + THUMBNAIL_RANDOM + " BLOB);";

  public static final String[] CREATE_INDEXS = {
    "CREATE INDEX IF NOT EXISTS part_mms_id_index ON " + TABLE_NAME + " (" + MMS_ID + ");",
    "CREATE INDEX IF NOT EXISTS pending_push_index ON " + TABLE_NAME + " (" + TRANSFER_STATE + ");",
  };

  private final ExecutorService thumbnailExecutor = Util.newSingleThreadedLifoExecutor();

  private final AttachmentSecret attachmentSecret;

  public AttachmentDatabase(Context context, SQLCipherOpenHelper databaseHelper, AttachmentSecret attachmentSecret) {
    super(context, databaseHelper);
    this.attachmentSecret = attachmentSecret;
  }

  public @NonNull InputStream getAttachmentStream(AttachmentId attachmentId, long offset)
      throws IOException
  {
    InputStream dataStream = getDataStream(attachmentId, DATA, offset);

    if (dataStream == null) throw new IOException("No stream for: " + attachmentId);
    else                    return dataStream;
  }

  public @NonNull InputStream getThumbnailStream(@NonNull AttachmentId attachmentId)
      throws IOException
  {
    Log.w(TAG, "getThumbnailStream(" + attachmentId + ")");
    InputStream dataStream = getDataStream(attachmentId, THUMBNAIL, 0);

    if (dataStream != null) {
      return dataStream;
    }

    try {
      InputStream generatedStream = thumbnailExecutor.submit(new ThumbnailFetchCallable(attachmentId)).get();

      if (generatedStream == null) throw new FileNotFoundException("No thumbnail stream available: " + attachmentId);
      else                         return generatedStream;
    } catch (InterruptedException ie) {
      throw new AssertionError("interrupted");
    } catch (ExecutionException ee) {
      Log.w(TAG, ee);
      throw new IOException(ee);
    }
  }

  public void setTransferProgressFailed(AttachmentId attachmentId, long mmsId)
      throws MmsException
  {
    SQLiteDatabase database = databaseHelper.getWritableDatabase();
    ContentValues  values   = new ContentValues();
    values.put(TRANSFER_STATE, TRANSFER_PROGRESS_FAILED);

    database.update(TABLE_NAME, values, PART_ID_WHERE, attachmentId.toStrings());
    notifyConversationListeners(DatabaseFactory.getMmsDatabase(context).getThreadIdForMessage(mmsId));
  }

  public @Nullable DatabaseAttachment getAttachment(@NonNull AttachmentId attachmentId)
  {
    SQLiteDatabase database = databaseHelper.getReadableDatabase();
    Cursor cursor           = null;

    try {
      cursor = database.query(TABLE_NAME, PROJECTION, PART_ID_WHERE, attachmentId.toStrings(), null, null, null);

      if (cursor != null && cursor.moveToFirst()) return getAttachment(cursor);
      else                                        return null;

    } finally {
      if (cursor != null)
        cursor.close();
    }
  }

  public @NonNull List<DatabaseAttachment> getAttachmentsForMessage(long mmsId) {
    SQLiteDatabase           database = databaseHelper.getReadableDatabase();
    List<DatabaseAttachment> results  = new LinkedList<>();
    Cursor                   cursor   = null;

    try {
      cursor = database.query(TABLE_NAME, PROJECTION, MMS_ID + " = ?", new String[] {mmsId+""},
                              null, null, null);

      while (cursor != null && cursor.moveToNext()) {
        results.add(getAttachment(cursor));
      }

      return results;
    } finally {
      if (cursor != null)
        cursor.close();
    }
  }

  public @NonNull List<DatabaseAttachment> getPendingAttachments() {
    final SQLiteDatabase           database    = databaseHelper.getReadableDatabase();
    final List<DatabaseAttachment> attachments = new LinkedList<>();

    Cursor cursor = null;
    try {
      cursor = database.query(TABLE_NAME, PROJECTION, TRANSFER_STATE + " = ?", new String[] {String.valueOf(TRANSFER_PROGRESS_STARTED)}, null, null, null);
      while (cursor != null && cursor.moveToNext()) {
        attachments.add(getAttachment(cursor));
      }
    } finally {
      if (cursor != null) cursor.close();
    }

    return attachments;
  }

  @SuppressWarnings("ResultOfMethodCallIgnored")
  void deleteAttachmentsForMessage(long mmsId) {
    SQLiteDatabase database = databaseHelper.getWritableDatabase();
    Cursor cursor           = null;

    try {
      cursor = database.query(TABLE_NAME, new String[] {DATA, THUMBNAIL}, MMS_ID + " = ?",
                              new String[] {mmsId+""}, null, null, null);

      while (cursor != null && cursor.moveToNext()) {
        String data = cursor.getString(0);
        String thumbnail = cursor.getString(1);

        if (!TextUtils.isEmpty(data)) {
          new File(data).delete();
        }

        if (!TextUtils.isEmpty(thumbnail)) {
          new File(thumbnail).delete();
        }
      }
    } finally {
      if (cursor != null)
        cursor.close();
    }

    database.delete(TABLE_NAME, MMS_ID + " = ?", new String[] {mmsId + ""});
  }

  @SuppressWarnings("ResultOfMethodCallIgnored")
  void deleteAllAttachments() {
    SQLiteDatabase database = databaseHelper.getWritableDatabase();
    database.delete(TABLE_NAME, null, null);

    File   attachmentsDirectory = context.getDir(DIRECTORY, Context.MODE_PRIVATE);
    File[] attachments          = attachmentsDirectory.listFiles();

    for (File attachment : attachments) {
      attachment.delete();
    }
  }

  public void insertAttachmentsForPlaceholder(long mmsId, @NonNull AttachmentId attachmentId, @NonNull InputStream inputStream)
      throws MmsException
  {
    SQLiteDatabase database = databaseHelper.getWritableDatabase();
    DataInfo       dataInfo = setAttachmentData(inputStream);
    ContentValues  values   = new ContentValues();

    values.put(DATA, dataInfo.file.getAbsolutePath());
    values.put(SIZE, dataInfo.length);
    values.put(DATA_RANDOM, dataInfo.random);
    values.put(TRANSFER_STATE, TRANSFER_PROGRESS_DONE);
    values.put(CONTENT_LOCATION, (String)null);
    values.put(CONTENT_DISPOSITION, (String)null);
    values.put(DIGEST, (byte[])null);
    values.put(NAME, (String) null);
    values.put(FAST_PREFLIGHT_ID, (String)null);

    if (database.update(TABLE_NAME, values, PART_ID_WHERE, attachmentId.toStrings()) == 0) {
      //noinspection ResultOfMethodCallIgnored
      dataInfo.file.delete();
    } else {
      notifyConversationListeners(DatabaseFactory.getMmsDatabase(context).getThreadIdForMessage(mmsId));
      notifyConversationListListeners();
    }

    thumbnailExecutor.submit(new ThumbnailFetchCallable(attachmentId));
  }

  void insertAttachmentsForMessage(long mmsId, @NonNull List<Attachment> attachments)
      throws MmsException
  {
    Log.w(TAG, "insertParts(" + attachments.size() + ")");

    for (Attachment attachment : attachments) {
      AttachmentId attachmentId = insertAttachment(mmsId, attachment);
      Log.w(TAG, "Inserted attachment at ID: " + attachmentId);
    }
  }

  public @NonNull Attachment updateAttachmentData(@NonNull Attachment attachment,
                                                  @NonNull MediaStream mediaStream)
      throws MmsException
  {
    SQLiteDatabase     database           = databaseHelper.getWritableDatabase();
    DatabaseAttachment databaseAttachment = (DatabaseAttachment) attachment;
    DataInfo           dataInfo           = getAttachmentDataFileInfo(databaseAttachment.getAttachmentId(), DATA);

    if (dataInfo == null) {
      throw new MmsException("No attachment data found!");
    }

    dataInfo = setAttachmentData(dataInfo.file, mediaStream.getStream());

    ContentValues contentValues = new ContentValues();
    contentValues.put(SIZE, dataInfo.length);
    contentValues.put(CONTENT_TYPE, mediaStream.getMimeType());
    contentValues.put(DATA_RANDOM, dataInfo.random);

    database.update(TABLE_NAME, contentValues, PART_ID_WHERE, databaseAttachment.getAttachmentId().toStrings());

    return new DatabaseAttachment(databaseAttachment.getAttachmentId(),
                                  databaseAttachment.getMmsId(),
                                  databaseAttachment.hasData(),
                                  databaseAttachment.hasThumbnail(),
                                  mediaStream.getMimeType(),
                                  databaseAttachment.getTransferState(),
                                  dataInfo.length,
                                  databaseAttachment.getFileName(),
                                  databaseAttachment.getLocation(),
                                  databaseAttachment.getKey(),
                                  databaseAttachment.getRelay(),
                                  databaseAttachment.getDigest(),
                                  databaseAttachment.getFastPreflightId(),
                                  databaseAttachment.isVoiceNote());
  }


  public void updateAttachmentFileName(@NonNull AttachmentId attachmentId,
                                       @Nullable String fileName)
  {
    SQLiteDatabase database = databaseHelper.getWritableDatabase();

    ContentValues contentValues = new ContentValues(1);
    contentValues.put(FILE_NAME, StorageUtil.getCleanFileName(fileName));

    database.update(TABLE_NAME, contentValues, PART_ID_WHERE, attachmentId.toStrings());
  }

  public void markAttachmentUploaded(long messageId, Attachment attachment) {
    ContentValues  values   = new ContentValues(1);
    SQLiteDatabase database = databaseHelper.getWritableDatabase();

    values.put(TRANSFER_STATE, TRANSFER_PROGRESS_DONE);
    database.update(TABLE_NAME, values, PART_ID_WHERE, ((DatabaseAttachment)attachment).getAttachmentId().toStrings());

    notifyConversationListeners(DatabaseFactory.getMmsDatabase(context).getThreadIdForMessage(messageId));
  }

  public void setTransferState(long messageId, @NonNull Attachment attachment, int transferState) {
    if (!(attachment instanceof DatabaseAttachment)) {
      throw new AssertionError("Attempt to update attachment that doesn't belong to DB!");
    }

    setTransferState(messageId, ((DatabaseAttachment) attachment).getAttachmentId(), transferState);
  }

  public void setTransferState(long messageId, @NonNull AttachmentId attachmentId, int transferState) {
    final ContentValues  values   = new ContentValues(1);
    final SQLiteDatabase database = databaseHelper.getWritableDatabase();

    values.put(TRANSFER_STATE, transferState);
    database.update(TABLE_NAME, values, PART_ID_WHERE, attachmentId.toStrings());
    notifyConversationListeners(DatabaseFactory.getMmsDatabase(context).getThreadIdForMessage(messageId));
  }

  @SuppressWarnings("WeakerAccess")
  @VisibleForTesting
  protected @Nullable InputStream getDataStream(AttachmentId attachmentId, String dataType, long offset)
  {
    DataInfo dataInfo = getAttachmentDataFileInfo(attachmentId, dataType);

    if (dataInfo == null) {
      return null;
    }

    try {
      if (dataInfo.random != null && dataInfo.random.length == 32) {
        return ModernDecryptingPartInputStream.createFor(attachmentSecret, dataInfo.random, dataInfo.file, offset);
      } else {
        InputStream stream  = ClassicDecryptingPartInputStream.createFor(attachmentSecret, dataInfo.file);
        long        skipped = stream.skip(offset);

        if (skipped != offset) {
          Log.w(TAG, "Skip failed: " + skipped + " vs " + offset);
          return null;
        }

        return stream;
      }
    } catch (IOException e) {
      Log.w(TAG, e);
      return null;
    }
  }

  private @Nullable DataInfo getAttachmentDataFileInfo(@NonNull AttachmentId attachmentId, @NonNull String dataType)
  {
    SQLiteDatabase database = databaseHelper.getReadableDatabase();
    Cursor         cursor   = null;

    String randomColumn;

    switch (dataType) {
      case DATA:      randomColumn = DATA_RANDOM;      break;
      case THUMBNAIL: randomColumn = THUMBNAIL_RANDOM; break;
      default:throw   new AssertionError("Unknown data type: " + dataType);
    }

    try {
      cursor = database.query(TABLE_NAME, new String[]{dataType, SIZE, randomColumn}, PART_ID_WHERE, attachmentId.toStrings(),
                              null, null, null);

      if (cursor != null && cursor.moveToFirst()) {
        if (cursor.isNull(0)) {
          return null;
        }

        return new DataInfo(new File(cursor.getString(0)),
                            cursor.getLong(1),
                            cursor.getBlob(2));
      } else {
        return null;
      }
    } finally {
      if (cursor != null)
        cursor.close();
    }

  }

  private @NonNull DataInfo setAttachmentData(@NonNull Uri uri)
      throws MmsException
  {
    try {
      InputStream inputStream = PartAuthority.getAttachmentStream(context, uri);
      return setAttachmentData(inputStream);
    } catch (IOException e) {
      throw new MmsException(e);
    }
  }

  private @NonNull DataInfo setAttachmentData(@NonNull InputStream in)
      throws MmsException
  {
    try {
      File partsDirectory = context.getDir(DIRECTORY, Context.MODE_PRIVATE);
      File dataFile       = File.createTempFile("part", ".mms", partsDirectory);
      return setAttachmentData(dataFile, in);
    } catch (IOException e) {
      throw new MmsException(e);
    }
  }

  private @NonNull DataInfo setAttachmentData(@NonNull File destination, @NonNull InputStream in)
      throws MmsException
  {
    try {
      Pair<byte[], OutputStream> out    = ModernEncryptingPartOutputStream.createFor(attachmentSecret, destination, false);
      long                       length = Util.copy(in, out.second);

      return new DataInfo(destination, length, out.first);
    } catch (IOException e) {
      throw new MmsException(e);
    }
  }

  DatabaseAttachment getAttachment(@NonNull Cursor cursor) {
    return new DatabaseAttachment(new AttachmentId(cursor.getLong(cursor.getColumnIndexOrThrow(ATTACHMENT_ID_ALIAS)),
                                                   cursor.getLong(cursor.getColumnIndexOrThrow(UNIQUE_ID))),
                                  cursor.getLong(cursor.getColumnIndexOrThrow(MMS_ID)),
                                  !cursor.isNull(cursor.getColumnIndexOrThrow(DATA)),
                                  !cursor.isNull(cursor.getColumnIndexOrThrow(THUMBNAIL)),
                                  cursor.getString(cursor.getColumnIndexOrThrow(CONTENT_TYPE)),
                                  cursor.getInt(cursor.getColumnIndexOrThrow(TRANSFER_STATE)),
                                  cursor.getLong(cursor.getColumnIndexOrThrow(SIZE)),
                                  StorageUtil.getCleanFileName(cursor.getString(cursor.getColumnIndexOrThrow(FILE_NAME))),
                                  cursor.getString(cursor.getColumnIndexOrThrow(CONTENT_LOCATION)),
                                  cursor.getString(cursor.getColumnIndexOrThrow(CONTENT_DISPOSITION)),
                                  cursor.getString(cursor.getColumnIndexOrThrow(NAME)),
                                  cursor.getBlob(cursor.getColumnIndexOrThrow(DIGEST)),
                                  cursor.getString(cursor.getColumnIndexOrThrow(FAST_PREFLIGHT_ID)),
                                  cursor.getInt(cursor.getColumnIndexOrThrow(VOICE_NOTE)) == 1);
  }


  private AttachmentId insertAttachment(long mmsId, Attachment attachment)
      throws MmsException
  {
    Log.w(TAG, "Inserting attachment for mms id: " + mmsId);

    SQLiteDatabase database = databaseHelper.getWritableDatabase();
    DataInfo       dataInfo = null;
    long           uniqueId = System.currentTimeMillis();

    if (attachment.getDataUri() != null) {
      dataInfo = setAttachmentData(attachment.getDataUri());
      Log.w(TAG, "Wrote part to file: " + dataInfo.file.getAbsolutePath());
    }

    ContentValues contentValues = new ContentValues();
    contentValues.put(MMS_ID, mmsId);
    contentValues.put(CONTENT_TYPE, attachment.getContentType());
    contentValues.put(TRANSFER_STATE, attachment.getTransferState());
    contentValues.put(UNIQUE_ID, uniqueId);
    contentValues.put(CONTENT_LOCATION, attachment.getLocation());
    contentValues.put(DIGEST, attachment.getDigest());
    contentValues.put(CONTENT_DISPOSITION, attachment.getKey());
    contentValues.put(NAME, attachment.getRelay());
    contentValues.put(FILE_NAME, StorageUtil.getCleanFileName(attachment.getFileName()));
    contentValues.put(SIZE, attachment.getSize());
    contentValues.put(FAST_PREFLIGHT_ID, attachment.getFastPreflightId());
    contentValues.put(VOICE_NOTE, attachment.isVoiceNote() ? 1 : 0);

    if (dataInfo != null) {
      contentValues.put(DATA, dataInfo.file.getAbsolutePath());
      contentValues.put(SIZE, dataInfo.length);
      contentValues.put(DATA_RANDOM, dataInfo.random);
    }

    long         rowId        = database.insert(TABLE_NAME, null, contentValues);
    AttachmentId attachmentId = new AttachmentId(rowId, uniqueId);

    if (dataInfo != null) {
      if (MediaUtil.hasVideoThumbnail(attachment.getDataUri())) {
        Bitmap bitmap = MediaUtil.getVideoThumbnail(context, attachment.getDataUri());

        if (bitmap != null) {
          ThumbnailData thumbnailData = new ThumbnailData(bitmap);
          updateAttachmentThumbnail(attachmentId, thumbnailData.toDataStream(), thumbnailData.getAspectRatio());
        } else {
          Log.w(TAG, "Retrieving video thumbnail failed, submitting thumbnail generation job...");
          thumbnailExecutor.submit(new ThumbnailFetchCallable(attachmentId));
        }
      } else {
        Log.w(TAG, "Submitting thumbnail generation job...");
        thumbnailExecutor.submit(new ThumbnailFetchCallable(attachmentId));
      }
    }

    return attachmentId;
  }

  @SuppressWarnings("WeakerAccess")
  @VisibleForTesting
  protected void updateAttachmentThumbnail(AttachmentId attachmentId, InputStream in, float aspectRatio)
      throws MmsException
  {
    Log.w(TAG, "updating part thumbnail for #" + attachmentId);

    DataInfo thumbnailFile = setAttachmentData(in);

    SQLiteDatabase database = databaseHelper.getWritableDatabase();
    ContentValues  values   = new ContentValues(2);

    values.put(THUMBNAIL, thumbnailFile.file.getAbsolutePath());
    values.put(THUMBNAIL_ASPECT_RATIO, aspectRatio);
    values.put(THUMBNAIL_RANDOM, thumbnailFile.random);

    database.update(TABLE_NAME, values, PART_ID_WHERE, attachmentId.toStrings());

    Cursor cursor = database.query(TABLE_NAME, new String[] {MMS_ID}, PART_ID_WHERE, attachmentId.toStrings(), null, null, null);

    try {
      if (cursor != null && cursor.moveToFirst()) {
        notifyConversationListeners(DatabaseFactory.getMmsDatabase(context).getThreadIdForMessage(cursor.getLong(cursor.getColumnIndexOrThrow(MMS_ID))));
      }
    } finally {
      if (cursor != null) cursor.close();
    }
  }


  @VisibleForTesting
  class ThumbnailFetchCallable implements Callable<InputStream> {

    private final AttachmentId attachmentId;

    ThumbnailFetchCallable(AttachmentId attachmentId) {
      this.attachmentId = attachmentId;
    }

    @Override
    public @Nullable InputStream call() throws Exception {
      Log.w(TAG, "Executing thumbnail job...");
      final InputStream stream = getDataStream(attachmentId, THUMBNAIL, 0);

      if (stream != null) {
        return stream;
      }

      DatabaseAttachment attachment = getAttachment(attachmentId);

      if (attachment == null || !attachment.hasData()) {
        return null;
      }

      ThumbnailData data;

      if (MediaUtil.isVideoType(attachment.getContentType())) {
        data = generateVideoThumbnail(attachmentId);
      } else{
        data = MediaUtil.generateThumbnail(context, attachment.getContentType(), attachment.getDataUri());
      }

      if (data == null) {
        return null;
      }

      updateAttachmentThumbnail(attachmentId, data.toDataStream(), data.getAspectRatio());

      return getDataStream(attachmentId, THUMBNAIL, 0);
    }

    @SuppressLint("NewApi")
    private ThumbnailData generateVideoThumbnail(AttachmentId attachmentId) {
      if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
        Log.w(TAG, "Video thumbnails not supported...");
        return null;
      }

      DataInfo dataInfo = getAttachmentDataFileInfo(attachmentId, DATA);

      if (dataInfo == null) {
        Log.w(TAG, "No data file found for video thumbnail...");
        return null;
      }

      EncryptedMediaDataSource dataSource = new EncryptedMediaDataSource(attachmentSecret, dataInfo.file, dataInfo.random, dataInfo.length);
      MediaMetadataRetriever   retriever  = new MediaMetadataRetriever();
      retriever.setDataSource(dataSource);

      Bitmap bitmap = retriever.getFrameAtTime(1000);

      Log.w(TAG, "Generated video thumbnail...");
      return new ThumbnailData(bitmap);
    }
  }

  private static class DataInfo {
    private final File   file;
    private final long   length;
    private final byte[] random;

    private DataInfo(File file, long length, byte[] random) {
      this.file = file;
      this.length = length;
      this.random = random;
    }
  }
}