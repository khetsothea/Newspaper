package com.github.ayltai.newspaper.net;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.github.ayltai.newspaper.BuildConfig;
import com.github.ayltai.newspaper.util.TestUtils;

import okhttp3.Call;
import okhttp3.Request;
import okhttp3.Response;

public final class HttpClient extends BaseHttpClient implements Closeable {
    private final List<Call> calls = Collections.synchronizedList(new ArrayList<>());

    private Context context;

    @Inject
    public HttpClient(@Nullable final Context context) {
        super();

        this.context = context;
    }

    @Override
    public void close() {
        synchronized (this.calls) {
            for (final Call call : this.calls) {
                if (call != null && !call.isCanceled()) call.cancel();
            }

            this.calls.clear();
        }

        this.context = null;
    }

    @Nullable
    public InputStream download(@NonNull final String url) throws IOException {
        if (TestUtils.isRunningInstrumentedTest()) return this.mockDownload(url);

        if (BuildConfig.DEBUG) Log.d(this.getClass().getSimpleName(), "Downloading from " + url);

        final Call call = this.client.newCall(new Request.Builder()
            .url(url)
            .header("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10.12; rv:53.0) Gecko/20100101 Firefox/53.0")
            .build());

        this.calls.add(call);

        final Response response = call.execute();

        if (response.isSuccessful()) {
            return response.body().byteStream();
        } else {
            throw new IOException("Unexpected response " + response);
        }
    }

    @Nullable
    private InputStream mockDownload(@NonNull final String url) throws IOException {
        if (this.context == null) return null;

        final Integer asset = BaseHttpClient.ASSETS.get(url);

        if (asset == null) return null;

        return this.context.getResources().openRawResource(asset);
    }
}
