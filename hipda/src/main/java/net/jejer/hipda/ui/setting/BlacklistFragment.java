package net.jejer.hipda.ui.setting;

import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import com.mikepenz.google_material_typeface_library.GoogleMaterial;
import com.mikepenz.iconics.IconicsDrawable;

import net.jejer.hipda.R;
import net.jejer.hipda.async.BlacklistHelper;
import net.jejer.hipda.bean.HiSettingsHelper;
import net.jejer.hipda.okhttp.OkHttpHelper;
import net.jejer.hipda.ui.BaseFragment;
import net.jejer.hipda.ui.widget.ContentLoadingView;
import net.jejer.hipda.ui.widget.HiProgressDialog;
import net.jejer.hipda.ui.widget.OnSingleClickListener;
import net.jejer.hipda.ui.widget.SimpleDivider;
import net.jejer.hipda.utils.ColorHelper;
import net.jejer.hipda.utils.HiParser;
import net.jejer.hipda.utils.UIUtils;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import okhttp3.Request;

/**
 * Created by GreenSkinMonster on 2017-07-15.
 */

public class BlacklistFragment extends BaseFragment implements SwipeRefreshLayout.OnRefreshListener {

    public static final String TAG_KEY = "BLACKLIST_KEY";

    private List<String> mBlacklists = new ArrayList<>();

    private String mFormhash;
    private LayoutInflater mInflater;
    private Drawable mDrawable;
    private boolean mDialogShown;

    private View.OnClickListener mOnClickListener;
    private SwipeRefreshLayout mSwipeLayout;
    private ContentLoadingView mLoadingView;
    private HiProgressDialog mProgressDialog;

    private RvAdapter mAdapter = new RvAdapter();
    private List<String> mRemoving = new ArrayList<>();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_blacklist, container, false);
        mInflater = inflater;

        mDrawable = new IconicsDrawable(getActivity(), GoogleMaterial.Icon.gmd_close)
                .color(Color.GRAY)
                .sizeDp(12);

        mOnClickListener = new OnSingleClickListener() {
            @Override
            public void onSingleClick(View v) {
                removeFromBlacklist((String) v.getTag());
                v.setVisibility(View.INVISIBLE);
            }
        };

        mSwipeLayout = (SwipeRefreshLayout) view.findViewById(R.id.swipe_container);
        mSwipeLayout.setOnRefreshListener(this);
        mSwipeLayout.setColorSchemeColors(ColorHelper.getSwipeColor(getActivity()));
        mSwipeLayout.setProgressBackgroundColorSchemeColor(ColorHelper.getSwipeBackgroundColor(getActivity()));

        RecyclerView recyclerView = (RecyclerView) view.findViewById(R.id.recyclerview);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        recyclerView.addItemDecoration(new SimpleDivider(getActivity()));

        mLoadingView = (ContentLoadingView) view.findViewById(R.id.content_loading);

        recyclerView.setAdapter(mAdapter);

        refresh();

        setActionBarTitle("黑名单");
        return view;
    }

    protected void refresh() {
        if (!mSwipeLayout.isRefreshing())
            mSwipeLayout.setRefreshing(true);

        mRemoving.clear();
        BlacklistHelper.getBlacklists(new OkHttpHelper.ResultCallback() {
            @Override
            public void onError(Request request, Exception e) {
                UIUtils.toast("获取黑名单发生错误 : " + OkHttpHelper.getErrorMessage(e).getMessage());
                mSwipeLayout.setRefreshing(false);
                mLoadingView.setState(mBlacklists.size() > 0 ? ContentLoadingView.CONTENT : ContentLoadingView.NO_DATA);
            }

            @Override
            public void onResponse(String response) {
                mSwipeLayout.setRefreshing(false);
                try {
                    Document doc = Jsoup.parse(response);
                    mFormhash = HiParser.parseFormhash(doc);
                    String errorMsg = HiParser.parseErrorMessage(doc);
                    if (TextUtils.isEmpty(errorMsg)) {
                        List<String> list = HiParser.parseBlacklist(doc);
                        for (String username : list) {
                            if (!mBlacklists.contains(username))
                                mBlacklists.add(username);
                        }
                        Collections.sort(mBlacklists);
                        mAdapter.notifyDataSetChanged();
                        HiSettingsHelper.getInstance().setBlacklists(mBlacklists);
                        HiSettingsHelper.getInstance().setBlacklistSyncTime();

                        if (!mDialogShown && HiSettingsHelper.getInstance().getOldBlacklists().size() > 0) {
                            mDialogShown = true;
                            showUploadBlacklistDialog(getActivity());
                        }

                        new Handler().postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                UIUtils.toast("黑名单数据已同步");
                            }
                        }, 250);
                    } else {
                        UIUtils.toast(errorMsg);
                    }
                } catch (Exception e) {
                    UIUtils.toast(OkHttpHelper.getErrorMessage(e).getMessage());
                }
                mLoadingView.setState(mBlacklists.size() > 0 ? ContentLoadingView.CONTENT : ContentLoadingView.NO_DATA);
            }
        });
    }

    private void removeFromBlacklist(final String username) {
        mRemoving.add(username);
        BlacklistHelper.delBlacklist(mFormhash, username, new OkHttpHelper.ResultCallback() {
            @Override
            public void onError(Request request, Exception e) {
                UIUtils.toast(OkHttpHelper.getErrorMessage(e).getMessage());
            }

            @Override
            public void onResponse(String response) {
                try {
                    Document doc = Jsoup.parse(response);
                    String errorMsg = HiParser.parseErrorMessage(doc);
                    if (!TextUtils.isEmpty(errorMsg)) {
                        UIUtils.toast(errorMsg);
                    } else {
                        int pos = -1;
                        for (int i = 0; i < mBlacklists.size(); i++) {
                            String u = mBlacklists.get(i);
                            if (username.equals(u)) {
                                pos = i;
                                break;
                            }
                        }
                        if (pos != -1) {
                            mBlacklists.remove(pos);
                            mAdapter.notifyItemRemoved(pos);
                            if (mAdapter.getItemCount() - pos - 1 > 0)
                                mAdapter.notifyItemRangeChanged(pos, mAdapter.getItemCount() - pos - 1);

                            mLoadingView.setState(mBlacklists.size() > 0 ? ContentLoadingView.CONTENT : ContentLoadingView.NO_DATA);
                        } else {
                            refresh();
                        }
                        HiSettingsHelper.getInstance().removeFromBlacklist(username);
                    }
                } catch (Exception e) {
                    UIUtils.toast(OkHttpHelper.getErrorMessage(e).getMessage());
                }

            }
        });
    }

    @Override
    public void onRefresh() {
        refresh();
    }

    public void showUploadBlacklistDialog(final Context context) {
        StringBuilder sb = new StringBuilder();
        sb.append("黑名单将与论坛 短消息>消息设置 中的黑名单同步，详情请看客户端帖子1楼说明")
                .append("\n\n");
        for (String u : HiSettingsHelper.getInstance().getOldBlacklists()) {
            sb.append(u).append("\n");
        }
        final String detail = sb.toString();
        AlertDialog.Builder builder = UIUtils.getMessageDialogBuilder(context, "上传老版本黑名单数据", detail);
        builder.setPositiveButton("上传", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                uploadOldBlacklists();
            }
        });
        builder.setNeutralButton(context.getResources().getString(R.string.action_copy),
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        UIUtils.copyToClipboard(detail);
                    }
                });
        builder.setNegativeButton("清空旧数据", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                HiSettingsHelper.getInstance().setOldBlacklists(new ArrayList<String>());
            }
        });
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void uploadOldBlacklists() {
        new AsyncTask<Void, Void, Boolean>() {

            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                mProgressDialog = HiProgressDialog.show(getActivity(), "正在处理...");
            }

            @Override
            protected Boolean doInBackground(Void... params) {
                return BlacklistHelper.uploadOldBlackLists(mFormhash);
            }

            @Override
            protected void onPostExecute(Boolean success) {
                super.onPostExecute(success);
                mProgressDialog.dismiss();
                refresh();
                if (!success) {
                    UIUtils.copyToClipboard(HiSettingsHelper.getInstance().getStringValue(HiSettingsHelper.PERF_OLD_BLACKLIST, ""));
                    UIUtils.toast("上传可能发生错误，老版本黑名单已经复制到粘贴板");
                }
                HiSettingsHelper.getInstance().setOldBlacklists(new ArrayList<String>());
            }
        }.execute();
    }

    private class RvAdapter extends RecyclerView.Adapter {

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            return new ViewHolder(mInflater.inflate(R.layout.item_blacklist, parent, false));
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
            ViewHolder viewHolder = (ViewHolder) holder;
            String username = mBlacklists.get(position);

            viewHolder.tv_username.setText(username);
            viewHolder.ib_remove.setImageDrawable(mDrawable);
            viewHolder.ib_remove.setTag(username);
            viewHolder.ib_remove.setOnClickListener(mOnClickListener);
            viewHolder.ib_remove.setVisibility(mRemoving.contains(username) ? View.INVISIBLE : View.VISIBLE);
        }

        @Override
        public int getItemCount() {
            return mBlacklists != null ? mBlacklists.size() : 0;
        }
    }

    private static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tv_username;
        ImageButton ib_remove;

        ViewHolder(View itemView) {
            super(itemView);
            tv_username = (TextView) itemView.findViewById(R.id.tv_username);
            ib_remove = (ImageButton) itemView.findViewById(R.id.ib_remove);
        }
    }

}
