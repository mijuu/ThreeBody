package net.jejer.threebody.ui.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;

import com.bumptech.glide.Glide;
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView;

import net.jejer.threebody.R;
import net.jejer.threebody.glide.GlideImageView;

/**
 * Created by GreenSkinMonster on 2015-11-07.
 */
public class ImageViewerLayout extends RelativeLayout {

    private final SubsamplingScaleImageView scaleImageView;
    private final GlideImageView glideImageView;
    private final ProgressBar progressBar;

    private String url;

    public ImageViewerLayout(Context context) {
        this(context, null);
    }

    public ImageViewerLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ImageViewerLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        LayoutInflater.from(context).inflate(R.layout.layout_image_viewer, this, true);

        scaleImageView = (SubsamplingScaleImageView) findViewById(R.id.scale_image);
        glideImageView = (GlideImageView) findViewById(R.id.glide_image);
        progressBar = (ProgressBar) findViewById(R.id.progressbar);
    }

    public GlideImageView getGlideImageView() {
        return glideImageView;
    }

    public SubsamplingScaleImageView getScaleImageView() {
        return scaleImageView;
    }

    public ProgressBar getProgressBar() {
        return progressBar;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public void recycle() {
        scaleImageView.recycle();
        Glide.clear(glideImageView);
    }
}
