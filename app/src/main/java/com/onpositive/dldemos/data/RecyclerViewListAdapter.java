package com.onpositive.dldemos.data;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.onpositive.dldemos.R;
import com.onpositive.dldemos.classification.ImageClassifier;
import com.onpositive.dldemos.tools.Logger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

public class RecyclerViewListAdapter extends RecyclerView.Adapter<RecyclerViewListAdapter.ListItemHolder> {

    private static final float FILTER_VALUE = 0.5f;
    private static Logger log = new Logger(RecyclerViewListAdapter.class);
    private List<ImageClassifier.Classification> classificationRIFilteredList;

    public RecyclerViewListAdapter(List<ImageClassifier.Classification> classificationResultItemList) {
        List<ImageClassifier.Classification> classificationFilteredList = new ArrayList<>();
        Collections.sort(classificationResultItemList);
        for (ImageClassifier.Classification classification : classificationResultItemList) {
            if (classification.getConfidence() > FILTER_VALUE) {
                classificationFilteredList.add(classification);
            }
        }
        Collections.reverse(classificationFilteredList);
        this.classificationRIFilteredList = classificationFilteredList;
        log.log("RecyclerViewListAdapter created");
    }

    @Override
    public ListItemHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View layoutView = LayoutInflater.from(parent.getContext()).inflate(R.layout.classification_item, parent, false);
        return new ListItemHolder(layoutView);
    }

    @Override
    public void onBindViewHolder(ListItemHolder holder, int position) {
        holder.itemTV.setText(
                classificationRIFilteredList.get(position).toString());
    }

    @Override
    public int getItemCount() {
        return classificationRIFilteredList.size();
    }

    public class ListItemHolder extends RecyclerView.ViewHolder {

        @BindView(R.id.classification_item_tv)
        TextView itemTV;

        public ListItemHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }
    }
}
