package id.co.projectscoid.mms;

import android.content.Context;
import android.util.Log;

import com.bumptech.glide.Glide;
import com.bumptech.glide.GlideBuilder;
import com.bumptech.glide.Registry;
import com.bumptech.glide.annotation.GlideModule;
import com.bumptech.glide.load.engine.cache.DiskCache;
import com.bumptech.glide.load.engine.cache.DiskCacheAdapter;
import com.bumptech.glide.load.model.GlideUrl;
import com.bumptech.glide.module.AppGlideModule;

import id.co.projectscoid.contacts.avatars.ContactPhoto;
import id.co.projectscoid.giph.model.GiphyPaddedUrl;
import id.co.projectscoid.glide.ContactPhotoLoader;
import id.co.projectscoid.glide.GiphyPaddedUrlLoader;
import id.co.projectscoid.glide.OkHttpUrlLoader;
import id.co.projectscoid.mms.AttachmentStreamUriLoader.AttachmentModel;
import id.co.projectscoid.mms.DecryptableStreamUriLoader.DecryptableUri;

import java.io.InputStream;

@GlideModule
public class SignalGlideModule extends AppGlideModule {

  @Override
  public boolean isManifestParsingEnabled() {
    return false;
  }

  @Override
  public void applyOptions(Context context, GlideBuilder builder) {
    builder.setLogLevel(Log.ERROR);
//    builder.setDiskCache(new NoopDiskCacheFactory());
  }

  @Override
  public void registerComponents(Context context, Glide glide, Registry registry) {
    registry.append(ContactPhoto.class, InputStream.class, new ContactPhotoLoader.Factory(context));
    registry.append(DecryptableUri.class, InputStream.class, new DecryptableStreamUriLoader.Factory(context));
    registry.append(AttachmentModel.class, InputStream.class, new AttachmentStreamUriLoader.Factory());
    registry.append(GiphyPaddedUrl.class, InputStream.class, new GiphyPaddedUrlLoader.Factory());
    registry.replace(GlideUrl.class, InputStream.class, new OkHttpUrlLoader.Factory());
  }

  public static class NoopDiskCacheFactory implements DiskCache.Factory {
    @Override
    public DiskCache build() {
      return new DiskCacheAdapter();
    }
  }
}
