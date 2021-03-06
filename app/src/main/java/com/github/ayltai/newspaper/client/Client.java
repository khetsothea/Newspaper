package com.github.ayltai.newspaper.client;

import java.io.Closeable;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.xmlpull.v1.XmlPullParserException;

import com.github.ayltai.newspaper.BuildConfig;
import com.github.ayltai.newspaper.Constants;
import com.github.ayltai.newspaper.model.Category;
import com.github.ayltai.newspaper.model.Item;
import com.github.ayltai.newspaper.model.Source;
import com.github.ayltai.newspaper.net.HttpClient;
import com.github.ayltai.newspaper.util.LogUtils;

import io.reactivex.Maybe;
import io.reactivex.MaybeEmitter;
import io.reactivex.Single;
import io.reactivex.SingleEmitter;
import io.reactivex.exceptions.UndeliverableException;

public abstract class Client implements Closeable {
    protected static final String ENCODING = "UTF-8";

    //region Variables

    protected final HttpClient client;
    protected final Source     source;

    //endregion

    //@Inject
    protected Client(@NonNull final HttpClient client, @Nullable final Source source) {
        this.client = client;
        this.source = source;
    }

    @NonNull
    public abstract Single<List<Item>> getItems(@NonNull String url);

    @NonNull
    public abstract Maybe<Item> updateItem(@NonNull Item item);

    @Override
    public void close() {
        this.client.close();
    }

    @Nullable
    protected final String getCategoryName(@NonNull final String url) {
        for (final Category category : this.source.getCategories()) {
            if (category.getUrl().equals(url)) return category.getName();
        }

        return null;
    }

    @NonNull
    protected List<Item> filters(@NonNull final String url, @NonNull final List<Item> items) {
        final List<Item> filteredItems = new ArrayList<>();

        for (final Item item : items) {
            if (item.getPublishDate() != null && item.getPublishDate().getTime() > System.currentTimeMillis() - Constants.HOUSEKEEP_TIME) filteredItems.add(item);
        }

        return filteredItems;
    }

    protected final <T> void handleError(@NonNull final SingleEmitter<T> emitter, @NonNull final Exception e) {
        if (e instanceof InterruptedIOException || e instanceof UndeliverableException || e instanceof IOException || e instanceof XmlPullParserException) {
            if (BuildConfig.DEBUG) LogUtils.getInstance().w(this.getClass().getSimpleName(), e.getMessage(), e);

            emitter.onSuccess((T)Collections.emptyList());
        } else {
            emitter.onError(e);
        }
    }

    protected final <T> void handleError(@NonNull final MaybeEmitter<T> emitter, @NonNull final Exception e) {
        if (e instanceof InterruptedIOException || e instanceof UndeliverableException || e instanceof IOException) {
            if (BuildConfig.DEBUG) LogUtils.getInstance().w(this.getClass().getSimpleName(), e.getMessage(), e);

            emitter.onComplete();
        } else {
            emitter.onError(e);
        }
    }
}
