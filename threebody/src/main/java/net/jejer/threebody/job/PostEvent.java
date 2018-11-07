package net.jejer.threebody.job;

import net.jejer.threebody.bean.PostBean;

/**
 * Created by GreenSkinMonster on 2016-03-28.
 */
public class PostEvent extends BaseEvent {
    public PostBean mPostResult;
    public int mMode;
    public boolean fromQuickReply;
}
