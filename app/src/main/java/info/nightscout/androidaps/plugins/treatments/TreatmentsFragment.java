package info.nightscout.androidaps.plugins.treatments;

import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.squareup.otto.Subscribe;

import info.nightscout.androidaps.MainApp;
import info.nightscout.androidaps.R;
import info.nightscout.androidaps.events.EventExtendedBolusChange;
import info.nightscout.androidaps.plugins.common.SubscriberFragment;
import info.nightscout.androidaps.plugins.configBuilder.ConfigBuilderPlugin;
import info.nightscout.androidaps.plugins.treatments.fragments.TreatmentsBolusFragment;
import info.nightscout.androidaps.plugins.treatments.fragments.TreatmentsCareportalFragment;
import info.nightscout.androidaps.plugins.treatments.fragments.TreatmentsExtendedBolusesFragment;
import info.nightscout.androidaps.plugins.treatments.fragments.TreatmentsProfileSwitchFragment;
import info.nightscout.androidaps.plugins.treatments.fragments.TreatmentsTempTargetFragment;
import info.nightscout.androidaps.plugins.treatments.fragments.TreatmentsTemporaryBasalsFragment;
import info.nightscout.androidaps.utils.FabricPrivacy;

public class TreatmentsFragment extends SubscriberFragment  {


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        try {
            View view = inflater.inflate(R.layout.treatments_fragment, container, false);

            TabLayout tabLayout = (TabLayout) view.findViewById(R.id.tab_layout);

            tabLayout.addTab(tabLayout.newTab().setText(R.string.bolus));
            tabLayout.addTab(tabLayout.newTab().setText(R.string.extendedbolus));
            tabLayout.addTab(tabLayout.newTab().setText(R.string.tempbasal));
            tabLayout.addTab(tabLayout.newTab().setText(R.string.temptarget));
            tabLayout.addTab(tabLayout.newTab().setText(R.string.profileswitch));
            tabLayout.addTab(tabLayout.newTab().setText(R.string.careportal));

            final ViewPager viewPager = (ViewPager) view.findViewById(R.id.viewPager);
            viewPager.setAdapter(new CustomAdapter(getFragmentManager(),
                    tabLayout.getTabCount()));

                viewPager.addOnPageChangeListener(new TabLayout.TabLayoutOnPageChangeListener(tabLayout));
                tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener(){
                @Override
                public void onTabSelected(TabLayout.Tab tab) {
                    viewPager.setCurrentItem(tab.getPosition());
                }

                @Override
                public void onTabUnselected(TabLayout.Tab tab) {

                }

                @Override
                public void onTabReselected(TabLayout.Tab tab) {

                }
            });

            viewPager.setCurrentItem(0);
            return view;
        } catch (Exception e) {
            FabricPrivacy.logException(e);
        }

        return null;

    }

    private class CustomAdapter extends FragmentStatePagerAdapter {
        int numberOfTabs;

        public CustomAdapter(FragmentManager fragmentManager, int numberOfTabs) {
            super( fragmentManager);
            this.numberOfTabs = numberOfTabs;
        }

        @Override
        public Fragment getItem(int position) {

            switch (position)
            {
                case 0:
                    Fragment tab0 = new TreatmentsBolusFragment();
                    return tab0;
                case 1:
                    Fragment tab1 = new TreatmentsExtendedBolusesFragment();
                    return tab1;
                case 2:
                    Fragment tab2 = new TreatmentsTemporaryBasalsFragment();
                    return tab2;
                case 3:
                    Fragment tab3 = new TreatmentsTempTargetFragment();
                    return tab3;
                case 4:
                    Fragment tab4 = new TreatmentsProfileSwitchFragment();
                    return tab4;
                case 5:
                    Fragment tab5 = new TreatmentsCareportalFragment();
                    return tab5;
                default:
                    Fragment tab_default = new TreatmentsBolusFragment();
                    return tab_default;
            }
        }

        @Override
        public int getCount() {
            return numberOfTabs;
        }
    }


    @Subscribe
    public void onStatusEvent(final EventExtendedBolusChange ev) {
        //updateGUI();
    }

    @Override
    protected void updateGUI() {
        if (ConfigBuilderPlugin.getPlugin().getActivePump().getPumpDescription().isExtendedBolusCapable
                || info.nightscout.androidaps.plugins.treatments.TreatmentsPlugin.getPlugin().getExtendedBolusesFromHistory().size() > 0) {
            //extendedBolusesTab.setVisibility(View.VISIBLE);
        } else {
           // extendedBolusesTab.setVisibility(View.GONE);
        }
    }
}