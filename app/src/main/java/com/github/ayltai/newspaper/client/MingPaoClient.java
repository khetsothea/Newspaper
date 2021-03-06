package com.github.ayltai.newspaper.client;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.apache.commons.io.IOUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.github.ayltai.newspaper.Constants;
import com.github.ayltai.newspaper.client.rss.RssClient;
import com.github.ayltai.newspaper.model.Image;
import com.github.ayltai.newspaper.model.Item;
import com.github.ayltai.newspaper.model.Source;
import com.github.ayltai.newspaper.model.Video;
import com.github.ayltai.newspaper.net.HttpClient;
import com.github.ayltai.newspaper.util.LogUtils;

import io.reactivex.Maybe;

final class MingPaoClient extends RssClient {
    //region Constants

    private static final String BASE_URI   = "https://news.mingpao.com/";
    private static final String BASE_IMAGE = "https://fs.mingpao.com/";
    private static final String SLASH      = "/";
    private static final String UNDERSCORE = "_";
    private static final String DATA       = "dat/";
    private static final String TAG_URL    = "URL";

    private static final String TYPE_IMAGE = "image";
    private static final String TYPE_VIDEO = "video";

    //endregion

    @Inject
    MingPaoClient(@NonNull final HttpClient client, @Nullable final Source source) {
        super(client, source);
    }

    @SuppressWarnings("checkstyle:magicnumber")
    @NonNull
    @Override
    public Maybe<Item> updateItem(@NonNull final Item item) {
        return Maybe.create(emitter -> {
            final String[] tokens    = item.getLink().substring(MingPaoClient.BASE_URI.length()).split(MingPaoClient.SLASH);
            final boolean  isInstant = item.getLink().contains("/ins/");

            try {
                final String     url    = MingPaoClient.BASE_URI + MingPaoClient.DATA + tokens[0] + MingPaoClient.SLASH + tokens[0] + MingPaoClient.UNDERSCORE + tokens[2] + MingPaoClient.SLASH + tokens[3] + "1/" + tokens[4] + (isInstant ? Constants.EMPTY : new JSONObject(IOUtils.toString(this.client.download(MingPaoClient.BASE_URI + MingPaoClient.DATA + tokens[0] + "/issuelist.js"), Client.ENCODING)).getJSONObject((tokens[0] + MingPaoClient.UNDERSCORE + tokens[2]).toUpperCase()).getJSONObject("1 " + tokens[4]).getString("E").toLowerCase()) + (isInstant ? "/content_" : "/todaycontent_") + tokens[6] + ".js";
                final JSONObject json   = new JSONObject(IOUtils.toString(this.client.download(url), Client.ENCODING));
                final JSONArray  images = json.getJSONArray("media:group");

                if (images != null) {
                    final List<Image> fullImages = MingPaoClient.extractImages(item, images);

                    if (!fullImages.isEmpty()) {
                        item.getImages().clear();
                        item.getImages().addAll(fullImages);
                    }
                }

                item.setDescription(json.getString("DESCRIPTION"));
                item.setIsFullDescription(true);

                emitter.onSuccess(item);
            } catch (final IOException | JSONException e) {
                this.handleError(emitter, e);
            }
        });
    }

    private static List<Image> extractImages(@NonNull final Item item, @NonNull final JSONArray images) {
        final List<Image> fullImages = new ArrayList<>();

        for (int i = 0; i < images.length(); i++) {
            try {
                final JSONObject image            = images.getJSONObject(i);
                final String     imageDescription = image.getString("media:title");
                final JSONArray  array            = image.getJSONArray("media:content");

                if (array != null && array.length() > 0) {
                    Image img = null;
                    int   max = 0;

                    for (int j = 0; j < array.length(); j++) {
                        final JSONObject obj    = array.getJSONObject(j).getJSONObject("ATTRIBUTES");
                        final String     type   = obj.getString("MEDIUM");
                        final int        height = obj.getInt("HEIGHT");

                        if (MingPaoClient.TYPE_IMAGE.equals(type) && height > max) {
                            img = new Image(MingPaoClient.BASE_IMAGE + obj.getString(MingPaoClient.TAG_URL), imageDescription);
                            max = height;
                        } else if (MingPaoClient.TYPE_VIDEO.equals(type)) {
                            final String videoUrl = obj.getString(MingPaoClient.TAG_URL);
                            item.setVideo(new Video(videoUrl, videoUrl.replace("mp4", "jpg")));
                        }
                    }

                    if (img != null) fullImages.add(img);
                }
            } catch (final JSONException e) {
                LogUtils.getInstance().e(MingPaoClient.class.getSimpleName(), e.getMessage(), e);
            }
        }

        return fullImages;
    }
}
