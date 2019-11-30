package com.onpositive.dldemos;

import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.onpositive.dldemos.data.ClassificationResultItem;
import com.onpositive.dldemos.data.RecyclerViewListAdapter;
import com.onpositive.dldemos.tools.Logger;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class ClassifyResultItemFragment extends Fragment {

    private static Logger log = new Logger(ClassificationFragment.class);
    @BindView(R.id.photo_iv)
    ImageView photoIV;
    @BindView(R.id.close_btn)
    Button closeBtn;
    @BindView(R.id.classify_results_rv)
    RecyclerView listRV;
    RecyclerViewListAdapter rvListAdapter;
    private ClassificationResultItem resultItem;

    public static ClassifyResultItemFragment newInstance(ClassificationResultItem item) {
        ClassifyResultItemFragment fragment = new ClassifyResultItemFragment();
        fragment.resultItem = item;
        log.log("Created fragment for the file: " + item.getFilePath());
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        log.log("onCreate executed");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_classifyitem, container, false);
        ButterKnife.bind(this, rootView);
        photoIV.setImageBitmap(BitmapFactory.decodeFile(resultItem.getFilePath()));
        RecyclerView.ItemAnimator itemAnimator = new DefaultItemAnimator();
        listRV.setLayoutManager(new LinearLayoutManager(this.getContext()));
        listRV.setItemAnimator(itemAnimator);
        try {
            rvListAdapter = new RecyclerViewListAdapter(resultItem.getClassificationResultList());
            listRV.setAdapter(rvListAdapter);
            log.log("Classification results were uploaded");
        } catch (Exception e) {
            log.log("Classification results upload failed: " + e.getMessage());
        }
        return rootView;
    }

    @OnClick({R.id.close_btn})
    public void onCick(View view) {
        switch (view.getId()) {
            case R.id.close_btn:
                log.log("Close button clicked");
                getActivity().getSupportFragmentManager().beginTransaction().remove(this).commit();
                break;
        }
    }
}