package com.github.ayltai.newspaper.client;

import java.io.IOException;
import java.io.InputStream;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import javax.inject.Inject;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.apache.commons.io.IOUtils;

import com.github.ayltai.newspaper.BuildConfig;
import com.github.ayltai.newspaper.model.Image;
import com.github.ayltai.newspaper.model.Item;
import com.github.ayltai.newspaper.model.Source;
import com.github.ayltai.newspaper.net.HttpClient;
import com.github.ayltai.newspaper.util.LogUtils;
import com.github.ayltai.newspaper.util.StringUtils;

import io.reactivex.Maybe;
import io.reactivex.Single;

final class SingPaoClient extends Client {
    //region Constants

    private static final String BASE_URI = "https://www.singpao.com.hk/";
    private static final String TAG      = "'";
    private static final String FONT     = "</font>";

    //endregion

    private static final ThreadLocal<DateFormat> DATE_FORMAT = new ThreadLocal<DateFormat>() {
        @Override
        protected DateFormat initialValue() {
            return new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH);
        }
    };

    @Inject
    SingPaoClient(@NonNull final HttpClient client, @Nullable final Source source) {
        super(client, source);
    }

    @NonNull
    @Override
    public Single<List<Item>> getItems(@NonNull final String url) {
        return Single.create(emitter -> {
            try {
                final InputStream inputStream = this.client.download(url);

                if (inputStream == null) {
                    emitter.onSuccess(Collections.emptyList());
                } else {
                    final String     html         = IOUtils.toString(inputStream, Client.ENCODING);
                    final String[]   sections     = StringUtils.substringsBetween(html, "<tr valign='top'><td width='220'>", "</td></tr>");
                    final List<Item> items        = new ArrayList<>(sections.length);
                    final String     categoryName = this.getCategoryName(url);

                    for (final String section : sections) {
                        final Item item = new Item();

                        item.setTitle(StringUtils.substringBetween(section, "class='list_title'>", "</a>"));
                        item.setLink(SingPaoClient.BASE_URI + StringUtils.substringBetween(section, "<td><a href='", SingPaoClient.TAG));
                        item.setDescription(StringUtils.substringBetween(section, "<br><br>\n", SingPaoClient.FONT));
                        item.setSource(this.source.getName());
                        item.setCategory(categoryName);
                        item.getImages().add(new Image(SingPaoClient.BASE_URI + StringUtils.substringBetween(section, "<img src='", SingPaoClient.TAG)));

                        try {
                            item.setPublishDate(SingPaoClient.DATE_FORMAT.get().parse(StringUtils.substringBetween(section, "<font class='list_date'>", "<br>")));

                            items.add(item);
                        } catch (final ParseException e) {
                            LogUtils.getInstance().w(this.getClass().getSimpleName(), e.getMessage(), e);
                        }
                    }

                    emitter.onSuccess(this.filters(url, items));
                }
            } catch (final IOException e) {
                this.handleError(emitter, e);
            }
        });
    }

    @NonNull
    @Override
    public Maybe<Item> updateItem(@NonNull final Item item) {
        return Maybe.create(emitter -> {
            if (BuildConfig.DEBUG) LogUtils.getInstance().d(this.getClass().getSimpleName(), item.getLink());

            try {
                final String      html              = StringUtils.substringBetween(IOUtils.toString(this.client.download(item.getLink()), Client.ENCODING), "<td class='news_title'>", "您可能有興趣:");
                final List<Image> images            = new ArrayList<>();
                final String[]    imageUrls         = StringUtils.substringsBetween(html, "target='_blank'><img src='", SingPaoClient.TAG);
                final String[]    imageDescriptions = StringUtils.substringsBetween(html, "<font size='4'>", SingPaoClient.FONT);

                for (int i = 0; i < imageUrls.length; i++) {
                    final Image image = new Image(SingPaoClient.BASE_URI + imageUrls[0], imageDescriptions[0]);

                    if (!images.contains(image)) images.add(image);
                }

                if (!images.isEmpty()) {
                    item.getImages().clear();
                    item.getImages().addAll(images);
                }

                final String[]      contents = StringUtils.substringsBetween(html, "<p>", "</p>");
                final StringBuilder builder  = new StringBuilder();

                for (final String content : contents) builder.append(content).append("<br><br>");

                item.setDescription(builder.toString());
                item.setIsFullDescription(true);

                emitter.onSuccess(item);
            } catch (final IOException e) {
                this.handleError(emitter, e);
            }
        });
    }
}
