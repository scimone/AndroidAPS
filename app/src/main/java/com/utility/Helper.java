package com.utility;

import android.content.Context;
import android.content.res.Resources;
import android.util.TypedValue;

public class Helper {
    public static int getAttributeColor(
            Context context,
            int attributeId) {
        TypedValue typedValue = new TypedValue();
        context.getTheme().resolveAttribute(attributeId, typedValue, true);
        int colorRes = typedValue.resourceId;
        int color = -1;
        try {
            color = context.getResources().getColor(colorRes, null);
        } catch (Resources.NotFoundException e) {
            //do nothing
        }
        return color;
    }
}
