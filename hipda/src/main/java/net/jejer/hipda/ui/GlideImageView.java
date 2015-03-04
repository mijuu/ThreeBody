package net.jejer.hipda.ui;

import android.app.DownloadManager;
import android.content.Context;
import android.graphics.Point;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.PopupWindow;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.FutureTarget;
import com.bumptech.glide.request.target.Target;
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView;

import net.jejer.hipda.R;

import java.io.File;

public class GlideImageView extends ImageView {

	private Context mCtx;
	private String mUrl;

	public GlideImageView(Context context) {
		super(context);
		mCtx = context;
	}

	public void setUrl(String url) {
		mUrl = url;
		setOnClickListener(new GlideImageViewClickHandler());
		setClickable(true);
	}

	private class GlideImageViewClickHandler implements OnClickListener {
		@Override
		public void onClick(View arg0) {

			LayoutInflater inflater = (LayoutInflater) mCtx.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			View layout = inflater.inflate(R.layout.popup_image, null);

			final SubsamplingScaleImageView wvImage = (SubsamplingScaleImageView) layout.findViewById(R.id.wv_image);
			wvImage.setBackgroundColor(mCtx.getResources().getColor(R.color.night_background));

			new AsyncTask<Void, Void, File>() {
				@Override
				protected File doInBackground(Void... voids) {
					try {
						FutureTarget<File> future =
								Glide.with(mCtx)
										.load(mUrl)
										.downloadOnly(Target.SIZE_ORIGINAL, Target.SIZE_ORIGINAL);
						File cacheFile = future.get();
						Glide.clear(future);
						return cacheFile;
					} catch (Exception e) {
					}
					return null;
				}

				@Override
				protected void onPostExecute(File cacheFile) {
					super.onPostExecute(cacheFile);
					Log.e("AA", cacheFile.getAbsolutePath());
					wvImage.setImageUri(Uri.fromFile(cacheFile));
				}
			}.execute();

			ImageButton btnDownload = (ImageButton) layout.findViewById(R.id.btn_download_image);
			btnDownload.setOnClickListener(new ImageButton.OnClickListener() {
				@Override
				public void onClick(View arg0) {
					DownloadManager dm = (DownloadManager) mCtx.getSystemService(Context.DOWNLOAD_SERVICE);
					DownloadManager.Request req = new DownloadManager.Request(Uri.parse(mUrl));
					req.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
					req.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, mUrl.substring(mUrl.lastIndexOf("/") + 1));
					dm.enqueue(req);
				}
			});

			PopupWindow popup = new PopupWindow(layout);
			popup.setFocusable(true);
			popup.setBackgroundDrawable(mCtx.getResources().getDrawable(R.drawable.ic_action_picture));
			popup.setOutsideTouchable(true);

			WindowManager wm = (WindowManager) mCtx.getSystemService(Context.WINDOW_SERVICE);
			Display display = wm.getDefaultDisplay();
			Point size = new Point();
			display.getSize(size);
			popup.setWidth(size.x - 50);
			popup.setHeight(size.y - 100);
			popup.showAtLocation(layout, Gravity.CENTER, 0, 25);
		}
	}
}
