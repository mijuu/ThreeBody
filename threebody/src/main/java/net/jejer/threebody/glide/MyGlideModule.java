package net.jejer.threebody.glide;

import android.content.Context;
import android.text.TextUtils;

import com.bumptech.glide.Glide;
import com.bumptech.glide.GlideBuilder;
import com.bumptech.glide.load.engine.cache.ExternalCacheDiskCacheFactory;
import com.bumptech.glide.load.model.GlideUrl;
import com.bumptech.glide.module.GlideModule;

import net.jejer.threebody.bean.HiSettingsHelper;
import net.jejer.threebody.cache.ImageInfo;
import net.jejer.threebody.okhttp.LoggingInterceptor;
import net.jejer.threebody.okhttp.OkHttpHelper;
import net.jejer.threebody.okhttp.ProgressListener;
import net.jejer.threebody.okhttp.ProgressResponseBody;
import net.jejer.threebody.utils.HiUtils;
import net.jejer.threebody.utils.Logger;

import org.greenrobot.eventbus.EventBus;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.TimeUnit;

import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Response;

/**
 * Created by GreenSkinMonster on 2016-11-03.
 */

public class MyGlideModule implements GlideModule {

    private final static int DEFAULT_CACHE_SIZE = 500;
    private final static int MIN_CACHE_SIZE = 300;


    @Override
    public void applyOptions(Context context, GlideBuilder gb) {
        String cacheSizeStr = HiSettingsHelper.getInstance().getStringValue(HiSettingsHelper.PERF_CACHE_SIZE_IN_MB, DEFAULT_CACHE_SIZE + "");
        int cacheSize = DEFAULT_CACHE_SIZE;
        if (TextUtils.isDigitsOnly(cacheSizeStr)) {
            cacheSize = Integer.parseInt(cacheSizeStr);
            if (cacheSize < MIN_CACHE_SIZE) {
                cacheSize = DEFAULT_CACHE_SIZE;
            }
        }
        gb.setDiskCache(new ExternalCacheDiskCacheFactory(context, cacheSize * 1024 * 1024));

        GlideHelper.initDefaultFiles();
    }

    @Override
    public void registerComponents(Context context, Glide glide) {
        OkHttpClient.Builder builder = new OkHttpClient.Builder();
        builder.connectTimeout(OkHttpHelper.NETWORK_TIMEOUT_SECS, TimeUnit.SECONDS)
                .readTimeout(OkHttpHelper.NETWORK_TIMEOUT_SECS, TimeUnit.SECONDS)
                .writeTimeout(OkHttpHelper.NETWORK_TIMEOUT_SECS, TimeUnit.SECONDS);

        if (HiSettingsHelper.getInstance().isTrustAllCerts()) {
            OkHttpHelper.setupTrustAllCerts(builder);
        }

        if (Logger.isDebug())
            builder.addInterceptor(new LoggingInterceptor());

        final ProgressListener progressListener = new ProgressListener() {
            @Override
            public void update(String url, long bytesRead, long contentLength, boolean done) {
                if (done) {
                    EventBus.getDefault().post(new GlideImageEvent(url, 100, ImageInfo.IN_PROGRESS));
                } else {
                    int progress = (int) Math.round((100.0 * bytesRead) / contentLength);
                    EventBus.getDefault().post(new GlideImageEvent(url, progress, ImageInfo.IN_PROGRESS));
                }
            }
        };

        builder.addInterceptor(new Interceptor() {
            @Override
            public Response intercept(Chain chain) throws IOException {
                Response originalResponse = chain.proceed(chain.request());
                String url = chain.request().url().toString();
                //avatar don't need a progress listener
                if (url.startsWith(HiUtils.AvatarBaseUrl)) {
                    return originalResponse;
                }
                return originalResponse.newBuilder()
                        .body(new ProgressResponseBody(url, originalResponse.body(), progressListener))
                        .build();
            }
        });

        OkHttpClient client = builder.build();

        glide.register(GlideUrl.class, InputStream.class, new OkHttpUrlLoader.Factory(client));
    }

}
