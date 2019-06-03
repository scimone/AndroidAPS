package info.nightscout.androidaps.utils;

import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.graphics.Color;
import android.widget.TextView;

/**
 * Created by mike on 08.07.2016.
 */
public class SetWarnColor {
    static final int warnColor = Color.YELLOW;
    static final int urgentColor = Color.RED;

    public static void setColor(TextView view, double value, double warnLevel, double urgentLevel) {
        ColorStateList colorStateList = view.getTextColors();
        final int normalColor = colorStateList.getDefaultColor();

        if (value >= urgentLevel) view.setTextColor(urgentColor);
        else if (value >= warnLevel) view.setTextColor(warnColor);
        else view.setTextColor(normalColor);
    }

    public static void setColorInverse(TextView view, double value, double warnLevel, double urgentLevel) {
        ColorStateList colorStateList = view.getTextColors();
        final int normalColor = colorStateList.getDefaultColor();

        if (value <= urgentLevel) view.setTextColor(urgentColor);
        else if (value <= warnLevel) view.setTextColor(warnColor);
        else view.setTextColor(normalColor);
    }
}
