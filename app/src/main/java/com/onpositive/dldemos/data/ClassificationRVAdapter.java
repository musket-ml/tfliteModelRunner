package com.onpositive.dldemos.data;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.view.ActionMode;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.onpositive.dldemos.ClassifyResultItemFragment;
import com.onpositive.dldemos.MLDemoApp;
import com.onpositive.dldemos.R;
import com.onpositive.dldemos.interpreter.ImageClassifier;
import com.onpositive.dldemos.tools.Logger;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.OnLongClick;

public class ClassificationRVAdapter extends RecyclerView.Adapter<ClassificationRVAdapter.ViewHolder> {

    private Context context;
    private List<? extends ResultItem> itemList;
    private Map<Integer, ClassificationRVAdapter.ViewHolder> selectedResultItems = new TreeMap<>(Collections.reverseOrder());
    private Logger log = new Logger(this.getClass());
    private boolean isSelectionMode = false;
    private ActionMode.Callback actionModeCallbacks = new ActionMode.Callback() {
        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            isSelectionMode = true;
            menu.add(context.getResources().getString(R.string.delete));
            log.log("onCreateActionMode");
            return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            log.log("onPrepareActionMode");
            return false;
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            ClassificationResultItemDao resultItemDao = MLDemoApp.getInstance().getDatabase().classificationResultItemDao();
            for (Map.Entry<Integer, ClassificationRVAdapter.ViewHolder> entry : selectedResultItems.entrySet()) {
                int position = entry.getKey();
                ClassificationResultItem ri = (ClassificationResultItem) itemList.get(position);
                resultItemDao.delete(ri);
                File itemFile = new File(ri.getFilePath());
                if (itemFile.exists())
                    itemFile.delete();
                File itemThumbnailFile = new File(ri.getThumbnailPath());
                if (itemThumbnailFile.exists())
                    itemThumbnailFile.delete();
                itemList.remove(position);
            }
            cancelSelection();
            selectedResultItems.clear();
            mode.finish();
            log.log("onActionItemClicked" + item.getTitle().toString());
            return true;
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
            isSelectionMode = false;
            cancelSelection();
            notifyDataSetChanged();
            log.log("onDestroyActionMode");
        }
    };

    public ClassificationRVAdapter(Context context, List<? extends ResultItem> itemList) {
        this.context = context;
        this.itemList = itemList;
    }

    @Override
    public ClassificationRVAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View layoutView = LayoutInflater.from(parent.getContext()).inflate(R.layout.segmentation_item_card, parent, false);
        return new ClassificationRVAdapter.ViewHolder(layoutView);
    }

    @Override
    public void onBindViewHolder(ClassificationRVAdapter.ViewHolder holder, int position) {
        ClassificationResultItem item = (ClassificationResultItem) itemList.get(position);
        ImageClassifier.Prediction prediction = item.getPredictionResultList().get(0);
        holder.fileNameTV.setText(item.getFileName());
        if (null != prediction) {
            holder.infoTV.setVisibility(View.VISIBLE);
            holder.infoTV.setText(prediction.toString());
        }
        RequestOptions requestOptions = new RequestOptions()
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .override(320, 320);
        Glide.with(context)
                .load(item.getThumbnailPath())
                .apply(requestOptions)
                .into(holder.previewIV);
        log.log("onBindViewHolder for position: " + position);
    }

    @Override
    public int getItemCount() {
        return itemList.size();
    }

    private void cancelSelection() {
        for (Map.Entry<Integer, ClassificationRVAdapter.ViewHolder> entry : selectedResultItems.entrySet()) {
            entry.getValue().itemView.setBackgroundColor(context.getResources().getColor(R.color.cardview_light_background));
        }
        log.log("cancelSelection(), unselected items count: " + selectedResultItems.size());
        selectedResultItems.clear();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        @BindView(R.id.file_nameTV)
        TextView fileNameTV;
        @BindView(R.id.info_tv)
        TextView infoTV;
        @BindView(R.id.segmentation_previewIV)
        ImageView previewIV;

        public ViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }

        @OnClick
        public void onClick() {
            if (isSelectionMode) {
                log.log("onClick() items selection mode");
                setChecked();
                return;
            }
            log.log("onClick() click mode");

            ClassificationResultItem item = (ClassificationResultItem) itemList.get(getAdapterPosition());
            ClassifyResultItemFragment itemResultFragment = ClassifyResultItemFragment.newInstance(item);
            FragmentManager fragmentManager = ((AppCompatActivity) context).getSupportFragmentManager();
            fragmentManager.beginTransaction().replace(R.id.fragment_classification, itemResultFragment).commit();
            log.log("onClick RV item. STarted fragment for the file path: " + item.getFilePath() + "\ngetAdapterPosition: " + getAdapterPosition());
        }

        @OnLongClick
        public boolean onLongClick() {
            isSelectionMode = !isSelectionMode;
            ((AppCompatActivity) context).startSupportActionMode(actionModeCallbacks);
            setChecked();
            log.log("OnLongClick, position: " + getAdapterPosition());
            return true;
        }

        public void setChecked() {
            int position = getAdapterPosition();
            if (position < 0) {
                log.log("setChecked() on the position less than 0");
                return;
            }
            boolean isChecked = selectedResultItems.containsKey(position);
            if (!isChecked) {
                selectedResultItems.put(position,
                        this);
                this.itemView.setBackgroundColor(context.getResources().getColor(R.color.cardview_select_background));
                log.log("Set item checked on position: " + position);
            } else {
                selectedResultItems.remove(position);
                this.itemView.setBackgroundColor(context.getResources().getColor(R.color.cardview_light_background));
                log.log("Set item unchecked on position: " + position);
            }
        }
    }
}

