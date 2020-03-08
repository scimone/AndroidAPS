package info.nightscout.androidaps.plugins.general.overview;

import android.content.res.ColorStateList;
import android.view.View;
import android.widget.TextView;

import androidx.arch.core.util.Function;

import java.util.Objects;

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
import info.nightscout.androidaps.utils.SetWarnColor;

public class StatuslightHandler {

    private ColorStateList oldColors = null;
    private boolean extended = false;
    /**
     * applies the statuslight subview on the overview fragement
     */
   public void statuslight(TextView cageView,  TextView reservoirView,
                     TextView sageView, TextView batteryView) {
        PumpInterface pump = ConfigBuilderPlugin.getPlugin().getActivePump();

        // Canula age
        if( cageView != null ) {
            applyStatuslight( "cage", CareportalEvent.SITECHANGE, cageView, extended ? (Objects.requireNonNull(MainApp.getDbHelper().getLastCareportalEvent(CareportalEvent.SITECHANGE)).age(true) + " ") : "", 24, 36);
            handleAge("cage", CareportalEvent.SITECHANGE, cageView, "CAN ",
                    24, 60);
        }

       assert pump != null;
       if( pump.isInitialized() ){
            // Reservoir age
            if ( reservoirView != null) {
                double reservoirLevel = pump.isInitialized() ? pump.getReservoirLevel() : -1;
                applyStatuslightLevel(R.string.key_statuslights_res_critical, 40.0,
                        R.string.key_statuslights_res_warning, 80.0, reservoirView, "", reservoirLevel);
                reservoirView.setText(extended ? (DecimalFormatter.to0Decimal(reservoirLevel) + "U  ") : "");
            }
            // Sensor age
            if( sageView != null) {
                applyStatuslight("sage", CareportalEvent.SENSORCHANGE, sageView, extended ? (Objects.requireNonNull(MainApp.getDbHelper().getLastCareportalEvent(CareportalEvent.SENSORCHANGE)).age(true) + " ") : "", 164, 166);
            }

            if(batteryView != null) {
                if (  pump.model() == PumpType.DanaRS || pump.model() == PumpType.DanaR) {
                    applyStatuslight( "bage", CareportalEvent.PUMPBATTERYCHANGE, batteryView, extended ? (Objects.requireNonNull(MainApp.getDbHelper().getLastCareportalEvent(CareportalEvent.PUMPBATTERYCHANGE)).age(true) + " ") : "", 240, 504);
                } else if(pump.model() == PumpType.DanaRv2 ||
                        pump.model() == PumpType.AccuChekCombo) {
                    applyStatuslight("bage", CareportalEvent.PUMPBATTERYCHANGE, batteryView, extended ? (Objects.requireNonNull(MainApp.getDbHelper().getLastCareportalEvent(CareportalEvent.PUMPBATTERYCHANGE)).age(true) + " ") : "", 240, 504);
                } else if ( pump.model() == PumpType.AccuChekInsight ||
                        pump.model() == PumpType.AccuChekInsightBluetooth ) {
                    handleLevel(R.string.key_statuslights_bat_critical, 26.0,
                            R.string.key_statuslights_bat_warning, 51.0,
                            batteryView, "BAT ", pump.getBatteryLevel());

                } else {
                    // all other pumps
                    handleLevel(R.string.key_statuslights_bat_critical, 26.0,
                            R.string.key_statuslights_bat_warning, 51.0,
                            batteryView, "BAT ", pump.getBatteryLevel());
                }
            }

        }
    }

    private void handleLevel(int criticalSetting, double criticalDefaultValue,
                             int warnSetting, double warnDefaultValue,
                             TextView view, String text, double batteryLevel) {
        if (view != null) {
            double resUrgent = SP.getDouble(criticalSetting, criticalDefaultValue);
            double resWarn = SP.getDouble(warnSetting, warnDefaultValue);
            view.setText(extended ? (DecimalFormatter.to0Decimal(batteryLevel) + "%  ") : "");
            SetWarnColor.setColorInverse(view, batteryLevel, resWarn, resUrgent);
        }
    }
    
    private void handleAge(String nsSettingPlugin, String eventName, TextView view, String text,
                           int defaultWarnThreshold, int defaultUrgentThreshold) {
        NSSettingsStatus nsSettings = new NSSettingsStatus().getInstance();

        if (view != null) {
            double urgent = nsSettings.getExtendedWarnValue(nsSettingPlugin, "urgent", defaultUrgentThreshold);
            double warn = nsSettings.getExtendedWarnValue(nsSettingPlugin, "warn", defaultWarnThreshold);
            CareportalFragment.handleAge(view, text, eventName, warn, urgent, true);
        }
    }

  private void applyStatuslight(String nsSettingPlugin, String eventName, TextView view, String text,
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

  private void applyStatuslightLevel(int criticalSetting, double criticalDefaultValue,
                                     int warnSetting, double warnDefaultValue,
                                     TextView view, String text, double level) {
        if (view != null) {
            double resUrgent = SP.getDouble(criticalSetting, criticalDefaultValue);
            double resWarn = SP.getDouble(warnSetting, warnDefaultValue);
            applyStatuslight(view, text, level, resWarn, resUrgent, -1, false);
        }
    }


  private void applyStatuslight(TextView view, String text, double value, double warnThreshold,
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
