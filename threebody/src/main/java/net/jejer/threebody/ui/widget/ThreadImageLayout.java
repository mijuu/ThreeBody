package net.jejer.threebody.ui.widget;

import android.support.v4.content.ContextCompat;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.RequestManager;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.resource.file.FileToStreamDecoder;

import net.jejer.threebody.R;
import net.jejer.threebody.bean.ContentImg;
import net.jejer.threebody.bean.HiSettingsHelper;
import net.jejer.threebody.cache.ImageContainer;
import net.jejer.threebody.cache.ImageInfo;
import net.jejer.threebody.glide.GifTransformation;
import net.jejer.threebody.glide.GlideBitmapTarget;
import net.jejer.threebody.glide.GlideHelper;
import net.jejer.threebody.glide.GlideImageEvent;
import net.jejer.threebody.glide.GlideImageView;
import net.jejer.threebody.glide.ThreadImageDecoder;
import net.jejer.threebody.job.GlideImageJob;
import net.jejer.threebody.job.JobMgr;
import net.jejer.threebody.ui.ThreadDetailFragment;
import net.jejer.threebody.utils.UIUtils;
import net.jejer.threebody.utils.Utils;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

/**
 * Created by GreenSkinMonster on 2015-11-07.
 */
public class ThreadImageLayout extends RelativeLayout {

    private static final int MIN_WIDTH = 120;

    private GlideImageView mImageView;
    private ProgressBar mProgressBar;
    private TextView mTextView;
    private String mUrl;
    private long mParsedFileSize;
    private RequestManager mRequestManager;
    private String mParentSessionId;
    private int mImageIndex;
    private ThreadDetailFragment mFragment;
    private ContentImg mContentImg;

    public ThreadImageLayout(ThreadDetailFragment fragment, String url) {
        super(fragment.getActivity(), null);

        LayoutInflater.from(fragment.getActivity()).inflate(R.layout.layout_thread_image, this, true);

        mFragment = fragment;
        mImageView = (GlideImageView) findViewById(R.id.thread_image);
        mProgressBar = (ProgressBar) findViewById(R.id.thread_image_progress);
        mTextView = (TextView) findViewById(R.id.thread_image_info);
        mRequestManager = Glide.with(mFragment);
        mUrl = url;

        ImageInfo imageInfo = ImageContainer.getImageInfo(url);
        if (!imageInfo.isReady()) {
            mImageView.setImageDrawable(ContextCompat.getDrawable(mFragment.getActivity(), R.drawable.ic_action_image));
        }
        mImageView.setFragment(mFragment);
        mImageView.setVisibility(View.VISIBLE);
        mImageView.setImageIndex(mImageIndex);
        mImageView.setUrl(mUrl);
        mImageView.setSingleClickListener();
    }

    public void setParsedFileSize(long parsedFileSize) {
        mParsedFileSize = parsedFileSize;
    }

    public void setParentSessionId(String parentSessionId) {
        mParentSessionId = parentSessionId;
    }

    public void setImageIndex(int imageIndex) {
        mImageIndex = imageIndex;
        mImageView.setImageIndex(mImageIndex);
    }

    public void setContentImg(ContentImg contentImg) {
        mContentImg = contentImg;
    }

    private void loadImage() {
        ImageInfo imageInfo = ImageContainer.getImageInfo(mUrl);
        mProgressBar.setVisibility(View.GONE);
        if (imageInfo.getStatus() == ImageInfo.SUCCESS) {
            mTextView.setVisibility(GONE);
            if (getLayoutParams().height != imageInfo.getDisplayHeight()) {
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, imageInfo.getDisplayHeight());
                setLayoutParams(params);
            }
            if (imageInfo.getWidth() >= MIN_WIDTH || imageInfo.isGif()) {
                mImageView.setOnLongClickListener(new OnLongClickListener() {
                    @Override
                    public boolean onLongClick(View view) {
                        showImageActionDialog();
                        return true;
                    }
                });
            }

            if (imageInfo.isGif()) {
                mRequestManager
                        .load(mUrl)
                        .asBitmap()
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                        .transform(new GifTransformation(getContext()))
                        .into(new GlideBitmapTarget(mImageView, imageInfo.getDisplayWidth(), imageInfo.getDisplayHeight()));
            } else {
                mRequestManager
                        .load(mUrl)
                        .asBitmap()
                        .cacheDecoder(new FileToStreamDecoder<>(new ThreadImageDecoder(imageInfo)))
                        .imageDecoder(new ThreadImageDecoder(imageInfo))
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                        .into(new GlideBitmapTarget(mImageView, imageInfo.getDisplayWidth(), imageInfo.getDisplayHeight()));
            }
        } else {
            mImageView.setImageResource(R.drawable.image_broken);
        }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();

        EventBus.getDefault().register(this);

        boolean isThumb = !TextUtils.isEmpty(mContentImg.getThumbUrl())
                && !mContentImg.getThumbUrl().equals(mContentImg.getContent());

        ImageInfo imageInfo = ImageContainer.getImageInfo(mUrl);
        if (imageInfo.getStatus() == ImageInfo.SUCCESS) {
            LinearLayout.LayoutParams params
                    = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, imageInfo.getHeight());
            setLayoutParams(params);
        } else {
            LinearLayout.LayoutParams params
                    = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, Utils.dpToPx(getContext(), 150));
            setLayoutParams(params);
            if (mParsedFileSize > 0 && mTextView.getVisibility() != VISIBLE) {
                mTextView.setVisibility(View.VISIBLE);
                if (isThumb)
                    mTextView.setText(Utils.toSizeText(mParsedFileSize) + "↑");
                else
                    mTextView.setText(Utils.toSizeText(mParsedFileSize));
            }
        }
        if (imageInfo.getStatus() == ImageInfo.SUCCESS) {
            loadImage();
        } else if (imageInfo.getStatus() == ImageInfo.FAIL) {
            mImageView.setImageResource(R.drawable.image_broken);
            mProgressBar.setVisibility(View.GONE);
        } else if (imageInfo.getStatus() == ImageInfo.IN_PROGRESS) {
            mProgressBar.setVisibility(View.VISIBLE);
            mProgressBar.setProgress(imageInfo.getProgress());
        } else {
            boolean autoload = HiSettingsHelper.getInstance().isImageLoadable(mParsedFileSize, isThumb);
            JobMgr.addJob(new GlideImageJob(
                    mUrl,
                    JobMgr.PRIORITY_LOW,
                    mParentSessionId,
                    autoload));
        }
    }

    private void showImageActionDialog() {
        SimplePopupMenu popupMenu = new SimplePopupMenu(getContext());
        popupMenu.add("save", getResources().getString(R.string.action_save),
                new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                        UIUtils.saveImage(mFragment.getActivity(), UIUtils.getSnackView(mFragment.getActivity()), mContentImg.getContent());
                    }
                });
        popupMenu.add("share", getResources().getString(R.string.action_share),
                new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                        UIUtils.shareImage(mFragment.getActivity(), UIUtils.getSnackView(mFragment.getActivity()), mUrl);
                    }
                });
        popupMenu.add("gallery", getResources().getString(R.string.action_image_gallery),
                new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                        mFragment.startImageGallery(mImageIndex, mImageView);
                    }
                });
        popupMenu.show();
    }


    @Override
    protected void onDetachedFromWindow() {
        EventBus.getDefault().unregister(this);
        super.onDetachedFromWindow();
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(GlideImageEvent event) {
        if (!event.getImageUrl().equals(mUrl))
            return;
        final ImageInfo imageInfo = ImageContainer.getImageInfo(mUrl);
        imageInfo.setMessage(event.getMessage());

        if (event.getStatus() == ImageInfo.IN_PROGRESS
                && imageInfo.getStatus() != ImageInfo.SUCCESS) {
            if (mProgressBar.getVisibility() != View.VISIBLE)
                mProgressBar.setVisibility(View.VISIBLE);
            mProgressBar.setProgress(event.getProgress());

            imageInfo.setProgress(event.getProgress());
            imageInfo.setStatus(ImageInfo.IN_PROGRESS);
        } else if (event.getStatus() == ImageInfo.SUCCESS) {
            if (mProgressBar.getVisibility() == View.VISIBLE)
                mProgressBar.setVisibility(View.GONE);
            if (mTextView.getVisibility() == View.VISIBLE)
                mTextView.setVisibility(View.GONE);
            if (GlideHelper.isOkToLoad(mFragment))
                loadImage();
        } else {
            mProgressBar.setVisibility(GONE);
            mImageView.setImageResource(R.drawable.image_broken);
            if (!TextUtils.isEmpty(imageInfo.getMessage())) {
                mTextView.setText("加载错误");
                mTextView.setClickable(true);
                mTextView.setOnClickListener(new OnSingleClickListener() {
                    @Override
                    public void onSingleClick(View v) {
                        UIUtils.showMessageDialog(mFragment.getActivity(), "错误信息", imageInfo.getMessage(), true);
                    }
                });
            }
        }
    }

}
