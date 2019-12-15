package info.nightscout.androidaps.activities;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import info.nightscout.androidaps.logging.L;
import info.nightscout.androidaps.utils.SP;
import static info.nightscout.androidaps.plugins.general.themeselector.util.ThemeUtil.THEME_PINK;


public abstract class BaseActivity extends AppCompatActivity {

    private Logger log = LoggerFactory.getLogger(L.UI);
    int theme;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        theme = SP.getInt("theme", THEME_PINK);
        try {
            setTheme(theme);
        } catch (Exception e) {
            e.printStackTrace();
        }

        super.onCreate(savedInstanceState);
    }

    @Override
    protected void onResume() {
        super.onResume();
        int storedtheme = SP.getInt("theme", THEME_PINK);
        if (theme != storedtheme) {
            //this.recreate();
            log.debug("global theme: " + theme + " stored theme:"  + storedtheme);
        }
    }
}
