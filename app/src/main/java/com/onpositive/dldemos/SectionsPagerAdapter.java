package com.onpositive.dldemos;

import android.content.Context;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;

import com.onpositive.dldemos.data.AppDatabase;
import com.onpositive.dldemos.data.TFLiteItem;
import com.onpositive.dldemos.data.TFLiteItemDao;
import com.onpositive.dldemos.data.TFModelType;
import com.onpositive.dldemos.tools.Logger;

import java.util.List;

public class SectionsPagerAdapter extends FragmentPagerAdapter {

    Logger log = new Logger(this.getClass());
    private AppDatabase db;
    private TFLiteItemDao tfLiteItemDao;
    private List<TFLiteItem> tfLiteItems;
    private Context context;

    public SectionsPagerAdapter(FragmentManager fm, Context context) {
        super(fm);
        this.context = context;
        db = MLDemoApp.getInstance().getDatabase();
        tfLiteItemDao = db.tfLiteItemDao();
        tfLiteItems = tfLiteItemDao.getAll();
        log.log("SectionsPagerAdapter created");
    }

    @Override
    public Fragment getItem(int position) {
        if (position == tfLiteItems.size()) {
            return TFliteAddFragment.newInstance(position);
        }
        TFLiteItem tfLiteItem = tfLiteItems.get(position);
        if (tfLiteItem.getModelType() == TFModelType.CLASSIFICATION) {
            return ClassificationFragment.newInstance(tfLiteItem, position);
        } else if (tfLiteItem.getModelType() == TFModelType.SEGMENTATION) {
            return SegmentationFragment.newInstance(tfLiteItem, position);
        }
        return null;
    }

    @Override
    public int getCount() {
        log.log("Tabs count: " + tfLiteItems.size() + 1);
        return tfLiteItems.size() + 1;
    }

    @Override
    public CharSequence getPageTitle(int position) {
        if (position < getCount() - 1) {
            log.log("Tab title: " + tfLiteItems.get(position).getTitle() + "\n Tab position: " + position);
            return tfLiteItems.get(position).getTitle();
        } else if (position == getCount() - 1) {
            log.log("Tab title: " + context.getResources().getString(R.string.add_new) + "\n Tab position: " + position);
            return context.getResources().getString(R.string.add_new);
        } else
            return null;
    }

    public void refreshDataSet() {
        tfLiteItems = tfLiteItemDao.getAll();
        log.log("tfLite model items list reloaded");
    }
}
