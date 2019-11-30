package com.onpositive.dldemos;

import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;

import com.onpositive.dldemos.data.TFLiteItem;
import com.onpositive.dldemos.data.TFLiteItemDao;
import com.onpositive.dldemos.tools.Logger;
import com.onpositive.dldemos.tools.Utils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class TFLiteDownloaderAT extends AsyncTask<Uri, Integer, Void> {

    private TFliteAddFragment fragment;
    private Logger logger = new Logger(TFLiteDownloaderAT.class);

    public TFLiteDownloaderAT(TFliteAddFragment fragment) {
        this.fragment = fragment;
        logger.log("TFLiteDownloaderAT object created");
    }

    @Override
    protected Void doInBackground(Uri... uris) {
        downloadFile(uris[0]);
        return null;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        fragment.showDownloading(true);
        logger.log("onPreExecute called");
    }

    @Override
    protected void onPostExecute(Void v) {
        fragment.showDownloading(false);
        ((MainActivity) fragment.getActivity()).getSectionsPagerAdapter().refreshDataSet();
        ((MainActivity) fragment.getActivity()).getSectionsPagerAdapter().notifyDataSetChanged();
        logger.log("onPostExecute called");

        Intent intent = fragment.getActivity().getIntent(); //FIXME activity should not reload after fragment creation. New tab with a model fragment should be inserted without activity reload.
        fragment.getActivity().finish();
        fragment.getActivity().startActivity(intent);
    }

    private void downloadFile(Uri uri) {
        File path = fragment.getContext().getExternalFilesDir("TFLite");
        path.mkdirs();
        File outFile = new File(path, Utils.getFileName(fragment.getContext(), uri));
        try {
            InputStream is = fragment.getContext().getContentResolver().openInputStream(uri);
            OutputStream os = new FileOutputStream(outFile);
            byte[] data = new byte[is.available()];
            is.read(data);
            os.write(data);
            is.close();
            os.close();

            TFLiteItemDao tfLiteDao = MLDemoApp.getInstance().getDatabase().tfLiteItemDao();
            tfLiteDao.insert(new TFLiteItem(
                    outFile.getAbsolutePath(),
                    fragment.tfliteAddTitelTV.getText().toString(),
                    fragment.getTfModelType(),
                    fragment.getSize_x(),
                    fragment.getSize_y(),
                    fragment.getLabelsPath()
            ));
            logger.log("Downloaded file path: " + outFile
                    + "\n Title: " + fragment.tfliteAddTitelTV.getText().toString()
                    + "\nModelType: " + fragment.getTfModelType().toString()
                    + "\nSize_x: " + fragment.getSize_x()
                    + "\nSize_y: " + fragment.getSize_y());
        } catch (IOException e) {
            logger.log("File downloading failed. Error: " + e);
        }
    }
}