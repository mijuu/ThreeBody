package net.jejer.threebody.ui;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import net.jejer.threebody.ui.widget.swipeback.SwipeBackActivityBase;
import net.jejer.threebody.ui.widget.swipeback.SwipeBackActivityHelper;
import net.jejer.threebody.ui.widget.swipeback.SwipeBackLayout;
import net.jejer.threebody.ui.widget.swipeback.SwipeUtils;


/**
 * Created by GreenSkinMonster on 2017-06-15.
 */

public class SwipeBackActivity extends AppCompatActivity implements SwipeBackActivityBase {
    private SwipeBackActivityHelper mHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mHelper = new SwipeBackActivityHelper(this);
        mHelper.onActivityCreate();
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
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
        return mHelper.getSwipeBackLayout();
    }

    @Override
    public void setSwipeBackEnable(boolean enable) {
        getSwipeBackLayout().setEnableGesture(enable);
    }

    @Override
    public void scrollToFinishActivity() {
        SwipeUtils.convertActivityToTranslucent(this);
        getSwipeBackLayout().scrollToFinishActivity();
    }

}
