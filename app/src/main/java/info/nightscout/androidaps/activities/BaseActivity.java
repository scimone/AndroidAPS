package info.nightscout.androidaps.activities;

import android.content.res.Resources;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import info.nightscout.androidaps.logging.L;
import info.nightscout.androidaps.plugins.general.themeselector.util.ThemeUtil;
import info.nightscout.androidaps.utils.SP;

import static info.nightscout.androidaps.plugins.general.themeselector.util.ThemeUtil.THEME_PINK;


public abstract class BaseActivity extends AppCompatActivity {

    private Logger log = LoggerFactory.getLogger(L.UI);
    int themeToSet;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        themeToSet = SP.getInt("theme", THEME_PINK);
        try {
            setTheme(themeToSet);
            Resources.Theme theme = super.getTheme();
            // https://stackoverflow.com/questions/11562051/change-activitys-theme-programmatically
            theme.applyStyle(ThemeUtil.getThemeId(themeToSet), true);
        } catch (Exception e) {
            e.printStackTrace();
        }
        super.onCreate(savedInstanceState);
    }

}
