package com.onpositive.dldemos.data;

import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.view.ActionMode;
import androidx.recyclerview.widget.RecyclerView;

import com.onpositive.dldemos.MLDemoApp;
import com.onpositive.dldemos.R;
import com.onpositive.dldemos.tools.Logger;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.OnLongClick;

public abstract class RVBaseAdapter<T extends RVBaseAdapter.ViewHolder> extends RecyclerView.Adapter<T> {

    private Context context;
    private List<? extends ResultItem> itemList;
    private Map<Integer, T> selectedResultItems = new HashMap<>();
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
            ResultItemDao resultItemDao = MLDemoApp.getInstance().getDatabase().resultItemDao();
            for (Map.Entry<Integer, T> entry : selectedResultItems.entrySet()) {
                int position = entry.getKey();
                ResultItem ri = itemList.get(position);
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

    public RVBaseAdapter(Context context, List<? extends ResultItem> itemList) {
        this.context = context;
        this.itemList = itemList;
    }

    @Override
    public T onCreateViewHolder(ViewGroup parent, int viewType) {
        View layoutView = LayoutInflater.from(parent.getContext()).inflate(R.layout.segmentation_item_card, parent, false);
        return (T) new ViewHolder(layoutView);
    }

    @Override
    public void onBindViewHolder(T holder, int position) {
        ResultItem item = itemList.get(position);
        holder.fileNameTV.setText(item.getFileName());
        holder.previewIV.setImageBitmap(BitmapFactory.decodeFile(item.getThumbnailPath()));
        log.log("onBindViewHolder for position: " + position);
    }

    @Override
    public int getItemCount() {
        return itemList.size();
    }

    private void cancelSelection() {
        for (Map.Entry<Integer, T> entry : selectedResultItems.entrySet()) {
            entry.getValue().itemView.setBackgroundColor(context.getResources().getColor(R.color.cardview_light_background));
        }
        log.log("cancelSelection(), unselected items count: " + selectedResultItems.size());
        selectedResultItems.clear();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        @BindView(R.id.file_nameTV)
        TextView fileNameTV;
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

            ResultItem item = itemList.get(getAdapterPosition());
            Intent intent = new Intent(Intent.ACTION_VIEW);
            String mimeType = item.getContentType() == ContentType.IMAGE ? "image/*" : "video/*";
            intent.setDataAndType(Uri.parse(item.getFilePath()), mimeType);
            log.log("onClick RV item. Path: " + item.getFilePath() + "\ngetAdapterPosition: " + getAdapterPosition());
            if (intent.resolveActivity(context.getPackageManager()) != null) {
                context.startActivity(intent);
            } else {
                log.log("No one application for opening: " + item.getFilePath());
            }
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
                        (T) this);
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
