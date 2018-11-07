package net.jejer.threebody.ui;

import android.os.Bundle;
import android.view.View;

import net.jejer.threebody.bean.HiSettingsHelper;
import net.jejer.threebody.ui.widget.swipeback.SwipeBackActivityBase;
import net.jejer.threebody.ui.widget.swipeback.SwipeBackActivityHelper;
import net.jejer.threebody.ui.widget.swipeback.SwipeBackLayout;
import net.jejer.threebody.ui.widget.swipeback.SwipeUtils;
import net.jejer.threebody.utils.UIUtils;


/**
 * Created by GreenSkinMonster on 2017-06-15.
 */

public class SwipeBaseActivity extends BaseActivity implements SwipeBackActivityBase {
    private SwipeBackActivityHelper mHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (HiSettingsHelper.getInstance().isGestureBack()) {
            mHelper = new SwipeBackActivityHelper(this);
            mHelper.onActivityCreate();

            getSwipeBackLayout().setEdgeSize((UIUtils.getWindowWidth(getWindow())));
            setSwipeBackEnable(HiSettingsHelper.getInstance().isGestureBack());
        }
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        if (mHelper != null)
            mHelper.onPostCreate();
    }

    @Override
    public View findViewById(int id) {
        View v = super.findViewById(id);
        if (v == null && mHelper != null)
            return mHelper.findViewById(id);
        return v;
    }

    @Override
    public SwipeBackLayout getSwipeBackLayout() {
        return mHelper != null ? mHelper.getSwipeBackLayout() : null;
    }

    @Override
    public void setSwipeBackEnable(boolean enable) {
        if (mHelper != null) {
            getSwipeBackLayout().setEnableGesture(enable);
        }
    }

    @Override
    public void scrollToFinishActivity() {
        if (mHelper != null) {
            SwipeUtils.convertActivityToTranslucent(this);
            getSwipeBackLayout().scrollToFinishActivity();
        }
    }

}
