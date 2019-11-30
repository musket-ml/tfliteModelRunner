package com.onpositive.dldemos;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager.widget.ViewPager;

import com.google.android.material.tabs.TabLayout;
import com.onpositive.dldemos.tools.Logger;

import butterknife.BindView;
import butterknife.ButterKnife;


public class MainActivity extends AppCompatActivity {

    private static SectionsPagerAdapter mSectionsPagerAdapter;
    @BindView(R.id.tabs)
    TabLayout tabLayout;
    @BindView(R.id.container)
    ViewPager mViewPager;
    private Logger log = new Logger(this.getClass());

    public static void refreshTabs() {
        mSectionsPagerAdapter.refreshDataSet();
        mSectionsPagerAdapter.notifyDataSetChanged();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager(), this);
        mViewPager.setAdapter(mSectionsPagerAdapter);
        tabLayout.setupWithViewPager(mViewPager);
        //TODO fix progress showing
        //TODO model properties image size(input/output)
        //TODO fragment model for image classification(find test model for this task)
        //TODO remove models tabs

        //FIXME bug on removing many tabs. Add few model -> reload app -> try to remove this models. Result: user can not remove model, also TFliteAddFragment is a model fragment.
        //FIXME bug on fragment resume, thumbnails are empty for result items
        //FIXME wrong fragment type on add/delete new model

        //TODO add crashlytics
        //TODO try to run segmentation model on the TF APP
        log.log("onCreate executed. Tabs load complete.");
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        log.log("onActivityResult");
    }

    public SectionsPagerAdapter getSectionsPagerAdapter() {
        return mSectionsPagerAdapter;
    }

    public void setSectionsPagerAdapter(SectionsPagerAdapter mSectionsPagerAdapter) {
        this.mSectionsPagerAdapter = mSectionsPagerAdapter;
    }
}
