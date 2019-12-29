package info.nightscout.androidaps.activities

import android.os.Bundle
import androidx.appcompat.app.AppCompatDelegate
import info.nightscout.androidaps.MainActivity
import info.nightscout.androidaps.MainApp
import info.nightscout.androidaps.R
import info.nightscout.androidaps.plugins.general.themeselector.util.ThemeUtil
import info.nightscout.androidaps.utils.ActivityMonitor
import info.nightscout.androidaps.utils.OKDialog
import info.nightscout.androidaps.utils.SP
import info.nightscout.androidaps.utils.TddCalculator
import info.nightscout.androidaps.utils.TirCalculator
import kotlinx.android.synthetic.main.stats_activity.*

class StatsActivity : NoSplashAppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        val newtheme = SP.getInt("theme", ThemeUtil.THEME_PINK)
        if (MainActivity.mIsNightMode) {
            delegate.localNightMode = AppCompatDelegate.MODE_NIGHT_YES
        } else {
            delegate.localNightMode = AppCompatDelegate.MODE_NIGHT_NO
        }
        setTheme(newtheme)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.stats_activity)

        stats_tdds.text = TddCalculator.stats()
        stats_tir.text = TirCalculator.stats()
        stats_activity.text = ActivityMonitor.stats()

        ok.setOnClickListener { finish() }
        stats_reset.setOnClickListener {
            OKDialog.showConfirmation(this, MainApp.gs(R.string.doyouwantresetstats), Runnable {
                ActivityMonitor.reset()
                recreate()
            })
        }
    }
}
