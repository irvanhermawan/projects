package id.co.projectscoid.giph.ui;


import android.os.Bundle;
import android.support.v4.content.Loader;

import id.co.projectscoid.giph.model.GiphyImage;
import id.co.projectscoid.giph.net.GiphyGifLoader;

import java.util.List;

public class GiphyGifFragment extends GiphyFragment {

  @Override
  public Loader<List<GiphyImage>> onCreateLoader(int id, Bundle args) {
    return new GiphyGifLoader(getActivity(), searchString);
  }

}
