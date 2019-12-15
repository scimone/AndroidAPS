package info.nightscout.androidaps;

import android.app.Activity;
import android.app.TaskStackBuilder;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.text.SpannableString;
import android.text.method.LinkMovementMethod;
import android.text.util.Linkify;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.arch.core.util.Function;
import androidx.core.app.ActivityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.FragmentManager;
import androidx.viewpager.widget.ViewPager;

import com.google.android.material.navigation.NavigationView;
import com.joanzapata.iconify.Iconify;
import com.joanzapata.iconify.fonts.FontAwesomeModule;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import info.nightscout.androidaps.activities.HistoryBrowseActivity;
import info.nightscout.androidaps.activities.NoSplashAppCompatActivity;
import info.nightscout.androidaps.activities.PreferencesActivity;
import info.nightscout.androidaps.activities.SingleFragmentActivity;
import info.nightscout.androidaps.activities.StatsActivity;
import info.nightscout.androidaps.db.CareportalEvent;
import info.nightscout.androidaps.events.EventAppExit;
import info.nightscout.androidaps.events.EventCareportalEventChange;
import info.nightscout.androidaps.events.EventInitializationChanged;
import info.nightscout.androidaps.events.EventPreferenceChange;
import info.nightscout.androidaps.events.EventPumpStatusChanged;
import info.nightscout.androidaps.events.EventRebuildTabs;
import info.nightscout.androidaps.interfaces.PluginBase;
import info.nightscout.androidaps.interfaces.PluginType;
import info.nightscout.androidaps.interfaces.PumpInterface;
import info.nightscout.androidaps.logging.L;
import info.nightscout.androidaps.plugins.aps.loop.LoopPlugin;
import info.nightscout.androidaps.plugins.bus.RxBus;
import info.nightscout.androidaps.plugins.configBuilder.ConfigBuilderPlugin;
import info.nightscout.androidaps.plugins.constraints.versionChecker.VersionCheckerUtilsKt;
import info.nightscout.androidaps.plugins.general.careportal.CareportalFragment;
import info.nightscout.androidaps.plugins.general.careportal.Dialogs.NewNSTreatmentDialog;
import info.nightscout.androidaps.plugins.general.nsclient.data.NSSettingsStatus;
import info.nightscout.androidaps.plugins.general.themeselector.ScrollingActivity;
import info.nightscout.androidaps.plugins.general.themeselector.util.ThemeUtil;
import info.nightscout.androidaps.setupwizard.SetupWizardActivity;
import info.nightscout.androidaps.tabs.TabPageAdapter;
import info.nightscout.androidaps.utils.AndroidPermission;
import info.nightscout.androidaps.utils.FabricPrivacy;
import info.nightscout.androidaps.utils.LocaleHelper;
import info.nightscout.androidaps.utils.PasswordProtection;
import info.nightscout.androidaps.utils.SP;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;

import static info.nightscout.androidaps.plugins.general.careportal.CareportalFragment.INSULINCHANGE;
import static info.nightscout.androidaps.plugins.general.careportal.CareportalFragment.PUMPBATTERYCHANGE;
import static info.nightscout.androidaps.plugins.general.careportal.CareportalFragment.SENSORCHANGE;
import static info.nightscout.androidaps.plugins.general.careportal.CareportalFragment.SENSORSTART;
import static info.nightscout.androidaps.plugins.general.careportal.CareportalFragment.SITECHANGE;
import static info.nightscout.androidaps.plugins.general.themeselector.util.ThemeUtil.THEME_PINK;

// public class MainActivity extends AppCompatActivity {
public class MainActivity extends NoSplashAppCompatActivity {

    private static final ScheduledExecutorService worker = Executors.newSingleThreadScheduledExecutor();
    private static ScheduledFuture<?> scheduledUpdate = null;

    TextView iage;
    TextView cage;
    TextView sage;
    TextView iageView;
    TextView cageView;
    TextView reservoirView;
    TextView sageView;
    TextView batteryView;
    LinearLayout statuslightsLayout;

    private static Logger log = LoggerFactory.getLogger(L.CORE);
    private CompositeDisposable disposable = new CompositeDisposable();

    private ActionBarDrawerToggle actionBarDrawerToggle;

    private MenuItem pluginPreferencesMenuItem;

    public static int mTheme = THEME_PINK;
    public static boolean mIsNightMode = true;

    public void changeTheme(int newTheme){
        mTheme = newTheme;
        SP.putInt("theme", mTheme);
        SP.putBoolean("daynight", mIsNightMode);

        if(mIsNightMode){
            getDelegate().setLocalNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        }else{
            getDelegate().setLocalNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }
        setTheme(mTheme);

        TaskStackBuilder.create(this)
                .addNextIntent(new Intent(this, MainActivity.class))
                .addNextIntent(this.getIntent())
                .startActivities();
        recreate();

    }


    public static void applyStatuslight(TextView view, String text, double value, double warnThreshold, double urgentThreshold, double invalid, boolean checkAscending) {
        ColorStateList colorStateList = view.getTextColors();
        final int normalColor = colorStateList.getDefaultColor();

            Function<Double, Boolean> check = checkAscending ? (Double threshold) -> value > threshold : (Double threshold) -> value <= threshold;
        if (value != invalid) {
            view.setText(text);
            if (check.apply(urgentThreshold)) {
                view.setTextColor(MainApp.gc(R.color.low));
            } else if (check.apply(warnThreshold)) {
                view.setTextColor(MainApp.gc(R.color.high));
            } else {
                view.setTextColor(normalColor);
            }
            view.setVisibility(View.VISIBLE);
        } else {
            view.setVisibility(View.GONE);
        }

    }

    public void getCareportalInfo() {
        boolean shorttextmode = true;

        shorttextmode = true;

        final PumpInterface pump = ConfigBuilderPlugin.getPlugin().getActivePump();

        statuslightsLayout = (LinearLayout) findViewById(R.id.overview_statuslights);
        sageView =  findViewById(R.id.careportal_sensorage);
        iageView =  findViewById(R.id.careportal_insulinage);
        cageView =  findViewById(R.id.careportal_canulaage);
        reservoirView = findViewById(R.id.careportal_prLevel);
        batteryView = findViewById(R.id.careportal_pbLevel);

        if (statuslightsLayout != null) {
            if (SP.getBoolean(R.string.key_show_statuslights, false)) {
                CareportalEvent careportalEvent;
                NSSettingsStatus nsSettings = new NSSettingsStatus().getInstance();
                double iageUrgent = nsSettings.getExtendedWarnValue("iage", "urgent", 40);
                double iageWarn = nsSettings.getExtendedWarnValue("iage", "warn", 70);
                double cageUrgent = nsSettings.getExtendedWarnValue("cage", "urgent", 72);
                double cageWarn = nsSettings.getExtendedWarnValue("cage", "warn", 48);
                double sageUrgent = nsSettings.getExtendedWarnValue("sage", "urgent", 166);
                double sageWarn = nsSettings.getExtendedWarnValue("sage", "warn", 164);
                double batUrgent = SP.getDouble(R.string.key_statuslights_bat_critical, 5.0);
                double batWarn = SP.getDouble(R.string.key_statuslights_bat_warning, 25.0);
                double resUrgent = SP.getDouble(R.string.key_statuslights_res_critical, 10.0);
                double resWarn = SP.getDouble(R.string.key_statuslights_res_warning, 80.0);
                if (sageView != null) {
                careportalEvent = MainApp.getDbHelper().getLastCareportalEvent(CareportalEvent.SENSORCHANGE);
                double sensorAge = careportalEvent != null ? careportalEvent.getHoursFromStart() : Double.MAX_VALUE;
                applyStatuslight(sageView, "", sensorAge, sageWarn, sageUrgent, Double.MAX_VALUE, true);
                }

                if (iageView != null) {
                careportalEvent = MainApp.getDbHelper().getLastCareportalEvent(CareportalEvent.INSULINCHANGE);
                double insulinAge = careportalEvent != null ? careportalEvent.getHoursFromStart() : Double.MAX_VALUE;
                applyStatuslight(iageView, "IAGE", insulinAge, iageWarn, iageUrgent, Double.MAX_VALUE, true);
                }
                if (cageView != null) {
                careportalEvent = MainApp.getDbHelper().getLastCareportalEvent(CareportalEvent.SITECHANGE);
                double canAge = careportalEvent != null ? careportalEvent.getHoursFromStart() : Double.MAX_VALUE;
                applyStatuslight(cageView, "CAGE", canAge, cageWarn, cageUrgent, Double.MAX_VALUE, true);
                }

                if (reservoirView != null) {
                double reservoirLevel = pump.isInitialized() ? pump.getReservoirLevel() : -1;
                applyStatuslight(reservoirView, "RES", reservoirLevel, resWarn, resUrgent, -1, false);
                }

                if (batteryView != null) {
                double batteryLevel = pump.isInitialized() ? pump.getBatteryLevel() : -1;
                applyStatuslight(batteryView, "BAT", batteryLevel, batWarn, batUrgent, -1, false);
                }

                CareportalFragment.updateAge( MainActivity.this, sageView, iageView, cageView, batteryView);
                CareportalFragment.updatePumpSpecifics(reservoirView, batteryView);
                statuslightsLayout.setVisibility(View.VISIBLE);
        } else {
            statuslightsLayout.setVisibility(View.GONE);
        }
    }
    }

    public void onClick(View view) {
        action(view.getId(), getSupportFragmentManager());
    }

    public static void action(int id, FragmentManager manager) {
        NewNSTreatmentDialog newDialog = new NewNSTreatmentDialog();
        switch (id) {
            case R.id.sensorage:
                newDialog.setOptions(SENSORCHANGE, R.string.careportal_cgmsensorinsert);
                break;
            case R.id.careportal_cgmsensorstart:
                newDialog.setOptions(SENSORSTART, R.string.careportal_cgmsensorstart);
                break;
            case R.id.insulinage:
                newDialog.setOptions(INSULINCHANGE, R.string.careportal_insulincartridgechange);
                break;
            case R.id.canulaage:
                newDialog.setOptions(SITECHANGE, R.string.careportal_pumpsitechange);
                break;
            case R.id.batteryage:
                newDialog.setOptions(PUMPBATTERYCHANGE, R.string.careportal_pumpbatterychange);
                break;
            default:
                newDialog = null;
        }
        if (newDialog != null)
            newDialog.show(manager, "NewNSTreatmentDialog");
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {

        int newtheme = SP.getInt("theme", THEME_PINK);
        mTheme = newtheme;
        boolean newMode = SP.getBoolean("daynight", mIsNightMode);
        mIsNightMode = newMode;

        if(newMode){
            getDelegate().setLocalNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        }else{
            getDelegate().setLocalNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }
        setTheme(ThemeUtil.getThemeId(newtheme));

        super.onCreate(savedInstanceState);

        Iconify.with(new FontAwesomeModule());
        LocaleHelper.INSTANCE.update(getApplicationContext());

        setContentView(R.layout.activity_main);
        setSupportActionBar(findViewById(R.id.toolbar));
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);
        getSupportActionBar().setShowHideAnimationEnabled(true);
        DrawerLayout drawerLayout = findViewById(R.id.drawer_layout);
        actionBarDrawerToggle = new ActionBarDrawerToggle(this, drawerLayout, R.string.open_navigation, R.string.close_navigation);
        drawerLayout.addDrawerListener(actionBarDrawerToggle);

        actionBarDrawerToggle.syncState();

        // initialize screen wake lock
        processPreferenceChange(new EventPreferenceChange(R.string.key_keep_screen_on));

        doMigrations();

        final ViewPager viewPager = findViewById(R.id.pager);
        viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
            }

            @Override
            public void onPageSelected(int position) {
                //checkPluginPreferences(viewPager);
            }

            @Override
            public void onPageScrollStateChanged(int state) {
            }
        });

        //Check here if loop plugin is disabled. Else check via constraints
        if (!LoopPlugin.getPlugin().isEnabled(PluginType.LOOP))
            VersionCheckerUtilsKt.triggerCheckVersion();

        FabricPrivacy.setUserStats();

        //setupTabs();
        setupViews();

        disposable.add(RxBus.INSTANCE
                .toObservable(EventRebuildTabs.class)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(event -> {
                    LocaleHelper.INSTANCE.update(getApplicationContext());
                    if (event.getRecreate()) {
                        recreate();
                    } else {
                        //setupTabs();
                        setupViews();
                    }
                    setWakeLock();
                }, FabricPrivacy::logException)
        );
        disposable.add(RxBus.INSTANCE
                .toObservable(EventPreferenceChange.class)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::processPreferenceChange, FabricPrivacy::logException)
        );

        if (!SP.getBoolean(R.string.key_setupwizard_processed, false) || !SP.contains(R.string.key_units)) {
            Intent intent = new Intent(this, SetupWizardActivity.class);
            startActivity(intent);
        }

        AndroidPermission.notifyForStoragePermission(this);
        AndroidPermission.notifyForBatteryOptimizationPermission(this);
        if (Config.PUMPDRIVERS) {
            AndroidPermission.notifyForLocationPermissions(this);
            AndroidPermission.notifyForSMSPermissions(this);
        }
    }

    private void checkPluginPreferences(ViewPager viewPager) {
        if (pluginPreferencesMenuItem == null) return;
        if (((TabPageAdapter) viewPager.getAdapter()).getPluginAt(viewPager.getCurrentItem()).getPreferencesId() != -1)
            pluginPreferencesMenuItem.setEnabled(true);
        else pluginPreferencesMenuItem.setEnabled(false);
    }

    @Override
    public void onPostCreate(@Nullable Bundle savedInstanceState, @Nullable PersistableBundle persistentState) {
        super.onPostCreate(savedInstanceState, persistentState);
        actionBarDrawerToggle.syncState();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        disposable.clear();
    }

    @Override
    protected void onResume() {
        super.onResume();
        disposable.add(RxBus.INSTANCE
                .toObservable(EventCareportalEventChange.class)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(event -> scheduleUpdate("EventCareportalEventChange"),
                        FabricPrivacy::logException
                ));
        disposable.add(RxBus.INSTANCE
                .toObservable(EventInitializationChanged.class)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(event -> scheduleUpdate("EventInitializationChanged"),
                        FabricPrivacy::logException
                ));
        disposable.add(RxBus.INSTANCE
                .toObservable(EventPumpStatusChanged.class)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(event -> scheduleUpdate(event.getStatus()),
                        FabricPrivacy::logException
                ));
        this.getCareportalInfo();
    }

    private void setWakeLock() {
        boolean keepScreenOn = SP.getBoolean(R.string.key_keep_screen_on, false);
        if (keepScreenOn)
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        else
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    public void processPreferenceChange(final EventPreferenceChange ev) {
        if (ev.isChanged(R.string.key_keep_screen_on))
            setWakeLock();
    }

    private void setupViews() {
        TabPageAdapter pageAdapter = new TabPageAdapter(getSupportFragmentManager(), this);
        NavigationView navigationView = findViewById(R.id.navigation_view);
        navigationView.getHeaderView(0);
        navigationView.setNavigationItemSelectedListener(menuItem -> true);
        Menu menu = navigationView.getMenu();
        menu.clear();
        for (PluginBase p : MainApp.getPluginsList()) {
            pageAdapter.registerNewFragment(p);
            if (p.hasFragment()  && p.isEnabled(p.pluginDescription.getType()) && !p.pluginDescription.neverVisible) {
                MenuItem menuItem = menu.add(p.getName());
                menuItem.setIcon(R.drawable.ic_settings);
                menuItem.setCheckable(true);
                menuItem.setOnMenuItemClickListener(item -> {
                    Intent intent = new Intent(this, SingleFragmentActivity.class);
                    intent.putExtra("plugin", MainApp.getPluginsList().indexOf(p));
                    startActivity(intent);
                    ((DrawerLayout) findViewById(R.id.drawer_layout)).closeDrawers();
                    return true;
                });
            }
        }
        ViewPager mPager = findViewById(R.id.pager);
        mPager.setAdapter(pageAdapter);
        //if (switchToLast)
        //    mPager.setCurrentItem(pageAdapter.getCount() - 1, false);
        checkPluginPreferences(mPager);
    }

    private void doMigrations() {
        // guarantee that the unreachable threshold is at least 30 and of type String
        // Added in 1.57 at 21.01.2018
        int unreachable_threshold = SP.getInt(R.string.key_pump_unreachable_threshold, 30);
        SP.remove(R.string.key_pump_unreachable_threshold);
        if (unreachable_threshold < 30) unreachable_threshold = 30;
        SP.putString(R.string.key_pump_unreachable_threshold, Integer.toString(unreachable_threshold));
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (permissions.length != 0) {
            if (ActivityCompat.checkSelfPermission(this, permissions[0]) == PackageManager.PERMISSION_GRANTED) {
                switch (requestCode) {
                    case AndroidPermission.CASE_STORAGE:
                        //show dialog after permission is granted
                        AlertDialog.Builder alert = new AlertDialog.Builder(this);
                        alert.setMessage(R.string.alert_dialog_storage_permission_text);
                        alert.setPositiveButton(R.string.ok, null);
                        alert.show();
                        break;
                    case AndroidPermission.CASE_LOCATION:
                    case AndroidPermission.CASE_SMS:
                    case AndroidPermission.CASE_BATTERY:
                    case AndroidPermission.CASE_PHONE_STATE:
                        break;
                }
            }
        }
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            View v = getCurrentFocus();
            if (v instanceof EditText) {
                Rect outRect = new Rect();
                v.getGlobalVisibleRect(outRect);
                if (!outRect.contains((int) event.getRawX(), (int) event.getRawY())) {
                    v.clearFocus();
                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
                }
            }
        }
        return super.dispatchTouchEvent(event);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        pluginPreferencesMenuItem = menu.findItem(R.id.nav_plugin_preferences);
       // checkPluginPreferences(findViewById(R.id.pager));
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch (id) {
            case R.id.nav_preferences:
                PasswordProtection.QueryPassword(this, R.string.settings_password, "settings_password", () -> {
                    Intent i = new Intent(this, PreferencesActivity.class);
                    i.putExtra("id", -1);
                    startActivity(i);
                }, null);
                return true;
            case R.id.nav_historybrowser:
                startActivity(new Intent(this, HistoryBrowseActivity.class));
                return true;
            case R.id.nav_themeselector:
                startActivity(new Intent(this, ScrollingActivity.class));
                return true;
            case R.id.nav_setupwizard:
                startActivity(new Intent(this, SetupWizardActivity.class));
                return true;
            case R.id.nav_about:
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle(MainApp.gs(R.string.app_name) + " " + BuildConfig.VERSION);
                builder.setIcon(MainApp.getIcon());
                String message = "Build: " + BuildConfig.BUILDVERSION + "\n";
                message += "Flavor: " + BuildConfig.FLAVOR + BuildConfig.BUILD_TYPE + "\n";
                message += MainApp.gs(R.string.configbuilder_nightscoutversion_label) + " " + NSSettingsStatus.getInstance().nightscoutVersionName;
                if (MainApp.engineeringMode)
                    message += "\n" + MainApp.gs(R.string.engineering_mode_enabled);
                message += MainApp.gs(R.string.about_link_urls);
                final SpannableString messageSpanned = new SpannableString(message);
                Linkify.addLinks(messageSpanned, Linkify.WEB_URLS);
                builder.setMessage(messageSpanned);
                builder.setPositiveButton(MainApp.gs(R.string.ok), null);
                AlertDialog alertDialog = builder.create();
                alertDialog.show();
                ((TextView) alertDialog.findViewById(android.R.id.message)).setMovementMethod(LinkMovementMethod.getInstance());
                return true;
            case R.id.nav_exit:
                log.debug("Exiting");
                MainApp.instance().stopKeepAliveService();
                RxBus.INSTANCE.send(new EventAppExit());
                MainApp.closeDbHelper();
                finish();
                System.runFinalization();
                System.exit(0);
                return true;
            case R.id.nav_plugin_preferences:
                ViewPager viewPager = findViewById(R.id.pager);
                final PluginBase plugin = ((TabPageAdapter) viewPager.getAdapter()).getPluginAt(viewPager.getCurrentItem());
                PasswordProtection.QueryPassword(this, R.string.settings_password, "settings_password", () -> {
                    Intent i = new Intent(this, PreferencesActivity.class);
                    i.putExtra("id", plugin.getPreferencesId());
                    startActivity(i);
                }, null);
                return true;
/*
            case R.id.nav_survey:
                startActivity(new Intent(this, SurveyActivity.class));
                return true;
*/
            case R.id.nav_stats:
                startActivity(new Intent(this, StatsActivity.class));
                return true;
        }
        return actionBarDrawerToggle.onOptionsItemSelected(item);
    }


    public void scheduleUpdate(final String from) {
        class UpdateRunnable implements Runnable {
            public void run() {
                Activity activity = MainActivity.this;
                if (activity != null)
                    activity.runOnUiThread(() -> {
                        getCareportalInfo();
                        scheduledUpdate = null;
                    });
            }
        }
        // prepare task for execution in 500 msec
        // cancel waiting task to prevent multiple updates
        if (scheduledUpdate != null)
            scheduledUpdate.cancel(false);
        Runnable task = new UpdateRunnable();
        final int msec = 500;
        scheduledUpdate = worker.schedule(task, msec, TimeUnit.MILLISECONDS);
    }
}
