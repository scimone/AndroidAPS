package info.nightscout.androidaps.activities

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import info.nightscout.androidaps.MainApp
import info.nightscout.androidaps.R
import info.nightscout.androidaps.interfaces.PluginBase
import info.nightscout.androidaps.plugins.general.themeselector.util.ThemeUtil
import info.nightscout.androidaps.plugins.general.themeselector.util.ThemeUtil.THEME_PINK
import info.nightscout.androidaps.utils.LocaleHelper
import info.nightscout.androidaps.utils.PasswordProtection
import info.nightscout.androidaps.utils.SP

class SingleFragmentActivity : AppCompatActivity() {
    private var plugin: PluginBase? = null
    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_single_fragment)
        plugin = MainApp.getPluginsList()[intent.getIntExtra("plugin", -1)]
        title = plugin?.name
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)

        val newtheme = SP.getInt("theme", THEME_PINK)
        val mIsNightMode = SP.getBoolean("daynight", true)

        if (mIsNightMode) {
            delegate.localNightMode = AppCompatDelegate.MODE_NIGHT_YES
        } else {
            delegate.localNightMode = AppCompatDelegate.MODE_NIGHT_NO
        }
        setTheme(ThemeUtil.getThemeId(newtheme))
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction().replace(R.id.frame_layout,
                supportFragmentManager.fragmentFactory.instantiate(ClassLoader.getSystemClassLoader(), plugin?.pluginDescription?.fragmentClass!!)).commit()
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        } else if (item.itemId == R.id.nav_plugin_preferences) {
            PasswordProtection.QueryPassword(this, R.string.settings_password, "settings_password", Runnable {
                val i = Intent(this, PreferencesActivity::class.java)
                i.putExtra("id", plugin?.preferencesId)
                startActivity(i)
            }, null)
            return true
        }
        return false
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        if (plugin?.preferencesId != -1) menuInflater.inflate(R.menu.menu_single_fragment, menu)
        return super.onCreateOptionsMenu(menu)
    }

    public override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.wrap(newBase))
    }
}