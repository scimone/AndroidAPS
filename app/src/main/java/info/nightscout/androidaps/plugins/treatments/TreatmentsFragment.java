package info.nightscout.androidaps.plugins.treatments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.google.android.material.tabs.TabLayout;

import info.nightscout.androidaps.R;
import info.nightscout.androidaps.events.EventExtendedBolusChange;
import info.nightscout.androidaps.plugins.bus.RxBus;
import info.nightscout.androidaps.plugins.configBuilder.ConfigBuilderPlugin;
import info.nightscout.androidaps.plugins.treatments.fragments.TreatmentsBolusFragment;
import info.nightscout.androidaps.plugins.treatments.fragments.TreatmentsCareportalFragment;
import info.nightscout.androidaps.plugins.treatments.fragments.TreatmentsExtendedBolusesFragment;
import info.nightscout.androidaps.plugins.treatments.fragments.TreatmentsProfileSwitchFragment;
import info.nightscout.androidaps.plugins.treatments.fragments.TreatmentsTempTargetFragment;
import info.nightscout.androidaps.plugins.treatments.fragments.TreatmentsTemporaryBasalsFragment;
import info.nightscout.androidaps.utils.FabricPrivacy;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;

public class TreatmentsFragment extends Fragment {
    private CompositeDisposable disposable = new CompositeDisposable();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

            View view = inflater.inflate(R.layout.treatments_fragment, container, false);

            TabLayout tabLayout = (TabLayout) view.findViewById(R.id.tab_layout);
            tabLayout.addTab(tabLayout.newTab().setText(R.string.bolus));
            tabLayout.addTab(tabLayout.newTab().setText(R.string.extendedbolus));
            tabLayout.addTab(tabLayout.newTab().setText(R.string.tempbasal));
            tabLayout.addTab(tabLayout.newTab().setText(R.string.temptarget));
            tabLayout.addTab(tabLayout.newTab().setText(R.string.profileswitch));
            tabLayout.addTab(tabLayout.newTab().setText(R.string.careportal));

            final ViewPager viewPager = (ViewPager) view.findViewById(R.id.viewPager);
            viewPager.setAdapter(new CustomAdapter(getFragmentManager(), tabLayout.getTabCount()));

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

    @Override
    public synchronized void onResume() {
        super.onResume();
        disposable.add(RxBus.INSTANCE
                .toObservable(EventExtendedBolusChange.class)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(event -> updateGui(), FabricPrivacy::logException)
        );
        updateGui();
    }

    @Override
    public synchronized void onPause() {
        super.onPause();
        disposable.clear();
    }

    private void updateGui() {
        if (ConfigBuilderPlugin.getPlugin().getActivePump().getPumpDescription().isExtendedBolusCapable
                || TreatmentsPlugin.getPlugin().getExtendedBolusesFromHistory().size() > 0) {
           // extendedBolusesTab.setVisibility(View.VISIBLE);
        } else {
           // extendedBolusesTab.setVisibility(View.GONE);
        }
    }
}