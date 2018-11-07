package net.jejer.threebody.async;

import android.content.Context;
import android.text.TextUtils;

import com.vdurmont.emoji.EmojiParser;

import net.jejer.threebody.bean.DetailListBean;
import net.jejer.threebody.bean.HiSettingsHelper;
import net.jejer.threebody.bean.PostBean;
import net.jejer.threebody.bean.PrePostInfoBean;
import net.jejer.threebody.okhttp.OkHttpHelper;
import net.jejer.threebody.okhttp.ParamsMap;
import net.jejer.threebody.utils.Constants;
import net.jejer.threebody.utils.HiParserThreadDetail;
import net.jejer.threebody.utils.HiUtils;
import net.jejer.threebody.utils.Logger;
import net.jejer.threebody.utils.Utils;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import okhttp3.Response;

public class PostHelper {

    public static final int MODE_REPLY_THREAD = 0;
    public static final int MODE_REPLY_POST = 1;
    public static final int MODE_QUOTE_POST = 2;
    public static final int MODE_NEW_THREAD = 3;
    public static final int MODE_QUICK_REPLY = 4;
    public static final int MODE_EDIT_POST = 5;
    public static final int MODE_QUICK_DELETE = 6;

    private static long LAST_POST_TIME = 0;
    private static final long POST_DELAY_IN_SECS = 30;

    private int mMode;
    private String mResult;
    private int mStatus = Constants.STATUS_FAIL;
    private DetailListBean mDetailListBean;
    private Context mCtx;
    private PrePostInfoBean mInfo;
    private PostBean mPostArg;

    private String mTid;
    private String mTitle;
    private int mFloor;

    public PostHelper(Context ctx, int mode, PrePostInfoBean info, PostBean postArg) {
        mCtx = ctx;
        mMode = mode;
        mInfo = info;
        mPostArg = postArg;
    }

    public PostBean post() {
        PostBean postBean = mPostArg;
        String replyText = postBean.getContent();
        String tid = postBean.getTid();
        String pid = postBean.getPid();
        int fid = postBean.getFid();
        int floor = postBean.getFloor();
        String subject = postBean.getSubject();
        String typeid = postBean.getTypeid();

        int count = 0;
        while (mInfo == null && count < 3) {
            count++;
            mInfo = new PrePostAsyncTask(mCtx, null, mMode).doInBackground(postBean);
        }

        mFloor = floor;

        replyText = replaceToTags(replyText);
        replyText = EmojiParser.parseToHtmlDecimal(replyText);
        if (!TextUtils.isEmpty(subject))
            subject = EmojiParser.parseToHtmlDecimal(subject);

        if (mMode != MODE_EDIT_POST) {
            String tailStr = HiSettingsHelper.getInstance().getTailStr();
            if (!TextUtils.isEmpty(tailStr) && HiSettingsHelper.getInstance().isAddTail()) {
                if (!replyText.trim().endsWith(tailStr))
                    replyText += "  " + tailStr;
            }
        }

        String url = HiUtils.ReplyUrl + tid + "&replysubmit=yes";
        // do send
        switch (mMode) {
            case MODE_REPLY_THREAD:
            case MODE_QUICK_REPLY:
                doPost(url, replyText, null, null, false);
                break;
            case MODE_REPLY_POST:
            case MODE_QUOTE_POST:
                doPost(url, EmojiParser.parseToHtmlDecimal(mInfo.getQuoteText()) + "\n\n    " + replyText, null, null, false);
                break;
            case MODE_NEW_THREAD:
                url = HiUtils.NewThreadUrl + fid + "&typeid=" + typeid + "&topicsubmit=yes";
                doPost(url, replyText, subject, null, false);
                break;
            case MODE_EDIT_POST:
                url = HiUtils.EditUrl + "&extra=&editsubmit=yes&mod=&editsubmit=yes" + "&fid=" + fid + "&tid=" + tid + "&pid=" + pid + "&page=1";
                doPost(url, replyText, subject, typeid, postBean.isDelete());
                break;
        }

        postBean.setSubject(mTitle);
        postBean.setFloor(mFloor);
        postBean.setTid(mTid);

        postBean.setMessage(mResult);
        postBean.setStatus(mStatus);
        postBean.setDetailListBean(mDetailListBean);

        return postBean;
    }

    private void doPost(String url, String replyText, String subject, String typeid, boolean delete) {
        String formhash = mInfo != null ? mInfo.getFormhash() : null;

        if (TextUtils.isEmpty(formhash)) {
            mResult = "发表失败，无法获取必要信息 ！";
            mStatus = Constants.STATUS_FAIL;
            return;
        }

        ParamsMap params = new ParamsMap();
        params.put("formhash", formhash);
        params.put("posttime", String.valueOf(System.currentTimeMillis()));
        params.put("wysiwyg", "0");
        //params.put("usesig", "1");
        params.put("message", replyText);
        if (mMode == MODE_EDIT_POST && delete)
            params.put("delete", "1");
        for (String attach : mInfo.getNewAttaches()) {
            params.put("attachnew[][description]", attach);
        }
        for (String attach : mInfo.getDeleteAttaches()) {
            params.put("attachdel[]", attach);
        }
        if (mMode == MODE_NEW_THREAD) {
            params.put("subject", subject);
            params.put("attention_add", "1");
            mTitle = subject;
        } else if (mMode == MODE_EDIT_POST) {
            if (!TextUtils.isEmpty(subject)) {
                params.put("subject", subject);
                mTitle = subject;
                if (!TextUtils.isEmpty(typeid)) {
                    params.put("typeid", typeid);
                }
            }
        }

        if (mMode == MODE_QUOTE_POST
                || mMode == MODE_REPLY_POST) {
            String noticeauthor = mInfo.getNoticeAuthor();
            String noticeauthormsg = mInfo.getNoticeAuthorMsg();
            String noticetrimstr = mInfo.getNoticeTrimStr();
            if (!TextUtils.isEmpty(noticeauthor)) {
                params.put("noticeauthor", noticeauthor);
                params.put("noticeauthormsg", Utils.nullToText(noticeauthormsg));
                params.put("noticetrimstr", Utils.nullToText(noticetrimstr));
            }
        }

        try {
            Response response = OkHttpHelper.getInstance().postAsResponse(url, params);
            String resp = OkHttpHelper.getResponseBody(response);
            String requestUrl = response.request().url().toString();

            if (delete && requestUrl.contains("forumdisplay.php")) {
                //delete first post == whole tread, forward to forum url
                mResult = "发表成功!";
                mStatus = Constants.STATUS_SUCCESS;
            } else {
                //when success, okhttp will follow 302 redirect get the page content
                String tid = Utils.getMiddleString(requestUrl, "tid=", "&");
                if (requestUrl.contains("viewthread.php") && HiUtils.isValidId(tid)) {
                    mTid = tid;
                    mResult = "发表成功!";
                    mStatus = Constants.STATUS_SUCCESS;
                    try {
                        //parse resp to get redirected page content
                        Document doc = Jsoup.parse(resp);
                        DetailListBean data = HiParserThreadDetail.parse(mCtx, doc, tid);
                        if (data != null && data.getCount() > 0) {
                            mDetailListBean = data;
                        }
                    } catch (Exception e) {
                        Logger.e(e);
                    }
                } else {
                    Logger.e(resp);
                    mResult = "发表失败! ";
                    mStatus = Constants.STATUS_FAIL;

                    Document doc = Jsoup.parse(resp);
                    Elements error = doc.select("div.alert_info");
                    if (error != null && error.size() > 0) {
                        mResult += "\n" + error.text();
                    }
                }
            }
        } catch (Exception e) {
            Logger.e(e);
            mResult = "发表失败 : " + OkHttpHelper.getErrorMessage(e);
            mStatus = Constants.STATUS_FAIL;
        }

        if (delete) {
            mResult = mResult.replace("发表", "删除");
        }

        if (mStatus == Constants.STATUS_SUCCESS && (mMode != MODE_EDIT_POST || delete))
            LAST_POST_TIME = System.currentTimeMillis();
    }

    public static int getWaitTimeToPost() {
        long delta = (System.currentTimeMillis() - LAST_POST_TIME) / 1000;
        if (POST_DELAY_IN_SECS > delta) {
            return (int) (POST_DELAY_IN_SECS - delta);
        }
        return 0;
    }

    private String replaceToTags(final String replyText) {
        String text = replyText;
        StringBuilder sb = new StringBuilder();
        try {
            while (!TextUtils.isEmpty(text)) {
                int tagStart = text.indexOf("[");
                if (tagStart == -1) {
                    sb.append(Utils.replaceUrlWithTag(text));
                    break;
                }
                int tagEnd = text.indexOf("]", tagStart);
                if (tagEnd == -1) {
                    sb.append(Utils.replaceUrlWithTag(text));
                    break;
                }
                String tag = text.substring(tagStart + 1, tagEnd);
                if (tag.contains("=")) {
                    tag = tag.substring(0, tag.indexOf("="));
                }
                String tagE = "[/" + tag + "]";
                int tagEIndex = text.indexOf(tagE);
                if (tagEIndex != -1) {
                    tagEIndex = tagEIndex + tagE.length();
                } else {
                    sb.append(Utils.replaceUrlWithTag(text));
                    break;
                }
                sb.append(Utils.replaceUrlWithTag(text.substring(0, tagStart)));
                sb.append(text.substring(tagStart, tagEIndex));
                text = text.substring(tagEIndex);
            }
        } catch (Exception e) {
            Logger.e(e);
            return replyText;
        }
        return sb.toString();
    }

}
