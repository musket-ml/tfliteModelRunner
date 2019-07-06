package com.onpositive.dldemos;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;

import com.onpositive.dldemos.tools.Logger;

import butterknife.BindView;
import butterknife.ButterKnife;


public class MainActivity extends AppCompatActivity {

    @BindView(R.id.tabs)
    TabLayout tabLayout;
    @BindView(R.id.container)
    ViewPager mViewPager;
    private SectionsPagerAdapter mSectionsPagerAdapter;
    private Logger log = new Logger(this.getClass());

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

        //FIXME thumbnails empty at the end of list, if more than 12 result items
        //FIXME bug on fragment resume, thumbnails are empty for result items
        //FIXME wrong fragment type on add/delete new model

        //TODO add Logger everywhere
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
