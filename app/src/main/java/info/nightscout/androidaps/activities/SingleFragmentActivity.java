package info.nightscout.androidaps.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Window;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.Fragment;

import info.nightscout.androidaps.MainApp;
import info.nightscout.androidaps.R;
import info.nightscout.androidaps.interfaces.PluginBase;
import info.nightscout.androidaps.plugins.general.themeselector.util.ThemeUtil;
import info.nightscout.androidaps.utils.PasswordProtection;
import info.nightscout.androidaps.utils.SP;

import static info.nightscout.androidaps.plugins.general.themeselector.util.ThemeUtil.THEME_PINK;

public class SingleFragmentActivity extends NoSplashAppCompatActivity {

    private PluginBase plugin;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {

        this.plugin = MainApp.getPluginsList().get(getIntent().getIntExtra("plugin", -1));
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setTitle(plugin.getName());
        getSupportActionBar().show();
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);


        int newtheme = SP.getInt("theme", THEME_PINK);
        boolean mIsNightMode = SP.getBoolean("daynight", true);

        if(mIsNightMode){
            getDelegate().setLocalNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        }else{
            getDelegate().setLocalNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }
        setTheme(ThemeUtil.getThemeId(newtheme));


        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_single_fragment);

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction().replace(R.id.frame_layout,
                    Fragment.instantiate(this, plugin.pluginDescription.getFragmentClass())).commit();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        else if (item.getItemId() == R.id.nav_plugin_preferences) {
            PasswordProtection.QueryPassword(this, R.string.settings_password, "settings_password", () -> {
                Intent i = new Intent(this, PreferencesActivity.class);
                i.putExtra("id", plugin.getPreferencesId());
                startActivity(i);
            }, null);
            return true;
        }
        return false;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (plugin.getPreferencesId() != -1)
            getMenuInflater().inflate(R.menu.menu_single_fragment, menu);
        return super.onCreateOptionsMenu(menu);
    }
}