package net.jejer.threebody.glide;

import android.content.Context;
import android.support.v7.widget.AppCompatImageView;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.Priority;
import com.bumptech.glide.load.engine.DiskCacheStrategy;

import net.jejer.threebody.R;
import net.jejer.threebody.cache.ImageContainer;
import net.jejer.threebody.cache.ImageInfo;
import net.jejer.threebody.job.GlideImageJob;
import net.jejer.threebody.job.JobMgr;
import net.jejer.threebody.ui.ThreadDetailFragment;
import net.jejer.threebody.ui.widget.OnSingleClickListener;

import java.lang.ref.WeakReference;

public class GlideImageView extends AppCompatImageView {

    private ThreadDetailFragment mFragment;
    private String mUrl;
    private int mImageIndex;

    private static WeakReference<ImageView> mCurrentViewHolder;
    private static String mCurrentUrl;

    public GlideImageView(Context context) {
        super(context);
    }

    public GlideImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void setUrl(String url) {
        mUrl = url;
    }

    public void setImageIndex(int index) {
        mImageIndex = index;
    }

    public void setFragment(ThreadDetailFragment fragment) {
        mFragment = fragment;
    }

    public void setSingleClickListener() {
        setOnClickListener(new GlideImageViewClickHandler());
    }

    private class GlideImageViewClickHandler extends OnSingleClickListener {
        @Override
        public void onSingleClick(View view) {
            ImageInfo imageInfo = ImageContainer.getImageInfo(mUrl);
            if (imageInfo.isReady()) {
                if (mUrl.equals(mCurrentUrl) && mCurrentViewHolder != null) {
                    boolean sameView = view.equals(mCurrentViewHolder.get());
                    stopCurrentGif();
                    if (!sameView)
                        loadGif();
                } else if (imageInfo.isGif()) {
                    stopCurrentGif();
                    loadGif();
                } else {
                    stopCurrentGif();
                    startImageGallery();
                }
            } else if (imageInfo.getStatus() == ImageInfo.FAIL || imageInfo.getStatus() == ImageInfo.IDLE) {
                if (mFragment != null)
                    JobMgr.addJob(new GlideImageJob(mUrl, JobMgr.PRIORITY_LOW, mFragment.mSessionId, true));
            }
        }
    }

    private void startImageGallery() {
        if (mFragment != null)
            mFragment.startImageGallery(mImageIndex, this);
    }

    private void loadGif() {
        mCurrentUrl = mUrl;
        mCurrentViewHolder = new WeakReference<ImageView>(this);
        Glide.clear(this);
        if (GlideHelper.isOkToLoad(getContext())) {
            ImageInfo imageInfo = ImageContainer.getImageInfo(mUrl);
            Glide.with(getContext())
                    .load(mUrl)
                    .priority(Priority.IMMEDIATE)
                    .diskCacheStrategy(DiskCacheStrategy.SOURCE)
                    .skipMemoryCache(true)
                    .error(R.drawable.image_broken)
                    .override(imageInfo.getDisplayWidth(), imageInfo.getDisplayHeight())
                    .into(this);
        }
    }

    public void stopCurrentGif() {
        try {
            if (mCurrentViewHolder != null && mCurrentViewHolder.get() != null) {
                ImageView lastView = mCurrentViewHolder.get();
                Glide.clear(mCurrentViewHolder.get());
                if (GlideHelper.isOkToLoad(getContext())) {
                    ImageInfo imageInfo = ImageContainer.getImageInfo(mUrl);
                    Glide.with(getContext())
                            .load(mCurrentUrl)
                            .asBitmap()
                            .diskCacheStrategy(DiskCacheStrategy.SOURCE)
                            .transform(new GifTransformation(getContext()))
                            .error(R.drawable.image_broken)
                            .override(imageInfo.getDisplayWidth(), imageInfo.getDisplayHeight())
                            .into(lastView);
                }
            }
        } catch (Exception ignored) {
        }
        mCurrentUrl = null;
        mCurrentViewHolder = null;
    }

}
