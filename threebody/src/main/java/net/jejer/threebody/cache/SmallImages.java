package net.jejer.threebody.cache;

import net.jejer.threebody.R;
import net.jejer.threebody.utils.HiUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * cache small images
 * Created by GreenSkinMonster on 2015-11-10.
 */
public class SmallImages {

    private static Map<String, Integer> IMAGES = null;

    private static Map<String, Integer> getImages() {
        if (IMAGES == null) {
            synchronized (SmallImages.class) {
                if (IMAGES == null) {
                    IMAGES = new HashMap<>();
                    IMAGES.put(HiUtils.ImageBaseUrl + "attachments/day_140621/1406211752793e731a4fec8f7b.png", R.drawable.win);
                    IMAGES.put(HiUtils.BaseUrl + "attachments/day_140621/1406211752793e731a4fec8f7b.png", R.drawable.win);
                    IMAGES.put("http://www.hi-pda.com/forum/attachments/day_140621/1406211752793e731a4fec8f7b.png", R.drawable.win);
                }
            }
        }
        return IMAGES;
    }

    public static boolean contains(String url) {
        return getImages().containsKey(url);
    }

    public static int getDrawable(String url) {
        return getImages().get(url);
    }

    public static void clear() {
        IMAGES = null;
    }

}
