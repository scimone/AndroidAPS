package info.nightscout.androidaps.activities

import android.content.Context
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import info.nightscout.androidaps.utils.LocaleHelper

open class NoSplashAppCompatActivity : AppCompatActivity() {
    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    public override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.wrap(newBase))
    }
}
