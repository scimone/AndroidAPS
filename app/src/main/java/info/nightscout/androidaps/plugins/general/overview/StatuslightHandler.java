package info.nightscout.androidaps.plugins.general.overview;

import android.content.res.ColorStateList;
import android.view.View;
import android.widget.TextView;

import androidx.arch.core.util.Function;

import info.nightscout.androidaps.MainApp;
import info.nightscout.androidaps.R;
import info.nightscout.androidaps.db.CareportalEvent;
import info.nightscout.androidaps.interfaces.PumpInterface;
import info.nightscout.androidaps.plugins.configBuilder.ConfigBuilderPlugin;
import info.nightscout.androidaps.plugins.general.careportal.CareportalFragment;
import info.nightscout.androidaps.plugins.general.nsclient.data.NSSettingsStatus;
import info.nightscout.androidaps.plugins.pump.common.defs.PumpType;
import info.nightscout.androidaps.utils.DecimalFormatter;
import info.nightscout.androidaps.utils.SP;

public class StatuslightHandler {

    ColorStateList oldColors = null;
    boolean extended = false;
    /**
     * applies the statuslight subview on the overview fragement
     */
   public void statuslight(TextView cageView,  TextView reservoirView,
                     TextView sageView, TextView batteryView) {
        PumpInterface pump = ConfigBuilderPlugin.getPlugin().getActivePump();

        applyStatuslight( "cage", CareportalEvent.SITECHANGE, cageView, extended ? (MainApp.getDbHelper().getLastCareportalEvent(CareportalEvent.SITECHANGE).age(true) + " ") : "", 24, 60);
        handleAge("cage", CareportalEvent.SITECHANGE, cageView, "CAN ",
               24, 60);

        double reservoirLevel = pump.isInitialized() ? pump.getReservoirLevel() : -1;
        applyStatuslightLevel(R.string.key_statuslights_res_critical, 20.0,
                R.string.key_statuslights_res_warning, 50.0, reservoirView, "", reservoirLevel);
        reservoirView.setText(extended ? (DecimalFormatter.to0Decimal(reservoirLevel) + "U  ") : "");

        applyStatuslight("sage", CareportalEvent.SENSORCHANGE, sageView, extended ? (MainApp.getDbHelper().getLastCareportalEvent(CareportalEvent.SENSORCHANGE).age(true) + " ") : "", 164, 166);

       if (  pump.model() == PumpType.DanaRS) {
           applyStatuslight( "bage", CareportalEvent.PUMPBATTERYCHANGE, batteryView, extended ? (MainApp.getDbHelper().getLastCareportalEvent(CareportalEvent.PUMPBATTERYCHANGE).age(true) + " ") : "", 240, 504);
        } else if(pump.model() == PumpType.DanaRv2 ||
                  pump.model() == PumpType.AccuChekCombo) {
            applyStatuslight("bage", CareportalEvent.PUMPBATTERYCHANGE, batteryView, extended ? (MainApp.getDbHelper().getLastCareportalEvent(CareportalEvent.PUMPBATTERYCHANGE).age(true) + " ") : "", 240, 504);
        } else {
           // all other pumps
           double batteryLevel = pump.isInitialized() ? pump.getBatteryLevel() : -1;
           applyStatuslightLevel( R.string.key_statuslights_bat_critical, 30.0,
                   R.string.key_statuslights_bat_warning, 51.0,
                   batteryView, "", batteryLevel);
           batteryView.setText(extended ? (DecimalFormatter.to0Decimal(batteryLevel) + "%  ") : "");
        }
    }

    void handleAge(String nsSettingPlugin, String eventName, TextView view, String text,
                   int defaultWarnThreshold, int defaultUrgentThreshold) {
        NSSettingsStatus nsSettings = new NSSettingsStatus().getInstance();

        if (view != null) {
            double urgent = nsSettings.getExtendedWarnValue(nsSettingPlugin, "urgent", defaultUrgentThreshold);
            double warn = nsSettings.getExtendedWarnValue(nsSettingPlugin, "warn", defaultWarnThreshold);
            CareportalFragment.handleAge(view, text, eventName, warn, urgent, true);
        }
    }

  public  void applyStatuslight(String nsSettingPlugin, String eventName, TextView view, String text,
                          int defaultWarnThreshold, int defaultUrgentThreshold) {
        NSSettingsStatus nsSettings = NSSettingsStatus.getInstance();

        if (view != null) {
            double urgent = nsSettings.getExtendedWarnValue(nsSettingPlugin, "urgent", defaultUrgentThreshold);
            double warn = nsSettings.getExtendedWarnValue(nsSettingPlugin, "warn", defaultWarnThreshold);
            CareportalEvent event = MainApp.getDbHelper().getLastCareportalEvent(eventName);
            double age = event != null ? event.getHoursFromStart() : Double.MAX_VALUE;
            applyStatuslight(view, text, age, warn, urgent, Double.MAX_VALUE, true);
        }
    }

  public  void applyStatuslightLevel( int criticalSetting, double criticalDefaultValue,
                               int warnSetting, double warnDefaultValue,
                               TextView view, String text, double level) {
        if (view != null) {
            double resUrgent = SP.getDouble(criticalSetting, criticalDefaultValue);
            double resWarn = SP.getDouble(warnSetting, warnDefaultValue);
            applyStatuslight(view, text, level, resWarn, resUrgent, -1, false);
        }
    }


  public  void applyStatuslight(TextView view, String text, double value, double warnThreshold,
                          double urgentThreshold, double invalid, boolean checkAscending) {
        Function<Double, Boolean> check = checkAscending ? (Double threshold) -> value >= threshold :
                (Double threshold) -> value < threshold;

        if( this.oldColors == null ) {
            this.oldColors = view.getTextColors();
        }
        view.setTextColor(this.oldColors);
      //Log.d("TAG", "get statuslight value: " + value + " text: " + text + " warn: " + warnThreshold + " urgent: " + urgentThreshold );
        if (value != invalid) {
            view.setText(text);
            if (check.apply(urgentThreshold)) {
                view.setTextColor(MainApp.gc(R.color.ribbonCritical));
                //Log.d("TAG", "urgentThreshold: " + urgentThreshold + " value: " + value + " text: " + text);
            } else if (check.apply(warnThreshold)) {
                view.setTextColor(MainApp.gc(R.color.ribbonWarning));
                //Log.d("TAG", "warnThreshold: " + warnThreshold + " value: " + value + " text: " + text );
            } else {
                view.setTextColor(this.oldColors);
            }
            view.setVisibility(View.VISIBLE);
        } else {
            view.setVisibility(View.GONE);
        }

    }

    /**
     * applies the extended statuslight subview on the overview fragement
     */
 public   void extendedStatuslight(TextView cageView,
                             TextView reservoirView, TextView sageView,
                             TextView batteryView) {

        extended = true;
        statuslight( cageView, reservoirView, sageView, batteryView);
    }
}