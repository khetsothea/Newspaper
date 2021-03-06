package com.github.ayltai.newspaper;

import javax.inject.Inject;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatDelegate;
import android.widget.TextView;

import com.google.firebase.remoteconfig.FirebaseRemoteConfig;

import com.github.ayltai.newspaper.client.ClientFactory;
import com.github.ayltai.newspaper.graphics.FaceDetectorFactory;
import com.github.ayltai.newspaper.graphics.FrescoImageLoader;
import com.github.ayltai.newspaper.net.ConnectivityChangeReceiver;
import com.github.ayltai.newspaper.setting.Settings;
import com.github.ayltai.newspaper.util.ContextUtils;
import com.github.ayltai.newspaper.util.LogUtils;
import com.github.ayltai.newspaper.util.TestUtils;
import com.github.piasy.biv.BigImageViewer;

import io.reactivex.Single;
import io.reactivex.schedulers.Schedulers;

public final class MainActivity extends BaseActivity {
    //region Variables

    @Inject
    FlowController controller;

    private FirebaseRemoteConfig       config;
    private ConnectivityChangeReceiver receiver;
    private Snackbar                   snackbar;

    private boolean rememberPosition;

    //endregion

    static {
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true);
    }

    @Inject
    public MainActivity() {
    }

    @Override
    protected void onCreate(@Nullable final Bundle savedInstanceState) {
        ContextUtils.setAppTheme(this);

        FrescoImageLoader.initialize(this.getCacheDir().getAbsolutePath());
        BigImageViewer.initialize(FrescoImageLoader.getInstance());

        super.onCreate(savedInstanceState);

        if (!TestUtils.isRunningInstrumentedTest()) this.setUpRemoteConfig();

        this.setUpConnectivityChangeReceiver();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (!this.rememberPosition) Settings.resetPosition();

        this.controller.onDestroy();
        this.controller = null;

        // Releases resources only if the app really quits
        // The app instances lifecycle may overlap when the app restarts
        if (this.isFinishing()) {
            RxBus.getInstance().unregisterAll();
            FrescoImageLoader.shutDown();
            FaceDetectorFactory.release();
            ClientFactory.getInstance(this).close();
        }
    }

    @Override
    protected void attachBaseContext(final Context newBase) {
        DaggerMainComponent.builder()
            .mainModule(new MainModule(this))
            .build()
            .inject(this);

        super.attachBaseContext(this.controller.attachNewBase(newBase));
    }

    @Override
    protected void onResume() {
        super.onResume();

        this.receiver.register();
    }

    @Override
    protected void onPause() {
        super.onPause();

        this.receiver.unregister();
    }

    @Override
    protected void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == Constants.REQUEST_SETTINGS && resultCode == Activity.RESULT_OK) {
            this.rememberPosition = true;

            this.finish();
            this.startActivity(this.getBaseContext().getPackageManager().getLaunchIntentForPackage(this.getBaseContext().getPackageName()).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
        }
    }

    @Override
    public void onBackPressed() {
        if (!this.controller.onBackPressed()) super.onBackPressed();
    }

    private void setUpConnectivityChangeReceiver() {
        this.snackbar = Snackbar.make(this.findViewById(android.R.id.content), R.string.error_no_connection, Snackbar.LENGTH_INDEFINITE);
        ((TextView)this.snackbar.getView().findViewById(android.support.design.R.id.snackbar_text)).setTextColor(ContextUtils.getColor(this, R.attr.textColorInverse));

        this.receiver = new ConnectivityChangeReceiver(this) {
            @Override
            protected void onConnectivityChange(final boolean isConnected) {
                if (isConnected) {
                    if (MainActivity.this.snackbar.isShown()) MainActivity.this.snackbar.dismiss();
                } else {
                    if (!MainActivity.this.snackbar.isShown()) MainActivity.this.snackbar.show();
                }
            }
        };
    }

    private void setUpRemoteConfig() {
        Single.<FirebaseRemoteConfig>create(emitter -> emitter.onSuccess(FirebaseRemoteConfig.getInstance()))
            .observeOn(Schedulers.io())
            .subscribeOn(Schedulers.io())
            .subscribe(config -> {
                this.config = config;
                this.config.setDefaults(R.xml.config);

                this.applyRemoteConfig();

                this.config.fetch(Constants.REMOTE_CONFIG_CACHE_EXPIRATION)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            this.config.activateFetched();
                        } else {
                            LogUtils.getInstance().w(this.getClass().getSimpleName(), "Failed to fetch remote config");
                        }
                    });
            });
    }

    private void applyRemoteConfig() {
        Configs.apply(this.config);
    }
}
